#!/usr/bin/env python3
"""Cold Linux release reproduction; no signing, device access or shared build cache.

Run in an isolated Linux guest/container or hosted runner. The SDK/JDK licensing
basis must already be authorised; this script never accepts licence agreements.
It downloads exact SDK packages and uses a new Gradle home, retaining all receipts.
"""
import argparse
import hashlib
import json
import os
from pathlib import Path, PurePosixPath
import platform
import re
import subprocess
import time
import urllib.request
from urllib.parse import urlsplit
import xml.etree.ElementTree as ET
import zipfile

TOOLS = Path(__file__).resolve().parents[1]


def sha(path, algorithm='sha256'):
    h = hashlib.new(algorithm)
    with path.open('rb') as stream:
        for block in iter(lambda: stream.read(1024 * 1024), b''):
            h.update(block)
    return h.hexdigest()


def fresh_directory(path):
    path.mkdir(parents=True, exist_ok=False)


def validate_revision(value):
    if not re.fullmatch('[a-f0-9]{40}', value):
        raise ValueError('Exact forty-character Git SHA required')


def approved_url(url):
    parsed = urlsplit(url)
    if (parsed.scheme != 'https' or parsed.netloc != 'dl.google.com' or
            not parsed.path.startswith('/android/repository/') or
            '..' in parsed.path.split('/') or parsed.query or parsed.fragment):
        raise ValueError('Unapproved SDK archive origin')


def verify_download(path, row):
    if (path.stat().st_size != row['size'] or
            sha(path, row['checksum_algorithm']) != row['checksum'] or
            (row.get('sha256') and sha(path) != row['sha256'])):
        raise ValueError('SDK archive checksum/size mismatch')


def extract_sdk(path, destination):
    with zipfile.ZipFile(path) as archive:
        members = archive.infolist()
        if len(members) > 100000 or sum(m.file_size for m in members) > 2 * 1024**3:
            raise ValueError('SDK archive expansion limit exceeded')
        for member in members:
            relative = PurePosixPath(member.filename)
            mode = member.external_attr >> 16
            if (relative.is_absolute() or '..' in relative.parts or '\\' in member.filename or
                    (mode & 0o170000) == 0o120000 or len(relative.parts) < 1):
                raise ValueError('Unsafe SDK archive member')
            if member.is_dir():
                continue
            if len(relative.parts) < 2:
                raise ValueError('SDK archive requires a single top-level directory')
            target = destination.joinpath(*relative.parts[1:])
            target.parent.mkdir(parents=True, exist_ok=True)
            with target.open('xb') as stream:
                stream.write(archive.read(member))
            target.chmod((mode & 0o777) or 0o644)


def junit_counts(source):
    counts = dict(tests=0, failures=0, errors=0, skipped=0)
    for path in (source / 'app/build/test-results/testReleaseUnitTest').glob('TEST-*.xml'):
        data = ET.parse(path).getroot().attrib
        for key in counts:
            counts[key] += int(data.get(key, 0))
    return counts


def run(source, output, revision, expected_apk):
    validate_revision(revision)
    if platform.system() != 'Linux':
        raise ValueError('A Linux environment is required')
    if subprocess.check_output(['git', 'rev-parse', 'HEAD'], cwd=source, text=True).strip() != revision:
        raise ValueError('Candidate Git revision mismatch')
    if subprocess.check_output(['git', 'status', '--porcelain'], cwd=source).strip():
        raise ValueError('Candidate source is dirty')
    fresh_directory(output)
    evidence = output / 'evidence'
    evidence.mkdir()
    sdk, cache = output / 'sdk', output / 'gradle-home'
    receipt = dict(success=False, release_ready=False, revision=revision,
                   linux=True, kernel=platform.release(), machine=platform.machine(),
                   physical_host_independence=False, hosted_ci=False,
                   cache_empty_before=not cache.exists(), sdk_empty_before=not sdk.exists(),
                   sdk_packages=[], source_files_unchanged=False,
                   script_sha256=sha(Path(__file__)), sdk_manifest_sha256=sha(TOOLS / 'qualification/release/linux-sdk-inputs.json'))
    start = time.monotonic()
    names = subprocess.check_output(['git', 'ls-files', '-z'], cwd=source).decode().split('\0')[:-1]
    before = {name: sha(source / name) for name in names}
    env = dict(os.environ, GRADLE_USER_HOME=str(cache), ANDROID_HOME=str(sdk))
    env.pop('ANDROID_SDK_ROOT', None)
    try:
        receipt['java_version'] = subprocess.check_output(['java', '-version'], stderr=subprocess.STDOUT, text=True)
        if not re.search(r'version "17[.]', receipt['java_version']):
            raise ValueError('JDK17 required')
        packages = json.loads((TOOLS / 'qualification/release/linux-sdk-inputs.json').read_text())
        destinations = {'cmdline-tools;19.0': sdk / 'cmdline-tools/19.0',
                        'platforms;android-36': sdk / 'platforms/android-36',
                        'build-tools;35.0.0': sdk / 'build-tools/35.0.0'}
        for row in packages:
            approved_url(row['url'])
            if row.get('licence') != 'android-sdk-license':
                raise ValueError('Different SDK licence needs separate qualification')
            archive = output / row['url'].rsplit('/', 1)[-1]
            with urllib.request.urlopen(row['url'], timeout=120) as response, archive.open('xb') as stream:
                approved_url(response.url)
                total = 0
                while block := response.read(1024 * 1024):
                    total += len(block)
                    if total > row['size']:
                        raise ValueError('SDK archive exceeds declared size')
                    stream.write(block)
            verify_download(archive, row)
            extract_sdk(archive, destinations[row['package']])
            receipt['sdk_packages'].append(dict(row, sha256=sha(archive)))
            print('Fresh SDK package verified: ' + row['package'], flush=True)
        graph = evidence / 'runtime-inventory.json'
        env['CALLTRACK_RUNTIME_OUT'] = str(graph)
        command = ['sh', 'gradlew', '--no-daemon', '--no-watch-fs', '--console=plain',
                   '-I', str(TOOLS / 'qualification/release/runtime-inventory.init.gradle'),
                   ':app:assembleRelease', ':app:testReleaseUnitTest', ':app:lintRelease',
                   'releaseBundleRuntimeInventory']
        receipt['command'] = command
        with (evidence / 'build.log').open('w') as stream:
            result = subprocess.run(command, cwd=source, env=env, stdout=stream, stderr=subprocess.STDOUT)
        receipt['build_exit_code'] = result.returncode
        receipt['build_log_sha256'] = sha(evidence / 'build.log')
        if result.returncode:
            raise RuntimeError('Linux build failed; inspect retained build.log')
        receipt['jvm'] = junit_counts(source)
        if receipt['jvm'] != dict(tests=115, failures=0, errors=0, skipped=0):
            raise RuntimeError('Candidate JVM acceptance count/outcome changed')
        lint = ET.parse(source / 'app/build/reports/lint-results-release.xml').getroot()
        counts = {}
        for issue in lint.findall('issue'):
            counts[issue.get('severity')] = counts.get(issue.get('severity'), 0) + 1
        receipt['lint'] = counts
        if counts.get('Error', 0) or counts.get('Fatal', 0):
            raise RuntimeError('Release lint failed')
        apk = source / 'app/build/outputs/apk/release/app-release-unsigned.apk'
        receipt['apk_sha256'] = sha(apk)
        (evidence / apk.name).write_bytes(apk.read_bytes())
        check = subprocess.run([str(sdk / 'build-tools/35.0.0/apksigner'), 'verify', str(apk)],
                               env=env, capture_output=True, text=True)
        receipt['unsigned_verified'] = check.returncode != 0 and 'ERROR: Missing META-INF/MANIFEST.MF' in check.stdout + check.stderr
        if not receipt['unsigned_verified']:
            raise RuntimeError('Unsigned APK status indeterminate')
        receipt['source_files_unchanged'] = before == {name: sha(source / name) for name in names}
        if not receipt['source_files_unchanged'] or subprocess.check_output(['git', 'status', '--porcelain'], cwd=source).strip():
            raise RuntimeError('Build modified candidate source')
        receipt['expected_apk_sha256'] = expected_apk
        receipt['bitwise_match_to_macos'] = receipt['apk_sha256'] == expected_apk
        receipt['success'] = receipt['bitwise_match_to_macos']
        if not receipt['success']:
            raise RuntimeError('Linux APK differs; investigate exported artifact before acceptance')
    except Exception as error:
        receipt['error'] = str(error)
        raise
    finally:
        receipt['elapsed_seconds'] = round(time.monotonic() - start, 1)
        (evidence / 'receipt.json').write_text(json.dumps(receipt, indent=2) + '\n')
    return receipt


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--source', required=True, type=Path)
    parser.add_argument('--output', required=True, type=Path)
    parser.add_argument('--expected-revision', required=True)
    parser.add_argument('--expected-apk-sha256', required=True)
    args = parser.parse_args()
    print(json.dumps(run(args.source.resolve(), args.output.resolve(), args.expected_revision,
                         args.expected_apk_sha256), indent=2))
