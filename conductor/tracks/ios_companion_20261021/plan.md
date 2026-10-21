# Plan: iOS companion implementation — last

## Phase 1: Refresh mapping and requirements

- [ ] Task: Read prerequisite evidence and archived research; map behaviour, native APIs, dependencies and code ownership.
- [ ] Task: Record findings, refined requirements and explicit acceptance/unsupported cases.
- [ ] Task: Phase Verification & Checkpoint (refer to ../../workflow.md).

## Phase 2: Dependency and prototype decision

- [ ] Task: Compare retained/native/owned/library replacement and deferral, including prototype-only versus long-term responsibilities.
- [ ] Task: Run only uncertainty-resolving bounded experiments; record versions/licences/transitives/toolchain/cleanup and expiry criteria.
- [ ] Task: Record adoption/deferral decision and refine implementation/rollback tasks before production change.
- [ ] Task: Phase Verification & Checkpoint (refer to ../../workflow.md).

## Phase 3: Conditional implementation

- [ ] Task: Establish golden/contract baseline and meaningful failing regressions before behaviour changes.
- [ ] Task: Verify all earlier stage dispositions before iOS implementation; then refresh supported iOS APIs, Xcode/Kotlin/Compose compatibility, signing/device and distribution requirements.
- [ ] Task: Implement native Apple file/calendar/permission/lifecycle adapters around shared core and suitable shared screens; deliberate preview/approval before effects.
- [ ] Task: Qualify simulator and authorised device separately, including signing/usage strings, denial/revocation, interruption/process restore, exact EventKit readback and native accessibility.
- [ ] Task: Test packaged update/recovery and platform capability disclosure; investigate any future automatic acquisition/default-role route as separate explicitly scoped research with current eligibility evidence.
- [ ] Task: Phase Verification & Checkpoint (refer to ../../workflow.md).

## Phase 4: Review, acceptance and handoff

- [ ] Task: Run applicable shared/rendered/native/cloud/package/operational acceptance with exact source/artifact evidence; verify cleanup and preserving upgrade/recovery.
- [ ] Task: Perform Conductor review, apply fixes and rerun affected checks.
- [ ] Task: Update requirements, decisions, remaining limitations and next-track inputs; retire superseded code/prototypes only after parity.
- [ ] Task: Final checkpoint and archive only after original acceptance, or explicit justified deferral with documented consequences and reconsideration trigger.
