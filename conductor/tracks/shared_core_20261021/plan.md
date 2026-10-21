# Plan: KMP shared core implementation

## Phase 1: Refresh mapping and requirements

- [ ] Task: Read prerequisite evidence and archived research; map behaviour, native APIs, dependencies and code ownership.
- [ ] Task: Record findings, refined requirements and explicit acceptance/unsupported cases.
- [ ] Task: Phase Verification & Checkpoint (refer to ../../workflow.md).

## Phase 2: Dependency and prototype decision

- [ ] Task: Compare retained/native/owned/library replacement and deferral, including prototype-only versus long-term responsibilities.
- [ ] Task: Run only uncertainty-resolving bounded experiments; record versions/licences/transitives/toolchain/cleanup and expiry criteria.
- [ ] Task: Record adoption/deferral decision and refine implementation/rollback tasks before production change.
- [ ] Task: Phase Verification & Checkpoint (refer to ../../workflow.md).

## Phase 3: Conditional implementation

- [ ] Task: Establish golden/contract baseline and meaningful failing regressions before behaviour changes.
- [ ] Task: Map Android/JVM dependencies and native target/toolchain constraints; compare retained code, common libraries and minimal native facades for hashing/time/overflow.
- [ ] Task: Extract models, mapping, identity/filtering and portable recovery decisions; native adapters own providers, persistence, scheduling, permissions and secrets.
- [ ] Task: Keep stored signatures, schemas, namespace and Java contracts unchanged; compare old/shared outputs read-only before switching one owner.
- [ ] Task: Run common golden/fault fixtures on Android/JVM and macOS Kotlin/Native, inspect target artifacts and preserve upgrade/recovery; no second ledger or dual writes.
- [ ] Task: Phase Verification & Checkpoint (refer to ../../workflow.md).

## Phase 4: Review, acceptance and handoff

- [ ] Task: Run applicable shared/rendered/native/cloud/package/operational acceptance with exact source/artifact evidence; verify cleanup and preserving upgrade/recovery.
- [ ] Task: Perform Conductor review, apply fixes and rerun affected checks.
- [ ] Task: Update requirements, decisions, remaining limitations and next-track inputs; retire superseded code/prototypes only after parity.
- [ ] Task: Final checkpoint and archive only after original acceptance, or explicit justified deferral with documented consequences and reconsideration trigger.
