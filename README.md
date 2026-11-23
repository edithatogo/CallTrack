# Active development — Android 17 only

The single active product target is **native Android 17 (API 37)**: Kotlin, Jetpack Compose, system CallLog/CalendarContract and WorkManager. [Active architecture](conductor/architecture-android17.md), [scope](conductor/active-scope.json), and [Conductor ownership](conductor/tracks.md) supersede older roadmaps below. macOS, iOS, KMP, desktop and older Android APIs are deferred; historical evidence is preserved.

Local Android 17 emulator acceptance is the preferred path: `scripts/qualify_android17_local.sh emulator-5554` against the dedicated `calltrack-android17-local` AVD, after a source-bound build and with pre-commit hooks configured. Room 3.0.3 has been selected for a **preserving** migration; the existing SQLite stores continue to own data until tested replacement. This repo is not yet an approved production release.

---

# CallTrack community development

A GPL-3.0 development fork of AndroidCallLogSync, working toward a maintainable
CallTrack successor. See [source provenance](NOTICE.md) and [Conductor roadmap](conductor/roadmap.md).

The development app now supports automatic call-to-calendar logging: choose a
visible writable device calendar, grant call/calendar access, select incoming,
outgoing and missed calls independently, and enable logging from now. Pause stops
automatic insertion; calls during a pause require a separately previewed and
confirmed historical import. The explicit latest-call action remains available.

Events preserve exact source instants, caller presentation and actual duration,
with private/free defaults and no reminders. Zero-duration calls use an explained
one-minute visual marker. Durable receipts prevent blind retry after uncertain
insertion and preserve edited/deleted events. Settings, import progress and
explicit recovery/reapproval/suppression/retention controls are implemented.
Opening the app reconciles a previously enabled policy; granting permission alone
does not enable logging or approve insertion.

This remains a development app (`org.calltrack.community.dev`), not a public parity
release. [Track4 acceptance](conductor/archive/parity_20261020/final-review.md) passes
on the dedicated API36 emulator:110 JVM/73 host tests and89 successful native
invocations plus one expected process termination. Physical calls, cloud/provider
sync, wider Android/OEM/accessibility qualification, production signing and
operational observation remain pending. No INTERNET permission is present;
calendar account software controls remote sync.

## Build and verify

Use Temurin JDK 25.0.5.1+2, Android SDK platforms 36 and 37.1, Build Tools 36.0.0, the
included Gradle 9.7.0 wrapper and Kotlin/KGP 2.4.21. Set `JAVA_HOME` and
`ANDROID_HOME` to those installed toolchains, then:

Create the selected Python runtime from `.python-version` and verify the actual
interpreter before running these commands. The current preferred selection is
Python 3.15.2; choose another version only for a concrete compatibility or task need,
record the reason and scope, and qualify it as described in the
[runtime policy](conductor/python-runtime-policy.md).

```sh
python_version="$(cat .python-version)"
uv venv --python "$python_version" .venv
test "$(.venv/bin/python --version)" = "Python $python_version"
.venv/bin/python scripts/run_slice_build.py --jdk "$JAVA_HOME" --sdk "$ANDROID_HOME"
.venv/bin/python -m unittest discover -s tests -v
.venv/bin/python scripts/verify_conductor.py
```

The local release- and Kotlin-upgrade emulator checks use the qualified macOS ARM64
JDK bundle at `.local/toolchain/temurin-jdk25` and its original publisher archive,
`OpenJDK25U-jdk_aarch64_mac_hotspot_25.0.5.1_2.tar.gz`, in the same directory.
These may be links to the verified toolchain storage. Before using the JDK, the
checks compare the archive to its pinned digest and every installed bundle file
to that archive; other platforms are not yet qualified for these local checks.
The Kotlin check retains its frozen historical Java baseline and validates its
original receipt separately from the active application's current JDK.

The local toolchain audit additionally requires its selected roots to use the
repository aliases below. The aliases may link to verified SDK and Gradle cache
storage on another volume; `--output` and `--archives-from` are unchanged.

```sh
export JAVA_HOME="$PWD/.local/toolchain/temurin-jdk25/Contents/Home"
export ANDROID_HOME="$PWD/.local/android-sdk"
export GRADLE_USER_HOME="$PWD/.local/toolchain/gradle-home"
```

The build runs JVM tests, builds app/test APKs and lints. Receipts and logs stay under
excluded `.local/slice-evidence`. The app uses Compose controls and Kotlin shared
call-selection rules. The [live plan](conductor/index.md#current-checkpoint) links
each track's remaining work and evidence. The [tech stack](conductor/tech-stack.md)
documents retained dependencies and compatibility overrides.

For an explicitly selected, authorised test device with neither development test
package already installed:

```sh
.venv/bin/python scripts/qualify_slice_device.py --serial DEVICE_SERIAL
```

The harness installs disposable APKs, launches the UI, checks denied calendar
permissions, grants calendar access for a synthetic LOCAL calendar test, verifies
native event readback and retry after reopening SQLite, revokes access, and checks
cleanup. It never grants call-log access, reads personal calls, makes a call or
changes cloud account settings. It refuses APKs that differ from the successful
build receipt. Fixture recovery is scoped by per-run token and LOCAL account type.
If cleanup cannot be verified, packages remain installed to retain recovery code;
inspect the receipt and recover that exact fixture before uninstalling.

Normal user consent flows, actual call acquisition and remote Outlook/Google sync
require separate device acceptance before routine use. Device calendars exposed by
Outlook can use the same adapter, but app installation alone does not establish
calendar availability or remote sync.

## Development and recovery

Run new Java files through google-java-format 2.0.0 with `--aosp --cleanup=false`; the checksum and
download recipe are in CI. Kotlin uses ktfmt 0.65 with `--kotlinlang-style`;
CI checks both languages. The upstream build reproducer remains available through
[qualification instructions](qualification/README.md). CI definitions are local
configuration until a hosted run is observed.

Ledger schema6 preserves earlier receipts and durable delivery state; import sessions
use schema2. Unsupported upgrades fail instead of dropping data. Manual, automatic
and confirmed historical delivery share durable ownership/receipts and conservative
recovery. Receipt identity includes the source namespace, call row ID and destination.
Do not clear app data as a retry strategy: losing receipts changes recovery and
identity semantics. Cross-device restore is not implemented. Keep working artifacts
and source history until preserving migration and recovery have passed.

## Community preparation

See [contribution guide](CONTRIBUTING.md), [data lifecycle](PRIVACY.md),
[security status](SECURITY.md) and [release runbook](RELEASING.md). These prepare
future community distribution; they do not establish a release candidate or public
reporting channel. Physical/cloud/operational acceptance and signing/distribution
gates remain open; Track4 selected-emulator acceptance is archived.

The app acceptance suite covers the retired standalone Compose experiment's
launcher cases. The [Compose research archive](conductor/archive/compose_20261020/index.md)
retains the original experiment and its evidence.

## Parity acceptance and recovery

Historical parity evidence is in the [Track4 handoff](conductor/archive/parity_20261020/handoff.md).
The [current Kotlin handoff](conductor/tracks/kotlin_20261020/active-migration-handoff.md)
and [qualified recovery recipe](conductor/tracks/kotlin_20261020/emulator-recovery-20261021.md)
record the new guest and current acceptance.
The parity harnesses require a disposable dedicated emulator, matching current
build/APK receipts and absent development packages. They refuse physical targets
and unrelated AVD names. Review the explicit target allowlist in
`scripts/parity_targets.py`; do not replace it with a wildcard.

Use `qualify_parity_emulator.py` for store/source-identity/wake-budget suites,
`qualify_parity_provider.py`, `qualify_parity_worker.py`,
`qualify_parity_lifecycle.py`, `qualify_parity_telephony.py`, and
`qualify_parity_ui.py --suite settings|imports|recovery`, each with an explicitly
selected `--serial`. Lifecycle changes only the guest's power/network state and
reboots the guest; scoped cleanup/restoration and package removal must be verified.
On uncertain cleanup retain instrumentation and the original fixture token.
Computer restart is not an unblock option.

Current Kotlin continuation: [implementation and acceptance handoff](conductor/tracks/kotlin_20261020/active-migration-handoff.md).
115 JVM/92 host tests and the full selected emulator matrix pass (89 successful
native invocations plus one expected termination). Emulator recovery used a fresh
dedicated guest and scoped helper cleanup without restarting the computer. Two clean
same-host unsigned release builds match. This is not production release or completion of
the Compose/KMP/macOS/Rust/iOS roadmap.
