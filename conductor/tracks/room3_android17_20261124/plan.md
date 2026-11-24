# Room 3 migration

[Specification](spec.md) · [GitHub issue #115](https://github.com/edithatogo/CallTrack/issues/115)

## Preserving migration

- [x] Use Room 3 and AndroidSQLiteDriver for both existing database files. Ledger schema 7 and import schema 3 preserve legacy ledger v1–v6 and import v1–v2.
- [x] Export both Room schemas and independently match 154 added dependency checksums against canonical bytes. Keep strict Gradle verification.
- [x] Add historical schema, injected-clock, rollback and retry, newer schema preservation, and uncertain-write coverage. The release-mode development-key upgrade uses synthetic data and a dedicated Android 17 emulator.

The compatibility bridge retains the proven synchronous SQL transactions during the migration. Use [PR #121](https://github.com/edithatogo/CallTrack/pull/121) for the exact build, native tests and dependency evidence.

## Remaining DAO work

- [ ] Move the import and ledger queries to suspend DAOs while retaining transaction boundaries, namespace and cancellation behavior.
- [ ] Repeat historical-file upgrade, concurrent delivery, process death, permission revocation, unknown-write recovery, and edit/deletion protection against the DAO implementation.
- [ ] Remove the compatibility bridge after parity. Close Track 22 with the exact source and test receipts.

## Preserving migration corrections (2026-11-24)

- Preserve the caller-injected `LongSupplier` during v1–v5 ledger migration, rather than substituting wall time; synthetic clock/retention tests depend on this contract.
- Room support-wrapper temporarily owns the original validated transaction logic; do not claim suspend-DAO conversion until confirmed parity and code removal. No destructive fallback.
- The official v6→7/v2→3 migration receives existing DBs without deleting their tables. Verify both on API37, including uncertain calendar write and re-open.
