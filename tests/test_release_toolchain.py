import json
import os
from pathlib import Path
import sys
import tarfile
import tempfile
import unittest
from unittest.mock import patch
import xml.etree.ElementTree as ET
import zipfile
sys.path.insert(0, str(Path(__file__).resolve().parents[1] / 'scripts'))
import audit_release_toolchain as tools
import audit_release_artifacts as artifacts


class ToolTrustTest(unittest.TestCase):
    def test_local_jdk_selection_requires_original_archive_and_unchanged_bundle(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            toolchain = root / '.local/toolchain'
            toolchain.mkdir(parents=True)
            archive = toolchain / tools.TEMURIN25_ARCHIVE
            with tarfile.open(archive, 'w:gz') as bundle:
                for name in ('java', 'keytool'):
                    data = ('publisher-' + name).encode()
                    member = tarfile.TarInfo('jdk-25.0.5.1+2/Contents/Home/bin/' + name)
                    member.size = len(data)
                    bundle.addfile(member, io.BytesIO(data))
            installed = root / 'publisher-jdk'
            binaries = installed / 'Contents/Home/bin'
            binaries.mkdir(parents=True)
            for name in ('java', 'keytool'):
                (binaries / name).write_bytes(('publisher-' + name).encode())
            (toolchain / 'temurin-jdk25').symlink_to(installed, target_is_directory=True)
            with patch.object(tools, 'ROOT', root), patch.object(tools, 'TEMURIN25_SHA256', tools.sha(archive)), patch.object(tools.platform, 'system', return_value='Darwin'), patch.object(tools.platform, 'machine', return_value='arm64'):
                with patch.dict(os.environ, {'JAVA_HOME': '/tmp/untrusted-jdk'}):
                    selected, evidence = tools.qualified_local_jdk()
                self.assertEqual(toolchain / 'temurin-jdk25/Contents/Home', selected)
                self.assertEqual(tools.sha(archive), evidence['archive_sha256'])
                self.assertEqual(tools.sha(Path(tools.__file__).resolve()), evidence['verification_script_sha256'])
                (binaries / 'keytool').write_bytes(b'tampered')
                with self.assertRaisesRegex(RuntimeError, 'bundle differs'):
                    tools.qualified_local_jdk()
                (binaries / 'keytool').write_bytes(b'publisher-keytool')
                archive.write_bytes(b'tampered archive')
                with self.assertRaisesRegex(RuntimeError, 'Publisher-pinned'):
                    tools.qualified_local_jdk()

    def test_local_jdk_rejects_unqualified_host_before_reading_files(self):
        with patch.object(tools.platform, 'system', return_value='Linux'), patch.object(tools, 'sha') as sha:
            with self.assertRaisesRegex(RuntimeError, 'only on macOS ARM64'):
                tools.qualified_local_jdk()
            sha.assert_not_called()

    def test_java_environment_drops_injected_runtime_options(self):
        overrides = {'JAVA_HOME': '/tmp/untrusted-jdk', 'JAVA_TOOL_OPTIONS': '-agentlib:malicious',
                     'JDK_JAVA_OPTIONS': '-javaagent:malicious', 'LD_PRELOAD': '/tmp/malicious'}
        with patch.dict(os.environ, overrides):
            env = tools.qualified_java_environment(Path('/verified/jdk'))
        self.assertEqual('/verified/jdk', env['JAVA_HOME'])
        for name in ('JAVA_TOOL_OPTIONS', 'JDK_JAVA_OPTIONS', 'LD_PRELOAD'):
            self.assertNotIn(name, env)

    def test_temurin_archive_comparison_rejects_missing_changed_and_extra_files(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            archive = root / 'jdk.tar.gz'
            with tarfile.open(archive, 'w:gz') as bundle:
                data = b'verified-java'
                member = tarfile.TarInfo('jdk-25.0.5.1+2/Contents/Home/bin/java')
                member.size = len(data)
                bundle.addfile(member, io.BytesIO(data))
            installed = root / 'jdk'
            java = installed / 'Contents/Home/bin/java'
            java.parent.mkdir(parents=True)
            self.assertFalse(tools.temurin_comparison(archive, installed)['all_jdk_files_match'])
            java.write_bytes(b'tampered')
            self.assertFalse(tools.temurin_comparison(archive, installed)['all_jdk_files_match'])
            java.write_bytes(data)
            self.assertTrue(tools.temurin_comparison(archive, installed)['all_jdk_files_match'])
            (java.parent / 'unexpected').write_bytes(b'extra')
            self.assertFalse(tools.temurin_comparison(archive, installed)['all_jdk_files_match'])

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

    def test_sdk_repository_dtd_fails_closed_and_retains_receipt(self):
        sdk = '<sdk:sdk-repository xmlns:sdk="http://schemas.android.com/sdk/android/repo/repository2/03"><remotePackage path="other"><license>Terms &amp; conditions</license></remotePackage></sdk:sdk-repository>'
        cases = [
            ('dtd-utf8', ('<!DOCTYPE sdk:sdk-repository [<!ENTITY x "expanded">]>' + sdk).encode(), ET.ParseError, 'DTD declarations are not allowed'),
            ('dtd-utf16', ('<!DOCTYPE sdk:sdk-repository [<!ENTITY x "expanded">]>' + sdk).encode('utf-16'), ET.ParseError, 'DTD declarations are not allowed'),
            ('malformed', b'<sdk-repository>', ET.ParseError, 'no element found'),
            ('normal', sdk.encode(), RuntimeError, 'Exact SDK package not found'),
        ]
        for name, xml, error_type, message in cases:
            with self.subTest(name=name), tempfile.TemporaryDirectory() as directory:
                root = Path(directory)
                gradle_home = root / 'gradle-home'
                (gradle_home / 'wrapper/dists/gradle-9.7.0-bin/test/gradle-9.7.0').mkdir(parents=True)
                def fetch(_url, destination):
                    destination.write_bytes(xml)
                with patch.dict(tools.os.environ, {'GRADLE_USER_HOME': str(gradle_home)}), \
                     patch.object(tools, 'fetch', side_effect=fetch), \
                     patch.object(tools, 'published_checksum', return_value='0' * 64), \
                     patch.object(tools, 'sha', return_value='0' * 64), \
                     patch.object(tools, 'zip_comparison', return_value={'all_archive_files_match': True}):
                    with self.assertRaisesRegex(error_type, message):
                        tools.run(root / 'output', archives_from=root / 'archives')
                receipt = json.loads((root / 'output/receipt.json').read_text())
                self.assertFalse(receipt['success'])
                self.assertIn(message, receipt['error'])

    def test_parent_pom_licence_evidence_is_retained(self):
        child = b'<project xmlns="http://maven.apache.org/POM/4.0.0"><parent><groupId>org.test</groupId><artifactId>parent</artifactId><version>1</version></parent></project>'
        parent = b'<project xmlns="http://maven.apache.org/POM/4.0.0"><licenses><license><name>Apache-2.0</name></license></licenses></project>'
        with patch.object(artifacts, 'read_publication', return_value=parent):
            evidence = []
            self.assertEqual('Apache-2.0', artifacts.inherited_licences(child, evidence)[0]['name'])
            self.assertEqual(artifacts.digest(parent), evidence[0]['sha256'])

    def test_untrusted_parent_pom_dtd_is_rejected(self):
        child = b'<project xmlns="http://maven.apache.org/POM/4.0.0"><parent><groupId>org.test</groupId><artifactId>parent</artifactId><version>1</version></parent></project>'
        parent = b'<!DOCTYPE project [<!ENTITY name "Apache-2.0">]><project xmlns="http://maven.apache.org/POM/4.0.0"><licenses><license><name>&name;</name></license></licenses></project>'
        with patch.object(artifacts, 'read_publication', return_value=parent):
            with self.assertRaisesRegex(ET.ParseError, 'DTD declarations are not allowed'):
                artifacts.inherited_licences(child, [])

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

    def test_truncated_macho_commands_fail_as_invalid_input(self):
        import struct
        malformed = struct.pack('<IiiIIIII', 0xfeedfacf, 0x100000c, 0, 6, 2, 0, 0, 0)
        with self.assertRaisesRegex(ValueError, 'Truncated'):
            tools.macho_payload(malformed)
