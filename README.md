# CallTrack community development

A GPL-3.0 development fork of AndroidCallLogSync, working toward a maintainable
CallTrack successor. See [source provenance](NOTICE.md) and [Conductor roadmap](conductor/roadmap.md).

The current slice is manual: choose a visible writable device calendar, grant the
requested permissions, then explicitly log the latest call. The event uses cached
caller identity when available, exact timestamps, private/free defaults and no
reminders. Restricted numbers remain private. Known insert IDs are retained even if readback fails; receipt presence alone is not
success. A zero-duration call has a one-minute
visual marker and describes its actual zero duration. Calendar providers may sync
these events to their accounts. Launching or granting access never reads a call.

This is a development app (`org.calltrack.community.dev`), not a parity release.
Automatic logging, historical import, settings parity, complete localisation,
release signing and distribution remain later tracks. No direct network permission
is present; calendar account software controls remote sync. Success here means
local provider readback, not remote account acceptance. Existing event changes or
missing receipts/events require review rather than automatic replacement. No review
UI is implemented yet; inspect the calendar and retain the ledger for investigation.

## Build and verify

Use JDK17, Android SDK platform36/build-tools35.0.0 and the included Gradle8.13
wrapper. Set `JAVA_HOME` and `ANDROID_HOME` to those installed toolchains, then:

```sh
python3 scripts/run_slice_build.py --jdk "$JAVA_HOME" --sdk "$ANDROID_HOME"
python3 -m unittest discover -s tests -v
python3 scripts/verify_conductor.py
```

The build runs JVM tests, builds app/test APKs and lints. Receipts and logs stay under
excluded `.local/slice-evidence`. Kotlin/Compose/KMP/Rust are research directions,
not dependencies of this slice. Dependencies inherited from upstream include
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
download recipe are in CI. The upstream build reproducer remains available through
[qualification instructions](qualification/README.md). CI definitions are local
configuration until a hosted run is observed.

Ledger schema1 is preserving: unsupported upgrades fail instead of dropping data.
The receipt key includes per-install source namespace, call row ID and destination.
Do not clear app data as a retry strategy: losing receipts changes recovery and
identity semantics. Cross-device restore, edited/deleted crash-gap tombstones and
full background reconciliation are Track4 work. Keep working releases and source
history until migration and recovery have passed.
