# Plan: CallTrack and FOSS capability mapping

Research execution complete; final verification and formal review pending. Application implementation phase is not applicable per decisions D04; checklist completion records disposition, not app delivery.

## Phase 1: Discovery and mapping

- [x] Task: Complete APK preference, API, event-format and scan-limit mapping; distinguish static inference from runtime proof
- [x] Task: Map both pinned FOSS sources including dependencies and custom code responsibilities
- [x] Task: Define synthetic parity fixtures, user outcomes and unresolved requirement decisions
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

## Execution evidence

- Phase 1: findings CT01–CT13, source/dependency maps, requirements U01–U12 and 25 synthetic scenarios; see findings.md, sources.json, fixtures.json.
- Phase 2: decisions D01–D05. Androguard used as isolated analysis tool; no runtime dependency promoted. A network/device prototype would not resolve a mapping uncertainty and is deferred.
- Phase 3: **N/A application changes/tests/migration**; scope clarification and D04 document the allowed deferral. Documentation/source verification applies instead.
- Phase 4: consolidated findings and downstream handoff updated; see verification.md.

## Phase: Review Fixes

- [~] Task: Correct stale global status and misleading fixture cross-reference; verify archival links and validator failure detection

## Commit history

- d3a68d7: track start.
- f5fb750: static mapping, requirements, decisions, fixtures and evidence verification.
