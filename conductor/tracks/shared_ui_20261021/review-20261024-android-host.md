# Review: Track 17 Android host slice

Date: 2026-10-24  
Scope: KMP Android target, synthetic Activity host, Android package, desktop
test compatibility, and fresh Android/desktop dependency graph audit.

## Result

The Android host and shared presentation compile and package on API36 with
AGP8.13.2, Kotlin2.4.21 and Compose Multiplatform1.10.3. The debug APK is
8.9 MiB with SHA-256
`3889951175e4654fc2d9ea32bc256d16a1e053d19f561824b55972b97ea667cc`.
The same command passed all seven desktop tests (three reducer cases and four
rendered interactions). No emulator install or Android rendered acceptance is
claimed; the only visible emulator is offline.

The first 1.12.2 Android integration attempt failed metadata checks: its
resolved AndroidX dependencies require API37.1 and AGP9.1. Pinning this
prototype to JetBrains Compose Multiplatform1.10.3 resolved that mismatch on
the qualified API36/AGP8.13.2 toolchain. Host integration also exposed and
fixed AndroidX configuration, Java/Kotlin target alignment, common dependency
API exposure, and the desktop test API import for this pinned version.

The updated desktop graph (63 unique artifacts) and combined Android host/KMP
Android graph (91 unique artifacts) matched fresh canonical HTTPS artifact
downloads; all POMs yielded effective license metadata. Embedded license and
notice files were fingerprinted. The recorded scopes and limitations are in
[android-host-dependency-audit.json](android-host-dependency-audit.json).

## Verification

- JDK17.0.21.0.2+0, Gradle8.13, isolated Gradle user home, local API36 SDK:
  `:android-host:assembleDebug desktopTest` passed.
- Android debug APK produced; no install or instrumentation run because
  `adb devices -l` reported `emulator-5554 offline`.
- `desktopTest`: seven tests, zero failures and errors.
- Python3.15.1 audit: Android 91/91 and desktop 63/63 canonical artifact bytes
  matched; zero artifacts lacked effective POM license declarations.
- The current Kotlin2.4.21/Gradle8.13 forward-compatibility warning remains;
  the repository wrapper was not changed.

## Remaining limits

This is a synthetic host, not integration into the CallTrack product
application. It does not qualify rendered Android behavior, accessibility,
process/lifecycle recovery, upgrades, native macOS packaging, providers,
permissions, release signing or operational use. License metadata and hashes
do not establish legal clearance, security posture, trusted build tools, or
complete notice delivery. Keep Track17 in progress and preserve Tracks14–16
acceptance dependencies.
