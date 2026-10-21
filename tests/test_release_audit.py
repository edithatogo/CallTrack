import io
from pathlib import Path
import sys
import unittest
import zipfile
sys.path.insert(0, str(Path(__file__).resolve().parents[1] / 'scripts'))
import audit_release_artifacts as audit


class AuditTest(unittest.TestCase):
    def test_uses_original_artifact_name_and_canonical_repository(self):
        row = dict(coordinate='androidx.room:room-runtime-android:2.7.0', name='room-runtime-android-2.7.0.aar')
        self.assertEqual('https://dl.google.com/dl/android/maven2/androidx/room/room-runtime-android/2.7.0/room-runtime-android-2.7.0.aar', audit.repository_url(row))
        row['name'] = '../escape'
        with self.assertRaises(ValueError):
            audit.repository_url(row)

    def test_missing_pom_declaration_is_not_invented(self):
        self.assertEqual([], audit.declarations(b'<project xmlns="http://maven.apache.org/POM/4.0.0"/>'))

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
