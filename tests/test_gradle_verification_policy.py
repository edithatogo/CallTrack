from pathlib import Path
import sys
import unittest
import hashlib
import json
sys.path.insert(0, str(Path(__file__).resolve().parents[1] / 'scripts'))
import audit_gradle_verification as policy


class VerificationPolicyTest(unittest.TestCase):
    def document(self, checksum='a' * 64, metadata='true', trust=''):
        return ('<verification-metadata><configuration><verify-metadata>' + metadata + '</verify-metadata>' + trust + '</configuration><components><component group="org.test" name="test" version="1"><artifact name="test-1.jar"><sha256 value="' + checksum + '"/></artifact></component></components></verification-metadata>').encode()

    def test_unverified_metadata_or_broad_trust_is_refused(self):
        for data in (self.document(metadata='false'), self.document(trust='<trust group=".*"/>')):
            with self.assertRaises(ValueError):
                policy.rows(data)

    def test_missing_or_invalid_hash_is_refused(self):
        with self.assertRaises(ValueError):
            policy.rows(self.document(checksum='generated is not trusted'))
        self.assertEqual(['a' * 64], policy.rows(self.document())[0]['sha256'])

    def test_renamed_variant_artifact_requires_allowlisted_module_bytes(self):
        artifact = b'aar payload'
        artifact_hash = hashlib.sha256(artifact).hexdigest()
        module = json.dumps({'variants': [{'files': [{'name': 'ui.aar', 'sha256': artifact_hash, 'url': 'ui.aar'}]}]}).encode()
        module_hash = hashlib.sha256(module).hexdigest()
        row = {'coordinate': 'androidx.compose.ui:ui-android:1.12.1', 'name': 'ui.aar'}
        allowed = {
            (row['coordinate'], 'ui-android-1.12.1.module'): {module_hash},
        }
        url = policy.module_fallback_url(row, artifact_hash, module, allowed)
        self.assertTrue(url.endswith('/ui-android/1.12.1/ui.aar'))
        with self.assertRaises(ValueError):
            policy.module_fallback_url(row, artifact_hash, module, {})
