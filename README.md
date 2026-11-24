# CallTrack — Android 17 native development

CallTrack is a GPL-3.0 development fork of AndroidCallLogSync for **Android 17 (API 37)**. It uses Kotlin, Jetpack Compose, WorkManager, Room 3, Android `CallLog` and `CalendarContract`. The first public release is in qualification.

- [Implemented and planned features](FEATURES.md)
- [Current architecture and rationale](conductor/architecture-android17.md) · [machine-readable scope](conductor/active-scope.json) · [Conductor tracks](conductor/tracks.md)
- [Ponytail scope PR #114](https://github.com/edithatogo/CallTrack/pull/114) · [Room migration #115](https://github.com/edithatogo/CallTrack/issues/115) · [UI simplification #116](https://github.com/edithatogo/CallTrack/issues/116) · [local runner #117](https://github.com/edithatogo/CallTrack/issues/117)
- [Release gates](RELEASING.md) · [privacy](PRIVACY.md) · [security](SECURITY.md) · [source provenance](NOTICE.md)

## Build and qualification

Use the versions in `build.gradle`, `app/build.gradle`, `gradle/wrapper/gradle-wrapper.properties` and `.python-version` as the authoritative selections. At this checkpoint the application expects Temurin JDK 25, Android SDK platform 37.1, Gradle 9.9.2, AGP 9.5.2 and Kotlin 2.4.21.

```sh
uv venv --python "$(cat .python-version)" .venv
uv tool install pre-commit
pre-commit install --hook-type pre-commit --hook-type pre-push
export JAVA_HOME=/path/to/qualified/jdk
export ANDROID_HOME=/path/to/android-sdk
.venv/bin/python -m unittest discover -s tests -v
.venv/bin/python scripts/verify_conductor.py
.venv/bin/python scripts/run_slice_build.py --jdk "$JAVA_HOME" --sdk "$ANDROID_HOME"
```

**Local Android 17 emulator is the first native acceptance option.** Create the dedicated `calltrack-android17-local` AVD with an SDK-manager-listed Android 17 image matching the host architecture (Apple Silicon needs ARM64), and confirm it reports API 37. Do not assume a package exists until it is listed by the installed SDK Manager. An explicit test command rejects physical phones, other AVDs and the wrong API.

```sh
bash scripts/qualify_android17_local.sh emulator-5554
```

This performs source-bound builds and synthetic settings, import, recovery and accessibility journeys. It requires a prepared `.venv` and Android SDK and preserves cleanup evidence. [Hosted API37 UI checks](.github/workflows/track14-hosted-ui.yml) are the independent fallback. The [official self-hosted macOS runner workflow](.github/workflows/android17-local.yml) is opt-in and cannot execute until a runner, Android17 ARM64 AVD and toolchain have actually been configured. Apple `container` is for optional Linux CLI/static checks, not a macOS or Android emulator host.

## Data safety and unfinished acceptance

Manual, automatic and confirmed historical calls use a durable intent/receipt ledger. Unknown writes require reconciliation rather than automatic re-insertion; user-edited/deleted calendar events must not be overwritten. No `INTERNET` permission or automatic email invitations. Prior schema versions and migrations are still test-critical despite Android 17-only runtime support.

Release remains gated by signing and identity, licences/provenance, physical call/OEM acceptance, calendar-account readback, existing-data upgrade, accessibility and operational observation. Google Play's call-log permission policy needs a separate distribution eligibility decision.

## Documentation and historical evidence

**Write once, link elsewhere.** Runtime and dependency settings belong to Gradle files; Python to `.python-version`; active scope to `conductor/active-scope.json`; GitHub issues/PRs to the execution record; source-bound local receipts to ignored `.local/`, and hosted receipts to immutable Actions artifacts. Keep only short referenced dispositions in Conductor.

The earlier Android 26/33/36 tests, KMP, Rust, macOS and iOS research, experiments, release receipts and decisions are **historical evidence**, not active development. Browse [Conductor archive](conductor/archive/index.md), [prior research roadmap](conductor/roadmap.md) and [qualification history](qualification/README.md) without treating their previously selected versions as live settings.
