# Plan: Parity and durable synchronisation

All tasks pending. Discovery is real track work. Implementation is conditional; do not mark it complete when only research/prototypes exist.

## Phase 1: Discovery and mapping

- [x] Task: Refresh legacy parity matrix and missing/zero-duration/contact behaviour
- [~] Task: Research durable outbox, deduplication and legacy-migration approaches including replacement libraries
- [~] Task: Specify edit/deletion, restart, offline, import and privacy semantics before implementing
- [~] Task: Write findings and refine track requirements/acceptance with source provenance
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

### Implementation slices still required

- [x] Durable intent engine, source-independent resume, v1 preservation and native persistence tests (31 JVM/19 host tests; four API36 native invocations passed; packages removed)
- [ ] Retained source facts and operation ownership/generation, with migration tests
- [ ] Bounded source paging, checkpoint/anchor validation, filter/settings policy and reset handling
- [ ] Frozen historical preview, confirmed bounded import, cancel/resume and partial-progress handling
- [ ] Read-only legacy confidence/ambiguity matcher
- [ ] Prompt scheduling plus periodic/reboot/reopen catch-up using a qualified WorkManager version
- [ ] User settings, destination/permission flows, pending/review/error status and explicit recovery policy
- [ ] Full emulator parity/background/failure matrix with exact artifact and cleanup evidence

## Phase 4: Handoff

- [ ] Task: Update consolidated requirements, stack and dependency decisions with actual outcomes
- [ ] Task: Document residual gaps, next-track inputs and checkpoint evidence
- [ ] Task: Phase Verification & Checkpoint (refer to ../../workflow.md)

## Execution checkpoints

Start: c4c8c02. User selected emulator tests initially (physical-call acceptance pending), prompt logging when possible with recovery after delays. Concrete P01–P10 requirements and pre-insert intent design are recorded before implementation.

First foundation: 11 new pure Java durable-delivery contracts passed with 16 prior slice contracts. Native store contracts first failed to compile on missing outbox() before schema2 implementation. Android build and emulator migration tests remain pending. Automatic scheduling, paging/import, UI/status and full parity matrix are still required; this foundation is not track completion.

Foundation build passed (27 JVM tests; app/test APKs and lint, unchanged inputs), followed by a twelfth durable-core contract for source-independent resume (28 pure Java tests passing). Three native store tests are compiled: v1 preservation/migration, committed state and payload clearing across reopen, and nested-transaction rejection. Emulator image installation is running after explicit user approval of android-sdk-arm-dbt-license.

## Resume checkpoint

Native foundation receipt: foundation-evidence.json. Current source has a whitespace-only formatter follow-up after that exact receipt; refresh the build receipt with the next implementation slice before installing another APK. Core remains manual in the user UI; DurableDelivery is not yet wired to automatic/import work.

Dedicated emulator is live: emulator-5580 / calltrack-parity-api36-20261020; host process 60763 (unified exec session 88739 at creation). Always inspect current process/device state before reuse; do not restart from a stale session or timeout alone. Store tests removed both development packages. The physical phone is not used for Track 4. SDK/image licences and user timing/emulator choices are recorded in findings/decisions.

Next: retain source facts/raw presentation and operation-owner generation in the durable model, with a preserving schema evolution and tests; then implement bounded source traversal/reset guards, import snapshots/cancel/resume, legacy matching, scheduler and user UI. Full emulator recovery matrix, final review/fixes/archive remain required.

## Integration research handoff (nonblocking)

[Track10 contracts](../../archive/destinations_20261020/requirements.md) identify future route
identity, epoch/rebinding and opaque remote-ID migration requirements. Retain current
receipt compatibility; no direct connector/schema expansion is adopted here.
Provider-cloud qualification and native marker survival remain unverified.
