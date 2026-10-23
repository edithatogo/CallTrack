# Track 14 findings — 2026-10-23

## Existing behavior and ownership

`AutomaticSettingsActivity` builds the screen programmatically and owns
permission checks, `CalendarContract` access through `AndroidCalendarSink`,
activation confirmation, scheduling actions, ledger counts, worker status,
executor coordination and resume-time reconciliation. UI automation addresses
stable resource IDs such as `auto_state`, `auto_counts`, `auto_feedback`,
`auto_last`, `auto_enable` and the calendar/filter controls.

The first migration boundary is the four read-only status/summary labels. The
Activity continues to format all strings from authoritative reads; Compose
receives only immutable display state. All controls, asynchronous work,
permission requests, confirmation and navigation remain Views/Activity-owned.
Each status label uses a lifecycle-disposed ComposeView retaining its original
view ID. Composition and recomposition perform no reads or writes.

## Dependency and compatibility refresh

- Android module: AGP 8.13.2, Kotlin 2.3.21, Java bytecode target 1.8, compileSdk
  36, minSdk 26, strict Gradle artifact and metadata SHA-256 verification.
- The prototype uses Compose BOM 2026.03.00, Activity Compose 1.12.4 and
  Material3, but prototype coordinates are not accepted as production trust.
- Android's current setup guide lists Compose BOM 2026.09.00 and requires the
  Compose compiler Gradle plugin version to match Kotlin. A root compile attempt
  with that BOM resolved Compose 1.12.1 and failed compatibility checks: those
  artifacts require compile SDK 37 and AGP 9.1, beyond this app's qualified
  compile SDK 36 / AGP 8.13.2. Track 14 selects stable BOM 2026.03.00, which
  maps to UI/Foundation 1.10.5 and passes AGP metadata checks and local source
  compilation. ComposeView supports incremental View/Compose coexistence.
- The production status Activity uses AndroidX `ComponentActivity` directly so
  lifecycle-aware ComposeViews receive an owner. Its selected1.7.0 runtime and
  eight additional lifecycle/AndroidX metadata files were checked against
  canonical Google Maven bytes; the overall strict policy now has940 entries.
- Existing test selectors use legacy view IDs; preserving them avoids
  needlessly changing the broader acceptance harness during this slice.

## Open acceptance

The status slice review found and fixed a lifecycle-owner integration failure
and a missing Compose test host declaration; API36 rendered and Activity tests
now pass. See [review-20261023](review-20261023.md) and the updated
[implementation evidence](implementation-evidence.md).

An earlier `connectedDebugAndroidTest` attempt stalled during AGP device-service
discovery on this AVD; that failed attempt remains local history. A later focused
run installed the packages and passed the component plus settings host tests.
TalkBack/Switch Access, font/RTL, permission revocation, process restoration,
manual logging/import/recovery migrations and broader provider regression checks
remain unaccepted.

## Recovery cards/action migration — 2026-10-24

`ReceiptRecoveryActivity` now projects up to ten recovery records into immutable
Compose card state and a separate previous/more navigation state. The activity
retains the data/provider read, permission and consent dialogs, effect checking,
suppression, reapproval, retention and event-launch behavior. Compatibility IDs
remain on the refresh ComposeView, feedback ComposeView, rows container and
native empty-state TextView; the new card and navigation ComposeViews have
dedicated IDs.

The recovery component and fixture-backed API36 recovery journeys passed with a
synthetic token-scoped LOCAL calendar. A separate existing historical-import
control selector failed in a broader selected UI test: `RenderedUi` could not
find `import_create`. Investigate that accessibility/selector path before
claiming historical-import or whole-track parity. Track14's wider screen-reader,
font, RTL, recreation and Android-version matrix remains open.
