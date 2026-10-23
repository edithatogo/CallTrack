"""Runtime-admission faults; injected versions are deliberately synthetic."""
import importlib.util
from pathlib import Path
import tempfile
import unittest

MODULE = Path(__file__).resolve().parents[1] / 'scripts/verify_python_runtime.py'
SPEC = importlib.util.spec_from_file_location('verify_python_runtime', MODULE)
RUNTIME = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(RUNTIME)


class RuntimeAdmission(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.addCleanup(self.temp.cleanup)
        self.root = Path(self.temp.name)
        (self.root / '.python-version').write_text('3.15.1\n')
        self.prefix = self.root / '.venv'
        self.executable = self.prefix / 'bin/python'

    def check_runtime(self, **overrides):
        values = dict(root=self.root, version=(3, 15, 1),
                      prefix=self.prefix, executable=self.executable, releaselevel='final')
        values.update(overrides)
        return RUNTIME.verify(**values)

    def test_selected_local_runtime_passes(self):
        self.assertEqual(self.check_runtime()['version'], '3.15.1')

    def test_explicit_compatible_runtime_selection_is_allowed(self):
        (self.root / '.python-version').write_text('3.14.9\n')
        self.assertEqual(self.check_runtime(version=(3, 14, 9))['version'], '3.14.9')

    def test_actual_runtime_must_match_selected_version(self):
        with self.assertRaisesRegex(RuntimeError, '3.15.1 selected; actual'):
            self.check_runtime(version=(3, 15, 0))

    def test_system_python_rejected_even_with_correct_version(self):
        with self.assertRaisesRegex(RuntimeError, 'repository-local'):
            self.check_runtime(prefix=Path('/opt/python3.15'))

    def test_external_executable_rejected_even_with_local_prefix(self):
        with self.assertRaisesRegex(RuntimeError, 'executable'):
            self.check_runtime(executable=Path('/opt/bin/python3.15'))

    def test_stale_version_manifest_rejected(self):
        (self.root / '.python-version').write_text('3.15\n')
        with self.assertRaisesRegex(RuntimeError, 'stable X.Y.Z'):
            self.check_runtime()

    def test_noncanonical_manifest_rejected(self):
        (self.root / '.python-version').write_text('03.15.1\n')
        with self.assertRaisesRegex(RuntimeError, 'canonical'):
            self.check_runtime()

    def test_prerelease_is_not_qualified(self):
        with self.assertRaisesRegex(RuntimeError, 'final Python release'):
            self.check_runtime(releaselevel='candidate')
