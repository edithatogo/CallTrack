# Build qualification findings

Source: AndroidCallLogSync `aaa56443a294aa61bad48b620427e53f9521c5fe`, unchanged reference checkout. Track 1 supplied the mapping and limitations. No original CallTrack source was copied into this implementation.

## Reproduction and repair

The first wrapper build failed with SDK location missing. Installed task-local SDK command-line tools19.0 (Google repository archive SHA1 checked against repository metadata), SDK32/build-tools30.0.3 and SDK36/build-tools35.0.0. SDK packages required the standard Google Android SDK licence; no paid service or new account was introduced. Tools19 run on the existing JDK17; original AGP runs on JDK11. The original reference build then succeeded with assembleDebug, testDebugUnitTest, lintDebug and a dependency report. The test task was NO-SOURCE. Lint reported0 errors/25 warnings. A subsequent clean build produced the identical debug APK SHA256. This demonstrates repeatability on this host, not independent-host bit-for-bit reproducibility.

A modern isolated copy uses AGP8.13.2/Gradle8.13/JDK17, compile/target36, namespace explicitly set and old manifest package removed. JCenter is removed from this copy. The checksum-pinned Gradle distribution verifies before use. A disposable `org.calltrack.qualification` application ID and separate test ID avoid replacing the upstream/original app. Source Java remains Java8; neither Kotlin nor Compose migration was necessary to establish this baseline.

The initial modern build passed packaging and lint with0 errors/28 warnings, but lint reported an incompatible inherited Fragment1.4.1 custom check registry and skipped nine checks. The targeted `androidx.fragment:fragment:1.5.1` override recommended by that lint diagnostic restores the checks. Its first build exposed duplicate ViewModel classes because Navigation retained lifecycle-viewmodel-ktx2.3.1 while Fragment selected lifecycle-viewmodel2.5.1; aligning KTX to2.5.1 resolves that graph conflict. Both overrides are part of the candidate. This is an interim compatibility repair, not a claim that Fragment1.5.1 is the latest or preferred long-term UI dependency. Final results after repair are in [evidence](evidence.json) and [verification](verification.md).

## Native device evidence

The pre-repair modern APK was installed and cold-launched successfully on the connected Pixel running Android17/API37. A dedicated instrumentation test checked denial, development ADB grant and revocation with selection `1=0`, projecting only `_ID`. All three checks passed and returned no call rows. Sync was disabled and no endpoint configured. Test packages were uninstalled afterward. The final repaired artifact is separately tested and identified in the evidence receipt; do not conflate these versions.

These checks prove provider permission enforcement and zero-row query availability for this installed development app. They do not exercise the app's permission prompt, default-dialler role, Play policy, real history acquisition, sync scheduling or callback semantics. No restricted-permission allowlist override, OS security bypass, device role change, personal-history query or calendar write was used.

## Dependency and implementation assessment

Original dependencies resolved successfully from Google Maven/Maven Central; deprecated JCenter remains declared in untouched upstream but is unnecessary for the qualified modern copy. Notable inherited transitives include Room2.2.5/SQLite2.1.0 through WorkManager, Kotlin stdlib1.6.0 and core1.7.0. Their presence does not mean the successor has selected Room as its own ledger or migrated its own Java to Kotlin. Generated View Binding follows the chosen AGP version. See the committed resolved dependency inventory for precise selected versions; security/licence approval is not inferred from successful resolution.

The upstream has14 app Java files: reusable settings/navigation, permission request and worker scaffolding, plus problematic app-owned query/HTTP/token/checkpoint logic mapped in Track1. Repairing the build required small configuration changes and two targeted compatibility overrides; no broad code rewrite was needed. This is concrete evidence favouring a fork over recreating the UI/build/scheduling scaffolding. Core identity/recovery/calendar behaviour still needs deliberate replacement in the next tracks.

## Tooling and storage

SDK and Gradle caches are local development prerequisites, not shipped dependencies. Internal free space became low during downloads. Task-created SDK files were copied to `/Volumes/PortableSSD/cache/calltrack-sdk-20261020`, file sizes and key JAR/package digests checked, then `.local/android-sdk` replaced by a symlink and its duplicate internal copy removed. SDK installation had completed before this move. Builds were subsequently rerun through the symlink. No unrelated user files were removed.

Root scripts bootstrap the audited reference, prepare an isolated modern copy, retain build receipts, and run bounded device checks. They refuse wrong revisions, overwriting prepared copies and replacing installed test packages. Source files, APK identities, wrapper checksum and device cleanup are evidence boundaries. No public GitHub fork/push/release or hosted CI run occurred. The locally prepared pinned GitHub Actions matrix qualifies the same build commands when run later.

## Primary references

- [AGP7.2 compatibility](https://developer.android.com/build/releases/agp-7-2-0-release-notes): JDK11, Gradle7.3.3, build-tools30.0.3.
- [AGP8.13 compatibility](https://developer.android.com/build/releases/agp-8-13-0-release-notes): JDK17, Gradle8.13, build-tools35.0.0; max API36.1.
- [AGP9.1.1 compatibility](https://developer.android.com/build/releases/agp-9-1-0-release-notes): supports API37; this newer toolchain was researched but not needed/adopted for the target36 experiment.
- [Android17 behaviour changes](https://developer.android.com/about/versions/17/behavior-changes-all): higher OS behaviour needs its own runtime tests even for older targets.
- [Fragment releases](https://developer.android.com/jetpack/androidx/releases/fragment): version choice is an interim compatibility decision, not a latest-version claim.
