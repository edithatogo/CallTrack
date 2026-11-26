# Room 3 migration

[Specification](spec.md) · [GitHub issue #115](https://github.com/edithatogo/CallTrack/issues/115)

## Preserving migration

- [x] Use Room 3 and AndroidSQLiteDriver for both existing database files. Ledger schema 7 and import schema 3 preserve legacy ledger v1–v6 and import v1–v2.
- [x] Export both Room schemas and independently match 154 added dependency checksums against canonical bytes. Keep strict Gradle verification.
- [x] Add historical schema, injected-clock, rollback and retry, newer schema preservation, and uncertain-write coverage. The release-mode development-key upgrade uses synthetic data and a dedicated Android 17 emulator.

The initial migration used the compatibility bridge to retain the synchronous SQL transactions. [Initial migration and qualification](https://github.com/edithatogo/CallTrack/pull/121).

## Suspend DAO adoption

- [x] Move the import and ledger queries to suspend DAOs while retaining transaction boundaries, namespace and cancellation behavior.
- [x] Repeat historical-file upgrade, concurrent delivery, process death, permission revocation, unknown-write recovery, and edit/deletion protection against the DAO implementation.
- [x] Retire the compatibility adapter and its Gradle dependency after migration and recovery parity. Gradle dependency insight confirmed its removal from the app runtime.

## Preserving migration corrections (2026-11-24)

- Preserve the caller-injected `LongSupplier` during v1–v5 ledger migration, rather than substituting wall time; synthetic clock/retention tests depend on this contract.
- The initial Room migration used the SupportSQLite wrapper for the established SQL transactions. The latest upgrade probes also test rollback, retry and preservation of an unsupported newer schema.
- The 6→7 and 2→3 migrations leave existing tables in place. The API 37 qualification includes reopening both databases and recovering uncertain calendar writes.

## Android 17 qualification

The tests ran on an Android 17 API 37 arm64 emulator with a 16 KiB page size. Both Room schema exports matched the previous build. [Source commit](https://github.com/edithatogo/CallTrack/commit/fdaab35ff65a3554362e0fac1faf6600ec7b990a).

- `run_slice_build.py` passed JVM tests, debug packaging and Android lint. `qualify_parity_emulator.py --suite store` passed 39 checks, including historical migration, candidate/progress rollback, outbox durability, expiry and checkpoint ordering.
- `qualify_parity_provider.py` passed 9 checks. Overlapping manual, automatic and import delivery through three Room handles produced one calendar event. Provider verification covered uncertain writes, permission revocation, edits and deleted destinations.
- `qualify_parity_ui.py` passed settings (18), imports (11), recovery (9) and accessibility (2). `qualify_parity_lifecycle.py` verified process interruption, reboot, offline and Doze recovery against the local Calendar provider.
- `qualify_release_upgrade.py --provider-schema` passed 15 instrumentation checks, including historical schema upgrade, rollback and retry, newer-schema preservation, recovery, and incorrect-signer and version downgrade rejection. The development-key receipt SHA-256 is `c9be0266781ee596bb4d05c1066c921033f3103b87648b2ba0fbd8aebf28caad`. [Release acceptance gates](../release_20261020/verification.md#active-release-gates).
