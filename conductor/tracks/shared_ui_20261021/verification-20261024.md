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

The prototype has only a KMP `desktop` JVM target. It has no Android KMP target,
Android Activity, real app screen or macOS Native target. No Android SDK/device,
full Xcode, accessibility inspector/manual keyboard/reader test, native package,
upgrade, provider, permissions, persistence or production data is involved.
Tracks14–16 are still unresolved and this bounded Track17 deferral does not
unlock later stages.
