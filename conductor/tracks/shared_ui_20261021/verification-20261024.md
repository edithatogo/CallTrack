# Track 17 local verification — 2026-10-24

## Contract-first and rendered checks

The new common/desktop tests were first run before the implementation. The red
result was the expected Kotlin compilation failure: `CompanionUiState`,
`CompanionUiAction`, `reduceCompanionUi`, and the shared `CompanionScreen` did
not yet exist. No prior failing app test was suppressed.

The implementation was then built using the explicit repository JDK17 path and
isolated Gradle cache. Final command:

```sh
JAVA_HOME=/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home \
GRADLE_USER_HOME="$PWD/.local/gradle-track17" \
bash ./gradlew --offline --no-daemon --console=plain \
  -p prototypes/multiplatform-ui desktopTest
```

Result: `BUILD SUCCESSFUL`; seven tests passed (three common reducer tests and
four desktop rendered interaction tests), zero failures/skips. The test
renderer is offscreen; no window, Android app or native macOS app was manually
opened. ktfmt0.59 `--kotlinlang-style --dry-run --set-exit-if-changed` passed
for all four source/test files after applying formatting. `git diff --check`
and the repository Conductor verifier are run again during review.

The test API uses Compose Multiplatform's JUnit v2 `createComposeRule` entry
point. There is no v1 API deprecation warning; the remaining Gradle8.13 warning
states a future Kotlin2.5 minimum of Gradle8.14.4.

## Resolved graph

Offline dependency reports were captured in the excluded local directory:

- `desktopRuntimeClasspath`: `.local/track17/desktop-runtime-dependencies.txt`,
  SHA-256 `bd274354ce36b0e5710af44ee892916775ef73570a129fc58c87180e702ceadb`.
- `desktopTestRuntimeClasspath`:
  `.local/track17/desktop-test-runtime-dependencies.txt`, SHA-256
  `1e7551ac48d3a1600b7a1a70a5772152904d51b3989ff04d9253f378ceb37bc0`.

The private graph files contain resolved transitive identities but are not
independent provenance, full license, vulnerability or distribution-notice
audits. Those remain promotion gates.

## Acceptance limits

At the initial bounded desktop implementation, the prototype had only a KMP
`desktop` JVM target. That statement is superseded by the Android-host follow-up
below, which adds an Android KMP target and synthetic Activity. There is still
no production Android screen, macOS Native target, accessibility/manual
keyboard/reader test, native package, upgrade, provider, persistence or
production data integration. Tracks14–16 remain in progress and this bounded
Track17 slice does not unlock later stages.

## Android host follow-up — 2026-10-24

The prior no-SDK statement above is historical and is superseded by this
follow-up: an explicit local Android SDK path was available. API36 with
build-tools35.0.0 was selected; the API36 emulator remained offline.

Final source command:

```sh
JAVA_HOME=/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home \
ANDROID_HOME=/Users/doughnut/Documents/calltrack/.local/android-sdk \
GRADLE_USER_HOME=/Users/doughnut/Documents/calltrack/.local/gradle-track17 \
bash ./gradlew --no-daemon --console=plain -p prototypes/multiplatform-ui \
  :android-host:assembleDebug desktopTest
```

Result: `BUILD SUCCESSFUL`; the Android KMP target and synthetic Android
Activity host compiled, the debug APK assembled, and all seven desktop tests
passed. APK SHA-256 is
`3889951175e4654fc2d9ea32bc256d16a1e053d19f561824b55972b97ea667cc`.
`aapt dump permissions` found only the generated non-exported dynamic receiver
permission; no call-log, contacts, calendar or storage permission is declared.
`adb devices -l` reports `emulator-5554 offline`, so no Android launch,
rendering, instrumentation, accessibility or lifecycle acceptance is claimed.

Python checks used Python3.15.1 with pytest9.0.3, pytest-cov7.1.0 and
anyio4.11.0: 27 targeted release-audit and Conductor tests passed.
`scripts/verify_conductor.py` passed with 232 documents, 21 tracks and 25
scenarios; `git diff --check` passed. Dependency audits for the current desktop
and Android graphs matched all canonical HTTPS artifact hashes and found
effective POM license metadata for the 91 Android and 63 desktop artifacts in
their respective scopes; evidence hashes and scope limitations are recorded in
[android-host-dependency-audit.json](android-host-dependency-audit.json).
