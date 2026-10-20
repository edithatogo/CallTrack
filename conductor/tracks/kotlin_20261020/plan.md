# Plan: Kotlin migration and dependency reassessment

Track start4e8148a. Independent discovery and a three-component migration proceed;
M4 prerequisite, full Java migration and original upgrade/acceptance gates remain.
No archive/completion from the bounded slice alone.

## Phase 1: Discovery and mapping

- [x] Task: Map Java modules, coupling and dependency/custom-code responsibility — findings.md,24 files/ownership groups
- [x] Task: Refresh Kotlin ecosystem options and test incremental migration; compare replace versus translate — K01/K02 decisions; Java golden baseline passed before conversion
- [x] Task: Specify migration order, persisted-data compatibility and remaining native responsibilities — requirements K01–K07
- [x] Task: Write findings and refine track requirements/acceptance with source provenance
- [~] Task: Phase Verification & Checkpoint (refer to ../../workflow.md)

## Phase 2: Options and dependency decision

- [x] Task: Compare retained/native/custom implementation with dependencies and deferral — Room/DataStore/coroutines/serialization/toolchain alternatives
- [x] Task: Run bounded prototype where it resolves uncertainty; identify temporary dependencies and expiry — mixed Kotlin2.3.21 pilot, retain Java snapshot/golden inputs privately; ktfmt0.59 build tool
- [x] Task: Record decision, update spec and implementation tasks; propagate prerequisites — no waiver of M3/M4 or stable-data contracts
- [~] Task: Phase Verification & Checkpoint (refer to ../../workflow.md)

## Phase 3: Conditional implementation and verification

- [x] Task: Establish Java golden contracts before conversion — five new cases pass with31 existing contracts on Java (36 total); no behaviour change requires an intentionally failing app contract
- [x] Task: Translate CallSelection/CallIdentity/CallEvent; preserve Java fields/static entry points/null, hash and exact arithmetic
- [x] Task: Add Kotlin-aware stale-receipt host regression; compiler targets/style tooling and CI
- [~] Task: Qualify mixed-language build, dependency graph, native SQLite regression and recovery at source-bound APK
- [ ] Task: Finish stabilized active Java conversions and justified legacy removal/exceptions after M3/M4; preserve persisted data and same acceptance suite
- [ ] Task: Verify supported upgrade/operational gates and remaining exceptions for whole M5
- [ ] Task: Phase Verification & Checkpoint (refer to ../../workflow.md)

## Phase 4: Handoff

- [~] Task: Update consolidated requirements, stack and dependency decisions with actual bounded outcomes
- [~] Task: Document residual gaps, next-track inputs and checkpoint evidence
- [ ] Task: Final full-track checkpoint; review/fix/archive when original acceptance passes

## Phase: Review Fixes

- [ ] Task: Review bounded migration, apply findings, record evidence/commit and remaining gates
