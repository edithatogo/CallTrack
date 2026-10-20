# Plan: Fork feasibility and build qualification

Build qualification, exact-artifact native checks and review fixes verified; completed track is ready for archival. Application parity is outside this track.

## Phase 1: Discovery and mapping

- [x] Task: Reproduce upstream build and assess modern SDK/permission constraints
- [x] Task: Compare fork repair with fresh implementation; inventory library versus custom-code replacement opportunities
- [x] Task: Qualify JDK/Gradle/AGP/SDK, formatting and CI commands; record package/signing strategy
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

## Execution evidence and scope

- Phase1: pinned original build/lint and clean repeat, toolchain compatibility research, source/dependency inventory, findings and requirements.
- Phase2: D-B01–D-B05 compare fork/fresh/native/retention and document interim tooling/dependency roles.
- Phase3: preparation/build/device harness, formatter/CI definition and tests implemented; clean build and zero-row native checks executed. Missing SDK and dependency-conflict failures were preserved before repair. No application logging or data migration claimed.
- Phase4: downstream handoff in requirements/verification and project stack/workflow updated.

## Phase: Review Fixes

- [x] Task: Resolve skipped Fragment lint/transitive mismatch, checksum/artifact identity and cleanup reporting; validate final source/artifact and archive continuity bae7121

## Commit history

- 2c3964a: track start.
- eec3a32: qualification implementation, tests, prepared CI and research decisions.

Review/verification checkpoint: bae7121. All four phase outputs and final source/device receipts verified; archive links receive a final post-move check.
