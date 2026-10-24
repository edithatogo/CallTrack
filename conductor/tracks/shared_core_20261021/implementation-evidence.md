# Track 15 implementation checkpoint — 2026-10-24

## Scope delivered

Added a `shared-core` Kotlin Multiplatform library with Android, JVM and
`macosArm64` targets. Its common API contains only raw call-type selection and
validated half-open timestamp ranges. The existing `CallSelection` JVM facade
delegates to those rules, preserving its constructor, `@JvmField` booleans,
method signatures and invalid-range message. Android call records still enter
through that facade. The module owns no providers, database, receipts, calendar
writer, scheduler, permission, credentials or network behavior.

## Local verification

The repo-local Temurin JDK17.0.21+9 and Android SDK36 were selected explicitly.
The `macosArm64` target is configured, but `xcodebuild -version` reports that the
active developer directory is Command Line Tools, not Xcode; no Kotlin/Native
build, test or artifact acceptance is claimed.

Strict dependency verification passed for the shared JVM test, app JVM suite,
Android compile and debug APK packaging:

```text
bash ./gradlew --offline --no-daemon --console=plain \
  :shared-core:jvmTest :app:testDebugUnitTest :app:compileDebugKotlin
```

The shared common test suite has3 passing cases on JVM for independent/unknown
call types, half-open start/end behavior and invalid ranges including `Long`
bounds. The same common suite passes3 cases through the Android unit-test target:

```text
bash ./gradlew --offline --no-daemon --console=plain :shared-core:testDebugUnitTest
```

The existing app JVM suite reports115 tests, zero failures; the Android app and
shared module compile for the debug target and the unsigned debug APK packages.
The strict artifact policy retains
all earlier entries and adds14 exact KMP compiler/test artifacts only after
fresh Maven Central byte comparisons; all14 match. The Kotlin test and KMP
compiler dependency POMs declare Apache-2.0. See
[dependency verification audit](dependency-verification-audit.json).

An earlier `--write-verification-metadata` build exposed broad all-target
resolution and attempted to rewrite unrelated trust entries. It was stopped;
the prior metadata was restored and only the12 source-verified additions were
kept. That broad writer run is not acceptance evidence.

## Remaining acceptance

- Run the common fixtures on macOS Kotlin/Native after full Xcode and Apple SDKs
  are selected and qualified.
- Compare common outputs with existing source/facade fixtures on both targets;
  retain signature, event fingerprint, schema6/import2, namespace and receipt
  compatibility.
- Complete Track 14's manual logging/import/recovery Compose work and remaining
  UI acceptance before Track 16; this Track 15 scoped deferral does not resolve
  Track 14 in the sequence.
- Track 15 remains in progress and `production_adopted=false`; no public
  cross-platform capability or release readiness is claimed.

## Kotlin/Native toolchain trust checkpoint — 2026-10-24

The `kotlin-native-prebuilt:2.3.21` macOS ARM64 Maven artifact and POM are now included in strict Gradle verification metadata. The archive SHA-256 is `ae4c8d0d0c18a9dd3ac6a9eb1222c1b902f78ac0996de634f7a771faf5e6e583`; the POM SHA-256 is `b76ad8c3146787c2e38a059e03419a5e766451e787eefad08b365626daeade3c`. Fresh Maven Central downloads matched both entries, and the Maven-published SHA-1 sidecars also matched. The POM declares Apache-2.0.

The raw Maven and JetBrains GitHub release tarball hashes differ, but an independent bounded member scan found 26,128 regular files in each archive with identical member paths, sizes, and SHA-256 content. This confirms payload equivalence for those regular files, not archive metadata identity. JetBrains' attached SPDX 2.3 document contains zero package and relationship records. The bundle README lists third-party licences, but it is source-wide evidence and does not establish binary-level applicability or complete legal clearance.

The native compiler's separate LLVM archive matches its published SHA-256 sidecar. The LLDB archive does not: downloaded SHA-256 `069193359103d4e4a4653f236e7a963e266a4a366905e858d4a68e701f43866e`; publisher sidecar `6c97f0a4755b83282b037800ac4c0591ebb8e7a3517bb53924bcb61684243d45`. Its contents are not treated as trusted native-test evidence. In addition, `xcodebuild -version` fails because only Command Line Tools are selected; Kotlin/Native 2.3.21's version-pinned `konan.properties` sets `minimalXcodeVersion=12.5` and its `macosArm64` toolchain entries reference `xcode_26.0.1_17A400`. Therefore `macosArm64Test` remains not accepted and the KMP milestone remains open. See [toolchain review](review-20261024-kotlin-native-toolchain.md) and [dependency audit](dependency-verification-audit.json).

## Hosted Kotlin/Native acceptance job prepared — 2026-10-24

Added `.github/workflows/shared-core-native.yml`, scoped to shared-core/build configuration changes and manual dispatch. It selects the public `macos-15` ARM64 runner, Temurin 17.0.20+101, and the currently listed Xcode 26.1.1 (build 17B100), verifies Android SDK36/build-tools35 are present, and runs `:shared-core:macosArm64Test`. The workflow requires generated JUnit XML. The current hosted image manifest lists this Xcode, Android SDK and ARM64 runner; see [runner image manifest](https://github.com/actions/runner-images/blob/main/images/macos/macos-15-arm64-Readme.md) and [GitHub-hosted runner reference](https://docs.github.com/en/actions/reference/runners/github-hosted-runners).

At preparation time this was only a CI mechanism, with native acceptance still pending. The actual hosted result and reviewed test report are recorded below.

## Hosted Kotlin/Native acceptance — 2026-10-24

The pull-request workflow completed successfully on commit `58341ddbf2878c2f2040862b38eed6c44b2246d3`: [GitHub Actions run 40083986461](https://github.com/edithatogo/CallTrack/actions/runs/40083986461). It ran on GitHub's `macos-15-arm64` image (macOS 15.7.9, image `20260907.0337.1`), selected Xcode 26.1.1 / build `17B100`, and used Temurin 17.0.20.1+1. The workflow verified Android platform 36 and build-tools 35.0.0, then ran `:shared-core:macosArm64Test` successfully (6 Gradle tasks executed; 3 fixture tests). The exact JUnit report records `tests=3`, `skipped=0`, `failures=0`, `errors=0` for `CallSelectionRulesTest`.

This proves the bounded common call-selection and timestamp-range fixtures run on Kotlin/Native ARM64 with the selected hosted toolchain. It does not prove UI, Apple provider, Android device, calendar, release, or operational behavior. Track 15 remains in progress for its broader cross-target artifact/compatibility review and prerequisite sequencing; production adoption remains false.

The final PR head, `a078705d1593dcd01017e150b18c6a80b5852d65`, reran the workflow with the stricter JUnit assertions enabled: [run 40084343991](https://github.com/edithatogo/CallTrack/actions/runs/40084343991). That pull-request run again used Xcode 26.1.1 / `17B100` and Temurin 17.0.20.1+1, succeeded in 2m01s, and reported the same 3 tests with zero skips, failures or errors. All other required repository checks also passed on this head; PR #75 was squash-merged as `22e66c1b9c3905f2016efc1e6c20763056163f27`.


## Local verification after compiler checksum update — 2026-10-24

With the already qualified isolated Temurin 17.0.20.1+1 JDK and PortableSSD Android SDK36 selected explicitly, the following offline tasks passed after the metadata update:

```text
:shared-core:jvmTest
:shared-core:testDebugUnitTest
:app:testDebugUnitTest
:app:compileDebugKotlin
:app:assembleDebug
BUILD SUCCESSFUL; 72 actionable tasks
```

The test result files report 6 shared-core tests total (3 JVM and 3 Android unit tests) and 115 app JVM tests, with zero failures, errors, or skips. The unsigned debug APK is 12,356,531 bytes with SHA-256 `949084fcb4c7b68ff5f98778cf7309bef2fca98c15215feb6cc5124658eba0bd`. This is local packaging evidence only; it does not satisfy macOS Kotlin/Native acceptance.

The Python 3.15.1 Conductor integrity verifier passed (229 documents, 21 tracks, 25 synthetic scenarios), and the focused `test_verify_conductor.py` unittest module passed 19 tests. `git diff --check` passed. An attempted `pytest` invocation could not run because pytest is not installed in the selected repository environment; the same focused suite was run successfully with its unittest runner, without installing or changing dependencies.

## Shared selection fixture expansion — 2026-10-25

The common Kotlin fixture now exercises all eight combinations of the three
CallLog type filters, checks that types `0`, `4`–`8`, `-1` and `Int.MAX_VALUE`
remain excluded, and verifies the half-open time range rejects
`Long.MIN_VALUE`. This extends the Native/JVM common contract to match the
existing Android facade matrix without changing production code or persisted
data.

With JDK17.0.21+9 and the already qualified Android SDK36, all targeted tests
passed locally:

```text
:shared-core:jvmTest
:shared-core:testDebugUnitTest
:app:testDebugUnitTest --tests com.markobl.calllogsync.slice.CallSelectionTest
```

Each suite reported three tests with zero skips, failures or errors. The
updated Kotlin source passed pinned ktfmt0.59 and `git diff --check`. The
previous hosted Kotlin/Native run [40084777973](https://github.com/edithatogo/CallTrack/actions/runs/40084777973)
passed the prior three-case common suite; a hosted rerun of this expanded
fixture is pending. Track15 remains open, and the native gate is not promoted
from the old test-source result.
