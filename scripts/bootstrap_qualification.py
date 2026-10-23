#!/usr/bin/env -S uv run --python 3.15.1 --no-project python
"""Fetch the audited public upstream revision into the excluded reference checkout."""
from pathlib import Path
import subprocess

ROOT = Path(__file__).resolve().parents[1]
URL = 'https://github.com/MarkoBL/AndroidCallLogSync.git'
REVISION = 'aaa56443a294aa61bad48b620427e53f9521c5fe'


def bootstrap():
    target = ROOT / '.local/AndroidCallLogSync'
    if not target.exists():
        target.mkdir(parents=True)
        subprocess.run(['git', 'init', str(target)], check=True)
        subprocess.run(['git', 'remote', 'add', 'origin', URL], cwd=target, check=True)
        subprocess.run(['git', 'fetch', '--depth=1', 'origin', REVISION], cwd=target, check=True)
        subprocess.run(['git', 'checkout', '--detach', REVISION], cwd=target, check=True)
    actual = subprocess.check_output(['git', 'rev-parse', 'HEAD'], cwd=target, text=True).strip()
    if actual != REVISION or subprocess.check_output(['git', 'status', '--porcelain'], cwd=target, text=True):
        raise SystemExit('Reference checkout is not the clean audited revision; leaving it untouched')
    print('Audited upstream ready:', REVISION)


if __name__ == '__main__':
    bootstrap()
