# Shared UI findings

Research refreshed2026-10-21, baseline `2d2de24`. The working Android app uses
Views. Track7's Compose screen is isolated and its final guarded native UI tests
failed on system dialogs. Track8 defines sharing contracts but has no KMP module.
Track12 has not qualified an iOS/desktop product. These outcomes remain in force.

## Product/workflow map

| Workflow | Common screen candidate | Platform-specific work and acceptance |
|---|---|---|
| Destination choice | Labels, explicit selection and empty/error/busy states | CalendarContract on Android; EventKit or service adapter elsewhere; list actual writable calendars and handle permission/account changes |
| Manual/import preview | Source provenance, actual duration, exclusion/review controls | Native file picker/entry, validation, timezone policy, duplicate/edited-event handling; none implemented in this experiment |
| Delivery status | Pending/review/confirmed labels and retry explanation | Durable outbox and native scheduling; cloud confirmation stays distinct from local readback |
| Notes/journal | Search/list/detail forms if later justified | Track11 owns needs, retention and editing; do not invent implemented journal features |
| Permission/acquisition setup | Capability-aware explanation | Native prompts and settings routes; no Android-equivalent cellular access assumed on iOS/desktop |
| Navigation/accessibility | Shared semantics and state where appropriate | Back gestures, window/keyboard focus, VoiceOver/TalkBack, text input and scalable layouts require target-specific testing |

Proposed first companion workflow: deliberate manual/import preview and later
calendar creation after native capability qualification. Desktop JVM is the chosen
**experiment target**, not a selected production platform. iOS manual/import remains
a proposal for Track12. No automatic capture parity, live sync or file import is
proved by rendering an example screen.

## Options and current code takeover

| Option | Benefit | Cost / current disposition |
|---|---|---|
| Retain Android Views, use native SwiftUI/AppKit for a later companion | Preserve qualified app behavior; native platform conventions | Duplicate small screen/state logic; no native companion exists yet; retain working Android UI |
| KMP state with native UI | Reuse rules while retaining native interactions | Requires Track8 contract/packaging qualification; viable future alternative, no KMP core adopted |
| Compose Multiplatform UI with native adapters | Reuse layout/semantics and test vocabulary | Native rendering/binaries, Kotlin/Compose versions, accessibility and platform conventions need qualification; prototype-only |
| Extend current Android Compose prototype directly | Familiar synthetic screen | That project uses Android APIs/Activity and is not common UI; must extract deliberately, never treat it as portable unchanged |
| New routing/DI/state framework | Could replace controller/lifecycle boilerplate | No multi-screen product or demonstrated need; constructor/callback state is sufficient for this trial; defer |
| Flutter/React Native/Rust UI rewrite | Alternative runtime or reuse | No measured advantage for current Java→Kotlin path. Rust is not a UI/platform-permission substitute; Track13 owns concrete component evidence |

Current CalendarSliceActivity owns widgets, refresh/busy state, executor and
provider coordination. Compose may replace widget rendering, but cannot silently
take over durable orchestration or provider lifecycle. No production code is
retired; no second ledger or sync implementation is introduced.

## Version/dependency refresh

The current compatibility guide lists Compose Multiplatform1.12.2 and separate
Kotlin/Compose plugin version requirements. Desktop runtime and distribution JDK
requirements differ. Framework availability is not application acceptance.
[Compatibility](https://kotlinlang.org/docs/multiplatform/compose-compatibility-and-versioning.html).

The trial pins the older Compose plugin1.10.3, Kotlin/JVM + Compose compiler2.3.21,
JDK17 and the existing Gradle8.13 wrapper to reduce unrelated toolchain changes and
align UI generation with the existing Android experiment. This is a bounded
compatibility experiment, not a newest-version recommendation. The1.10.3 release
lists Material3 as alpha; the trial uses Material2 instead. Resolved artifact
versions, not this shortlist, determine the actual graph.
[Versioned1.10.3 release](https://github.com/JetBrains/compose-multiplatform/releases/tag/v1.10.3).

The newer1.11.1 line changed Apple architecture support and introduced v2 testing;
iOS native text input remains an explicit experimental option in that release.
Do not assume an old test API or Intel simulator target remains valid when updating.
[Versioned1.11.1 notes](https://kotlinlang.org/docs/multiplatform/whats-new-compose-111.html).

Desktop Compose adds Skiko/Skia and native target artifacts plus Kotlin/AndroidX
transitives. Kotlin/Compose are Apache-2.0 upstream; JUnit4 is EPL-1.0 and the native
rendering graph has additional notices. A resolved artefact licence/notice audit is
required before distribution; no licence compatibility or security sign-off is
inferred from build success. Only local cache downloads are needed for the trial.
[Compose source/licence](https://github.com/JetBrains/compose-multiplatform),
[JUnit licence](https://github.com/junit-team/junit4/blob/main/LICENSE-junit.txt).

## Primary-source acceptance inputs

- [UI testing](https://kotlinlang.org/docs/multiplatform/compose-test.html): common
  test API and desktop JUnit support; this trial tests a single JVM renderer.
- [Platform behavior differences](https://kotlinlang.org/docs/multiplatform/compose-platform-specifics.html): input/navigation behavior needs platform review.
- [iOS accessibility](https://kotlinlang.org/docs/multiplatform/compose-ios-accessibility.html): native accessibility integration still needs actual app tests.
- [Native desktop distributions](https://kotlinlang.org/docs/multiplatform/compose-native-distribution.html): packaging is separate from running tests; signing/notarization and installer acceptance are not executed here.

No native Swift framework, iOS simulator, installed desktop distribution, physical
phone or personal data is used. Startup/size/memory/battery/performance comparisons
are not measured; prototype build effort cannot predict long-term maintenance cost.
