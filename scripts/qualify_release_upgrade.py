#!/usr/bin/env python3
"""Version-increasing, existing-debug-key release-mode probe on the dedicated emulator.

Default scope is private state only; --provider-schema adds explicit synthetic LOCAL
provider fixtures. No production key, permanent identity or release approval.
"""
import argparse
import json
import os
from pathlib import Path
import re
import subprocess
import uuid
import tempfile
import zipfile

from parity_health import require_healthy_boot
from parity_targets import is_dedicated_avd
from qualify_kotlin_update import PACKAGE, TEST_PACKAGE, require_successful_receipt, sha
from run_slice_build import inputs

ROOT = Path(__file__).resolve().parents[1]
JDK = Path(os.environ.get('JAVA_HOME', '/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home'))


def require_rejection(result, reason):
    if result.returncode == 0 or reason not in result.stdout + result.stderr:
        raise RuntimeError('Expected controlled rejection: ' + reason)


def cleanup_owned_packages(adb, attempted):
    errors = []
    for package in reversed(attempted):
        try:
            if 'Success' not in adb('uninstall', package):
                raise RuntimeError('Uninstall unconfirmed')
            if 'package:' + package in adb('shell', 'pm', 'list', 'packages', package).splitlines():
                raise RuntimeError('Package still present')
        except Exception as error:
            errors.append(str(error))
    return errors


def qualification_inputs(directory):
    paths = [p for folder in ('java', 'assets') for p in (directory / folder).rglob('*') if p.is_file()]
    paths.extend(directory / name for name in ('schema-provenance.json', 'import-schema-provenance.json'))
    return {str(p.relative_to(ROOT)): sha(p) for p in sorted(paths)}


def frozen_release_apk(receipt_path, revision, sdk, env):
    """Require two exact-source, byte-identical, still-unsigned release artifacts."""
    build = json.loads(receipt_path.read_text())
    records = build.get('builds', [])
    if (not build.get('success') or build.get('signed') is not False or
            build.get('revision') != revision or len(records) != 2):
        raise RuntimeError('Matching frozen unsigned build receipt required')
    from qualify_release_build import verify_unsigned
    hashes = []
    for record in records:
        apk = Path(record['apk'])
        if (record.get('exit_code') != 0 or
                record.get('signature_check', {}).get('unsigned_verified') is not True or
                sha(apk) != record.get('apk_sha256')):
            raise RuntimeError('Frozen unsigned artifact changed or failed')
        verify_unsigned(apk, sdk, env)
        hashes.append(sha(apk))
    if hashes[0] != hashes[1]:
        raise RuntimeError('Frozen unsigned artifacts differ')
    return Path(records[0]['apk'])


def require_identical_payload(unsigned, signed):
    # Signing may add only signature metadata, never alter an original entry.
    with zipfile.ZipFile(unsigned) as before, zipfile.ZipFile(signed) as after:
        first, second = before.namelist(), after.namelist()
        if len(first) != len(set(first)) or len(second) != len(set(second)):
            raise RuntimeError('Duplicate APK entries')
        allowed = {'META-INF/MANIFEST.MF', 'META-INF/ANDROIDD.SF', 'META-INF/ANDROIDD.RSA'}
        if set(second) - set(first) - allowed:
            raise RuntimeError('Unexpected signed APK entries')
        for name in first:
            if name not in second or before.read(name) != after.read(name):
                raise RuntimeError('Signed fixture changed candidate payload: ' + name)


def qualify(serial, output, provider_schema=False, release_build_receipt=None):
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
    qualification = ROOT / 'qualification/release'
    fixture_inputs = qualification_inputs(qualification) if provider_schema else {}
    revision = subprocess.check_output(['git', 'rev-parse', 'HEAD'], cwd=ROOT, text=True).strip()
    output.mkdir(parents=True, exist_ok=False)
    sdk = (ROOT / '.local/android-sdk').resolve()
    env = dict(os.environ, JAVA_HOME=str(JDK), ANDROID_HOME=str(sdk),
               CALLTRACK_RELEASE_QUALIFICATION=str(qualification))
    receipt = dict(scope='development-key-release-mode-version-increase-private-state-only',
                   revision=revision, inputs=current['inputs'], token=uuid.uuid4().hex,
                   production_signed=False, release_ready=False, result='failed', steps=[])
    receipt['qualification_inputs'] = fixture_inputs
    if provider_schema:
        receipt['scope'] = 'development-key-release-mode-historical-schema-provider-upgrade'
    attempted = []
    provider_attempted = False
    if release_build_receipt is not None:
        unsigned = frozen_release_apk(release_build_receipt, revision, sdk, env)
        original = output / 'exact-release-development-signed.apk'
        signing_env = dict(env, CALLTRACK_DEBUG_PASSWORD='android')
        subprocess.run([str(sdk / 'build-tools/35.0.0/apksigner'), 'sign',
                        '--ks', str(key), '--ks-key-alias', 'androiddebugkey',
                        '--ks-pass', 'env:CALLTRACK_DEBUG_PASSWORD',
                        '--key-pass', 'env:CALLTRACK_DEBUG_PASSWORD',
                        '--out', str(original), str(unsigned)], env=signing_env,
                       capture_output=True, text=True, check=True)
        require_identical_payload(unsigned, original)
        receipt['exact_release_baseline'] = dict(
            unsigned_sha256=sha(unsigned), signed_fixture_sha256=sha(original),
            payload_identical=True, build_receipt_sha256=sha(release_build_receipt),
            production_signing=False)


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

    def native(method, class_name="KotlinPackageUpdateTest"):
        result = adb('shell', 'am', 'instrument', '-w', '-r', '-e', 'fixtureToken', receipt['token'],
                     '-e', 'class', 'com.markobl.calllogsync.slice.' + class_name + '#' + method,
                     TEST_PACKAGE + '/androidx.test.runner.AndroidJUnitRunner')
        if 'OK (1 test)' not in result or 'FAILURES' in result or 'INSTRUMENTATION_FAILED' in result:
            raise RuntimeError('Native release qualification failed: ' + method)

    def provider(method):
        native(method, 'ReleaseUpgradeProviderTest')

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
        if provider_schema:
            overlay.write_text(overlay.read_text() + """
gradle.beforeProject { project ->
    project.plugins.withId('com.android.application') {
        project.androidComponents.finalizeDsl { android ->
            def directory = System.getenv('CALLTRACK_RELEASE_QUALIFICATION')
            android.sourceSets.androidTest.java.srcDir(directory + '/java')
            android.sourceSets.androidTest.assets.srcDir(directory + '/assets')
        }
    }
}
""")
        receipt['overlay_sha256'] = sha(overlay)
        with (output / 'build.log').open('w') as log:
            subprocess.run(['sh', 'gradlew', '--no-daemon', '--console=plain', '-I', str(overlay),
                            *([':app:assembleRelease', ':app:assembleDebugAndroidTest'] if provider_schema else [':app:assembleRelease'])], cwd=checkout, env=env, stdout=log,
                           stderr=subprocess.STDOUT, check=True)
        if subprocess.check_output(['git', 'status', '--porcelain'], cwd=checkout).strip():
            raise RuntimeError('Build modified committed source')
        if provider_schema:
            if fixture_inputs != qualification_inputs(qualification):
                raise RuntimeError('Qualification inputs changed during build')
            tests = checkout / 'app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk'
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
        # Building can take minutes: recheck ownership and guest health before mutation.
        if adb('shell', 'getprop', 'ro.kernel.qemu') != '1' or not is_dedicated_avd(adb('emu', 'avd', 'name')):
            raise RuntimeError('Dedicated emulator changed during build')
        require_healthy_boot(adb)
        for package in (PACKAGE, TEST_PACKAGE):
            if 'package:' + package in adb('shell', 'pm', 'list', 'packages', package).splitlines():
                raise RuntimeError('Refusing an installation created during build')
        for apk, package in ((original, PACKAGE), (tests, TEST_PACKAGE)):
            attempted.append(package)
            if 'Success' not in adb('install', str(apk)):
                raise RuntimeError('Install unconfirmed')
        if release_build_receipt is not None:
            native('freshInstallIsUnconfiguredAndDenied')
            launch = adb('shell', 'am', 'start', '-W', '-n',
                         PACKAGE + '/com.markobl.calllogsync.slice.CalendarSliceActivity')
            if 'Status: ok' not in launch:
                raise RuntimeError('Fresh candidate launcher failed')
            adb('shell', 'am', 'force-stop', PACKAGE)
            native('freshInstallIsUnconfiguredAndDenied')
            receipt['exact_release_fresh_install_qualified'] = True
        native('seedJavaEraState')
        if provider_schema:
            provider_attempted = True
            for permission in ('READ_CALENDAR', 'WRITE_CALENDAR'):
                adb('shell', 'pm', 'grant', PACKAGE, 'android.permission.' + permission)
            provider('seedHistoricalSchemasAndProviderState')
        adb('shell', 'am', 'force-stop', PACKAGE)
        if 'Success' not in adb('install', '-r', str(candidate)):
            raise RuntimeError('Upgrade unconfirmed')
        if not re.search(r'\bversionCode=11\b', adb('shell', 'dumpsys', 'package', PACKAGE)):
            raise RuntimeError('Installed increasing version unverified')
        native('verifyPersistedState')
        if provider_schema:
            provider('migrateAndRecoverWithoutChangingHumanEvents')
            provider('migrationFailureRollsBackThenRetries')
            provider('unsupportedNewerSchemaIsPreserved')
            provider('importMigrationPreservesProgressAndCancelledSession')
        adb('install', '-r', str(original), reject='INSTALL_FAILED_VERSION_DOWNGRADE')
        native('verifyPersistedState')
        # A one-day disposable fixture certificate tests rejection, never production custody.
        with tempfile.TemporaryDirectory(prefix='calltrack-test-key-', dir=output) as temporary:
            fixture_key = Path(temporary) / 'fixture.keystore'
            fixture_apk = Path(temporary) / 'wrong-certificate.apk'
            fixture_env = dict(env, CALLTRACK_FIXTURE_PASSWORD='disposable-fixture-only')
            subprocess.run([str(JDK / 'bin/keytool'), '-genkeypair', '-alias', 'fixture',
                            '-dname', 'CN=Disposable Calltrack Test Only', '-keyalg', 'RSA',
                            '-validity', '1', '-keystore', str(fixture_key),
                            '-storepass:env', 'CALLTRACK_FIXTURE_PASSWORD',
                            '-keypass:env', 'CALLTRACK_FIXTURE_PASSWORD'], env=fixture_env,
                           capture_output=True, text=True, check=True)
            subprocess.run([str(sdk / 'build-tools/35.0.0/apksigner'), 'sign',
                            '--ks', str(fixture_key), '--ks-key-alias', 'fixture',
                            '--ks-pass', 'env:CALLTRACK_FIXTURE_PASSWORD',
                            '--key-pass', 'env:CALLTRACK_FIXTURE_PASSWORD',
                            '--out', str(fixture_apk), str(candidate)], env=fixture_env,
                           capture_output=True, text=True, check=True)
            wrong_signature = subprocess.check_output([str(sdk / 'build-tools/35.0.0/apksigner'),
                'verify', '--print-certs', str(fixture_apk)], env=env, text=True)
            wrong_certs = re.findall(r'Signer #\d+ certificate SHA-256 digest: ([a-f0-9]{64})', wrong_signature)
            if len(wrong_certs) != 1 or wrong_certs[0] == certs[0]:
                raise RuntimeError('Different verified fixture signer required')
            receipt['rejected_fixture_signer_sha256'] = wrong_certs[0]
            adb('install', '-r', str(fixture_apk), reject='INSTALL_FAILED_UPDATE_INCOMPATIBLE')
            native('verifyPersistedState')
        receipt['disposable_fixture_key_removed'] = not Path(temporary).exists()
        if provider_schema:
            for permission in ('READ_CALENDAR', 'WRITE_CALENDAR'):
                adb('shell', 'pm', 'revoke', PACKAGE, 'android.permission.' + permission)
            provider('deniedPermissionCannotReplayAfterUpgrade')
            for permission in ('READ_CALENDAR', 'WRITE_CALENDAR'):
                adb('shell', 'pm', 'grant', PACKAGE, 'android.permission.' + permission)
            provider('migrateAndRecoverWithoutChangingHumanEvents')
            provider('explicitPendingResumeIsExactlyOnce')
            provider('cleanupFixture')
            provider('removedDestinationCannotRetargetOrEraseReceipts')
        receipt['result'] = 'passed'
    except Exception as error:
        receipt['error'] = str(error)
        raise
    finally:
        errors = []
        if provider_attempted:
            try:
                for permission in ('READ_CALENDAR', 'WRITE_CALENDAR'):
                    adb('shell', 'pm', 'grant', PACKAGE, 'android.permission.' + permission)
                provider('cleanupFixture')
                receipt['provider_fixture_removed'] = True
            except Exception as error:
                errors.append('Provider cleanup unverified: ' + str(error))
                receipt['provider_fixture_removed'] = False
        errors.extend(cleanup_owned_packages(adb, attempted))
        clean = not errors
        if errors:
            receipt['cleanup_errors'] = errors
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
    parser.add_argument('--provider-schema', action='store_true', help='Use synthetic source-pinned schemas and LOCAL provider fixtures')
    parser.add_argument('--release-build-receipt', type=Path, help='Qualify an exact unsigned release payload using the existing development key only')
    args = parser.parse_args()
    print(json.dumps(qualify(args.serial, args.output.resolve(), args.provider_schema, args.release_build_receipt), indent=2))
