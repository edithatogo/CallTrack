#!/usr/bin/env python3
"""Build/test the root development slice and bind native acceptance to exact inputs/APKs."""
import argparse
import hashlib
import json
import os
from pathlib import Path
import subprocess
import time

ROOT = Path(__file__).resolve().parents[1]


def inputs():
    paths = [ROOT / p for p in ('build.gradle', 'settings.gradle', 'gradle.properties', 'gradlew')]
    paths += sorted((ROOT / 'gradle/wrapper').glob('*'))
    paths += [ROOT / 'app/build.gradle', ROOT / 'app/proguard-rules.pro']
    paths += sorted(p for p in (ROOT / 'app/src').rglob('*') if p.is_file())
    return {str(p.relative_to(ROOT)): hashlib.sha256(p.read_bytes()).hexdigest() for p in paths}


def run(jdk, sdk):
    evidence = ROOT / '.local/slice-evidence'
    evidence.mkdir(parents=True, exist_ok=True)
    before = inputs()
    command = ['sh', 'gradlew', '--no-daemon', '--console=plain', ':app:testDebugUnitTest',
               ':app:assembleDebug', ':app:assembleDebugAndroidTest', ':app:lintDebug']
    start = time.monotonic()
    with (evidence / 'build.log').open('w') as stream:
        result = subprocess.run(command, cwd=ROOT, env=dict(os.environ, JAVA_HOME=str(jdk),
                                 ANDROID_HOME=str(sdk)), stdout=stream, stderr=subprocess.STDOUT)
    unchanged = before == inputs()
    artifacts = {str(p.relative_to(ROOT)): hashlib.sha256(p.read_bytes()).hexdigest()
                 for p in sorted((ROOT / 'app/build/outputs/apk').rglob('*.apk'))}
    receipt = {'exit_code': result.returncode, 'inputs_unchanged': unchanged, 'inputs': before,
               'artifacts': artifacts, 'command': command, 'jdk': str(jdk), 'sdk': str(sdk),
               'elapsed_seconds': round(time.monotonic() - start, 1)}
    (evidence / 'build-receipt.json').write_text(json.dumps(receipt, indent=2) + '\n')
    print(json.dumps({k: receipt[k] for k in ('exit_code', 'inputs_unchanged', 'elapsed_seconds')}))
    return result.returncode if result.returncode else (0 if unchanged else 1)


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--jdk', type=Path, required=True)
    parser.add_argument('--sdk', type=Path, required=True)
    args = parser.parse_args()
    raise SystemExit(run(args.jdk.resolve(), args.sdk.resolve()))
