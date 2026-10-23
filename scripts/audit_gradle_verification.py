#!/usr/bin/env -S uv run --python 3.15.1 --no-project python
"""Independently check generated Gradle trust entries against canonical repository evidence.

Generation alone is not approval. Every checksum must match fresh repository bytes
or an already completed source-bound canonical binary audit.
"""
import argparse
from concurrent.futures import ThreadPoolExecutor
import hashlib
import json
from pathlib import Path
import re
from urllib.error import HTTPError
import xml.etree.ElementTree as ET
from audit_release_artifacts import module_artifact_url, repository_url, read_publication


def rows(data):
    root = ET.fromstring(data)
    if root.findtext('./{*}configuration/{*}verify-metadata') != 'true':
        raise ValueError('Repository metadata verification must be enabled')
    if root.findall('.//{*}trust') or root.findall('.//{*}trusted-artifact'):
        raise ValueError('Broad artifact trust exceptions are forbidden')
    result = []
    for component in root.findall('./{*}components/{*}component'):
        parts = [component.get(k, '') for k in ('group', 'name', 'version')]
        if not all(re.fullmatch('[A-Za-z0-9_.+-]+', value) for value in parts):
            raise ValueError('Unsafe component identity')
        for artifact in component.findall('./{*}artifact'):
            if not re.fullmatch('[A-Za-z0-9_.+-]+', artifact.get('name', '')):
                raise ValueError('Unsafe verification artifact filename')
            hashes = [node.get('value', '') for node in artifact.findall('./{*}sha256')]
            if not hashes or not all(re.fullmatch('[a-f0-9]{64}', value) for value in hashes):
                raise ValueError('Every artifact needs exact SHA256 evidence')
            result.append(dict(coordinate=':'.join(parts), name=artifact.get('name'), sha256=sorted(set(hashes))))
    if not result:
        raise ValueError('Empty verification policy')
    return result


def module_fallback_url(row, expected_hash, metadata_bytes, allowed_hashes):
    """Resolve a Gradle variant's renamed artifact only through trusted .module bytes."""
    module_name = row['coordinate'].split(':')[1]
    version = row['coordinate'].split(':')[2]
    metadata_name = f'{module_name}-{version}.module'
    actual_metadata_hash = hashlib.sha256(metadata_bytes).hexdigest()
    if actual_metadata_hash not in allowed_hashes.get((row['coordinate'], metadata_name), set()):
        raise ValueError('Canonical module metadata hash is absent from the verification policy')
    bound_row = dict(row, sha256=expected_hash)
    return module_artifact_url(bound_row, json.loads(metadata_bytes))


def audit(metadata, canonical, output):
    data = metadata.read_bytes()
    policy = rows(data)
    prior = canonical.read_bytes()
    binary_audit = json.loads(prior)
    if not binary_audit.get('all_repository_bytes_match'):
        raise ValueError('Successful canonical audit required')
    known = {}
    for row in binary_audit['artifacts']:
        for name in (row['name'], row['artifact_url'].rsplit('/', 1)[-1]):
            known[(row['coordinate'], name)] = row
    output.mkdir(parents=True, exist_ok=False)

    allowed_hashes = {(row['coordinate'], row['name']): set(row['sha256']) for row in policy}

    def check(item):
        index, row = item
        proof = dict(row)
        previous = known.get((row['coordinate'], row['name']))
        if previous:
            proof['matches'] = row['sha256'] == [previous['sha256']]
            proof['evidence'] = 'completed-canonical-binary-audit'
            return proof
        primary = repository_url(dict(row, sha256='unused'))
        relative = primary.split('/maven2/', 1)[1]
        urls = [primary] + [base + relative for base in (
            'https://dl.google.com/dl/android/maven2/', 'https://repo.maven.apache.org/maven2/') if not primary.startswith(base)]
        matched = set()
        proof['downloads'] = []
        module_bases = [primary.split('/maven2/', 1)[0] + '/maven2/']
        module_bases.extend(base for base in (
            'https://dl.google.com/dl/android/maven2/', 'https://repo.maven.apache.org/maven2/')
            if base not in module_bases)
        for attempt, url in enumerate(urls):
            try:
                content = read_publication(url)
                actual = hashlib.sha256(content).hexdigest()
                (output / f'{index}-{attempt}').write_bytes(content)
                proof['downloads'].append(dict(url=url, sha256=actual))
                if actual in row['sha256']:
                    matched.add(actual)
                if matched == set(row['sha256']):
                    break
            except HTTPError as error:
                proof['downloads'].append(dict(url=url, error=str(error)))
                if error.code != 404:
                    continue
                for module_base in module_bases:
                    group, module, version = row['coordinate'].split(':')
                    module_url = (module_base + group.replace('.', '/') + '/' + module + '/' +
                                  version + '/' + module + '-' + version + '.module')
                    try:
                        module_bytes = read_publication(module_url)
                        module_hashes = allowed_hashes.get((row['coordinate'], module_url.rsplit('/', 1)[-1]), set())
                        actual_url = module_fallback_url(row, row['sha256'][0], module_bytes, allowed_hashes)
                        artifact_bytes = read_publication(actual_url)
                        actual_hash = hashlib.sha256(artifact_bytes).hexdigest()
                        proof['downloads'].append(dict(url=module_url, sha256=hashlib.sha256(module_bytes).hexdigest()))
                        proof['downloads'].append(dict(url=actual_url, sha256=actual_hash))
                        if actual_hash in row['sha256'] and module_hashes:
                            matched.add(actual_hash)
                        if matched == set(row['sha256']):
                            break
                    except Exception as fallback_error:
                        proof['downloads'].append(dict(url=module_base, error=str(fallback_error)))
                if matched == set(row['sha256']):
                    break
            except Exception as error:
                proof['downloads'].append(dict(url=url, error=str(error)))
        proof['matches'] = matched == set(row['sha256'])
        return proof

    with ThreadPoolExecutor(max_workers=4) as pool:
        results = list(pool.map(check, enumerate(policy)))
    receipt = dict(scope=__doc__, metadata_sha256=hashlib.sha256(data).hexdigest(),
                   canonical_audit_sha256=hashlib.sha256(prior).hexdigest(),
                   entries=len(results), artifacts=results,
                   all_entries_verified=all(row['matches'] for row in results),
                   independent_signatures=False, release_ready=False)
    if metadata.read_bytes() != data:
        receipt['all_entries_verified'] = False
        receipt['error'] = 'Verification metadata changed during audit'
    (output / 'receipt.json').write_text(json.dumps(receipt, indent=2) + '\n')
    return receipt


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--metadata', type=Path, required=True)
    parser.add_argument('--canonical', type=Path, required=True)
    parser.add_argument('--output', type=Path, required=True)
    args = parser.parse_args()
    receipt = audit(args.metadata, args.canonical, args.output)
    print(json.dumps({key: value for key, value in receipt.items() if key != 'artifacts'}, indent=2))
    if not receipt['all_entries_verified']:
        raise SystemExit(1)
