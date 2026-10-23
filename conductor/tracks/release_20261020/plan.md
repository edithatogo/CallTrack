# Plan: Community distribution and release readiness

Original release acceptance is retained. Preparation can progress independently of
Track4; a source-matched release candidate and recorded operational acceptance
cannot. Track remains active and cannot yet be archived as complete.

## Phase 1: Discovery and mapping

- [x] Task: Research distribution eligibility, licensing and reproducible release/signing options — findings.md
- [x] Task: Map contributor needs, dependency inventory and privacy/data lifecycle — CONTRIBUTING.md, PRIVACY.md, dependency-report.json
- [x] Task: Specify alpha observation, update/migration/rollback and supported-device criteria — RELEASING.md
- [x] Task: Write findings and refine track requirements/acceptance with source provenance — requirements.md D01–D09; original acceptance retained
- [x] Task: Phase Verification & Checkpoint (refer to ../../workflow.md) — verification.md and preparation-evidence.json

## Phase 2: Options and dependency decision

- [x] Task: Compare retained/native/custom implementation with dependencies and deferral — decisions.md RD01–RD03
- [x] Task: Run a bounded prototype only where it resolves uncertainty; identify temporary dependencies and expiry criteria — native dependency report only; extra plugin/signing prototype unjustified
- [x] Task: Record decision, update spec and implementation tasks; propagate any changed prerequisite assumptions — independent preparation permitted; release still depends on M3
- [x] Task: Phase Verification & Checkpoint (refer to ../../workflow.md) — no new dependencies adopted

## Phase 3: Conditional implementation and verification

- [x] Task: Prepare contributor, security, lifecycle and release documents plus issue/PR templates; no app behaviour or dependency changes
- [x] Task: If adopted, write meaningful failing contract/regression tests before changing behaviour — no app change; existing documentation fixture test exposed new root-document dependency and was corrected
- [ ] Task: Qualify a source-matched release candidate after M3, stable identity/key custody and dependency/artifact provenance audit
- [ ] Task: Verify candidate tests plus device/provider readback, preserving upgrade and recovery; record operational acceptance separately
- [ ] Task: Phase Verification & Checkpoint (refer to ../../workflow.md)

## Phase 4: Handoff

- [x] Task: Update consolidated requirements and roadmap with actual preparation outcomes; no production stack adoption
- [x] Task: Document residual gaps, next-track inputs and checkpoint evidence — D01–D09, verification.md
- [ ] Task: Complete release/operational gates, then final checkpoint and archive

## Phase: Review Fixes

- [x] Task: Apply review suggestions — 7a7d180; README schema2 correction, transitive inventory, test fixture completeness and retained release gate

## Execution checkpoint

Track start: 4b31e39. User explicitly authorised implementation, review, fixes and
archive once completed. Track4 remains active, physical-call acceptance pending;
that prerequisite cannot be manufactured by release documentation. No publishing,
key generation, account consent or external messaging was performed. Resume at
D01–D09 after the M3 evidence exists; do not repeat discovery without changed inputs.

Preparation implementation: 4754d72. Review corrections: 7a7d180. Final repository
checks passed; no candidate release was created and archive remains conditional on
the open Phase3/4 acceptance tasks.

## Productivity research handoff

[Track11 requirements](../../archive/productivity_20261020/requirements.md) distinguish future
annotations/index/export copies from existing payload/receipt retention. No new
storage/backup policy is adopted. Reopen lifecycle and recovery acceptance before
shipping authored journal data; research closure is not release readiness.
