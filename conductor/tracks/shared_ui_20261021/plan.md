# Plan: Compose Multiplatform Android and macOS UI

## Phase 1: Refresh mapping and requirements

- [x] Task: Read prerequisite evidence and archived research; map behaviour, native APIs, dependencies and code ownership.
- [x] Task: Record findings, refined requirements and explicit acceptance/unsupported cases.
- [x] Task: Phase Verification & Checkpoint (refreshed compatibility, host gates and source links recorded).

## Phase 2: Dependency and prototype decision

- [x] Task: Compare retained/native/owned/library replacement and deferral, including prototype-only versus long-term responsibilities.
- [x] Task: Run only uncertainty-resolving bounded experiments; record versions/licences/transitives/toolchain/cleanup and expiry criteria.
- [x] Task: Record adoption/deferral decision and refine implementation/rollback tasks before production change.
- [x] Task: Phase Verification & Checkpoint (prototype-only choice; final Compose Multiplatform 1.10.3 graph selected for the qualified API36/AGP8.13.2 toolchain; refreshed desktop and Android graph hashes match canonical artifacts and all POMs declare effective licences; legal clearance, build-tool trust and distribution notices remain separate gates).

## Phase 3: Conditional implementation

- [x] Task: Establish golden/contract baseline and meaningful failing regressions before behaviour changes. (The new reducer/UI tests failed on unresolved contracts before implementation, then passed.)
- [x] Task: Map Android/macOS screen differences, controller ownership, keyboard/focus/accessibility and packaging requirements; refresh Compose Multiplatform/native graph.
- [x] Task: Implement shared immutable presentation state and explicit actions, then share rendering without duplicate production owners or automatic effects. (Synthetic commonMain + desktop renderer only.)
- [x] Task: Retain native prompts/pickers/adapters and platform navigation where needed; qualification determines each shared boundary. (No platform adapter or root app integration added.)
- [x] Task: Compile and package the synthetic shared screen through an Android KMP target and Android Activity host. (Debug APK assembled; no device install/rendering claimed.)
- [ ] Task: Run rendered Android/macOS workflows, native keyboard/focus/large-text/RTL/manual accessibility, package/update/restoration checks before removing old rendering.
- [ ] Task: Phase Verification & Checkpoint (refer to ../../workflow.md).

## Phase 4: Review, acceptance and handoff

- [ ] Task: Run applicable shared/rendered/native/cloud/package/operational acceptance with exact source/artifact evidence; verify cleanup and preserving upgrade/recovery.
- [x] Task: Perform bounded-slice Conductor review, apply fixes and rerun affected checks (review-20261024.md; native acceptance remains open).
- [ ] Task: Update requirements, decisions, remaining limitations and next-track inputs; retire superseded code/prototypes only after parity.
- [ ] Task: Final checkpoint and archive only after original acceptance, or explicit justified deferral with documented consequences and reconsideration trigger.

## Local checkpoint — 2026-10-24

- The pre-implementation `desktopTest` red run reported unresolved
  `CompanionUiState`, `CompanionUiAction`, reducer and shared-screen references.
- With JDK17.0.21+9, isolated Gradle user home, Gradle8.13, Kotlin2.4.21 and
  Compose Multiplatform1.12.2, the offline `desktopTest` rerun passed seven tests:
  three common reducer cases and four rendered desktop interactions.
- Gradle8.13 emits a Kotlin warning that future Kotlin2.5 requires at least
  Gradle8.14.4. Current project pins remain compatible; no root wrapper was changed.
- Android target compilation/rendering and native macOS packaging/accessibility
  had not yet been qualified at the earlier checkpoint.
- This is not completion or production adoption. The native/UI/upgrade/recovery
  acceptance phase remains open, as do Tracks14–16.

## Android-host checkpoint — 2026-10-24

- The explicit local Android SDK exposed API32 and API36 with build-tools
  30.0.3 and 35.0.0. The API36 emulator is present but reports `offline`.
- Compose Multiplatform1.12.2 failed Android AAR metadata checks because its
  AndroidX artifacts require API37.1/AGP9.1. The prototype was pinned to
  JetBrains Compose Multiplatform1.10.3, which compiles on the qualified
  API36/AGP8.13.2 setup. This is a prototype-only compatibility pin.
- The shared KMP Android target compiled and
  `:android-host:assembleDebug desktopTest` passed. The debug APK is 8.9 MiB;
  its SHA-256 and source/toolchain details are in
  [review-20261024-android-host.md](review-20261024-android-host.md).
- All seven desktop reducer/rendered UI tests passed. Android rendered,
  accessibility, lifecycle, install/update and recovery checks remain open
  because the emulator is offline. No real product screen or data adapter was
  integrated.
- Fresh canonical download/hash and POM metadata audits cover the Android
  host/shared Android graph (91 artifacts) and updated desktop runtime/test
  graph (63 artifacts); see
  [android-host-dependency-audit.json](android-host-dependency-audit.json).
  These do not provide legal clearance, build-tool trust, vulnerability
  assessment, or complete notice-text delivery.

## Review checkpoint — 2026-10-24

- Reviewed implementation commit `bea8385` against `de96b16`; non-Conductor
  scope was 214 lines across nine changed files.
- Review corrected the dependency decision's Gradle pin and removed a duplicate
  plan link from the track index. No remaining correctness/style findings.
- Focused desktop, Python verifier, Conductor validation and whitespace checks
  passed; details are in [review-20261024.md](review-20261024.md).
- Track remains in progress: Android target/rendering, native macOS packaging,
  accessibility, lifecycle, package/update and recovery acceptance are open.
