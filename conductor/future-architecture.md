# Approved future architecture implementation sequence

User-approved2026-10-21. This is the current execution order; earlier roadmap
sections retain dated research history. Tracks below are implementation successors,
not reopened or overwritten research archives. [Machine-readable order](future-sequence.json).

| Stage | Track | Gate |
|---|---|---|
|1|[Track6 Kotlin](tracks/kotlin_20261020/index.md)|Active-code conversion, legacy audit, identical contracts and preserving update; full M4-linked acceptance retained|
|2|[Track5 release](tracks/release_20261020/index.md)|Identity/key custody, licences/trust, independent reproduction, upgrade/recovery/support and observation; development identity retained until decision|
|3|[Track14 Jetpack Compose Android implementation](tracks/compose_app_20261021/index.md)|Extract authoritative screen state and actions; migrate settings/status, manual logging, import and recovery in that order. Compose must never acquire write authority through rendering.|
|4|[Track15 KMP shared core implementation](tracks/shared_core_20261021/index.md)|Extract portable product rules behind compatibility facades; keep one persistence/writer owner. Validate common fixtures on Android/JVM and macOS Kotlin/Native.|
|5|[Track16 macOS import and calendar companion](tracks/macos_companion_20261021/index.md)|Provide deliberate supported-file import, preview, review and selected-calendar delivery using native file selection and EventKit. No automatic device synchronization.|
|6|[Track17 Compose Multiplatform Android and macOS UI](tracks/shared_ui_20261021/index.md)|Share suitable presentation state and screens after useful native companion workflow exists. Preserve native permission, file, calendar and platform navigation boundaries.|
|7|[Track18 Rust component evaluation and conditional integration](tracks/rust_component_20261021/index.md)|Evaluate one demonstrated parsing or portable-backup need against Kotlin/KMP/native alternatives. Adopt a narrow component only after application-level benefit; justified deferral is a valid decision.|
|8|[Track19 Consolidated Android and macOS qualification](tracks/cross_platform_20261021/index.md)|Qualify Android/macOS interoperability, supported formats, packaged builds, upgrades/recovery and claimed Google/Outlook calendar routes. Resolve Rust disposition and retire superseded prototypes.|
|9|[Track20 iOS companion implementation — last](tracks/ios_companion_20261021/index.md)|Only after stages1–8 pass or have explicit justified deferrals, reuse qualified core/UI for an ordinary iOS manual/import/review/calendar companion. No Android-equivalent automatic capture promise.|

## Sequencing and authority

Stages1–2 retain existing partial checkpoints and may complete safe preparatory
work. Track6's metadata prerequisite on release denotes its original full-track
acceptance, not a prohibition on the user-approved local Kotlin conversion.
No dependency cycle is introduced to make Track5 depend on full Track6 acceptance.
Later stages start after prerequisites pass or have an explicit justified deferral.
iOS implementation starts last, after dispositions for every earlier stage.
A deferral records reason, evidence, consequences and reconsideration trigger in
track-local deferral.md, metadata implementation_status=deferred and metadata
deferral fields reason, consequences and reconsideration_trigger. It never
qualifies deferred capabilities or waives signing/publication/device authority.
The verifier rejects active/completed iOS while any earlier stage lacks a disposition.

## Architecture and dependency rules

Shared core contains portable rules/state transitions without Android/UI/credentials.
Shared presentation has immutable state and explicit actions; rendering never writes
calendars. Native adapters own acquisition/calendars/storage/scheduling/permissions/
secrets/lifecycle. Retain the Android ledger initially and preserve schema6/import2,
signatures, namespaces and old receipts. One writer per actual destination; foreign
imports do not grant receipt replay authority. No automatic cross-device sync.

Every successor performs mapping, requirement refinement, dependency/native/custom
comparison and uncertainty-resolving prototype work before production adoption.
Temporary dependencies have expiry/removal criteria; long-term takeovers require
parity, upgrade/recovery and retirement of duplicate responsibilities. Research
versions are not production pins. Rust is a narrow optional component decision,
not a second synchronization engine or a prerequisite forcing adoption.

## Verification and scope

Preserve golden identity/rendering/null/time/overflow fixtures. Test uncertainty,
duplicates, edits/deletions, permissions, cancellation and process recovery.
Shared/unit/rendered/native/cloud/operational outcomes are separate. Rust adds
hostile-input bounds/fuzzing, panic/error/lifetime/FFI and end-to-end measurements.
Review/fix each successor before closure and preserve working outputs until parity.

macOS is the first companion. Windows/Linux are outside this sequence. Direct
Graph/Google/CalDAV connectors need a demonstrated native coverage gap and separate
authentication/reconciliation acceptance. Physical calls, cloud consent, production
signing and publication remain separate gates. No new skills or dependencies were
installed by creating these tracks. Repository Markdown remains the maintained
source; this does not require structured Word generation.
