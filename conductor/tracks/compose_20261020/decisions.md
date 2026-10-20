# Compose experiment and adoption decision

Date2026-10-21. C01, R15/R26. Track compose_20261020.

## C01 — Isolated screen experiment; conditional production adoption

- Current responsibility: CalendarSliceActivity owns Views, permission requests,
  calendar refresh generation, selection preferences, executor and result messages.
- Options: retain Views; ComposeView in the existing Activity; separate Compose
  Activity with shared controller; isolated prototype; whole-screen rewrite.
- Select isolated standalone Gradle project under prototypes/compose for research.
  It is not included in root settings and cannot pull Compose into the working app.
  No prototype code reads providers, grants permissions or calls synchronization.
- Hypothesis: state-driven controls can preserve explicit-action gating, stable
  destination identity, saveable screen state and accessible semantics.
- Candidate matrix: JDK17/Gradle8.13/AGP8.13.2/Kotlin and Compose compiler2.3.21;
  BOM2026.03.00 maps UI1.10.5/Material3 1.4.0, activity-compose1.12.4. AndroidX/Kotlin
  publish Apache2.0 licences; artifact/POM and transitive evidence is recorded when
  resolved, not blanket clearance for community distribution. Use stable pinned
  artifacts rather than an alpha preview; current docs show a newer BOM2026.09.00.
- This bounded baseline uses the qualified compiler family; BOM/Activity metadata
  must pass actual AGP/SDK resolution. The BOM does not pin the compiler plugin.
- Role: prototype-only. No production dependency, persistence migration or ABI/FFI
  change. New app ID is disposable and private Activity is instrumentation-only.
- Takeover comparison: Compose could replace widget wiring/layout/semantics and
  recomposition, not provider access, transaction recovery or operation ownership.
  Material3 can replace owned visual controls; existing WorkManager, SQLite and
  executor responsibilities remain separately owned. No navigation/DI/MVI library
  needed for one screen; no extra architecture framework added speculatively.
- Cost: separate test/semantics tooling and newer AndroidX transitives; root app's
  old UI dependencies cannot simply be deleted. Measure resolution/build/test
  evidence; no performance or development-speed improvement assumed.
- Tests: policy contracts first, actual emulator semantics/click/recreation and
  large-font/RTL checks plus automated accessibility. TalkBack/Switch Access and
  actual permission/provider flows remain independent manual adoption gates.
- Expiry: reconsider after M3/M5 controller/state contracts and an accessible screen
  acceptance session. Promote only through a production integration change with
  no automatic effects, provider regressions, saved-state and lifecycle acceptance.
  Otherwise delete the whole standalone prototype and its optional tooling; root
  app requires no rollback. Do not preserve duplicate production UI indefinitely.
- Acceptance of this research track may close with a justified defer/reject decision
  as already permitted by the spec/workflow. It does not complete M6 production
  migration or turn incomplete M5 into an adopted prerequisite. Update the final
  decision from measured results before closure.

Primary sources: [migration](https://developer.android.com/develop/ui/compose/migrate/strategy),
[BOM](https://developer.android.com/develop/ui/compose/bom),
[compiler](https://developer.android.com/develop/ui/compose/compiler),
[accessibility testing](https://developer.android.com/develop/ui/compose/accessibility/testing).
