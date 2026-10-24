# Track 19 evidence reconciliation — 2026-10-24

This is a bounded prerequisite reconciliation, not cross-platform acceptance.
Sources are the current Track 14–18 metadata, plans, findings, decisions,
verification and scoped prerequisite records.

| Qualification area | Current evidence and owner | Gap / acceptance needed |
|---|---|---|
| Android app and event ledger | Root Android app remains the qualified native owner for CallLog acquisition, identity/receipt, SQLite ledger, scheduling and CalendarContract delivery. Track 14 replaced only a read-only settings/status slice. | Finish Track 14 UI parity/lifecycle/accessibility and Track 5/6-linked release acceptance; do not treat Android state as a shared portable receipt. |
| Shared domain | Track 15 shares bounded call-type selection and half-open time-range rules on JVM/Android. | Run the same golden/fault fixtures on macOS Kotlin/Native and document ABI/serialization boundaries before claiming portable core parity. |
| macOS import | Track 16 has a bounded Foundation JSON preview parser, now exercised against a prototype Android encoder and shared golden: schema v1, 5 MiB, 10,000 records, opaque UUIDs, direction, epoch-millisecond timestamps, durations, record count and canonical SHA-256 corruption checksum. | The Android exporter is not integrated and this is not an accepted interchange specification. Define version evolution, hostile-input/JSON ambiguity rules, provenance, foreign-receipt handling and restore/recovery before compatibility claims. |
| macOS calendar | EventKit API declaration probe only; no permission, destination enumeration, event write or exact readback. | Native app, user-selected calendar, permission/denial handling, durable intent, one writer, exact local readback, edit/delete recovery and cloud-route evidence are absent. |
| Shared UI | Track 17 now has a shared synthetic screen, a KMP Android target and a debug Android Activity host. PR #60 merged the compile/package evidence. Android KMP/host and desktop graphs were audited; production adoption remains false. | The API36 emulator is offline, so Android rendering/accessibility/lifecycle acceptance remains false. There is no production app integration or native macOS package. Qualify rendered Android and macOS-native workflows, accessibility/focus/keyboard/scaling/RTL and lifecycle recreation; prove rendering causes no writes. |
| Package and upgrade | Android has separate release evidence under Track 5; no packaged macOS companion or common update contract exists. | Freeze exact per-platform candidates and provenance; perform fresh install, preserving upgrade, interrupted update/restore, identity and rollback checks on each packaged product. |
| Calendar route | Android provider-local behavior has selected emulator evidence; user has not selected Google/Outlook for live qualification. macOS EventKit has no selected destination. | Keep Google/Outlook and actual account-cloud synchronization unclaimed until explicitly selected and exact local plus remote readback is authorized and captured. |
| Rust | Track 18's reviewed decision defers production Rust: the current shared format is prototype-only, with no app-integrated producer, qualified workflow or measured bottleneck. | Keep Rust out of production. Reconsider only after a real cross-platform workflow establishes a stable format and measurable need. |

## Prototype interchange contract — 2026-10-24

A bounded prototype now connects `CallRecord` to Track 16's preview parser using
one synthetic schema-v1 golden fixture. The Kotlin producer emits only opaque
per-source-row UUIDv8 identifiers, direction, epoch-millisecond start,
duration, export UUID, record count and a SHA-256 corruption checksum. Names,
numbers, account fields, raw source namespace/row IDs and calendar receipts are
excluded. Unknown call directions, malformed source IDs, duplicate identities,
time overflow, over-10,000 rows and output over 5 MiB fail closed. The checksum
covers the producer's canonical JSON without the checksum member; it detects
accidental corruption and is not a signature or authenticity guarantee. Call
times and directions are still sensitive personal data; any eventual export
workflow needs explicit user choice, safe storage/sharing guidance and deletion
behavior.

The Swift preview validates the same canonical checksum and record count before
returning records. `ParserContractHarness.swift` compiled with the installed
Swift 6.4 command-line toolchain and passed its synthetic cross-language golden,
duplicate, version, timestamp, duration, count, checksum and size checks. This is
source-level parsing evidence only. The producer is not exposed by the Android
app, the file is not saved or delivered by a UI, the format is not a stable
public API, and the importer still grants no receipt authority or calendar-write
permission. Android JVM unit tests and the SwiftPM/XCTest runner remain subject
to their separately recorded results.

The golden UUIDs are synthetic. Stable portable IDs are scoped to the Android
source-install namespace and source row identity, so they support deterministic
export comparisons without revealing either input. A foreign export ID or row
ID does not authorize replay. Schema evolution, unknown/duplicate JSON-key
handling, restore semantics, hostile-input fuzzing, native file selection,
packaging, and native/remote calendar acceptance remain open.

## Conditional end-to-end acceptance matrix

1. **Contract:** freeze versioned Android export/macOS import schema and golden
   fixtures; test malformed, oversized, duplicate, unknown-field, redaction,
   timestamp/null/overflow, foreign-receipt and ambiguous-identity cases.
2. **Shared logic:** run identical contract/fault vectors on Android/JVM and
   macOS Kotlin/Native; record exact source, toolchain, artifact and result.
3. **Delivery:** deliberate preview and confirmation; one platform-local writer;
   verify exact native event readback. Keep provider-local persistence separate
   from remote synchronization evidence.
4. **Failure/recovery:** interruption before/after insert, uncertain result,
   permission loss, account/calendar disappearance, edited/deleted events,
   duplicate replay and restore. Preserve human edits and receipts.
5. **Packages:** inspect signed/unsigned provenance and dependency/licence closure;
   fresh-install and preserving-upgrade each platform build; verify rollback and
   data preservation.
6. **Calendar claims:** only for each explicitly selected route, retain exact
   local provider and exact-account remote readback, with route and coverage
   limits. Otherwise label the route unsupported/unqualified.
7. **Human factors and operation:** screen-reader, keyboard/focus, scaling,
   localization/RTL and lifecycle/process recovery; separate device coverage and
   operational observation from automated snapshots.

Completion requires the relevant source/artifact-bound evidence for every claimed
capability. Unselected routes remain explicitly unsupported. The current work
does not execute or pass these gates.

## Track 17 evidence refresh — 2026-10-24

The Track 17 refresh added an Android KMP target and synthetic host to the
evidence set: source commit `6bdd3b4efc99d281f2d6d231a75bdfc14363d80a`, PR
[#60](https://github.com/edithatogo/CallTrack/pull/60). Its API36 debug APK
assembled locally and the shared desktop test suite passed seven tests. The
Android emulator is offline, so this does not count as Android rendering,
instrumentation, accessibility or lifecycle acceptance. The host does not
integrate into the production Android application and declares no call-log,
contacts, calendar or storage access.

The current prototype pins Compose Multiplatform1.10.3 for API36/AGP8.13.2;
1.12.2 failed local AAR metadata checks because its Android artifacts require
API37.1/AGP9.1. The Android graph audit covers 91 unique artifacts and the
desktop runtime/test audit covers 63; repository bytes matched and all POMs
provided effective licence metadata. This is provenance metadata only, not
legal clearance, a vulnerability assessment, complete notice-text delivery or
build-tool trust. See Track 17's
[Android host review](../shared_ui_20261021/review-20261024-android-host.md)
and [graph audit](../shared_ui_20261021/android-host-dependency-audit.json).

This fresh evidence narrows the Android build uncertainty but does not satisfy
Track 19's package, upgrade, portability, native EventKit, real event readback,
remote-route or operational gates. Tracks14–16 and Track5 release acceptance
remain upstream dependencies. Track20/iOS remains locked.
