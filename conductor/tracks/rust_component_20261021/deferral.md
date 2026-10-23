# Track 18 production Rust deferral — 2026-10-24

## Reason and evidence

The user authorized Track18 evaluation while Tracks14–17 remain open. Current
inspection found no Rust production dependency. The Android app owns call
identity, import/provider access, persistence and delivery in Kotlin/native code.
The only plausible shared parser consumer is a synthetic, preview-only Swift
JSON parser; Android does not export its format, and no supported cross-platform
format or packaged second application exists. The prior Rust identity prototype
was rerun successfully but covers only three synthetic tests and does not prove
full Java identity compatibility or application benefit.

The current UniFFI KMP plugin has JVM and Kotlin/Native generation, but its
Android configuration requires AGP9, and its current release requirements also
include Gradle9.6.1 and Kotlin2.4. The app's qualified baseline remains
AGP8.13.2/Gradle8.13/Kotlin2.3.21. The desktop Compose prototype is on Kotlin
2.4.21 but Gradle8.13 and no Android target. No native binding or package was
built.

## Consequences

No capability, parser format, backup format, dependency or production target is
added. Existing Kotlin/native code and persisted identities remain authoritative.
The Rust identity experiment remains prototype-only. Track18's evaluation
decision is complete as a deferral, but Tracks14–17 remain unresolved; the
Conductor sequence continues to block Track19 and iOS.

## Reconsideration trigger

Reopen after an accepted Track16/19 application workflow defines a portable
format and Android emits it for a real Kotlin consumer. Refresh current Rust,
serde, UniFFI/KMP plugin and AGP compatibility then. Require real two-platform
integration, hostile-input and golden parity, panic/error/cancellation/lifetime
tests, ABI/package and recovery acceptance, full transitive license/security
review, and measured end-to-end benefit against the native Kotlin option before
adopting Rust.
