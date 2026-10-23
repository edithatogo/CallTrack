# Build and device qualification

The source baseline is AndroidCallLogSync commit
`aaa56443a294aa61bad48b620427e53f9521c5fe`. The reference checkout stays unchanged.
The modern qualification copy changes only build infrastructure/identity and adds
zero-row permission tests. See the build track's findings and decisions through the
[track registry](../conductor/tracks.md) or [archive](../conductor/archive/index.md).

## Qualified toolchain

| Variant | JDK | Gradle | AGP | compile/target | Build tools |
|---|---|---|---|---|---|
| Upstream reference | 11 | 7.3.3 | 7.2.2 | 32/32 | 30.0.3 |
| Modern development candidate | 17 | 8.13 | 8.13.2 | 36/36 | 35.0.0 |

SDK command-line tools19.0 were used with JDK17 to install these packages. Set
`ANDROID_HOME` to an SDK you maintain, and `JAVA_HOME` to the required JDK. The
local SDK currently lives on PortableSSD with a symlink under `.local/android-sdk`;
this storage layout is not required by the project. If the volume is disconnected,
use another qualified SDK location. Google SDK licence acceptance is a local
prerequisite; tools and keys are not committed.

## Build

Run from repository root with Python3.12+ and Git installed:

```sh
python3 scripts/bootstrap_qualification.py
python3 scripts/prepare_qualification.py --modern
python3 scripts/run_qualification.py --variant modern --jdk "$JAVA_HOME" --sdk "$ANDROID_HOME" --clean
python3 -m unittest discover -s tests -v
python3 scripts/verify_conductor.py
```

Preparation deliberately refuses to overwrite an existing copy. Reuse that copy
for subsequent builds; if intentionally regenerating it, preserve its evidence and
move it aside first. For the original build, use `--variant upstream` with JDK11
and SDK32/build-tools30.0.3 installed. Original source's unit-test task is NO-SOURCE;
build success is not behavioural coverage. Build/lint receipts and APK hashes are
written to `.local/build-evidence` and nonzero Gradle status propagates to the shell.

The modern candidate also aligns Fragment1.5.1 and ViewModel KTX2.5.1 to restore
legacy lint compatibility and avoid duplicate classes. These are interim repairs
whose long-term replacement is owned by later dependency/UI tracks.

## Device checks

Use only a device authorised for this project. The harness refuses to replace
pre-existing qualification packages and checks APK identities and digests against a successful clean-build receipt. It installs its
own app/test packages, launches the app, verifies a denied zero-row provider query,
grants READ_CALL_LOG through development ADB, verifies a zero-row query, revokes
permission and verifies denial again. Sync remains disabled and no endpoint is
configured. It uninstalls only its newly installed packages and verifies absence.
No call data, contacts or calendar records are collected. The ADB path does not
establish normal user-consent UX or store eligibility.

```sh
python3 scripts/qualify_device.py --serial YOUR_AUTHORISED_DEVICE_SERIAL
```

The harness uses the project's local SDK build-tools35.0.0/aapt for identity checks.
Any failed stage stops qualification; inspect the local receipt, including cleanup
status. Device access is optional and is not part of the build-only CI job.

## Formatting and CI

New qualification Java uses google-java-format1.24.0 in AOSP mode (four-space
indentation), JDK17. It is development tooling, not an app dependency. The verified
formatter SHA256 is `812f805f58112460edf01bf202a8e61d0fd1f35c0d4fabd54220640776ec57a1`.
Download it from its published Maven Central coordinate and verify that checksum.
With the JAR at `.local/toolchain/google-java-format-1.24.0.jar`:

```sh
java -jar .local/toolchain/google-java-format-1.24.0.jar --aosp --dry-run --set-exit-if-changed qualification/src/androidTest/java/com/markobl/calllogsync/PermissionQualificationTest.java
```

Upstream source is not mass-reformatted. Android lint is the static-analysis gate;
its warning inventory and skipped legacy custom checks must be reviewed before
production. The pinned GitHub Actions matrix is prepared locally; a hosted CI run
is not claimed until actually executed. It fetches the audited public upstream,
never the private CallTrack APK, and does not install onto a physical phone.

## Signing and support boundary

Only local debug signing is qualified. Do not publish the debug keystore or use it
as a release key. Public package name, release key custody, signing lineage and
store approval belong to the release track. Successful target36 tests on Android17
establish a bounded development baseline, not target37/API37 compilation support,
actual call logging, background reliability or calendar parity. Before wider
release, test the chosen minimum supported OS and real workflows as later tracks
specify.


## Track4 dedicated emulator acceptance

Build exact root debug inputs first, then run one harness at a time. These require
absent development/test packages and never replace an existing installation. The
provider and worker harnesses verify the dedicated CallTrack AVD, source/APK hashes,
fixture cleanup and package absence. They preserve cleanup code if external fixture
removal cannot be verified. Inspect the saved receipt before retrying a failure.

```sh
python3 scripts/run_slice_build.py --jdk "$JAVA_HOME" --sdk .local/android-sdk
python3 scripts/qualify_parity_emulator.py --serial emulator-5580
python3 scripts/qualify_parity_provider.py --serial emulator-5580
python3 scripts/qualify_parity_worker.py --serial emulator-5580
```

Use the currently verified dedicated emulator serial if it changes. The store suite
grants no provider permissions. The provider suite uses synthetic source objects
and real LOCAL calendar effects. The worker suite additionally requires an initially
empty emulator call provider, seeds exact tagged synthetic rows through test-only
shell identity and grants/revokes app READ_CALL_LOG for positive/revoked branches.
It never uses a physical phone. Receipts are in .local/parity-evidence and
.local/slice-evidence; committed Track4 checkpoint evidence records the exact scope.
These commands do not qualify cloud sync, physical calls, rendered UI, reboot or
Doze/offline acceptance. Host fault fixtures validate cleanup control flow separately.


The lifecycle harness adds ordinary launcher resume, explicit force-stop/reopen,
actual process termination between provider insertion and ID persistence, a guest
reboot, local offline delivery and deferred Doze recovery:

```sh
python3 scripts/qualify_parity_lifecycle.py --serial emulator-5580
```

It requires a qualified initial emulator power/network snapshot and refuses existing
battery overrides. It changes only the dedicated emulator's airplane/Wi-Fi/mobile-data,
stay-awake, simulated battery and idle/screen state. It checks restoration against
the snapshot in finally, allowing bounded asynchronous radio readback to settle.
A boot observation may take180 seconds after boot completion; this is a test budget,
not a delivery guarantee. Never restart the emulator or another harness because an
observation call yielded: poll the live process first. On failure, inspect
.local/slice-evidence/parity-lifecycle-receipt.json for separate fixture, package and
emulator-restoration results. If cleanup is unverified the test APK is deliberately
retained; do not replace it or clear app data before using its saved fixture token
for scoped recovery. The receipt records the original emulator settings and exact
failed command/state. These cases do not qualify physical calls, cloud delivery,
rendered UI acceptance, every OEM/version or automatic execution while force-stopped.

### Retired retained-class JVM fallback

The old `scripts/run_parity_local.py` now fails closed with an actionable message.
Expanded Kotlin migration cannot be qualified by compiling only remaining Java
sources against retained implementation classes. Historical local receipts remain
evidence of their original subset, never current Android acceptance. Use
`scripts/run_slice_build.py` with the qualified JDK17 and SDK. If PortableSSD is
unavailable, reconnect it or explicitly qualify another SDK/cache route; do not
claim current tests from old compiled classes. No symlink/cache replacement is
performed by the retired command.

### Development package replacement fixture

`python3 scripts/qualify_kotlin_update.py --serial emulator-5582` verifies a
same-version, same-debug-key development replacement with private synthetic state.
It requires absent packages, a verified dedicated guest, a retained Java baseline
source/APK receipt at commitc7c3d91 and a current source-bound app/test build. It
seeds paused activation/namespace/destination and READY/IN_FLIGHT/REVIEW/CONFIRMED
receipts, replaces the app using `adb install -r`, reads the state through candidate
code and verifies package removal. No call/calendar permissions or external effects
are used. Receipts are uniquely named under .local/kotlin-evidence. This is not an
increasing-version production update, wrong-key rejection, provider/cloud readback
or signed release gate. The retained baseline is local; missing inputs must not be
reconstructed from unverified APKs.

### Isolated recovery AVD without a host restart

Track4 also permits the exact dedicated identity
`calltrack-parity-api36-recovery-20261021`, created from the same API36 Google APIs
ARM64 image with fresh writable storage under `.local/emulator-avds/`. It is
independent of `calltrack-parity-api36-20261020`; never launch another instance
against the old AVD while its stuck process remains. The recovery guest uses console
port5582 and ADB serial `emulator-5582`, within the emulator's documented port range.
No physical phone or arbitrary emulator becomes authorized through this exception.

The four Track4 harnesses share an exact AVD allowlist in `scripts/parity_targets.py`.
They still require qemu identity, completed boot, source-bound build/APK hashes and
absent test packages. Pass `--serial emulator-5582` only after guest health is read
back. Launch uses `ANDROID_AVD_HOME=$PWD/.local/emulator-avds`, `-no-snapshot` and
fresh writable files, with the original guest/storage left untouched. Recovery
identity/config evidence is saved in `.local/parity-evidence/recovery-avd-identity.json`.

For the isolated source-identity preferences fault suite, use
`python3 scripts/qualify_parity_emulator.py --serial emulator-5582 --suite source-identity`.
It runs three methods in separate instrumentation processes and writes
`.local/parity-evidence/source-identity-device-receipt.json`; the default store suite
continues to write its own store receipt. False/thrown identity outcomes deliberately
latch for the app process, so this suite must not be collapsed into one shared process.

### Protected emulator call-end acceptance

Run `python3 scripts/qualify_parity_telephony.py --serial emulator-5582` only on the
qualified dedicated guest with matching build/APKs and absent fixture packages.
It requires an empty source provider and idle Android radio, creates one fictional
incoming call, observes actual Android RINGING/IDLE and a background event/status
before verifier startup, then verifies receipt identity and duplicate prevention.
The modem simulator's console `gsm list` can omit a live Android call; it is not an
idle or cleanup proof. Actual telephony registry state and exact source-row readback
are gates. Failure cleanup cancels only the known fixture number, waits for its
processed row and preserves cleanup instrumentation if termination/cleanup is unknown.
The receipt is `.local/slice-evidence/parity-telephony-receipt.json`. Physical phones,
existing calls and unrelated AVDs are refused; this tests no outgoing real calls.

### Rendered settings acceptance

Run `python3 scripts/qualify_parity_ui.py --serial emulator-5582` against the matching
build and dedicated guest. Native tests use visible accessibility selectors and
injected touch events, wait for permission-dialog progression, and verify committed
settings/provider effects. This is separate from constructing or testing controllers.
The suite currently covers settings/permission/filter/confirmation/pause/reopen and
restoration plus top-content geometry on all four Views screens. Positive import and
recovery actions remain additional work.

PNG screenshots and accessibility trees are written to private fixture files, then
pulled to `.local/slice-evidence/ui-<fixture-token>/` before uninstall, with hashes in
`parity-ui-receipt.json`. Names are unique across instrumentation processes. Missing
or invalid screenshots preserve installed evidence/cleanup code instead of reporting
success. The harness snapshots/restores the dedicated emulator environment and
verifies token-owned calendar cleanup and both package removals. No physical phone,
real caller data or cloud account is used. Native API36/day evidence does not establish
older-platform/night-mode runtime parity.


## Minimum/intermediate matrix — 2026-10-22

[Recorded acceptance](../conductor/tracks/release_20261020/emulator-matrix-qualification-20261022.md)
uses the exact additional AVDs `calltrack-release-api26-20261022` (5578) and
`calltrack-release-api33-20261022` (5584), official Google APIs ARM64 images. The
shared allowlist accepts exact owned names; an emulator prefix alone is insufficient.
Set JAVA_HOME to a qualified JDK17 before building; this run uses the isolated
Temurin17.0.20.1+1 recorded in the receipts, with PortableSSD's SDK available.

Build source-bound debug/test APKs once, keep them stable while native cases run,
and retain build and per-version receipts before another invocation overwrites a
default receipt. Core harnesses use fixed receipt files and must run serially.
UI runs may use unique new receipt directories:

```sh
python3 scripts/run_slice_build.py --jdk "$JAVA_HOME" --sdk .local/android-sdk
python3 scripts/qualify_parity_ui.py --serial emulator-5584 --suite accessibility --receipt-dir .local/matrix-new/api33-accessibility
python3 scripts/qualify_release_upgrade.py --serial emulator-5584 --provider-schema --output .local/matrix-new/api33-upgrade
```

Accessibility checks cover four rendered screens, labels,48dp targets, scroll
reachability and actual keyboard Tab focus. This qualification ran normal and2.0
font scales and restored their saved values. Manual speech/contrast and physical
acceptance remain separate. A focused guest ANR refuses installation; repair only
the owned guest and retain failure/cleanup evidence before retrying.

API26 call-log-seeded worker/import/lifecycle/telephony fixtures additionally need
the selected userdebug image's existing su tool and explicitly selected temporary
ADB root mode. Verify the exact AVD, qemu identity and API26 before selecting that
mode. The test-only adapter provisions tagged rows through SDK provider commands;
a real round-trip checks quoted data and proves app write permission stays denied.
No app permission or release dependency is added. Ordinary UI/provider/store and
upgrade tests do not need that fixture mode. Lifecycle reselects it after a real
guest reboot only after background delivery has already been observed.

After fixture cleanup is confirmed, restore ordinary ADB mode with the selected
guest's `unroot` command and verify UID2000/root flag0. Retain settings/row/package
readback and stop only the newly owned guests. The qualified run did this for both
new guests; no computer restart, physical-phone action or cloud acceptance occurred.
