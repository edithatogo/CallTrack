# Plan: KMP shared core implementation

## Phase 1: Refresh mapping and requirements

- [x] Task: Read prerequisite evidence and archived research; map behaviour, native APIs, dependencies and code ownership (findings.md).
- [x] Task: Record findings, refined requirements and explicit acceptance/unsupported cases (requirements.md).
- [x] Task: Phase Verification & Checkpoint (refer to ../../workflow.md).

## Phase 2: Dependency and prototype decision

- [x] Task: Compare retained/native/owned/library replacement and deferral, including prototype-only versus long-term responsibilities (findings.md).
- [x] Task: Run bounded compatibility/toolchain checks; record versions/licences/transitives and unavailable native prerequisites (findings.md).
- [x] Task: Record adoption/deferral decision and refine implementation/rollback tasks before production change (decisions.md).
- [x] Task: Phase Verification & Checkpoint (refer to ../../workflow.md).

## Phase 3: Conditional implementation

- [x] Task: Preserve the existing call-selection contract baseline and add common boundary/unknown-type cases; see [implementation evidence](implementation-evidence.md).
- [x] Task: Map Android/JVM dependencies and native target/toolchain constraints; compare retained code, common libraries and minimal native facades for hashing/time/overflow (findings.md).
- [x] Task: Extract only raw call-type selection and half-open timestamp rules; native adapters remain authoritative for platform behavior.
- [x] Task: Preserve stored signatures, schema, namespace, receipts and Java facade; existing contract suite and common boundary tests pass; no writer or ledger change.
- [ ] Task: Run common golden/fault fixtures on Android/JVM and macOS Kotlin/Native, inspect target artifacts and preserve upgrade/recovery; JVM/Android build evidence passes, macOS Kotlin/Native is pending full Xcode.
- [ ] Task: Phase Verification & Checkpoint (JVM/Android tests and APK packaging pass; macOS Native acceptance remains pending).

## Phase 4: Review, acceptance and handoff

- [ ] Task: Run applicable shared/rendered/native/cloud/package/operational acceptance with exact source/artifact evidence; verify cleanup and preserving upgrade/recovery.
- [x] Task: Perform Conductor review, fix the synthetic prerequisite test fixture and rerun affected checks (review-20261024.md).
- [x] Task: Update requirements, decisions, remaining limitations and next-track inputs; no superseded writer or ledger code is retired.
- [ ] Task: Final checkpoint and archive only after original acceptance, or explicit justified deferral with documented consequences and reconsideration trigger; macOS Kotlin/Native acceptance is still pending.
