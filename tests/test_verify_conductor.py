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

    def reset_track19_gate(self):
        path = self.root / 'conductor/tracks/cross_platform_20261021/metadata.json'
        data = json.loads(path.read_text())
        data['status'] = 'new'
        data.pop('prerequisite_deferral_document', None)
        path.write_text(json.dumps(data))

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
        data.pop('prerequisite_deferral_document', None)
        path.write_text(json.dumps(data))
        with self.assertRaisesRegex(AssertionError, 'Unresolved earlier execution sequence'):
            self.run_verifier()

    def test_scoped_compose_deferral_does_not_unlock_shared_core(self):
        compose = self.root / 'conductor/tracks/compose_app_20261021/metadata.json'
        data = json.loads(compose.read_text())
        data['status'] = 'in_progress'
        data['prerequisite_deferral_document'] = 'prerequisite-deferral.md'
        compose.write_text(json.dumps(data))
        self.run_verifier()

        shared_core = self.root / 'conductor/tracks/shared_core_20261021/metadata.json'
        data = json.loads(shared_core.read_text())
        data['status'] = 'in_progress'
        data.pop('prerequisite_deferral_document', None)
        shared_core.write_text(json.dumps(data))
        with self.assertRaisesRegex(AssertionError, 'Unresolved earlier execution sequence'):
            self.run_verifier()

    def test_scoped_compose_deferral_cannot_complete_track(self):
        path = self.root / 'conductor/tracks/compose_app_20261021/metadata.json'
        data = json.loads(path.read_text())
        data['status'] = 'completed'
        data['prerequisite_deferral_document'] = 'prerequisite-deferral.md'
        path.write_text(json.dumps(data))
        with self.assertRaisesRegex(AssertionError, 'cannot complete the track'):
            self.run_verifier()

    def test_scoped_track15_deferral_does_not_unlock_track16(self):
        shared_core = self.root / 'conductor/tracks/shared_core_20261021'
        path = shared_core / 'metadata.json'
        data = json.loads(path.read_text())
        data['status'] = 'in_progress'
        data['prerequisite_deferral_document'] = 'prerequisite-deferral.md'
        path.write_text(json.dumps(data))
        (shared_core / 'prerequisite-deferral.md').write_text(
            'Synthetic bounded Track15 scope; no later stage is resolved.'
        )
        self.run_verifier()

        data['status'] = 'completed'
        path.write_text(json.dumps(data))
        with self.assertRaisesRegex(AssertionError, 'cannot complete the track'):
            self.run_verifier()

        data['status'] = 'in_progress'
        path.write_text(json.dumps(data))

        macos = self.root / 'conductor/tracks/macos_companion_20261021/metadata.json'
        data = json.loads(macos.read_text())
        data['status'] = 'in_progress'
        data.pop('prerequisite_deferral_document', None)
        macos.write_text(json.dumps(data))
        with self.assertRaisesRegex(AssertionError, 'Unresolved earlier execution sequence'):
            self.run_verifier()

    def test_scoped_track16_deferral_allows_only_bounded_stage5_work(self):
        macos = self.root / 'conductor/tracks/macos_companion_20261021'
        path = macos / 'metadata.json'
        data = json.loads(path.read_text())
        data['status'] = 'in_progress'
        data['prerequisite_deferral_document'] = 'prerequisite-deferral.md'
        path.write_text(json.dumps(data))
        (macos / 'prerequisite-deferral.md').write_text(
            'Synthetic bounded stage5 scope; no previous stage is resolved.'
        )
        self.run_verifier()

        shared_ui = self.root / 'conductor/tracks/shared_ui_20261021/metadata.json'
        data = json.loads(shared_ui.read_text())
        data['status'] = 'in_progress'
        data.pop('prerequisite_deferral_document', None)
        shared_ui.write_text(json.dumps(data))
        with self.assertRaisesRegex(AssertionError, 'Unresolved earlier execution sequence'):
            self.run_verifier()

        data = json.loads(path.read_text())
        data['status'] = 'completed'
        path.write_text(json.dumps(data))
        with self.assertRaisesRegex(AssertionError, 'cannot complete the track'):
            self.run_verifier()

    def test_scoped_track17_deferral_allows_only_bounded_stage6_work(self):
        shared_ui = self.root / 'conductor/tracks/shared_ui_20261021'
        path = shared_ui / 'metadata.json'
        data = json.loads(path.read_text())
        data['status'] = 'in_progress'
        data['prerequisite_deferral_document'] = 'prerequisite-deferral.md'
        path.write_text(json.dumps(data))
        (shared_ui / 'prerequisite-deferral.md').write_text(
            'Synthetic bounded stage6 work; no earlier stage is resolved.'
        )
        rust = self.root / 'conductor/tracks/rust_component_20261021/metadata.json'
        rust_data = json.loads(rust.read_text())
        rust_data['status'] = 'new'
        rust_data['implementation_status'] = 'pending'
        rust_data.pop('prerequisite_deferral_document', None)
        rust.write_text(json.dumps(rust_data))
        self.run_verifier()

        data = json.loads(rust.read_text())
        data['status'] = 'in_progress'
        rust.write_text(json.dumps(data))
        with self.assertRaisesRegex(AssertionError, 'Unresolved earlier execution sequence'):
            self.run_verifier()

        data = json.loads(path.read_text())
        data['status'] = 'completed'
        path.write_text(json.dumps(data))
        with self.assertRaisesRegex(AssertionError, 'cannot complete the track'):
            self.run_verifier()

    def test_track18_rust_deferral_does_not_unlock_track19(self):
        self.reset_track19_gate()
        rust = self.root / 'conductor/tracks/rust_component_20261021'
        path = rust / 'metadata.json'
        data = json.loads(path.read_text())
        data.pop('prerequisite_deferral_document', None)
        data.update({
            'status': 'completed',
            'implementation_status': 'deferred',
            'deferral': {
                'reason': 'Synthetic no-consumer decision',
                'consequences': 'No Rust capability accepted',
                'reconsideration_trigger': 'Reopen after a real supported consumer exists',
            },
        })
        path.write_text(json.dumps(data))
        (rust / 'deferral.md').write_text('Synthetic Rust adoption deferral.')
        self.run_verifier()

        cross_platform = self.root / 'conductor/tracks/cross_platform_20261021/metadata.json'
        data = json.loads(cross_platform.read_text())
        data['status'] = 'in_progress'
        cross_platform.write_text(json.dumps(data))
        with self.assertRaisesRegex(AssertionError, 'Unresolved earlier execution sequence'):
            self.run_verifier()

    def test_scoped_track18_work_does_not_complete_or_unlock_track19(self):
        self.reset_track19_gate()
        rust = self.root / 'conductor/tracks/rust_component_20261021'
        path = rust / 'metadata.json'
        data = json.loads(path.read_text())
        data['status'] = 'in_progress'
        data['implementation_status'] = 'scoped-evaluation-only'
        data['prerequisite_deferral_document'] = 'prerequisite-deferral.md'
        path.write_text(json.dumps(data))
        (rust / 'prerequisite-deferral.md').write_text(
            'Synthetic bounded stage7 evaluation; later stages stay blocked.'
        )
        self.run_verifier()

        cross_platform = self.root / 'conductor/tracks/cross_platform_20261021/metadata.json'
        data = json.loads(cross_platform.read_text())
        data['status'] = 'in_progress'
        cross_platform.write_text(json.dumps(data))
        with self.assertRaisesRegex(AssertionError, 'Unresolved earlier execution sequence'):
            self.run_verifier()

        data = json.loads(path.read_text())
        data['status'] = 'completed'
        path.write_text(json.dumps(data))
        with self.assertRaisesRegex(AssertionError, 'cannot complete the track'):
            self.run_verifier()

    def test_scoped_track19_evidence_reconciliation_does_not_unlock_ios(self):
        cross_platform = self.root / 'conductor/tracks/cross_platform_20261021'
        path = cross_platform / 'metadata.json'
        data = json.loads(path.read_text())
        data['status'] = 'in_progress'
        data['prerequisite_deferral_document'] = 'prerequisite-deferral.md'
        path.write_text(json.dumps(data))
        (cross_platform / 'prerequisite-deferral.md').write_text(
            'Synthetic bounded stage8 evidence reconciliation; native acceptance remains blocked.'
        )
        self.run_verifier()

        ios = self.root / 'conductor/tracks/ios_companion_20261021/metadata.json'
        data = json.loads(ios.read_text())
        data['status'] = 'in_progress'
        ios.write_text(json.dumps(data))
        with self.assertRaisesRegex(AssertionError, 'iOS must remain last'):
            self.run_verifier()

        data = json.loads(path.read_text())
        data['status'] = 'completed'
        path.write_text(json.dumps(data))
        with self.assertRaisesRegex(AssertionError, 'cannot complete the track'):
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
