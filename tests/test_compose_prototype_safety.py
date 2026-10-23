"""Fault fixtures for prototype target/build/cleanup boundaries; never contact a device."""
import hashlib
import importlib.util
import json
from pathlib import Path
import subprocess
import tempfile
import unittest
from unittest.mock import patch

SCRIPT = Path(__file__).resolve().parents[1] / 'scripts/qualify_compose_prototype.py'
SPEC = importlib.util.spec_from_file_location('compose_probe', SCRIPT)
PROBE = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(PROBE)


class ComposeSafety(unittest.TestCase):
    def test_physical_device_is_rejected_before_inputs_or_tool_access(self):
        with patch.object(PROBE, 'inputs') as inputs, patch.object(PROBE.subprocess, 'run') as run:
            with self.assertRaisesRegex(RuntimeError, 'Physical devices'):
                PROBE.qualify('physical-fixture')
            inputs.assert_not_called()
            run.assert_not_called()

    def fixture(self):
        temporary = tempfile.TemporaryDirectory()
        self.addCleanup(temporary.cleanup)
        root = Path(temporary.name)
        project = root / 'prototypes/compose'
        evidence = root / 'evidence'
        evidence.mkdir()
        artifacts = {}
        for name in ('debug/compose-prototype-debug.apk',
                     'androidTest/debug/compose-prototype-debug-androidTest.apk'):
            path = project / 'build/outputs/apk' / name
            path.parent.mkdir(parents=True, exist_ok=True)
            path.write_bytes(b'fixture')
            artifacts[str(path.relative_to(root))] = hashlib.sha256(b'fixture').hexdigest()
        (evidence / 'build-receipt.json').write_text(json.dumps({
            'exit_code': 0, 'inputs_unchanged': True, 'inputs': {}, 'artifacts': artifacts}))
        for name, value in (('ROOT', root), ('PROJECT', project), ('EVIDENCE', evidence)):
            patcher = patch.object(PROBE, name, value)
            patcher.start()
            self.addCleanup(patcher.stop)
        return evidence

    def test_stale_inputs_refuse_device_access(self):
        self.fixture()
        with patch.object(PROBE, 'inputs', return_value={'changed.kt': 'new'}), patch.object(
            PROBE.subprocess, 'run'
        ) as run:
            with self.assertRaisesRegex(RuntimeError, 'does not match'):
                PROBE.qualify('emulator-5580')
            run.assert_not_called()

    def test_unrelated_avd_never_installs(self):
        self.fixture()
        calls = []
        def run(command, **kwargs):
            args = command[3:]
            calls.append(args)
            output = '1' if args == ['shell', 'getprop', 'ro.kernel.qemu'] else 'another-avd\nOK'
            return subprocess.CompletedProcess(command, 0, output, '')
        with patch.object(PROBE, 'inputs', return_value={}), patch.object(PROBE.subprocess, 'run', side_effect=run):
            with self.assertRaisesRegex(RuntimeError, 'dedicated AVD'):
                PROBE.qualify('emulator-5580')
        self.assertFalse(any(command[0] == 'install' for command in calls))

    def test_install_timeout_still_attempts_uninstall_and_checks_absence(self):
        evidence = self.fixture()
        calls = []
        def run(command, **kwargs):
            args = command[3:]
            calls.append(args)
            if args[0] == 'install':
                raise subprocess.TimeoutExpired(command, 180)
            if args == ['shell', 'getprop', 'ro.kernel.qemu']:
                output = '1'
            elif args == ['emu', 'avd', 'name']:
                output = PROBE.AVD + '\nOK'
            elif args == ['shell', 'getprop', 'sys.boot_completed']:
                output = '1'
            elif args == ['shell', 'getprop', 'ro.build.version.sdk']:
                output = '36'
            elif args[0] == 'uninstall':
                output = 'Success'
            else:
                output = ''
            return subprocess.CompletedProcess(command, 0, output, '')
        identities = ["package: name='" + name + "'" for name in (PROBE.PACKAGE, PROBE.TEST_PACKAGE)]
        with patch.object(PROBE, 'inputs', return_value={}), patch.object(PROBE.subprocess, 'run', side_effect=run), patch.object(
            PROBE.subprocess, 'check_output', side_effect=identities
        ):
            self.assertEqual(1, PROBE.qualify('emulator-5580'))
        self.assertIn(['uninstall', PROBE.PACKAGE], calls)
        self.assertEqual(['shell', 'pm', 'list', 'packages', PROBE.PACKAGE], calls[-1])
        result = json.loads((evidence / 'device-receipt.json').read_text())
        self.assertEqual('failed', result['result'])
        self.assertTrue(result['packages_removed'])

    def test_sensitive_permission_regression_refuses_installation(self):
        self.fixture()
        calls = []
        def run(command, **kwargs):
            args = command[3:]
            calls.append(args)
            if args == ['shell', 'getprop', 'ro.kernel.qemu']:
                output = '1'
            elif args == ['emu', 'avd', 'name']:
                output = PROBE.AVD + '\nOK'
            elif args == ['shell', 'getprop', 'sys.boot_completed']:
                output = '1'
            else:
                output = ''
            return subprocess.CompletedProcess(command, 0, output, '')
        identity = "package: name='" + PROBE.PACKAGE + "'\nuses-permission: name='android.permission.READ_CALENDAR'"
        with patch.object(PROBE, 'inputs', return_value={}), patch.object(PROBE.subprocess, 'run', side_effect=run), patch.object(
            PROBE.subprocess, 'check_output', side_effect=[identity, "package: name='" + PROBE.TEST_PACKAGE + "'"]
        ):
            with self.assertRaisesRegex(RuntimeError, 'forbidden permission'):
                PROBE.qualify('emulator-5580')
        self.assertFalse(any(c[0] == 'install' for c in calls))
