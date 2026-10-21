# Application asset provenance

Audit date:2026-10-22. Scope is the current app, not historical Git revisions.

| Material | Origin and reuse basis | Current disposition |
|---|---|---|
| Launcher foreground/background | Original geometric calendar/list vector and solid colour, authored in this project on2026-10-22; GPLv3 | Preferred editable XML is in `app/src/main/res/drawable/`; no external icon, font, tracing, image model or bitmap input |
| Adaptive icon wrappers | Minimal platform XML authored here; GPLv3 | Both launcher resources remain; supported floor is API26 |
| Upstream bitmap/vector launcher and Play store icon | Unestablished independent artwork origin | Removed from the current source/application; historical files remain in Git history and old evidence only |
| Default and German strings | Project-authored active controls and translations,2026-10-20 onward; GPLv3 | The44 unused upstream endpoint keys are removed from each locale; `app_name` replaced in both. Existing active translations are retained |
| Themes/night theme/colours | Functional source configuration from the GPLv3 upstream tree at aaa5644 | Retained under the source licence; simple colour values/platform properties, no embedded artwork/font/trademark. Unused style declarations are not a rights exception |
| IDs and backup/extraction XML | Project-authored functional configuration; GPLv3 | Retained unchanged |
| Library resources | Exact63 runtime artifacts from Google Maven/Maven Central; Apache2 declarations/source headers | Covered by third-party inventory, licence text and retained notices; not relabelled as project artwork |
| Fonts, screenshots, audio, original CallTrack APK/resources | None included in current project app | No reuse assumed; the private APK remains outside source/archive inputs |

The working package remains `org.calltrack.community.dev`; the label is
“CallTrack Community (dev)”. This is a development label, not selection or clearance
of a permanent release name/logo/trademark. App namespace/schema/receipt ownership
are unaffected. A store screenshot/banner/icon package is deliberately absent
until a channel and production identity are selected.

[Asset inventory](qualification/release/assets-provenance.json) binds every current
non-code file under `app/src/main` to a SHA256 and provenance class. The old audit
is preserved as historical evidence. A source bundle uses a current Git snapshot;
it never exports repository history containing superseded artwork.
