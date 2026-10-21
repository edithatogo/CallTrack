# Review Report: Track5 extended emulator acceptance

## Summary

Review passes for the selected local Android matrix after runtime, fixture and evidence-handling fixes; original release/physical/cloud/operational gates remain open.

## Verification Checks

- [x] **Plan Compliance**: Complete for the selected API26/API33 matrix and affected API36 checks; partial for original full M4.
- [x] **Style Compliance**: Pass; Kotlin formatting, native Views/providers/SQLite retained, test-only adapters and explicit authority boundaries.
- [x] **New Tests**: Yes; rendered dimensions/labels/scroll/focus, real fixture round-trip with app write access denied, legacy state restoration, receipt isolation, guest ANR refusal and root-scope guards.
- [x] **Test Coverage**: Real emulator provider/UI/lifecycle/upgrade readback plus separate JVM and host/control tests; no physical/cloud/operational substitution.
- [x] **Test Results**:115 JVM/141 host tests;229 successful native invocations, two expected process terminations, four expected installation rejections,134 screenshot/tree pairs; no lint Error/Fatal findings.

## Findings

### [High — fixed] Newer SQLite helper interface broke scoped use on API26

- **Files**: `app/src/main/kotlin/com/markobl/calllogsync/slice/SqliteLedger.kt`, `SqliteImportStore.kt`.
- **Context**: Kotlin use compiled against the newer platform cast a helper to AutoCloseable. Android8's platform helper did not implement that interface. Recovery caught the failure and displayed an unavailable check; affected scoped-use paths could not run.
- **Applied fix**: Explicit Closeable implementation on both owned helpers. No schema, identity or public method removal. Native minimum-version store/UI/worker/import/lifecycle and preserving upgrades now pass.

### [Medium — fixed] Interactive checkboxes exposed32dp targets

- **File**: `app/src/main/kotlin/com/markobl/calllogsync/slice/ActivityInsets.kt`.
- **Context**: Native rendered measurements failed the48dp target requirement.
- **Applied fix**: Preserve larger existing dimensions and enforce48dp minima for initial buttons, checkboxes, spinners and text inputs. Four-screen normal/large-text checks and actual Tab focus pass on all selected APIs. Existing behavioural UI cases continue to pass.

### [Medium — fixed] Test infrastructure assumed newer platform contracts

- **Files**: Historical import/ledger test fixtures, `RenderedUi.java`, `FixtureCallLog.java`, `scripts/parity_environment.py`, `scripts/qualify_parity_lifecycle.py`.
- **Context**: DROP COLUMN, permission-dialog ownership, shell delegation and network-command output differed on the added OS versions. CLI success could be silent; unsupported-command text could be on stderr.
- **Applied fix**: Portable preserving fixture tables; exact version-gated dialog owners; SDK-selected network commands; bounded test-only userdebug fixture routing; provider readback for silent deletion. Fixture root is restricted to an exact owned API26 emulator, restored after observed boot recovery and withdrawn at final cleanup. The real round-trip confirms the app's write permission remains denied. These changes introduce no app permission, runtime dependency or production root requirement.

### [Low — fixed] Shared UI receipt paths and guest ANR obscured interpretation

- **File**: `scripts/qualify_parity_ui.py`.
- **Context**: Multiple version runs needed isolated receipts. A guest System UI ANR prevented screen discovery while boot/phone checks passed.
- **Applied fix**: Optional new per-run receipt directories refuse overwrite; focused ANRs are rejected before installation. The owned guest was repaired and requalified. Native failure receipts, scoped cleanup recovery and original snapshots remain retained rather than overwritten or counted as successful runs.

## Final evidence and limits

Standard suites match the same production input hashes and application APK.
Upgrade probes start from that baseline and track their separately hashed
version11 overlays. Each keeps its exact test-APK/build binding; variants are explicit.
Review includes source diffs, native assertion/cleanup paths, actual XML/PNG hash
readback and original failed receipts. Final provider/package/environment checks
pass; temporary API26 ADB privilege is restored and only newly owned guests stop.

Manual speech/contrast, physical/OEM/ABI, remote delivery, real production signing/
upgrade baselines and actual observation days remain separate. The prior unsigned
release is frozen at9464408; runtime fixes require refreezing/reproducing the next
candidate. No full-M4 completion or archive is recommended.

Final review confines privileged fixture scripts to protected internal app cache.
The real API26 round-trip/worker suite is requalified after this hardening; the
application APK remains identical. Ordinary ADB privilege, provider/package state
and guest shutdown are verified again after that final regression.
