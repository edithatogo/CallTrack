#!/usr/bin/env python3
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
import xml.etree.ElementTree as ET
from audit_release_artifacts import repository_url, read_publication


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
