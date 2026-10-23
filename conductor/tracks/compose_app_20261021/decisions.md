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

## D14-5: Render historical import session history with Compose

**Decision:** Replace the import-history title, bounded session rows and empty
message with one lifecycle-disposed `ComposeView` that retains the
`import_history` ID. The Activity continues querying `SqliteImportStore`,
formatting localized labels, maintaining pagination cursors and owning session
IDs. Each rendered row receives an opaque per-query token; the Activity maps a
current token back to a session ID and ignores stale tokens. Opening a row still
uses the existing `loadSession` path and never confirms or resumes delivery.

**Alternatives:** Keep the history list native (no presentation parity); pass
session IDs into Compose (unneeded identity exposure); move database reads,
cursor rules or resume behavior into a composable (rejected); change import
schema or session ordering (out of scope).

**Promotion/removal:** This is a presentation-only migration. Retain the
`import_history` host ID, SQLite schema and Activity-owned query/resume logic.
History component tests and an API36 rendered Activity test cover empty state,
ordered rows and explicit row opening; broader permissions, lifecycle and OS
acceptance remain open.

**Rollback:** Restore the existing `TextView`/`LinearLayout` history rendering
and row listeners. No session, candidate, receipt or calendar data migration is
required.

## D14-6: Render historical import destination choice with Compose

**Decision:** Replace the native import `Spinner` with a lifecycle-disposed
`ComposeView` that retains `import_calendar_picker`. Render only the current
Activity-supplied calendar labels. Each provider choice receives a fresh opaque
token; the Activity retains the token-to-calendar-ID map, rejects stale tokens,
persists the selected calendar ID in the existing saved-state field, and
continues to own permission checks, provider queries and all import actions.
Use only the already-qualified Compose Foundation and UI stack. The list
expands inline so no Material dependency is introduced.

**Alternatives:** Keep the native spinner (no Compose control-migration
evidence); pass calendar IDs to the composable (unneeded identity exposure);
add Compose Material for a dropdown (not required for this bounded screen and
would expand the qualified dependency graph); move provider queries or import
delivery into the composable (rejected).

**Promotion/removal:** This is a presentation migration. Preserve the host ID,
saved calendar-ID key and Activity-owned `selected` ID. Component tests verify
selection is explicit and returns only an opaque token. Activity tests verify
the Compose host and the empty/permission-denied state. Real provider selection,
permission revocation, activity recreation with an actual selected calendar,
and broader accessibility acceptance remain open.

**Rollback:** Restore the native Spinner listener/adapter and remove the
Compose destination component. No schema, receipt, calendar event or
confirmation semantics change.

## D14-7: Render historical-import access action with Compose

**Decision:** Replace the native historical-import access button with a
lifecycle-disposed ComposeView retaining a stable `import_access_panel` host
ID. Compose only dispatches an explicit tap to the existing Activity-owned
`requestPermissions` call. Android's permission prompt and user consent remain
platform-owned. Use the already-qualified `ComposeActionButton`; do not add a
dependency or request a permission during composition.

**Alternatives:** Keep the action in Views (no control-migration evidence); move
permission checks or consent into the composable (rejected); add a new
permission library (not needed for this button).

**Promotion/removal:** This is a presentation migration only. Component tests
verify no callback during rendering and one callback per explicit tap; the
Activity test verifies the Compose host is present. Runtime grant, denial,
revocation and resulting provider behavior remain in the permission acceptance
matrix and are not inferred from the component callback test.

**Rollback:** Restore the native button and remove the new host ID/panel. No
permission state, provider access or data changes are involved.

## D14-8: Render historical-import explanatory text with Compose

**Decision:** Replace the historical-import intro TextView with a
lifecycle-disposed ComposeView retaining a stable `import_intro_panel` host
ID. The Activity formats the same localized string using its already selected
time-zone ID and supplies the text to a read-only Compose panel. Import range,
session creation and provider work remain Activity-owned.

**Alternatives:** Keep the one remaining static intro in Views (no complete
presentation migration evidence); move time-zone selection or import defaults
into composition (rejected); add a dependency (not needed).

**Promotion/removal:** This is a presentation-only migration. An API36
instrumentation test checks the exact formatted text and stable host, while the
Activity test verifies no import session was created. Broader font/RTL and
screen-reader acceptance remain open.

**Rollback:** Restore the TextView with its existing formatted resource string
and remove the Compose host/panel. No data or state migration is required.
