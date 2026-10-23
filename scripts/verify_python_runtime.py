#!/usr/bin/env -S uv run --python 3.15.1 --no-project python
"""Fail closed unless the actual helper process uses the selected local runtime."""
import json
from pathlib import Path
import sys

ROOT = Path(__file__).resolve().parents[1]
REQUIRED = (3, 15, 1)


def verify(root=ROOT, version=None, prefix=None, executable=None, releaselevel=None):
    root = Path(root).resolve()
    version = tuple(sys.version_info[:3] if version is None else version)
    prefix = Path(sys.prefix if prefix is None else prefix).resolve()
    executable = Path(sys.executable if executable is None else executable).absolute()
    if (root / '.python-version').read_text().strip() != '3.15.1':
        raise RuntimeError('Python version manifest must select3.15.1')
    if version != REQUIRED:
        raise RuntimeError(f'Python3.15.1 required; actual {version}')
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
