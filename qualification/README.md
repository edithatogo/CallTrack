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
python3 scripts/run_slice_build.py --jdk /opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home --sdk .local/android-sdk
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

### Local JVM fallback while qualified storage is unavailable

`scripts/run_parity_local.py` uses the existing `.local/counter-jvm` library manifest,
classpath and last successful build receipt. It verifies library hashes and unchanged
Kotlin source hashes, compiles current pure Java sources/tests into a fresh temporary
directory and writes `.local/counter-jvm/latest-local-receipt.json`. It requires the
locally retained compiled Kotlin/dependency classes and JDK17. It is an explicitly
bounded development fallback, not a clean build or a portable qualification route.
It does not change SDK/cache symlinks or install an alternative Android toolchain.

Run `python3 scripts/run_parity_local.py` and `python3 -m unittest discover -s tests`.
The fallback excludes SchedulingCommitTest, Android adapters and instrumentation.
Its successful receipt must never authorise an APK install; native harnesses continue
to require the full source-bound Android build. On storage restoration, use the normal
build and native commands above before claiming Android acceptance.

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
