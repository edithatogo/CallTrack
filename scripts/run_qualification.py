#!/usr/bin/env python3
"""Build a prepared qualification checkout and retain local command/artifact receipts."""
import argparse
import hashlib
import json
import os
from pathlib import Path
import subprocess
import time

ROOT = Path(__file__).resolve().parents[1]


def run(variant, jdk, sdk, clean=False):
    checkout = ROOT / '.local' / ('AndroidCallLogSync' if variant == 'upstream' else 'modern-qualification')
    evidence = ROOT / '.local/build-evidence'
    evidence.mkdir(parents=True, exist_ok=True)
    if not (jdk / 'bin/java').exists() or not (sdk / 'platforms').exists():
        raise SystemExit('Provide an installed JDK and Android SDK')
    reference = ROOT / '.local/AndroidCallLogSync'
    source_revision = subprocess.check_output(['git', 'rev-parse', 'HEAD'], cwd=reference, text=True).strip()
    if source_revision != 'aaa56443a294aa61bad48b620427e53f9521c5fe':
        raise SystemExit('Audited source revision mismatch')
    if subprocess.check_output(['git', 'status', '--porcelain'], cwd=reference, text=True):
        raise SystemExit('Reference checkout is dirty; refusing an ambiguous build')
    command = ['sh', str(checkout / 'gradlew'), '-p', str(checkout), '--no-daemon', '--console=plain']
    if variant == 'modern':
        command += ['-Dqualification.sources=' + str(ROOT / 'qualification/src/androidTest/java')]
    if clean:
        command += ['clean']
    command += [':app:assembleDebug']
    if variant == 'modern':
        command += [':app:assembleDebugAndroidTest']
    command += [':app:testDebugUnitTest', ':app:lintDebug', ':app:dependencies', '--configuration', 'debugRuntimeClasspath']
    environment = dict(os.environ, JAVA_HOME=str(jdk), ANDROID_HOME=str(sdk))
    label = variant + ('-clean' if clean else '')
    log = evidence / (label + '-verification.log')
    input_paths = [checkout / 'build.gradle', checkout / 'app/build.gradle',
                   checkout / 'settings.gradle', checkout / 'gradle.properties',
                   checkout / 'gradle/wrapper/gradle-wrapper.properties',
                   checkout / 'app/src/main/AndroidManifest.xml']
    input_paths += sorted(path for path in (checkout / 'app/src/main').rglob('*') if path.is_file() and path not in input_paths)
    if variant == 'modern':
        input_paths += sorted((ROOT / 'qualification/src').rglob('*.java'))
    inputs = {str(path.relative_to(ROOT)): hashlib.sha256(path.read_bytes()).hexdigest() for path in input_paths}
    started = time.monotonic()
    with log.open('w') as output:
        result = subprocess.run(command, cwd=ROOT, env=environment, stdout=output, stderr=subprocess.STDOUT)
    artifacts = []
    for artifact in sorted((checkout / 'app/build/outputs/apk').rglob('*.apk')):
        artifacts.append({'path': str(artifact.relative_to(ROOT)), 'sha256': hashlib.sha256(artifact.read_bytes()).hexdigest()})
    receipt = {'variant': variant, 'clean': clean, 'exit_code': result.returncode,
               'elapsed_seconds': round(time.monotonic() - started, 1), 'command': command,
               'jdk': str(jdk), 'sdk': str(sdk), 'log': str(log.relative_to(ROOT)), 'artifacts': artifacts,
               'source_revision': source_revision, 'inputs': inputs}
    (evidence / (label + '-receipt.json')).write_text(json.dumps(receipt, indent=2) + '\n')
    print(json.dumps(receipt, indent=2))
    raise SystemExit(result.returncode)


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--variant', choices=['upstream', 'modern'], required=True)
    parser.add_argument('--jdk', type=Path, required=True)
    parser.add_argument('--sdk', type=Path, required=True)
    parser.add_argument('--clean', action='store_true')
    args = parser.parse_args()
    run(args.variant, args.jdk.resolve(), args.sdk.resolve(), args.clean)
