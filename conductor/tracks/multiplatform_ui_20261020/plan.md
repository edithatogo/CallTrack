# Plan: Compose Multiplatform product and UI discovery

Research and the bounded desktop experiment are complete. Production adoption is deferred under UI-D01; M8 implementation acceptance remains open.

## Phase 1: Discovery and mapping

- [x] Task: Map iOS/desktop workflows and platform differences without assuming Android capture parity
- [x] Task: Compare Compose Multiplatform with native UI and prototype representative interactions
- [x] Task: Document target scope, accessibility, packaging, state and support acceptance
- [x] Task: Write findings and refine track requirements/acceptance with source provenance
- [x] Task: Phase Verification & Checkpoint (refer to ../../workflow.md)

## Phase 2: Options and dependency decision

- [x] Task: Compare retained/native/custom implementation with dependencies and deferral
- [x] Task: Run a bounded prototype only where it resolves uncertainty; identify temporary dependencies and expiry criteria
- [x] Task: Record decision, update spec and implementation tasks; propagate any changed prerequisite assumptions
- [x] Task: Phase Verification & Checkpoint (refer to ../../workflow.md)

## Phase 3: Conditional implementation and verification

- N/A: production adoption/failing integration contracts deferred. Prototype tests failed before screen implementation.
- [x] Task: Implement standalone synthetic desktop trial only. No production change, migration or retirement.
- [x] Task: Verify tests plus applicable device/provider readback and rollback; otherwise document justified deferral
- [x] Task: Phase Verification & Checkpoint (refer to ../../workflow.md)

## Phase 4: Handoff

- [x] Task: Update consolidated requirements, stack and dependency decisions with actual outcomes
- [x] Task: Document residual gaps, next-track inputs and checkpoint evidence
- [x] Task: Phase Verification & Checkpoint (refer to ../../workflow.md)

## Activation refinement

`9968b1b` activates this track. Run a standalone desktop JVM Compose1.10.3
experiment against synthetic data. Establish failing UI contracts first, then test
rendered destination gating/preview invalidation/capability disclosure. Decide on
production sharing after evidence; Track7/8 deferrals remain in force.

## Evidence checkpoint

- Desktop prototype:3 rendered interaction tests pass; source and graph receipt recorded.
- Production shared UI deferred; native/iOS/manual/packaging gates remain open.
- Final host, format and link checks plus formal review precede archive.
