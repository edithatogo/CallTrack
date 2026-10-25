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
    def test_instrumentation_failures_keep_only_protocol_state_categories(self):
        cases = (
            ('', 'instrumentation-output-empty', 'instrumentation-protocol=none'),
            ('INSTRUMENTATION_FAILED: Process crashed.', 'instrumentation-process-failed',
             'instrumentation-protocol=none'),
            ('FAILURES!!!\nINSTRUMENTATION_STATUS_CODE: -1', 'instrumentation-test-failed',
             'instrumentation-protocol=none'),
            ('INSTRUMENTATION_RESULT: shortMsg=Other failure',
             'instrumentation-result-without-success-marker', 'instrumentation-protocol=result'),
            ('INSTRUMENTATION_STATUS: stream=private text',
             'instrumentation-result-marker-missing', 'instrumentation-protocol=status'),
            ('arbitrary private text', 'instrumentation-result-marker-missing',
             'instrumentation-protocol=none'),
        )
        for output, expected, protocol in cases:
            with self.subTest(expected=expected):
                summary = device.native_failure_summary('renderedRangePreviewCancelConfirmAndRepeat', output)
                self.assertIn(expected, summary)
                self.assertIn(protocol, summary)
                self.assertNotIn('arbitrary private text', summary)

    def test_instrumentation_status_summary_keeps_only_keys_and_numeric_codes(self):
        output = (
            'INSTRUMENTATION_STATUS: id=AndroidJUnitRunner\n'
            'INSTRUMENTATION_STATUS: stream=private assertion text\n'
            'INSTRUMENTATION_STATUS: Error=private exception detail\n'
            'INSTRUMENTATION_STATUS_CODE: 1\n'
            'INSTRUMENTATION_STATUS_CODE: -1\n'
        )
        summary = device.native_failure_summary(
            'renderedRangePreviewCancelConfirmAndRepeat', output)
        self.assertIn('instrumentation-status-keys=Error,id,stream', summary)
        self.assertIn('instrumentation-status-codes=1,-1', summary)
        self.assertNotIn('private assertion text', summary)
        self.assertNotIn('private exception detail', summary)

    def test_apk_permission_summary_reports_only_allowlisted_manifest_names(self):
        outputs = [
            "package: org.calltrack.community.dev\n"
            "uses-permission: name='android.permission.READ_CALL_LOG'\n"
            "uses-permission: name='android.permission.READ_PHONE_STATE'\n"
            "uses-permission: name='android.permission.READ_SMS'\n",
            "package: org.calltrack.community.dev.test\n"
            "uses-permission: name='android.permission.READ_CONTACTS'\n",
        ]
        with patch.object(device.subprocess, 'check_output', side_effect=outputs):
            summary = device.apk_runtime_permission_summary(
                Path('app.apk'), Path('test.apk'), Path('aapt'))
        self.assertEqual(
            summary,
            'READ_CALL_LOG,READ_PHONE_STATE|instrumentation:READ_CONTACTS',
        )

    def test_apk_permission_summary_suppresses_other_permission_names(self):
        with patch.object(
                device.subprocess, 'check_output',
                side_effect=[
                    "uses-permission: name='android.permission.READ_SMS'\n",
                    "uses-permission: name='android.permission.ACCESS_NETWORK_STATE'\n",
                ]):
            summary = device.apk_runtime_permission_summary(
                Path('app.apk'), Path('test.apk'), Path('aapt'))
        self.assertEqual(summary, '|instrumentation:')

    def test_permission_activity_diagnostic_retains_only_its_launcher_package(self):
        dump = (
            '* Hist #0: ActivityRecord{launcher com.example.unrelated/.MainActivity}\n'
            'launchedFromPackage=com.example.unrelated userId=0\n'
            '* Hist #1: ActivityRecord{prompt '
            'com.android.packageinstaller/.permission.ui.GrantPermissionsActivity}\n'
            'packageName=com.android.packageinstaller\n'
            'launchedFromUid=10123 launchedFromPackage=com.markobl.calllogsync userId=0\n'
            '* Hist #2: ActivityRecord{other com.example.other/.MainActivity}\n'
            'launchedFromPackage=com.example.other userId=0\n'
        )
        summary = device.permission_activity_launcher_summary(dump)
        self.assertEqual(summary, 'permission-launcher=com.markobl.calllogsync')
        self.assertNotIn('com.example.unrelated', summary)
        self.assertNotIn('com.example.other', summary)

    def test_permission_activity_diagnostic_does_not_retain_arbitrary_text(self):
        dump = (
            '* Hist #0: ActivityRecord{prompt '
            'com.android.packageinstaller/.permission.ui.GrantPermissionsActivity}\n'
            'launchedFromPackage=+15551234567 account@example.test\n'
        )
        self.assertEqual(
            device.permission_activity_launcher_summary(dump),
            'permission-launcher=unavailable',
        )

    def test_activity_top_summary_retains_only_owner_category(self):
        dump = (
            'mResumedActivity: ActivityRecord{abc u0 '
            'com.google.android.inputmethod.latin/.LatinIME t42}\n'
            'mFocusedActivity: ActivityRecord{def u0 com.example.private/.Screen t43}\n'
        )
        self.assertEqual(device.activity_top_summary(dump), 'activity-top=input-method')
        self.assertNotIn('com.example.private', device.activity_top_summary(dump))

    def test_runtime_diagnostic_keeps_only_failure_categories_and_exception_class(self):
        logcat = (
            'E AndroidRuntime: FATAL EXCEPTION: main\n'
            'E AndroidRuntime: Process: org.calltrack.community.dev.test, PID: 4321\n'
            'E AndroidRuntime: java.lang.SecurityException: private number 0000000000\n'
            'E AndroidRuntime: \tat org.calltrack.community.dev.test.slice.ImportUiTest.run(ImportUiTest.java:120)\n'
            'ANR in org.calltrack.community.dev\n'
            'Permission Denial: private account details\n'
            'GrantPermissionsActivity visible\n'
        )
        diagnostic = device.runtime_failure_summary(logcat)
        self.assertIn('runtime-fatal-exception=true', diagnostic)
        self.assertIn('runtime-anr=true', diagnostic)
        self.assertIn('runtime-test-process-died=true', diagnostic)
        self.assertIn('runtime-permission-denial=true', diagnostic)
        self.assertIn('runtime-permission-controller=true', diagnostic)
        self.assertIn('runtime-exception=java.lang.SecurityException', diagnostic)
        self.assertIn('runtime-fatal-process=test', diagnostic)
        self.assertIn('runtime-fatal-source=test', diagnostic)
        self.assertNotIn('0000000000', diagnostic)
        self.assertNotIn('private account details', diagnostic)

    def test_runtime_diagnostic_classifies_input_method_without_retaining_stack_text(self):
        diagnostic = device.runtime_failure_summary(
            'E AndroidRuntime: FATAL EXCEPTION: main\n'
            'E AndroidRuntime: Process: com.google.android.inputmethod.latin, PID: 4321\n'
            'E AndroidRuntime: java.lang.IllegalStateException: private text\n'
            'E AndroidRuntime: \tat com.google.android.inputmethod.latin.SomeClass.run(SomeClass.java:12)\n'
        )
        self.assertIn('runtime-fatal-process=input-method', diagnostic)
        self.assertIn('runtime-fatal-source=input-method', diagnostic)
        self.assertNotIn('SomeClass', diagnostic)
        self.assertNotIn('private text', diagnostic)

    def test_nonzero_instrumentation_failure_uses_bounded_host_diagnostics(self):
        activity_dump = (
            '* Hist #0: ActivityRecord{prompt '
            'com.android.packageinstaller/.permission.ui.GrantPermissionsActivity}\n'
            'launchedFromUid=10123 launchedFromPackage=com.markobl.calllogsync userId=0\n'
            'mResumedActivity: ActivityRecord{abc u0 '
            'org.calltrack.community.dev/.MainActivity t4}\n'
        )
        logcat = (
            'FATAL EXCEPTION: main\n'
            'Process: org.calltrack.community.dev.test, PID: 4321\n'
            'java.lang.SecurityException: private test detail\n'
        )
        summary = device.instrumentation_failure_summary(
            'renderedRangePreviewCancelConfirmAndRepeat',
            'INSTRUMENTATION_STATUS: stream=private runner output\n',
            activity_dump,
            logcat,
            'READ_CALL_LOG|instrumentation:',
        )
        self.assertIn('permission-launcher=com.markobl.calllogsync', summary)
        self.assertIn('activity-top=app', summary)
        self.assertIn('runtime-fatal-exception', summary)
        self.assertIn('runtime-test-process-died', summary)
        self.assertIn('runtime-exception=java.lang.SecurityException', summary)
        self.assertIn('instrumentation-protocol=status', summary)
        self.assertNotIn('private runner output', summary)
        self.assertNotIn('private test detail', summary)

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
            'java.lang.AssertionError: manual visible content begins at 17 beneath system area 24\n'
            '    at com.markobl.calllogsync.slice.PositiveUiTest.renderedActivitiesRespectSystemBars(PositiveUiTest.java:114)\n'
            'private rendered label must remain hidden\n',
        )
        self.assertIn('system-inset screen=manual content_top=17 required_top=24', summary)
        self.assertNotIn('private rendered label', summary)

    def test_failure_summary_exposes_fixture_source_frame_without_call_details(self):
        summary = device.native_failure_summary(
            'prepareCalendarAndTaggedHistory',
            'java.lang.AssertionError: private fixture detail\n'
            '    at com.markobl.calllogsync.slice.ImportUiTest.prepareCalendarAndTaggedHistory(ImportUiTest.java:124)\n'
            '    at com.markobl.calllogsync.slice.ParityProviderTest.createCalendar(ParityProviderTest.java:109)\n',
        )
        self.assertIn('ImportUiTest.java:124', summary)
        self.assertIn('ParityProviderTest.java:109', summary)
        self.assertNotIn('private fixture detail', summary)

    def test_failure_summary_uses_fixed_import_dialog_category(self):
        summary = device.native_failure_summary(
            'renderedRangePreviewCancelConfirmAndRepeat',
            'java.lang.AssertionError: Visible enabled control missing: button1\n'
            '    at com.markobl.calllogsync.slice.RenderedUi.tap(RenderedUi.java:154)\n'
            '    at com.markobl.calllogsync.slice.ImportUiTest.confirm(ImportUiTest.java:104)\n',
        )
        self.assertIn('import-confirmation-dialog-action-missing', summary)
        self.assertIn('ImportUiTest.java:104', summary)

    def test_failure_summary_retains_only_allowlisted_dialog_action_metadata(self):
        summary = device.native_failure_summary(
            'renderedRangePreviewCancelConfirmAndRepeat',
            'Visible enabled positive dialog action missing; dialog-active=input-method '
            'dialog-windows=[app:application:[button1:android.widget.Button:'
            'enabled=false:clickable=true], permission:application:'
            '[permission_allow_button:android.widget.Button:enabled=true:clickable=true], '
            'input-method:input-method:[]] app-permissions=[calendar-read=granted,'
            'calendar-write=granted,call-log-read=denied,phone-state-read=denied] '
            'declared-runtime-permissions={app=[READ_CALL_LOG,READ_CALENDAR],'
            'instrumentation=[READ_CONTACTS]} '
            'permission-purpose=calendar\n'
            'private dialog title and message must remain hidden\n'
            'android.permission.READ_SMS must remain hidden\n'
            '    at com.markobl.calllogsync.slice.ImportUiTest.confirm(ImportUiTest.java:104)\n',
        )
        self.assertIn('dialog-active=input-method', summary)
        self.assertIn(
            'dialog-windows=app:application,permission:application,input-method:input-method',
            summary,
        )
        self.assertIn('dialog-controls=button1:android.widget.Button:false', summary)
        self.assertIn(
            'permission_allow_button:android.widget.Button:true', summary)
        self.assertIn('app-permissions=calendar-read:granted,calendar-write:granted,'
                      'call-log-read:denied,phone-state-read:denied', summary)
        self.assertIn('permission-purpose=calendar', summary)
        self.assertIn(
            'declared-runtime-permissions=app:READ_CALL_LOG,READ_CALENDAR'
            '|instrumentation:READ_CONTACTS',
            summary,
        )
        self.assertNotIn('READ_SMS', summary)
        self.assertNotIn('private dialog title', summary)

    def test_failure_summary_retains_only_allowlisted_permission_diagnostic(self):
        summary = device.native_failure_summary(
            'renderedPermissionsFiltersConfirmPauseAndReopen',
            'java.lang.AssertionError: Visible enabled control missing: permission_deny_button; '
            'permission-root=com.android.permissioncontroller controls='
            '[com.android.permissioncontroller:id/permission_deny_and_dont_ask_again_button:'
            'android.widget.Button:enabled=true]\n'
            '    at com.markobl.calllogsync.slice.PositiveUiTest.renderedPermissionsFiltersConfirmPauseAndReopen(PositiveUiTest.java:233)\n',
        )
        self.assertIn('missing-permission-control=permission_deny_button', summary)
        self.assertIn('permission-root=com.android.permissioncontroller', summary)
        self.assertIn('permission_deny_and_dont_ask_again_button:android.widget.Button:enabled=true', summary)
        self.assertNotIn('private label', summary)

    def test_failure_summary_reports_safe_controls_from_all_permission_windows(self):
        summary = device.native_failure_summary(
            'renderedPermissionsFiltersConfirmPauseAndReopen',
            'Visible enabled control missing: permission_allow_button; '
            'permission-root=com.android.permissioncontroller '
            'permission-controls=[com.android.permissioncontroller:id/'
            'permission_allow_foreground_only_button:android.widget.Button:enabled=true]\n',
        )
        self.assertIn('permission-root=com.android.permissioncontroller', summary)
        self.assertIn(
            'permission_allow_foreground_only_button:android.widget.Button:enabled=true',
            summary,
        )

    def test_failure_summary_reports_only_permission_requester_label_visibility(self):
        summary = device.native_failure_summary(
            'renderedRangePreviewCancelConfirmAndRepeat',
            'Visible enabled positive dialog action missing; permission-purpose=contacts '
            'permission-app-label=visible permission-launcher=com.android.shell '
            'app label and private permission message\n',
        )
        self.assertIn('permission-app-label=visible', summary)
        self.assertIn('permission-launcher=com.android.shell', summary)
        self.assertNotIn('app label', summary)
        self.assertNotIn('private permission message', summary)

    def test_failure_summary_rejects_unbounded_permission_launcher_text(self):
        summary = device.native_failure_summary(
            'renderedRangePreviewCancelConfirmAndRepeat',
            'Visible enabled positive dialog action missing; '
            'permission-launcher=+15551234567 private content\n',
        )
        self.assertNotIn('permission-launcher=', summary)
        self.assertNotIn('15551234567', summary)

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
