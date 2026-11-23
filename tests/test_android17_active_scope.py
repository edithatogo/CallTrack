"""Keep current Android release scope aligned with its executable configuration."""
import json
from pathlib import Path
import unittest

ROOT = Path(__file__).resolve().parents[1]


def scope_local_avd():
    return json.loads((ROOT / "conductor/active-scope.json").read_text())["local_primary_avd"]


class Android17Scope(unittest.TestCase):
    def test_native_android_configuration_matches_canonical_scope(self):
        scope = json.loads((ROOT / "conductor/active-scope.json").read_text())
        self.assertEqual("Android 17 (API 37)", scope["active_platform"])
        self.assertEqual(
            {"min_sdk": 37, "target_sdk": 37, "compile_sdk_minor": "37.1"},
            scope["required_android"],
        )
        gradle = (ROOT / "app/build.gradle").read_text()
        self.assertRegex(gradle, r"(?m)^\s*minSdk 37\s*$")
        self.assertRegex(gradle, r"(?m)^\s*targetSdk 37\s*$")
        self.assertRegex(gradle, r"(?s)compileSdk\s*\{\s*version = release\(37\)\s*\{\s*minorApiLevel = 1")
        self.assertNotIn("implementation project(':shared-core')", gradle)
        settings = (ROOT / "settings.gradle").read_text()
        self.assertNotIn("include ':shared-core'", settings)

    def test_deferred_tracks_are_not_implementing_other_platforms(self):
        scope = json.loads((ROOT / "conductor/active-scope.json").read_text())
        for track in scope["deferred_tracks"]:
            with self.subTest(track=track):
                path = ROOT / "conductor/tracks" / track
                meta = json.loads((path / "metadata.json").read_text())
                self.assertEqual("deferred", meta["implementation_status"])
                self.assertNotEqual("in_progress", meta["status"])
                self.assertTrue((path / "deferral.md").is_file())
                self.assertFalse(meta.get("production_adopted", True))

    def test_active_ui_matrix_only_checks_android17(self):
        workflow = (ROOT / ".github/workflows/track14-hosted-ui.yml").read_text()
        self.assertIn("api-level: 37", workflow)
        self.assertNotRegex(workflow, r"api-level: (26|33|36)\b")
        self.assertIn("calltrack-track14-api37", workflow)
        self.assertEqual("calltrack-android17-local", scope_local_avd())


if __name__ == "__main__":
    unittest.main()
