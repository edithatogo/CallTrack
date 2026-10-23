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

## Final production decision — retain Views; defer adoption

The experiment demonstrates a feasible Compose rendering/state boundary; it does
not justify replacing the current screen yet. Retain the working Views UI. M3's
operation ownership/import/status contract and full M5 remain unsettled, prototype
access is simulated rather than OS truth, and manual TalkBack/Switch Access/task
acceptance has not occurred. Adopting now would couple UI and controller changes
without the required real-behaviour evidence. This is a justified production
deferral under the original spec, not rejection of Compose or completion of M6.

Production implementation in this research track is therefore **not applicable**.
No adopted screen needs to be silently grandfathered into acceptance. Research
closure requires the bounded results, reviewed fixes and explicit handoff below.
The prototype stays isolated for reproducible research; its dependencies remain
prototype-only. The optional CI job may be removed with the standalone directory
and harness when superseded. No root app data, dependency or code rollback is needed.

Reopen the production M6 decision when: (1) M3/M5 controller/state contracts are
stable; (2) real permission/revocation, destination refresh/loss, busy/recovery and
Activity/process restoration cases have an authoritative state model; (3) manual
accessibility and localization criteria are scheduled against an actual screen;
(4) a fresh compatible dependency graph and supported-API matrix are qualified.
Then choose one screen/section with ComposeView coexistence and explicit disposal,
keep effects in the controller, and prove parity before removing its Views. Compare
native/owned/library takeover again; prototype versions are not a production lock.
Compose Multiplatform work must not assume an Android Compose UI already exists.

Final result:3 JVM/25 host passes, build/lint success, but5/5 foreground-guarded UI
tests fail because Android system crash windows obstruct the emulator. Earlier
semantics success is not accepted as visual proof. Both packages are removed.
This strengthens the retain/defer decision; resolve the environment and rerun all
strict native/manual gates before any production adoption. No successful full
prototype qualification, screen migration or library performance claim is made.
