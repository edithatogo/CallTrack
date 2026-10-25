# Track 16 decisions — 2026-10-24

## D16-01: Native system APIs and a preview-only import prototype

**Status:** bounded prototype; no production adoption.

Use Foundation `JSONDecoder` for the first isolated import contract and retain
AppKit/SwiftUI file selection plus EventKit as the candidate native integration
route. No third-party parser, persistence, calendar, UI, or FFI dependency is
introduced. This keeps prototype licensing/transitives at zero and avoids
duplicating destination authority in a cross-platform library.

The prototype schema is versioned and metadata-minimal, with byte/count bounds,
UUID source/call identities, direction, UTC epoch milliseconds, and duration.
It exists only to test safe parsing and preview contracts. The Android app does
not yet export this schema, so no compatibility claim is made. Promote the
format only after Android export/import golden fixtures, provenance/identity
rules, migration policy, hostile-input tests, and user-facing disclosure are
reviewed. Remove or revise it if the actual exporter contract differs.

Do not select a parser dependency unless real schema complexity or performance
evidence demonstrates a need. Do not implement EventKit delivery until durable
intent, receipt identity, exact readback, uncertainty recovery, edit/delete
preservation, destination-change behavior, and consent UX are reviewed. Native
permission and selected-calendar writes are not exercised by the prototype.

## D16-02: Do not advance downstream stages

The user requested Track 16 work while Track 14 and 15 remain incomplete. A
scoped prerequisite record permits only bounded discovery/prototype work. It
does not resolve either earlier stage, accept the KMP macOS target, or unlock
Track 17. Track 16 remains open until its original implementation/acceptance
criteria pass or a separately justified disposition is recorded.

## D16-03: Prototype durable delivery behind a native adapter

**Status:** local prototype only; no native calendar acceptance.

The user's instruction to continue remaining repository tasks permits a
bounded implementation prototype beyond the earlier parser-only checkpoint.
The prototype now records privacy-minimal delivery intents in a private SQLite
ledger, requires a separate explicit confirmation input before writes, and
uses EventKit only through an injected gateway. A failed or interrupted write
is reconciled by readback and never blindly replayed; exact-match receipts,
uncertain outcomes, conflicts, human edits and deletions have separate states.
The complete reviewed boundary is in
[the EventKit delivery contract](eventkit-delivery-contract-20261025.md).

This does not accept Track 14 or Track 15, unlock Track 17, or establish
compatibility with the Android export format. No app UI is wired to the adapter,
no TCC prompt or real calendar was used, and no packaged app declares the
required usage string. Native permission, selected-calendar write/readback,
restart recovery, accessibility and packaging remain acceptance work. Keep
the existing preview-only UI delivery-disabled until that work is completed.
