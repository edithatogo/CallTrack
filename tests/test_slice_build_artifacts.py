"""A debug build receipt must not attest to a leftover release artifact."""
import sys
from pathlib import Path
import tempfile
import unittest
from unittest.mock import patch
sys.path.insert(0, str(Path(__file__).resolve().parents[1] / 'scripts'))
import run_slice_build as build


class BuildArtifactScope(unittest.TestCase):
    def test_stale_release_apk_is_excluded(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            for name in ('debug/app-debug.apk', 'androidTest/debug/app-debug-androidTest.apk',
                         'release/app-release-unsigned.apk'):
                path = root / 'app/build/outputs/apk' / name
                path.parent.mkdir(parents=True, exist_ok=True)
                path.write_bytes(name.encode())
            with patch.object(build, 'ROOT', root):
                artifacts = build.debug_artifacts()
            self.assertEqual({'app/build/outputs/apk/debug/app-debug.apk',
                              'app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk'},
                             set(artifacts))

    def test_dependency_trust_policy_changes_invalidate_source_receipt(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            for name in ('build.gradle', 'settings.gradle', 'gradle.properties', 'gradlew',
                         'app/build.gradle', 'app/proguard-rules.pro'):
                path = root / name; path.parent.mkdir(parents=True, exist_ok=True); path.write_text('fixture')
            (root / 'gradle/wrapper').mkdir(parents=True)
            with patch.object(build, 'ROOT', root):
                before = build.inputs()
                policy = root / 'gradle/verification-metadata.xml'
                policy.write_text('first approved policy')
                added = build.inputs()
                self.assertNotEqual(before, added)
                policy.write_text('different trust policy')
                self.assertNotEqual(added, build.inputs())
