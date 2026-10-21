"""Historical schemas must come from committed source, not a version label on modern tables."""
import hashlib
import json
from pathlib import Path
import re
import subprocess
import unittest

ROOT = Path(__file__).resolve().parents[1]


class HistoricalSchemaTest(unittest.TestCase):
    def test_fixtures_are_exact_creates_from_each_frozen_historical_schema(self):
        rows = json.loads((ROOT / 'qualification/release/schema-provenance.json').read_text())
        self.assertEqual([1, 2, 3, 4, 5], [r['version'] for r in rows])
        rows.append(json.loads((ROOT / 'qualification/release/import-schema-provenance.json').read_text()))
        for row in rows:
            data = subprocess.check_output(['git', 'show', row['commit'] + ':' + row['source_path']], cwd=ROOT)
            self.assertEqual(row['source_sha256'], hashlib.sha256(data).hexdigest())
            self.assertIn('super(context, name, null, ' + str(row['version']) + ');', data.decode())
            statements = []
            for expression in re.findall(r'db\.execSQL\((.*?)\);', data.decode(), re.S):
                sql = ''.join(json.loads(s) for s in re.findall(r'"(?:[^"\\]|\\.)*"', expression))
                if sql.startswith(('CREATE TABLE', 'CREATE INDEX')) and sql not in statements:
                    statements.append(sql)
            fixture = (ROOT / row['fixture']).read_bytes()
            self.assertEqual(row['fixture_sha256'], hashlib.sha256(fixture).hexdigest())
            self.assertEqual('\n'.join(statements) + '\n', fixture.decode())
            self.assertEqual(row['statement_count'], len(statements))
