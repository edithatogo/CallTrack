"""Source trust metadata can only be expanded after fresh canonical byte checks."""
import hashlib
from pathlib import Path
import sys
import tempfile
import unittest
from unittest.mock import patch

sys.path.insert(0, str(Path(__file__).resolve().parents[1] / "scripts"))
import audit_room3_metadata as audit


def policy(entries):
    components = []
    for group, name, version, artifact, digest in entries:
        components.append(
            f'<component group="{group}" name="{name}" version="{version}">'
            f'<artifact name="{artifact}"><sha256 value="{digest}"/></artifact></component>'
        )
    return ('<verification-metadata><configuration><verify-metadata>true</verify-metadata>'
            '</configuration><components>' + ''.join(components) +
            '</components></verification-metadata>').encode()


class CandidateTrust(unittest.TestCase):
    def test_reject_dropping_or_changing_existing_approved_hash(self):
        previous = policy([("example", "core", "1.0", "core-1.0.jar", "a" * 64)])
        with self.assertRaisesRegex(ValueError, "may not be removed"):
            audit.pending(previous, policy([]))
        with self.assertRaisesRegex(ValueError, "may not be removed"):
            audit.pending(previous, policy([("example", "core", "1.0", "core-1.0.jar", "b" * 64)]))

    def test_verifies_one_new_canonical_artifact(self):
        body = b"real canonical artifact"
        digest = hashlib.sha256(body).hexdigest()
        prior = policy([("example", "core", "1.0", "core-1.0.jar", "a" * 64)])
        changed = policy([
            ("example", "core", "1.0", "core-1.0.jar", "a" * 64),
            ("example", "new", "2.0", "new-2.0.jar", digest),
        ])
        records, names = audit.pending(prior, changed)
        self.assertEqual(1, len(records))
        with patch.object(audit, "read_publication", return_value=body):
            verified = audit.verify_one(records[0], names)
        self.assertTrue(verified["sha256_verified"])

    def test_rejects_incorrect_bytes(self):
        entry = dict(coordinate="example:new:2.0", name="new-2.0.jar", sha256="0" * 64)
        with patch.object(audit, "read_publication", return_value=b"not trusted"):
            with self.assertRaisesRegex(ValueError, "Canonical hash mismatch"):
                audit.verify_one(entry, {})


if __name__ == "__main__":
    unittest.main()
