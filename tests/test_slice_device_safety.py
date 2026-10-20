"""Fault injection for synthetic acceptance cleanup and installation boundaries."""
import importlib.util
import json
from pathlib import Path
import subprocess
import sys
import tempfile
import unittest
from unittest.mock import patch

SCRIPTS = Path(__file__).resolve().parents[1] / 'scripts'
sys.path.insert(0, str(SCRIPTS))
spec = importlib.util.spec_from_file_location('slice_device', SCRIPTS / 'qualify_slice_device.py')
DEVICE = importlib.util.module_from_spec(spec)
spec.loader.exec_module(DEVICE)


class DeviceSafety(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.addCleanup(self.temp.cleanup)
        self.root = Path(self.temp.name)
        evidence = self.root / '.local/slice-evidence'
        evidence.mkdir(parents=True)
        paths = ['app/build/outputs/apk/debug/app-debug.apk',
                 'app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk']
        artifacts = {}
        for name in paths:
            path = self.root / name
            path.parent.mkdir(parents=True, exist_ok=True)
            path.write_bytes(b'fixture')
            artifacts[name] = DEVICE.hashlib.sha256(b'fixture').hexdigest()
        self.build = {'exit_code': 0, 'inputs_unchanged': True, 'inputs': {}, 'artifacts': artifacts}
        self.receipt = evidence / 'build-receipt.json'
        self.receipt.write_text(json.dumps(self.build))
        self.calls = []

    def fake_run(self, command, **kwargs):
        args = command[3:]
        self.calls.append(args)
        output = ''
        if args[0] in ('install', 'uninstall'):
            output = 'Success'
        elif args[:3] == ['shell', 'am', 'start']:
            output = 'Status: ok'
        elif args[:2] == ['shell', 'pidof']:
            output = '123'
        elif args[:3] == ['shell', 'am', 'instrument']:
            if self.fail_cleanup and ('nativeRoundtripAndReopenedLedger' in ' '.join(args) or 'cleanupFixture' in ' '.join(args)):
                output = 'FAILURES'
            else:
                output = 'OK (1 test)'
        return subprocess.CompletedProcess(command, 0, output, '')

    def run_fixture(self):
        identities = ["package: name='" + p + "'" for p in (DEVICE.PACKAGE, DEVICE.TEST_PACKAGE)]
        with patch.object(DEVICE, 'ROOT', self.root), patch.object(DEVICE, 'inputs', return_value={}), patch.object(
            DEVICE.subprocess, 'check_output', side_effect=identities
        ), patch.object(DEVICE.subprocess, 'run', side_effect=self.fake_run):
            return DEVICE.qualify('synthetic-device')

    def test_cleanup_failure_preserves_installed_cleanup_code(self):
        self.fail_cleanup = True
        self.assertEqual(1, self.run_fixture())
        self.assertFalse(any(args[0] == 'uninstall' for args in self.calls))
        receipt = json.loads((self.root / '.local/slice-evidence/device-receipt.json').read_text())
        self.assertFalse(receipt['fixture_cleanup_verified'])
        self.assertEqual('failed', receipt['result'])

    def test_success_requires_fixture_and_package_absence_checks(self):
        self.fail_cleanup = False
        self.assertEqual(0, self.run_fixture())
        self.assertEqual(2, sum(args[0] == 'uninstall' for args in self.calls))
        receipt = json.loads((self.root / '.local/slice-evidence/device-receipt.json').read_text())
        self.assertTrue(receipt['fixture_cleanup_verified'])
        self.assertTrue(receipt['packages_removed'])

    def test_install_timeout_still_cleans_attempted_package(self):
        self.fail_cleanup = False
        normal = self.fake_run
        def timeout_install(command, **kwargs):
            if command[3] == 'install':
                self.calls.append(command[3:])
                raise subprocess.TimeoutExpired(command, 180)
            return normal(command, **kwargs)
        identities = ["package: name='" + p + "'" for p in (DEVICE.PACKAGE, DEVICE.TEST_PACKAGE)]
        with patch.object(DEVICE, 'ROOT', self.root), patch.object(DEVICE, 'inputs', return_value={}), patch.object(
            DEVICE.subprocess, 'check_output', side_effect=identities
        ), patch.object(DEVICE.subprocess, 'run', side_effect=timeout_install):
            self.assertEqual(1, DEVICE.qualify('synthetic-device'))
        self.assertTrue(any(args[0] == 'uninstall' and args[1] == DEVICE.PACKAGE for args in self.calls))

    def test_failed_build_never_installs(self):
        self.build['exit_code'] = 1
        self.receipt.write_text(json.dumps(self.build))
        with patch.object(DEVICE, 'ROOT', self.root), patch.object(DEVICE.subprocess, 'run') as run:
            with self.assertRaisesRegex(RuntimeError, 'Successful build'):
                DEVICE.qualify('synthetic-device')
            run.assert_not_called()
