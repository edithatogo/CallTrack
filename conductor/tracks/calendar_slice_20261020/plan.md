# Plan: Call-to-calendar vertical slice

Discovery has selected a manual native-provider slice. Completion requires actual device readback, cleanup and review; research alone is insufficient.

## Phase 1: Discovery and mapping

- [x] Task: Map call source and writable-calendar contracts, including Outlook-backed device calendars
- [x] Task: Compare direct provider APIs with maintained wrappers; prototype one synthetic/controlled event
- [x] Task: Define source identity, ledger boundary and persistence/readback acceptance
- [x] Task: Write findings and refine track requirements/acceptance with source provenance
- [x] Task: Phase Verification & Checkpoint (refer to ../../workflow.md)

## Phase 2: Options and dependency decision

- [x] Task: Compare retained/native/custom implementation with dependencies and deferral
- [x] Task: Run a bounded prototype only where it resolves uncertainty; identify temporary dependencies and expiry criteria
- [x] Task: Record decision, update spec and implementation tasks; propagate any changed prerequisite assumptions
- [x] Task: Phase Verification & Checkpoint (refer to ../../workflow.md)

## Phase 3: Conditional implementation and verification

- [x] Task: If adopted, write meaningful failing contract/regression tests before changing behaviour
- [x] Task: Implement the selected bounded change and any data/config migration; remove superseded code only after parity
- [x] Task: Verify tests plus applicable device/provider readback and rollback; otherwise document justified deferral
- [x] Task: Phase Verification & Checkpoint (refer to ../../workflow.md)

## Phase 4: Handoff

- [x] Task: Update consolidated requirements, stack and dependency decisions with actual outcomes
- [x] Task: Document residual gaps, next-track inputs and checkpoint evidence
- [x] Task: Phase Verification & Checkpoint (refer to ../../workflow.md)

## Execution evidence (in progress)

- Direct javac red check: missing CallRecord/CallEvent/SliceEngine symbols in `.local/slice-domain-red.log`, before implementation. Initial 10 pure Java JUnit contracts passed afterward. Expanded suite and Android/native checks pending.
- The first Gradle invocation completed after domain implementation; despite its historical log filename it is a green result, not the red evidence.
- Research sources consulted 2026-10-20; native wrapper alternatives and deferral triggers documented in findings.

## Phase 5: Review fixes

- [x] Task: Fix uncertain install cleanup, stale refresh selection and explicit transfer policy; verify final APK/native receipts

Implementation checkpoint: b2e105d. Initial Android build passed with 14 JVM tests and zero lint errors (31 warnings). Final input-bound receipt, native provider readback and cleanup remain pending.

First native evidence: `.local/slice-evidence/pre-review-device-receipt.json` passed on Android17/API37. Synthetic local event readback, reopened SQLite retry, crash-gap recovery, edited-event preservation, read-only destination, denied/revoked permissions and fixture/package removal passed. Final review fixes require another source-bound build/device verification.

Review fix checkpoint: 66ca1e7 (UI refresh, backup policy, query limit and harness cleanup). UI-review APK passed all native invocations and fixture/package cleanup. Additional uncertain-readback regression/fix is being verified in the final build.

## Final checkpoint — 2026-10-20

All phases passed. Implementation b2e105d; review fixes 66ca1e7 and a7e54a5. Final source-bound build and Android17/API37 native acceptance passed, including exact fixture/package absence. 16 JVM tests, 16 host tests, zero lint errors, AOSP formatting and Conductor/private-source validation passed. See verification.md, review.md and evidence.json for scope, receipts and downstream limitations.
