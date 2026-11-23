import io
import hashlib
from email.message import Message
from pathlib import Path
import sys
import unittest
import tempfile
import json
import xml.etree.ElementTree as ET
from unittest.mock import patch
from urllib.response import addinfourl
import zipfile
sys.path.insert(0, str(Path(__file__).resolve().parents[1] / 'scripts'))
import audit_release_artifacts as audit
import audit_release_toolchain as toolchain
import qualify_linux_build as linux


class AuditTest(unittest.TestCase):
    def publication_transport(self, routes, visited, responses=None):
        def open_request(_handler, request):
            visited.append(request.full_url)
            status, content = routes[request.full_url]
            headers = Message()
            if status != 200:
                headers['Location'] = content
                content = b''
            response = addinfourl(io.BytesIO(content), headers, request.full_url, status)
            response.msg = 'OK' if status == 200 else 'Redirect'
            if responses is not None:
                responses.append(response)
            return response
        return open_request

    def test_publication_rejects_insecure_intermediate_redirect_before_contact(self):
        start = 'https://repo.example.test/start'
        insecure = 'http://relay.example.test/intermediate'
        final = 'https://repo.example.test/final'
        routes = {start: (302, insecure), insecure: (302, final), final: (200, b'artifact')}
        visited = []
        responses = []
        transport = self.publication_transport(routes, visited, responses)
        with patch.object(audit.urllib.request.HTTPSHandler, 'https_open', new=transport), \
             patch.object(audit.urllib.request.HTTPHandler, 'http_open', new=transport):
            with self.assertRaisesRegex(ValueError, 'Insecure publication redirect'):
                audit.read_publication(start)
        self.assertEqual([start], visited)
        self.assertTrue(responses[0].closed)

    def test_publication_allows_secure_relative_and_cross_host_redirects(self):
        start = 'https://repo.example.test/folder/start'
        mirror = 'https://mirror.example.test/folder/file'
        final = 'https://mirror.example.test/final'
        routes = {start: (302, mirror), mirror: (301, '../final'), final: (200, b'artifact')}
        visited = []
        transport = self.publication_transport(routes, visited)
        with patch.object(audit.urllib.request.HTTPSHandler, 'https_open', new=transport), \
             patch.object(audit.urllib.request.HTTPHandler, 'http_open', new=transport):
            self.assertEqual(b'artifact', audit.read_publication(start))
        self.assertEqual([start, mirror, final], visited)

    def test_tool_download_blocks_insecure_intermediate_before_contact(self):
        start = 'https://tools.example.test/archive'
        insecure = 'http://relay.example.test/archive'
        final = 'https://tools.example.test/final'
        routes = {start: (302, insecure), insecure: (302, final), final: (200, b'tool')}
        visited = []
        transport = self.publication_transport(routes, visited)
        with tempfile.TemporaryDirectory() as directory, \
             patch.object(audit.urllib.request.HTTPSHandler, 'https_open', new=transport), \
             patch.object(audit.urllib.request.HTTPHandler, 'http_open', new=transport):
            destination = Path(directory) / 'archive'
            with self.assertRaisesRegex(ValueError, 'Insecure tool archive redirect'):
                toolchain.fetch(start, destination)
            self.assertFalse(destination.exists())
        self.assertEqual([start], visited)

    def test_tool_download_allows_secure_mirror_redirect(self):
        start = 'https://tools.example.test/archive'
        mirror = 'https://mirror.example.test/archive'
        payload = b'tool'
        visited = []
        transport = self.publication_transport({start: (302, mirror), mirror: (200, payload)}, visited)
        with tempfile.TemporaryDirectory() as directory, \
             patch.object(audit.urllib.request.HTTPSHandler, 'https_open', new=transport):
            destination = Path(directory) / 'archive'
            self.assertEqual(hashlib.sha256(payload).hexdigest(), toolchain.fetch(start, destination))
            self.assertEqual(payload, destination.read_bytes())
        self.assertEqual([start, mirror], visited)

    def test_sdk_download_rejects_unapproved_hop_and_accepts_official_redirect(self):
        start = 'https://dl.google.com/android/repository/start'
        outside = 'https://other.example.test/android/repository/archive'
        final = 'https://dl.google.com/android/repository/archive'
        payload = b'sdk'
        row = {'size': len(payload), 'checksum_algorithm': 'sha1',
               'checksum': hashlib.sha1(payload).hexdigest()}
        for redirect, accepted in [(outside, False),
                                   ('http://dl.google.com/android/repository/archive', False),
                                   ('https://dl.google.com/android/repository/%2e%2e/other', False),
                                   ('/android/repository/archive', True)]:
            with self.subTest(redirect=redirect), tempfile.TemporaryDirectory() as directory:
                visited = []
                routes = {start: (302, redirect), final: (200, payload)}
                transport = self.publication_transport(routes, visited)
                destination = Path(directory) / 'archive'
                with patch.object(audit.urllib.request.HTTPSHandler, 'https_open', new=transport), \
                     patch.object(audit.urllib.request.HTTPHandler, 'http_open', new=transport):
                    if accepted:
                        linux.download_sdk_archive(start, destination, row)
                        self.assertEqual(payload, destination.read_bytes())
                    else:
                        with self.assertRaisesRegex(ValueError, 'Unapproved SDK archive origin'):
                            linux.download_sdk_archive(start, destination, row)
                        self.assertFalse(destination.exists())
                self.assertEqual([start, final] if accepted else [start], visited)

    def test_uses_original_artifact_name_and_canonical_repository(self):
        row = dict(coordinate='androidx.room:room-runtime-android:2.7.0', name='room-runtime-android-2.7.0.aar')
        self.assertEqual('https://dl.google.com/dl/android/maven2/androidx/room/room-runtime-android/2.7.0/room-runtime-android-2.7.0.aar', audit.repository_url(row))
        row['name'] = '../escape'
        with self.assertRaises(ValueError):
            audit.repository_url(row)

    def test_missing_pom_declaration_is_not_invented(self):
        self.assertEqual([], audit.declarations(b'<project xmlns="http://maven.apache.org/POM/4.0.0"/>'))

    def test_pom_dtd_is_rejected_before_entity_expansion(self):
        project = '<project xmlns="http://maven.apache.org/POM/4.0.0"><licenses><license><name>&x;</name></license></licenses></project>'
        for dtd in ('<!DOCTYPE project [<!ENTITY x "expanded">]>',
                    '<!DOCTYPE project [<!ENTITY x invalid>]>',
                    '<!DOCTYPE project SYSTEM "file:///nonexistent/external.dtd">'):
            for encoding in ('utf-8', 'utf-16'):
                data = (dtd + project).encode(encoding)
                with self.subTest(dtd=dtd, encoding=encoding):
                    for parser in (audit.declarations, lambda value: audit.inherited_licences(value, [])):
                        with self.assertRaisesRegex(ET.ParseError, 'DTD declarations are not allowed'):
                            parser(data)

    def test_normal_pom_entities_comments_and_parse_errors_are_preserved(self):
        pom = ('<project xmlns="http://maven.apache.org/POM/4.0.0">'
               '<!-- literal <!DOCTYPE project> is only a comment -->'
               '<licenses><license><name>Research &amp; Development</name></license></licenses></project>')
        for encoding in ('utf-8', 'utf-16'):
            with self.subTest(encoding=encoding):
                self.assertEqual('Research & Development', audit.declarations(pom.encode(encoding))[0]['name'])
        with self.assertRaises(ET.ParseError):
            audit.declarations(b'<project>')

    def test_rejected_pom_prevents_successful_artifact_audit(self):
        archive = io.BytesIO()
        with zipfile.ZipFile(archive, 'w') as bundle:
            bundle.writestr('META-INF/LICENSE', 'license')
        artifact = archive.getvalue()
        pom = b'<!DOCTYPE project [<!ENTITY x "expanded">]><project><name>&x;</name></project>'
        def response(url, **_kwargs):
            stream = io.BytesIO(pom if url.endswith('.pom') else artifact)
            stream.url = url
            return stream
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            inventory = root / 'inventory.json'
            inventory.write_text(json.dumps({'artifacts': [dict(
                coordinate='org.test:test:1', name='test-1.jar', sha256=hashlib.sha256(artifact).hexdigest())]}))
            with patch.object(audit.PUBLICATION_OPENER, 'open', side_effect=response):
                result = audit.audit(inventory, root / 'output')
            self.assertTrue(result['artifacts'][0]['repository_bytes_match'])
            self.assertIn('DTD declarations are not allowed', result['artifacts'][0]['error'])
            self.assertFalse(result['all_repository_bytes_match'])
            self.assertFalse(json.loads((root / 'output/receipt.json').read_text())['release_ready'])

    def test_nested_aar_notices_are_hashed_without_extraction(self):
        jar = io.BytesIO()
        with zipfile.ZipFile(jar, 'w') as archive:
            archive.writestr('META-INF/LICENSE', 'notice')
        aar = io.BytesIO()
        with zipfile.ZipFile(aar, 'w') as archive:
            archive.writestr('classes.jar', jar.getvalue())
        self.assertEqual([dict(path='classes.jar!/META-INF/LICENSE', sha256=audit.digest(b'notice'), size=6)], audit.embedded_notices(aar.getvalue()))

    def test_module_metadata_binds_renamed_artifact_to_hash(self):
        row = dict(coordinate='androidx.room:room-runtime-android:2.7.0', name='room-runtime-release.aar', sha256='a' * 64)
        metadata = {'variants': [{'files': [dict(name=row['name'], sha256=row['sha256'], url='room-runtime-android-2.7.0.aar')]}]}
        self.assertTrue(audit.module_artifact_url(row, metadata).endswith('/room-runtime-android-2.7.0.aar'))
        metadata['variants'][0]['files'][0]['sha256'] = 'b' * 64
        with self.assertRaisesRegex(ValueError, 'uniquely bind'):
            audit.module_artifact_url(row, metadata)

    def test_mismatch_retains_failed_receipt_and_existing_output_is_preserved(self):
        data = io.BytesIO()
        with zipfile.ZipFile(data, 'w') as archive:
            archive.writestr('classes.txt', 'test')
        def response(url, **kwargs):
            stream = io.BytesIO(b'<project xmlns="http://maven.apache.org/POM/4.0.0"/>' if url.endswith('.pom') else data.getvalue())
            stream.url = url
            return stream
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            inventory = root / 'inventory.json'
            inventory.write_text(json.dumps({'artifacts': [dict(coordinate='org.test:test:1', name='test-1.jar', sha256='0' * 64)]}))
            output = root / 'audit'
            with patch.object(audit.PUBLICATION_OPENER, 'open', side_effect=response):
                result = audit.audit(inventory, output)
            self.assertFalse(result['all_repository_bytes_match'])
            self.assertFalse(json.loads((output / 'receipt.json').read_text())['release_ready'])
            before = (output / 'receipt.json').read_bytes()
            with self.assertRaises(FileExistsError):
                audit.audit(inventory, output)
            self.assertEqual(before, (output / 'receipt.json').read_bytes())

    def test_classes_named_notice_are_not_licence_documents(self):
        data = io.BytesIO()
        with zipfile.ZipFile(data, 'w') as archive:
            archive.writestr('org/example/UserNotice.class', 'not a legal notice')
            archive.writestr('org/example/LICENSE.class', 'executable bytecode')
            archive.writestr('META-INF/NOTICE.txt', 'real notice')
        rows = audit.embedded_notices(data.getvalue())
        self.assertEqual(['META-INF/NOTICE.txt'], [r['path'] for r in rows])

    def test_recursive_classes_jars_do_not_expand_without_a_depth_bound(self):
        data = io.BytesIO()
        with zipfile.ZipFile(data, 'w') as archive:
            archive.writestr('LICENSE', 'text')
        for _ in range(2):
            nested = io.BytesIO()
            with zipfile.ZipFile(nested, 'w') as archive:
                archive.writestr('classes.jar', data.getvalue())
            data = nested
        with self.assertRaisesRegex(ValueError, 'nested classes.jar'):
            audit.embedded_notices(data.getvalue())

    def test_insecure_origin_and_redirect_are_rejected(self):
        with patch.object(audit.PUBLICATION_OPENER, 'open') as request:
            with self.assertRaisesRegex(ValueError, 'HTTPS'):
                audit.read_publication('http://example.test/artifact')
            request.assert_not_called()
        response = io.BytesIO(b'untrusted redirect'); response.url = 'http://example.test/artifact'
        with patch.object(audit.PUBLICATION_OPENER, 'open', return_value=response):
            with self.assertRaisesRegex(ValueError, 'Insecure'):
                audit.read_publication('https://example.test/artifact')
