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

## D14-3: Move recovery cards and controls behind Compose presentation

**Decision:** Render the bounded recovery card list, refresh/retention actions and
previous/more pagination with the already-qualified Compose Foundation stack.
The Activity continues to own ledger/calendar reads, permission requests,
confirmation dialogs, reconciliation, suppression, reapproval and retention
effects. Compose receives preformatted immutable card details, action
eligibility and opaque per-render tokens; the Activity retains the receipt and
calendar mapping and ignores stale tokens. Compose dispatches only explicit taps.
Preserve `recovery_refresh`,
`recovery_feedback`, `recovery_rows` and `recovery_empty`; add stable hosts for
the cards, retention action and navigation panels. Keep the retention policy
text before its clear-expired action, matching the original screen order.

**Alternatives:** Keep the remaining recovery UI in Views (no presentation
parity evidence); move provider/effect logic into composables (rejected); replace
the whole Activity or change receipt/persistence ownership (not required for
this incremental slice).

**Promotion/removal:** This is an incremental Android UI migration, not a change
to data authority or calendar semantics. Retain the native empty-state view and
legacy resource IDs until the existing automation migrates. Broader permission,
recreation, font/RTL and accessibility acceptance remains a Track14 gate.

**Rollback:** Restore the previous Activity card/button construction and remove
the recovery panels and two added host IDs; no stored schema or receipt identity
changes are involved.

## D14-4: Render recovery empty state with Compose

**Decision:** Move only the recovery empty-state message from `TextView` to a
lifecycle-disposed `ComposeView` retaining the `recovery_empty` resource ID.
The Activity continues to derive the visibility flag from the completed ledger
page. The composable receives that flag and the localized string and performs
no ledger/calendar reads or writes. This supersedes D14-3's choice to retain a
native empty-state view; the compatibility ID remains.

**Alternatives:** Keep a mixed native/Compose recovery list (rejected after the
cards and controls were migrated); move query or permission logic into
composition (rejected); remove the legacy ID (rejected to preserve existing
automation and host lookups).

**Promotion/removal:** Compose now owns all recovery list presentation while
the Activity owns data/query and effect authority. Retain the ID and native
Activity boundary until broader UI and provider acceptance pass.

**Rollback:** Restore the `TextView` and its visibility update; no persisted
state, schema or calendar behavior changes.
