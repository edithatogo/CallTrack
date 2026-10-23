# Track 17 decisions — 2026-10-24

## D17-01: Shared Compose prototype, desktop renderer only

**Status:** prototype-only; no production adoption.

Refactor the existing synthetic desktop screen into a Kotlin Multiplatform
`commonMain` presentation contract and render it through the existing desktop
JVM host. Use immutable screen state plus explicit actions, with state
transitions tested independently. Keep window launch, Android Activity,
permissions, data acquisition, persistence, EventKit and file selection in
platform-owned code. The screen and its tests contain sample constants only.

Refresh the isolated candidate to Kotlin2.4.21 and Compose Multiplatform1.12.2
with the existing Gradle8.13 wrapper and JDK17 for this prototype, using stable Material2. The candidate
versions are compatible according to current official guidance, but the build
must still resolve and pass locally. Use the installed JDK17 explicitly and an
isolated Gradle user home. Do not change the application/root build pins. Do not
add routing, DI, state-management or multiplatform library dependencies; explicit
state/action values are sufficient for one sample screen. Retain JUnit4 for
desktop rendered tests only; no test framework is a runtime dependency.

The official Android KMP library plugin is the documented future bridge, but no
Android SDK is available and the production app is not prepared to consume this
module. Do not configure an untestable Android target or change production app
integration in this slice. Add that target only with an SDK and explicit
compatibility/build graph qualification, then test actual Android rendering.
Desktop JVM on macOS remains distinct from a native macOS app.

Promote only after Tracks 14–16 provide a stable, useful native workflow and
Android/macOS rendered, accessibility, packaging, lifecycle, update and recovery
acceptance passes. Otherwise remove the standalone prototype without modifying
production state or the Android app.
