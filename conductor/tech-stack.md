# Technology stack and decision status

## Observed baseline

Reference only: AndroidCallLogSync is Java/XML, AndroidX/View Binding/WorkManager, target SDK 32, AGP 7.2.2 at the pinned revision in the [audit](research/baseline-audit.md). No application source has been adopted into this root. These versions are not endorsed for the successor.

## Direction

Preserve useful upstream behaviour while proving a modern build. Kotlin is the planned migration language. Android call-log/calendar providers and native scheduling are initial integration candidates. Room, DataStore, coroutines/Flow, Ktor, serialization, Compose, KMP and Rust/UniFFI are candidates, not installed or selected production dependencies.

The build track will select a compatible JDK/Gradle/AGP/SDK baseline. Kotlin, Compose and multiplatform tracks refresh compatibility and alternatives at activation. Platform APIs remain platform-specific; cross-platform code cannot confer call-history access.

## Dependency governance

Follow [workflow](workflow.md) and [decision template](templates/dependency-decision.md). Record both existing dependencies and app-owned implementations that a library could replace. Classify decisions as prototype-only, production candidate, adopted, retained, rejected, deferred or retired. A temporary prototype dependency must have an exit/review trigger and cleanup plan; it must not silently become production architecture.

Update this file when a dependency is actually selected, including track/decision reference, version, purpose and replacement scope. Keep preliminary suggestions in research documents. No new skills were installed: the available setup catalog offered no necessary project-specific addition.

## Track 1 evidence update

[Static dependency/custom-code inventory](archive/baseline_20261020/findings.md) is complete at pinned sources. Androguard4.1.4 was used only in the excluded local analysis environment; it is not an app dependency. No production stack change or fork adoption occurred. Build qualification and transitive dependency resolution remain in the build track.

## Track 2 qualified development direction

Proceed with a local fork of the audited AndroidCallLogSync revision. Preserve the reference and prepare an isolated modern candidate using [build scripts](../qualification/README.md). AGP8.13.2/Gradle8.13/JDK17/compile-target36/build-tools35.0.0 is the selected development baseline; Fragment1.5.1 and ViewModel KTX2.5.1 are interim compatibility overrides subject to the build-track verification record. Original AGP7.2.2/JDK11/SDK32 is a reference reproducer only.

This does not adopt all old runtime dependencies for the long term. Kotlin/Compose/KMP/Rust research remains owned by the later tracks. Local debug signing and disposable test IDs are qualified; public identity, release signing and distribution remain unqualified. See [build findings](tracks/build_20261020/findings.md) and [decisions](tracks/build_20261020/decisions.md).
