# Review: Manual launcher Compose screen

Date: 2026-10-25
Scope: move the complete manual launcher presentation into a lifecycle-disposed
Compose root while retaining Activity-owned provider and delivery effects. This
review does not close Track14 or the physical/operational acceptance gates.

## Review outcome

- Replaced the manual launcher intro, native panel container and automatic
  settings Button with a single Compose screen hosted by the existing
  ScrollView. The existing viewport and inset behavior remain in place.
- Kept calendar permission requests, provider reads, destination-ID mapping,
  saved selection, reconciliation and manual delivery in
  `CalendarSliceActivity`. Composition renders supplied state and dispatches
  explicit callbacks only.
- Preserved the manual controls' accessibility resource identifiers through
  `testTagsAsResourceId`, and retained the existing `slice/calendar` preference.
- Added a component test for inert composition and explicit access, selection,
  logging and navigation actions. The hosted settings suite now invokes this
  test and asserts that the launcher has one Compose root, then runs the
  fixture-backed destination selection journey.
- Review caught a test compilation error where the Activity holder array was
  used instead of its Activity element. The assertion now inspects `activity[0]`;
  final instrumentation compilation and APK assembly pass.

## Verification

- JDK17 / SDK36: `:app:testDebugUnitTest`,
  `:app:compileDebugAndroidTestJavaWithJavac`,
  `:app:compileDebugAndroidTestKotlin`, `:app:lintDebug` and
  `:app:assembleDebugAndroidTest` pass.
- Python3.15.1: `tests.test_parity_ui_safety` and
  `tests.test_verify_conductor` pass (54 tests); `scripts/verify_conductor.py`
  passes (255 documents, 21 tracks, 25 synthetic scenarios).
- Pinned ktfmt0.59 and google-java-format1.24.0 checks pass; `git diff --check`
  passes.
- The first hosted run, [40169350891](https://github.com/edithatogo/CallTrack/actions/runs/40169350891),
  failed on API26, API33 and API36 at the rendered wait for the manual screen's
  four stable identifiers. The preceding picker wait and pure Compose callback
  test passed on each API; all fixture cleanup and test-package removal checks
  passed. The compound assertion did not identify which semantic tag was
  missing. Commit `ffab440` now exposes each stable identifier on its own
  semantics node and separates the rendered waits so a repeat failure is
  attributable to one selector. The new source revision still needs the hosted
  API26/API33/API36 rerun before that acceptance task can close.
- The local API36 emulator process is not available through the selected ADB
  endpoint, so no local rendered result is claimed. Physical-device and
  broader permission, lifecycle, accessibility and operational acceptance
  remain separate.

## Findings

The hosted identifier issue is corrected in source, pending a fresh matrix
result. No other implementation findings remain. Broader font/RTL, assistive
technology, permission/revocation, process restoration, physical-device and
operational acceptance remain separate Track14 gates.
