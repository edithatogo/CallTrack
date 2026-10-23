#!/usr/bin/env -S uv run python
"""Build or qualify the isolated synthetic Compose app; never use a physical device."""
import argparse
import hashlib
import json
import os
from pathlib import Path
import re
import subprocess

ROOT = Path(__file__).resolve().parents[1]
PROJECT = ROOT / 'prototypes/compose'
EVIDENCE = ROOT / '.local/compose-evidence'
PACKAGE = 'org.calltrack.community.composeprototype'
TEST_PACKAGE = PACKAGE + '.test'
AVD = 'calltrack-parity-api36-20261020'


def digest(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()


def inputs():
    paths = [ROOT / 'gradlew', ROOT / 'scripts/qualify_compose_prototype.py']
    paths += list((ROOT / 'gradle/wrapper').glob('*'))
    paths += [p for p in PROJECT.rglob('*') if p.is_file()
              and not {'build', '.gradle', '.kotlin'} & set(p.relative_to(PROJECT).parts)]
    return {str(p.relative_to(ROOT)): digest(p) for p in sorted(paths)}


def build(jdk, sdk):
    EVIDENCE.mkdir(parents=True, exist_ok=True)
    before = inputs()
    command = ['sh', 'gradlew', '-p', 'prototypes/compose', '--no-daemon', '--console=plain',
               'testDebugUnitTest', 'assembleDebug', 'assembleDebugAndroidTest', 'lintDebug',
               'dependencies', '--configuration', 'debugRuntimeClasspath']
    with (EVIDENCE / 'build.log').open('w') as log:
        result = subprocess.run(command, cwd=ROOT, env=dict(os.environ, JAVA_HOME=str(jdk),
                                ANDROID_HOME=str(sdk)), stdout=log, stderr=subprocess.STDOUT)
    receipt = {'exit_code': result.returncode, 'inputs_unchanged': before == inputs(),
               'inputs': before, 'command': command,
               'artifacts': {str(p.relative_to(ROOT)): digest(p)
                             for p in sorted((PROJECT / 'build/outputs/apk').rglob('*.apk'))}}
    (EVIDENCE / 'build-receipt.json').write_text(json.dumps(receipt, indent=2) + '\n')
    print(json.dumps({k: receipt[k] for k in ('exit_code', 'inputs_unchanged')}))
    return 0 if not result.returncode and receipt['inputs_unchanged'] else 1


def qualify(serial):
    if not re.fullmatch(r'emulator-\d+', serial):
        raise RuntimeError('Physical devices are not permitted for this prototype')
    receipt = json.loads((EVIDENCE / 'build-receipt.json').read_text())
    if receipt['exit_code'] or not receipt['inputs_unchanged'] or receipt['inputs'] != inputs():
        raise RuntimeError('Successful prototype build does not match current inputs')
    apks = [PROJECT / 'build/outputs/apk/debug/compose-prototype-debug.apk',
            PROJECT / 'build/outputs/apk/androidTest/debug/compose-prototype-debug-androidTest.apk']
    for apk in apks:
        if receipt['artifacts'].get(str(apk.relative_to(ROOT))) != digest(apk):
            raise RuntimeError('Prototype artifact differs from build receipt')
    state = {'inputs': receipt['inputs'], 'artifacts': receipt['artifacts'], 'steps': [],
             'result': 'not-started', 'packages_removed': False}
    attempted = []

    def persist():
        (EVIDENCE / 'device-receipt.json').write_text(json.dumps(state, indent=2) + '\n')

    def adb(*args):
        result = subprocess.run([str(ROOT / '.local/android-sdk/platform-tools/adb'), '-s', serial, *args],
                                capture_output=True, text=True, timeout=180)
        state['steps'].append({'args': list(args), 'exit_code': result.returncode,
                               'output': result.stdout + result.stderr})
        persist()
        if result.returncode:
            raise RuntimeError('ADB command failed')
        return result.stdout.strip()

    # All gates precede installation, and there is no default/fallback device.
    if adb('shell', 'getprop', 'ro.kernel.qemu') != '1':
        raise RuntimeError('Target is not an emulator')
    if AVD not in adb('emu', 'avd', 'name').splitlines():
        raise RuntimeError('Target is not the dedicated AVD')
    if adb('shell', 'getprop', 'sys.boot_completed') != '1':
        raise RuntimeError('Emulator has not finished booting')
    for package, apk in zip((PACKAGE, TEST_PACKAGE), apks):
        identity = subprocess.check_output([str(ROOT / '.local/android-sdk/build-tools/35.0.0/aapt'),
                                           'dump', 'badging', str(apk)], text=True)
        if "package: name='" + package + "'" not in identity:
            raise RuntimeError('Unexpected package identity')
        if package == PACKAGE:
            forbidden = {'android.permission.READ_CALL_LOG', 'android.permission.WRITE_CALL_LOG',
                         'android.permission.READ_CALENDAR', 'android.permission.WRITE_CALENDAR',
                         'android.permission.READ_CONTACTS', 'android.permission.RECORD_AUDIO',
                         'android.permission.INTERNET'}
            declared = set(re.findall(r"uses-permission(?:-sdk-23)?: name='([^']+)'", identity))
            if declared & forbidden:
                raise RuntimeError('Prototype requests a forbidden permission')
        if 'package:' + package in adb('shell', 'pm', 'list', 'packages', package).splitlines():
            raise RuntimeError('Refusing to replace existing prototype package')
    try:
        state['sdk'] = adb('shell', 'getprop', 'ro.build.version.sdk')
        if int(state['sdk']) < 34:
            raise RuntimeError('Accessibility experiment requires API34 or newer')
        for package, apk in zip((PACKAGE, TEST_PACKAGE), apks):
            attempted.append(package)
            state['attempted_packages'] = list(attempted)
            persist()
            if 'Success' not in adb('install', str(apk)):
                raise RuntimeError('Installation not confirmed')
        classes = ','.join('org.calltrack.composeprototype.' + name
                           for name in ('PrototypeScreenTest', 'PrototypeLayoutTest'))
        output = adb('shell', 'am', 'instrument', '-w', '-r', '-e', 'class', classes,
                     TEST_PACKAGE + '/androidx.test.runner.AndroidJUnitRunner')
        if 'OK (5 tests)' not in output or 'FAILURES' in output or 'INSTRUMENTATION_FAILED' in output:
            raise RuntimeError('Compose native acceptance failed')
        shot = subprocess.run([str(ROOT / '.local/android-sdk/platform-tools/adb'), '-s', serial,
                               'exec-out', 'run-as', PACKAGE, 'cat', 'files/compose-prototype.png'],
                              capture_output=True, timeout=30)
        if shot.returncode or not shot.stdout.startswith(b'\x89PNG\r\n\x1a\n'):
            raise RuntimeError('Synthetic screenshot readback failed')
        (EVIDENCE / 'prototype.png').write_bytes(shot.stdout)
        state['screenshot_sha256'] = hashlib.sha256(shot.stdout).hexdigest()
        state['result'] = 'passed'
    except Exception as error:
        state['result'] = 'failed'
        state['error'] = str(error)
    finally:
        removed = []
        for package in reversed(attempted):
            try:
                adb('uninstall', package)
            except Exception as error:
                state.setdefault('cleanup_errors', []).append(str(error))
            try:
                removed.append('package:' + package not in adb('shell', 'pm', 'list', 'packages', package).splitlines())
            except Exception:
                removed.append(False)
        state['packages_removed'] = all(removed)
        if not state['packages_removed']:
            state['result'] = 'failed'
        persist()
    print(json.dumps({k: state.get(k) for k in ('result', 'packages_removed', 'error')}))
    return 0 if state['result'] == 'passed' else 1


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    sub = parser.add_subparsers(dest='mode', required=True)
    builder = sub.add_parser('build')
    builder.add_argument('--jdk', type=Path, required=True)
    builder.add_argument('--sdk', type=Path, required=True)
    device = sub.add_parser('device')
    device.add_argument('--serial', required=True)
    args = parser.parse_args()
    raise SystemExit(build(args.jdk.resolve(), args.sdk.resolve()) if args.mode == 'build' else qualify(args.serial))
