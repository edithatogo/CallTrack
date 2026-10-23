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
spec = importlib.util.spec_from_file_location('parity_provider', SCRIPTS / 'qualify_parity_provider.py')
DEVICE = importlib.util.module_from_spec(spec)
spec.loader.exec_module(DEVICE)


class ProviderSafety(unittest.TestCase):
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
        if args == ['shell', 'getprop', 'ro.kernel.qemu']:
            output = '1'
        elif args == ['emu', 'avd', 'name']:
            output = 'calltrack-parity-api36-20261020\nOK'
        elif args == ['shell', 'getprop', 'sys.boot_completed']:
            output = '1'
        elif args[0] in ('install', 'uninstall'):
            output = 'Success'
        elif args[:3] == ['shell', 'am', 'start']:
            output = 'Status: ok'
        elif args[:2] == ['shell', 'pidof']:
            output = '123'
        elif args[:3] == ['shell', 'am', 'instrument']:
            if self.fail_cleanup and ('durableCommitFailuresRecoverRealEffectsWithoutReinsertion' in ' '.join(args) or 'cleanupFixture' in ' '.join(args)):
                output = 'FAILURES'
            else:
                output = 'OK (1 test)'
        return subprocess.CompletedProcess(command, 0, output, '')

    def run_fixture(self):
        identities = ["package: name='" + p + "'" for p in (DEVICE.PACKAGE, DEVICE.TEST_PACKAGE)]
        with patch.object(DEVICE, 'ROOT', self.root), patch.object(DEVICE, 'inputs', return_value={}), patch.object(
            DEVICE.subprocess, 'check_output', side_effect=identities
        ), patch.object(DEVICE.subprocess, 'run', side_effect=self.fake_run):
            return DEVICE.qualify('emulator-5580')

    def test_cleanup_failure_preserves_installed_cleanup_code(self):
        self.fail_cleanup = True
        self.assertEqual(1, self.run_fixture())
        self.assertFalse(any(args[0] == 'uninstall' for args in self.calls))
        receipt = json.loads((self.root / '.local/slice-evidence/parity-provider-receipt.json').read_text())
        self.assertFalse(receipt['fixture_cleanup_verified'])
        self.assertEqual('failed', receipt['result'])

    def test_success_requires_fixture_and_package_absence_checks(self):
        self.fail_cleanup = False
        self.assertEqual(0, self.run_fixture())
        self.assertEqual(2, sum(args[0] == 'uninstall' for args in self.calls))
        receipt = json.loads((self.root / '.local/slice-evidence/parity-provider-receipt.json').read_text())
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
            self.assertEqual(1, DEVICE.qualify('emulator-5580'))
        self.assertTrue(any(args[0] == 'uninstall' and args[1] == DEVICE.PACKAGE for args in self.calls))

    def test_failed_build_never_installs(self):
        self.build['exit_code'] = 1
        self.receipt.write_text(json.dumps(self.build))
        with patch.object(DEVICE, 'ROOT', self.root), patch.object(DEVICE.subprocess, 'run') as run:
            with self.assertRaisesRegex(RuntimeError, 'Successful build'):
                DEVICE.qualify('emulator-5580')
            run.assert_not_called()


    def test_physical_serial_rejected_before_reading_files_or_tools(self):
        with patch.object(DEVICE.subprocess, 'run') as run:
            with self.assertRaisesRegex(RuntimeError, 'physical-device'):
                DEVICE.qualify('physical-device')
            run.assert_not_called()

    def test_wrong_avd_cannot_install_or_grant(self):
        normal = self.fake_run
        def wrong_avd(command, **kwargs):
            if command[3:] == ['emu', 'avd', 'name']:
                return subprocess.CompletedProcess(command, 0, 'unrelated-avd', '')
            return normal(command, **kwargs)
        identities = ["package: name='" + p + "'" for p in (DEVICE.PACKAGE, DEVICE.TEST_PACKAGE)]
        with patch.object(DEVICE, 'ROOT', self.root), patch.object(DEVICE, 'inputs', return_value={}), patch.object(
            DEVICE.subprocess, 'check_output', side_effect=identities
        ), patch.object(DEVICE.subprocess, 'run', side_effect=wrong_avd):
            with self.assertRaisesRegex(RuntimeError, 'dedicated'):
                DEVICE.qualify('emulator-5580')
        self.assertFalse(any(args[0] == 'install' or args[:3] == ['shell', 'pm', 'grant'] for args in self.calls))

    def test_read_only_phase_failure_restores_write_before_cleanup(self):
        self.fail_cleanup = False
        normal = self.fake_run
        def fail_read_only(command, **kwargs):
            if command[3:6] == ['shell', 'am', 'instrument'] and 'readOnlyRecoveryVerifiesExistingEffectWithoutWritePermission' in ' '.join(command):
                self.calls.append(command[3:])
                return subprocess.CompletedProcess(command, 0, 'FAILURES', '')
            return normal(command, **kwargs)
        identities = ["package: name='" + p + "'" for p in (DEVICE.PACKAGE, DEVICE.TEST_PACKAGE)]
        with patch.object(DEVICE, 'ROOT', self.root), patch.object(DEVICE, 'inputs', return_value={}), patch.object(
            DEVICE.subprocess, 'check_output', side_effect=identities
        ), patch.object(DEVICE.subprocess, 'run', side_effect=fail_read_only):
            self.assertEqual(1, DEVICE.qualify('emulator-5580'))
        failure = next(i for i, args in enumerate(self.calls) if 'readOnlyRecoveryVerifiesExistingEffectWithoutWritePermission' in ' '.join(args))
        recovery = self.calls[failure + 1:]
        write_grant = next(i for i, args in enumerate(recovery) if args[:3] == ['shell', 'pm', 'grant'] and args[-1] == 'android.permission.WRITE_CALENDAR')
        cleanup = next(i for i, args in enumerate(recovery) if 'cleanupFixture' in ' '.join(args))
        self.assertLess(write_grant, cleanup)
        receipt = json.loads((self.root / '.local/slice-evidence/parity-provider-receipt.json').read_text())
        self.assertTrue(receipt['fixture_cleanup_verified'])
        self.assertTrue(receipt['packages_removed'])
