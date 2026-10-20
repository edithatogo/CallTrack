# Plan: iOS and desktop acquisition feasibility

Research and compile-only surface experiment complete; production implementation deferred under PLAT-D01. Capability documentation is not runtime acceptance.

## Phase 1: Discovery and mapping

- [x] Task: Research current supported iOS/desktop call, calendar, import and background APIs
- [x] Task: Map manual/import/companion workflows separately from automatic capture and platform entitlement limits
- [x] Task: Prototype only the uncertain capability with synthetic data and document feasible requirements
- [x] Task: Write findings and refine track requirements/acceptance with source provenance
- [x] Task: Phase Verification & Checkpoint (refer to ../../workflow.md)

## Phase 2: Options and dependency decision

- [x] Task: Compare retained/native/custom implementation with dependencies and deferral
- [x] Task: Run a bounded prototype only where it resolves uncertainty; identify temporary dependencies and expiry criteria
- [x] Task: Record decision, update spec and implementation tasks; propagate any changed prerequisite assumptions
- [x] Task: Phase Verification & Checkpoint (refer to ../../workflow.md)

## Phase 3: Conditional implementation and verification

- N/A: production behavior/failing integration contracts deferred; compile-only SDK check performs no runtime action.
- [x] Task: Add compile-only EventKit method-reference probe; no app integration/migration/retirement.
- [x] Task: Verify tests plus applicable device/provider readback and rollback; otherwise document justified deferral
- [x] Task: Phase Verification & Checkpoint (refer to ../../workflow.md)

## Phase 4: Handoff

- [x] Task: Update consolidated requirements, stack and dependency decisions with actual outcomes
- [x] Task: Document residual gaps, next-track inputs and checkpoint evidence
- [x] Task: Phase Verification & Checkpoint (refer to ../../workflow.md)

## History and checkpoint

- `cdd61e8`: activate research track.
- SDK typecheck passes with warnings-as-errors; no runtime API invocation.
- [~] Final source/link/host verification, formal review and archive.
