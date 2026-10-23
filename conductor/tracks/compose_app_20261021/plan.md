# Plan: Jetpack Compose Android implementation

## Phase 1: Refresh mapping and requirements

- [x] Task: Read prerequisite evidence and archived research; map behaviour, native APIs, dependencies and code ownership.
- [x] Task: Record findings, refined requirements and explicit acceptance/unsupported cases.
- [x] Task: Phase Verification & Checkpoint (refer to ../../workflow.md).

## Phase 2: Dependency and prototype decision

- [x] Task: Compare retained/native/owned/library replacement and deferral, including prototype-only versus long-term responsibilities.
- [x] Task: Run only uncertainty-resolving bounded experiments; record versions/licences/transitives/toolchain/cleanup and expiry criteria.
- [x] Task: Record adoption/deferral decision and refine implementation/rollback tasks before production change.
- [x] Task: Phase Verification & Checkpoint (refer to ../../workflow.md).

## Phase 3: Conditional implementation

- [x] Task: Establish golden/contract baseline and a meaningful failing regression before the manual logging presentation change; the regression remained red until the Compose panel was added.
- [x] Task: Refresh controller/lifecycle/permission mapping and Compose/toolchain/transitive compatibility; choose stable pinned versions from actual evidence.
- [x] Task: Implement the read-only automatic status presentation with ComposeView coexistence and explicit disposal; no provider effects in composition. Other settings controls remain Views. (19bb361)
- [x] Task: Migrate the manual “Log latest call” action and status presentation to a lifecycle-disposed ComposeView using the already-qualified Compose UI/Foundation stack. Preserve the native destination picker and permission prompts; keep all delivery and provider work in the Activity callback. The focused instrumentation test compiles; rendered execution remains pending an available emulator.
- [x] Task: Migrate historical import summary and feedback to separate lifecycle-disposed ComposeViews with Compose Foundation text and polite announcements. Keep date/type inputs, destination selection, history navigation, confirmations and bounded delivery under the Activity; preserve `import_summary` and `import_feedback` IDs.
- [x] Task: Migrate receipt recovery feedback to a lifecycle-disposed ComposeView using the existing Compose Foundation stack. Keep calendar reads, existing-effect reconciliation, suppression, reapproval confirmation and retention work under the Activity; preserve `recovery_feedback`.
- [x] Task: Migrate bounded recovery cards, explicit check/suppress/reapprove/open-event controls, refresh/retention actions and previous/more pagination to lifecycle-disposed ComposeViews; preserve Activity-owned effects and compatibility view IDs (see [recovery cards review](review-20261024-recovery-cards.md)).
- [x] Task: Migrate historical import history title, bounded session rows and empty state into a lifecycle-disposed ComposeView; keep query, cursors and session identity in the Activity, preserve `import_history`, and verify explicit opening on API36.
- [ ] Task: Migrate historical import and recovery screen by screen; preserve IDs, destination binding, operation ownership and saved-state semantics.
- [ ] Task: Qualify real permission/revocation, destination disappearance, cancellation/recreation/process restore, font/RTL, TalkBack/Switch Access and no synchronization regression; remove replaced Views only after parity.
- [ ] Task: Phase Verification & Checkpoint (refer to ../../workflow.md).

## Phase 4: Review, acceptance and handoff

- [ ] Task: Run applicable shared/rendered/native/cloud/package/operational acceptance with exact source/artifact evidence; verify cleanup and preserving upgrade/recovery.
- [x] Task: Review and fix the automatic-settings status slice and scoped-deferral gate; rerun rendered/host API36 checks and Conductor fault tests (see [review](review-20261023.md)). (19bb361)
- [x] Task: Review the manual logging presentation slice; remove the initially tried Material 3 dependency to comply with D14-1, retain Activity-owned delivery, and document the unavailable rendered-test runtime in [review](review-20261024-manual-logging.md).
- [x] Task: Review the historical import status slice; keep the existing dependency set, preserve the status host IDs and transaction callbacks, and document the pending emulator result in [review](review-20261024-import-status.md).
- [x] Task: Review the recovery feedback slice; retain all recovery effects and resource IDs, and document the pending runtime result in [review](review-20261024-recovery-status.md).
- [x] Task: Review and fix the recovery card/action/pagination slice; run the Compose component and fixture-backed API36 recovery journeys, and record the separate historical-import accessibility test failure (see [review](review-20261024-recovery-cards.md)).
- [x] Task: Fix the Compose test-tag accessibility selector mismatch, review the change, and rerun settings and historical-import panel checks (see [review](review-20261024-accessibility-selector.md)).
- [x] Task: Migrate historical import date and call-type filters to a lifecycle-disposed ComposeView; preserve filter IDs and Activity-saved filter values, and qualify explicit edits/toggles plus Activity recreation on API36 (see [review](review-20261024-import-filters.md)).
- [x] Task: Qualify filter date labels, checkbox role/state, large-font visibility, RTL rendering and 48 dp touch target semantics in the Compose test harness (see [review](review-20261024-import-accessibility.md)).
- [x] Task: Migrate the recovery empty-state message into a lifecycle-disposed ComposeView, preserving `recovery_empty` and Activity-owned query state; qualify its visibility transition and no-side-effect behavior (see [review](review-20261024-recovery-empty.md)).
- [x] Task: Review and fix the historical import history Compose slice; qualify empty and populated panels plus explicit rendered session opening on the API36 emulator (see [review](review-20261024-import-history.md)).
- [ ] Task: Perform Conductor review, apply fixes and rerun affected checks.
- [ ] Task: Update requirements, decisions, remaining limitations and next-track inputs; retire superseded code/prototypes only after parity.
- [ ] Task: Final checkpoint and archive only after original acceptance, or explicit justified deferral with documented consequences and reconsideration trigger.
