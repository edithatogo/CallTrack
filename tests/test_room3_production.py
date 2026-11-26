"""Production persistence must be owned by Room 3 with preserved original file identities."""
import tomllib
from pathlib import Path
import unittest

ROOT = Path(__file__).resolve().parents[1]
SOURCE = ROOT / "app/src/main/kotlin/com/markobl/calllogsync/slice"


class Room3ProductionContract(unittest.TestCase):
    def test_room3_is_the_production_database_owner(self):
        build = (ROOT / "app/build.gradle.kts").read_text()
        catalog = tomllib.loads((ROOT / "gradle/libs.versions.toml").read_text())
        self.assertEqual(catalog["plugins"]["ksp"]["id"], "com.google.devtools.ksp")
        self.assertIn("alias(libs.plugins.ksp)", build)
        for kind, configuration in (("runtime", "implementation"), ("compiler", "ksp")):
            library = catalog["libraries"][f"androidx-room-{kind}"]
            self.assertEqual(library["module"], f"androidx.room3:room3-{kind}")
            self.assertEqual(library["version"]["ref"], "room")
            self.assertGreaterEqual(tuple(map(int, catalog["versions"]["room"].split("."))), (3, 0, 3))
            self.assertIn(f"{configuration}(libs.androidx.room.{kind})", build)
        room = (SOURCE / "RoomPersistence.kt").read_text()
        self.assertIn("Room.databaseBuilder", room)
        self.assertIn("AndroidSQLiteDriver", room)
        ledger = (SOURCE / "SqliteLedger.kt").read_text()
        imports = (SOURCE / "SqliteImportStore.kt").read_text()
        self.assertIn("calltrack-ledger.db", ledger)
        self.assertIn("calltrack-imports.db", imports)
        self.assertNotIn("room3-sqlite-wrapper", build)
        self.assertFalse((SOURCE / "RoomSqliteCompat.kt").exists())
        self.assertNotIn("getSupportWrapper", room)
        for store in (ledger, imports):
            self.assertNotIn("SQLiteOpenHelper(", store)
            self.assertNotIn("rawQuery(", store)

    def test_ledger_and_import_writes_use_suspend_daos(self):
        queries = (SOURCE / "RoomQueries.kt").read_text()
        for operation in (
            "storeIntent", "expirePayloads", "storeCheckpoint", "rollover",
            "insertCandidates", "storeOutcome", "storeProgress",
        ):
            self.assertRegex(queries, rf"\bsuspend fun {operation}\(")

    def test_preserving_migration_and_single_writer_are_nonnegotiable(self):
        room = (SOURCE / "RoomPersistence.kt").read_text()
        self.assertNotIn("fallbackToDestructiveMigration", room)
        self.assertIn("Migration(6, 7)", room)
        self.assertIn("Migration(2, 3)", room)
        self.assertIn("outbox_state", room)
        self.assertIn("payload_saved_at", room)
        self.assertIn("source_presentation_supplied", room)


if __name__ == "__main__":
    unittest.main()
