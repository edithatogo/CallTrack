#!/usr/bin/env -S uv run python
"""Compare runtime inventory with fresh canonical HTTPS repository downloads.

Repository byte agreement and POM declarations are evidence, not legal clearance,
independent signatures, build-plugin verification or a complete SBOM.
"""
import argparse
from concurrent.futures import ThreadPoolExecutor
import hashlib
import io
import json
import os
import re
from pathlib import Path
import urllib.request
from urllib.parse import urljoin, urlsplit
import zipfile
from redirect_validation import ValidatingRedirectHandler
from xml_safety import parse_xml

LIMIT = 64 * 1024 * 1024
OUTPUT_ROOT = Path(__file__).resolve().parents[1] / '.local' / 'release-evidence'


def validate_publication_redirect(url):
    if urlsplit(url).scheme != 'https':
        raise ValueError('Insecure publication redirect')


PUBLICATION_OPENER = urllib.request.build_opener(ValidatingRedirectHandler(validate_publication_redirect))


def read_publication(url):
    if not url.startswith('https://'):
        raise ValueError('HTTPS publication required')
    with PUBLICATION_OPENER.open(url, timeout=60) as response:
        if not getattr(response, 'url', url).startswith('https://'):
            raise ValueError('Insecure publication redirect')
        data = response.read(LIMIT + 1)
    if len(data) > LIMIT:
        raise ValueError('Oversized publication')
    return data


def digest(data):
    return hashlib.sha256(data).hexdigest()


def repository_url(row, pom=False):
    group, module, version = row['coordinate'].split(':')
    base = 'https://dl.google.com/dl/android/maven2/' if group.startswith(('androidx.', 'com.android.', 'com.google.testing.platform')) or group in ('com.android', 'com.google.android.material') else 'https://repo.maven.apache.org/maven2/'
    name = module + '-' + version + '.pom' if pom else row['name']
    if '/' in name or '..' in name:
        raise ValueError('Unsafe artifact filename')
    return base + group.replace('.', '/') + '/' + module + '/' + version + '/' + name


def module_artifact_url(row, metadata):
    files = [file for variant in metadata.get('variants', []) for file in variant.get('files', [])
             if file.get('name') == row['name'] and file.get('sha256') == row['sha256']]
    urls = {file['url'] for file in files}
    if len(urls) != 1:
        raise ValueError('Module metadata must uniquely bind artifact name and SHA256')
    relative = urls.pop()
    if urlsplit(relative).scheme or '/' in relative or '..' in relative:
        raise ValueError('Unsafe module artifact URL')
    return urljoin(repository_url(row), relative)


def declarations(data):
    root = parse_xml(data)
    ns = {'m': 'http://maven.apache.org/POM/4.0.0'}
    return [{key: node.findtext('m:' + key, default='', namespaces=ns)
             for key in ('name', 'url', 'distribution')}
            for node in root.findall('m:licenses/m:license', ns)]


def inherited_licences(data, evidence, depth=0):
    direct = declarations(data)
    if direct:
        return direct
    if depth >= 8:
        raise ValueError('POM parent chain exceeds audit limit')
    root = parse_xml(data)
    ns = {'m': 'http://maven.apache.org/POM/4.0.0'}
    parent = root.find('m:parent', ns)
    if parent is None:
        return []
    coordinate = ':'.join(parent.findtext('m:' + key, default='', namespaces=ns)
                          for key in ('groupId', 'artifactId', 'version'))
    if not all(re.fullmatch(r'[A-Za-z0-9_.+-]+', part) for part in coordinate.split(':')):
        raise ValueError('Unresolved or unsafe parent coordinate')
    url = repository_url(dict(coordinate=coordinate, name='unused'), pom=True)
    if any(item['url'] == url for item in evidence):
        raise ValueError('Cyclic POM parent chain')
    parent_data = read_publication(url)
    evidence.append(dict(url=url, sha256=digest(parent_data)))
    return inherited_licences(parent_data, evidence, depth + 1)


def embedded_notices(data, depth=0):
    if depth > 1:
        raise ValueError('Unexpected nested classes.jar depth')
    rows = []
    with zipfile.ZipFile(io.BytesIO(data)) as archive:
        if len(archive.infolist()) > 100000 or sum(m.file_size for m in archive.infolist()) > 512 * 1024 * 1024:
            raise ValueError('Archive exceeds audit expansion bounds')
        for member in archive.infolist():
            if member.file_size > LIMIT:
                raise ValueError('Archive member exceeds audit limit')
            lower = member.filename.lower()
            if not member.is_dir() and re.fullmatch(r'(?:licen[sc]e|notice|copying|dependencies)(?:[._-].*)?', lower.rsplit('/', 1)[-1]) and not lower.endswith(('.class', '.java')):
                rows.append(dict(path=member.filename, sha256=digest(archive.read(member)), size=member.file_size))
            elif member.filename == 'classes.jar':
                rows.extend(dict(item, path='classes.jar!/' + item['path']) for item in embedded_notices(archive.read(member), depth + 1))
    return rows


def create_output_directory(output):
    """Create a fresh output below the local evidence root without following links."""
    root = os.path.realpath(OUTPUT_ROOT)
    resolved = os.path.realpath(output)
    if not resolved.startswith(root + os.sep):
        raise ValueError('Audit output must be inside the evidence root')
    if resolved != os.path.join(os.path.realpath(output.parent), output.name):
        raise FileExistsError('Audit output already exists as a link')

    OUTPUT_ROOT.mkdir(mode=0o700, parents=True, exist_ok=True)
    parts = Path(os.path.relpath(resolved, root)).parts
    flags = os.O_RDONLY | os.O_DIRECTORY | os.O_NOFOLLOW
    parent = os.open(root, flags)
    try:
        for part in parts[:-1]:
            try:
                os.mkdir(part, mode=0o700, dir_fd=parent)
            except FileExistsError:
                pass
            child = os.open(part, flags, dir_fd=parent)
            os.close(parent)
            parent = child
        os.mkdir(parts[-1], mode=0o700, dir_fd=parent)
        return os.open(parts[-1], flags, dir_fd=parent)
    finally:
        os.close(parent)


def audit(inventory, output):
    source = inventory.read_bytes()
    rows = json.loads(source)['artifacts']
    unique = {(r['coordinate'], r['name'], r['sha256']): r for r in rows}
    directory = create_output_directory(output)
    try:
        return _audit_output(source, rows, unique, output, directory)
    finally:
        os.close(directory)


def _audit_output(source, rows, unique, output, directory):
    def write_new(name, data):
        descriptor = os.open(name, os.O_WRONLY | os.O_CREAT | os.O_EXCL | os.O_NOFOLLOW,
                             0o600, dir_fd=directory)
        with os.fdopen(descriptor, 'wb') as stream:
            stream.write(data)

    def check(item):
        number, row = item
        result = {key: row[key] for key in ('coordinate', 'name', 'sha256')}
        try:
            artifact_url = repository_url(row)
            group, module, version = row['coordinate'].split(':')
            if not row['name'].startswith(module + '-' + version):
                module_url = repository_url(row, pom=True)[:-4] + '.module'
                metadata = read_publication(module_url)
                write_new(str(number) + '-module', metadata)
                result['module_url'] = module_url
                result['module_sha256'] = digest(metadata)
                artifact_url = module_artifact_url(row, json.loads(metadata))
            for kind in ('artifact', 'pom'):
                url = artifact_url if kind == 'artifact' else repository_url(row, pom=True)
                data = read_publication(url)
                write_new(str(number) + '-' + kind, data)
                result[kind + '_url'] = url
                result[kind + '_sha256'] = digest(data)
                if kind == 'artifact':
                    result['repository_bytes_match'] = digest(data) == row['sha256']
                    result['embedded_notices'] = embedded_notices(data)
                else:
                    result['declared_licences'] = declarations(data)
                    result['parent_pom_evidence'] = []
                    result['effective_licences'] = inherited_licences(data, result['parent_pom_evidence'])
        except Exception as error:
            result['error'] = str(error)
        return result

    with ThreadPoolExecutor(max_workers=4) as pool:
        results = list(pool.map(check, enumerate(unique.values())))
    receipt = dict(scope=__doc__, inventory_sha256=digest(source), resolved_rows=len(rows),
                   unique_artifacts=len(unique), artifacts=results,
                   all_repository_bytes_match=all(r.get('repository_bytes_match') is True and 'error' not in r for r in results),
                   licence_clearance=False, build_tool_trust=False, release_ready=False)
    def require_same_directory():
        try:
            current = output.stat(follow_symlinks=False)
        except OSError as error:
            raise RuntimeError('Audit output directory changed during the run') from error
        opened = os.fstat(directory)
        if (current.st_dev, current.st_ino) != (opened.st_dev, opened.st_ino):
            raise RuntimeError('Audit output directory changed during the run')

    require_same_directory()
    write_new('receipt.json', (json.dumps(receipt, indent=2) + '\n').encode())
    require_same_directory()
    return receipt


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--inventory', type=Path, required=True)
    parser.add_argument('--output', type=Path, required=True,
                        help='New directory under .local/release-evidence')
    args = parser.parse_args()
    receipt = audit(args.inventory, args.output)
    print(json.dumps(receipt, indent=2))
    if not receipt['all_repository_bytes_match']:
        raise SystemExit(1)
