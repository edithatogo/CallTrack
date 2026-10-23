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

    def test_older_minor_rejected(self):
        with self.assertRaisesRegex(RuntimeError, 'Python3.15.1 required'):
            self.check_runtime(version=(3, 14, 9))

    def test_older_patch_rejected(self):
        with self.assertRaisesRegex(RuntimeError, 'Python3.15.1 required'):
            self.check_runtime(version=(3, 15, 0))

    def test_system_python_rejected_even_with_correct_version(self):
        with self.assertRaisesRegex(RuntimeError, 'repository-local'):
            self.check_runtime(prefix=Path('/opt/python3.15'))

    def test_external_executable_rejected_even_with_local_prefix(self):
        with self.assertRaisesRegex(RuntimeError, 'executable'):
            self.check_runtime(executable=Path('/opt/bin/python3.15'))

    def test_stale_version_manifest_rejected(self):
        (self.root / '.python-version').write_text('3.14.9\n')
        with self.assertRaisesRegex(RuntimeError, 'manifest'):
            self.check_runtime()

    def test_prerelease_is_not_qualified(self):
        with self.assertRaisesRegex(RuntimeError, 'final Python release'):
            self.check_runtime(releaselevel='candidate')
