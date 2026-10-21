"""Artifact comparison must reject missing, empty and differing build outputs."""
import importlib.util
from pathlib import Path
import tempfile
import unittest

spec = importlib.util.spec_from_file_location('release_build', Path(__file__).resolve().parents[1] / 'scripts/qualify_release_build.py')
release = importlib.util.module_from_spec(spec)
spec.loader.exec_module(release)


class ComparisonTest(unittest.TestCase):
    def test_requires_two_nonempty_artifacts(self):
        with tempfile.TemporaryDirectory() as folder:
            a, b = Path(folder) / 'a.apk', Path(folder) / 'b.apk'
            a.write_bytes(b'')
            with self.assertRaises(ValueError):
                release.compare_artifacts([a, b])
            b.write_bytes(b'apk')
            with self.assertRaises(ValueError):
                release.compare_artifacts([a, b])

    def test_content_difference_fails_comparison(self):
        with tempfile.TemporaryDirectory() as folder:
            a, b = Path(folder) / 'a.apk', Path(folder) / 'b.apk'
            a.write_bytes(b'first')
            b.write_bytes(b'second')
            self.assertFalse(release.compare_artifacts([a, b])['identical'])
            b.write_bytes(a.read_bytes())
            self.assertTrue(release.compare_artifacts([a, b])['identical'])
