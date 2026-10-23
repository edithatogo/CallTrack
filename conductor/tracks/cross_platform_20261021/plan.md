# Plan: Consolidated Android and macOS qualification

## Phase 1: Refresh mapping and requirements

- [x] Task: Read prerequisite evidence and archived research; map behaviour, native APIs, dependencies and code ownership (bounded to available evidence).
- [x] Task: Record findings, refined requirements and explicit acceptance/unsupported cases (full qualification remains pending).
- [ ] Task: Phase Verification & Checkpoint (refer to ../../workflow.md).

## Phase 2: Dependency and prototype decision

- [x] Task: Compare retained/native/owned/library replacement and deferral, including prototype-only versus long-term responsibilities.
- [x] Task: Run only uncertainty-resolving bounded experiments; no experiment was justified without an Android producer or accepted shared format.
- [x] Task: Record adoption/deferral decision and refine implementation/rollback tasks before production change; no production change is authorized by this scope.
- [ ] Task: Phase Verification & Checkpoint (refer to ../../workflow.md).

## Phase 3: Conditional implementation

- [ ] Task: Establish golden/contract baseline and meaningful failing regressions before behaviour changes.
- [ ] Task: Freeze exact source/toolchain/format/capability matrix and acceptance scope; distinguish native persistence from actual remote synchronization.
- [ ] Task: Run compatible import/export/restore fixtures with inactive foreign receipts, redaction, deliberate delivery and one actual destination writer.
- [ ] Task: Execute preserving package upgrades, process/permission/account failures, edited/deleted events and real provider-cloud readback where claimed; retain unobserved routes as unsupported.
- [ ] Task: Review Android/macOS source/artifact/licence/signing/operational evidence, resolve Rust disposition, remove only superseded experiments/implementations and produce iOS handoff.
- [ ] Task: Phase Verification & Checkpoint (refer to ../../workflow.md).

## Phase 4: Review, acceptance and handoff

- [ ] Task: Run applicable shared/rendered/native/cloud/package/operational acceptance with exact source/artifact evidence; verify cleanup and preserving upgrade/recovery.
- [ ] Task: Perform Conductor review, apply fixes and rerun affected checks.
- [ ] Task: Update requirements, decisions, remaining limitations and next-track inputs; retire superseded code/prototypes only after parity.
- [ ] Task: Final checkpoint and archive only after original acceptance, or explicit justified deferral with documented consequences and reconsideration trigger.

## Scoped checkpoint — 2026-10-24

- Bounded evidence reconciliation and dependency disposition are recorded in
  `findings.md` and `decisions.md`.
- Track 14–17 native completion, shared format/export, packaged apps, upgrades,
  EventKit/provider readback and selected cloud routes remain prerequisites.
- This checkpoint does not complete or archive Track 19 and does not unlock
  Track 20/iOS. Continue the original phases once prerequisites are satisfied.
