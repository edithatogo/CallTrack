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
        gradle = (ROOT / "app/build.gradle.kts").read_text()
        self.assertRegex(gradle, r"(?m)^\s*minSdk = 37\s*$")
        self.assertRegex(gradle, r"(?m)^\s*targetSdk = 37\s*$")
        self.assertRegex(gradle, r"(?s)compileSdk\s*\{\s*version = release\(37\)\s*\{\s*minorApiLevel = 1")
        self.assertNotIn('project(":shared-core")', gradle)
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

    def test_native_screens_use_one_compose_root_and_compose_insets(self):
        screens = ROOT / "app/src/main/kotlin/com/markobl/calllogsync/slice"
        root = (screens / "CallTrackScreen.kt").read_text()
        self.assertEqual(1, root.count("ComposeView(this)"))
        self.assertIn("DisposeOnViewTreeLifecycleDestroyed", root)
        self.assertIn("enableEdgeToEdge()", root)
        self.assertIn("contentWindowInsets = WindowInsets.safeDrawing", root)
        self.assertIn(".padding(padding).consumeWindowInsets(padding).imePadding()", root)
        self.assertIn("verticalScroll(", root)
        self.assertFalse((screens / "ActivityInsets.kt").exists())
        self.assertFalse((screens / "ComposeActionButton.kt").exists())
        navigation = (screens / "CallTrackRoute.kt").read_text()
        self.assertEqual(1, navigation.count("setCallTrackContent {"))
        self.assertEqual(1, navigation.count("rememberNavBackStack(root)"))
        self.assertEqual(1, navigation.count("NavDisplay("))
        for name, route in (("CalendarSlice", "Manual"), ("AutomaticSettings", "Automatic"),
                            ("HistoricalImport", "Import"), ("ReceiptRecovery", "Recovery")):
            with self.subTest(screen=name):
                source = (screens / f"{name}Activity.kt").read_text()
                self.assertEqual(1, source.count(f"setCallTrackNavigation(CallTrackRoute.{route})"))
                self.assertEqual(1, navigation.count(f"entry<CallTrackRoute.{route}>"))
                self.assertNotIn("ScrollView", source)
                self.assertNotIn("LinearLayout", source)
                self.assertNotIn("setContentView", source)

    def test_durable_delivery_is_the_single_calendar_insertion_path(self):
        slice_source = ROOT / "app/src/main/kotlin/com/markobl/calllogsync/slice"
        writers = sorted(path.name for path in slice_source.glob("*.kt")
                         if "sink.insert(" in path.read_text())
        self.assertEqual(["DurableDelivery.kt"], writers)
        self.assertIn("resolver.insert(Events.CONTENT_URI", (slice_source / "AndroidCalendarSink.kt").read_text())
        self.assertNotIn("fun run(", (slice_source / "SliceEngine.kt").read_text())
        self.assertNotIn("insertReceipt(", (slice_source / "RoomQueries.kt").read_text())

    def test_active_ui_matrix_only_checks_android17(self):
        workflow = (ROOT / ".github/workflows/track14-hosted-ui.yml").read_text()
        self.assertIn("api-level: 37", workflow)
        self.assertNotRegex(workflow, r"api-level: (26|33|36)\b")
        self.assertIn("calltrack-track14-api37", workflow)
        self.assertEqual("calltrack-android17-local", scope_local_avd())


if __name__ == "__main__":
    unittest.main()
