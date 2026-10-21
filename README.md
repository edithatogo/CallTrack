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

Use JDK17, Android SDK platform36/build-tools35.0.0 and the included Gradle8.13
wrapper and Kotlin/KGP2.3.21. Set `JAVA_HOME` and `ANDROID_HOME` to those installed toolchains, then:

```sh
python3 scripts/run_slice_build.py --jdk "$JAVA_HOME" --sdk "$ANDROID_HOME"
python3 -m unittest discover -s tests -v
python3 scripts/verify_conductor.py
```

The build runs JVM tests, builds app/test APKs and lints. Receipts and logs stay under
excluded `.local/slice-evidence`. The active implementation now comprises36 Kotlin
production files and one Java bridge preserving nullable calendar-label behaviour.
[Track6](conductor/tracks/kotlin_20261020/index.md) records migration and acceptance
separately. Compose/KMP/Rust are pending successor tracks in the
[approved iOS-last sequence](conductor/future-architecture.md), not runtime dependencies.
Retained dependencies include
interim compatibility overrides documented in [tech stack](conductor/tech-stack.md).

For an explicitly selected, authorised test device with neither development test
package already installed:

```sh
python3 scripts/qualify_slice_device.py --serial DEVICE_SERIAL
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

Run new Java files through google-java-format1.24.0 with `--aosp`; the checksum and
download recipe are in CI. Kotlin uses ktfmt0.59 with `--kotlinlang-style`;
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

[Compose research](prototypes/compose/README.md) is an isolated synthetic app, not
part of this working application's build or runtime. Its research decision retains
Views until real screen behaviour and manual accessibility are qualified.

## Parity acceptance and recovery

Current evidence and remaining boundaries are in the [Track4 handoff](conductor/archive/parity_20261020/handoff.md).
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
115 JVM/89 host tests and selected native suites pass; guest startup ANRs/phone
service availability block the full final emulator matrix. Two clean same-host
unsigned release builds match. This is not production release or completion of
the Compose/KMP/macOS/Rust/iOS roadmap.
