# Desktop companion UI experiment

Standalone Compose Multiplatform desktop JVM trial. It is absent from the Android
root Gradle settings and contains sample constants only. The screen lets you select
a sample calendar, preview two synthetic entries and clear the selection. Nothing
is imported, persisted or sent. It never opens a file, calendar, contacts or call
provider. There is no shared production core or iOS build.

From the repository root with JDK17:

```sh
JAVA_HOME=/path/to/jdk17 bash ./gradlew -p prototypes/multiplatform-ui test jar --console=plain
```

`test` uses the offscreen desktop renderer (headless Java) and asserts rendered
semantics/actions. This is not a window, screen-reader or visual layout check.
The regular JAR is not a standalone installer; dependencies/native libraries are
needed. Tests resolve the host-specific rendering dependency, not every platform.

For optional manual exploration from the same root:

```sh
JAVA_HOME=/path/to/jdk17 bash ./gradlew -p prototypes/multiplatform-ui run
```

No manual run, packaging, signing, installation or accessibility acceptance follows
from the automated test receipt. The window can be closed normally. No application
data survives closing it; retained preview state is deliberately ephemeral.

Pinned trial: Kotlin/JVM and Compose compiler2.3.21, Compose plugin/Material2 1.10.3,
JDK17, existing Gradle8.13 wrapper, JUnit4.13.2. Newer framework releases are research
inputs in the [track](../../conductor/tracks/multiplatform_ui_20261020/index.md),
not automatically selected. Use the project's ktfmt0.59 Kotlin style check.
The root app and prototype have separate dependency graphs.

Review at second-platform feasibility/shared-core reopening. If no companion is
selected, remove this directory (including its ignored build output) without
changing the Android app or clearing global caches. Promotion requires the track's
native/controller, licence, packaging, upgrade and manual accessibility gates.
