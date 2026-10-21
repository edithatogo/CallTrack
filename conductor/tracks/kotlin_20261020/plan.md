# Plan: Kotlin migration and dependency reassessment

Track start4e8148a. Independent discovery and a three-component migration proceed;
M4 prerequisite, full Java migration and original upgrade/acceptance gates remain.
No archive/completion from the bounded slice alone.

## Phase 1: Discovery and mapping

- [x] Task: Map Java modules, coupling and dependency/custom-code responsibility — findings.md,24 files/ownership groups
- [x] Task: Refresh Kotlin ecosystem options and test incremental migration; compare replace versus translate — K01/K02 decisions; Java golden baseline passed before conversion
- [x] Task: Specify migration order, persisted-data compatibility and remaining native responsibilities — requirements K01–K07
- [x] Task: Write findings and refine track requirements/acceptance with source provenance
- [x] Task: Phase Verification & Checkpoint (refer to ../../workflow.md) — bounded evidence in verification.md

## Phase 2: Options and dependency decision

- [x] Task: Compare retained/native/custom implementation with dependencies and deferral — Room/DataStore/coroutines/serialization/toolchain alternatives
- [x] Task: Run bounded prototype where it resolves uncertainty; identify temporary dependencies and expiry — mixed Kotlin2.3.21 pilot, retain Java snapshot/golden inputs privately; ktfmt0.59 build tool
- [x] Task: Record decision, update spec and implementation tasks; propagate prerequisites — no waiver of M3/M4 or stable-data contracts
- [x] Task: Phase Verification & Checkpoint (refer to ../../workflow.md) — bounded evidence in verification.md

## Phase 3: Conditional implementation and verification

- [x] Task: Establish Java golden contracts before conversion — five new cases pass with31 existing contracts on Java (36 total); no behaviour change requires an intentionally failing app contract
- [x] Task: Translate CallSelection/CallIdentity/CallEvent; preserve Java fields/static entry points/null, hash and exact arithmetic
- [x] Task: Add Kotlin-aware stale-receipt host regression; compiler targets/style tooling and CI
- [x] Task: Qualify mixed-language build, dependency graph, native SQLite regression and recovery at source-bound APK — 7cdd8d4, pilot-evidence.json
- [ ] Task: Finish stabilized active Java conversions and justified legacy removal/exceptions after M3/M4; preserve persisted data and same acceptance suite
- [ ] Task: Verify supported upgrade/operational gates and remaining exceptions for whole M5
- [ ] Task: Phase Verification & Checkpoint (refer to ../../workflow.md)

## Phase 4: Handoff

- [x] Task: Update consolidated requirements, stack and dependency decisions with actual bounded outcomes — 7cdd8d4
- [x] Task: Document residual gaps, next-track inputs and checkpoint evidence — K06/K07 remain open
- [ ] Task: Final full-track checkpoint; review/fix/archive when original acceptance passes

## Phase: Review Fixes

- [x] Task: Review bounded migration, apply findings, record evidence/commit and remaining gates — 491f936

## Resume checkpoint

Implementation7cdd8d4. Pilot qualification passes36 JVM/20 host/five native tests,
debug/test and unsigned release packaging. Both emulator packages are removed.
Continue K06/K07 after stabilized M3/M4 acceptance; do not repeat the completed
pure migration or equate schema fixtures with a production update. Next conversion
order: CallRecord after source-facts/presentation settles, then orchestration after
operation-owner/import/scheduler contracts, store/adapters with preserving tests,
current Activity, and reachability-based legacy removal/exceptions. Keep UI redesign
in Compose track. Archive remains conditional on full original M5 acceptance.

Review fixes/evidence: 491f936; original full-track acceptance remains open.

Compose handoff: [Track7 research](../../archive/compose_20261020/decisions.md) retains Views
and defers production integration. After stabilizing M3/M5, reopen its adoption
gates rather than assume the isolated prototype migrated the real Activity. This
is a handoff note, not a new Compose prerequisite for completing Kotlin work.

## M3 acceptance handoff — 2026-10-21

[Track4 handoff](../parity_20261020/handoff.md) now supplies selected API36 emulator
acceptance: schema6/import2,110 JVM/73 host tests and89 successful native invocations
plus one expected process kill. Resume the original remaining gates above; do not
repeat completed discovery or infer physical/cloud/release acceptance. This handoff
does not mark this track complete or waive its original prerequisites.
