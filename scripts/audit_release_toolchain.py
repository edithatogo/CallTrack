#!/usr/bin/env -S uv run python
"""Read-only comparison against official Gradle/SDK/Homebrew archive evidence.

Never installs tools, accepts licences or treats HTTPS checksums as independent signatures.
Success qualifies payload-origin comparison only; JDK bundle-resource sealing is separate.
"""
import argparse
import hashlib
import json
from pathlib import Path
import re
import subprocess
import struct
import tarfile
import urllib.request
import xml.etree.ElementTree as ET
import zipfile

ROOT = Path(__file__).resolve().parents[1]


def sha(path, algorithm='sha256'):
    hasher = hashlib.new(algorithm)
    with path.open('rb') as stream:
        for block in iter(lambda: stream.read(1024 * 1024), b''):
            hasher.update(block)
    return hasher.hexdigest()


def fetch(url, path):
    if not url.startswith('https://'):
        raise ValueError('HTTPS required')
    with urllib.request.urlopen(url, timeout=120) as response, path.open('xb') as stream:
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


def zip_comparison(archive, installed):
    rows = []
    with zipfile.ZipFile(archive) as bundle:
        for member in bundle.infolist():
            if member.is_dir():
                continue
            parts = Path(member.filename).parts
            if len(parts) < 2 or '..' in parts or member.filename.startswith('/'):
                raise ValueError('Unsafe archive path')
            relative = Path(*parts[1:])
            actual = installed / relative
            expected = hashlib.sha256(bundle.read(member)).hexdigest()
            rows.append(dict(path=str(relative), archive_sha256=expected,
                             installed_sha256=sha(actual) if actual.is_file() else None))
    return dict(files=rows, all_archive_files_match=bool(rows) and all(
        r['archive_sha256'] == r['installed_sha256'] for r in rows))


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
                   reused_download_archives=archives_from is not None)
    try:
        wrapper_url = 'https://services.gradle.org/distributions/gradle-7.0.2-wrapper.jar.sha256'
        wrapper_expected = published_checksum(wrapper_url, output / 'wrapper.sha256')
        wrapper_actual = sha(ROOT / 'gradle/wrapper/gradle-wrapper.jar')
        receipt['wrapper'] = dict(official_matching_hash_reference='7.0.2 (shared bootstrap hash; not inferred from distribution URL)', url=wrapper_url,
                                   expected=wrapper_expected, actual=wrapper_actual, matches=wrapper_actual == wrapper_expected,
                                   execution_distribution='8.13')
        distribution_url = 'https://services.gradle.org/distributions/gradle-8.13-bin.zip.sha256'
        expected = published_checksum(distribution_url, output / 'gradle.sha256')
        properties = (ROOT / 'gradle/wrapper/gradle-wrapper.properties').read_text()
        receipt['distribution_pin'] = dict(url=distribution_url, sha256=expected,
                                           pin_matches=('distributionSha256Sum=' + expected) in properties)
        distribution = output / 'gradle-8.13-bin.zip'
        if archives_from is not None:
            distribution.symlink_to((archives_from / distribution.name).resolve())
        else:
            fetch('https://services.gradle.org/distributions/gradle-8.13-bin.zip', distribution)
        if sha(distribution) != expected:
            raise RuntimeError('Gradle distribution checksum mismatch')
        caches = list((Path.home() / '.gradle/wrapper/dists/gradle-8.13-bin').glob('*/gradle-8.13'))
        if len(caches) != 1:
            raise RuntimeError('Exact installed Gradle distribution required')
        receipt['gradle_distribution'] = zip_comparison(distribution, caches[0])
        repository = output / 'sdk-repository.xml'
        fetch('https://dl.google.com/android/repository/repository2-3.xml', repository)
        root = ET.fromstring(repository.read_bytes())
        receipt['sdk_repository_sha256'] = sha(repository)
        receipt['sdk'] = []
        sdk = (ROOT / '.local/android-sdk').resolve()
        targets = {'build-tools;35.0.0': sdk / 'build-tools/35.0.0',
                   'platforms;android-36': sdk / 'platforms/android-36',
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
            archive = output / complete.findtext('url')
            url = 'https://dl.google.com/android/repository/' + complete.findtext('url')
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
        formula_file = output / 'openjdk17-formula.json'
        fetch('https://formulae.brew.sh/api/formula/openjdk@17.json', formula_file)
        formula = json.loads(formula_file.read_text())
        bottles = list((Path.home() / 'Library/Caches/Homebrew/downloads').glob('*openjdk@17--17.0.21.arm64_golden_gate.bottle.tar.gz'))
        if len(bottles) != 1:
            raise RuntimeError('Exact installed JDK bottle cache required')
        bottle = bottles[0]
        record = formula['bottle']['stable']['files']['arm64_golden_gate']
        actual = sha(bottle)
        if actual != record['sha256']:
            raise RuntimeError('JDK bottle differs from current official formula evidence')
        files = []
        with tarfile.open(bottle) as archive:
            for member in archive:
                if not member.isfile() or '/libexec/openjdk.jdk/' not in member.name:
                    continue
                relative = member.name.split('/libexec/openjdk.jdk/', 1)[1]
                installed = Path('/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk') / relative
                expected_file = hashlib.sha256(archive.extractfile(member).read()).hexdigest()
                row = dict(path=relative, archive_sha256=expected_file,
                           installed_sha256=sha(installed) if installed.is_file() else None)
                if row['installed_sha256'] != expected_file:
                    copy = output / 'jdk-relocation' / relative
                    copy.parent.mkdir(parents=True, exist_ok=True)
                    copy.write_bytes(archive.extractfile(member).read())
                    row['reproduced_relocation_options'] = reproduce_relocation(copy)
                    row['reproduced_sha256'] = sha(copy)
                    if row['reproduced_sha256'] != row['installed_sha256']:
                        row['signature_check'] = verify_jdk_signature(installed, relative)
                        subprocess.run(['codesign', '--verify', '--strict', str(copy)], capture_output=True, check=True)
                        row['reproduced_payload_sha256'] = macho_payload(copy.read_bytes())
                        row['installed_payload_sha256'] = macho_payload(installed.read_bytes())
                        row['signature_allocation_only'] = row['reproduced_payload_sha256'] == row['installed_payload_sha256']
                files.append(row)
        receipt['jdk'] = dict(formula_url='https://formulae.brew.sh/api/formula/openjdk@17.json',
                               formula_sha256=sha(formula_file), bottle_sha256=actual,
                               licence=formula['license'], files=files,
                               bundle_resource_seal_qualified=all(r.get('signature_check', {}).get('strict_valid', True) for r in files),
                               all_jdk_files_match=bool(files) and all(r.get('reproduced_sha256', r['archive_sha256']) == r['installed_sha256'] for r in files),
                               all_jdk_payloads_match=bool(files) and all(r.get('reproduced_sha256', r['archive_sha256']) == r['installed_sha256'] or r.get('signature_allocation_only') is True for r in files))
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
