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
all earlier entries and adds12 exact KMP compiler/test artifacts only after
fresh Maven Central byte comparisons; all12 match. The Kotlin test and KMP
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
