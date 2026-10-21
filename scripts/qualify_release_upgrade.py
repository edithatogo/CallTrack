#!/usr/bin/env python3
"""Version-increasing, existing-debug-key release-mode probe on the dedicated emulator.

No production key, permanent identity, provider writes or release approval.
"""
import argparse
import json
import os
from pathlib import Path
import re
import subprocess
import uuid

from parity_health import require_healthy_boot
from parity_targets import is_dedicated_avd
from qualify_kotlin_update import PACKAGE, TEST_PACKAGE, require_successful_receipt, sha
from run_slice_build import inputs

ROOT = Path(__file__).resolve().parents[1]
JDK = Path('/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home')


def require_rejection(result, reason):
    if result.returncode == 0 or reason not in result.stdout + result.stderr:
        raise RuntimeError('Expected controlled rejection: ' + reason)


def qualify(serial, output):
    if not re.fullmatch(r'emulator-\d+', serial):
        raise RuntimeError('Release upgrade probe refuses physical-device serials')
    if subprocess.check_output(['git', 'status', '--porcelain'], cwd=ROOT).strip():
        raise RuntimeError('Commit source before release upgrade probe')
    key = Path.home() / '.android/debug.keystore'
    if not key.is_file():
        raise RuntimeError('Existing development key required; no key will be generated')
    current = json.loads((ROOT / '.local/slice-evidence/build-receipt.json').read_text())
    require_successful_receipt(current)
    if current['inputs'] != inputs():
        raise RuntimeError('Current development build differs from source')
    original = ROOT / 'app/build/outputs/apk/debug/app-debug.apk'
    tests = ROOT / 'app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk'
    for apk in (original, tests):
        if current['artifacts'].get(str(apk.relative_to(ROOT))) != sha(apk):
            raise RuntimeError('Development artifact hash mismatch')
    revision = subprocess.check_output(['git', 'rev-parse', 'HEAD'], cwd=ROOT, text=True).strip()
    output.mkdir(parents=True, exist_ok=False)
    sdk = (ROOT / '.local/android-sdk').resolve()
    env = dict(os.environ, JAVA_HOME=str(JDK), ANDROID_HOME=str(sdk))
    receipt = dict(scope='development-key-release-mode-version-increase-private-state-only',
                   revision=revision, inputs=current['inputs'], token=uuid.uuid4().hex,
                   production_signed=False, release_ready=False, result='failed', steps=[])
    attempted = []

    def adb(*args, reject=None):
        result = subprocess.run([str(sdk / 'platform-tools/adb'), '-s', serial, *args],
                                capture_output=True, text=True, timeout=180)
        receipt['steps'].append(dict(args=list(args), exit_code=result.returncode,
                                    output=result.stdout + result.stderr))
        if reject:
            require_rejection(result, reject)
        elif result.returncode:
            raise RuntimeError('ADB failed: ' + ' '.join(args))
        return result.stdout.strip()

    def native(method):
        result = adb('shell', 'am', 'instrument', '-w', '-r', '-e', 'fixtureToken', receipt['token'],
                     '-e', 'class', 'com.markobl.calllogsync.slice.KotlinPackageUpdateTest#' + method,
                     TEST_PACKAGE + '/androidx.test.runner.AndroidJUnitRunner')
        if 'OK (1 test)' not in result or 'FAILURES' in result or 'INSTRUMENTATION_FAILED' in result:
            raise RuntimeError('Private-state verification failed: ' + method)

    try:
        if adb('shell', 'getprop', 'ro.kernel.qemu') != '1' or not is_dedicated_avd(adb('emu', 'avd', 'name')):
            raise RuntimeError('Dedicated emulator required')
        require_healthy_boot(adb)
        for package in (PACKAGE, TEST_PACKAGE):
            if 'package:' + package in adb('shell', 'pm', 'list', 'packages', package).splitlines():
                raise RuntimeError('Refusing an existing installation')
        checkout = output / 'checkout'
        subprocess.run(['git', 'clone', '--quiet', '--no-hardlinks', '--no-checkout', str(ROOT), str(checkout)], check=True)
        subprocess.run(['git', 'checkout', '--quiet', '--detach', revision], cwd=checkout, check=True)
        overlay = output / 'version-probe.init.gradle'
        overlay.write_text("""gradle.beforeProject { project ->
    project.plugins.withId('com.android.application') {
        project.androidComponents.finalizeDsl { android ->
            android.defaultConfig.versionCode = 11
            android.buildTypes.release.signingConfig = android.signingConfigs.debug
        }
    }
}
""")
        receipt['overlay_sha256'] = sha(overlay)
        with (output / 'build.log').open('w') as log:
            subprocess.run(['sh', 'gradlew', '--no-daemon', '--console=plain', '-I', str(overlay),
                            ':app:assembleRelease'], cwd=checkout, env=env, stdout=log,
                           stderr=subprocess.STDOUT, check=True)
        if subprocess.check_output(['git', 'status', '--porcelain'], cwd=checkout).strip():
            raise RuntimeError('Build modified committed source')
        candidate = checkout / 'app/build/outputs/apk/release/app-release.apk'
        certs = []
        for apk, package, version in ((original, PACKAGE, 10), (candidate, PACKAGE, 11), (tests, TEST_PACKAGE, None)):
            badge = subprocess.check_output([str(sdk / 'build-tools/35.0.0/aapt'), 'dump', 'badging', str(apk)], text=True)
            if "package: name='" + package + "'" not in badge or (version and "versionCode='" + str(version) + "'" not in badge):
                raise RuntimeError('Unexpected package/version')
            signature = subprocess.check_output([str(sdk / 'build-tools/35.0.0/apksigner'), 'verify', '--print-certs', str(apk)], env=env, text=True)
            found = re.findall(r'Signer #\d+ certificate SHA-256 digest: ([a-f0-9]{64})', signature)
            if len(found) != 1:
                raise RuntimeError('Exactly one verified signer required')
            certs.append(found[0])
        if len(set(certs)) != 1:
            raise RuntimeError('Development/instrumentation certificate mismatch')
        receipt.update(artifacts={str(p): sha(p) for p in (original, candidate, tests)},
                       signer_sha256=certs[0], versions=[10, 11], build_log_sha256=sha(output / 'build.log'))
        for apk, package in ((original, PACKAGE), (tests, TEST_PACKAGE)):
            attempted.append(package)
            if 'Success' not in adb('install', str(apk)):
                raise RuntimeError('Install unconfirmed')
        native('seedJavaEraState')
        adb('shell', 'am', 'force-stop', PACKAGE)
        if 'Success' not in adb('install', '-r', str(candidate)):
            raise RuntimeError('Upgrade unconfirmed')
        native('verifyPersistedState')
        adb('install', '-r', str(original), reject='INSTALL_FAILED_VERSION_DOWNGRADE')
        native('verifyPersistedState')
        receipt['result'] = 'passed'
    except Exception as error:
        receipt['error'] = str(error)
        raise
    finally:
        clean = True
        for package in reversed(attempted):
            try:
                if 'Success' not in adb('uninstall', package):
                    raise RuntimeError('Uninstall unconfirmed')
                if 'package:' + package in adb('shell', 'pm', 'list', 'packages', package).splitlines():
                    raise RuntimeError('Package still present')
            except Exception as error:
                clean = False
                receipt.setdefault('cleanup_errors', []).append(str(error))
        receipt['packages_removed'] = bool(attempted) and clean
        if not clean:
            receipt['result'] = 'failed'
        (output / 'receipt.json').write_text(json.dumps(receipt, indent=2) + '\n')
        if not clean:
            raise RuntimeError('Package cleanup unverified')
    return receipt


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--serial', required=True)
    parser.add_argument('--output', type=Path, required=True)
    args = parser.parse_args()
    print(json.dumps(qualify(args.serial, args.output.resolve()), indent=2))
