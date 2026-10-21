#!/usr/bin/env python3
"""Qualify unsigned release artifacts from two clean same-host source checkouts.

No signing, installation or publication. This is not independent-host reproduction.
The shared dependency cache is an explicit limitation, not a trust attestation.
"""
import argparse
import hashlib
import json
import os
from pathlib import Path
import subprocess
import time

ROOT = Path(__file__).resolve().parents[1]


def digest(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()


def compare_artifacts(paths):
    if len(paths) != 2 or any(not p.is_file() or p.stat().st_size == 0 for p in paths):
        raise ValueError('Two nonempty unsigned APKs are required')
    hashes = [digest(p) for p in paths]
    return {'sha256': hashes, 'identical': hashes[0] == hashes[1]}


def verify_unsigned(apk, sdk, env):
    result = subprocess.run([str(sdk / 'build-tools/35.0.0/apksigner'), 'verify',
                             '--print-certs', str(apk)], env=env, capture_output=True, text=True)
    output = result.stdout + result.stderr
    if result.returncode == 0 or 'ERROR: Missing META-INF/MANIFEST.MF' not in output:
        raise RuntimeError('Expected unsigned APK; signature status is signed or indeterminate')
    return dict(exit_code=result.returncode, output=output, unsigned_verified=True)


def run(output, jdk, sdk):
    if subprocess.check_output(['git', 'status', '--porcelain'], cwd=ROOT).strip():
        raise ValueError('Commit source changes before qualification')
    revision = subprocess.check_output(['git', 'rev-parse', 'HEAD'], cwd=ROOT, text=True).strip()
    output.mkdir(parents=True, exist_ok=False)
    receipt = {'revision': revision, 'scope': 'unsigned-same-host-development-identity',
               'independent_environment_reproduction': False, 'shared_dependency_cache': True,
               'signed': False, 'release_ready': False, 'builds': [], 'success': False}
    env = dict(os.environ, JAVA_HOME=str(jdk), ANDROID_HOME=str(sdk))
    start = time.monotonic()
    artifacts = []
    try:
        for number in (1, 2):
            checkout = output / f'checkout-{number}'
            subprocess.run(['git', 'clone', '--quiet', '--no-hardlinks', '--no-checkout',
                            str(ROOT), str(checkout)], check=True)
            subprocess.run(['git', 'checkout', '--quiet', '--detach', revision],
                           cwd=checkout, check=True)
            command = ['sh', 'gradlew', '--no-daemon', '--console=plain',
                       ':app:assembleRelease', ':app:testReleaseUnitTest', ':app:lintRelease',
                       ':app:dependencies', '--configuration', 'releaseRuntimeClasspath']
            log = output / f'build-{number}.log'
            with log.open('w') as stream:
                result = subprocess.run(command, cwd=checkout, env=env, stdout=stream,
                                        stderr=subprocess.STDOUT)
            record = {'command': command, 'exit_code': result.returncode,
                      'log_sha256': digest(log)}
            receipt['builds'].append(record)
            if result.returncode:
                raise RuntimeError(f'Build {number} failed; inspect {log}')
            if subprocess.check_output(['git', 'status', '--porcelain'], cwd=checkout).strip():
                raise RuntimeError('Build changed committed source checkout')
            apk = checkout / 'app/build/outputs/apk/release/app-release-unsigned.apk'
            record['signature_check'] = verify_unsigned(apk, sdk, env)
            artifacts.append(apk)
            record['apk'] = str(apk)
            record['apk_sha256'] = digest(apk)
            record['badging'] = subprocess.check_output(
                [str(sdk / 'build-tools/35.0.0/aapt'), 'dump', 'badging', str(apk)], text=True)
        receipt['comparison'] = compare_artifacts(artifacts)
        receipt['success'] = receipt['comparison']['identical']
        if not receipt['success']:
            raise RuntimeError('Unsigned APKs differ; reproduction is unqualified')
    except Exception as error:
        receipt['error'] = str(error)
        raise
    finally:
        receipt['elapsed_seconds'] = round(time.monotonic() - start, 1)
        receipt['jdk'] = str(jdk)
        receipt['sdk'] = str(sdk)
        (output / 'receipt.json').write_text(json.dumps(receipt, indent=2) + '\n')
    return receipt


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--output', type=Path, required=True)
    parser.add_argument('--jdk', type=Path, required=True)
    parser.add_argument('--sdk', type=Path, required=True)
    args = parser.parse_args()
    print(json.dumps(run(args.output.resolve(), args.jdk.resolve(), args.sdk.resolve()), indent=2))
