"""The private package-update test is never an implicit physical-device/install route."""
import importlib.util
from pathlib import Path
import sys
import unittest
import tempfile
import json
import subprocess
from unittest.mock import patch

sys.path.insert(0, str(Path(__file__).resolve().parents[1] / 'scripts'))
spec = importlib.util.spec_from_file_location('kotlin_update', Path(__file__).resolve().parents[1] / 'scripts/qualify_kotlin_update.py')
update = importlib.util.module_from_spec(spec)
spec.loader.exec_module(update)


class UpdateSafetyTest(unittest.TestCase):
    def test_physical_serial_refused_before_subprocess_or_file_access(self):
        with patch.object(update.subprocess, 'run') as run, patch.object(update.subprocess, 'check_output') as read:
            with self.assertRaisesRegex(RuntimeError, 'refuses physical-device'):
                update.qualify('physical-phone')
            run.assert_not_called()
            read.assert_not_called()

    def test_failed_or_changed_receipts_never_authorise_an_update(self):
        for receipt in ({}, {'exit_code': 1, 'inputs_unchanged': True}, {'exit_code': 0, 'inputs_unchanged': False}):
            with self.assertRaisesRegex(RuntimeError, 'successful unchanged'):
                update.require_successful_receipt(receipt)
        update.require_successful_receipt({'exit_code': 0, 'inputs_unchanged': True})


class UpdateFaultTest(unittest.TestCase):
    def fixture(self, existing=False, replace_failure=False, cleanup_failure=False):
        directory = tempfile.TemporaryDirectory()
        self.addCleanup(directory.cleanup)
        root = Path(directory.name)
        old = root / '.local/kotlin-evidence/expanded-java-baseline'
        old.mkdir(parents=True)
        baseline = old / 'app-debug.apk'
        candidate = root / 'app/build/outputs/apk/debug/app-debug.apk'
        tests = root / 'app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk'
        for path in (baseline, candidate, tests):
            path.parent.mkdir(parents=True, exist_ok=True)
            path.write_bytes(b'fixture-apk')
        source_inputs = {'build.gradle': update.hashlib.sha256(b'source').hexdigest()}
        artifacts = {str(path.relative_to(root)): update.sha(path) for path in (candidate, tests)}
        receipt = dict(exit_code=0, inputs_unchanged=True, inputs=source_inputs, artifacts=artifacts)
        (old / 'build-receipt.json').write_text(json.dumps(receipt))
        current = root / '.local/slice-evidence/build-receipt.json'
        current.parent.mkdir(parents=True)
        current.write_text(json.dumps(receipt))
        installed = {update.PACKAGE} if existing else set()
        commands = []

        def read(command, **kwargs):
            if command[:2] == ['git', 'ls-tree']:
                return 'build.gradle\n'
            if command[:2] == ['git', 'show']:
                return b'source'
            if 'aapt' in command[0]:
                package = update.TEST_PACKAGE if command[-1] == str(tests) else update.PACKAGE
                return "package: name='" + package + "' versionCode='" + ('' if package == update.TEST_PACKAGE else '10') + "'"
            if 'apksigner' in command[0]:
                return 'Signer #1 certificate SHA-256 digest: ' + 'a' * 64
            raise AssertionError(command)

        def run(command, **kwargs):
            args = command[3:]
            commands.append(args)
            if args == ['shell', 'getprop', 'ro.kernel.qemu']:
                output = '1'
            elif args == ['emu', 'avd', 'name']:
                output = 'calltrack-parity-api36-recovery-20261021\nOK'
            elif args == ['shell', 'getprop', 'sys.boot_completed']:
                output = '1'
            elif args[:4] == ['shell', 'pm', 'list', 'packages']:
                output = 'package:' + args[-1] if args[-1] in installed else ''
            elif args[0] == 'install':
                if '-r' in args and replace_failure:
                    return subprocess.CompletedProcess(command, 1, '', 'replacement fault')
                installed.add(update.TEST_PACKAGE if args[-1] == str(tests) else update.PACKAGE)
                output = 'Success'
            elif args[:3] == ['shell', 'am', 'instrument']:
                output = 'OK (1 test)'
            elif args[:3] == ['shell', 'am', 'force-stop']:
                output = ''
            elif args[0] == 'uninstall':
                if cleanup_failure and args[-1] == update.PACKAGE:
                    raise subprocess.TimeoutExpired(command, 180)
                installed.remove(args[-1])
                output = 'Success'
            else:
                raise AssertionError(args)
            return subprocess.CompletedProcess(command, 0, output, '')

        with patch.object(update, 'ROOT', root), patch.object(update, 'inputs', return_value=source_inputs), patch.object(update.subprocess, 'check_output', side_effect=read), patch.object(update.subprocess, 'run', side_effect=run):
            if existing or replace_failure or cleanup_failure:
                with self.assertRaises(RuntimeError):
                    update.qualify('emulator-5582')
            else:
                update.qualify('emulator-5582')
        files = list((root / '.local/kotlin-evidence').glob('package-update-*.json'))
        self.assertEqual(1, len(files))
        return commands, json.loads(files[0].read_text()), installed

    def test_existing_installation_is_never_replaced_or_removed(self):
        commands, receipt, installed = self.fixture(existing=True)
        self.assertFalse(any(c[0] in ('install', 'uninstall') for c in commands))
        self.assertEqual({update.PACKAGE}, installed)
        self.assertEqual('failed', receipt['result'])

    def test_replacement_failure_preserves_failure_and_cleans_only_owned_packages(self):
        commands, receipt, installed = self.fixture(replace_failure=True)
        self.assertTrue(any(c[:2] == ['install', '-r'] for c in commands))
        self.assertEqual('failed', receipt['result'])
        self.assertTrue(receipt['packages_removed'])
        self.assertFalse(installed)

    def test_cleanup_failure_cannot_report_acceptance(self):
        _, receipt, installed = self.fixture(cleanup_failure=True)
        self.assertEqual('failed', receipt['result'])
        self.assertFalse(receipt['packages_removed'])
        self.assertEqual({update.PACKAGE}, installed)

    def test_unversioned_instrumentation_allows_in_place_update_and_verified_removal(self):
        commands, receipt, installed = self.fixture()
        update_index = next(i for i, c in enumerate(commands) if c[:2] == ['install', '-r'])
        self.assertFalse(any(c[0] == 'uninstall' for c in commands[:update_index]))
        self.assertEqual('passed', receipt['result'])
        self.assertTrue(receipt['packages_removed'])
        self.assertFalse(installed)
