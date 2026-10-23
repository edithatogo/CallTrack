"""Exercise destructive-action guards without touching a device or source checkout."""
import importlib.util
from pathlib import Path
import tempfile
import json
import hashlib
import contextlib
import io
from types import SimpleNamespace
import unittest
from unittest.mock import patch

ROOT = Path(__file__).resolve().parents[1]


def load(name):
    spec = importlib.util.spec_from_file_location(name, ROOT / 'scripts' / (name + '.py'))
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    return module


class QualificationGuards(unittest.TestCase):
    def test_prepare_refuses_to_overwrite_existing_copy(self):
        module = load('prepare_qualification')
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            (root / '.local/modern-qualification').mkdir(parents=True)
            with patch.object(module, 'ROOT', root), patch.object(
                module.subprocess, 'check_output', return_value=module.REVISION + '\n'
            ):
                with self.assertRaisesRegex(SystemExit, 'Refusing to overwrite'):
                    module.prepare(modern=True)

    def test_prepare_rejects_wrong_reference_revision(self):
        module = load('prepare_qualification')
        with patch.object(module.subprocess, 'check_output', return_value='wrong\n'):
            with self.assertRaisesRegex(SystemExit, 'Reference revision differs'):
                module.prepare()

    def test_device_refuses_existing_package_before_install(self):
        module = load('qualify_device')
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            for path in ('debug/app-debug.apk', 'androidTest/debug/app-debug-androidTest.apk'):
                apk = root / '.local/modern-qualification/app/build/outputs/apk' / path
                apk.parent.mkdir(parents=True, exist_ok=True)
                apk.write_bytes(b'test only')
            result = type('Result', (), {'returncode': 0, 'stdout': 'package:org.calltrack.qualification', 'stderr': ''})()
            with patch.object(module, 'ROOT', root), patch.object(module.subprocess, 'run', return_value=result) as run:
                with self.assertRaisesRegex(RuntimeError, 'Refusing to replace'):
                    module.qualify('test-device')
                self.assertEqual(1, run.call_count)
                self.assertEqual(['shell', 'pm', 'list'], run.call_args.args[0][3:6])

    def test_bootstrap_preserves_dirty_reference(self):
        module = load('bootstrap_qualification')
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            (root / '.local/AndroidCallLogSync').mkdir(parents=True)
            with patch.object(module, 'ROOT', root), patch.object(
                module.subprocess, 'check_output', side_effect=[module.REVISION + '\n', ' M app/build.gradle\n']
            ), patch.object(module.subprocess, 'run') as run:
                with self.assertRaisesRegex(SystemExit, 'leaving it untouched'):
                    module.bootstrap()
                run.assert_not_called()

    def test_device_rejects_failed_build_before_install(self):
        module = load('qualify_device')
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            for path in ('debug/app-debug.apk', 'androidTest/debug/app-debug-androidTest.apk'):
                apk = root / '.local/modern-qualification/app/build/outputs/apk' / path
                apk.parent.mkdir(parents=True, exist_ok=True)
                apk.write_bytes(b'test only')
            evidence = root / '.local/build-evidence'
            evidence.mkdir()
            (evidence / 'modern-clean-receipt.json').write_text(json.dumps({'exit_code': 1, 'artifacts': []}))
            result = type('Result', (), {'returncode': 0, 'stdout': '', 'stderr': ''})()
            with patch.object(module, 'ROOT', root), patch.object(module.subprocess, 'run', return_value=result) as run:
                with self.assertRaisesRegex(RuntimeError, 'unsuccessful build'):
                    module.qualify('test-device')
                self.assertEqual(2, run.call_count)

    def test_disconnected_cleanup_cannot_report_passed(self):
        module = load('qualify_device')
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            artifacts = []
            for path in ('debug/app-debug.apk', 'androidTest/debug/app-debug-androidTest.apk'):
                apk = root / '.local/modern-qualification/app/build/outputs/apk' / path
                apk.parent.mkdir(parents=True, exist_ok=True)
                apk.write_bytes(b'test only')
                artifacts.append({'path': str(apk.relative_to(root)), 'sha256': hashlib.sha256(apk.read_bytes()).hexdigest()})
            evidence = root / '.local/build-evidence'
            evidence.mkdir()
            (evidence / 'modern-clean-receipt.json').write_text(json.dumps({'exit_code': 0, 'artifacts': artifacts}))
            package_checks = 0

            def fake_run(arguments, **kwargs):
                nonlocal package_checks
                command = arguments[3:]
                output = ''
                code = 0
                if command[:4] == ['shell', 'pm', 'list', 'packages']:
                    package_checks += 1
                    if package_checks > 2:
                        code = 1
                elif command[0] in ('install', 'uninstall'):
                    output = 'Success'
                elif command[:3] == ['shell', 'am', 'start']:
                    output = 'Status: ok'
                elif command[:2] == ['shell', 'pidof']:
                    output = '123'
                elif command[:3] == ['shell', 'am', 'instrument']:
                    output = 'OK (1 test)'
                elif command[:2] == ['shell', 'getprop']:
                    output = '37'
                return SimpleNamespace(returncode=code, stdout=output, stderr='device offline' if code else '')

            def fake_identity(arguments, **kwargs):
                package = module.TEST_PACKAGE if 'androidTest' in arguments[-1] else module.PACKAGE
                return "package: name='" + package + "'"

            with patch.object(module, 'ROOT', root), patch.object(module.subprocess, 'run', side_effect=fake_run), patch.object(
                module.subprocess, 'check_output', side_effect=fake_identity
            ), contextlib.redirect_stdout(io.StringIO()):
                with self.assertRaisesRegex(RuntimeError, 'cleanup not verified'):
                    module.qualify('test-device')
            receipt = json.loads((evidence / 'device-receipt.json').read_text())
            self.assertEqual('failed', receipt['result'])
            self.assertFalse(receipt['cleanup_complete'])

    def test_build_requires_installed_toolchain(self):
        module = load('run_qualification')
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            with patch.object(module, 'ROOT', root), patch.object(module.subprocess, 'run') as run:
                with self.assertRaisesRegex(SystemExit, 'Provide an installed'):
                    module.run('modern', root / 'missing-jdk', root / 'missing-sdk')
                run.assert_not_called()


if __name__ == '__main__':
    unittest.main()
