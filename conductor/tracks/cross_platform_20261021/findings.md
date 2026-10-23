# Track 19 evidence reconciliation — 2026-10-24

This is a bounded prerequisite reconciliation, not cross-platform acceptance.
Sources are the current Track 14–18 metadata, plans, findings, decisions,
verification and scoped prerequisite records.

| Qualification area | Current evidence and owner | Gap / acceptance needed |
|---|---|---|
| Android app and event ledger | Root Android app remains the qualified native owner for CallLog acquisition, identity/receipt, SQLite ledger, scheduling and CalendarContract delivery. Track 14 replaced only a read-only settings/status slice. | Finish Track 14 UI parity/lifecycle/accessibility and Track 5/6-linked release acceptance; do not treat Android state as a shared portable receipt. |
| Shared domain | Track 15 shares bounded call-type selection and half-open time-range rules on JVM/Android. | Run the same golden/fault fixtures on macOS Kotlin/Native and document ABI/serialization boundaries before claiming portable core parity. |
| macOS import | Track 16 has a bounded Foundation JSON preview parser: schema v1, 5 MiB, 10,000 records, UUID identities, direction, epoch-millisecond timestamps and bounded durations. Unknown fields are ignored; malformed records fail closed. It excludes contact/phone/account data and foreign receipts. | No Android exporter or accepted interchange specification exists. Establish producer-owned export, canonical fixtures, version evolution, redaction, checksums/provenance, duplicate/ambiguity rules, and restore/recovery before compatibility claims. |
| macOS calendar | EventKit API declaration probe only; no permission, destination enumeration, event write or exact readback. | Native app, user-selected calendar, permission/denial handling, durable intent, one writer, exact local readback, edit/delete recovery and cloud-route evidence are absent. |
| Shared UI | Track 17's Compose Multiplatform UI is an isolated desktop JVM prototype with synthetic state/actions; Android integration and native macOS packaging are false/unaccepted. | Qualify Android target rendering, macOS native package, accessibility/focus/keyboard/scaling/RTL and lifecycle recreation; prove rendering causes no writes. |
| Package and upgrade | Android has separate release evidence under Track 5; no packaged macOS companion or common update contract exists. | Freeze exact per-platform candidates and provenance; perform fresh install, preserving upgrade, interrupted update/restore, identity and rollback checks on each packaged product. |
| Calendar route | Android provider-local behavior has selected emulator evidence; user has not selected Google/Outlook for live qualification. macOS EventKit has no selected destination. | Keep Google/Outlook and actual account-cloud synchronization unclaimed until explicitly selected and exact local plus remote readback is authorized and captured. |
| Rust | Track 18's reviewed decision defers production Rust: no shared accepted format, actual second consumer or measured bottleneck. | Keep Rust out of production. Reconsider only after an Android producer and a real second-platform consumer exercise the same format. |

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
