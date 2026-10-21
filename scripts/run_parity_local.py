#!/usr/bin/env python3
"""Bounded JVM fallback using retained Kotlin classes; never Android qualification."""
import hashlib
import json
from pathlib import Path
import subprocess
import os

ROOT = Path(__file__).resolve().parents[1]


def run():
    local = ROOT / '.local/counter-jvm'
    build = json.loads((ROOT / '.local/slice-evidence/build-receipt.json').read_text())
    if build['exit_code'] != 0 or not build['inputs_unchanged']:
        raise RuntimeError('Retained build was not successful')
    for source in (ROOT / 'app/src/main').rglob('*.kt'):
        key = str(source.relative_to(ROOT))
        if hashlib.sha256(source.read_bytes()).hexdigest() != build['inputs'].get(key):
            raise RuntimeError('Retained Kotlin class sources changed: ' + key)
    libraries = json.loads((local / 'libraries.json').read_text())
    for entry in libraries.values():
        jar = local / 'lib' / entry['url'].rsplit('/', 1)[1]
        if hashlib.sha256(jar.read_bytes()).hexdigest() != entry['sha256']:
            raise RuntimeError('Unexpected fallback library: ' + jar.name)
    classpath = (local / 'classpath.txt').read_text().strip()
    if not all(Path(item).exists() for item in classpath.split(os.pathsep)):
        raise RuntimeError('Fallback classpath unavailable')
    production = sorted(p for p in (ROOT / 'app/src/main/java/com/markobl/calllogsync/slice').glob('*.java')
                        if 'import android' not in p.read_text())
    tests = sorted(p for p in (ROOT / 'app/src/test/java/com/markobl/calllogsync/slice').glob('*.java')
                   if p.name != 'SchedulingCommitTest.java')
    # Fresh output prevents removed sources being silently supplied by a prior fallback run.
    import tempfile
    with tempfile.TemporaryDirectory(prefix='parity-jvm-') as fresh:
        classes = Path(fresh)
        java = Path(os.environ.get('JAVA_HOME', '/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home')) / 'bin'
        subprocess.run([str(java / 'javac'), '--release', '8', '-cp', classpath, '-d', str(classes)]
                       + [str(p) for p in production + tests], check=True, cwd=ROOT)
        result = subprocess.run([str(java / 'java'), '-cp', str(classes) + os.pathsep + classpath,
                                 'org.junit.runner.JUnitCore']
                                + ['com.markobl.calllogsync.slice.' + p.stem for p in tests],
                                capture_output=True, text=True, cwd=ROOT)
        output = result.stdout + result.stderr
        print(output)
        report = {
            'kind': 'local javac/JUnit subset only', 'exit_code': result.returncode,
            'output': output, 'libraries': libraries,
            'compiled_sources': {str(p.relative_to(ROOT)): hashlib.sha256(p.read_bytes()).hexdigest()
                                 for p in production + tests},
            'excluded': ['SchedulingCommitTest', 'Android adapters and instrumentation'],
            'limits': 'Retained Kotlin/dependency classes used; Kotlin source hashes match prior build. '
                      'Not a full Gradle build, lint, APK or native runtime proof.',
        }
        (local / 'latest-local-receipt.json').write_text(json.dumps(report, indent=2) + '\n')
        if result.returncode:
            raise RuntimeError('Local JVM tests failed')


if __name__ == '__main__':
    run()
