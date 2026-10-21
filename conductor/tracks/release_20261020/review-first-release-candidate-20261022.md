# Review Report: Track5 first-release baseline and final local candidate

## Summary

The local unsigned candidate and development-lineage baseline pass after fixes;
production identity/signing, physical/cloud and operational gates remain open.

## Verification Checks

- [x] **Plan Compliance**: Partial overall M4; the requested local baseline, freeze, reproduction, native acceptance and review are complete.
- [x] **Style Compliance**: Pass; general/Kotlin guidance retained, changed Java fixture uses pinned AOSP formatter; no production framework change.
- [x] **New Tests**: Yes; native fresh-state assertions and host artifact/certificate-payload/cleanup fault checks.
- [x] **Test Coverage**: Partial release scope; exact release payload fresh launch plus development10→11 historical/provider preservation on26/33/36,60 successful invocations and6 expected install rejections; broader previous matrix retains its own scope.
- [x] **Test Results**: Passed;115 JVM tests per release build and debug build,144 host tests on Mac/Linux, release lint with zero Error/Fatal,3 rendered screenshot/tree pairs and verified cleanup. Source archive and actual copied Linux APK hashes were checked.

## Findings

### Medium — provider cleanup failure could remove the recovery packages — fixed

- **File**: `scripts/qualify_release_upgrade.py`, cleanup/finally paths.
- **Context**: The old finally block uninstalled packages after unverified provider cleanup, potentially losing fixture state needed for recovery. This is test-environment integrity, not a production app data change.
- **Suggestion applied**: a8c7f9f retains packages, fails qualification and preserves evidence when provider cleanup is unknown. The regression test asserts no uninstall/readback command runs in that fault branch. Successful native cleanup passes on all three APIs.

```diff
+ if not provider_cleanup_verified:
+     return ["Provider cleanup unverified; owned packages retained for recovery"]
```

### Low — fresh-install test confused maintenance storage with delivery state — fixed

- **File**: `app/src/androidTest/java/com/markobl/calllogsync/slice/KotlinPackageUpdateTest.java` and `first-release-baseline.md`.
- **Context**: API26 passed the pre-launch check, then correctly created an empty ledger during onResume retention maintenance. The new no-file assertion was stricter than the product requirement. The failure and confirmed package cleanup are retained.
- **Suggestion applied**:15998ba permits empty storage and checks receipt/outbox/scan and import tables for zero rows, with all relevant permissions denied and activation/destination unset. Preflight passes before/after launch on26/33/36; the final frozen-candidate runs repeat that acceptance.

```diff
- assertFalse(context().getDatabasePath("calltrack-ledger.db").exists());
+ // If present, query receipts/outbox/scan_cursors and require zero rows.
```

### Low — one-off rendered capture ran after instrumentation teardown — fixed

- **File**: private `rendered_install.py` evidence runner.
- **Context**: Instrumentation completion closes its target activity; the first capture therefore showed the system launcher despite successful app launch. No permission or app-state failure occurred; packages were removed.
- **Suggestion applied**: Capture actual app window/tree/PNG before the next instrumentation teardown, then verify empty state and reopen. All three final runs pass. The initial probe is excluded from the60 accepted invocations and retained separately.

## Disposition

No unresolved finding blocks the stated local scope. The frozen source is15998ba;
later evidence/document commits do not relabel its artifacts. Historical research
and older qualification receipts remain intact. No independent human/agent review
or hosted CI is claimed. Retain all original release gates and keep M4 active.
