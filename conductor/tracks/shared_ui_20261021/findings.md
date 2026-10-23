# Track 17 findings — 2026-10-24

## Workflow and ownership map

| Area | Current owner/evidence | Shared UI boundary |
|---|---|---|
| Android active UI | Views remain authoritative; Track 14 has one reviewed read-only settings/status ComposeView slice | No Android production screen is replaced in this track slice. Android providers, permission prompts, lifecycle and navigation remain native/Activity-owned. |
| Desktop sample | Archived Track 9 prototype uses Compose Multiplatform, but its screen and window entry point are JVM-only | Move the screen rendering and presentation contract into `commonMain`; keep `Window`/application entry in desktop source. |
| Shared state | Track 15 exports narrow common call-selection/time-range rules only | Add only immutable sample screen state and explicit actions for selection/preview; do not import Android source models, receipts, providers or Track 16 parser as production dependencies. |
| macOS companion | Track 16 currently has preview-parser source only; no native app, file picker or EventKit adapter | Desktop JVM on macOS is a prototype renderer, not an AppKit/EventKit native package or accepted companion workflow. |
| Accessibility/package | Track 9 tests only offscreen desktop semantics; no active SDK/native package | Keep manual keyboard, screen-reader, scaling, RTL, focus, native packaging and upgrade gates open. |

The prototype screen remains synthetic: it offers a sample destination and two
sample entries, has no real import or save action, and exposes the platform
capability limitation. Shared rendering must be a pure function of immutable
state and callbacks. Recomposition cannot trigger external work.

## Current compatibility and dependency refresh

- Kotlin Multiplatform compatibility documentation currently lists Kotlin
  2.4.21 stable and a compatible Gradle range through 9.7.0, with AGP through
  9.3.1 and Xcode 26.4 for that Kotlin line:
  https://kotlinlang.org/docs/multiplatform/multiplatform-compatibility-guide.html
- JetBrains' Compose Multiplatform compatibility page lists 1.12.2 as the
  current stable release and says the latest Compose Multiplatform is compatible
  with the latest Kotlin version:
  https://kotlinlang.org/docs/multiplatform/compose-compatibility-and-versioning.html
- JetBrains' 1.12.2 release identifies Material 1.12.2 as stable while Material3
  remains alpha03. This prototype retains Material2 and does not adopt alpha UI:
  https://github.com/JetBrains/compose-multiplatform/releases/tag/v1.12.2
- Android's current KMP guidance recommends the dedicated
  `com.android.kotlin.multiplatform.library` plugin (minimum AGP8.10) and
  documents its variant/test differences:
  https://developer.android.com/kotlin/multiplatform/plugin
- Current local machine evidence: OpenJDK11 is the default. The explicitly
  selected OpenJDK17 installation at
  `/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home` reports
  `17.0.21.0.2+0`.
  No Android SDK path, Android SDK platforms, or connected ADB device is
  available. `/Applications` has no full Xcode; therefore no native Apple app
  packaging or Kotlin/Native qualification is claimed.

These versions are a prototype-only compatibility candidate, not production
adoption. The rendered desktop test resolved with Gradle8.13 and the captured
runtime graph is stored outside Git at
`.local/track17/desktop-runtime-dependencies.txt` (SHA-256
`bd274354ce36b0e5710af44ee892916775ef73570a129fc58c87180e702ceadb`) and the
test runtime graph is at `.local/track17/desktop-test-runtime-dependencies.txt`
(SHA-256 `1e7551ac48d3a1600b7a1a70a5772152904d51b3989ff04d9253f378ceb37bc0`). Selected
runtime nodes include Kotlin stdlib2.4.21, Compose runtime/foundation/material
1.12.2 and coroutines1.9.0, with AndroidX lifecycle2.11.0 and macOS-arm64 Skiko
0.150.2. The test graph adds Compose UI test1.12.2, Kotlin test2.4.21 and
coroutines-test1.9.0. Gradle emits a warning that Gradle8.13 will no longer be supported
starting at Kotlin2.5 and future use needs Gradle8.14.4 or later. This current
pin is buildable; do not suppress the warning. Root Android remains on its
selected AGP8.13.2/Kotlin2.3.21 graph. A future Android/KMP integration needs its
own version/graph audit and must not silently upgrade the root app. Compose
artifacts, Skiko natives, AndroidX and test transitive licences/Notices need a
resolved-graph review before distribution; this prototype graph has not had a
full licence/security audit.

The pre-implementation desktop test run failed as intended on missing common
state/action/reducer/screen contracts. After implementation, an offline run
passed the three common reducer tests and four rendered desktop interactions.
Compose Multiplatform's desktop JUnit test API warns that the old
`createComposeRule` entry point is deprecated; this implementation uses the v2
`androidx.compose.ui.test.junit4.v2.createComposeRule` API recommended in current
JetBrains testing guidance.

## Uncertainties and acceptance cases

- The true macOS workflow does not yet exist, so there is no product screen to
  share or native platform parity claim.
- Android app consumption of the common module cannot be compiled without the
  SDK, and actual rendering on Android is not available in this environment.
- Desktop JVM rendering under macOS does not qualify native AppKit interaction,
  accessibility, sandboxing, EventKit, application packaging or upgrade.
- Future promotion must verify common state/action parity, no external effects
  from rendering, correct destination invalidation, denial/busy/error/loading
  state rendering, keyboard/focus/accessibility per platform, window/activity
  recreation, packaging and preserving upgrade/recovery.
