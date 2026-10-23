"""Emulator-only target gates; fixtures do not contact ADB or prove native behaviour."""
import importlib.util
import json
from pathlib import Path
import sys
import tempfile
import unittest
from unittest.mock import patch
import subprocess

SCRIPTS = Path(__file__).resolve().parents[1] / 'scripts'
sys.path.insert(0, str(SCRIPTS))
spec = importlib.util.spec_from_file_location('parity_emulator', SCRIPTS / 'qualify_parity_emulator.py')
EMU = importlib.util.module_from_spec(spec)
spec.loader.exec_module(EMU)


class EmulatorSafety(unittest.TestCase):
    def test_physical_serial_is_rejected_before_any_tool_or_file_operation(self):
        with patch.object(EMU.subprocess, 'run') as run, patch.object(EMU.subprocess, 'check_output') as identity:
            with self.assertRaisesRegex(RuntimeError, 'physical-device'):
                EMU.qualify('physical-fixture-serial')
            run.assert_not_called()
            identity.assert_not_called()

    def fixture(self, qemu, name):
        directory = tempfile.TemporaryDirectory()
        self.addCleanup(directory.cleanup)
        root = Path(directory.name)
        receipt = root / '.local/slice-evidence/build-receipt.json'
        receipt.parent.mkdir(parents=True)
        artifacts = {}
        for relative in ['app/build/outputs/apk/debug/app-debug.apk', 'app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk']:
            path = root / relative
            path.parent.mkdir(parents=True, exist_ok=True)
            path.write_bytes(b'fixture')
            artifacts[relative] = EMU.hashlib.sha256(b'fixture').hexdigest()
        receipt.write_text(json.dumps({'exit_code': 0, 'inputs_unchanged': True, 'inputs': {}, 'artifacts': artifacts}))
        commands = []
        def run(command, **kwargs):
            args = command[3:]
            commands.append(args)
            if args == ['shell', 'getprop', 'ro.kernel.qemu']:
                output = qemu
            elif args == ['emu', 'avd', 'name']:
                output = name + '\nOK'
            else:
                raise AssertionError('Unexpected command before emulator target gates')
            return subprocess.CompletedProcess(command, 0, output, '')
        identities = ["package: name='" + package + "'" for package in (EMU.PACKAGE, EMU.TEST_PACKAGE)]
        with patch.object(EMU, 'ROOT', root), patch.object(EMU, 'inputs', return_value={}), patch.object(
            EMU.subprocess, 'check_output', side_effect=identities
        ), patch.object(EMU.subprocess, 'run', side_effect=run):
            with self.assertRaises(RuntimeError):
                EMU.qualify('emulator-5580')
        self.assertFalse(any(args[0] == 'install' for args in commands))

    def test_serial_prefix_alone_cannot_qualify_a_target(self):
        self.fixture('0', EMU.AVD)

    def test_an_unrelated_emulator_is_never_used(self):
        self.fixture('1', 'unrelated-avd')
