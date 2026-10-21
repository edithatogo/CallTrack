"""Boundary tests for a distributable source/notices archive."""
import io
import importlib.util
from pathlib import Path
import tempfile
import unittest
import zipfile

SPEC = importlib.util.spec_from_file_location('release_bundle', Path(__file__).resolve().parents[1] / 'scripts/prepare_release_bundle.py')
bundle = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(bundle)


class BundleBoundaries(unittest.TestCase):
    def test_rejects_private_or_escaping_archive_paths(self):
        for name in ('../outside', '/absolute', '.local/phone.apk', 'secrets.jks', '.git/config', 'x/../y', 'x\\evil'):
            with self.subTest(name=name), self.assertRaises(ValueError):
                bundle.safe_path(name)

    def test_candidate_inventory_must_match_binary_names_and_hashes(self):
        rows = [{'coordinate': 'a:b:1', 'name': 'b-1.jar', 'sha256': 'a' * 64}]
        bundle.validate_inventory(rows, rows)
        for changed in ([], [dict(rows[0], sha256='b' * 64)], [dict(rows[0], name='other.jar')]):
            with self.assertRaises(ValueError):
                bundle.validate_inventory(changed, rows)

    def test_modified_or_external_source_cache_is_rejected(self):
        with tempfile.TemporaryDirectory() as tmp:
            root = Path(tmp)
            (root/'source.jar').write_bytes(b'changed')
            with self.assertRaises(ValueError):
                bundle.checked_file(root, 'source.jar', 'a' * 64)
            (root/'outside-link').symlink_to('/etc/hosts')
            with self.assertRaises(ValueError):
                bundle.checked_file(root, 'outside-link', 'a' * 64)

    def test_source_jars_reject_traversal_and_missing_source(self):
        for path in ('../evil.java', '/evil.java', 'only.txt'):
            data = io.BytesIO()
            with zipfile.ZipFile(data, 'w') as archive:
                archive.writestr(path, 'text')
            with self.subTest(path=path), self.assertRaises(ValueError):
                bundle.validate_source_zip(data.getvalue())

    def test_reproducible_archive_preserves_manifest_and_never_follows_links(self):
        with tempfile.TemporaryDirectory() as tmp:
            root = Path(tmp)
            (root/'a.txt').write_text('owned source')
            bundle.write_archive(root/'one.tar', {'source/a.txt': root/'a.txt'})
            bundle.write_archive(root/'two.tar', {'source/a.txt': root/'a.txt'})
            self.assertEqual((root/'one.tar').read_bytes(), (root/'two.tar').read_bytes())
            (root/'link').symlink_to(root/'a.txt')
            with self.assertRaises(ValueError):
                bundle.write_archive(root/'bad.tar', {'source/link': root/'link'})
