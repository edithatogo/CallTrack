#!/usr/bin/env -S uv run python
"""Prepare a local source/licence/notices candidate; never sign, install or publish.

Requires a clean committed source, matching successful unsigned builds, freshly
resolved runtime inventory and checksum-bound published dependency source archives.
It is not independent-host or from-source dependency reproduction.
"""
import argparse
import hashlib
import io
import json
from pathlib import Path, PurePosixPath
import re
import subprocess
import tarfile
import zipfile

ROOT = Path(__file__).resolve().parents[1]
AUDIT = 'conductor/tracks/release_20261020/canonical-runtime-audit-20261021.json'
SOURCES = 'qualification/release/runtime-sources.json'


def digest(data):
    return hashlib.sha256(data).hexdigest()


def safe_path(name):
    path = PurePosixPath(name)
    if (not name or '\\' in name or path.is_absolute() or
            any(part in ('..', '.local', '.git') for part in path.parts) or
            any(part.lower().endswith(('.jks', '.keystore', '.p12', '.pfx', '.key', '.pem', '.apk', '.aab')) for part in path.parts)):
        raise ValueError('Private or unsafe source archive path: ' + name)
    return path


def checked_file(root, name, expected):
    relative = safe_path(name)
    path = root.joinpath(*relative.parts)
    if path.is_symlink() or not path.resolve().is_relative_to(root.resolve()):
        raise ValueError('External/symlinked source cache entry')
    data = path.read_bytes()
    if digest(data) != expected:
        raise ValueError('Source cache checksum mismatch: ' + name)
    return data


def validate_inventory(actual, expected):
    def keys(rows):
        return {(r['coordinate'], r['name'], r['sha256']) for r in rows}
    if not actual or keys(actual) != keys(expected):
        raise ValueError('Candidate runtime inventory differs from audited binary set')


def validate_source_zip(data):
    with zipfile.ZipFile(io.BytesIO(data)) as archive:
        members = archive.infolist()
        if len(members) > 100000 or sum(m.file_size for m in members) > 512 * 1024 * 1024:
            raise ValueError('Source archive expansion limit exceeded')
        for member in members:
            safe_path(member.filename)
            if (member.external_attr >> 16) & 0o170000 == 0o120000:
                raise ValueError('Symlink in dependency source archive')
        if not any(m.filename.endswith(('.java', '.kt')) for m in members):
            raise ValueError('No Java/Kotlin source in dependency archive')
        if archive.testzip() is not None:
            raise ValueError('Corrupt dependency source archive')


def write_archive(output, files):
    """Deterministic metadata, exact file bytes, no filesystem traversal or links."""
    with output.open('xb') as stream, tarfile.open(fileobj=stream, mode='w') as archive:
        for name, source in sorted(files.items()):
            safe_path(name)
            if isinstance(source, Path):
                if source.is_symlink() or not source.is_file():
                    raise ValueError('Archive inputs must be regular files')
                data = source.read_bytes()
            else:
                data = source
            info = tarfile.TarInfo(name)
            info.size = len(data)
            info.mode = 0o755 if name.endswith(('/gradlew', '.sh')) else 0o644
            info.mtime = 0
            archive.addfile(info, io.BytesIO(data))


def source_snapshot(revision):
    data = subprocess.check_output(['git', 'archive', '--format=tar', revision], cwd=ROOT)
    files = {}
    with tarfile.open(fileobj=io.BytesIO(data)) as archive:
        for member in archive:
            safe_path(member.name)
            if member.isdir():
                continue
            if not member.isfile():
                raise ValueError('Git source snapshot contains a link/special entry')
            files['source/' + member.name] = archive.extractfile(member).read()
    return files


def fetch_sources(cache):
    from audit_release_artifacts import read_publication
    rows = json.loads((ROOT / SOURCES).read_text())['artifacts']
    for row in rows:
        for kind in ('source', 'pom'):
            name, expected = row[kind + '_file'], row[kind + '_sha256']
            path = cache.joinpath(*safe_path(name).parts)
            if not path.resolve().is_relative_to(cache.resolve()):
                raise ValueError('External source-cache parent')
            if not path.exists():
                data = read_publication(row[kind + '_url'])
                if digest(data) != expected:
                    raise ValueError('Published source/POM differs from reviewed checksum')
                path.parent.mkdir(parents=True, exist_ok=True)
                if not path.parent.resolve().is_relative_to(cache.resolve()):
                    raise ValueError('External source-cache parent')
                with path.open('xb') as stream:
                    stream.write(data)
            checked_file(cache, name, expected)
    return {'source_count': len(rows), 'success': True}


def run(output, build_receipt, inventory, source_cache):
    if subprocess.check_output(['git', 'status', '--porcelain'], cwd=ROOT).strip():
        raise ValueError('Commit all source/notice changes before packaging')
    revision = subprocess.check_output(['git', 'rev-parse', 'HEAD'], cwd=ROOT, text=True).strip()
    build = json.loads(build_receipt.read_text())
    if (not build.get('success') or build.get('revision') != revision or
            build.get('signed') is not False or len(build.get('builds', [])) != 2):
        raise ValueError('Two successful matching-source unsigned build receipts required')
    actual = json.loads(inventory.read_text())['artifacts']
    audit = json.loads((ROOT / AUDIT).read_text())
    validate_inventory(actual, audit['artifacts'])
    sources = json.loads((ROOT / SOURCES).read_text())['artifacts']
    expected = {r['coordinate']: r['sha256'] for r in audit['artifacts']}
    if len(sources) != len(expected) or {r['coordinate'] for r in sources} != set(expected):
        raise ValueError('Dependency source coverage is incomplete/duplicated')
    files = source_snapshot(revision)
    files['SOURCE_REVISION.txt'] = (revision + '\n').encode()
    for row in sources:
        if row['runtime_sha256'] != expected[row['coordinate']]:
            raise ValueError('Dependency source row does not bind audited runtime binary')
        data = checked_file(source_cache, row['source_file'], row['source_sha256'])
        validate_source_zip(data)
        files['dependency-sources/' + row['source_file']] = data
        files['dependency-sources/' + row['pom_file']] = checked_file(source_cache, row['pom_file'], row['pom_sha256'])
    prefix = 'source/app/src/main/assets/open_source/'
    legal = {name[len(prefix):]: data for name, data in files.items() if name.startswith(prefix)}
    required = {'GPL-3.0.txt', 'Apache-2.0.txt', 'NOTICE.md', 'THIRD_PARTY_NOTICES.md', 'retained-index.json'}
    if not required.issubset(legal):
        raise ValueError('Required licence/notices payload missing from committed source')
    for name, data in legal.items():
        files['licences/' + name] = data
    prefix = 'source/qualification/release/licences/'
    for name, data in list(files.items()):
        if name.startswith(prefix):
            files['licences/build-tools/' + name[len(prefix):]] = data
    files['dependency-sources/manifest.json'] = (ROOT / SOURCES).read_bytes()
    files['candidate-runtime-inventory.json'] = inventory.read_bytes()
    files['README.txt'] = (ROOT / 'qualification/release/BUNDLE_README.txt').read_bytes()
    # Correspondence does not depend on local checkout paths or private build logs.
    apk_hashes = []
    for record in build['builds']:
        if record.get('exit_code') != 0 or record.get('signature_check', {}).get('unsigned_verified') is not True:
            raise ValueError('Build is failed or unsigned status was not verified')
        apk = Path(record['apk'])
        if digest(apk.read_bytes()) != record['apk_sha256']:
            raise ValueError('Candidate APK changed after build receipt')
        apk_hashes.append(record['apk_sha256'])
        with zipfile.ZipFile(apk) as archive:
            for name, data in legal.items():
                if archive.read('assets/open_source/' + name) != data:
                    raise ValueError('APK omitted/changed a retained licence or notice')
    if apk_hashes[0] != apk_hashes[1]:
        raise ValueError('Candidate APK comparison failed')
    receipt = {'revision': revision, 'scope': __doc__, 'success': False,
               'release_ready': False, 'publication_performed': False,
               'independent_environment_reproduction': False,
               'dependency_from_source_rebuild_qualified': False,
               'runtime_source_count': len(sources), 'unsigned_apk_sha256': apk_hashes[0],
               'build_receipt_sha256': digest(build_receipt.read_bytes()),
               'runtime_inventory_sha256': digest(inventory.read_bytes())}
    output.mkdir(parents=True, exist_ok=False)
    try:
        manifest = {name: digest(data) for name, data in sorted(files.items())}
        files['MANIFEST.sha256.json'] = (json.dumps(manifest, indent=2) + '\n').encode()
        first = output / 'calltrack-source-and-notices.tar'
        second = output / 'repeatability-check.tar'
        write_archive(first, files)
        write_archive(second, files)
        if digest(first.read_bytes()) != digest(second.read_bytes()):
            raise ValueError('Source archive repeatability failed')
        with tarfile.open(first) as archive:
            if set(archive.getnames()) != set(files):
                raise ValueError('Source bundle manifest/member mismatch')
            for name, expected_hash in manifest.items():
                if digest(archive.extractfile(name).read()) != expected_hash:
                    raise ValueError('Source bundle readback mismatch')
        receipt.update(success=True, archive_sha256=digest(first.read_bytes()),
                       file_count=len(files), archive_readback_verified=True,
                       repeatability_verified=True, manifest=manifest)
        second.unlink()
    except Exception as error:
        receipt['error'] = str(error)
        raise
    finally:
        (output / 'receipt.json').write_text(json.dumps(receipt, indent=2) + '\n')
    return receipt


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--fetch-sources', type=Path, help='Download/check the pinned source/POM cache only; no builds or publication')
    parser.add_argument('--output', type=Path)
    parser.add_argument('--build-receipt', type=Path)
    parser.add_argument('--runtime-inventory', type=Path)
    parser.add_argument('--source-cache', type=Path)
    args = parser.parse_args()
    if args.fetch_sources:
        if any((args.output, args.build_receipt, args.runtime_inventory, args.source_cache)):
            parser.error('--fetch-sources is a separate cache-only mode')
        print(json.dumps(fetch_sources(args.fetch_sources.resolve()), indent=2))
        raise SystemExit(0)
    if not all((args.output, args.build_receipt, args.runtime_inventory, args.source_cache)):
        parser.error('Packaging needs --output, --build-receipt, --runtime-inventory and --source-cache')
    result = run(args.output.resolve(), args.build_receipt.resolve(), args.runtime_inventory.resolve(), args.source_cache.resolve())
    print(json.dumps({k:v for k,v in result.items() if k != 'manifest'}, indent=2))
