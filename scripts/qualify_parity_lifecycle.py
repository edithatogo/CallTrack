#!/usr/bin/env python3
"""Dedicated-emulator launcher, reboot, offline and Doze recovery acceptance.

Requires selected device, matching build receipt and absent test packages. On native
failure attempts scoped fixture recovery BEFORE uninstalling its cleanup code.
"""
from parity_targets import is_dedicated_avd
import argparse
import hashlib
import json
import re
import time
import xml.etree.ElementTree as ET
from parity_environment import EmulatorEnvironment
from pathlib import Path
import subprocess
import uuid
from run_slice_build import inputs

ROOT = Path(__file__).resolve().parents[1]
PACKAGE = 'org.calltrack.community.dev'
TEST_PACKAGE = PACKAGE + '.test'
TEST_CLASS = 'com.markobl.calllogsync.slice.LifecycleProviderTest'


def qualify(serial):
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
    receipt = {'fixture_token': uuid.uuid4().hex, 'steps': [], 'artifacts': build['artifacts'], 'inputs': build['inputs']}
    installed = []
    fixture_possible = False
    fixture_clean = False
    environment = None
    metadata = {}

    def command(*args):
        started = time.monotonic()
        result = subprocess.run([str(ROOT / '.local/android-sdk/platform-tools/adb'), '-s', serial, *args],
                                capture_output=True, text=True, timeout=180 if 'instrument' in args else 45)
        shown = result.stdout + result.stderr
        if args == ('shell', 'dumpsys', 'power'):
            shown = '\n'.join(line for line in shown.splitlines() if 'mWakefulness=' in line)
        elif args == ('shell', 'cmd', 'wifi', 'status'):
            shown = shown.splitlines()[0] if shown.splitlines() else shown
        elif args[:4] == ('exec-out', 'run-as', PACKAGE, 'cat'):
            try:
                shown = json.dumps({node.attrib.get('name'): node.attrib.get('value', node.text)
                                    for node in ET.fromstring(result.stdout)
                                    if node.attrib.get('name', '').startswith('automaticStatus')})
            except ET.ParseError:
                shown = '<transient preference-file read>'
        receipt['steps'].append({'args': list(args), 'exit_code': result.returncode, 'output': shown,
                                 'host_monotonic_started': started, 'elapsed_seconds': time.monotonic() - started})
        if result.returncode:
            raise RuntimeError('ADB command failed: ' + ' '.join(args))
        return result.stdout.strip()

    def check(method):
        output = command('shell', 'am', 'instrument', '-w', '-r', '-e', 'fixtureToken', receipt['fixture_token'],
                         '-e', 'class', TEST_CLASS + '#' + method,
                         TEST_PACKAGE + '/androidx.test.runner.AndroidJUnitRunner')
        for key, value in re.findall(r'INSTRUMENTATION_STATUS: (fixtureCalendar|expectedEvents|fixtureSourceAt)=(\d+)', output):
            metadata[key] = int(value)
        if 'OK (1 test)' not in output or 'FAILURES' in output or 'INSTRUMENTATION_FAILED' in output:
            raise RuntimeError('Native acceptance failed: ' + method)

    def event_count():
        calendar = metadata['fixtureCalendar']
        output = command('shell', 'content', 'query', '--uri', 'content://com.android.calendar/events',
                         '--projection', '_id', '--where', 'calendar_id=' + str(calendar))
        if output == 'No result found.':
            return 0
        if not output.startswith('Row:'):
            raise RuntimeError('Scoped calendar read failed')
        return len(re.findall(r'^Row:', output, re.MULTILINE))

    def wait_effect_and_status(timeout=90):
        deadline = time.monotonic() + timeout
        while time.monotonic() < deadline:
            count = event_count()
            if count > metadata['expectedEvents']:
                raise RuntimeError('Unexpected duplicate calendar effect')
            if count == metadata['expectedEvents']:
                try:
                    encoded = command('exec-out', 'run-as', PACKAGE, 'cat', 'shared_prefs/slice.xml')
                    values = {node.attrib.get('name'): node.attrib.get('value', node.text)
                              for node in ET.fromstring(encoded)}
                    status = {key: value for key, value in values.items() if key.startswith('automaticStatus')}
                    # Keep nonpersonal execution fields, not the complete preferences document.
                    receipt['steps'][-1]['output'] = json.dumps(status)
                    if (status.get('automaticStatusCode') == 'COMPLETE'
                            and int(status.get('automaticStatusAt', '0')) >= metadata['fixtureSourceAt']):
                        return
                except (ET.ParseError, ValueError, RuntimeError):
                    pass  # Atomic preference replacement may briefly make direct disk observation unavailable.
            time.sleep(1)
        raise RuntimeError('Verified event/status did not appear within the observation window')

    def launch():
        output = command('shell', 'am', 'start', '-W', '-n', PACKAGE + '/com.markobl.calllogsync.slice.CalendarSliceActivity')
        if 'Status: ok' not in output:
            raise RuntimeError('Launcher reopen not confirmed')

    def terminate_held_insertion():
        expected = metadata['expectedEvents'] + 1
        args = ['shell', 'am', 'instrument', '-w', '-r', '-e', 'fixtureToken', receipt['fixture_token'],
                '-e', 'class', TEST_CLASS + '#insertAndWaitBeforeReturningProviderId',
                TEST_PACKAGE + '/androidx.test.runner.AndroidJUnitRunner']
        process = subprocess.Popen([str(ROOT / '.local/android-sdk/platform-tools/adb'), '-s', serial, *args],
                                   stdout=subprocess.PIPE, stderr=subprocess.STDOUT, text=True)
        terminated = False
        try:
            deadline = time.monotonic() + 40
            while time.monotonic() < deadline:
                if process.poll() is not None:
                    raise RuntimeError('Held insertion exited before host termination')
                if event_count() == expected:
                    if PACKAGE not in command('shell', 'ps', '-A', '-o', 'NAME').splitlines():
                        raise RuntimeError('Held application process disappeared before host termination')
                    command('shell', 'am', 'force-stop', PACKAGE)
                    if PACKAGE in command('shell', 'ps', '-A', '-o', 'NAME').splitlines():
                        raise RuntimeError('Held application process survived force-stop')
                    terminated = True
                    break
                time.sleep(0.25)
            if not terminated:
                raise RuntimeError('Held insertion effect not observed')
            output, _ = process.communicate(timeout=30)
            receipt['steps'].append({'args': args, 'exit_code': process.returncode,
                                     'output': output, 'expected_process_termination': True})
            for key, value in re.findall(r'INSTRUMENTATION_STATUS: (fixtureCalendar|expectedEvents|fixtureSourceAt)=(\d+)', output):
                metadata[key] = int(value)
            if metadata['expectedEvents'] != expected:
                raise RuntimeError('Held insertion metadata was not committed before process termination')
            receipt['held_process_termination_verified'] = True
        finally:
            if process.poll() is None:
                command('shell', 'am', 'force-stop', PACKAGE)
                try:
                    process.communicate(timeout=15)
                except subprocess.TimeoutExpired:
                    process.terminate()
                    process.communicate(timeout=15)

    if command('shell', 'getprop', 'ro.kernel.qemu') != '1':
        raise RuntimeError('Not an emulator')
    if not is_dedicated_avd(command('emu', 'avd', 'name')):
        raise RuntimeError('Not the dedicated CallTrack AVD')
    if command('shell', 'getprop', 'sys.boot_completed') != '1':
        raise RuntimeError('Boot incomplete')
    for package in (PACKAGE, TEST_PACKAGE):
        if 'package:' + package in command('shell', 'pm', 'list', 'packages', package).splitlines():
            raise RuntimeError('Refusing to replace existing package ' + package)
    try:
        environment = EmulatorEnvironment(command)
        receipt['emulator_original'] = environment.snapshot().copy()
        receipt['android'] = command('shell', 'getprop', 'ro.build.version.release')
        receipt['sdk'] = command('shell', 'getprop', 'ro.build.version.sdk')
        for package, apk in zip((PACKAGE, TEST_PACKAGE), apks):
            # A timeout is an unknown outcome, not evidence that installation did not occur.
            installed.append(package)
            if 'Success' not in command('install', str(apk)):
                raise RuntimeError('Installation not confirmed')
        for permission in ('READ_CALENDAR', 'WRITE_CALENDAR'):
            command('shell', 'pm', 'grant', PACKAGE, 'android.permission.' + permission)
        fixture_possible = True
        check('prepareReopenFixtureWithoutScheduledWork')
        command('shell', 'pm', 'grant', PACKAGE, 'android.permission.READ_CALL_LOG')
        command('shell', 'am', 'force-stop', PACKAGE)
        if PACKAGE in command('shell', 'ps', '-A', '-o', 'NAME').splitlines():
            raise RuntimeError('Force-stop left the app process running')
        launch()
        wait_effect_and_status()
        check('verifyExpectedDeliveryAndReceipt')
        check('launcherResumeWithoutSystemWakeRequestsRecovery')
        # Actual death after CalendarContract insertion and before the returned ID is saved.
        terminate_held_insertion()
        launch()
        wait_effect_and_status()
        check('verifyExpectedDeliveryAndReceipt')
        check('prepareNextCallWithoutWake')
        before_boot = command('shell', 'cat', '/proc/sys/kernel/random/boot_id')
        command('reboot')
        # Offline ADB is expected during reboot; include connection recovery in the boot budget.
        boot_deadline = time.monotonic() + 180
        while time.monotonic() < boot_deadline:
            try:
                if (command('shell', 'getprop', 'sys.boot_completed') == '1'
                        and command('shell', 'cat', '/proc/sys/kernel/random/boot_id') != before_boot):
                    break
            except RuntimeError:
                pass
            time.sleep(2)
        else:
            raise RuntimeError('Reboot completion not verified')
        receipt['reboot_verified'] = True
        receipt['boot_observation_window_seconds'] = 180
        wait_effect_and_status(timeout=180)  # No app launch or instrumentation before actual boot recovery.
        check('verifyExpectedDeliveryAndReceipt')
        check('prepareNextCallWithoutWake')
        environment.offline()
        check('requestOfflineWakeAndVerifyDelivery')
        receipt['offline_restore'] = environment.restore()
        if not receipt['offline_restore']['verified']:
            raise RuntimeError('Offline environment restoration failed')
        check('prepareDelayedCallForDoze')
        command('shell', 'input', 'keyevent', 'KEYCODE_HOME')
        environment.doze()
        # Delay exceeds the staged request's30-second eligibility, without starting the app.
        hold_until = time.monotonic() + 40
        while time.monotonic() < hold_until:
            if event_count() != metadata['expectedEvents'] - 1:
                raise RuntimeError('Doze request delivered before deferred-recovery observation')
            time.sleep(2)
        if command('shell', 'dumpsys', 'deviceidle', 'get', 'deep') != 'IDLE':
            raise RuntimeError('Deep idle ended before deferral observation')
        receipt['doze_deferral_verified'] = True
        receipt['doze_restore'] = environment.restore()
        if not receipt['doze_restore']['verified']:
            raise RuntimeError('Doze environment restoration failed')
        wait_effect_and_status()  # Observe effect before starting verification instrumentation.
        check('verifyExpectedDeliveryAndReceipt')
        check('cleanupFixture')
        fixture_clean = True
        receipt['result'] = 'passed'
    except Exception as error:
        receipt['result'] = 'failed'
        receipt['error'] = str(error)
        # Observe scheduler/permission/status state before cleanup changes the failed condition.
        if fixture_possible:
            for args in [('shell', 'dumpsys', 'activity', 'exit-info', PACKAGE),
                         ('shell', 'dumpsys', 'meminfo', PACKAGE),
                         ('shell', 'dumpsys', 'package', PACKAGE),
                         ('shell', 'dumpsys', 'jobscheduler', PACKAGE),
                         ('shell', 'cmd', 'appops', 'get', PACKAGE, 'READ_CALL_LOG'),
                         ('exec-out', 'run-as', PACKAGE, 'cat', 'shared_prefs/slice.xml')]:
                try:
                    command(*args)
                except Exception as diagnostic_error:
                    receipt.setdefault('diagnostic_errors', []).append(str(diagnostic_error))
    finally:
        if environment is not None:
            receipt['emulator_restored'] = environment.restore()
            if not receipt['emulator_restored']['verified']:
                receipt['result'] = 'failed'
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
        if receipt['fixture_cleanup_verified']:
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
        (evidence / 'parity-lifecycle-receipt.json').write_text(json.dumps(receipt, indent=2) + '\n')
    print(json.dumps({k: receipt.get(k) for k in ('result', 'fixture_cleanup_verified', 'packages_removed', 'error')}))
    return 0 if receipt.get('result') == 'passed' else 1


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--serial', required=True)
    raise SystemExit(qualify(parser.parse_args().serial))
