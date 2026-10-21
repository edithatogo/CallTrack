"""Controlled downgrade rejection must not be mistaken for an unrelated install failure."""
import sys
from pathlib import Path
import subprocess
import tempfile
import unittest
import json
import zipfile
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

    def test_frozen_release_rejects_wrong_revision_and_modified_artifact(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            apk = root / 'candidate.apk'
            apk.write_bytes(b'unsigned')
            record = dict(apk=str(apk), apk_sha256=upgrade.sha(apk), exit_code=0,
                          signature_check=dict(unsigned_verified=True))
            receipt = root / 'receipt.json'
            data = dict(success=True, signed=False, revision='frozen', builds=[record, record])
            receipt.write_text(json.dumps(data))
            with self.assertRaisesRegex(RuntimeError, 'Matching frozen'):
                upgrade.frozen_release_apk(receipt, 'different', root, {})
            with patch('qualify_release_build.verify_unsigned') as verify:
                self.assertEqual(apk, upgrade.frozen_release_apk(receipt, 'frozen', root, {}))
                self.assertEqual(2, verify.call_count)
                apk.write_bytes(b'changed')
                with self.assertRaisesRegex(RuntimeError, 'artifact changed'):
                    upgrade.frozen_release_apk(receipt, 'frozen', root, {})

    def test_signing_cannot_change_or_remove_payload_or_add_unexpected_entries(self):
        with tempfile.TemporaryDirectory() as directory:
            before, after = [Path(directory) / name for name in ('unsigned.apk', 'signed.apk')]
            def write(path, entries):
                with zipfile.ZipFile(path, 'w') as archive:
                    for name, value in entries.items():
                        archive.writestr(name, value)
            write(before, {'classes.dex': b'code', 'META-INF/app-metadata.properties': b'pinned'})
            original = {'classes.dex': b'code', 'META-INF/app-metadata.properties': b'pinned'}
            write(after, dict(original, **{'META-INF/ANDROIDD.SF': b'signature'}))
            upgrade.require_identical_payload(before, after)
            for entries in ({'classes.dex': b'changed'}, {'classes.dex': b'code'},
                            dict(original, **{'assets/unexpected': b'extra'})):
                write(after, entries)
                with self.assertRaises(RuntimeError):
                    upgrade.require_identical_payload(before, after)
