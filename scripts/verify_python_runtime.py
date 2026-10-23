#!/usr/bin/env -S uv run python
"""Require the actual helper process to match this repo's selected local runtime."""
import json
from pathlib import Path
import sys

ROOT = Path(__file__).resolve().parents[1]


def selected_version(root):
    raw = (root / '.python-version').read_text().strip()
    parts = raw.split('.')
    if len(parts) != 3 or any(not part.isdigit() for part in parts):
        raise RuntimeError('Python version manifest must select a stable X.Y.Z release')
    version = tuple(map(int, parts))
    if '.'.join(map(str, version)) != raw:
        raise RuntimeError('Python version manifest must use canonical X.Y.Z form')
    return raw, version


def verify(root=ROOT, version=None, prefix=None, executable=None, releaselevel=None):
    root = Path(root).resolve()
    version = tuple(sys.version_info[:3] if version is None else version)
    prefix = Path(sys.prefix if prefix is None else prefix).resolve()
    executable = Path(sys.executable if executable is None else executable).absolute()
    selected, required = selected_version(root)
    if version != required:
        raise RuntimeError(f'Python {selected} selected; actual {version}')
    releaselevel = sys.version_info.releaselevel if releaselevel is None else releaselevel
    if releaselevel != 'final':
        raise RuntimeError('A qualified final Python release is required')
    expected = root / '.venv'
    if prefix != expected.resolve():
        raise RuntimeError(f'Use repository-local environment: {expected}')
    if executable.parent.resolve() != (expected / 'bin').resolve():
        raise RuntimeError(f'Use repository-local executable: {expected / "bin/python"}')
    return dict(version='.'.join(map(str, version)), executable=str(executable),
                prefix=str(prefix), repository_local=True)


if __name__ == '__main__':
    print(json.dumps(verify(), indent=2))
