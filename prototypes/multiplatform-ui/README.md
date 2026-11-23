# Shared UI prototype

This standalone Compose Multiplatform prototype puts an immutable sample screen
state, explicit actions, pure reducer, and rendering in `src/commonMain`. A
desktop JVM host remains in `src/desktopMain`. The shared screen only displays
synthetic sample data. It performs no file, calendar, provider, permission,
network, persistence, or receipt operation.

The desktop tests cover the reducer plus offscreen rendered interactions. They
do not qualify Android rendering, native macOS/AppKit behavior, keyboard/focus,
screen readers, large-text/RTL, application packaging, upgrade, or recovery.
The Android host builds as a separate test app against SDK 37.0. The macOS
desktop renderer runs on the JVM. Production adoption remains false.

From the repository root, use the qualified JDK 25 installation and isolated
Gradle state:

```sh
GRADLE_USER_HOME="$PWD/.local/gradle-track17" \
sh gradlew --no-daemon --console=plain -p prototypes/multiplatform-ui \
  desktopTest desktopJar :android-host:assembleDebug
```

The prototype uses Kotlin 2.4.21, Compose Multiplatform 1.12.2, AGP 9.3.1 and
the repository's Gradle 9.7.0 wrapper. Its dependency verification policy
covers plugin, application and test artifacts and their metadata. The dated
dependency decision is captured in the Track 17 findings.

The prototype refresh and boundaries are recorded in
`conductor/tracks/shared_ui_20261021/`. Android rendered acceptance requires the
app host and suitable device. Native macOS qualification requires the Track 16
workflow and full Xcode.
