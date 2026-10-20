# Plan: Rust and reusable component evaluation

Research and bounded host experiment complete; production Rust adoption is deferred under RUST-D01. Full contracts/target packaging/material product benefit remain open.

## Phase 1: Discovery and mapping

- [x] Task: Identify concrete cross-application consumers and current custom/native components potentially replaceable by libraries
- [x] Task: Research maintained Rust libraries, UniFFI target support, licences and packaging versus Kotlin/KMP alternatives
- [x] Task: Benchmark one bounded component when justified; document temporary versus production role, exit criteria and costs
- [x] Task: Write findings and refine track requirements/acceptance with source provenance
- [x] Task: Phase Verification & Checkpoint (refer to ../../workflow.md)

## Phase 2: Options and dependency decision

- [x] Task: Compare retained/native/custom implementation with dependencies and deferral
- [x] Task: Run a bounded prototype only where it resolves uncertainty; identify temporary dependencies and expiry criteria
- [x] Task: Record decision, update spec and implementation tasks; propagate any changed prerequisite assumptions
- [x] Task: Phase Verification & Checkpoint (refer to ../../workflow.md)

## Phase 3: Conditional implementation and verification

- [x] Task: Prototype golden/invalid-input tests fail before implementation; production integration contracts remain deferred.
- [x] Task: Implement isolated host signature subset and JVM-source baseline only; no app/data migration or retirement.
- [x] Task: Verify tests plus applicable device/provider readback and rollback; otherwise document justified deferral
- [x] Task: Phase Verification & Checkpoint (refer to ../../workflow.md)

## Phase 4: Handoff

- [x] Task: Update consolidated requirements, stack and dependency decisions with actual outcomes
- [x] Task: Document residual gaps, next-track inputs and checkpoint evidence
- [x] Task: Phase Verification & Checkpoint (refer to ../../workflow.md)

## History and final checkpoint

- `a0da851`: activate research track.
- Rust3 tests and JVM4 golden assertions pass; sequential raw benchmark receipt recorded.
- [x] Host/link/format validation and formal review/fixes; archive follows.

- `512edaf`: implement bounded Rust/JVM experiment and record research/decision.

## Phase: Review Fixes

- [~] Expand benchmark provenance with exact tool/artifact hashes and validate.
