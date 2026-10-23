# Track 20 iOS readiness — 2026-10-24

This is a source-pinned preparation slice only. It does not implement or qualify
an iOS app. Previous research remains dated input; compatibility below was
refreshed against official vendor documentation on the date above.

## Current stage and product boundary

The product scope is a deliberate iOS manual/import/review/calendar companion.
Automatic Android-equivalent call acquisition is not promised and remains a
separate future investigation. The intended flow is a supported-file import,
redacted preview, explicit approval, one native calendar writer and local
readback/recovery; no cloud sync or invitation sending is implied.

| Area | Current owner/evidence | Gap before iOS implementation |
|---|---|---|
| Shared domain | Track 15 shares a bounded call-type selection/time-range slice on JVM/Android; macOS Kotlin/Native is still pending. | Finish and qualify the shared core, fixture parity and stable format contract across Android and macOS first. |
| Shared presentation | Track 17 has a synthetic desktop JVM prototype; Android integration and native macOS package are unaccepted. | Decide whether an iOS screen is suitable for Compose Multiplatform only after Android/macOS workflow and accessibility acceptance. Compose availability is not proof of app fit. |
| Import contract | Track 16 parser prototype consumes a proposed JSON shape; no Android producer or accepted interchange format exists. | Define producer-owned export, versioning, provenance, redaction, duplicate/ambiguity and recovery behavior before Swift/Kotlin iOS consumes it. |
| Calendar | No iOS EventKit adapter or selected calendar route exists. | Select a least-privilege flow and qualify denial, cancellation, edits/deletions, event readback and offline/process recovery on simulator and authorized device. |
| Build and package | No iOS target/project exists; full Xcode and `simctl` are unavailable locally. | Install/use a qualified full Xcode environment, pin toolchain and simulator target, and test a reproducible package. No installation is requested here. |
| Release | Track 5 identity, hosted CI, physical and observation gates remain separate. | Define iOS bundle identity, signing/provisioning custodian and distribution route in the release track before signing or publication. |

## Refreshed platform evidence

- Kotlin Multiplatform and Compose Multiplatform list iOS as Stable in their
  current platform stability tables. This only describes framework stability;
  it does not establish that this app's shared core, UI or native adapters are
  ready. [Kotlin platform stability](https://kotlinlang.org/docs/multiplatform/supported-platforms.html)
- Current Compose Multiplatform documentation lists version **1.12.2**, iOS
  **14** as its minimum, and requires the Compose compiler plugin version to
  match the Kotlin Multiplatform plugin. This is a future candidate only, not a
  pin or adoption decision. [Compose Multiplatform compatibility](https://kotlinlang.org/docs/multiplatform/compose-compatibility-and-versioning.html)
- Current Kotlin Multiplatform compatibility documentation lists Kotlin
  **2.4.20–2.4.21** with Xcode **26.4**, Gradle **7.6.3–9.7.0**, and AGP
  **8.5.2–9.3.1**. The root Android app remains Kotlin2.3.21/Gradle8.13/AGP8.13.2;
  these older root pins and the newer candidate are not evidence of a qualified
  iOS build. Refresh exact compatibility and transitive trust at implementation
  time. [Kotlin Multiplatform compatibility guide](https://kotlinlang.org/docs/multiplatform/multiplatform-compatibility-guide.html)
- Apple documents iOS 17's separate calendar write-only and full-access usage
  description keys and APIs. Write-only cannot read or delete events; full
  access is needed for direct fetch/edit/delete. Apple documents user-mediated
  EventKitUI editor/chooser routes, while its example APIs describe differing
  access requirements; the precise requirement depends on the selected UI API
  and supported OS range and must be checked against that API before coding.
  iOS 10–16 compatibility also requires the legacy
  `NSCalendarsUsageDescription` and older request path; the iOS 17
  `requestAccess(to: .event)` API no longer prompts and throws. Keep this as an
  explicit least-privilege product decision: the chosen path must meet exact
  readback and recovery requirements. [Apple EventKit access levels](https://developer.apple.com/documentation/eventkit/accessing-calendar-using-eventkit-and-eventkitui?changes=_4), [Apple EventKit store access](https://developer.apple.com/documentation/EventKit/accessing-the-event-store?language=objc)
- Apple's simulator guidance distinguishes simulated from physical-device
  verification and notes that simulator hardware features may be unavailable.
  It requires Xcode platform support for the selected destination. [Apple device and simulator testing](https://developer.apple.com/documentation/Xcode/running-your-app-on-simulated-or-physical-devices)

## Options and decision

| Option | Role | Decision now |
|---|---|---|
| SwiftUI + native EventKit adapters; consume the shared KMP core through a narrow framework boundary once Track 15/19 pass | Native Apple presentation, permissions, file selection, calendar and lifecycle | Preferred candidate for platform-specific behavior; do not implement until stage gates and actual shared contracts pass. |
| Compose Multiplatform for selected screens | Reuse presentation only if Track 17 and iOS usability/accessibility justify it | Defer. Stability and minimum OS support do not prove parity or suitability. |
| EventKitUI editor/chooser with write-only or system-mediated save | Lower permission scope and user-controlled event entry | Candidate for a simple single-event flow; may not satisfy batch import, durable reconciliation or exact event readback. Test and make a product decision later. |
| Full EventKit access | Direct calendar enumeration/readback/edit/delete needed for robust reconciliation | Candidate only if demonstrated required behavior cannot be met by the lower-privilege route; requires explicit disclosure and denial handling. |
| New cross-platform calendar or import dependency | Potential replacement for native adapters | Defer; no demonstrated capability gap or accepted format, and platform permission semantics still need native qualification. |

No dependency, permission scope, iOS deployment target, bundle ID or signing
identity is adopted by this preparation slice.

## Conditional iOS acceptance handoff

After stages 1–8 resolve, qualify the chosen target and package; shared golden
identity/rendering/null/time/overflow and supported-format fixtures; Swift/Kotlin
boundary and error mapping; file-picker preview/approval; EventKit permission
denial/revocation and selected-calendar behavior; exact event readback; user
edits/deletions; process interruption/restart and duplicate recovery; VoiceOver,
Dynamic Type, localization, keyboard/focus where applicable; simulator and
authorized physical-device behavior; signed preserving upgrade/rollback; and
actual remote synchronization only for explicitly selected routes. Keep each
evidence layer separate. No simulator or package test substitutes for physical
device, cloud, signing or operational release gates.
