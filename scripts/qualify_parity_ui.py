#!/usr/bin/env -S uv run python
"""Dedicated-emulator real CallLog/CalendarContract and rendered UI acceptance.

Requires selected device, matching build receipt and absent test packages. On native
failure attempts scoped fixture recovery BEFORE uninstalling its cleanup code.
"""
from parity_targets import is_dedicated_ui_avd
from parity_health import require_healthy_boot
import argparse
import hashlib
import json
import re
from parity_environment import EmulatorDisplayEnvironment as EmulatorEnvironment
from pathlib import Path
import subprocess
import uuid
from run_slice_build import inputs

ROOT = Path(__file__).resolve().parents[1]
PACKAGE = 'org.calltrack.community.dev'
TEST_PACKAGE = PACKAGE + '.test'
TEST_CLASS = 'com.markobl.calllogsync.slice.PositiveUiTest'


def native_failure_summary(method, output):
    """Return source locations and allowlisted numeric/platform diagnostics."""
    frames = []
    for line in output.splitlines():
        frame = line.strip()
        if (frame.startswith('at com.markobl.calllogsync.slice.')
                and re.search(r'(?:PositiveUiTest|ImportUiTest|RecoveryUiTest|ParityProviderTest|RenderedUi)\.java:\d+', frame)):
            frames.append(frame)
    detail = '; '.join(frames[:6])
    # The rendered accessibility gate may fail on an unlabeled control. Retain
    # only its resource ID and widget class; suppress its visible text/label.
    control = re.search(
        r'Unlabelled interactive control id=([^\s]*) class=([^\s]*)', output)
    if control:
        detail = (detail + '; ' if detail else '') + (
            'unlabelled control id=' + control.group(1) +
            ' class=' + control.group(2))
    anchor = re.search(
        r'Enabled anchor must expose keyboard focus ([a-z0-9_]+) enabled=(true|false)',
        output)
    if anchor:
        detail = (detail + '; ' if detail else '') + (
            'non-focusable anchor id=' + anchor.group(1) +
            ' enabled=' + anchor.group(2))
    # Keep only numeric geometry from the system-bars assertion; screenshot or
    # arbitrary rendered strings remain private in the ignored local receipt.
    geometry = re.search(
        r'(settings|manual|imports|recovery) visible content begins at (-?\d+) beneath system area (\d+)',
        output)
    if geometry:
        detail = (detail + '; ' if detail else '') + (
            'system-inset screen=' + geometry.group(1) +
            ' content_top=' + geometry.group(2) +
            ' required_top=' + geometry.group(3))
    # Permission-controller UI text varies by Android release. Surface only
    # known assertion categories and allowlisted control IDs, never dialog text.
    if 'Permission choice did not advance the actual dialog' in output:
        detail = (detail + '; ' if detail else '') + 'permission-dialog-choice-did-not-advance'
    elif 'Permission dialog sequence did not complete' in output:
        detail = (detail + '; ' if detail else '') + 'permission-dialog-sequence-incomplete'
    if 'Refusing nonempty call provider' in output:
        detail = (detail + '; ' if detail else '') + 'synthetic-calllog-provider-not-empty'
    if 'java.lang.AssertionError: expected non-null' in output:
        detail = (detail + '; ' if detail else '') + 'expected-provider-result-was-null'
    if 'Visible enabled control missing: button1' in output:
        detail = (detail + '; ' if detail else '') + 'import-confirmation-dialog-action-missing'
    if 'Refusing a control in an unexpected package:' in output:
        detail = (detail + '; ' if detail else '') + 'dialog-action-owned-by-unexpected-window'
    dialog_root = re.search(r'dialog-root=(app|other|unavailable)', output)
    if dialog_root:
        detail = (detail + '; ' if detail else '') + 'dialog-root=' + dialog_root.group(1)
    dialog_controls = re.findall(
        r'(button[1-3]|other):(android\.widget\.Button|other):enabled=(true|false):'
        r'clickable=(true|false):label-match=(true|false)', output)
    if dialog_controls:
        detail = (detail + '; ' if detail else '') + 'dialog-controls=' + ','.join(
            ':'.join(control) for control in dialog_controls[:12])
    missing_permission = re.search(
        r'Visible enabled control missing: (permission_(?:deny|allow)_button)', output)
    if missing_permission:
        detail = (detail + '; ' if detail else '') + (
            'missing-permission-control=' + missing_permission.group(1))
    permission_root = re.search(
        r'permission-root=(com\.android\.permissioncontroller|com\.google\.android\.permissioncontroller|com\.android\.packageinstaller|com\.google\.android\.packageinstaller|unavailable|diagnostic-unavailable)',
        output)
    if permission_root:
        detail = (detail + '; ' if detail else '') + 'permission-root=' + permission_root.group(1)
    system_controls = re.findall(
        r':id/([a-z0-9_]+):(android\.[A-Za-z0-9_.$]+):enabled=(true|false)', output)
    if system_controls:
        safe_controls = [
            item for item in system_controls[:16]
            if item[0].startswith('permission_') or item[0] in ('button1', 'button2', 'button3')
        ]
        if safe_controls:
            detail = (detail + '; ' if detail else '') + (
                'permission-controls=' + ','.join(
                    control + ':' + widget + ':enabled=' + enabled
                    for control, widget, enabled in safe_controls))
    suffix = ' (' + detail + ')' if detail else ''
    return 'Native acceptance failed: ' + method + suffix


def qualify(serial, suite="settings", receipt_dir=None):
    if suite not in ("settings", "imports", "recovery", "accessibility"):
        raise ValueError("Unknown UI suite")
    test_class = {"settings": TEST_CLASS, "imports": "com.markobl.calllogsync.slice.ImportUiTest",
                  "recovery": "com.markobl.calllogsync.slice.RecoveryUiTest", "accessibility": TEST_CLASS}[suite]
    if not re.fullmatch(r'emulator-\d+', serial):
        raise RuntimeError('Provider harness refuses a physical-device serial')
    evidence = ROOT / '.local/slice-evidence'
    build = json.loads((evidence / 'build-receipt.json').read_text())
    if build['exit_code'] != 0 or not build['inputs_unchanged'] or build['inputs'] != inputs():
        raise RuntimeError('Successful build receipt does not match current source')
    apks = [ROOT / 'app/build/outputs/apk/debug/app-debug.apk',
            ROOT / 'app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk']
    aapt = ROOT / '.local/android-sdk/build-tools/35.0.0/aapt'
    for package, apk in zip((PACKAGE, TEST_PACKAGE), apks):
        if build['artifacts'].get(str(apk.relative_to(ROOT))) != hashlib.sha256(apk.read_bytes()).hexdigest():
            raise RuntimeError('Artifact differs from successful build')
        identity = subprocess.check_output([str(aapt), 'dump', 'badging', str(apk)], text=True)
        if "package: name='" + package + "'" not in identity:
            raise RuntimeError('Unexpected APK identity')
    if receipt_dir is not None:
        receipt_dir = Path(receipt_dir)
        receipt_dir.mkdir(parents=True, exist_ok=False)
    receipt = {'suite': suite, 'fixture_token': uuid.uuid4().hex, 'steps': [], 'artifacts': build['artifacts'], 'inputs': build['inputs']}
    installed = []
    fixture_possible = False
    fixture_clean = False
    ui_attempted = False
    ui_captured = False
    environment = None

    def command(*args):
        # A rendered resume includes setup, stop/reopen and a bounded 150-second
        # completion wait. Leave time for its own failure/capture/cleanup to report.
        timeout = 300 if 'instrument' in args else 180
        try:
            result = subprocess.run([str(ROOT / '.local/android-sdk/platform-tools/adb'), '-s', serial, *args],
                                    capture_output=True, text=True, timeout=timeout)
        except subprocess.TimeoutExpired as error:
            def decoded(value):
                return value.decode(errors='replace') if isinstance(value, bytes) else (value or '')
            receipt['steps'].append({'args': list(args), 'timed_out': True,
                                     'timeout_seconds': timeout,
                                     'output': decoded(error.stdout) + decoded(error.stderr)})
            raise
        receipt['steps'].append({'args': list(args), 'exit_code': result.returncode,
                                 'output': result.stdout + result.stderr})
        if result.returncode:
            raise RuntimeError('ADB command failed: ' + ' '.join(args))
        return result.stdout.strip()

    def check(method):
        output = command('shell', 'am', 'instrument', '-w', '-r', '-e', 'fixtureToken', receipt['fixture_token'],
                         '-e', 'class', test_class + '#' + method,
                         TEST_PACKAGE + '/androidx.test.runner.AndroidJUnitRunner')
        if 'OK (1 test)' not in output or 'FAILURES' in output or 'INSTRUMENTATION_FAILED' in output:
            # The full output stays in the ignored receipt; public CI only gets
            # source locations, never rendered labels or fixture values.
            raise RuntimeError(native_failure_summary(method, output))

    def capture_ui():
        directory = evidence / ('ui-' + receipt['fixture_token'])
        directory.mkdir(exist_ok=True)
        names = command('shell', 'run-as', PACKAGE, 'ls', 'files/ui-evidence').splitlines()
        files = {}
        for name in names:
            if not re.fullmatch(r'[a-z0-9-]+\.(png|txt)', name):
                raise RuntimeError('Unexpected UI artifact name')
            args = ['exec-out', 'run-as', PACKAGE, 'cat', 'files/ui-evidence/' + name]
            result = subprocess.run([str(ROOT / '.local/android-sdk/platform-tools/adb'), '-s', serial, *args],
                                    capture_output=True, timeout=60)
            if result.returncode or not result.stdout:
                raise RuntimeError('UI artifact readback failed')
            if name.endswith('.png') and (len(result.stdout) < 32 or not result.stdout.startswith(b'\x89PNG\r\n\x1a\n')):
                raise RuntimeError('Invalid screenshot readback')
            path = directory / name
            path.write_bytes(result.stdout)
            files[str(path.relative_to(ROOT))] = {'sha256': hashlib.sha256(result.stdout).hexdigest(), 'bytes': len(result.stdout)}
        receipt['ui_files'] = files
        if not any(name.endswith('.png') for name in names):
            raise RuntimeError('No rendered screenshot retained')

    if command('shell', 'getprop', 'ro.kernel.qemu') != '1':
        raise RuntimeError('Not an emulator')
    if not is_dedicated_ui_avd(command('emu', 'avd', 'name')):
        raise RuntimeError('Not the dedicated CallTrack AVD')
    require_healthy_boot(command)
    for package in (PACKAGE, TEST_PACKAGE):
        if 'package:' + package in command('shell', 'pm', 'list', 'packages', package).splitlines():
            raise RuntimeError('Refusing to replace existing package ' + package)
    try:
        window = command('shell', 'dumpsys', 'window')
        if re.search(r'mCurrentFocus=.*Application Not Responding:', window):
            raise RuntimeError('Dedicated emulator has an ANR; repair guest before rendered acceptance')
        environment = EmulatorEnvironment(command)
        receipt['emulator_original'] = environment.snapshot().copy()
        environment.changed = True
        command('shell', 'input', 'keyevent', 'KEYCODE_WAKEUP')
        command('shell', 'wm', 'dismiss-keyguard')
        receipt['android'] = command('shell', 'getprop', 'ro.build.version.release')
        receipt['sdk'] = command('shell', 'getprop', 'ro.build.version.sdk')
        for package, apk in zip((PACKAGE, TEST_PACKAGE), apks):
            # A timeout is an unknown outcome, not evidence that installation did not occur.
            installed.append(package)
            if 'Success' not in command('install', str(apk)):
                raise RuntimeError('Installation not confirmed')
        # Dex-opt only these exact fresh test packages before UI instrumentation. This
        # avoids attributing first-launch ART extraction delays to application behavior.
        for package in (PACKAGE, TEST_PACKAGE):
            if 'Success' not in command('shell', 'cmd', 'package', 'compile', '-m', 'speed', package):
                raise RuntimeError('Package dex compilation not confirmed: ' + package)
        for permission in ('READ_CALENDAR', 'WRITE_CALENDAR'):
            command('shell', 'pm', 'grant', PACKAGE, 'android.permission.' + permission)
        fixture_possible = True
        if suite == 'accessibility':
            ui_attempted = True
            check('renderedAccessibilityLabelsTargetsAndKeyboardFocus')
        elif suite == 'settings':
            check('prepareCalendarWithoutCallAccess')
            for permission in ('READ_CALENDAR', 'WRITE_CALENDAR'):
                command('shell', 'pm', 'revoke', PACKAGE, 'android.permission.' + permission)
            ui_attempted = True
            check('renderedActivitiesRespectSystemBars')
            check('renderedPermissionsFiltersConfirmPauseAndReopen')
            command('shell', 'pm', 'revoke', PACKAGE, 'android.permission.READ_CALENDAR')
            check('revokedCalendarAndVisibleRestoreDoNotEnableLogging')
        elif suite == 'imports':
            check('prepareCalendarAndTaggedHistory')
            command('shell', 'pm', 'grant', PACKAGE, 'android.permission.READ_CALL_LOG')
            ui_attempted = True
            check('renderedRangePreviewCancelConfirmAndRepeat')
            check('preparePersistedPartialImport')
            check('renderedExplicitHistoryResumeCompletesPartialWork')
        else:
            check('preparePendingWithoutCallAccess')
            ui_attempted = True
            check('leavingAndReturningWithdrawsQueuedApproval')
            command('shell', 'pm', 'revoke', PACKAGE, 'android.permission.WRITE_CALENDAR')
            ui_attempted = True
            check('renderedPermissionCancelAndExplicitReapproval')
            command('shell', 'pm', 'revoke', PACKAGE, 'android.permission.WRITE_CALENDAR')
            check('renderedExistingCheckWithoutWriteAccess')
            command('shell', 'pm', 'grant', PACKAGE, 'android.permission.WRITE_CALENDAR')
            check('renderedEditDeletionAndSuppression')
            check('renderedExplicitRetentionClearsOnlyExpiredPayload')
        capture_ui()
        ui_captured = True
        check('cleanupFixture')
        fixture_clean = True
        receipt['result'] = 'passed'
    except Exception as error:
        receipt['result'] = 'failed'
        receipt['error'] = str(error)
    finally:
        if ui_attempted and not ui_captured:
            try:
                capture_ui()
                ui_captured = True
            except Exception as error:
                receipt['ui_capture_error'] = str(error)
        if environment is not None:
            receipt['emulator_restored'] = environment.restore()
        if fixture_possible and not fixture_clean:
            try:
                for permission in ('READ_CALENDAR', 'WRITE_CALENDAR'):
                    command('shell', 'pm', 'grant', PACKAGE, 'android.permission.' + permission)
                check('cleanupFixture')
                fixture_clean = True
            except Exception as error:
                receipt['cleanup_error'] = str(error)
        receipt['fixture_cleanup_verified'] = fixture_clean if fixture_possible else True
        # Preserve the cleanup instrumentation if fixture removal cannot be verified.
        if receipt['fixture_cleanup_verified'] and (not ui_attempted or ui_captured):
            removal_checks = []
            for package in reversed(installed):
                try:
                    # A failed/uncertain install may have left no package; absence is the gate.
                    command('uninstall', package)
                except Exception as error:
                    receipt.setdefault('uninstall_notes', []).append(str(error))
                try:
                    absent = 'package:' + package not in command('shell', 'pm', 'list', 'packages', package).splitlines()
                    removal_checks.append(absent)
                except Exception as error:
                    removal_checks.append(False)
                    receipt['cleanup_error'] = str(error)
            receipt['packages_removed'] = all(removal_checks)
        else:
            receipt['packages_removed'] = False
        if (not receipt['fixture_cleanup_verified'] or not receipt['packages_removed']
                or not receipt.get('emulator_restored', {'verified': True})['verified']):
            receipt['result'] = 'failed'
        ((receipt_dir or evidence) / ('parity-ui-receipt.json' if suite == 'settings' else 'parity-ui-' + suite + '-receipt.json')).write_text(json.dumps(receipt, indent=2) + '\n')
    print(json.dumps({k: receipt.get(k) for k in ('result', 'fixture_cleanup_verified', 'packages_removed', 'error')}))
    return 0 if receipt.get('result') == 'passed' else 1


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--serial', required=True)
    parser.add_argument('--suite', choices=('settings', 'imports', 'recovery', 'accessibility'), default='settings')
    parser.add_argument('--receipt-dir', type=Path, help='New per-run directory; refuse reuse to retain matrix evidence')
    args = parser.parse_args()
    raise SystemExit(qualify(args.serial, args.suite, args.receipt_dir))
