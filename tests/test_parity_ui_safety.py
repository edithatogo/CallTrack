"""Rendered acceptance needs retained screenshots and cleanup, not just an OK fixture result."""
import json
from pathlib import Path
import subprocess
import sys
import tempfile
import unittest
from unittest.mock import patch, Mock
sys.path.insert(0, str(Path(__file__).resolve().parents[1] / 'scripts'))
import qualify_parity_ui as device
from parity_targets import RECOVERY_AVD, TRACK14_UI_AVD


class UiSafety(unittest.TestCase):
    def test_failure_summary_exposes_source_location_not_rendered_content(self):
        summary = device.native_failure_summary(
            'renderedAccessibilityLabelsTargetsAndKeyboardFocus',
            'INSTRUMENTATION_STATUS: stream=private UI label\n'
            'java.lang.AssertionError: Unlabelled interactive control id=private_id class=android.widget.ImageButton text=private UI label description=private UI label\n'
            '    at com.markobl.calllogsync.slice.PositiveUiTest.assertRenderedAccessibilityControls(PositiveUiTest.java:201)\n'
            '    at com.markobl.calllogsync.slice.PositiveUiTest.renderedAccessibilityLabelsTargetsAndKeyboardFocus(PositiveUiTest.java:145)\n'
            '    at org.junit.runner.Runner.run(Runner.java:10)\n',
        )
        self.assertIn('PositiveUiTest.java:201', summary)
        self.assertIn('PositiveUiTest.java:145', summary)
        self.assertIn('unlabelled control id=private_id class=android.widget.ImageButton', summary)
        self.assertNotIn('private UI label', summary)

        anchor_summary = device.native_failure_summary(
            'renderedAccessibilityLabelsTargetsAndKeyboardFocus',
            'java.lang.AssertionError: Enabled anchor must expose keyboard focus auto_access_action enabled=true\n'
            '    at com.markobl.calllogsync.slice.PositiveUiTest.renderedAccessibilityLabelsTargetsAndKeyboardFocus(PositiveUiTest.java:160)\n',
        )
        self.assertIn('non-focusable anchor id=auto_access_action enabled=true', anchor_summary)

    def test_failure_summary_retains_safe_system_inset_measurements(self):
        summary = device.native_failure_summary(
            'renderedActivitiesRespectSystemBars',
            'java.lang.AssertionError: manual first content begins at 17 beneath system area 24\n'
            '    at com.markobl.calllogsync.slice.PositiveUiTest.renderedActivitiesRespectSystemBars(PositiveUiTest.java:114)\n'
            'private rendered label must remain hidden\n',
        )
        self.assertIn('system-inset screen=manual content_top=17 required_top=24', summary)
        self.assertNotIn('private rendered label', summary)

    def test_failure_summary_retains_only_allowlisted_permission_diagnostic(self):
        summary = device.native_failure_summary(
            'renderedPermissionsFiltersConfirmPauseAndReopen',
            'java.lang.AssertionError: Permission choice did not advance the actual dialog: private label\n'
            '    at com.markobl.calllogsync.slice.PositiveUiTest.renderedPermissionsFiltersConfirmPauseAndReopen(PositiveUiTest.java:233)\n',
        )
        self.assertIn('permission-dialog-choice-did-not-advance', summary)
        self.assertNotIn('private label', summary)

    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.addCleanup(self.temp.cleanup)
        self.root = Path(self.temp.name)
        evidence = self.root / '.local/slice-evidence'
        evidence.mkdir(parents=True)
        artifacts = {}
        for relative in ('app/build/outputs/apk/debug/app-debug.apk', 'app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk'):
            path = self.root / relative
            path.parent.mkdir(parents=True, exist_ok=True)
            path.write_bytes(b'fixture')
            artifacts[relative] = device.hashlib.sha256(b'fixture').hexdigest()
        (evidence / 'build-receipt.json').write_text(json.dumps({'exit_code': 0, 'inputs_unchanged': True, 'inputs': {}, 'artifacts': artifacts}))
        self.calls = []
        self.phone_available = True
        self.systemui_anr = False
        self.files = 'screen.png\nscreen.txt'
        self.fail_cleanup = False
        self.avd = RECOVERY_AVD

    def command(self, command, **kwargs):
        args = command[3:]
        self.calls.append(args)
        output = ''
        if args == ['shell', 'getprop', 'ro.kernel.qemu'] or args == ['shell', 'getprop', 'sys.boot_completed']:
            output = '1'
        elif args == ['shell', 'service', 'check', 'phone']:
            output = 'Service phone: found' if self.phone_available else 'Service phone: not found'
        elif args == ['emu', 'avd', 'name']:
            output = self.avd + '\nOK'
        elif args == ['shell', 'dumpsys', 'window']:
            output = 'mCurrentFocus=Window{Application Not Responding: com.android.systemui}' if self.systemui_anr else 'mCurrentFocus=Window{Launcher}'
        elif args[0] in ('install', 'uninstall'):
            output = 'Success'
        elif args[:4] == ['shell', 'cmd', 'package', 'compile']:
            output = 'Success'
        elif args[:3] == ['shell', 'am', 'instrument']:
            output = 'FAILURES' if self.fail_cleanup and ('renderedPermissions' in ' '.join(args) or 'cleanupFixture' in ' '.join(args)) else 'OK (1 test)'
        elif args[:3] == ['shell', 'run-as', device.PACKAGE]:
            output = self.files
        elif args[0] == 'exec-out':
            output = b'\x89PNG\r\n\x1a\n' + b'0' * 100 if args[-1].endswith('.png') else b'visible fixture controls'
        return subprocess.CompletedProcess(command, 0, output, b'' if isinstance(output, bytes) else '')

    def run_fixture(self, suite="settings", receipt_dir=None):
        identities = ["package: name='" + p + "'" for p in (device.PACKAGE, device.TEST_PACKAGE)]
        environment = Mock()
        environment.snapshot.return_value = {'awake': 'Awake'}
        environment.restore.return_value = {'verified': True}
        with patch.object(device, 'ROOT', self.root), patch.object(device, 'inputs', return_value={}), patch.object(device.subprocess, 'check_output', side_effect=identities), patch.object(device.subprocess, 'run', side_effect=self.command), patch.object(device, 'EmulatorEnvironment', return_value=environment):
            return device.qualify('emulator-5582', suite, receipt_dir)

    def test_guest_anr_refuses_ui_acceptance_before_install(self):
        self.systemui_anr = True
        self.assertEqual(1, self.run_fixture())
        self.assertFalse(any(args[0] == 'install' for args in self.calls))
        receipt = json.loads((self.root / '.local/slice-evidence/parity-ui-receipt.json').read_text())
        self.assertIn('emulator has an ANR', receipt['error'])
        self.assertTrue(receipt['packages_removed'])

    def test_matrix_receipt_isolated_and_cannot_overwrite_a_prior_run(self):
        output = self.root / 'matrix' / 'first'
        self.assertEqual(0, self.run_fixture('accessibility', output))
        receipt = output / 'parity-ui-accessibility-receipt.json'
        original = receipt.read_bytes()
        self.assertFalse((self.root / '.local/slice-evidence/parity-ui-accessibility-receipt.json').exists())
        self.calls.clear()
        with self.assertRaises(FileExistsError):
            self.run_fixture('accessibility', output)
        self.assertFalse(self.calls)
        self.assertEqual(original, receipt.read_bytes())

    def test_accessibility_suite_retains_screenshots_and_cleanup(self):
        self.assertEqual(0, self.run_fixture('accessibility'))
        calls = ' '.join(' '.join(args) for args in self.calls)
        self.assertIn('renderedAccessibilityLabelsTargetsAndKeyboardFocus', calls)
        self.assertIn('cleanupFixture', calls)
        receipt = json.loads((self.root / '.local/slice-evidence/parity-ui-accessibility-receipt.json').read_text())
        self.assertTrue(receipt['packages_removed'])
        self.assertTrue(receipt['fixture_cleanup_verified'])

    def test_physical_target_refused_before_tools(self):
        with patch.object(device.subprocess, 'run') as run:
            with self.assertRaisesRegex(RuntimeError, 'physical-device'):
                device.qualify('physical-phone')
            run.assert_not_called()

    def test_wrong_guest_never_installs(self):
        self.avd = 'unrelated'
        with self.assertRaisesRegex(RuntimeError, 'dedicated'):
            self.run_fixture()
        self.assertFalse(any(c[0] == 'install' for c in self.calls))

    def test_no_screenshot_preserves_installed_evidence_and_cleanup_code(self):
        self.files = ''
        self.assertEqual(1, self.run_fixture())
        self.assertFalse(any(c[0] == 'uninstall' for c in self.calls))

    def test_fixture_cleanup_failure_preserves_code(self):
        self.fail_cleanup = True
        self.assertEqual(1, self.run_fixture())
        self.assertFalse(any(c[0] == 'uninstall' for c in self.calls))

    def test_artifact_traversal_is_refused(self):
        self.files = '../escape.png'
        self.assertEqual(1, self.run_fixture())
        self.assertFalse((self.root / '.local/slice-evidence/escape.png').exists())
        self.assertFalse(any(c[0] == 'uninstall' for c in self.calls))

    def test_import_suite_uses_its_own_class_and_receipt(self):
        self.assertEqual(0, self.run_fixture('imports'))
        methods = [' '.join(c) for c in self.calls if c[:3] == ['shell', 'am', 'instrument']]
        self.assertTrue(all('ImportUiTest#' in method for method in methods))
        receipt = json.loads((self.root / '.local/slice-evidence/parity-ui-imports-receipt.json').read_text())
        self.assertEqual('imports', receipt['suite'])
        self.assertTrue(receipt['fixture_cleanup_verified'])

    def test_recovery_suite_keeps_read_only_permission_boundary(self):
        self.assertEqual(0, self.run_fixture('recovery'))
        methods = [' '.join(c) for c in self.calls if c[:3] == ['shell', 'am', 'instrument']]
        self.assertTrue(all('RecoveryUiTest#' in method for method in methods))
        check_index = next(i for i,c in enumerate(self.calls) if 'renderedExistingCheckWithoutWriteAccess' in ' '.join(c))
        self.assertEqual(['shell', 'pm', 'revoke', device.PACKAGE, 'android.permission.WRITE_CALENDAR'], self.calls[check_index-1])
        self.assertEqual(['shell', 'pm', 'grant', device.PACKAGE, 'android.permission.WRITE_CALENDAR'], self.calls[check_index+1])
        receipt = json.loads((self.root / '.local/slice-evidence/parity-ui-recovery-receipt.json').read_text())
        self.assertEqual('recovery', receipt['suite'])
        self.assertTrue(receipt['fixture_cleanup_verified'])

    def test_timed_out_instrumentation_retains_partial_diagnostics(self):
        original = self.command
        def timeout(command, **kwargs):
            if 'renderedExplicitHistoryResumeCompletesPartialWork' in ' '.join(command):
                self.assertEqual(300, kwargs['timeout'])
                raise subprocess.TimeoutExpired(command, 300, output=b'partial native progress', stderr=b'diagnostic')
            return original(command, **kwargs)
        self.command = timeout
        self.assertEqual(1, self.run_fixture('imports'))
        receipt = json.loads((self.root / '.local/slice-evidence/parity-ui-imports-receipt.json').read_text())
        timed = [step for step in receipt['steps'] if step.get('timed_out')]
        self.assertEqual(1, len(timed))
        self.assertIn('partial native progress', timed[0]['output'])
        self.assertIn('diagnostic', timed[0]['output'])
        self.assertTrue(receipt['fixture_cleanup_verified'])
        self.assertTrue(receipt['packages_removed'])

    def test_unknown_suite_refused_before_tools(self):
        with patch.object(device.subprocess, 'run') as run:
            with self.assertRaisesRegex(ValueError, 'Unknown UI suite'):
                device.qualify('emulator-5582', 'arbitrary')
            run.assert_not_called()

    def test_success_retains_binary_screenshot_hash_before_uninstall(self):
        self.assertEqual(0, self.run_fixture())
        receipt = json.loads((self.root / '.local/slice-evidence/parity-ui-receipt.json').read_text())
        self.assertEqual(2, len(receipt['ui_files']))
        self.assertTrue(receipt['packages_removed'])
        pull = next(i for i, c in enumerate(self.calls) if c[0] == 'exec-out')
        uninstall = next(i for i, c in enumerate(self.calls) if c[0] == 'uninstall')
        self.assertLess(pull, uninstall)

    def test_missing_phone_service_refuses_fixture_installation(self):
        self.phone_available = False
        with self.assertRaisesRegex(RuntimeError, 'phone service'):
            self.run_fixture()
        self.assertFalse(any(args[0] == 'install' for args in self.calls))

    def test_track14_ui_guest_is_accepted_without_widening_other_harnesses(self):
        self.avd = TRACK14_UI_AVD
        self.assertEqual(0, self.run_fixture('imports'))
        self.assertTrue(any(c[0] == 'install' for c in self.calls))

    def test_installed_packages_are_compiled_before_instrumentation(self):
        self.assertEqual(0, self.run_fixture('imports'))
        compile_positions = [i for i, c in enumerate(self.calls) if c[:4] == ['shell', 'cmd', 'package', 'compile']]
        instrument_position = next(i for i, c in enumerate(self.calls) if c[:3] == ['shell', 'am', 'instrument'])
        self.assertEqual(2, len(compile_positions))
        self.assertLess(max(compile_positions), instrument_position)
