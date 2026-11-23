# Room 3 migration — executable work plan

- [x] Verify latest stable Room 3.0.3 and its KSP/coroutine/SQLiteDriver boundary against official AndroidX documentation.
- [x] Record the migration constraints and acceptance criteria in [spec](spec.md).
- [ ] Write *failing* regression cases for pre-Room legacy import schema versions 1/2 with all saved sessions and candidate outcomes preserved; verify original fixture works.
- [ ] Apply verified KSP plugin + Room 3.0.3 runtime/compiler to the **native Android** module; run strict dependency verification and compile. Pin new metadata only after canonical artifact/hash audit.
- [ ] Implement Room entities and suspend DAOs for import-session store using its original DB filename and a preserving schema strategy. No second writer, no data clears, no parallel legacy helper.
- [ ] Test Room versus pre-change implementation on synthetic fixture database snapshots, transactions, concurrent workers, cancellation and process termination. Remove superseded import-store helpers only after equivalence.
- [ ] Repeat the above for ledger v1–v6, with source namespace, durable intent-before-calendar-insert and unknown-write recovery.
- [ ] Run all app unit/lint/release tests and local Android 17 native provider/UI tests, including a real reinstall/upgrade with retained private DB files.
- [ ] Record exact PR commit, ABI/API37, SHA256, test receipts and migration decision in one track checkpoint and close the old implementation.
- [ ] If KSP/AGP or Room 3 incompatibility blocks build, retain working legacy stores and report the compatibility evidence instead of merging unverified persistence changes.
