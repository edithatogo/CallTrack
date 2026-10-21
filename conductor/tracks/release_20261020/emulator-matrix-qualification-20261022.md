# Extended Android emulator acceptance — 2026-10-22

The selected minimum/intermediate Android matrix passes, with current-platform
recovery/accessibility regression checks. See [machine evidence](emulator-matrix-evidence-20261022.json)
and [review](review-emulator-matrix-20261022.md). Track5 remains active.

## Qualified scope

| Android / API | Successful native invocations | Accepted cases |
|---|---:|---|
| Android8 / API26 — minimum | 109 | Store/schema preservation, real LOCAL calendar delivery, worker filtering/duplicate prevention/permission loss, identity faults, wake budget, rendered settings/import/recovery, accessibility at1.0/2.0 font scale, process termination/reopen, guest reboot, offline/Doze, actual emulator RINGING/IDLE and missed-call logging, development10→11 upgrades |
| Android13 / API33 — intermediate | 109 | Same selected cases, using native shell-permission delegation for fixtures |
| Android16 / API36 — current regression | 11 | Four-screen accessibility at both font scales and rendered recovery, including human edits/deletions, permission loss, explicit reapproval/suppression and retention |

The229 successful invocations include30 upgrade-fixture invocations. Two deliberate
process terminations are recorded separately. Four expected package-install
rejections cover downgrade and wrong-certificate attempts. These are invocations
across versions, not229 unique test methods. All134 retained screenshots and their
associated accessibility trees pass hash readback.

The115 JVM fixtures pass with zero failures/errors/skips;141 host/control tests
pass. Android lint reports23 warnings,2 hints and zero Error/Fatal findings.
The historical full API36 matrix retains its original source/APK binding; it is
not silently relabelled as a new full-current-platform run.

All199 standard-suite invocations share unchanged production inputs and this
exact development APK SHA256:
`983892c6ad011c229baa9d82349fd4cfaada7fe945bac7f4d4196c25dddb17f1`.
Production changes are committed at6f3381c. Test-only fixtures and host harnesses
were refined during qualification; each receipt retains its exact build-input and
test-APK hashes. The common application APK remained identical throughout those
test-only refinements. Upgrade probes begin with that baseline and install their
separately hashed version11 release-mode overlay artifacts. Build receipts are
retained separately for each variant.

## Runtime and test fixes

1. **API26 SQLite scoped-use failure.** A debugger stopped at a caught
   ClassCastException in recovery's Kotlin `use` call. Android8's
   [SQLiteOpenHelper declaration](https://android.googlesource.com/platform/frameworks/base/+/refs/tags/android-8.0.0_r1/core/java/android/database/sqlite/SQLiteOpenHelper.java)
   lacks AutoCloseable; the [Android10 declaration](https://android.googlesource.com/platform/frameworks/base/+/refs/tags/android-10.0.0_r1/core/java/android/database/sqlite/SQLiteOpenHelper.java)
   implements it. The newer compile SDK selected a cast that failed on Android8.
   Both owned helpers now explicitly implement Closeable, preserving ledger6,
   import2, identities, receipt ownership and existing closing behaviour. Actual
   API26 recovery, worker, import, reboot and upgrade paths pass after the fix.
2. **Touch targets.** The first accessibility run found32dp checkboxes. Initial
   interactive controls now retain at least48dp height/width, following
   [Android accessibility guidance](https://developer.android.com/guide/topics/ui/accessibility/apps).
   Rendered checks cover labels, dimensions, scroll reachability and actual Tab
   input exposing visible application focus on settings, manual, import and
   recovery screens. Directly requesting focus while still in touch mode was a
   test error; the harness now injects the keyboard input it intends to qualify.
3. **Historical test tables.** Older SQLite rejected fixture-only DROP COLUMN
   statements. Fixtures reconstruct the same historical table shapes and preserve
   their data, primary keys and index before exercising the real migration.
   Production migration SQL is unchanged. Source-pinned historical-schema upgrade
   fixtures also pass on both added Android versions.
4. **Platform test contracts.** Tests recognise exact legacy permission-dialog
   owners on old Android, use the verified SDK to select old network commands and
   preserve battery snapshots with or without a Dock field. UI-only checks restore
   display state without requiring unsupported network inspection commands.

## Fixture privilege and recovery

API26's shell account does not request call-log permission, and
[UiAutomation shell-permission delegation](https://developer.android.com/reference/android/app/UiAutomation)
is unavailable before API29. The test-only adapter uses the official userdebug
image's existing `su` tool while explicit temporary ADB root mode is selected.
It operates through the public call-log provider, with token-owned rows, quoted
arguments, bounded output, real inserted-ID readback and post-delete readback.
The old content CLI is silent on successful deletion; a missing confirmation
string is never treated as cleanup proof.

A real provider round-trip verifies apostrophe/space handling and proves the app's
call-log write permission remains denied before, during and after legacy fixture
provisioning. No application permission or runtime dependency is added. Ordinary
API26 UI and upgrade acceptance preceded this fixture mode. After a real guest
reboot, the host restores fixture privilege only after observing the background
event/status and revalidating the exact API26 emulator. Foreign and modern targets
are refused by the root-recovery guard.

API26 and API33 both pass actual process termination between calendar insertion
and receipt publication, preserving one effect after reopen. Reboot recovery is
observed before verifier instrumentation. Offline delivery,40-second deferred
Doze observation and subsequent recovery pass, with power/network/display state
restored against its saved snapshot. These observation windows are test budgets,
not delivery-time promises.

Both10→11 release-mode probes use the existing development certificate, source-
pinned schemas1–5→6/import1→2, actual LOCAL calendar readback, preservation of human
edits/deletions and interrupted recovery. Downgrade and different-certificate
installations are rejected; the fixture key is removed. Permanent signing and
real production-baseline upgrade qualification retain their gates.

## Environment, evidence and cleanup

Official Google APIs ARM64 images26/revision3 and33/revision17 were installed using
already accepted SDK licences. Dedicated identities are
`calltrack-release-api26-20261022` on5578 and
`calltrack-release-api33-20261022` on5584. Existing API36 guests remain separate.
The first API33 rendered run was blocked by a System UI ANR. Its window/screenshot
and failed receipt are retained; cleanup was verified before relaunching only the
owned guest with4GiB/four CPUs. Core probes initially used2GiB/two CPUs. API26 used
2GiB/two CPUs. Qualified OS fingerprints and configuration changes are retained.
No computer restart occurred. The UI harness now refuses a focused ANR before
installing fixtures; new per-run receipt directories refuse reuse.

Original failed, cancelled, source-preflight and diagnostic runs remain distinct
from accepted cases. A failed native cleanup confirmation is preserved alongside
its later token-scoped external provider readback; it is not rewritten as success.
Final checks verify fixture rows absent, app/test packages removed and original
settings restored. API26's ADB UID2000 and root flag0 are restored after fixture
work. Both newly owned guests are stopped. Earlier guest processes/storage remain
untouched. Raw receipts/captures are excluded under
`.local/release-evidence/emulator-matrix-20261022/`; screenshot files remain in
UUID-specific `.local/slice-evidence/ui-*` directories.

## Remaining acceptance

The selected portrait ARM64 emulator matrix does not establish physical/OEM/32-bit
coverage, manual screen-reader speech or contrast acceptance, remote calendar
synchronisation, production signing or seven actual observation days. No operational
day is claimed. The permanent identity/custodian decision remains pending.

The independent unsigned release/source bundle is still bound to9464408. The
current runtime fixes require a new release freeze, corresponding-source bundle
and independent reproduction before that newer candidate can inherit release
qualification. Existing evidence is preserved at its original revision; the
prepared hosted workflow remains a separate unexecuted gate.

Final review confines privileged fixture scripts to protected internal app cache.
The real API26 round-trip/worker suite is requalified after this hardening; the
application APK remains identical. Ordinary ADB privilege, provider/package state
and guest shutdown are verified again after that final regression.
