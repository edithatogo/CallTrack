# Journal/productivity findings

Refreshed2026-10-21 at baseline `6ee341e`; activation `1f3b07c`. This is requirements
research, not a delivered journal. Track5 release acceptance and Track4 parity remain
open. The user requested later research of these enhancements, not unconditional
adoption. A priority question was offered during discovery; absent an answer the
ordering below is an analyst proposal, not validated user preference.

## Current data and behavior

The working app has a latest-call manual action and delivery receipts. It has no
journal editor/search, tags, callback queue, portable backup, rule preview or
billing workflow. Calendar notes edited by people are protected by readback; they
are not an app-owned notes database. DurableDelivery clears personal payloads on
confirmed/review/suppressed transitions; pending payload lacks time-based expiry.
Receipts retain source/event identity and hashes. Hashes can still be sensitive.
[Current lifecycle](../../../PRIVACY.md).

A journal cannot use outbox payload as durable user content. It needs explicit
annotation identity and ownership independent of source-call deletion, delivery
status, renamed contacts and regenerated calendar fields. This expands retained
personal data and needs a deliberate retention/erase/export policy. The source
call log, journal annotation and remote calendar each retain their own authority.
No source deletion, calendar note copying or automatic remote note merge is implied.

Manifest allowBackup=false and slice backup/transfer exclusions are present. They
are not a portable user backup or proof of every device's transfer behavior. No
current encryption/restore capability is asserted. The initial inspection used
wrong backup filenames; actual slice_backup_rules/slice_extraction_rules were read.

## Workflow mapping and proposed sequence

| Proposed order | Slice / need | Why / boundary |
|---|---|---|
| First candidate | Local notes/outcomes/tags with bounded search | Useful independent user work; preserve edits across replay/source deletion; local-only by default |
| Companion safety gate | Versioned export/restore of annotations and configuration | Establish recovery before relying on journal data; never restore credentials or blindly reuse destination IDs |
| Next candidate | Callback list and optional reminder | User decides which missed call matters; explicit done/snooze/cancel, no call placement or automatic messaging |
| Later | Rules for exclusions/redaction/work-personal routing | Dry-run and deterministic precedence; Track10 destination identity/fan-out prerequisites |
| Later | Time summaries/project attribution | Keep actual seconds, user-selected reporting period/timezone and separate rounding policy; no automatic invoice |
| Deferred without demonstrated need | SMS capture, CRM/tasks, AI classification, attachments | Additional permissions/data/support burden; no assumed SMS access, recording, transcript, cloud analysis or external writes |

The first two slices may change order after user choice. No feature is marked
user-validated or ready for implementation just because it appears in this table.
Manual-entry/import companion workflows remain proposals owned with Track12.

## Reuse and dependency research

| Option / observed version | Existing/new responsibility it could replace | Cost / disposition |
|---|---|---|
| Native SQLite queries + small annotation repository | Extend existing app-owned persistence without search framework | Schema/migration/Unicode query and performance tests; candidate only; never make ledger payload into journal |
| Room FTS and DAO layer | Replace handwritten annotation SQL/schema work | Compiler/driver/migration cost; current app has SQLiteOpenHelper, no Room. Android/SQLite feature support must match selected driver/version |
| AppSearch stable1.1.0, alpha1.2.0-alpha02 observed | Replace custom ranking/tokenization/index maintenance | Extra index/backend/schema/privacy lifecycle; Android backend differences and no automatic KMP support; defer pending search need/measurements |
| Apache Commons CSV1.14.1 release; site header1.14.2-SNAPSHOT | Replace handwritten quoting/parsing | Java8+ baseline, transitive graph/Android compatibility untested. Format library does not decide schema, spreadsheet-safe display or restore semantics; defer |
| ical4j4.4.0 observed release | Replace handwritten ICS parser/serializer/timezone handling | JVM/Android graph/runtime and security review needed; not common KMP by default. No ICS library installed |
| Versioned JSON with serialization library | Machine-readable round-trip annotations/configuration | Exact library/compiler versions and schema migration must be pinned at trial; no production dependency selected |
| Native document picker/SAF | Reuse supported save/open flows instead of broad file access | Chosen URI can be remote/cloud; cancellation, partial write and revoked access must be handled; no picker implemented |
| Native notifications/AlarmManager | Reuse platform scheduling instead of custom timer | Permission/reboot/timezone/recovery behavior and timing limits; no new exact-alarm or notification permission selected |
| Tink Streaming AEAD candidate | Reviewed encryption primitive instead of app-owned crypto | Key/password recovery, backup format and platform/runtime qualification still required; exact release/licence graph unresolved; defer |
| Declarative small ordered rules | Avoid embedding a general rules runtime | Conflicts/preview/explanation tests needed; external rule engine unjustified for current needs |
| Rust parser/archive component | Possible future shared import/export implementation | Track13 must prove real reuse/robustness/maintenance benefit; no Rust or FFI adoption |

Room versus SQLDelight/KMP is already a deferred comparison in Track8; do not add
multiple database owners. AppSearch adds a derived index, never a second canonical
journal. No latency, binary-size, import throughput or battery measurements exist.
Libraries are candidates to take over custom work, not mandatory dependencies.

## Primary sources refreshed2026-10-21

- [AppSearch release status](https://developer.android.com/jetpack/androidx/releases/appsearch) distinguishes stable1.1.0 from alpha examples.
- [AppSearch backends](https://developer.android.com/develop/ui/views/search/appsearch) distinguishes Local/Platform/PlayServices storage; do not infer global visibility or backend availability from a library name.
- [Room FTS/entity support](https://developer.android.com/training/data-storage/room/defining-data) documents FTS and driver-dependent support; prove the chosen Android build rather than assuming all system SQLite versions match.
- [SAF document access](https://developer.android.com/training/data-storage/shared/documents-files) supports user-selected documents; provider-specific write/overwrite behavior requires testing.
- [Notifications permission](https://developer.android.com/develop/ui/compose/notifications/notification-permission) and [alarm guidance](https://developer.android.com/develop/background-work/services/alarms) inform denied-permission/inexact behavior; exact timing is not promised.
- [Auto Backup](https://developer.android.com/identity/data/autobackup) is separate from explicit user export/restore.
- [Work profiles](https://developer.android.com/work/managed-profiles) require respecting platform/admin boundaries; no cross-profile movement is authorized here.
- [Commons CSV1.14.1 download](https://commons.apache.org/proper/commons-csv/download_csv.cgi) and [release history](https://commons.apache.org/proper/commons-csv/changes.html) show a released artifact distinct from snapshot site headers.
- [CSV format](https://www.rfc-editor.org/rfc/rfc4180) and [iCalendar format](https://www.rfc-editor.org/rfc/rfc5545) define interchange syntax, not a complete backup/restore protocol.
- [ical4j4.4.0 release](https://github.com/ical4j/ical4j/releases/tag/ical4j-4.4.0), [project](https://github.com/ical4j/ical4j) provide candidate maintenance evidence.
- [Tink Streaming AEAD](https://developers.google.com/tink/streaming-aead) is a primitive candidate, not a selected password-based backup solution.

Commons CSV's upstream Apache-2.0 terms are linked from its site. AndroidX/Kotlin
candidates have upstream Apache-2.0 licensing; all exact artifact/transitive notices
must be captured at adoption. ical4j/Tink exact artifact licences, versions and Android
matrix remain trial gates; failed auxiliary licence/release pages were not treated
as evidence. No dependency was downloaded, trialed or selected. API/release snapshots
must be refreshed before production decisions; maintenance activity is not acceptance.
