"""Lifecycle target/cleanup gates; fixture ADB responses are not native acceptance."""
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
spec = importlib.util.spec_from_file_location('lifecycle', SCRIPTS / 'qualify_parity_lifecycle.py')
DEVICE = importlib.util.module_from_spec(spec)
spec.loader.exec_module(DEVICE)


class LifecycleSafety(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.addCleanup(self.temp.cleanup)
        self.root = Path(self.temp.name)
        self.receipt = self.root / '.local/slice-evidence/build-receipt.json'
        self.receipt.parent.mkdir(parents=True)
        artifacts = {}
        for name in ['app/build/outputs/apk/debug/app-debug.apk',
                     'app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk']:
            path = self.root / name
            path.parent.mkdir(parents=True, exist_ok=True)
            path.write_bytes(b'fixture')
            artifacts[name] = DEVICE.hashlib.sha256(b'fixture').hexdigest()
        self.build = dict(exit_code=0, inputs_unchanged=True, inputs={}, artifacts=artifacts)
        self.receipt.write_text(json.dumps(self.build))
        self.calls = []
        self.cleanup_fails = False
        self.phone_available = True
        self.avd = 'calltrack-parity-api36-20261020'

    def fake_run(self, command, **kwargs):
        args = command[3:]
        self.calls.append(args)
        exact = {
            ('shell', 'getprop', 'ro.kernel.qemu'): '1',
            ('emu', 'avd', 'name'): self.avd + '\nOK',
            ('shell', 'getprop', 'sys.boot_completed'): '1',
            ('shell', 'service', 'check', 'phone'): 'Service phone: found' if self.phone_available else 'Service phone: not found',
            ('shell', 'cmd', 'connectivity', 'airplane-mode'): 'disabled',
            ('shell', 'cmd', 'wifi', 'status'): 'Wifi is enabled',
            ('shell', 'settings', 'get', 'global', 'mobile_data'): '1',
            ('shell', 'settings', 'get', 'global', 'stay_on_while_plugged_in'): '1',
            ('shell', 'dumpsys', 'power'): 'mWakefulness=Awake',
            ('shell', 'dumpsys', 'deviceidle', 'get', 'deep'): 'ACTIVE',
            ('shell', 'dumpsys', 'battery'): 'AC powered: true\nUSB powered: false\nWireless powered: false\nDock powered: false',
        }
        output = exact.get(tuple(args), '')
        if args[0] in ('install', 'uninstall'): output = 'Success'
        if args[:3] == ['shell', 'am', 'instrument']:
            # Fail before power/network mutations to exercise cleanup regardless of test outcome.
            output = 'OK (1 test)' if 'cleanupFixture' in ' '.join(args) and not self.cleanup_fails else 'FAILURES'
        return subprocess.CompletedProcess(command, 0, output, '')

    def qualify(self):
        identities = ["package: name='" + p + "'" for p in (DEVICE.PACKAGE, DEVICE.TEST_PACKAGE)]
        with patch.object(DEVICE, 'ROOT', self.root), patch.object(DEVICE, 'inputs', return_value={}), patch.object(
            DEVICE.subprocess, 'check_output', side_effect=identities
        ), patch.object(DEVICE.subprocess, 'run', side_effect=self.fake_run):
            return DEVICE.qualify('emulator-5580')

    def test_physical_serial_refused_before_tools_or_files(self):
        with patch.object(DEVICE.subprocess, 'run') as run:
            with self.assertRaisesRegex(RuntimeError, 'physical-device'): DEVICE.qualify('physical')
            run.assert_not_called()

    def test_failed_build_never_contacts_adb(self):
        self.build['exit_code'] = 1
        self.receipt.write_text(json.dumps(self.build))
        with patch.object(DEVICE, 'ROOT', self.root), patch.object(DEVICE.subprocess, 'run') as run:
            with self.assertRaisesRegex(RuntimeError, 'Successful build'): DEVICE.qualify('emulator-5580')
            run.assert_not_called()

    def test_wrong_avd_never_installs_or_changes_environment(self):
        self.avd = 'unrelated'
        with self.assertRaisesRegex(RuntimeError, 'dedicated'): self.qualify()
        self.assertFalse(any(args[0] == 'install' or args[:3] == ['shell', 'settings', 'put'] for args in self.calls))

    def test_native_failure_requires_scoped_cleanup_before_uninstall(self):
        self.assertEqual(1, self.qualify())
        cleanup = next(i for i, args in enumerate(self.calls) if 'cleanupFixture' in ' '.join(args))
        uninstall = next(i for i, args in enumerate(self.calls) if args[0] == 'uninstall')
        self.assertLess(cleanup, uninstall)
        receipt = json.loads((self.root / '.local/slice-evidence/parity-lifecycle-receipt.json').read_text())
        self.assertTrue(receipt['fixture_cleanup_verified'])
        self.assertTrue(receipt['emulator_restored']['verified'])

    def test_unverified_cleanup_preserves_instrumentation(self):
        self.cleanup_fails = True
        self.assertEqual(1, self.qualify())
        self.assertFalse(any(args[0] == 'uninstall' for args in self.calls))

    def test_restore_failure_is_retained_as_a_failed_result(self):
        with patch.object(DEVICE.EmulatorEnvironment, 'restore', return_value={'verified': False}) as restore:
            self.assertEqual(1, self.qualify())
            restore.assert_called_once()
        receipt = json.loads((self.root / '.local/slice-evidence/parity-lifecycle-receipt.json').read_text())
        self.assertFalse(receipt['emulator_restored']['verified'])
        self.assertEqual('failed', receipt['result'])

    def test_missing_phone_service_refuses_fixture_installation(self):
        self.phone_available = False
        with self.assertRaisesRegex(RuntimeError, 'phone service'):
            self.qualify()
        self.assertFalse(any(args[0] == 'install' for args in self.calls))


class LegacyFixtureRoot(unittest.TestCase):
    def test_foreign_target_refused_before_root_request(self):
        calls = []
        def command(*args):
            calls.append(args)
            if args == ('shell', 'getprop', 'ro.kernel.qemu'): return '1'
            if args == ('emu', 'avd', 'name'): return 'unrelated-emulator'
            raise AssertionError(args)
        with self.assertRaisesRegex(RuntimeError, 'dedicated emulator'):
            DEVICE.ensure_legacy_fixture_root(command)
        self.assertNotIn(('root',), calls)

    def test_owned_guest_root_reconnect_is_read_back(self):
        state = {'uid': '2000'}
        def command(*args):
            if args == ('shell', 'getprop', 'ro.kernel.qemu'): return '1'
            if args == ('emu', 'avd', 'name'): return 'calltrack-release-api26-20261022\nOK'
            if args == ('shell', 'getprop', 'ro.build.version.sdk'): return '26'
            if args == ('shell', 'id', '-u'): return state['uid']
            if args == ('root',): state['uid'] = '0'; return 'restarting adbd as root'
            raise AssertionError(args)
        self.assertTrue(DEVICE.ensure_legacy_fixture_root(command))
        self.assertFalse(DEVICE.ensure_legacy_fixture_root(command))

    def test_modern_guest_never_requests_legacy_root(self):
        calls = []
        def command(*args):
            calls.append(args)
            if args == ('shell', 'getprop', 'ro.kernel.qemu'): return '1'
            if args == ('emu', 'avd', 'name'): return 'calltrack-release-api33-20261022\nOK'
            if args == ('shell', 'getprop', 'ro.build.version.sdk'): return '33'
            raise AssertionError(args)
        with self.assertRaisesRegex(RuntimeError, 'restricted to API26'):
            DEVICE.ensure_legacy_fixture_root(command)
        self.assertNotIn(('root',), calls)
