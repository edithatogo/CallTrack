"""Cold Linux-build boundary tests; no Docker or network required."""
import contextlib
import hashlib
import importlib.util
import io
import json
from pathlib import Path
import subprocess
import sys
import tempfile
import unittest
from unittest.mock import patch
import xml.etree.ElementTree as ET
import zipfile

sys.path.insert(0, str(Path(__file__).resolve().parents[1] / 'scripts'))
SPEC = importlib.util.spec_from_file_location('linux_build', Path(__file__).resolve().parents[1] / 'scripts/qualify_linux_build.py')
linux = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(linux)


class LinuxBoundaries(unittest.TestCase):
    def test_junit_counts_sum_all_release_result_files(self):
        with tempfile.TemporaryDirectory() as tmp:
            root=Path(tmp)
            results=root/'app/build/test-results/testReleaseUnitTest'
            results.mkdir(parents=True)
            (results/'TEST-one.xml').write_text('<testsuite tests="7" failures="1" errors="0" skipped="2"/>')
            (results/'TEST-two.xml').write_text('<testsuite tests="5" failures="0" errors="1" skipped="0"/>')
            self.assertEqual({'tests':12,'failures':1,'errors':1,'skipped':2},linux.junit_counts(root))

    def test_junit_results_reject_dtd_before_expansion(self):
        xml = '<!DOCTYPE testsuite [<!ENTITY n "119">]><testsuite tests="&n;" failures="0" errors="0" skipped="0"/>'
        for encoding in ('utf-8', 'utf-16'):
            with self.subTest(encoding=encoding), tempfile.TemporaryDirectory() as tmp:
                root = Path(tmp)
                results = root / 'app/build/test-results/testReleaseUnitTest'
                results.mkdir(parents=True)
                (results / 'TEST-attacker.xml').write_bytes(xml.encode(encoding))
                with self.assertRaisesRegex(ET.ParseError, 'DTD declarations are not allowed'):
                    linux.junit_counts(root)
                (results / 'TEST-attacker.xml').write_bytes(b'<testsuite>')
                with self.assertRaises(ET.ParseError):
                    linux.junit_counts(root)

    def test_existing_work_cache_is_not_reused(self):
        with tempfile.TemporaryDirectory() as tmp:
            path = Path(tmp)/'work'
            path.mkdir()
            (path/'cached-artifact').write_text('old')
            with self.assertRaises(FileExistsError):
                linux.fresh_directory(path)
            self.assertEqual('old', (path/'cached-artifact').read_text())

    def test_sdk_origin_and_path_are_restricted(self):
        for url in ['http://dl.google.com/android/repository/a.zip', 'https://example.org/a.zip',
                    'https://dl.google.com/other/a.zip',
                    'https://dl.google.com/android/repository/%2e%2e/other']:
            with self.subTest(url=url), self.assertRaises(ValueError):
                linux.approved_url(url)

    def test_archive_cannot_escape_or_create_a_symlink(self):
        for filename, mode in [('root/../../outside', 0o100644), ('/outside', 0o100644), ('root/link', 0o120777)]:
            with self.subTest(filename=filename), tempfile.TemporaryDirectory() as tmp:
                path=Path(tmp);data=io.BytesIO()
                with zipfile.ZipFile(data,'w') as z:
                    item=zipfile.ZipInfo(filename);item.external_attr=mode<<16;z.writestr(item,'payload')
                archive=path/'sdk.zip';archive.write_bytes(data.getvalue())
                with self.assertRaises(ValueError):
                    linux.extract_sdk(archive,path/'sdk')
                self.assertFalse((path/'outside').exists())

    def test_partial_or_changed_download_is_rejected(self):
        with tempfile.TemporaryDirectory() as tmp:
            f=Path(tmp)/'archive';f.write_bytes(b'changed')
            with self.assertRaises(ValueError):
                linux.verify_download(f,{'size':7,'checksum_algorithm':'sha1','checksum':'0'*40})
            linux.verify_download(f,{'size':7,'checksum_algorithm':'sha1','checksum':hashlib.sha1(b'changed').hexdigest()})

    def test_revision_is_exact_not_a_shell_expression_or_branch(self):
        for value in ['main','HEAD','abc; echo bad','0'*39]:
            with self.assertRaises(ValueError):linux.validate_revision(value)
        linux.validate_revision('a'*40)

    def test_failed_receipt_binds_shared_policies(self):
        revision = 'a' * 40
        with tempfile.TemporaryDirectory() as tmp:
            root = Path(tmp)
            source = root / 'source'
            source.mkdir()
            output = root / 'output'
            calls = [revision + '\n', b'', b'', 'openjdk version "17.0.21"']
            with patch.object(linux.platform, 'system', return_value='Linux'), \
                 patch.object(linux.subprocess, 'check_output', side_effect=calls):
                with self.assertRaisesRegex(ValueError, 'JDK25 required'):
                    linux.run(source, output, revision, '0' * 64)
            receipt = json.loads((output / 'evidence/receipt.json').read_text())
            helper = Path(linux.__file__).with_name('redirect_validation.py')
            self.assertEqual(hashlib.sha256(helper.read_bytes()).hexdigest(), receipt['redirect_policy_sha256'])
            xml_helper = Path(linux.__file__).with_name('xml_safety.py')
            self.assertEqual(hashlib.sha256(xml_helper.read_bytes()).hexdigest(), receipt['xml_policy_sha256'])
            self.assertFalse(receipt['success'])

    def test_lint_xml_policy_and_existing_error_gate(self):
        dtd = '<!DOCTYPE issues [<!ENTITY level "Error">]><issues><issue severity="&level;"/></issues>'
        cases = [
            ('dtd-utf8', dtd.encode(), ET.ParseError, 'DTD declarations are not allowed'),
            ('dtd-utf16', dtd.encode('utf-16'), ET.ParseError, 'DTD declarations are not allowed'),
            ('lint-error', b'<issues><issue severity="Error"/></issues>', RuntimeError, 'Release lint failed'),
            ('lint-fatal', b'<issues><issue severity="Fatal"/></issues>', RuntimeError, 'Release lint failed'),
            ('missing-jvm-test', b'<issues/>', RuntimeError, 'Candidate JVM acceptance count/outcome changed'),
            ('warning', b'<issues><issue severity="Warning"/></issues>', None, None),
        ]
        revision = 'a' * 40
        xml_hash = hashlib.sha256(Path(linux.__file__).with_name('xml_safety.py').read_bytes()).hexdigest()
        for name, lint_bytes, error_type, message in cases:
            with self.subTest(name=name), tempfile.TemporaryDirectory() as tmp:
                root = Path(tmp)
                source = root / 'source'
                lint_file = source / 'app/build/reports/lint-results-release.xml'
                lint_file.parent.mkdir(parents=True)
                lint_file.write_bytes(lint_bytes)
                apk = source / 'app/build/outputs/apk/release/app-release-unsigned.apk'
                apk.parent.mkdir(parents=True)
                apk.write_bytes(b'candidate')
                apk_hash = hashlib.sha256(b'candidate').hexdigest()
                calls = [revision + '\n', b'', b'', 'openjdk version "25.0.5.1"', b'']
                build = subprocess.CompletedProcess([], 0)
                unsigned = subprocess.CompletedProcess([], 1, stdout='ERROR: Missing META-INF/MANIFEST.MF', stderr='')
                def download(_url, archive, _row):
                    archive.write_bytes(b'sdk-fixture')
                with patch.object(linux.platform, 'system', return_value='Linux'), \
                     patch.object(linux.subprocess, 'check_output', side_effect=calls), \
                     patch.object(linux.subprocess, 'run', side_effect=[build, unsigned]), \
                     patch.object(linux, 'download_sdk_archive', side_effect=download), \
                     patch.object(linux, 'extract_sdk'), \
                     patch.object(linux, 'junit_counts', return_value={
                         'tests':118 if name == 'missing-jvm-test' else 119,
                         'failures':0, 'errors':0, 'skipped':0}), \
                     contextlib.redirect_stdout(io.StringIO()):
                    if error_type:
                        with self.assertRaisesRegex(error_type, message):
                            linux.run(source, root / 'output', revision, apk_hash)
                    else:
                        self.assertTrue(linux.run(source, root / 'output', revision, apk_hash)['success'])
                receipt = json.loads((root / 'output/evidence/receipt.json').read_text())
                self.assertEqual(xml_hash, receipt['xml_policy_sha256'])
                self.assertEqual(error_type is None, receipt['success'])
                if name == 'lint-error':
                    self.assertEqual({'Error': 1}, receipt['lint'])
                if name == 'warning':
                    self.assertEqual({'Warning': 1}, receipt['lint'])

    def test_older_jdk_fails_and_retains_receipt(self):
        revision = 'a' * 40
        with tempfile.TemporaryDirectory() as tmp:
            root = Path(tmp)
            source = root / 'source'
            source.mkdir()
            calls = [revision + '\n', b'', b'', 'openjdk version "17.0.21"']
            with patch.object(linux.platform, 'system', return_value='Linux'), \
                 patch.object(linux.subprocess, 'check_output', side_effect=calls):
                with self.assertRaisesRegex(ValueError, 'JDK25 required'):
                    linux.run(source, root / 'output', revision, '0' * 64)
            receipt = json.loads((root / 'output/evidence/receipt.json').read_text())
            self.assertFalse(receipt['success'])
            self.assertEqual('JDK25 required', receipt['error'])
