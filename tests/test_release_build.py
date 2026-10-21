"""Artifact comparison must reject missing, empty and differing build outputs."""
import importlib.util
from pathlib import Path
import tempfile
import unittest
from unittest.mock import patch
import json

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


class RunnerSafetyTest(unittest.TestCase):
    def test_dirty_source_refused_before_creating_output(self):
        with tempfile.TemporaryDirectory() as folder:
            output = Path(folder) / 'evidence'
            with patch.object(release.subprocess, 'check_output', return_value=b' M app/build.gradle'):
                with self.assertRaisesRegex(ValueError, 'Commit source'):
                    release.run(output, Path(folder), Path(folder))
            self.assertFalse(output.exists())

    def test_existing_evidence_is_never_replaced(self):
        with tempfile.TemporaryDirectory() as folder:
            output = Path(folder)
            sentinel = output / 'receipt.json'
            sentinel.write_text('preserve')
            with patch.object(release.subprocess, 'check_output', side_effect=[b'', 'abc123']):
                with self.assertRaises(FileExistsError):
                    release.run(output, output, output)
            self.assertEqual('preserve', sentinel.read_text())

    def test_failed_checkout_leaves_failed_receipt(self):
        with tempfile.TemporaryDirectory() as folder:
            output = Path(folder) / 'evidence'
            with patch.object(release.subprocess, 'check_output', side_effect=[b'', 'abc123']), patch.object(release.subprocess, 'run', side_effect=RuntimeError('clone fault')):
                with self.assertRaisesRegex(RuntimeError, 'clone fault'):
                    release.run(output, Path(folder), Path(folder))
            receipt = json.loads((output / 'receipt.json').read_text())
            self.assertFalse(receipt['success'])
            self.assertFalse(receipt['release_ready'])
            self.assertEqual('clone fault', receipt['error'])
