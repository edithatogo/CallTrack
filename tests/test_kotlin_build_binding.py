"""A Kotlin edit must invalidate native-acceptance receipts before any device operation."""
import importlib.util
import json
from pathlib import Path
import sys
import tempfile
import unittest
from unittest.mock import patch

SCRIPTS = Path(__file__).resolve().parents[1] / 'scripts'
sys.path.insert(0, str(SCRIPTS))
SPEC = importlib.util.spec_from_file_location('kotlin_binding', SCRIPTS / 'qualify_parity_emulator.py')
EMU = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(EMU)


class KotlinBuildBinding(unittest.TestCase):
    def test_kotlin_source_change_refuses_native_acceptance_before_tool_access(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            for relative in ('build.gradle', 'settings.gradle', 'gradle.properties', 'gradlew',
                             'app/build.gradle', 'app/proguard-rules.pro',
                             'app/src/main/kotlin/Fixture.kt'):
                path = root / relative
                path.parent.mkdir(parents=True, exist_ok=True)
                path.write_text('fixture before')
            evidence = root / '.local/slice-evidence/build-receipt.json'
            evidence.parent.mkdir(parents=True)
            with patch.object(EMU, 'ROOT', root), patch.dict(EMU.inputs.__globals__, ROOT=root):
                before = EMU.inputs()
                self.assertIn('app/src/main/kotlin/Fixture.kt', before)
                evidence.write_text(json.dumps({'exit_code': 0, 'inputs_unchanged': True,
                                                'inputs': before, 'artifacts': {}}))
                (root / 'app/src/main/kotlin/Fixture.kt').write_text('fixture changed')
                with patch.object(EMU.subprocess, 'run') as run, patch.object(
                    EMU.subprocess, 'check_output'
                ) as identity:
                    with self.assertRaisesRegex(RuntimeError, 'does not match current sources'):
                        EMU.qualify('emulator-5580')
                    run.assert_not_called()
                    identity.assert_not_called()
