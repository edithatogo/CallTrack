# Plan: Call-to-calendar vertical slice

Discovery has selected a manual native-provider slice. Completion requires actual device readback, cleanup and review; research alone is insufficient.

## Phase 1: Discovery and mapping

- [x] Task: Map call source and writable-calendar contracts, including Outlook-backed device calendars
- [ ] Task: Compare direct provider APIs with maintained wrappers; prototype one synthetic/controlled event
- [x] Task: Define source identity, ledger boundary and persistence/readback acceptance
- [x] Task: Write findings and refine track requirements/acceptance with source provenance
- [ ] Task: Phase Verification & Checkpoint (refer to ../../workflow.md)

## Phase 2: Options and dependency decision

- [x] Task: Compare retained/native/custom implementation with dependencies and deferral
- [ ] Task: Run a bounded prototype only where it resolves uncertainty; identify temporary dependencies and expiry criteria
- [x] Task: Record decision, update spec and implementation tasks; propagate any changed prerequisite assumptions
- [ ] Task: Phase Verification & Checkpoint (refer to ../../workflow.md)

## Phase 3: Conditional implementation and verification

- [x] Task: If adopted, write meaningful failing contract/regression tests before changing behaviour
- [~] Task: Implement the selected bounded change and any data/config migration; remove superseded code only after parity
- [ ] Task: Verify tests plus applicable device/provider readback and rollback; otherwise document justified deferral
- [ ] Task: Phase Verification & Checkpoint (refer to ../../workflow.md)

## Phase 4: Handoff

- [ ] Task: Update consolidated requirements, stack and dependency decisions with actual outcomes
- [ ] Task: Document residual gaps, next-track inputs and checkpoint evidence
- [ ] Task: Phase Verification & Checkpoint (refer to ../../workflow.md)

## Execution evidence (in progress)

- Direct javac red check: missing CallRecord/CallEvent/SliceEngine symbols in `.local/slice-domain-red.log`, before implementation. Initial 10 pure Java JUnit contracts passed afterward. Expanded suite and Android/native checks pending.
- The first Gradle invocation completed after domain implementation; despite its historical log filename it is a green result, not the red evidence.
- Research sources consulted 2026-10-20; native wrapper alternatives and deferral triggers documented in findings.
