# Plan: Parity and durable synchronisation

Track active: schema4 durable foundations have bounded JVM/native evidence. Paging, historical import, scheduling, UI/recovery and full parity acceptance remain required.

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
- [x] Operation owner/generation and preserving schema3 migration (39 JVM/25 host/7 API36 native checks; packages removed)
- [x] Retained source facts/raw presentation, preserving schema4 migration (42 JVM/25 host/10 API36 native checks; packages removed)
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

## Current ownership checkpoint

Schema3 ownership change verified in ownership-evidence.json. Next implement raw
presentation/exact source metadata retention, then bounded source traversal, frozen
import sessions, legacy matching, scheduling and UI. Final full-track review/archive
remain required; do not treat this checkpoint as parity completion.

## Current source-facts checkpoint

Source facts/presentation verified in source-facts-evidence.json. Next implement
bounded source traversal with stable snapshot/anchor reset handling, then frozen
historical sessions and automatic scheduling/UI integration. Prior resume sections
are historical; use this checkpoint and fresh device/process checks. Full review/
fixes/archive remain outstanding.


## Current paging checkpoint

Schema5 bounded paging/checkpoint foundation verified in paging-evidence.json:
48 JVM tests,25 host tests,13 API36 native invocations; package cleanup passed.
The initial native build exposed unavailable JUnit assertThrows; local assertion
helpers fixed compilation without changing test dependencies. Source-bound build,
formatting and lint pass (0 errors,30 warnings,5 hints). Frozen bounds, anchor
validation, cursor limits and preserving v4 migration are implemented. Filter/
activation settings and coordinator advancement are still required before P02 can
close. Next integrate scan delivery and frozen import sessions, then scheduler/UI.
Full-track review/fixes/archive remain outstanding.


## Scan coordinator checkpoint

Bounded source scans now connect to DurableDelivery and SqliteLedger checkpoint
ports. Five test-first coordinator regressions pass: multi-page activation/type
policy, crash after receipt before progress, pause/destination stops, extended type
exclusion and changed candidate rejection. Source-bound build passes53 JVM tests,
app/test APK compilation and lint;25 host tests pass. Exact inputs/artifacts in
scan-evidence.json. No new native coordinator runtime proof is claimed. Persisted
activation settings/generation publication, source-independent pending recovery,
frozen historical sessions, scheduler/UI and full acceptance remain required.


## Activation control checkpoint

Four test-first controller regressions pass: absent/disabled default, pause across
controller recreation, new generation/boundary invalidating old workers, publication
failure/overflow and full policy match. Versioned Android SharedPreferences adapter
compiles, with process fail-closed latch for failed commit. Source-bound build passes
57 JVM tests, APK assembly and lint;25 host tests pass. Exact inputs/artifacts in
activation-evidence.json. Native persistence/fault tests remain pending. Next qualify
store readback and connect settings to scanner/scheduler/UI, then frozen imports,
legacy matching and full acceptance. This is not completed automatic logging.


## Native activation checkpoint

Two isolated Android settings tests pass: committed document exists on disk,
adapter recreation reads selected activation/pause/type settings, generation2
rejects generation1 workers, and malformed/future documents fail closed. Fixture
preferences deleted in finally. Full native regression matrix15/15 passes on API36;
test packages removed. Source-bound build57 JVM tests/APKs/lint and25 host tests
pass. Exact inputs/artifacts/native output in activation-native-evidence.json.
Reopen remains within one instrumentation process; no process-death or disk-failure
injection claim. Next implement verified automatic scan rollover and bounded
source-independent recovery before scheduler/UI; full Track4 remains open.
