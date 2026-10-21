"""Cold Linux-build boundary tests; no Docker or network required."""
import hashlib
import importlib.util
import io
from pathlib import Path
import tempfile
import unittest
import zipfile

SPEC = importlib.util.spec_from_file_location('linux_build', Path(__file__).resolve().parents[1] / 'scripts/qualify_linux_build.py')
linux = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(linux)


class LinuxBoundaries(unittest.TestCase):
    def test_existing_work_cache_is_not_reused(self):
        with tempfile.TemporaryDirectory() as tmp:
            path = Path(tmp)/'work'
            path.mkdir()
            (path/'cached-artifact').write_text('old')
            with self.assertRaises(FileExistsError):
                linux.fresh_directory(path)
            self.assertEqual('old', (path/'cached-artifact').read_text())

    def test_sdk_origin_and_path_are_restricted(self):
        for url in ['http://dl.google.com/android/repository/a.zip', 'https://example.org/a.zip', 'https://dl.google.com/other/a.zip']:
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
