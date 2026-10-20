#!/usr/bin/env python3
"""Check Conductor integrity; optionally verify locally retained mapping inputs.

This verifies documentation and evidence identities, not app behaviour.
"""
import argparse
import hashlib
import json
from pathlib import Path
import re
import subprocess

ROOT = Path(__file__).resolve().parents[1]


def verify(private_sources=False):
    conductor = ROOT / 'conductor'
    tracks = {}
    for area in ('tracks', 'archive'):
        for metadata in (conductor / area).glob('*/metadata.json'):
            data = json.loads(metadata.read_text())
            assert data['track_id'] not in tracks, f'Duplicate track: {metadata}'
            tracks[data['track_id']] = data
            for name in ('index.md', 'spec.md', 'plan.md'):
                assert (metadata.parent / name).exists(), (metadata, name)
    def walk(track_id, path):
        assert track_id not in path, f'Prerequisite cycle: {path + [track_id]}'
        assert track_id in tracks, f'Missing prerequisite: {track_id}'
        for dependency in tracks[track_id]['depends_on']:
            walk(dependency, path + [track_id])
    for track_id in tracks:
        walk(track_id, [])
    documents = list(conductor.rglob('*.md'))
    for document in documents:
        for link in re.findall(r'\]\(([^)]+)\)', document.read_text()):
            if not link.startswith(('https:', 'http:', '#')):
                target = document.parent / link.split('#')[0]
                assert target.exists(), f'Broken link: {document}: {link}'
    baseline = next(conductor.glob('*/baseline_20261020'))
    fixtures = json.loads((baseline / 'fixtures.json').read_text())
    assert fixtures['synthetic'] is True
    cases = fixtures['cases']
    assert len({case['id'] for case in cases}) == len(cases)
    requirements = (conductor / 'requirements.md').read_text()
    for case in cases:
        for field in ('id', 'requirement', 'scenario', 'input', 'expected'):
            assert field in case, (case, field)
        assert re.search(r'\b' + case['requirement'] + r'\b', requirements), case
    assert {'F07', 'F08', 'F10', 'F12', 'F18', 'F20'} <= {case['id'] for case in cases}
    tracked = subprocess.check_output(['git', 'ls-files'], cwd=ROOT, text=True).splitlines()
    assert not any(path.startswith('.local/') or path.endswith('.apk') for path in tracked)
    if private_sources:
        evidence = json.loads((baseline / 'sources.json').read_text())
        apk = ROOT / '.local/apk/CallTrack_1.12.apk'
        assert hashlib.sha256(apk.read_bytes()).hexdigest() == evidence['CallTrack']['sha256']
        for repo in evidence['repositories']:
            checkout = ROOT / '.local' / repo['name']
            revision = subprocess.check_output(['git', 'rev-parse', 'HEAD'], cwd=checkout, text=True).strip()
            assert revision == repo['commit'], repo['name']
            assert not subprocess.check_output(['git', 'status', '--porcelain'], cwd=checkout, text=True)
            for record in repo['files']:
                assert hashlib.sha256((checkout / record['path']).read_bytes()).hexdigest() == record['sha256'], record
    print(f'PASS: {len(documents)} documents, {len(tracks)} tracks, {len(cases)} synthetic scenarios; links, identities and prerequisites valid.'
          + (' Private APK and pinned source digests verified.' if private_sources else ' Private inputs not checked.'))


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--private-sources', action='store_true')
    verify(parser.parse_args().private_sources)
