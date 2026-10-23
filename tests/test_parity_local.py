"""The retired retained-class path must never run a partial Java-only qualification."""
import importlib.util
from pathlib import Path
import unittest
import tempfile
import json
from types import SimpleNamespace
from unittest.mock import patch

MODULE = Path(__file__).resolve().parents[1] / 'scripts/run_parity_local.py'
spec = importlib.util.spec_from_file_location('parity_local', MODULE)
local = importlib.util.module_from_spec(spec)
spec.loader.exec_module(local)


class RetiredFallbackSafetyTest(unittest.TestCase):
    def test_retired_fallback_refuses_before_compiler_or_device_execution(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            build = root / '.local/slice-evidence/build-receipt.json'
            build.parent.mkdir(parents=True)
            build.write_text(json.dumps({'exit_code': 0, 'inputs_unchanged': True, 'inputs': {}}))
            cache = root / '.local/counter-jvm'
            cache.mkdir()
            (cache / 'libraries.json').write_text('{}')
            (cache / 'classpath.txt').write_text(str(root))
            self._assert_retired(root)

    def _assert_retired(self, root):
        with patch.object(local, 'ROOT', root), patch('subprocess.run', return_value=SimpleNamespace(returncode=0, stdout='', stderr='')) as run, patch('subprocess.check_output') as read:
            with self.assertRaisesRegex(RuntimeError, 'Retired.*run_slice_build.py'):
                local.run()
            run.assert_not_called()
            read.assert_not_called()
