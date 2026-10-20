# Plan: Call-to-calendar vertical slice

All tasks pending. Discovery is real track work. Implementation is conditional; do not mark it complete when only research/prototypes exist.

## Phase 1: Discovery and mapping

- [~] Task: Map call source and writable-calendar contracts, including Outlook-backed device calendars
- [ ] Task: Compare direct provider APIs with maintained wrappers; prototype one synthetic/controlled event
- [ ] Task: Define source identity, ledger boundary and persistence/readback acceptance
- [ ] Task: Write findings and refine track requirements/acceptance with source provenance
- [ ] Task: Phase Verification & Checkpoint (refer to ../../workflow.md)

## Phase 2: Options and dependency decision

- [ ] Task: Compare retained/native/custom implementation with dependencies and deferral
- [ ] Task: Run a bounded prototype only where it resolves uncertainty; identify temporary dependencies and expiry criteria
- [ ] Task: Record decision, update spec and implementation tasks; propagate any changed prerequisite assumptions
- [ ] Task: Phase Verification & Checkpoint (refer to ../../workflow.md)

## Phase 3: Conditional implementation and verification

- [ ] Task: If adopted, write meaningful failing contract/regression tests before changing behaviour
- [ ] Task: Implement the selected bounded change and any data/config migration; remove superseded code only after parity
- [ ] Task: Verify tests plus applicable device/provider readback and rollback; otherwise document justified deferral
- [ ] Task: Phase Verification & Checkpoint (refer to ../../workflow.md)

## Phase 4: Handoff

- [ ] Task: Update consolidated requirements, stack and dependency decisions with actual outcomes
- [ ] Task: Document residual gaps, next-track inputs and checkpoint evidence
- [ ] Task: Phase Verification & Checkpoint (refer to ../../workflow.md)
