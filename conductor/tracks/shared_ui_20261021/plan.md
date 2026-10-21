# Plan: Compose Multiplatform Android and macOS UI

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
- [ ] Task: Map Android/macOS screen differences, controller ownership, keyboard/focus/accessibility and packaging requirements; refresh Compose Multiplatform/native graph.
- [ ] Task: Implement shared immutable presentation state and explicit actions, then share rendering without duplicate production owners or automatic effects.
- [ ] Task: Retain native prompts/pickers/adapters and platform navigation where needed; qualification determines each shared boundary.
- [ ] Task: Run rendered Android/macOS workflows, native keyboard/focus/large-text/RTL/manual accessibility, package/update/restoration checks before removing old rendering.
- [ ] Task: Phase Verification & Checkpoint (refer to ../../workflow.md).

## Phase 4: Review, acceptance and handoff

- [ ] Task: Run applicable shared/rendered/native/cloud/package/operational acceptance with exact source/artifact evidence; verify cleanup and preserving upgrade/recovery.
- [ ] Task: Perform Conductor review, apply fixes and rerun affected checks.
- [ ] Task: Update requirements, decisions, remaining limitations and next-track inputs; retire superseded code/prototypes only after parity.
- [ ] Task: Final checkpoint and archive only after original acceptance, or explicit justified deferral with documented consequences and reconsideration trigger.
