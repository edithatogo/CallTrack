# Track 19 decisions — 2026-10-24

## XPLAT-D19-01: Reconcile evidence only; do not adopt another runtime yet

**Status:** bounded prerequisite work; cross-platform acceptance pending.

Keep Android provider, ledger, scheduling and CalendarContract behavior under
the existing native app. Track 15's selection/range module is the only current
shared domain slice. Keep Track 16's parser and Track 17's desktop UI synthetic
and prototype-only. Do not add an interchange library or Rust component before
an Android producer and accepted format make a real compatibility test possible.

This choice avoids selecting dependencies against an unowned format and avoids
duplicating calendar writers or synchronization receipts. A synthetic schema-v1
producer/parser contract now exists as a bounded prototype under
`app/src/main/kotlin/com/markobl/calllogsync/slice/CallLogExport.kt` and the
Track 16 parser. It is not integrated into the Android product or a stable
interchange API. Reassess native versus library support after product use cases
and restore semantics are specified; record exact versions,
licences, transitives, maintenance, migration, rollback and removal criteria
before implementation. Track 18's Rust deferral remains valid until a real
cross-platform consumer and measurable end-to-end benefit exist.

The prototype checksum detects accidental corruption but is not authentication.
No portable receipts or destination authority cross the format boundary.
This bounded decision neither passes Track 19's package/provider/upgrade matrix
nor resolves Tracks 14–17. It does not unlock iOS.
