"""Controlled downgrade rejection must not be mistaken for an unrelated install failure."""
import sys
from pathlib import Path
import subprocess
import tempfile
import unittest
from unittest.mock import patch
sys.path.insert(0, str(Path(__file__).resolve().parents[1] / 'scripts'))
import qualify_release_upgrade as upgrade


class UpgradeSafetyTest(unittest.TestCase):
    def test_refuses_physical_device_before_access(self):
        with patch.object(upgrade.subprocess, 'check_output') as read:
            with self.assertRaisesRegex(RuntimeError, 'refuses physical'):
                upgrade.qualify('phone', Path('/unused'))
            read.assert_not_called()

    def test_rejection_requires_failure_and_exact_android_reason(self):
        for code, error in ((0, 'INSTALL_FAILED_VERSION_DOWNGRADE'), (1, 'device offline'), (1, 'INSTALL_FAILED_UPDATE_INCOMPATIBLE')):
            with self.assertRaisesRegex(RuntimeError, 'controlled rejection'):
                upgrade.require_rejection(subprocess.CompletedProcess([], code, '', error), 'INSTALL_FAILED_VERSION_DOWNGRADE')
        upgrade.require_rejection(subprocess.CompletedProcess([], 1, '', 'Failure [INSTALL_FAILED_VERSION_DOWNGRADE]'), 'INSTALL_FAILED_VERSION_DOWNGRADE')

    def test_wrong_certificate_failure_is_distinct_from_downgrade(self):
        result = subprocess.CompletedProcess([], 1, '', 'Failure [INSTALL_FAILED_UPDATE_INCOMPATIBLE]')
        upgrade.require_rejection(result, 'INSTALL_FAILED_UPDATE_INCOMPATIBLE')
        with self.assertRaises(RuntimeError):
            upgrade.require_rejection(result, 'INSTALL_FAILED_VERSION_DOWNGRADE')

    def test_cleanup_does_not_touch_unowned_packages(self):
        command = unittest.mock.Mock()
        self.assertEqual([], upgrade.cleanup_owned_packages(command, []))
        command.assert_not_called()

    def test_cleanup_continues_after_failure_and_reports_it(self):
        command = unittest.mock.Mock(side_effect=[RuntimeError('test package uninstall failed'), 'Success', ''])
        errors = upgrade.cleanup_owned_packages(command, [upgrade.PACKAGE, upgrade.TEST_PACKAGE])
        self.assertEqual(['test package uninstall failed'], errors)
        self.assertEqual(('uninstall', upgrade.TEST_PACKAGE), command.call_args_list[0].args)
        self.assertEqual(('uninstall', upgrade.PACKAGE), command.call_args_list[1].args)

    def test_cleanup_requires_package_absence_readback(self):
        command = unittest.mock.Mock(side_effect=['Success', 'package:' + upgrade.PACKAGE])
        self.assertEqual(['Package still present'], upgrade.cleanup_owned_packages(command, [upgrade.PACKAGE]))

    def test_only_actual_native_fixture_inputs_are_bound(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            fixture = root / 'qualification/release'
            for folder in ('java', 'assets', 'licences'):
                (fixture / folder).mkdir(parents=True)
            for name in ('schema-provenance.json', 'import-schema-provenance.json'):
                (fixture / name).write_text('source-pinned')
            (fixture / 'java/Test.java').write_text('test fixture')
            (fixture / 'assets/schema.sql').write_text('schema')
            with patch.object(upgrade, 'ROOT', root):
                before = upgrade.qualification_inputs(fixture)
                (fixture / 'licences/license.txt').write_text('independent audit work')
                self.assertEqual(before, upgrade.qualification_inputs(fixture))
                (fixture / 'assets/schema.sql').write_text('changed actual input')
                self.assertNotEqual(before, upgrade.qualification_inputs(fixture))
