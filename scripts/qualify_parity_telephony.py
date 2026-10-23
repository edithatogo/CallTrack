#!/usr/bin/env python3
"""Dedicated-emulator real CallLog/CalendarContract and protected telephony wake acceptance.

Requires selected device, matching build receipt and absent test packages. On native
failure attempts scoped fixture recovery BEFORE uninstalling its cleanup code.
"""
from parity_targets import is_dedicated_avd
from parity_health import require_healthy_boot
import argparse
import hashlib
import json
import re
import shlex
import time
import xml.etree.ElementTree as ET
from pathlib import Path
import subprocess
import uuid
from run_slice_build import inputs

ROOT = Path(__file__).resolve().parents[1]
PACKAGE = 'org.calltrack.community.dev'
TEST_PACKAGE = PACKAGE + '.test'
TEST_CLASS = 'com.markobl.calllogsync.slice.TelephonyProviderTest'
NUMBER = '+12025550109'


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
    call_attempted = False

    def command(*args):
        result = subprocess.run([str(ROOT / '.local/android-sdk/platform-tools/adb'), '-s', serial, *args],
                                capture_output=True, text=True, timeout=180)
        receipt['steps'].append({'args': list(args), 'exit_code': result.returncode,
                                 'output': result.stdout + result.stderr})
        if result.returncode:
            raise RuntimeError('ADB command failed: ' + ' '.join(args))
        return result.stdout.strip()

    def check(method):
        output = command('shell', 'am', 'instrument', '-w', '-r', '-e', 'fixtureToken', receipt['fixture_token'],
                         '-e', 'class', TEST_CLASS + '#' + method,
                         TEST_PACKAGE + '/androidx.test.runner.AndroidJUnitRunner')
        if 'OK (1 test)' not in output or 'FAILURES' in output or 'INSTRUMENTATION_FAILED' in output:
            raise RuntimeError('Native acceptance failed: ' + method)

    def phone_states():
        text = command('shell', 'dumpsys', 'telephony.registry')
        states = [int(v) for v in re.findall(r'^\s*mCallState=(\d+)', text, re.MULTILINE)]
        if not states:
            raise RuntimeError('Android telephony state unavailable')
        return states

    def wait_phone_state(expected):
        for _ in range(40):
            states = phone_states()
            if (expected == 0 and all(v == 0 for v in states)) or (expected == 1 and 1 in states):
                return
            time.sleep(0.25)
        raise RuntimeError('Android telephony state not verified: ' + str(expected))

    if command('shell', 'getprop', 'ro.kernel.qemu') != '1':
        raise RuntimeError('Not an emulator')
    if not is_dedicated_avd(command('emu', 'avd', 'name')):
        raise RuntimeError('Not the dedicated CallTrack AVD')
    require_healthy_boot(command)
    for package in (PACKAGE, TEST_PACKAGE):
        if 'package:' + package in command('shell', 'pm', 'list', 'packages', package).splitlines():
            raise RuntimeError('Refusing to replace existing package ' + package)
    try:
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
        if any(v != 0 for v in phone_states()) or command('emu', 'gsm', 'list').strip() != 'OK':
            raise RuntimeError('Refusing a guest with an existing telephony call')
        check('prepareEmptyProviderAndCalendar')
        for permission in ('READ_CALL_LOG', 'READ_PHONE_STATE'):
            command('shell', 'pm', 'grant', PACKAGE, 'android.permission.' + permission)
        check('armPolicyAndRemoveInitialWakes')
        state_path = 'shared_prefs/org.calltrack.community.dev.parity.' + receipt['fixture_token'] + '-telephony.xml'
        tree = ET.fromstring(command('shell', 'run-as', PACKAGE, 'cat', state_path))
        calendar = next(int(x.get('value')) for x in tree if x.get('name') == 'calendar')
        source_at = next(int(x.get('value')) for x in tree if x.get('name') == 'sourceAt')
        call_attempted = True  # Unknown console outcome may still have created the call.
        if 'OK' not in command('emu', 'gsm', 'call', NUMBER):
            raise RuntimeError('Inbound call creation not acknowledged')
        wait_phone_state(1)
        receipt['android_ringing_observed'] = True
        time.sleep(3)
        ended_at = int(command('shell', 'date', '+%s')) * 1000
        if 'OK' not in command('emu', 'gsm', 'cancel', NUMBER):
            raise RuntimeError('Synthetic call end not acknowledged')
        wait_phone_state(0)
        receipt['android_idle_observed'] = True
        receipt['call_end_guest_at'] = ended_at
        # No Activity launch, instrumentation or direct enqueue before background effect/status.
        deadline = time.monotonic() + 180
        while time.monotonic() < deadline:
            events = command('shell', 'content', 'query', '--uri', 'content://com.android.calendar/events',
                             '--projection', '_id', '--where', 'calendar_id=' + str(calendar))
            xml = ET.fromstring(command('shell', 'run-as', PACKAGE, 'cat', 'shared_prefs/slice.xml'))
            values = {x.get('name'): x.get('value') if x.tag != 'string' else x.text for x in xml}
            if (len(re.findall(r'Row: \d+', events)) == 1
                    and values.get('automaticStatusCode') == 'COMPLETE'
                    and int(values.get('automaticStatusAt', '0')) >= ended_at):
                break
            time.sleep(1)
        else:
            raise RuntimeError('No verified background effect/status after actual telephony end')
        receipt['background_observed_before_verifier'] = True
        receipt['broadcast_history'] = command('shell', 'dumpsys', 'activity', 'broadcasts')
        check('verifyRealMissedCallAndUniqueReceipt')
        check('repeatCatchUpDoesNotDuplicate')
        check('cleanupFixture')
        fixture_clean = True
        receipt['result'] = 'passed'
    except Exception as error:
        receipt['result'] = 'failed'
        receipt['error'] = str(error)
    finally:
        if call_attempted:
            try:
                # New modem simulator can return an empty console list while Android rings.
                command('emu', 'gsm', 'cancel', NUMBER)
                wait_phone_state(0)
                if not fixture_clean:
                    where = "number IN ('" + NUMBER + "','" + NUMBER[1:] + "') AND date>=" + str(source_at)
                    query = shlex.join(['content', 'query', '--uri', 'content://call_log/calls',
                                       '--projection', '_id:number:type:date', '--where', where])
                    for _ in range(40):
                        rows = command('shell', query)
                        if len(re.findall(r'Row: \d+', rows)) == 1:
                            receipt['actual_call_row_before_cleanup'] = rows
                            break
                        time.sleep(0.25)
                    else:
                        raise RuntimeError('Cannot verify exact fixture call row before cleanup')
            except Exception as error:
                receipt['call_cleanup_error'] = str(error)
                fixture_clean = False
        if fixture_possible and not fixture_clean:
            try:
                for permission in ('READ_CALENDAR', 'WRITE_CALENDAR'):
                    command('shell', 'pm', 'grant', PACKAGE, 'android.permission.' + permission)
                check('cleanupFixture')
                fixture_clean = True
            except Exception as error:
                receipt['cleanup_error'] = str(error)
        receipt['fixture_cleanup_verified'] = (fixture_clean if fixture_possible else True) and not receipt.get('call_cleanup_error')
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
        if not receipt['fixture_cleanup_verified'] or not receipt['packages_removed'] or receipt.get('call_cleanup_error'):
            receipt['result'] = 'failed'
        (evidence / 'parity-telephony-receipt.json').write_text(json.dumps(receipt, indent=2) + '\n')
    print(json.dumps({k: receipt.get(k) for k in ('result', 'fixture_cleanup_verified', 'packages_removed', 'error')}))
    return 0 if receipt.get('result') == 'passed' else 1


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--serial', required=True)
    raise SystemExit(qualify(parser.parse_args().serial))
