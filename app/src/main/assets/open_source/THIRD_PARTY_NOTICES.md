# Runtime third-party notices — development candidate

The application source retains the GPLv3 licence and upstream attribution in
[LICENSE](LICENSE) and [NOTICE.md](NOTICE.md). Runtime dependencies below declare
Apache License2.0; the retained licence text is in
[Apache-2.0.txt](qualification/release/licences/Apache-2.0.txt).

This inventory accompanies the exact-source candidate bundle; it does not
authorise publication or assert permanent trademark rights. Published source
archives, retained copyright headers and all discovered binary/source licence and
notice documents accompany the bundle. A clean app rebuild uses the pinned
prebuilt dependencies; rebuilding every dependency from its upstream repository
is a separate unqualified scope. Exact runtime binary and published source/POM
hashes are recorded in the runtime source inventory; a matching canonical
binary audit remains a separate release prerequisite. Listenablefuture 1.0
has no direct POM licence declaration; its parent POM and published source
header supply the Apache 2.0 evidence.

Build-only dependency licences are recorded separately in the Track5 tool audit;
those tools are not included as application runtime libraries. Do not bundle SDK,
JDK or Gradle distributions with an app/source release by default.

| Runtime coordinate | Licence declaration |
|---|---|
| androidx.activity:activity-compose:1.13.0 | Apache-2.0 |
| androidx.activity:activity-ktx:1.13.0 | Apache-2.0 |
| androidx.activity:activity:1.13.0 | Apache-2.0 |
| androidx.annotation:annotation-experimental:1.5.0 | Apache-2.0 |
| androidx.annotation:annotation-jvm:1.10.0 | Apache-2.0 |
| androidx.arch.core:core-common:2.2.0 | Apache-2.0 |
| androidx.arch.core:core-runtime:2.2.0 | Apache-2.0 |
| androidx.autofill:autofill:1.0.0 | Apache-2.0 |
| androidx.collection:collection-jvm:1.6.0 | Apache-2.0 |
| androidx.collection:collection-ktx:1.6.0 | Apache-2.0 |
| androidx.compose.animation:animation-android:1.13.0 | Apache-2.0 |
| androidx.compose.animation:animation-core-android:1.13.0 | Apache-2.0 |
| androidx.compose.foundation:foundation-android:1.13.0 | Apache-2.0 |
| androidx.compose.foundation:foundation-layout-android:1.13.0 | Apache-2.0 |
| androidx.compose.material3:material3-android:1.5.0 | Apache-2.0 |
| androidx.compose.material3:material3-ripple-android:1.5.0 | Apache-2.0 |
| androidx.compose.material:material-ripple-android:1.13.0 | Apache-2.0 |
| androidx.compose.runtime:runtime-android:1.13.0 | Apache-2.0 |
| androidx.compose.runtime:runtime-annotation-android:1.13.0 | Apache-2.0 |
| androidx.compose.runtime:runtime-retain-android:1.13.0 | Apache-2.0 |
| androidx.compose.runtime:runtime-saveable-android:1.13.0 | Apache-2.0 |
| androidx.compose.ui:ui-android:1.13.0 | Apache-2.0 |
| androidx.compose.ui:ui-geometry-android:1.13.0 | Apache-2.0 |
| androidx.compose.ui:ui-graphics-android:1.13.0 | Apache-2.0 |
| androidx.compose.ui:ui-text-android:1.13.0 | Apache-2.0 |
| androidx.compose.ui:ui-unit-android:1.13.0 | Apache-2.0 |
| androidx.compose.ui:ui-util-android:1.13.0 | Apache-2.0 |
| androidx.concurrent:concurrent-futures-ktx:1.1.0 | Apache-2.0 |
| androidx.concurrent:concurrent-futures:1.1.0 | Apache-2.0 |
| androidx.core:core-ktx:1.19.0 | Apache-2.0 |
| androidx.core:core-viewtree:1.0.0 | Apache-2.0 |
| androidx.core:core:1.19.0 | Apache-2.0 |
| androidx.customview:customview-poolingcontainer:1.0.0 | Apache-2.0 |
| androidx.emoji2:emoji2:1.7.0 | Apache-2.0 |
| androidx.graphics:graphics-path:1.1.0 | Apache-2.0 |
| androidx.graphics:graphics-shapes-android:1.0.1 | Apache-2.0 |
| androidx.interpolator:interpolator:1.0.0 | Apache-2.0 |
| androidx.lifecycle:lifecycle-common-java8:2.11.1 | Apache-2.0 |
| androidx.lifecycle:lifecycle-common-jvm:2.11.1 | Apache-2.0 |
| androidx.lifecycle:lifecycle-livedata-core-ktx:2.11.1 | Apache-2.0 |
| androidx.lifecycle:lifecycle-livedata-core:2.11.1 | Apache-2.0 |
| androidx.lifecycle:lifecycle-livedata:2.11.1 | Apache-2.0 |
| androidx.lifecycle:lifecycle-process:2.11.1 | Apache-2.0 |
| androidx.lifecycle:lifecycle-runtime-android:2.11.1 | Apache-2.0 |
| androidx.lifecycle:lifecycle-runtime-compose-android:2.11.1 | Apache-2.0 |
| androidx.lifecycle:lifecycle-runtime-ktx-android:2.11.1 | Apache-2.0 |
| androidx.lifecycle:lifecycle-service:2.11.1 | Apache-2.0 |
| androidx.lifecycle:lifecycle-viewmodel-android:2.11.1 | Apache-2.0 |
| androidx.lifecycle:lifecycle-viewmodel-ktx:2.11.1 | Apache-2.0 |
| androidx.lifecycle:lifecycle-viewmodel-savedstate-android:2.11.1 | Apache-2.0 |
| androidx.navigationevent:navigationevent-android:1.1.0 | Apache-2.0 |
| androidx.navigationevent:navigationevent-compose-android:1.1.0 | Apache-2.0 |
| androidx.profileinstaller:profileinstaller:1.4.0 | Apache-2.0 |
| androidx.room3:room3-common-jvm:3.0.3 | Apache-2.0 |
| androidx.room3:room3-runtime-android:3.0.3 | Apache-2.0 |
| androidx.room:room-common-jvm:2.7.0 | Apache-2.0 |
| androidx.room:room-runtime-android:2.7.0 | Apache-2.0 |
| androidx.savedstate:savedstate-android:1.5.0 | Apache-2.0 |
| androidx.savedstate:savedstate-compose-android:1.5.0 | Apache-2.0 |
| androidx.savedstate:savedstate-ktx:1.5.0 | Apache-2.0 |
| androidx.sqlite:sqlite-android:2.7.1 | Apache-2.0 |
| androidx.sqlite:sqlite-async-android:2.7.1 | Apache-2.0 |
| androidx.sqlite:sqlite-framework-android:2.7.1 | Apache-2.0 |
| androidx.startup:startup-runtime:1.1.1 | Apache-2.0 |
| androidx.tracing:tracing-android:2.0.5 | Apache-2.0 |
| androidx.tracing:tracing-ktx:2.0.5 | Apache-2.0 |
| androidx.versionedparcelable:versionedparcelable:1.1.1 | Apache-2.0 |
| androidx.window:window-core-android:1.5.0 | Apache-2.0 |
| androidx.window:window:1.5.0 | Apache-2.0 |
| androidx.work:work-runtime:2.12.0 | Apache-2.0 |
| com.google.guava:listenablefuture:1.0 | Apache-2.0 (parent POM and source header) |
| org.jetbrains.kotlin:kotlin-stdlib:2.4.21 | Apache-2.0 |
| org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0 | Apache-2.0 |
| org.jetbrains.kotlinx:kotlinx-coroutines-core-jvm:1.9.0 | Apache-2.0 |
| org.jetbrains.kotlinx:kotlinx-serialization-core-jvm:1.7.3 | Apache-2.0 |
| org.jetbrains:annotations:23.0.0 | Apache-2.0 |
| org.jspecify:jspecify:1.0.0 | Apache-2.0 |

## Modified project and assets

The fork and original replacement vector artwork use GPLv3; upstream attribution
is retained in NOTICE.md. See [asset provenance](ASSET_PROVENANCE.md) and
[modification notices](MODIFICATIONS.md). The 77 runtime dependency source
coordinates and hashes are in
[the source inventory](qualification/release/runtime-sources.json). 9 published
source archives contain metadata and licence text only; their runtime artifacts
contain no JVM class entries. Retained notice entries include earlier audited
origins as well as the current runtime graph.
The APK carries this notice, GPL/Apache texts and retained embedded notices under
`assets/open_source/`. No original CallTrack APK material, upstream launcher bitmap,
font, screenshot or store artwork is used in the current app.
