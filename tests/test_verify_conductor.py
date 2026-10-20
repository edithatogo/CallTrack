"""Fault-injection checks for the documentation verifier, not app conformance."""
import contextlib
import importlib.util
import io
import json
from pathlib import Path
import shutil
import tempfile
import unittest
from unittest.mock import patch

MODULE = Path(__file__).resolve().parents[1] / 'scripts/verify_conductor.py'
SPEC = importlib.util.spec_from_file_location('verify_conductor', MODULE)
VERIFY = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(VERIFY)


class VerificationFailures(unittest.TestCase):
    def setUp(self):
        self.directory = tempfile.TemporaryDirectory()
        self.addCleanup(self.directory.cleanup)
        self.root = Path(self.directory.name)
        shutil.copytree(MODULE.parent.parent / 'conductor', self.root / 'conductor')
        self.baseline = next((self.root / 'conductor').glob('*/baseline_20261020'))

    def run_verifier(self):
        with patch.object(VERIFY, 'ROOT', self.root), patch.object(
            VERIFY.subprocess, 'check_output', return_value=''
        ), contextlib.redirect_stdout(io.StringIO()):
            VERIFY.verify()

    def test_current_documents_pass(self):
        self.run_verifier()

    def test_broken_document_link_fails(self):
        (self.root / 'conductor/broken.md').write_text('[missing](missing.md)')
        with self.assertRaisesRegex(AssertionError, 'Broken link'):
            self.run_verifier()

    def test_prerequisite_cycle_fails(self):
        path = self.baseline / 'metadata.json'
        data = json.loads(path.read_text())
        data['depends_on'] = [data['track_id']]
        path.write_text(json.dumps(data))
        with self.assertRaisesRegex(AssertionError, 'Prerequisite cycle'):
            self.run_verifier()

    def test_duplicate_scenario_id_fails(self):
        path = self.baseline / 'fixtures.json'
        data = json.loads(path.read_text())
        data['cases'].append(data['cases'][0])
        path.write_text(json.dumps(data))
        with self.assertRaises(AssertionError):
            self.run_verifier()

    def test_unmapped_requirement_fails(self):
        path = self.baseline / 'fixtures.json'
        data = json.loads(path.read_text())
        data['cases'][0]['requirement'] = 'R999999'
        path.write_text(json.dumps(data))
        with self.assertRaises(AssertionError):
            self.run_verifier()


if __name__ == '__main__':
    unittest.main()
