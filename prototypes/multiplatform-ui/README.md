# Shared UI prototype

This standalone Compose Multiplatform prototype puts an immutable sample screen
state, explicit actions, pure reducer, and rendering in `src/commonMain`. A
desktop JVM host remains in `src/desktopMain`. The shared screen only displays
synthetic sample data. It performs no file, calendar, provider, permission,
network, persistence, or receipt operation.

The desktop tests cover the reducer plus offscreen rendered interactions. They
do not qualify Android rendering, native macOS/AppKit behavior, keyboard/focus,
screen readers, large-text/RTL, application packaging, upgrade, or recovery.
There is no Android SDK or native macOS target configured in this slice. Desktop
JVM on macOS is not a native macOS companion. Production adoption remains false.

From the repository root, use the qualified JDK17 installation and isolated
Gradle state:

```sh
JAVA_HOME=/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home \
GRADLE_USER_HOME="$PWD/.local/gradle-track17" \
bash ./gradlew --no-daemon --console=plain -p prototypes/multiplatform-ui desktopTest
```

The current prototype pins Kotlin Multiplatform/Compose compiler2.4.21 and
Compose Multiplatform1.12.2. Root Android build pins and production source remain
unchanged. The root Gradle wrapper is8.13; Kotlin currently builds this prototype
with a warning that future Kotlin2.5 requires at least8.14.4. No suppressions or
root wrapper changes were made. Requalify a newer wrapper before promoting this
prototype. The resolved runtime graph and dated dependency decision are captured
in the Track17 findings; do a full licence/notice audit before distribution.

The prototype refresh and boundaries are recorded in
`conductor/tracks/shared_ui_20261021/`. Android integration requires the SDK,
qualified app host and a separate compatibility graph. Native macOS qualification
requires a selected Track16 workflow and full Xcode. Do not infer either outcome
from desktop tests.
