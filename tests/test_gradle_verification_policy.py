from pathlib import Path
import sys
import unittest
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
