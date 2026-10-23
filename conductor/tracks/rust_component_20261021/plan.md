# Plan: Rust component evaluation and conditional integration

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
- [ ] Task: Identify an actual consumer/bottleneck and baseline; refresh crate/toolchain/licence/transitive maintenance evidence and compare owned Kotlin, libraries and deferral.
- [ ] Task: Prototype bounded import parsing or a selected backup format only if it resolves the named uncertainty; compare UniFFI and direct bindings including Kotlin/Native access, not JVM-only bindings.
- [ ] Task: Test full input contracts, hostile bounds/fuzzing, panic/error/cancellation/lifetime handling and target/ABI packaging; measure end-to-end including FFI copies/build cost.
- [ ] Task: Promote only with documented material benefit and retirement of duplicate responsibility, preserving receipts/old readers/recovery; otherwise record deferral/removal and leave Kotlin selected.
- [ ] Task: Phase Verification & Checkpoint (refer to ../../workflow.md).

## Phase 4: Review, acceptance and handoff

- [ ] Task: Run applicable shared/rendered/native/cloud/package/operational acceptance with exact source/artifact evidence; verify cleanup and preserving upgrade/recovery.
- [ ] Task: Perform Conductor review, apply fixes and rerun affected checks.
- [ ] Task: Update requirements, decisions, remaining limitations and next-track inputs; retire superseded code/prototypes only after parity.
- [ ] Task: Final checkpoint and archive only after original acceptance, or explicit justified deferral with documented consequences and reconsideration trigger.
