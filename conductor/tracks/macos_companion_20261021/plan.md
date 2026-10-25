# Plan: macOS import and calendar companion

## Phase 1: Refresh mapping and requirements

- [x] Task: Read prerequisite evidence and archived research; map behaviour, native APIs, dependencies and code ownership.
- [x] Task: Record findings, refined requirements and explicit acceptance/unsupported cases.
- [ ] Task: Phase Verification & Checkpoint (local checks below; native gate pending).

## Phase 2: Dependency and prototype decision

- [x] Task: Compare retained/native/owned/library replacement and deferral, including prototype-only versus long-term responsibilities.
- [x] Task: Run only uncertainty-resolving bounded experiments; record versions/licences/transitives/toolchain/cleanup and expiry criteria.
- [x] Task: Record adoption/deferral decision and refine implementation/rollback tasks before production change.
- [ ] Task: Phase Verification & Checkpoint (SwiftPM build-system initialization is blocked; use only source typecheck/harness locally).

## Phase 3: Conditional implementation

- [x] Task: Establish parser contract cases and meaningful regression coverage before behaviour changes.
- [x] Task: Map a bounded prototype import format/provenance, preview/approval, capability and privacy requirements before choosing parser/storage dependencies.
- [x] Task: Implement and review a bounded, preview-only Foundation parser; test byte/count, schema, identity, timestamp, and duration boundaries with synthetic inputs (see [parser boundary review](review-20261024-parser-boundaries.md)).
- [x] Task: Add an isolated AppKit JSON file picker and aggregate-only preview using a bounded security-scoped reader; keep delivery disabled and add synthetic regular-file, directory, and oversized-file checks (see [file-preview prototype evidence](file-preview-prototype-20261025.md)).
- [x] Task: Specify the one-writer EventKit delivery contract and implement an isolated durable-intent/SQLite ledger prototype with fake-provider recovery tests (see [delivery contract](eventkit-delivery-contract-20261025.md)).
- [x] Task: Add an isolated EventKit adapter and coordinator prototype that requires explicit confirmation, exact selected-calendar readback and fail-closed uncertain-write recovery; do not wire it to UI or request native access.
- [ ] Task: Implement native file selection and explicit approval UI with no write during preview; imported foreign receipts remain inactive and never confer replay authority.
- [ ] Task: Integrate and qualify the adapter in the packaged host: usage-description metadata, native permission, list/select, controlled calendar insert/exact-readback and recovery. Denial/write-only cannot be called reconciled.
- [ ] Task: Qualify interruption/restart/uncertain insert, edited/deleted events, missing/recreated calendar, scoped cleanup, native accessibility and packaged upgrade/recovery using synthetic data.
- [ ] Task: Phase Verification & Checkpoint (refer to ../../workflow.md).

## Phase 4: Review, acceptance and handoff

- [ ] Task: Run applicable shared/rendered/native/cloud/package/operational acceptance with exact source/artifact evidence; verify cleanup and preserving upgrade/recovery.
- [x] Task: Review the bounded local slice, apply fixes and rerun affected checks (including [parser boundary review](review-20261024-parser-boundaries.md)). Track-level review remains open until native acceptance.
- [x] Task: Update requirements, decisions, remaining limitations and next-track inputs; retire superseded code/prototypes only after parity. (No prototype retired; Track17 remains blocked.)
- [ ] Task: Final checkpoint and archive only after original acceptance, or explicit justified deferral with documented consequences and reconsideration trigger.

## Local checkpoint — 2026-10-24

- Implemented preview-only Foundation JSON parser with bounded size/count,
  identity uniqueness and timestamp/duration validation.
- `swift test --package-path prototypes/macos-companion` could not initialize
  SwiftPM's build system (`Unknown error parsing property list`); no package
  tests are claimed from that command.
- Direct source type-check and the checked-in synthetic parser harness pass;
  the available SDK lacks XCTest, so package tests still require full Xcode.
- Full Xcode, native file picker, selected calendar, EventKit permissions and
  exact provider readback remain required before further acceptance.

## Delivery prototype checkpoint — 2026-10-25

- Added a durable SQLite intent ledger, explicit single-writer coordinator and
  EventKit adapter behind an unconnected module. No real calendar or permission
  prompt was accessed; the preview UI remains delivery-disabled.
- The synthetic delivery harness passes on the local Swift 6.4 toolchain.
- Full XCTest and native EventKit acceptance remain pending full Xcode and a
  deliberate, bounded calendar test configuration. See D16-03 and the delivery
  contract for the exact limits.
