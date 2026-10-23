#!/usr/bin/env python3
"""Private-state development APK replacement on the authorised dedicated emulator.

No version-increase, production-signing, provider, cloud or physical-device proof.
"""
import argparse
import hashlib
import json
import os
from pathlib import Path
import re
import subprocess
import uuid
from parity_targets import is_dedicated_avd
from run_slice_build import inputs

ROOT = Path(__file__).resolve().parents[1]
PACKAGE = 'org.calltrack.community.dev'
TEST_PACKAGE = PACKAGE + '.test'
BASELINE_COMMIT = 'c7c3d919963d5225a09fa9c0ba81b2d3c1293f77'


def sha(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()


def require_successful_receipt(receipt):
    if receipt.get('exit_code') != 0 or receipt.get('inputs_unchanged') is not True:
        raise RuntimeError('A successful unchanged build receipt is required')


def qualify(serial):
    if not re.fullmatch(r'emulator-\d+', serial):
        raise RuntimeError('Update harness refuses physical-device serials')
    baseline_dir = ROOT / '.local/kotlin-evidence/expanded-java-baseline'
    baseline = json.loads((baseline_dir / 'build-receipt.json').read_text())
    current = json.loads((ROOT / '.local/slice-evidence/build-receipt.json').read_text())
    for receipt in (baseline, current):
        require_successful_receipt(receipt)
    if current['inputs'] != inputs():
        raise RuntimeError('Current build does not match current source')
    baseline_paths = subprocess.check_output(
        ['git', 'ls-tree', '-r', '--name-only', BASELINE_COMMIT, '--',
         'build.gradle', 'settings.gradle', 'gradle.properties', 'gradlew', 'gradle/wrapper',
         'app/build.gradle', 'app/proguard-rules.pro', 'app/src'], cwd=ROOT, text=True).splitlines()
    if set(baseline['inputs']) != set(baseline_paths):
        raise RuntimeError('Baseline receipt omits frozen source inputs')
    # Validate the retained receipt against a committed pre-conversion source, not a label.
    for path, expected in baseline['inputs'].items():
        blob = subprocess.check_output(['git', 'show', BASELINE_COMMIT + ':' + path], cwd=ROOT)
        if hashlib.sha256(blob).hexdigest() != expected:
            raise RuntimeError('Baseline inputs differ from frozen Java commit: ' + path)
    original = baseline_dir / 'app-debug.apk'
    candidate = ROOT / 'app/build/outputs/apk/debug/app-debug.apk'
    tests = ROOT / 'app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk'
    if baseline['artifacts'].get('app/build/outputs/apk/debug/app-debug.apk') != sha(original):
        raise RuntimeError('Baseline APK hash mismatch')
    for apk in (candidate, tests):
        if current['artifacts'].get(str(apk.relative_to(ROOT))) != sha(apk):
            raise RuntimeError('Candidate APK hash mismatch')
    sdk = ROOT / '.local/android-sdk'
    jdk = '/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home'
    env = dict(os.environ, JAVA_HOME=jdk)
    certs = []
    versions = []
    for apk, package in ((original, PACKAGE), (candidate, PACKAGE), (tests, TEST_PACKAGE)):
        badging = subprocess.check_output([str(sdk / 'build-tools/35.0.0/aapt'), 'dump', 'badging', str(apk)], text=True)
        if "package: name='" + package + "'" not in badging:
            raise RuntimeError('Unexpected package')
        if package == PACKAGE:
            version = re.search(r"versionCode='(\d+)'", badging)
            if version is None:
                raise RuntimeError('Package version missing')
            versions.append(version.group(1))
        signature = subprocess.check_output([str(sdk / 'build-tools/35.0.0/apksigner'), 'verify', '--print-certs', str(apk)], env=env, text=True)
        found = re.findall(r'Signer #\d+ certificate SHA-256 digest: ([a-f0-9]{64})', signature)
        if len(found) != 1:
            raise RuntimeError('Expected one verified signer')
        certs.append(found[0])
    if len(set(versions)) != 1:
        raise RuntimeError('This fixture requires same-version development packages')
    if len(set(certs)) != 1:
        raise RuntimeError('Baseline/candidate/instrumentation signer mismatch')
    receipt = dict(scope='same-version-development-package-replacement-private-state-only',
                   fixture_token=uuid.uuid4().hex, baseline_commit=BASELINE_COMMIT,
                   baseline_inputs=baseline['inputs'], candidate_inputs=current['inputs'],
                   artifacts={str(p): sha(p) for p in (original, candidate, tests)},
                   version_code=versions[0], signer_sha256=certs[0], steps=[], result='failed', packages_removed=False)
    attempted = []
    out = ROOT / '.local/kotlin-evidence'
    out.mkdir(parents=True, exist_ok=True)
    output = out / ('package-update-' + receipt['fixture_token'] + '.json')

    def command(*args):
        result = subprocess.run([str(sdk / 'platform-tools/adb'), '-s', serial, *args],
                                capture_output=True, text=True, timeout=180)
        receipt['steps'].append(dict(args=list(args), exit_code=result.returncode,
                                    output=result.stdout + result.stderr))
        if result.returncode:
            raise RuntimeError('ADB command failed: ' + ' '.join(args))
        return result.stdout.strip()

    def native(method):
        output = command('shell', 'am', 'instrument', '-w', '-r', '-e', 'fixtureToken', receipt['fixture_token'],
                         '-e', 'class', 'com.markobl.calllogsync.slice.KotlinPackageUpdateTest#' + method,
                         TEST_PACKAGE + '/androidx.test.runner.AndroidJUnitRunner')
        if 'OK (1 test)' not in output or 'FAILURES' in output or 'INSTRUMENTATION_FAILED' in output:
            raise RuntimeError('Native update check failed: ' + method)

    try:
        if command('shell', 'getprop', 'ro.kernel.qemu') != '1' or not is_dedicated_avd(command('emu', 'avd', 'name')):
            raise RuntimeError('Target is not the dedicated emulator')
        if command('shell', 'getprop', 'sys.boot_completed') != '1':
            raise RuntimeError('Guest boot incomplete')
        for package in (PACKAGE, TEST_PACKAGE):
            if 'package:' + package in command('shell', 'pm', 'list', 'packages', package).splitlines():
                raise RuntimeError('Refusing an existing installation')
        for apk, package in ((original, PACKAGE), (tests, TEST_PACKAGE)):
            attempted.append(package)
            if 'Success' not in command('install', str(apk)):
                raise RuntimeError('Install not confirmed')
        native('seedJavaEraState')
        command('shell', 'am', 'force-stop', PACKAGE)
        if 'Success' not in command('install', '-r', str(candidate)):
            raise RuntimeError('Replacement not confirmed')
        native('verifyPersistedState')
        receipt['result'] = 'passed'
    except Exception as error:
        receipt['error'] = str(error)
        raise
    finally:
        clean = True
        for package in reversed(attempted):
            try:
                command('uninstall', package)
                if 'package:' + package in command('shell', 'pm', 'list', 'packages', package).splitlines():
                    raise RuntimeError('Package still present')
            except Exception as error:
                clean = False
                receipt.setdefault('cleanup_errors', []).append(str(error))
        receipt['packages_removed'] = bool(attempted) and clean
        if not clean:
            receipt['result'] = 'failed'
        output.write_text(json.dumps(receipt, indent=2) + '\n')
        if attempted and not clean:
            raise RuntimeError('Update fixture package cleanup unverified: ' + str(output))
    print(json.dumps(dict(result=receipt['result'], packages_removed=receipt['packages_removed'], receipt=str(output))))


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--serial', required=True)
    qualify(parser.parse_args().serial)
