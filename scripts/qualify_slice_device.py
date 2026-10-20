#!/usr/bin/env python3
"""Synthetic local-calendar acceptance; no calls, personal call queries or cloud sync.

Requires selected device, matching build receipt and absent test packages. On native
failure attempts scoped fixture recovery BEFORE uninstalling its cleanup code.
"""
import argparse
import hashlib
import json
from pathlib import Path
import subprocess
import uuid
from run_slice_build import inputs

ROOT = Path(__file__).resolve().parents[1]
PACKAGE = 'org.calltrack.community.dev'
TEST_PACKAGE = PACKAGE + '.test'
TEST_CLASS = 'com.markobl.calllogsync.slice.CalendarSliceTest'


def qualify(serial):
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
    receipt = {'fixture_token': uuid.uuid4().hex, 'steps': [], 'artifacts': build['artifacts']}
    installed = []
    fixture_possible = False
    fixture_clean = False

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

    for package in (PACKAGE, TEST_PACKAGE):
        if 'package:' + package in command('shell', 'pm', 'list', 'packages', package).splitlines():
            raise RuntimeError('Refusing to replace existing package ' + package)
    try:
        receipt['android'] = command('shell', 'getprop', 'ro.build.version.release')
        receipt['sdk'] = command('shell', 'getprop', 'ro.build.version.sdk')
        for package, apk in zip((PACKAGE, TEST_PACKAGE), apks):
            if 'Success' not in command('install', str(apk)):
                raise RuntimeError('Installation not confirmed')
            installed.append(package)
        launch = command('shell', 'am', 'start', '-W', '-n', PACKAGE + '/com.markobl.calllogsync.slice.CalendarSliceActivity')
        if 'Status: ok' not in launch or not command('shell', 'pidof', PACKAGE):
            raise RuntimeError('Cold launch not confirmed')
        check('deniedCalendarPermission')
        check('syntheticCursorMapsPresentationWithoutPersonalQuery')
        for permission in ('READ_CALENDAR', 'WRITE_CALENDAR'):
            command('shell', 'pm', 'grant', PACKAGE, 'android.permission.' + permission)
        fixture_possible = True
        check('nativeRoundtripAndReopenedLedger')
        check('cleanupFixture')
        fixture_clean = True
        for permission in ('READ_CALENDAR', 'WRITE_CALENDAR'):
            command('shell', 'pm', 'revoke', PACKAGE, 'android.permission.' + permission)
        check('deniedCalendarPermission')
        receipt['result'] = 'passed'
    except Exception as error:
        receipt['result'] = 'failed'
        receipt['error'] = str(error)
    finally:
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
            try:
                for package in reversed(installed):
                    if 'Success' not in command('uninstall', package):
                        raise RuntimeError('Uninstall not confirmed')
                receipt['packages_removed'] = all('package:' + p not in command('shell', 'pm', 'list', 'packages', p).splitlines() for p in installed)
            except Exception as error:
                receipt['packages_removed'] = False
                receipt['cleanup_error'] = str(error)
        else:
            receipt['packages_removed'] = False
        if not receipt['fixture_cleanup_verified'] or not receipt['packages_removed']:
            receipt['result'] = 'failed'
        (evidence / 'device-receipt.json').write_text(json.dumps(receipt, indent=2) + '\n')
    print(json.dumps({k: receipt.get(k) for k in ('result', 'fixture_cleanup_verified', 'packages_removed', 'error')}))
    return 0 if receipt.get('result') == 'passed' else 1


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--serial', required=True)
    raise SystemExit(qualify(parser.parse_args().serial))
