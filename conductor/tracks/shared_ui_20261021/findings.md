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
- JetBrains' Compose Multiplatform compatibility page listed 1.12.2 during the
  original desktop-only prototype; that version is not compatible with the
  locally qualified Android toolchain because its Android artifacts now require
  API37.1/AGP9.1. The bounded Android host therefore pins the separately
  qualified 1.10.3 release while API36/AGP8.13.2 remains selected:
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
  The explicit SDK under `.local/android-sdk` exposes API32/API36 with
  build-tools30.0.3/35.0.0; its API36 emulator reports `offline` at the latest
  check. `/Applications` has no full Xcode; therefore no native Apple app
  packaging or Kotlin/Native qualification is claimed.

The original desktop-only 1.12.2 compatibility review below is historical and
its audit remains preserved in [dependency-runtime-audit.json](dependency-runtime-audit.json).
It is superseded as the current candidate by the Android-compatible Compose
Multiplatform1.10.3 pin described later in this document. The original rendered
desktop test resolved with Gradle8.13 and its captured runtime graph is stored
outside Git at
`.local/track17/desktop-runtime-dependencies.txt` (SHA-256
`bd274354ce36b0e5710af44ee892916775ef73570a129fc58c87180e702ceadb`) and the
test runtime graph is at `.local/track17/desktop-test-runtime-dependencies.txt`
(SHA-256 `1e7551ac48d3a1600b7a1a70a5772152904d51b3989ff04d9253f378ceb37bc0`). Selected
runtime nodes include Kotlin stdlib2.4.21, Compose runtime/foundation/material
1.12.2 and coroutines1.9.0, with AndroidX lifecycle2.11.0 and macOS-arm64 Skiko
0.150.2. The historical test graph adds Compose UI test1.12.2, Kotlin test2.4.21 and
coroutines-test1.9.0. Gradle emits a warning that Gradle8.13 will no longer be supported
starting at Kotlin2.5 and future use needs Gradle8.14.4 or later. The historical
pin was buildable for the desktop-only target; do not suppress the warning.
Root Android remains on its selected AGP8.13.2/Kotlin2.3.21 graph. Compose
artifacts, Skiko natives, AndroidX and test transitive licences/Notices were
audited from the resolved desktop runtime and test runtime classpaths on
2026-10-24; see [the runtime audit](dependency-runtime-audit.json). The
inventory contained 45 runtime and 18 test-only artifacts (63 unique). Fresh
canonical repository downloads matched all 63 recorded artifact hashes, every
POM had an effective licence declaration, and 37 embedded licence/notice files
were fingerprinted. All 45 runtime artifacts declare Apache-2.0. The 18
test-only artifacts include EPL1.0, BSD-3-Clause, MIT, and GPL-2.0 with the
Classpath Exception; they are not on the runtime classpath. This is metadata
and byte-provenance evidence, not legal clearance, a vulnerability assessment,
or build-tool trust. Gradle plugins/compiler artifacts, actual notice-text
bundling, future Android/native package graphs, and legal review remain before
distribution or adoption.

The pre-implementation desktop test run failed as intended on missing common
state/action/reducer/screen contracts. After implementation, an offline run
passed the three common reducer tests and four rendered desktop interactions.
The current final 1.10.3 prototype uses
`androidx.compose.ui.test.junit4.createComposeRule`; the v2 test package was
introduced in the newer 1.12.x line and is not present in this pinned graph.

## Android-host qualification — 2026-10-24

The synthetic screen now compiles through the dedicated KMP Android target and
an Android Activity host. The host has no dangerous permissions, provider,
calendar, file-picker, account or persistence integration. Activity lifecycle
and state handling are isolated in the Android host.

Compose Multiplatform1.12.2's resolved AndroidX artifacts failed AAR metadata
validation against compileSdk36/AGP8.13.2; the release requires API37.1/AGP9.1.
The bounded prototype uses 1.10.3. Kotlin2.4.21 continues to warn that Gradle
8.13 will be unsupported starting with Kotlin2.5; the wrapper remains unchanged.
This pin does not constitute final production dependency approval.

The debug APK assembled at
`prototypes/multiplatform-ui/android-host/build/outputs/apk/debug/android-host-debug.apk`
is 8.9 MiB, SHA-256
`3889951175e4654fc2d9ea32bc256d16a1e053d19f561824b55972b97ea667cc`.
`desktopTest` passed seven tests (three reducer and four rendered desktop),
with zero failures/errors. The fresh Android graph has 91 unique artifacts,
all matching canonical HTTPS repository bytes and all carrying effective POM
license metadata; 55 embedded notice/license files were fingerprinted. The
updated desktop graph has 63 artifacts with matching bytes and effective
metadata; 16 embedded notice/license files were fingerprinted. Full receipt
and input hashes are in
[android-host-dependency-audit.json](android-host-dependency-audit.json).

These audits are not legal clearance, vulnerability assessment, build-plugin
or compiler trust verification, full NOTICE text assembly, or a production
SBOM. Android installation/rendering and native macOS acceptance remain
unverified. No emulator acceptance is inferred from successful APK assembly.

## Uncertainties and acceptance cases

- The true macOS workflow does not yet exist, so there is no product screen to
  share or native platform parity claim.
- The synthetic Android host and KMP Android target now compile and package,
  but the production app does not consume the shared module; Android rendering
  remains unavailable while the emulator is offline.
- Desktop JVM rendering under macOS does not qualify native AppKit interaction,
  accessibility, sandboxing, EventKit, application packaging or upgrade.
- Future promotion must verify common state/action parity, no external effects
  from rendering, correct destination invalidation, denial/busy/error/loading
  state rendering, keyboard/focus/accessibility per platform, window/activity
  recreation, packaging and preserving upgrade/recovery.
