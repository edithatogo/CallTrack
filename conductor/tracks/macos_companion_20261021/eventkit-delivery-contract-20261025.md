# Track 16 EventKit delivery contract — 2026-10-25

## Scope and authority

This contract defines a local, explicit, one-destination-at-a-time import route.
It does not make the JSON prototype an Android-supported export format, and it
does not transfer authority from foreign record IDs to an Android receipt. The
imported file remains untrusted input. Preview performs no EventKit request,
calendar read, persistence, or write. EventKit access is requested only after a
user action that starts calendar setup; delivery requires a separate explicit
confirmation after the selected destination and aggregate counts are shown.

Use full EventKit event access, not write-only access, because the route must
list writable destinations and independently read back inserts. The host app
must provide `NSCalendarsFullAccessUsageDescription`; a Swift package cannot
provide the final host application's privacy declaration. Permission denial,
restricted access, write-only access, or revocation fails closed. No attendee,
alarm, invitation, or automatic synchronization is created.

## Identity and event mapping

- Local operation identity: a new random UUID, generated and durably saved before
  any EventKit mutation. Its `CallTrack operation: <UUID>` marker is stored in
  event notes so a write whose process dies before saving the EventKit ID can
  be found without relying on a mutable title.
- Import dedupe identity: `(calendarIdentifier, exportID, callID)`, unique in the
  local ledger. It is only a local duplicate guard; it is not proof of CallTrack
  ownership or permission to replay an Android receipt.
- Destination: the exact selected `calendarIdentifier` captured in the intent.
  A missing, read-only, renamed/recreated, or permission-inaccessible calendar
  is never silently substituted.
- Event fields: title is the generic direction label (`Incoming call`,
  `Outgoing call`, or `Missed call`); start/end are derived from validated UTC
  epoch milliseconds and duration; notes contain the opaque operation marker;
  attendees and alarms are empty. Phone numbers, contact names, imported
  payload IDs, and call text are not rendered into the event.
- Exact readback compares marker, destination, title, start/end, all-day state,
  attendees, and alarms. Save success alone is not reconciliation.

## Durable state machine

| State | Meaning | Permitted next action |
|---|---|---|
| `prepared` | Validated payload and exact destination are durably recorded; no write has been attempted. | User-confirmed first write may transition to `write_attempted`. |
| `write_attempted` | This state was durably committed before calling EventKit. Outcome may be unknown. | Read back by EventKit ID or operation marker; reconcile, mark conflict, or remain uncertain. Never insert again automatically. |
| `reconciled` | One exact event was read back and its EventKit identifier recorded. | Read-only verification; detect edits/deletion. |
| `uncertain` | A write was attempted but exact outcome cannot be proved. | Read-only retry/recovery; never blind reinsert. Human resolution is required if still absent/ambiguous. |
| `edited` | A previously reconciled event differs from the app's stored expected fields. | Preserve the human event; no automatic overwrite. |
| `deleted` | A previously reconciled event is absent from the selected destination. | Preserve the deletion; no automatic recreation. |
| `conflict` | Multiple marker matches, identity mismatch, destination mismatch, or inconsistent ledger state. | Stop and present a recovery choice; no mutation. |

The ledger uses SQLite from the platform-provided `libsqlite3`, with one process
writer, `BEGIN IMMEDIATE` transactions, a unique dedupe constraint, full
synchronous commits, and a private `0600` database in the host application's
Application Support directory. The ledger stores only UUID identities,
calendar/event identifiers, direction, start/duration, expected event fields,
state, and timestamps. It never stores phone numbers or source file contents.

## Failure and recovery rules

- Persist `prepared`, then persist `write_attempted`, before calling EventKit.
- After `save`, read the event back through EventKit and verify all exact fields
  before marking `reconciled`.
- After restart, `prepared` may proceed only after explicit user confirmation;
  `write_attempted`/`uncertain` may only be searched/read back. Zero matches do
  not prove a failed write, so they remain unresolved. One exact match may
  reconcile. One mismatch or multiple matches becomes `conflict`.
- A calendar disappearing, account/permission change, or EventKit error leaves
  the intent recoverable and blocks delivery. It never redirects to a different
  calendar.
- Human edits and deletions are preserved. A changed event is `edited`; a
  missing formerly reconciled event is `deleted`. Neither is recreated or
  rewritten automatically.
- Local EventKit readback proves only the local EventKit store. Remote Google,
  Outlook, or other provider synchronization requires separate exact-account
  evidence and is not inferred here.

## Dependency and acceptance decision

Use EventKit and Foundation as native platform APIs, plus Apple's SQLite library
through a small SwiftPM system module. Do not add a calendar SDK, parser, ORM, or
cross-platform writer. The SQLite wrapper is app-owned and is preferred over an
in-memory ledger or ad-hoc JSON because the write-ahead intent, unique identity,
and state transition need transactions across process interruption. Keep the
wrapper bounded to the delivery ledger; review schema migrations and permission
handling before packaging.

The user's instruction to continue locally actionable repository work permits
this implementation slice while Track 14 broader acceptance remains active.
It does not mark Track 14/15 accepted, unlock Track 17, prove a packaged host
app, or authorize requesting EventKit permission or writing to a real calendar.
Synthetic ledger tests and type checks are local evidence only. Native TCC,
selected-calendar readback, interrupted insert, human-edit/delete, accessibility,
and packaged-upgrade acceptance remain required before enabling delivery.
