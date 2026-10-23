# Plan: iOS companion implementation — last

## Phase 1: Refresh mapping and requirements

- [x] Task: Read prerequisite evidence and archived research; map behaviour, native APIs, dependencies and code ownership (readiness-only scope).
- [x] Task: Record findings, refined requirements and explicit acceptance/unsupported cases (product implementation remains gated).
- [x] Task: Phase Verification & Checkpoint (readiness evidence and Conductor tests verified; iOS remains blocked).

## Phase 2: Dependency and prototype decision

- [x] Task: Compare retained/native/owned/library replacement and deferral, including prototype-only versus long-term responsibilities (see readiness.md).
- [x] Task: Run only uncertainty-resolving bounded experiments; no prototype is justified before the shared contract and stage gates exist.
- [x] Task: Record adoption/deferral decision and refine implementation/rollback tasks before production change (no runtime dependency or implementation adopted).
- [x] Task: Phase Verification & Checkpoint (no runtime adoption; sequence remains unresolved).

## Phase 3: Conditional implementation

- [ ] Task: Establish golden/contract baseline and meaningful failing regressions before behaviour changes.
- [x] Task: Verify all earlier stage dispositions before iOS implementation; refresh current iOS API and Xcode/Kotlin/Compose compatibility. Stages 1–8 remain unresolved; signing/device/distribution work is still pending.
- [ ] Task: Implement native Apple file/calendar/permission/lifecycle adapters around shared core and suitable shared screens; deliberate preview/approval before effects.
- [ ] Task: Qualify simulator and authorised device separately, including signing/usage strings, denial/revocation, interruption/process restore, exact EventKit readback and native accessibility.
- [ ] Task: Test packaged update/recovery and platform capability disclosure; investigate any future automatic acquisition/default-role route as separate explicitly scoped research with current eligibility evidence.
- [ ] Task: Phase Verification & Checkpoint (refer to ../../workflow.md).

## Phase 4: Review, acceptance and handoff

- [ ] Task: Run applicable shared/rendered/native/cloud/package/operational acceptance with exact source/artifact evidence; verify cleanup and preserving upgrade/recovery.
- [x] Task: Perform Conductor review, apply fixes (none required) and rerun affected checks; readiness-only slice reviewed, iOS implementation remains gated.
- [ ] Task: Update requirements, decisions, remaining limitations and next-track inputs; retire superseded code/prototypes only after parity.
- [ ] Task: Final checkpoint and archive only after original acceptance, or explicit justified deferral with documented consequences and reconsideration trigger.

## Review record

See [review-20261024.md](review-20261024.md). The reviewed slice does not
complete Track 20 or satisfy the stage-1–8 prerequisite.

## Scoped checkpoint — 2026-10-24

- Only stage-9 requirement and dependency readiness research is recorded.
- No iOS application source, project, package, dependency or signing setup was
  created. Full implementation and acceptance remain blocked by stages 1–8.
- This scoped checkpoint does not count as a Track disposition and cannot
  unlock an earlier or later stage.
