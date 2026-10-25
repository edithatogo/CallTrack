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
- [x] Task: Migrate historical import history title, bounded session rows and empty state into a lifecycle-disposed ComposeView; keep query, cursors and session identity in the Activity, preserve `import_history`, and verify explicit opening on API36 (66a73b1).
- [x] Task: Migrate the historical-import destination picker to a lifecycle-disposed ComposeView with Activity-owned provider choices, opaque per-load tokens and saved calendar-ID semantics; keep `import_calendar_picker` stable and do not add Material dependencies.
- [x] Task: Migrate the historical-import permission-access button to a lifecycle-disposed ComposeView; keep the Android permission prompt and request callback in the Activity.
- [x] Task: Migrate the zone-formatted historical-import explanatory text to a read-only Compose panel with a stable host; keep time-zone, session and provider state in the Activity.
- [x] Task: Migrate the launcher manual screen's calendar-access and destination controls to Compose; retain provider/permission operations in the Activity, preserve the `slice_calendar_picker` accessibility identity and stored calendar ID, and require an explicit option tap (see [review](review-20261025-manual-destination.md)).
- [x] Task: Run hosted rendered acceptance for the manual provider selection/reopen flow on API26/API33/API36, including API-specific accessibility and permission coverage. Run 40165971174 attempt 2 passed settings/imports/recovery/accessibility on all three APIs; every suite verified fixture cleanup, package removal and screenshot hashes, and each settings suite exercised explicit manual calendar selection. Attempt 1's API26 focus failure remains recorded; retry passed on exact app/test source.
- [x] Task: Move the manual launcher's remaining intro, panel layout and automatic-settings navigation into a lifecycle-disposed Compose screen; retain Activity-owned permissions, provider reads, destination IDs and delivery (D14-13; `97a7739`, see [review](review-20261025-manual-screen.md)).
- [ ] Task: Run the new pure Compose manual-screen callback test and the rendered manual-provider journey on API26/API33/API36; verify Compose identifiers, scroll/insets and fixture cleanup.
- [ ] Task: Migrate historical import and recovery screen by screen; preserve IDs, destination binding, operation ownership and saved-state semantics.
- [ ] Task: Qualify real permission/revocation, destination disappearance, cancellation/recreation/process restore, font/RTL, TalkBack/Switch Access and no synchronization regression; remove replaced Views only after parity.
- [ ] Task: Phase Verification & Checkpoint (refer to ../../workflow.md).

## Phase: Hosted API36 UI acceptance fallback (2026-10-24)
- [x] Task: Reconcile the reattached SSD, verify the current source-bound build receipt and diagnose a fresh isolated ARM64 emulator boot failure without touching the retained process.
- [x] Task: Add a scoped Linux/KVM workflow for the existing API36 synthetic settings, import, recovery and accessibility journeys; require cleanup and package removal evidence.
- [x] Task: Run hosted API36 UI acceptance, inspect sanitized suite/method/screenshot-hash evidence, fix the host/tag and recovery-anchor findings, and repeat affected checks (PR #77; run 40095495432; see `review-20261024-hosted-ui.md`).
- [x] Task: Review the hosted API36 x86_64 acceptance slice and update only the gates supported by its evidence; physical, local ARM64, remote-calendar and operational acceptance remain separate (see `review-20261024-hosted-ui.md`).
- [x] Task: Extend the hosted source-bound UI journeys to API26/API33 and rerun API36 on current source; inspect every suite result, accessibility receipt and cleanup proof. Hosted run 40149896511 attempt 3 passed settings, imports, recovery and accessibility on API26/API33/API36, with cleanup, package removal and screenshot hashes verified for every suite. API33 attempts 1 and 2 failed in the rendered permission-dialog path before attempt 3 passed; all attempts remain in the evidence history (see [evidence](implementation-evidence.md) and [matrix result review](review-20261025-hosted-matrix-pass.md)).

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
- [x] Task: Review and fix the historical import history Compose slice; qualify empty and populated panels plus explicit rendered session opening on the API36 emulator (66a73b1; see [review](review-20261024-import-history.md)).
- [x] Task: Perform Conductor review, apply the bounded PermissionController diagnostics, and rerun affected host checks. Record hosted API33/API36 passes and the unresolved API26 import permission prompt; retain the rendered matrix as an open acceptance gate (see [review](review-20261025-hosted-ui-matrix.md), commits `ac42f2a` and `b5c2a90`).
- [x] Task: Review the historical-import destination selector slice; verify host/permission-denied rendering, explicit opaque-token selection, and no import side effects; record provider-backed selection and restored-calendar acceptance as pending in [review](review-20261024-import-destination.md).
- [x] Task: Review the historical-import access-action slice; verify explicit callback dispatch, stable host and no permission request during composition; keep runtime grant/deny/revocation checks pending in [review](review-20261024-import-access.md).
- [x] Task: Review the historical-import introductory-text slice; verify exact zone-formatted copy, stable host and no import side effects in [review](review-20261024-import-intro.md).
- [x] Task: Move the recovery introduction and retention-policy copy into lifecycle-disposed Compose hosts, preserving Activity-owned permissions, retention execution and query state; verify component rendering and exact API36 resource text in [review](review-20261024-recovery-copy.md).
- [x] Task: Review the recovery copy slice; distinguish the passing focused panel/Activity evidence from the unqualified fixture-backed full-class attempt in [review](review-20261024-recovery-copy.md).
- [x] Task: Move the automatic-settings introduction and permission-access action into lifecycle-disposed Compose hosts; keep permission prompts and activation policy in the Activity and verify explicit callback plus no-activation behavior in [review](review-20261024-settings-access.md).
- [x] Task: Review the automatic-settings introduction/access slice, fix missing accessibility test-tag exposure, and rerun the focused component and API36 Activity checks (see [review](review-20261024-settings-access.md)).
- [x] Task: Replace the automatic logging call-type CheckBoxes with an Activity-owned Compose panel; preserve accessible resource IDs, Activity-owned activation decisions and selected call types through recreation (see [review](review-20261024-auto-call-types.md)).
- [x] Task: Review the automatic call-type panel, verify state callbacks and RTL/large-text semantics, and run the focused Android checks (see [review](review-20261024-auto-call-types.md)).
- [x] Task: Replace the automatic destination Spinner with a lifecycle-disposed Compose selector; keep provider discovery and destination IDs in the Activity and bind selections through per-load opaque tokens (see [review](review-20261024-auto-destination.md)).
- [x] Task: Review explicit destination choice, stable accessibility selector, and unchanged permission/provider ownership; keep rendered/provider runtime acceptance pending if no emulator is available (see [review](review-20261024-auto-destination.md)).
- [x] Task: Replace automatic-settings enable, pause and refresh Views with a lifecycle-disposed Compose action panel; keep confirmation, scheduler and refresh effects in the Activity and preserve stable accessibility tags (see [review](review-20261024-auto-actions.md)).
- [x] Task: Review automatic-settings actions for state parity, explicit callback dispatch and no composition effects; record hosted build and emulator-runtime boundaries in [review](review-20261024-auto-actions.md).
- [x] Task: Replace the recovery calendar-access View button with a lifecycle-disposed Compose action panel; keep the permission request in the Activity and preserve explicit user activation (see [review](review-20261024-recovery-access.md)).
- [x] Task: Review recovery access action rendering and callback ownership; verify composition does not request permission and record hosted/emulator acceptance boundaries (see [review](review-20261024-recovery-access.md)).
- [x] Task: Replace the automatic-settings import and recovery navigation Views with a lifecycle-disposed Compose action panel; preserve navigation IDs and keep `startActivity` in Activity callbacks (see [review](review-20261024-settings-navigation.md)).
- [x] Task: Review explicit navigation callbacks and rendered IDs; record hosted build and emulator runtime boundaries in [review](review-20261024-settings-navigation.md).
- [x] Task: Refresh current-main source-bound Android build, JVM, lint, Conductor and host safety-test evidence; leave new rendered acceptance pending while the authorized emulator is offline (see [implementation evidence](implementation-evidence.md)).
- [x] Task: Review the current-main evidence refresh, verify its source/artifact and test references, and preserve explicit emulator acceptance limits (see [review](review-20261024-current-main-refresh.md)).
- [x] Task: Review and refine sanitized hosted diagnostics for fatal logcat and resumed activity ownership; rerun the hosted matrix and keep API26/API33 acceptance open (see [review](review-20261025-diagnostics.md); commit `0a140e7`).
- [x] Task: Review the latest merged hosted API26/API33/API36 UI matrix result, reconcile the previously open hosted focus and permission-control findings, and retain separate device/operational gates (see [matrix result review](review-20261025-hosted-matrix-pass.md)).
- [x] Task: Review manual destination token ownership, explicit selection persistence, permission/provider boundary and test harness; compile Android test sources, run app unit tests/lint, and record hosted API26/API33/API36 acceptance as passed in run 40165971174 attempt 2 (see [review](review-20261025-manual-destination.md)).
- [ ] Task: Update requirements, decisions, remaining limitations and next-track inputs; retire superseded code/prototypes only after parity.
- [ ] Task: Final checkpoint and archive only after original acceptance, or explicit justified deferral with documented consequences and reconsideration trigger.

## Phase: Review Fixes — manual destination runtime acceptance
- [x] Task: Diagnose the API26 keyboard-focus probe when its screen anchor is disabled; select an enabled clickable app control for the keyboard journey while preserving the visible-focus assertion. Android-test compilation passed. (fb034f3)
- [x] Task: Rerun the hosted API26/API33/API36 matrix on this exact source, preserve the first API26 failure, and verify the successful retry with fixture cleanup and artifact receipts (run 40165971174 attempt 2).

## Phase: Review Fixes — manual launcher Compose screen

- [x] Task: Review the complete manual launcher diff, correct the instrumentation assertion to inspect the Activity held in its array, and rerun Android test compilation, unit tests, lint, APK assembly, Conductor tests and format checks (commit `97a7739`; see [review](review-20261025-manual-screen.md)).
- [x] Task: Diagnose the first hosted failure and expose each stable manual-screen tag on its own Compose semantics node; split the rendered assertions to isolate any missing identifier (commit `ffab440`).
- [ ] Task: Rerun the pure Compose callback test and manual provider journey on API26/API33/API36, then record exact-source receipts and cleanup results.
