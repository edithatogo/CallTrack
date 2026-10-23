#!/usr/bin/env -S uv run --python 3.15.1 --no-project python
"""Prepare an isolated, traceable upstream build; reference checkout stays untouched."""
import argparse
import io
from pathlib import Path
import subprocess
import tarfile

ROOT = Path(__file__).resolve().parents[1]
REVISION = 'aaa56443a294aa61bad48b620427e53f9521c5fe'


def prepare(modern=False):
    source = ROOT / '.local/AndroidCallLogSync'
    revision = subprocess.check_output(['git', 'rev-parse', 'HEAD'], cwd=source, text=True).strip()
    if revision != REVISION:
        raise SystemExit('Reference revision differs from the audited revision')
    output = ROOT / '.local' / ('modern-qualification' if modern else 'upstream-qualification')
    if output.exists():
        raise SystemExit(f'Refusing to overwrite existing qualification directory: {output}')
    archive = subprocess.check_output(['git', 'archive', REVISION], cwd=source)
    with tarfile.open(fileobj=io.BytesIO(archive)) as bundle:
        # git archive has only paths controlled by the pinned source tree.
        bundle.extractall(output, filter='data')
    build = output / 'app/build.gradle'
    script = build.read_text()
    script += '''\n// Qualification-only identity and zero-row instrumentation; no upstream settings changed.\nandroid {\n    defaultConfig {\n        applicationId 'org.calltrack.qualification'\n        testApplicationId 'org.calltrack.qualification.test'\n        testInstrumentationRunner 'androidx.test.runner.AndroidJUnitRunner'\n    }\n    sourceSets.androidTest.java.srcDir(System.getProperty('qualification.sources'))\n}\n'''
    if modern:
        script = script.replace('compileSdk 32', "namespace 'com.markobl.calllogsync'\n    compileSdk 36")
        script = script.replace('targetSdk 32', 'targetSdk 36')
        script += "\n// Restore Fragment lint compatibility with modern AGP.\ndependencies { implementation 'androidx.fragment:fragment:1.5.1'; implementation 'androidx.lifecycle:lifecycle-viewmodel-ktx:2.5.1' }\n"
        top = output / 'build.gradle'
        top.write_text(top.read_text().replace('gradle:7.2.2', 'gradle:8.13.2'))
        manifest = output / 'app/src/main/AndroidManifest.xml'
        manifest.write_text(manifest.read_text().replace('\n    package="com.markobl.calllogsync"', ''))
        settings = output / 'settings.gradle'
        settings.write_text(settings.read_text().replace('        jcenter() // Warning: this repository is going to shut down soon\n', ''))
        wrapper = output / 'gradle/wrapper/gradle-wrapper.properties'
        # Verified against the official Gradle8.13 distribution SHA256 during qualification.
        checksum = '20f1b1176237254a6fc204d8434196fa11a4cfb387567519c61556e8710aed78'
        wrapper.write_text(wrapper.read_text().replace('gradle-7.3.3-bin.zip', 'gradle-8.13-bin.zip') + '\ndistributionSha256Sum=' + checksum + '\n')
    build.write_text(script)
    print(output.relative_to(ROOT))


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--modern', action='store_true')
    prepare(parser.parse_args().modern)
