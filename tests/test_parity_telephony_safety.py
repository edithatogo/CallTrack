"""Telephony fault gates must not reach physical devices or leave an uncertain active call."""
import importlib.util
import json
from pathlib import Path
import subprocess
import sys
import tempfile
import unittest
from unittest.mock import patch
sys.path.insert(0, str(Path(__file__).resolve().parents[1] / 'scripts'))
import qualify_parity_telephony as device
from parity_targets import RECOVERY_AVD


class TelephonySafety(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.addCleanup(self.temp.cleanup)
        self.root = Path(self.temp.name)
        evidence = self.root / '.local/slice-evidence'
        evidence.mkdir(parents=True)
        artifacts = {}
        for relative in ('app/build/outputs/apk/debug/app-debug.apk', 'app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk'):
            p = self.root / relative
            p.parent.mkdir(parents=True, exist_ok=True)
            p.write_bytes(b'fixture')
            artifacts[relative] = device.hashlib.sha256(b'fixture').hexdigest()
        (evidence / 'build-receipt.json').write_text(json.dumps({'exit_code': 0, 'inputs_unchanged': True, 'inputs': {}, 'artifacts': artifacts}))
        self.calls = []
        self.active = False
        self.creation_timeout = False
        self.cancel_failure = False
        self.cleaned = False

    def command(self, command, **kwargs):
        args = command[3:]
        self.calls.append(args)
        output, code = '', 0
        if args == ['shell', 'getprop', 'ro.kernel.qemu'] or args == ['shell', 'getprop', 'sys.boot_completed']:
            output = '1'
        elif args == ['emu', 'avd', 'name']:
            output = RECOVERY_AVD + '\nOK'
        elif args[0] in ('install', 'uninstall'):
            output = 'Success'
        elif args[:3] == ['shell', 'am', 'instrument']:
            if 'cleanupFixture' in ' '.join(args):
                self.cleaned = True
            output = 'OK (1 test)'
        elif args == ['shell', 'dumpsys', 'telephony.registry']:
            output = 'mCallState=' + ('1' if self.active else '0')
        elif args == ['emu', 'gsm', 'list']:
            output = device.NUMBER + '\nOK' if self.active else 'OK'
        elif args[:3] == ['emu', 'gsm', 'call']:
            self.active = True
            if self.creation_timeout:
                raise subprocess.TimeoutExpired(command, 180)
            output = 'OK'
        elif args[:3] == ['emu', 'gsm', 'cancel']:
            if self.cancel_failure:
                code = 1
            else:
                self.active = False
                output = 'OK'
        elif args == ['shell', 'date', '+%s']:
            output = '1'
        elif args[:3] == ['shell', 'run-as', device.PACKAGE]:
            output = '<map><long name="calendar" value="1"/><long name="sourceAt" value="0"/></map>' if args[-1].endswith('-telephony.xml') else '<map><string name="automaticStatusCode">COMPLETE</string><long name="automaticStatusAt" value="2000"/></map>'
        elif (len(args) == 2 and args[0] == 'shell' and args[1].startswith('content query')) or args[:3] == ['shell', 'content', 'query']:
            output = 'No result found.' if self.cleaned else 'Row: 0 _id=1'
        return subprocess.CompletedProcess(command, code, output, '')

    def run_fixture(self):
        identities = ["package: name='" + p + "'" for p in (device.PACKAGE, device.TEST_PACKAGE)]
        with patch.object(device, 'ROOT', self.root), patch.object(device, 'inputs', return_value={}), patch.object(device.subprocess, 'check_output', side_effect=identities), patch.object(device.subprocess, 'run', side_effect=self.command), patch.object(device.time, 'sleep'):
            return device.qualify('emulator-5582')

    def test_physical_target_refused_before_tools(self):
        with patch.object(device.subprocess, 'run') as run:
            with self.assertRaisesRegex(RuntimeError, 'physical-device'):
                device.qualify('physical-phone')
            run.assert_not_called()

    def test_existing_call_is_never_replaced_or_cancelled(self):
        self.active = True
        self.assertEqual(1, self.run_fixture())
        self.assertFalse(any(c[:3] in (['emu', 'gsm', 'call'], ['emu', 'gsm', 'cancel']) for c in self.calls))

    def test_empty_console_list_does_not_hide_android_ringing_state(self):
        normal = self.command
        def empty_console(command, **kwargs):
            if command[3:] == ['emu', 'gsm', 'list']:
                self.calls.append(command[3:])
                return subprocess.CompletedProcess(command, 0, 'OK', '')
            return normal(command, **kwargs)
        self.command = empty_console
        self.assertEqual(0, self.run_fixture())
        self.assertFalse(self.active)

    def test_unknown_call_creation_outcome_is_cleaned_before_uninstall(self):
        self.creation_timeout = True
        self.assertEqual(1, self.run_fixture())
        self.assertFalse(self.active)
        cancel = next(i for i, c in enumerate(self.calls) if c[:3] == ['emu', 'gsm', 'cancel'])
        uninstall = next(i for i, c in enumerate(self.calls) if c[0] == 'uninstall')
        self.assertLess(cancel, uninstall)

    def test_uncleared_call_preserves_cleanup_code(self):
        self.cancel_failure = True
        self.assertEqual(1, self.run_fixture())
        self.assertFalse(any(c[0] == 'uninstall' for c in self.calls))
        receipt = json.loads((self.root / '.local/slice-evidence/parity-telephony-receipt.json').read_text())
        self.assertFalse(receipt['fixture_cleanup_verified'])

    def test_success_requires_background_barrier_then_verifier_and_cleanup(self):
        self.assertEqual(0, self.run_fixture())
        end = next(i for i, c in enumerate(self.calls) if c[:3] == ['emu', 'gsm', 'cancel'])
        effect = next(i for i, c in enumerate(self.calls) if c[:3] == ['shell', 'content', 'query'])
        verify = next(i for i, c in enumerate(self.calls) if 'verifyRealMissedCallAndUniqueReceipt' in ' '.join(c))
        self.assertLess(end, effect)
        self.assertLess(effect, verify)
        self.assertFalse(self.active)
