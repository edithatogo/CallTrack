#!/usr/bin/env -S uv run python
"""Read-only comparison against official Gradle, SDK and Temurin archive evidence.

Never installs tools, accepts licences or treats HTTPS checksums as independent signatures.
Success qualifies payload-origin comparison only; JDK bundle-resource sealing is separate.
"""
import argparse
import errno
import hashlib
import json
import os
from pathlib import Path, PurePosixPath
import platform
import re
import stat
import subprocess
import struct
import tarfile
import urllib.request
from urllib.parse import urlsplit
import zipfile
import audit_release_artifacts
from redirect_validation import ValidatingRedirectHandler
from xml_safety import parse_xml

ROOT = Path(__file__).resolve().parents[1]
TEMURIN25_ARCHIVE = 'OpenJDK25U-jdk_aarch64_mac_hotspot_25.0.5.1_2.tar.gz'
TEMURIN25_SHA256 = '248353ea41be39d5d2c1c67c7ce0ae4d1668966aacd0c5308464bc1be42f677b'


def validate_tool_redirect(url):
    if urlsplit(url).scheme != 'https':
        raise ValueError('Insecure tool archive redirect')


TOOL_OPENER = urllib.request.build_opener(ValidatingRedirectHandler(validate_tool_redirect))


def sha(path, algorithm='sha256'):
    hasher = hashlib.new(algorithm)
    with path.open('rb') as stream:
        for block in iter(lambda: stream.read(1024 * 1024), b''):
            hasher.update(block)
    return hasher.hexdigest()


def fetch(url, path):
    if not url.startswith('https://'):
        raise ValueError('HTTPS required')
    with TOOL_OPENER.open(url, timeout=120) as response, path.open('xb') as stream:
        if not response.url.startswith('https://'):
            raise ValueError('Insecure tool archive redirect')
        total = 0
        while block := response.read(1024 * 1024):
            total += len(block)
            if total > 512 * 1024 * 1024:
                raise ValueError('Tool archive exceeds audit download limit')
            stream.write(block)
    return sha(path)


def published_checksum(url, path):
    fetch(url, path)
    value = path.read_text().strip()
    if not re.fullmatch('[a-f0-9]{64}', value):
        raise ValueError('Expected exact official SHA256')
    return value


def selected_toolchain_path(name, relative):
    """Bind environment selections to an explicitly provisioned local alias."""
    selected = ROOT / relative
    if os.environ.get(name) != str(selected):
        raise RuntimeError(f'{name} must select {selected}')
    return selected


def sdk_archive_name(name):
    if not isinstance(name, str) or not re.fullmatch(r'[A-Za-z0-9][A-Za-z0-9_.-]*\.zip', name) or '..' in name:
        raise ValueError('Unsafe SDK archive name')
    return name


def archive_relative(name):
    if not isinstance(name, str) or name.startswith('/') or '\\' in name:
        raise ValueError('Unsafe archive path')
    parts = name.split('/')
    if len(parts) < 2 or any(part in ('', '.', '..') for part in parts):
        raise ValueError('Unsafe archive path')
    return Path(*parts[1:])


def sha_installed(installed, relative):
    """Hash a regular file beneath a trusted root without following child links."""
    parent = None
    try:
        parent = audit_release_artifacts.open_directory(os.path.realpath(installed), relative.parts[:-1])
        descriptor = os.open(relative.name, os.O_RDONLY | os.O_NOFOLLOW | os.O_NONBLOCK,
                             dir_fd=parent)
        with os.fdopen(descriptor, 'rb') as stream:
            if not stat.S_ISREG(os.fstat(stream.fileno()).st_mode):
                return None
            return hashlib.file_digest(stream, 'sha256').hexdigest()
    except OSError as error:
        if error.errno in (errno.ENOENT, errno.ENOTDIR, errno.ELOOP):
            return None
        raise
    finally:
        if parent is not None:
            os.close(parent)


def zip_comparison(archive, installed):
    rows, seen = [], set()
    with zipfile.ZipFile(archive) as bundle:
        for member in bundle.infolist():
            if member.is_dir():
                continue
            relative = archive_relative(member.filename)
            if relative in seen:
                raise ValueError('Duplicate archive path')
            seen.add(relative)
            expected = hashlib.sha256(bundle.read(member)).hexdigest()
            rows.append(dict(path=str(relative), archive_sha256=expected,
                             installed_sha256=sha_installed(installed, relative)))
    return dict(files=rows, all_archive_files_match=bool(rows) and all(
        r['archive_sha256'] == r['installed_sha256'] for r in rows))


def temurin_comparison(archive, installed):
    """Match every regular bundle file to the pinned publisher archive."""
    rows, expected_paths = [], set()
    with tarfile.open(archive) as bundle:
        for member in bundle:
            path = PurePosixPath(member.name)
            if not path.parts or path.parts[0] != 'jdk-25.0.5.1+2' or '..' in path.parts:
                raise ValueError('Unsafe or unexpected JDK archive path')
            if member.isdir():
                continue
            if not member.isfile() or len(path.parts) < 2:
                raise ValueError('Unexpected JDK archive member')
            relative = archive_relative(member.name)
            if relative in expected_paths:
                raise ValueError('Duplicate JDK archive member')
            expected_paths.add(relative)
            with bundle.extractfile(member) as stream:
                expected = hashlib.file_digest(stream, 'sha256').hexdigest()
            actual = None if installed.is_symlink() else sha_installed(installed, relative)
            rows.append(dict(path=str(relative), archive_sha256=expected,
                             installed_sha256=actual))
    actual_paths = {p.relative_to(installed) for p in installed.rglob('*') if p.is_symlink() or p.is_file()}
    return dict(files=rows, unexpected_files=sorted(str(p) for p in actual_paths - expected_paths),
                all_jdk_files_match=bool(rows) and actual_paths == expected_paths and all(
                    r['archive_sha256'] == r['installed_sha256'] for r in rows))


def qualified_local_jdk():
    """Select only the repo-local macOS JDK whose full payload matches the publisher archive."""
    if platform.system() != 'Darwin' or platform.machine() != 'arm64':
        raise RuntimeError('The local JDK upgrade probe is qualified only on macOS ARM64')
    toolchain = ROOT / '.local/toolchain'
    archive = toolchain / TEMURIN25_ARCHIVE
    bundle = toolchain / 'temurin-jdk25'
    if not archive.is_file() or sha(archive) != TEMURIN25_SHA256:
        raise RuntimeError('Publisher-pinned Temurin 25 archive required in .local/toolchain')
    if not bundle.is_dir() or not temurin_comparison(archive, bundle.resolve())['all_jdk_files_match']:
        raise RuntimeError('The local Temurin 25 bundle differs from the pinned archive')
    return bundle / 'Contents/Home', {'archive_sha256': TEMURIN25_SHA256,
                                      'bundle': str(bundle.resolve()),
                                      'verification_script_sha256': sha(Path(__file__).resolve()),
                                      'path_helper_sha256': sha(Path(audit_release_artifacts.__file__).resolve())}


def qualified_java_environment(jdk):
    env = dict(os.environ, JAVA_HOME=str(jdk))
    for name in ('JAVA_TOOL_OPTIONS', 'JDK_JAVA_OPTIONS', '_JAVA_OPTIONS', 'CLASSPATH',
                 'LD_PRELOAD', 'LD_LIBRARY_PATH', 'DYLD_INSERT_LIBRARIES',
                 'DYLD_LIBRARY_PATH', 'DYLD_FRAMEWORK_PATH'):
        env.pop(name, None)
    return env


def macho_payload(data):
    """Ignore only signature allocation metadata; retain every relocated code/data byte."""
    if len(data) < 32 or struct.unpack_from('<I', data)[0] != 0xfeedfacf:
        raise ValueError('Expected thin little-endian Mach-O64')
    count, size = struct.unpack_from('<II', data, 16)
    if count > 1000 or 32 + size > len(data):
        raise ValueError('Invalid Mach-O commands')
    normalized = bytearray(data)
    offset = 32
    signature = None
    linkedit = False
    for _ in range(count):
        if offset + 8 > 32 + size:
            raise ValueError('Truncated Mach-O command header')
        command, length = struct.unpack_from('<II', data, offset)
        if length < 8 or offset + length > 32 + size:
            raise ValueError('Invalid Mach-O command length')
        if command == 0x1d:
            if signature is not None or length != 16:
                raise ValueError('Invalid signature command')
            signature, allocation = struct.unpack_from('<II', data, offset + 8)
            if signature <= 32 + size or signature + allocation != len(data):
                raise ValueError('Signature allocation must cover exact file tail')
            normalized[offset + 12:offset + 16] = bytes(4)
        elif command == 0x19 and data[offset + 8:offset + 24].rstrip(b'\0') == b'__LINKEDIT':
            if length < 72 or linkedit:
                raise ValueError('Invalid LINKEDIT command')
            linkedit = True
            file_offset, file_size = struct.unpack_from('<QQ', data, offset + 40)
            if file_offset + file_size != len(data):
                raise ValueError('LINKEDIT allocation must cover exact file tail')
            normalized[offset + 48:offset + 56] = bytes(8)
        offset += length
    if signature is None or not linkedit or offset != 32 + size:
        raise ValueError('Missing signature/LINKEDIT boundary')
    return hashlib.sha256(normalized[:signature]).hexdigest()


def verify_jdk_signature(path, relative):
    result = subprocess.run(['codesign', '--verify', '--strict', str(path)], capture_output=True, text=True)
    if result.returncode == 0:
        return dict(strict_valid=True)
    known = 'code has no resources but signature indicates they must be present'
    if relative != 'Contents/MacOS/libjli.dylib' or known not in result.stderr:
        raise RuntimeError('Unexpected JDK code signature failure: ' + result.stderr)
    # This tests code pages only. The bundle resource-seal defect remains explicitly open.
    pages = subprocess.run(['codesign', '--verify', '--strict', '--ignore-resources', str(path)],
                           capture_output=True, text=True)
    if pages.returncode:
        raise RuntimeError('JDK code page signature verification failed')
    return dict(strict_valid=False, bundle_resource_seal_issue=result.stderr,
                code_pages_valid=True, resources_verified=False)


def reproduce_relocation(path):
    """Apply only Homebrew placeholder relocation and ad-hoc signing to an excluded copy."""
    def relocate(value):
        return value.replace('@@HOMEBREW_PREFIX@@', '/opt/homebrew').replace('@@HOMEBREW_CELLAR@@', '/opt/homebrew/Cellar')
    options = []
    identity = subprocess.check_output(['otool', '-D', str(path)], text=True).splitlines()
    old_id = identity[1].strip() if len(identity) > 1 else ''
    if relocate(old_id) != old_id:
        options.extend(['-id', relocate(old_id)])
    for line in subprocess.check_output(['otool', '-L', str(path)], text=True).splitlines()[1:]:
        old = line.strip().split(' (compatibility')[0]
        if old != old_id and relocate(old) != old:
            options.extend(['-change', old, relocate(old)])
    for old in re.findall(r'path (\S+) \(offset', subprocess.check_output(['otool', '-l', str(path)], text=True)):
        if relocate(old) != old:
            options.extend(['-rpath', old, relocate(old)])
    if not options:
        raise RuntimeError('No declared Homebrew placeholder explains binary difference')
    subprocess.run(['install_name_tool', *options, str(path)], capture_output=True, check=True)
    subprocess.run(['codesign', '--force', '--sign', '-', '--pagesize', '4096', str(path)], capture_output=True, check=True)
    return options


def run(output, archives_from=None):
    output.mkdir(parents=True, exist_ok=False)
    receipt = dict(scope=__doc__, release_ready=False, independently_signed=False, success=False,
                   reused_download_archives=archives_from is not None,
                   toolchain_script_sha256=sha(Path(__file__).resolve()),
                   path_helper_sha256=sha(Path(audit_release_artifacts.__file__).resolve()),
                   redirect_policy_sha256=sha(Path(__file__).with_name('redirect_validation.py').resolve()),
                   xml_policy_sha256=sha(Path(__file__).with_name('xml_safety.py').resolve()))
    try:
        gradle_home = selected_toolchain_path('GRADLE_USER_HOME', '.local/toolchain/gradle-home')
        sdk = selected_toolchain_path('ANDROID_HOME', '.local/android-sdk')
        jdk_home = selected_toolchain_path('JAVA_HOME', '.local/toolchain/temurin-jdk25/Contents/Home')
        properties = (ROOT / 'gradle/wrapper/gradle-wrapper.properties').read_text().splitlines()
        urls = [line.partition('=')[2].replace(r'\:', ':')
                for line in properties if line.startswith('distributionUrl=')]
        match = re.fullmatch(r'https://services[.]gradle[.]org/distributions/gradle-([0-9]+[.][0-9]+[.][0-9]+)-bin[.]zip',
                             urls[0]) if len(urls) == 1 else None
        if match is None:
            raise ValueError('Expected one canonical stable Gradle binary distribution URL')
        version = match.group(1)
        wrapper_url = f'https://services.gradle.org/distributions/gradle-{version}-wrapper.jar.sha256'
        wrapper_expected = published_checksum(wrapper_url, output / 'wrapper.sha256')
        wrapper_actual = sha(ROOT / 'gradle/wrapper/gradle-wrapper.jar')
        receipt['wrapper'] = dict(official_matching_hash_reference=version, url=wrapper_url,
                                   expected=wrapper_expected, actual=wrapper_actual, matches=wrapper_actual == wrapper_expected,
                                   execution_distribution=version)
        distribution_url = urls[0] + '.sha256'
        expected = published_checksum(distribution_url, output / 'gradle.sha256')
        pins = [line.partition('=')[2] for line in properties if line.startswith('distributionSha256Sum=')]
        receipt['distribution_pin'] = dict(url=distribution_url, sha256=expected, pin_matches=pins == [expected])
        distribution = output / f'gradle-{version}-bin.zip'
        if archives_from is not None:
            distribution.symlink_to((archives_from / distribution.name).resolve())
        else:
            fetch(urls[0], distribution)
        if sha(distribution) != expected:
            raise RuntimeError('Gradle distribution checksum mismatch')
        caches = list((gradle_home / f'wrapper/dists/gradle-{version}-bin').glob(f'*/gradle-{version}'))
        if len(caches) != 1:
            raise RuntimeError('Exact installed Gradle distribution required')
        receipt['gradle_distribution'] = zip_comparison(distribution, caches[0])
        repository = output / 'sdk-repository.xml'
        fetch('https://dl.google.com/android/repository/repository2-3.xml', repository)
        root = parse_xml(repository.read_bytes())
        receipt['sdk_repository_sha256'] = sha(repository)
        receipt['sdk'] = []
        targets = {'build-tools;36.0.0': sdk / 'build-tools/36.0.0',
                   'platforms;android-36': sdk / 'platforms/android-36',
                   'platforms;android-37.1': sdk / 'platforms/android-37.1',
                   'platform-tools': sdk / 'platform-tools',
                   'cmdline-tools;19.0': sdk / 'cmdline-tools/19.0'}
        for package, installed in targets.items():
            nodes = [p for p in root.findall('remotePackage') if p.get('path') == package]
            if len(nodes) != 1:
                raise RuntimeError('Exact SDK package not found: ' + package)
            node = nodes[0]
            archives = [a.find('complete') for a in node.findall('archives/archive')
                        if a.findtext('host-os') in (None, 'macosx') and
                        a.findtext('host-arch') in (None, 'aarch64')]
            if len(archives) != 1:
                raise RuntimeError('Ambiguous SDK archive: ' + package)
            complete = archives[0]
            archive_name = sdk_archive_name(complete.findtext('url'))
            archive = output / archive_name
            url = 'https://dl.google.com/android/repository/' + archive_name
            if archives_from is not None:
                archive.symlink_to((archives_from / archive.name).resolve())
            else:
                fetch(url, archive)
            checksum = complete.find('checksum')
            actual = sha(archive, checksum.get('type'))
            if actual != checksum.text or archive.stat().st_size != int(complete.findtext('size')):
                raise RuntimeError('SDK archive checksum/size mismatch: ' + package)
            row = dict(package=package, url=url, repository_checksum_algorithm=checksum.get('type'),
                       repository_checksum=actual, computed_sha256=sha(archive),
                       comparison=zip_comparison(archive, installed))
            licence = next((e for e in node if e.tag.endswith('uses-license')), None)
            row['licence_id'] = licence.get('ref') if licence is not None else None
            receipt['sdk'].append(row)
        jdk_name = TEMURIN25_ARCHIVE
        jdk_url = ('https://github.com/adoptium/temurin25-binaries/releases/download/'
                   'jdk-25.0.5.1%2B2/' + jdk_name)
        checksum_file = output / 'temurin25.sha256'
        fetch(jdk_url + '.sha256.txt', checksum_file)
        if checksum_file.read_text().strip().split() != [
                TEMURIN25_SHA256, jdk_name]:
            raise RuntimeError('Temurin publisher checksum differs from the pinned release')
        jdk_archive = output / jdk_name
        if archives_from is not None:
            jdk_archive.symlink_to((archives_from / jdk_name).resolve())
        else:
            fetch(jdk_url, jdk_archive)
        if sha(jdk_archive) != TEMURIN25_SHA256:
            raise RuntimeError('Temurin archive checksum mismatch')
        if jdk_home.name != 'Home' or jdk_home.parent.name != 'Contents':
            raise RuntimeError('Expected a macOS JDK bundle JAVA_HOME')
        jdk_bundle = jdk_home.parent.parent.resolve()
        comparison = temurin_comparison(jdk_archive, jdk_bundle)
        if not comparison['all_jdk_files_match']:
            raise RuntimeError('Installed Temurin bundle differs from publisher archive')
        subprocess.run(['/usr/bin/codesign', '--verify', '--deep', '--strict', str(jdk_bundle)],
                       capture_output=True, text=True, check=True)
        identity = subprocess.run(['/usr/bin/codesign', '-dv', '--verbose=4', str(jdk_bundle)],
                                  capture_output=True, text=True, check=True).stderr
        if 'TeamIdentifier=JCDTMS22B4' not in identity.splitlines():
            raise RuntimeError('Temurin bundle signature has an unexpected publisher')
        receipt['jdk'] = dict(url=jdk_url, archive_sha256=sha(jdk_archive), java_home=str(jdk_home),
                               bundle_resource_seal_qualified=True, all_jdk_payloads_match=True,
                               **comparison)
        receipt['success'] = receipt['wrapper']['matches'] and receipt['distribution_pin']['pin_matches'] and receipt['gradle_distribution']['all_archive_files_match'] and all(
            s['comparison']['all_archive_files_match'] for s in receipt['sdk']) and receipt['jdk']['all_jdk_payloads_match']
    except Exception as error:
        receipt['error'] = str(error)
        raise
    finally:
        (output / 'receipt.json').write_text(json.dumps(receipt, indent=2) + '\n')
    return receipt


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--output', type=Path, required=True)
    parser.add_argument('--archives-from', type=Path, help='Reuse retained downloads; official checksums and every installed payload are rechecked')
    args = parser.parse_args()
    receipt = run(args.output.resolve(), args.archives_from.resolve() if args.archives_from else None)
    summary = {k: v for k, v in receipt.items() if k not in ('sdk', 'jdk', 'gradle_distribution')}
    summary['jdk_bundle_resource_seal_qualified'] = receipt.get('jdk', {}).get('bundle_resource_seal_qualified', False)
    print(json.dumps(summary, indent=2))
    if not receipt['success']:
        raise SystemExit(1)
