# Track 14 decisions — 2026-10-23

## D14-1: Begin with read-only automatic-settings status

**Decision:** Use ComposeView for the four status/summary strings only, retaining
all Views and Activity-owned effects. Use a small immutable screen-state type,
with the Activity remaining the only producer. Preserve resource IDs for the
status host and existing controls. Use `DisposeOnViewTreeLifecycleDestroyed` so
the composition cannot outlive the Activity view lifecycle.

**Alternatives:** Keep all Views (lowest cost but no migration evidence); replace
the whole screen (larger lifecycle/permission risk); move state/effects into a
ViewModel now (not needed for this presentation-only first slice); use
prototype versions without a fresh trust audit (rejected).

**Dependency rule:** Match `org.jetbrains.kotlin.plugin.compose` to the
qualified Kotlin 2.3.21 plugin. The latest official BOM (2026.09.00) resolves to
Compose 1.12.1, which fails this app's AGP 8.13.2 / compile SDK 36 compatibility
check by requiring AGP 9.1 / API 37. Use the previously prototyped stable BOM
2026.03.00 only after independently resolving and auditing the production graph;
depend on only UI/foundation and the Compose test harness required by this
slice. Keep dependency verification enabled and compare every new artifact to
canonical repository bytes.

**Promotion/removal:** This boundary is production app code but does not retire
any View or alter product behavior. Extend Compose screen-by-screen only after
the current slice passes source, UI, accessibility and lifecycle checks. Remove
the status Views only when the existing automation and native acceptance prove
parity.

**Rollback:** Revert the Compose host and dependency changes; existing label
behavior is retained in git history and no persisted state/schema changes.

## D14-2: Scoped prerequisite deferral

Proceed with local Track 14 work while Track 5/6 acceptance remains open, with
the evidence, consequences and reconsideration trigger in
`prerequisite-deferral.md`. This does not waive release, physical, cloud or
observation acceptance.
