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

## M3 acceptance handoff — 2026-10-21

[Track4 handoff](../../archive/parity_20261020/handoff.md) now supplies selected API36 emulator
acceptance: schema6/import2,110 JVM/73 host tests and89 successful native invocations
plus one expected process kill. Resume the original remaining gates above; do not
repeat completed discovery or infer physical/cloud/release acceptance. This handoff
does not mark this track complete or waive its original prerequisites.

## Resumed execution — 2026-10-21

- [x] Task: Qualify unsigned release build from two isolated clean committed-source checkouts — 6b353f4; unsigned-evidence.json: identical APKs,110 JVM tests each,zero lint errors. Same-host comparison is not independent-environment reproduction.
- [x] Task: Review candidate tooling and results — review-20261021.md; fixes66593c3,78 host tests. Original identity/signing, upgrade and operational gates retained.

Track4 M3 selected-emulator handoff is available. No production signing, key
generation, publication or physical-call testing is implied by this resumption.

User selected retaining the development identity and unsigned qualification only;
production signing remains pending. Native release/upgrade acceptance was not run.
Runtime graph inventory now includes89 artifact hashes and cached POM declarations,
with one missing POM licence declaration; this is not licence clearance or trusted
verification. Both clean builds used candidate6b353f4; later changes are tooling
tests/docs only and app/build inputs remain identical. Track remains active; no
archive or complete-M4 claim. See unsigned-evidence.json and review-20261021.md.
