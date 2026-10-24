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

- [x] Task: Establish a bounded synthetic schema-v1 Android producer/macOS preview golden and checksum/count regressions; production format acceptance remains open.
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

## Evidence refresh — 2026-10-24

- Added Track 17 PR #60's Android KMP target and synthetic host compile/package
  evidence to the cross-platform matrix. The host remains a prototype; no
  production integration or Android rendered acceptance is claimed.
- Kept phase 3 blocked: the Android emulator is offline; macOS native packaging,
  an accepted Android export contract, preserving app upgrades and native/
  remote calendar readback remain unavailable.
- Kept Track 19 and the iOS handoff open. This refresh advances evidence only;
  it does not substitute for a qualifying second-platform workflow.

## Prototype contract — 2026-10-24

- Added a redacted Android `CallRecord` JSON encoder and shared synthetic golden
  consumed by the Swift preview parser. Validation covers deterministic opaque
  row IDs, checksum/count, duplicate identity, invalid direction/identity,
  overflow and size limits.
- Direct Swift 6.4 source harness compiled and passed, including the shared
  golden fixture. This does not establish SwiftPM/XCTest execution or native app
  acceptance.
- Android JVM unit tests were started under the selected repo JDK; the observed
  result is recorded in the review after the process exits.
- Kept UI/file export, public schema adoption, restore semantics, native
  packaging, cross-platform upgrades, provider/cloud readback and operational
  acceptance open. Track 19 remains in progress and Track 20/iOS remains locked.
