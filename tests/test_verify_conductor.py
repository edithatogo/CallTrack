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
        shutil.copytree(MODULE.parent.parent / '.github', self.root / '.github')
        if (MODULE.parent.parent / 'qualification').exists():
            shutil.copytree(MODULE.parent.parent / 'qualification', self.root / 'qualification')
        for source in [*MODULE.parent.parent.glob('*.md'), MODULE.parent.parent / 'LICENSE']:
            if source.exists():
                shutil.copy2(source, self.root / source.name)
        if (MODULE.parent.parent / 'prototypes').exists():
            shutil.copytree(MODULE.parent.parent / 'prototypes', self.root / 'prototypes',
                            ignore=shutil.ignore_patterns('build', '.gradle', '.kotlin'))
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

    def test_ios_cannot_start_before_earlier_stages(self):
        path = self.root / 'conductor/tracks/ios_companion_20261021/metadata.json'
        data = json.loads(path.read_text())
        data['status'] = 'in_progress'
        path.write_text(json.dumps(data))
        with self.assertRaisesRegex(AssertionError, 'iOS must remain last'):
            self.run_verifier()

    def test_ios_reordering_is_rejected(self):
        path = self.root / 'conductor/future-sequence.json'
        data = json.loads(path.read_text())
        data['stages'][7], data['stages'][8] = data['stages'][8], data['stages'][7]
        path.write_text(json.dumps(data))
        with self.assertRaisesRegex(AssertionError, 'execution sequence'):
            self.run_verifier()

    def test_deferral_without_record_is_not_acceptance(self):
        path = self.root / 'conductor/tracks/kotlin_20261020/metadata.json'
        data = json.loads(path.read_text())
        data['implementation_status'] = 'deferred'
        path.write_text(json.dumps(data))
        with self.assertRaisesRegex(AssertionError, 'Deferral decision missing'):
            self.run_verifier()

    def test_compose_cannot_start_before_kotlin_and_release_dispositions(self):
        path = self.root / 'conductor/tracks/compose_app_20261021/metadata.json'
        data = json.loads(path.read_text())
        data['status'] = 'in_progress'
        path.write_text(json.dumps(data))
        with self.assertRaisesRegex(AssertionError, 'Unresolved earlier execution sequence'):
            self.run_verifier()

    def test_ios_can_start_after_explicit_documented_earlier_deferrals(self):
        sequence = json.loads((self.root / 'conductor/future-sequence.json').read_text())
        for entry in sequence['stages'][:-1]:
            folder = self.root / 'conductor/tracks' / entry['track_id']
            path = folder / 'metadata.json'
            data = json.loads(path.read_text())
            data['implementation_status'] = 'deferred'
            data['deferral'] = {'reason': 'Synthetic policy fixture',
                                'consequences': 'No capability accepted',
                                'reconsideration_trigger': 'Reopen before support claim'}
            path.write_text(json.dumps(data))
            (folder / 'deferral.md').write_text('Synthetic justified deferral; no capability accepted.')
        path = self.root / 'conductor/tracks/ios_companion_20261021/metadata.json'
        data = json.loads(path.read_text())
        data['status'] = 'in_progress'
        path.write_text(json.dumps(data))
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
