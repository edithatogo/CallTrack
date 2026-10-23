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
import re
from pathlib import Path
import urllib.request
from urllib.parse import urljoin, urlsplit
import xml.etree.ElementTree as ET
import zipfile

LIMIT = 64 * 1024 * 1024


def read_publication(url):
    if not url.startswith('https://'):
        raise ValueError('HTTPS publication required')
    with urllib.request.urlopen(url, timeout=60) as response:
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
    root = ET.fromstring(data)
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
    root = ET.fromstring(data)
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


def audit(inventory, output):
    source = inventory.read_bytes()
    rows = json.loads(source)['artifacts']
    unique = {(r['coordinate'], r['name'], r['sha256']): r for r in rows}
    output.mkdir(parents=True, exist_ok=False)

    def check(item):
        number, row = item
        result = {key: row[key] for key in ('coordinate', 'name', 'sha256')}
        try:
            artifact_url = repository_url(row)
            group, module, version = row['coordinate'].split(':')
            if not row['name'].startswith(module + '-' + version):
                module_url = repository_url(row, pom=True)[:-4] + '.module'
                metadata = read_publication(module_url)
                (output / (str(number) + '-module')).write_bytes(metadata)
                result['module_url'] = module_url
                result['module_sha256'] = digest(metadata)
                artifact_url = module_artifact_url(row, json.loads(metadata))
            for kind in ('artifact', 'pom'):
                url = artifact_url if kind == 'artifact' else repository_url(row, pom=True)
                data = read_publication(url)
                (output / (str(number) + '-' + kind)).write_bytes(data)
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
    (output / 'receipt.json').write_text(json.dumps(receipt, indent=2) + '\n')
    return receipt


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--inventory', type=Path, required=True)
    parser.add_argument('--output', type=Path, required=True)
    args = parser.parse_args()
    receipt = audit(args.inventory, args.output)
    print(json.dumps(receipt, indent=2))
    if not receipt['all_repository_bytes_match']:
        raise SystemExit(1)
