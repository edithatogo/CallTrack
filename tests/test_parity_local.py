"""Fallback must refuse stale build/source/dependency inputs before compiling anything."""
import importlib.util
import hashlib
import json
from pathlib import Path
import tempfile
import unittest
from unittest.mock import patch

MODULE = Path(__file__).resolve().parents[1] / 'scripts/run_parity_local.py'
spec = importlib.util.spec_from_file_location('parity_local', MODULE)
local = importlib.util.module_from_spec(spec)
spec.loader.exec_module(local)


class LocalFallbackSafetyTest(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.addCleanup(self.temp.cleanup)
        self.root = Path(self.temp.name)
        (self.root / '.local/slice-evidence').mkdir(parents=True)
        (self.root / '.local/counter-jvm/lib').mkdir(parents=True)
        source = self.root / 'app/src/main/kotlin/Fixture.kt'
        source.parent.mkdir(parents=True)
        source.write_text('fixture source')
        self.build = {'exit_code': 0, 'inputs_unchanged': True,
                      'inputs': {'app/src/main/kotlin/Fixture.kt': hashlib.sha256(source.read_bytes()).hexdigest()}}
        self.save()

    def save(self):
        (self.root / '.local/slice-evidence/build-receipt.json').write_text(json.dumps(self.build))

    def refused(self, message):
        with patch.object(local, 'ROOT', self.root), patch.object(local.subprocess, 'run') as process:
            with self.assertRaisesRegex(RuntimeError, message):
                local.run()
            process.assert_not_called()

    def test_failed_prior_build_refused(self):
        self.build['exit_code'] = 1
        self.save()
        self.refused('not successful')

    def test_changed_kotlin_source_refused(self):
        (self.root / 'app/src/main/kotlin/Fixture.kt').write_text('changed')
        self.refused('Kotlin class sources changed')

    def test_changed_library_refused(self):
        (self.root / '.local/counter-jvm/lib/fixture.jar').write_bytes(b'changed')
        (self.root / '.local/counter-jvm/libraries.json').write_text(json.dumps({
            'fixture': {'url': 'https://example.invalid/fixture.jar', 'sha256': '0' * 64}}))
        self.refused('Unexpected fallback library')
