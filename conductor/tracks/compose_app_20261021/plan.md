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

- [ ] Task: Establish golden/contract baseline and meaningful failing regressions before behaviour changes.
- [x] Task: Refresh controller/lifecycle/permission mapping and Compose/toolchain/transitive compatibility; choose stable pinned versions from actual evidence.
- [x] Task: Implement the read-only automatic status presentation with ComposeView coexistence and explicit disposal; no provider effects in composition. Other settings controls remain Views. (19bb361)
- [ ] Task: Migrate manual logging, historical import and recovery screen by screen; preserve IDs, destination binding, operation ownership and saved-state semantics.
- [ ] Task: Qualify real permission/revocation, destination disappearance, cancellation/recreation/process restore, font/RTL, TalkBack/Switch Access and no synchronization regression; remove replaced Views only after parity.
- [ ] Task: Phase Verification & Checkpoint (refer to ../../workflow.md).

## Phase 4: Review, acceptance and handoff

- [ ] Task: Run applicable shared/rendered/native/cloud/package/operational acceptance with exact source/artifact evidence; verify cleanup and preserving upgrade/recovery.
- [x] Task: Review and fix the automatic-settings status slice and scoped-deferral gate; rerun rendered/host API36 checks and Conductor fault tests (see [review](review-20261023.md)). (19bb361)
- [ ] Task: Perform Conductor review, apply fixes and rerun affected checks.
- [ ] Task: Update requirements, decisions, remaining limitations and next-track inputs; retire superseded code/prototypes only after parity.
- [ ] Task: Final checkpoint and archive only after original acceptance, or explicit justified deferral with documented consequences and reconsideration trigger.
