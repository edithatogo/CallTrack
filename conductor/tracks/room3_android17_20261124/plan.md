# Room 3 migration — executable work plan

- [x] Verify latest stable Room 3.0.3 and its KSP/coroutine/SQLiteDriver boundary against official AndroidX documentation.
- [x] Record the migration constraints and acceptance criteria in [spec](spec.md).
- [x] Add source-bound synthetic schema-6/schema-2 Room readback/reopen regression and rewire Android 17 settings acceptance to v1–v5 historical ledger and import-v1 fixtures. Native execution remains pending; the new tests have not been marked as passed.
- [x] Add KSP 2.3.14 / Room 3.0.3 / SQLite driver 2.7.1 and independently verify 154 new dependency artifact hashes against canonical repository bytes; bot admitted exact metadata and Room schema JSON in commit 2d56f2c. Isolated compile, JVM and unsigned release build passed; strict ordinary CI on the accepted tree remains pending.
- [ ] Implement Room entities and suspend DAOs for import-session store using its original DB filename and a preserving schema strategy. No second writer, no data clears, no parallel legacy helper.
- [ ] Test Room versus pre-change implementation on synthetic fixture database snapshots, transactions, concurrent workers, cancellation and process termination. Remove superseded import-store helpers only after equivalence.
- [ ] Repeat the above for ledger v1–v6, with source namespace, durable intent-before-calendar-insert and unknown-write recovery.
- [ ] Run all app unit/lint/release tests and local Android 17 native provider/UI tests, including a real reinstall/upgrade with retained private DB files.
- [ ] Record exact PR commit, ABI/API37, SHA256, test receipts and migration decision in one track checkpoint and close the old implementation.
- [ ] If KSP/AGP or Room 3 incompatibility blocks build, retain working legacy stores and report the compatibility evidence instead of merging unverified persistence changes.

## Preserving migration corrections (2026-11-24)

- Preserve the caller-injected `LongSupplier` during v1–v5 ledger migration, rather than substituting wall time; synthetic clock/retention tests depend on this contract.
- Room support-wrapper temporarily owns the original validated transaction logic; do not claim suspend-DAO conversion until confirmed parity and code removal. No destructive fallback.
- The official v6→7/v2→3 migration receives existing DBs without deleting their tables. Verify both on API37, including uncertain calendar write and re-open.
