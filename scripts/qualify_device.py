#!/usr/bin/env python3
"""Run bounded, zero-row checks on a dedicated qualification app; clean up afterward.

Requires explicit --serial. Never configures an endpoint or sync and refuses to
replace an installed package. ADB grant is a development test, not UI-consent proof.
"""
import argparse
import hashlib
import json
from pathlib import Path
import subprocess

ROOT = Path(__file__).resolve().parents[1]
PACKAGE = 'org.calltrack.qualification'
TEST_PACKAGE = PACKAGE + '.test'
TEST_CLASS = 'com.markobl.calllogsync.PermissionQualificationTest'


def qualify(serial):
    adb = ['adb', '-s', serial]
    evidence = ROOT / '.local/build-evidence'
    checkout = ROOT / '.local/modern-qualification'
    apks = [checkout / 'app/build/outputs/apk/debug/app-debug.apk',
            checkout / 'app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk']
    for apk in apks:
        if not apk.is_file():
            raise RuntimeError(f'Missing built APK: {apk}')
    steps = []
    installed = []

    def command(*arguments, allow_failure=False):
        result = subprocess.run(adb + list(arguments), capture_output=True, text=True)
        steps.append({'command': ['adb', '-s', '<selected-device>'] + list(arguments),
                      'exit_code': result.returncode, 'output': result.stdout + result.stderr})
        if result.returncode and not allow_failure:
            raise RuntimeError('ADB command failed: ' + ' '.join(arguments))
        return result.stdout.strip()

    for package in (PACKAGE, TEST_PACKAGE):
        output = command('shell', 'pm', 'list', 'packages', package)
        if 'package:' + package in output.splitlines():
            raise RuntimeError('Refusing to replace pre-existing test package ' + package)
    build_receipt = json.loads((evidence / 'modern-clean-receipt.json').read_text())
    expected_artifacts = {entry['path']: entry['sha256'] for entry in build_receipt['artifacts']}
    if build_receipt['exit_code'] != 0:
        raise RuntimeError('Refusing device installation from an unsuccessful build')
    for apk in apks:
        if expected_artifacts.get(str(apk.relative_to(ROOT))) != hashlib.sha256(apk.read_bytes()).hexdigest():
            raise RuntimeError('APK differs from successful build receipt')
    aapt = ROOT / '.local/android-sdk/build-tools/35.0.0/aapt'
    for package, apk in zip((PACKAGE, TEST_PACKAGE), apks):
        identity = subprocess.check_output([str(aapt), 'dump', 'badging', str(apk)], text=True)
        if "package: name='" + package + "'" not in identity:
            raise RuntimeError('APK identity does not match test package ' + package)
    receipt = {'device': {}, 'apks': [{'name': apk.name, 'sha256': hashlib.sha256(apk.read_bytes()).hexdigest()} for apk in apks], 'steps': steps}
    try:
        receipt['device']['android'] = command('shell', 'getprop', 'ro.build.version.release')
        receipt['device']['sdk'] = command('shell', 'getprop', 'ro.build.version.sdk')
        for package, apk in zip((PACKAGE, TEST_PACKAGE), apks):
            if 'Success' not in command('install', str(apk)):
                raise RuntimeError('Install did not report success')
            installed.append(package)
        launch = command('shell', 'am', 'start', '-W', '-n', PACKAGE + '/com.markobl.calllogsync.MainActivity')
        if 'Status: ok' not in launch:
            raise RuntimeError('Activity launch did not report success')
        if not command('shell', 'pidof', PACKAGE):
            raise RuntimeError('No running app process after launch')

        def check(method):
            output = command('shell', 'am', 'instrument', '-w', '-r', '-e', 'class',
                             TEST_CLASS + '#' + method,
                             TEST_PACKAGE + '/androidx.test.runner.AndroidJUnitRunner')
            if 'OK (1 test)' not in output or 'FAILURES' in output or 'INSTRUMENTATION_FAILED' in output:
                raise RuntimeError('Instrumentation contract failed: ' + method)
        check('testDenied')
        command('shell', 'pm', 'grant', PACKAGE, 'android.permission.READ_CALL_LOG')
        check('testGranted')
        command('shell', 'pm', 'revoke', PACKAGE, 'android.permission.READ_CALL_LOG')
        check('testDenied')
        receipt['result'] = 'passed'
    except Exception as error:
        receipt['result'] = 'failed'
        receipt['error'] = str(error)
        raise
    finally:
        for package in reversed(installed):
            command('uninstall', package, allow_failure=True)
        cleanup_checks = []
        for package in installed:
            output = command('shell', 'pm', 'list', 'packages', package, allow_failure=True)
            cleanup_checks.append(steps[-1]['exit_code'] == 0 and 'package:' + package not in output.splitlines())
        receipt['cleanup_complete'] = len(installed) == 2 and all(cleanup_checks)
        if not receipt['cleanup_complete']:
            receipt['result'] = 'failed'
            receipt['cleanup_error'] = 'Test-package absence was not verified'
        evidence.mkdir(parents=True, exist_ok=True)
        (evidence / 'device-receipt.json').write_text(json.dumps(receipt, indent=2) + '\n')
        print(json.dumps({'result': receipt.get('result'), 'cleanup_complete': receipt['cleanup_complete']}))
        if not receipt['cleanup_complete']:
            raise RuntimeError('Test-package cleanup not verified; inspect device receipt')


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--serial', required=True)
    qualify(parser.parse_args().serial)
