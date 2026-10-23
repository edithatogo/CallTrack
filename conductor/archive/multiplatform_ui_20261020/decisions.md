# Dependency decision UI-D01

- Owner/date/refresh: Track9/M8,2026-10-21. Requirements R17/R24, U01–U08;
  [findings](findings.md), [verification](verification.md).
- Existing implementation: Android Views/CalendarSliceActivity, isolated Android
  Compose trial, no shared core or companion. Potential replacement is screen
  rendering/state wiring only; native adapters and ledger ownership remain separate.
- Compared options: retained Views + later native UI; KMP state/native UI;
  Compose Multiplatform; additional state/router libraries; alternate runtime; defer.
- Candidate versions/licences/matrix: findings records selected trial versions,
  current newer releases, native dependencies and unresolved licence audit. No
  production-compatible Apple/Android graph has been selected.
- Intended role: Compose1.10.3/Kotlin2.3.21/JUnit4.13.2 are **prototype-only** in
  a standalone JVM project absent from root settings. No app dependency change.
- Hypothesis: one declarative screen can render/test explicit destination gating,
  stale-preview invalidation and capability disclosure on desktop without provider
  or shared-core coupling. Tests use offscreen desktop renderer and sample constants.
- Cost: Kotlin/compiler plugin, rendering natives/transitives and target packaging;
  native input/accessibility and durable state remain separate. No performance or
  development-time superiority is established.
- Test/security/error plan: meaningful failing UI contracts before screen exists;
  rendered U01–U03 assertions, source-bound receipt, dependency graph and host checks.
  U04–U07 remain future actual-native/controller/manual acceptance.
- Production decision: **retain current Android UI; defer shared-UI adoption**.
  This trial can inform later design but Track7's Android adoption and Track8's
  core contracts remain unfulfilled. A desktop result cannot qualify iOS.
- Expiry/removal: reconsider when Track12 chooses a useful feasible platform and
  Track8 contracts plus real Android state are ready. Refresh versions and tests;
  delete isolated project/build products if no useful companion is selected.
  Promotion requires replacing duplicated rendering rather than retaining two
  production UI owners, resolved licences and reproducible target artifacts.
- Migration/rollback: no app/schema/config migration or dual writes. Remove only
  the experiment and its ignored build directory to retire it; leave working app
  and global Gradle cache intact. Future production migration needs screen-by-screen
  parity, persisted-state compatibility and a tested return to retained UI.
- Actual validation: recorded in verification; never infer manual accessibility,
  remote sync, signed distribution or operational acceptance from compiler/tests.
- Handoff: Track12 capability scope; Track8 shared contracts; Track7 Android UI
  reopening; Track10 destinations/auth; Track11 journal needs. Conditional research
  closure does not satisfy M8 production exit gates.
