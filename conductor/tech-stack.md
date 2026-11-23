> **Active stack (2026-11-24):** Android 17 API37-only Kotlin/Compose; native CallLog/CalendarContract + WorkManager. Room 3.0.3 is selected but its preserving migration has **not** replaced SQLite. `app/build.gradle` is authoritative for SDK/versions; the rest of this file includes prior checkpoints. [Architecture](architecture-android17.md) · [scope](active-scope.json).

# Technology stack and decision status

## Toolchain and Android dependencies — 2026-11-24

The active app and cold Linux release gate select Temurin JDK 25.0.5.1+2,
Gradle 9.7.0, AGP 9.3.1 and Kotlin/Compose compiler 2.4.21. AGP supplies the
app's Kotlin plugin and the first-party Kotlin Multiplatform Android library
plugin was used by a historical shared module (no longer in the active app). The upstream comparison fixtures retain
their JDK 11 and JDK 17 runtimes.

Compose BOM 2026.11.01 requires compile SDK 37.1. The app compiles to JVM 11
bytecode, targets **and requires** Android 17 API37. The sole active hosted
UI acceptance is API37. Prior API26/33/36 evidence is archival; the Android 17.2
16 KB emulator is the current native acceptance target. The
[runtime audit](tracks/release_20261020/canonical-runtime-audit-20261124.json)
and [source inventory](../qualification/release/runtime-sources.json) bind
91 runtime artifacts to their canonical publications.

Renovate proposes AGP 9.5.2 and Gradle 9.9.2, above the fully supported
range for Kotlin 2.4.21. The Android and multiplatform plugins use AGP
9.3.1 with Gradle 9.7.0. The Actions proposal covers the pinned Ubuntu
runner images and the historical JDK 17 comparison fixture. The app and
release gate use JDK 25. The pinned actionlint rejects the explicit
Ubuntu 26.04 runner label. [Renovate dashboard](https://github.com/edithatogo/CallTrack/issues/91),
[Kotlin compatibility range](https://kotlinlang.org/docs/gradle-configure-project.html),
[actionlint runner-label support](https://github.com/rhysd/actionlint/pull/683).

Strict dependency resolution found the Netty 4.1.141 BOM as the only Netty
entry in the root buildscript and release runtime graphs. The proposed 4.2
BOM offers no runtime update for this graph. CallTrack uses the Android
Calendar and CallLog providers, WorkManager for scheduling, and SQLite
for durable receipts and recovery.

## Dependency refresh — 2026-10-23

Python3.15.1 passed all144 local helper tests and Conductor validation and
remains the current preferred selection. The runtime preference is now the
newest qualified stable version, with compatibility-based alternatives allowed
when their reason and scope are recorded; see the current [runtime policy](python-runtime-policy.md).
The [dependency refresh report](../qualification/dependency-refresh-20261023.md)
records current stable Android/tool versions and isolated qualification results.
The AGP9.3.1/Kotlin2.4.21/Gradle9.7.0 candidate stopped at138 untrusted classpath
artifacts under strict verification. The JDK17.0.21+9 patch passed115 JVM tests, APK builds and lint in isolation;
regular modern CI selects that patch. Root Android dependency pins and the frozen
release APK remain unchanged. Newer versions are not promoted by this audit.

## Observed baseline

Reference only: AndroidCallLogSync is Java/XML, AndroidX/View Binding/WorkManager, target SDK 32, AGP 7.2.2 at the pinned revision in the [audit](research/baseline-audit.md). Track 3 adopts the audited fork into the root app workspace with the qualified modern build configuration. These versions are not endorsed for the successor.

## Direction

Preserve useful upstream behaviour while proving a modern build. Kotlin is the planned migration language. Android call-log/calendar providers and native scheduling are initial integration candidates. App-owned Room, DataStore, coroutines/Flow, Ktor, serialization, Compose, KMP and Rust/UniFFI adoption remains conditional; transitive framework libraries and the bounded Kotlin pilot are recorded below.

The completed build track selected the qualified JDK/Gradle/AGP/SDK baseline below. Kotlin, Compose and multiplatform tracks refresh compatibility and alternatives at activation. Platform APIs remain platform-specific; cross-platform code cannot confer call-history access.

## Dependency governance

Follow [workflow](workflow.md) and [decision template](templates/dependency-decision.md). Record both existing dependencies and app-owned implementations that a library could replace. Classify decisions as prototype-only, production candidate, adopted, retained, rejected, deferred or retired. A temporary prototype dependency must have an exit/review trigger and cleanup plan; it must not silently become production architecture.

Update this file when a dependency is actually selected, including track/decision reference, version, purpose and replacement scope. Keep preliminary suggestions in research documents. No new skills were installed: the available setup catalog offered no necessary project-specific addition.

## Track 1 evidence update

[Static dependency/custom-code inventory](archive/baseline_20261020/findings.md) is complete at pinned sources. Androguard4.1.4 was used only in the excluded local analysis environment; it is not an app dependency. No production stack change or fork adoption occurred. Build qualification and transitive dependency resolution remain in the build track.

## Track 2 qualified development direction

Proceed with a local fork of the audited AndroidCallLogSync revision. Preserve the reference and prepare an isolated modern candidate using [build scripts](../qualification/README.md). AGP8.13.2/Gradle8.13/JDK17/compile-target36/build-tools35.0.0 is the selected development baseline; Fragment1.5.1 and ViewModel KTX2.5.1 are interim compatibility overrides subject to the build-track verification record. Original AGP7.2.2/JDK11/SDK32 is a reference reproducer only.

This does not adopt all old runtime dependencies for the long term. Kotlin/Compose/KMP/Rust research remains owned by the later tracks. Local debug signing and disposable test IDs are qualified; public identity, release signing and distribution remain unqualified. See [build findings](archive/build_20261020/findings.md) and [decisions](archive/build_20261020/decisions.md).

## Track 3 implementation decision

Native Android CalendarContract/CallLog provider adapters and a small SQLiteOpenHelper ledger are selected for the initial manual vertical slice. Pure Java mapping/ports stay independent of Android for later Kotlin migration. No new runtime framework is added; provider wrappers and richer persistence libraries are reassessed when they address a demonstrated need. New development app ID is org.calltrack.community.dev.

Track 3 native CalendarContract/CallLog and SQLite selection is now implemented and [verified](archive/calendar_slice_20261020/verification.md) for the bounded manual slice. The app omits network permission, disables legacy boot entry, uses explicit backup/transfer exclusions and retains insert-attempt receipts across uncertain readback. No new runtime framework was added. The modern build passed with 16 JVM tests and zero lint errors; Android17/API37 synthetic provider/SQLite/UI acceptance passed and disposable state was removed. Live source acquisition, remote sync, full parity and production upgrade/restore remain unqualified.

## Track 6 bounded Kotlin development migration

[Track6 decisions](tracks/kotlin_20261020/decisions.md) select Kotlin/KGP2.3.21 for
CallEvent, CallIdentity and CallSelection only, retaining AGP8.13.2/Gradle8.13/JDK17
and Java/Kotlin bytecode1.8. ktfmt0.59 is a checksum-pinned build-only formatter.
Existing Kotlin/coroutines transitives predated this migration; adding the compiler
changes runtime resolution and requires recorded graph readback. No app-owned
coroutine, Room, DataStore, Compose, KMP or Rust adoption occurs. Twenty-one Java
production files remained at the pilot checkpoint; Track4 subsequently enlarged
the source set. Re-inventory it using the Track4 handoff before continuing the
conversion/removal gates. Mixed-language qualification and full M5 completion remain distinct.

## Track7 Compose research outcome

The [isolated experiment](archive/compose_20261020/index.md) uses prototype-only
Compose BOM2026.03.00 (UI1.10.5/Material3 1.4.0), Activity Compose1.12.4 and
Kotlin/Compose compiler2.3.21. It is a standalone Gradle project absent from root
settings, with separate ID, no sensitive/network permissions and no provider code.
Retain Views in the working app; no Compose runtime adoption or legacy UI removal
is approved by this experiment. Revisit after stable M3/M5 state contracts, refreshed
compatibility and real screen/manual accessibility acceptance. Prototype tools can
be retired independently; no application data/code rollback is needed.


## Track4 development scheduling checkpoint

[WORK-D01](archive/parity_20261020/decisions.md) upgrades the existing WorkManager
runtime2.7.1 to2.12.0 for the bounded durable worker. Actual AAR metadata, debug
build and disabled/stale plus positive CallLog/calendar native worker branches pass.
Bounded filters, duplicate requests, permission revocation and latency are recorded
in Track4 worker-provider-evidence.json. lifecycle-evidence.json adds bounded
resume/process-death/reboot/offline/Doze proof and enqueue acknowledgement. The final selected API36 emulator matrix also passes protected call-end wake,
rendered settings/import/recovery UI and full P01–P10 review; physical/cloud/release
and wider-platform acceptance remain pending in the Track4 handoff. Transitive Room2.7.0 and
coroutines1.9.0 are framework internals; app-owned ledgers remain SQLiteOpenHelper
and no new app coroutine/Room API adoption occurs. No experimental WorkManager
analytics or test helper dependency is added. See graph/licence/artifact inventory
in work-dependency-report.json under Track4; no public release claim follows.

Final Track4 review fixes add bounded wake admission, permanent withdrawal of
queued recovery approval after pause, native system/cutout/IME padding and accurate
import feedback. No new runtime dependency is introduced. Ledger schema6/import
schema2 and source-bound110 JVM/73 host/89 successful native invocations plus one
expected termination are recorded in [final evidence](archive/parity_20261020/final-acceptance-evidence.json).

## Current active-code Kotlin continuation — 2026-10-21

The approved future sequence supersedes the bounded pilot scope above: all33
active Java classes have Kotlin implementations (36 production Kotlin files
including the three pilot components). One Java NullableCalendarLabel bridge
preserves the nullable legacy toString ABI. Fourteen unreachable inherited Java
files and12 obsolete resources are retired; see Track6 legacy-removal-audit.json.
Existing native Views, SQLiteOpenHelper, executors and WorkManager remain.
Schema6/import2, namespaces/signatures and development identity are preserved.
No app-owned Compose/KMP/macOS/Rust/iOS implementation is yet qualified.

Seven unused direct dependency declarations are removed. Three still-consumed
Material/AppCompat transitives retain previously qualified versions through
Gradle constraints: ConstraintLayout2.1.3, RecyclerView1.2.1, Transition1.4.1.
These are compatibility constraints, with retirement tied to their native consumers,
not adoption of new UI features. Final source-matched native/release review remains
tracked in Track6; original full-M5/release operational gates stay open.
