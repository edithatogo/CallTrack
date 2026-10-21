import io
import json
from pathlib import Path
import sys
import tempfile
import unittest
from unittest.mock import patch
import zipfile
sys.path.insert(0, str(Path(__file__).resolve().parents[1] / 'scripts'))
import audit_release_toolchain as tools
import audit_release_artifacts as artifacts


class ToolTrustTest(unittest.TestCase):
    def test_changed_or_missing_installed_tool_does_not_pass(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            bundle = root / 'archive.zip'
            with zipfile.ZipFile(bundle, 'w') as archive:
                archive.writestr('tool/bin/tool', 'expected')
            installed = root / 'installed'
            (installed / 'bin').mkdir(parents=True)
            self.assertFalse(tools.zip_comparison(bundle, installed)['all_archive_files_match'])
            (installed / 'bin/tool').write_text('tampered')
            self.assertFalse(tools.zip_comparison(bundle, installed)['all_archive_files_match'])
            (installed / 'bin/tool').write_text('expected')
            self.assertTrue(tools.zip_comparison(bundle, installed)['all_archive_files_match'])

    def test_archive_cannot_address_files_outside_selected_tool(self):
        with tempfile.TemporaryDirectory() as directory:
            bundle = Path(directory) / 'archive.zip'
            with zipfile.ZipFile(bundle, 'w') as archive:
                archive.writestr('tool/../../outside', 'test')
            with self.assertRaisesRegex(ValueError, 'Unsafe archive'):
                tools.zip_comparison(bundle, Path(directory))

    def test_checksum_endpoint_must_be_an_exact_sha256(self):
        with tempfile.TemporaryDirectory() as directory:
            path = Path(directory) / 'checksum'
            path.write_text('<html>error</html>')
            with patch.object(tools, 'fetch'):
                with self.assertRaisesRegex(ValueError, 'exact official SHA256'):
                    tools.published_checksum('https://example.test/checksum', path)

    def test_parent_pom_licence_evidence_is_retained(self):
        child = b'<project xmlns="http://maven.apache.org/POM/4.0.0"><parent><groupId>org.test</groupId><artifactId>parent</artifactId><version>1</version></parent></project>'
        parent = b'<project xmlns="http://maven.apache.org/POM/4.0.0"><licenses><license><name>Apache-2.0</name></license></licenses></project>'
        with patch.object(artifacts.urllib.request, 'urlopen', return_value=io.BytesIO(parent)):
            evidence = []
            self.assertEqual('Apache-2.0', artifacts.inherited_licences(child, evidence)[0]['name'])
            self.assertEqual(artifacts.digest(parent), evidence[0]['sha256'])

    def test_unresolved_parent_properties_do_not_invent_licence(self):
        child = b'<project xmlns="http://maven.apache.org/POM/4.0.0"><parent><groupId>org.test</groupId><artifactId>parent</artifactId><version>${version}</version></parent></project>'
        with self.assertRaisesRegex(ValueError, 'Unresolved'):
            artifacts.inherited_licences(child, [])

    def test_signature_allocation_normalization_preserves_every_payload_byte(self):
        import struct
        def binary(signature_size):
            header = struct.pack('<IiiIIIII', 0xfeedfacf, 0x100000c, 0, 6, 2, 88, 0, 0)
            segment = struct.pack('<II16sQQQQiiII', 0x19, 72, b'__LINKEDIT', 0, 4096, 120, 16 + signature_size, 0, 0, 0, 0)
            signature = struct.pack('<IIII', 0x1d, 16, 136, signature_size)
            return header + segment + signature + b'unchangedpayload' + bytes(signature_size)
        first = binary(8)
        self.assertEqual(tools.macho_payload(first), tools.macho_payload(binary(24)))
        corrupted = bytearray(first); corrupted[121] ^= 1
        self.assertNotEqual(tools.macho_payload(first), tools.macho_payload(corrupted))
        with self.assertRaises(ValueError):
            tools.macho_payload(first + b'unaccounted tail')

    def test_jdk_resource_exception_never_masks_code_corruption(self):
        import subprocess
        resource = subprocess.CompletedProcess([], 1, '', 'code has no resources but signature indicates they must be present')
        valid = subprocess.CompletedProcess([], 0, '', 'valid on disk (not all contents verified)')
        invalid = subprocess.CompletedProcess([], 1, '', 'code or signature modified')
        with patch.object(tools.subprocess, 'run', side_effect=[resource, valid]):
            proof = tools.verify_jdk_signature(Path('copy'), 'Contents/MacOS/libjli.dylib')
            self.assertTrue(proof['code_pages_valid'])
            self.assertFalse(proof['strict_valid'])
            self.assertFalse(proof['resources_verified'])
        with patch.object(tools.subprocess, 'run', side_effect=[resource, invalid]):
            with self.assertRaisesRegex(RuntimeError, 'code page'):
                tools.verify_jdk_signature(Path('copy'), 'Contents/MacOS/libjli.dylib')
        with patch.object(tools.subprocess, 'run', return_value=invalid):
            with self.assertRaisesRegex(RuntimeError, 'Unexpected'):
                tools.verify_jdk_signature(Path('copy'), 'Contents/MacOS/libjli.dylib')
        with patch.object(tools.subprocess, 'run', return_value=resource):
            with self.assertRaisesRegex(RuntimeError, 'Unexpected'):
                tools.verify_jdk_signature(Path('copy'), 'Contents/Home/bin/java')
