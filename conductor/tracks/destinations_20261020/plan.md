# Plan: Calendar and service integration discovery

Research completed with justified deferral under INT-D01; provider/runtime acceptance remains open. No direct connector or prototype is selected.

## Phase 1: Discovery and mapping

- [x] Task: Map Outlook/Google/CalDAV needs and native-provider coverage before direct connectors
- [x] Task: Research Graph/Google/CalDAV APIs, auth libraries, maintained clients and temporary connector prototypes
- [x] Task: Specify conflict, destination identity, rate-limit, deletion and loop-prevention requirements
- [x] Task: Write findings and refine track requirements/acceptance with source provenance
- [x] Task: Phase Verification & Checkpoint (refer to ../../workflow.md)

## Phase 2: Options and dependency decision

- [x] Task: Compare retained/native/custom implementation with dependencies and deferral
- [x] Task: Run a bounded prototype only where it resolves uncertainty; identify temporary dependencies and expiry criteria
- [x] Task: Record decision, update spec and implementation tasks; propagate any changed prerequisite assumptions
- [x] Task: Phase Verification & Checkpoint (refer to ../../workflow.md)

## Phase 3: Conditional implementation and verification

- N/A: failing implementation contracts; no executable change is selected. Future provider cases are specified.
- N/A: direct connector, data migration or code retirement under INT-D01 deferral.
- [x] Task: Verify tests plus applicable device/provider readback and rollback; otherwise document justified deferral
- [x] Task: Phase Verification & Checkpoint (refer to ../../workflow.md)

## Phase 4: Handoff

- [x] Task: Update consolidated requirements, stack and dependency decisions with actual outcomes
- [x] Task: Document residual gaps, next-track inputs and checkpoint evidence
- [x] Task: Phase Verification & Checkpoint (refer to ../../workflow.md)

## History

- `90445b7`: activate research track.
- [~] Formal review, host/source/link verification and final archive still to run.
