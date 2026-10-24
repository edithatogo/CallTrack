# Track 15 findings — refreshed 2026-10-23

## Prerequisite and scope refresh

Track 14 remains in progress: its read-only automatic-settings status component
and host tests passed on API 36, while manual logging, import, recovery and wider
accessibility/lifecycle acceptance remain. Its scoped prerequisite record requires
this refresh and native status-component acceptance before Track 15 adopts shared
abstractions; both are now evidenced. Track 5 remains not release-ready and Track
6 retains linked M5 gates. Their current metadata and the project checkpoint were
read at activation. This Track 15 slice does not change release, physical, cloud,
or operational acceptance.

## Behavior and ownership map

| Candidate responsibility | Current owner | Shared-core boundary |
|---|---|---|
| Raw call type selection (incoming=1, outgoing=2, missed=3; unknown excluded) | `CallSelection` | Pure integer-to-selection rule; no Android call-log types |
| Half-open source timestamp range | `CallSelection.inRange` | Pure validated range predicate; preserve `[start,end)` and invalid-range failure |
| Call source facts and identity/signature | `CallRecord`, `CallIdentity` | Keep JVM facade and persisted signature unchanged until byte-identical KMP parity is separately established |
| Event rendering, SHA-256, timezone and overflow | `CallEvent` | Retain current JVM behavior; no CommonCrypto, date/time or hash library selected in this slice |
| Receipts, SQLite ledger, provider reconciliation and writer serialization | `DurableDelivery`, `SqliteLedger`, Android adapters | Remain single-owner native code; no second ledger or common writer |

The shared module will contain only the first two bounded rules. Android callers
continue through the existing Java-visible `CallSelection` facade. No provider,
permission, database, scheduler, credential, calendar, or network API moves.

## Toolchain and dependency decision

The root project uses Gradle 8.13, AGP 8.13.2 and Kotlin Gradle Plugin 2.3.21.
The refreshed official Kotlin compatibility table lists Kotlin Multiplatform
2.3.20–2.3.21 with Gradle 7.6.3–9.3.0 and AGP 8.2.2–9.0.0; this includes the
repository's pinned pair. The same current guide lists 2.4.20–2.4.21 as stable
and compatible with this Gradle/AGP pair, but this track retains the already
qualified 2.3.21 compiler instead of upgrading the whole Android app without a
separate toolchain qualification. The guide recommends Google's newer
`com.android.kotlin.multiplatform.library` plugin for Android targets; this slice
uses the existing AGP `com.android.library` plus `androidTarget` integration.
Reassess that plugin migration with a compatible AGP/compiler pair before
promoting KMP beyond the bounded module. Reuse the existing KGP artifact and
avoid a new production runtime dependency. KMP's test adapter is Kotlin test;
its exact artifacts are separately verified in the strict Gradle policy.

Options compared: retain native logic (lowest migration risk), extract a small
owned common module (chosen bounded implementation), add a third-party rules or
date/hash library (no demonstrated need), or defer all KMP work (preserves the
existing boundary but misses the requested cross-target contract experiment).
The selected scope does not justify hashing, date/time or serialization
dependencies. The `macosArm64` target is configured because this host is Apple
Silicon. `xcodebuild -version` reports that only Command Line Tools are selected;
Xcode and its Apple SDK are unavailable, so Kotlin/Native build/test acceptance
cannot be claimed here. The current KMP compatibility matrix lists Xcode26.0 for
KGP2.3.20–2.3.21. Reconsider that gate when a compatible full Xcode and Apple
SDK are selected and qualified.

No package-size, performance, energy, maintenance-time or developer-productivity
benefit has been measured. The shared code remains a local implementation
candidate until JVM/Android parity and macOS Kotlin/Native tests both pass.

## Primary-source refresh

- Kotlin Multiplatform [compatibility guide](https://kotlinlang.org/docs/multiplatform/multiplatform-compatibility-guide.html), accessed 2026-10-23.
- Kotlin [Gradle project configuration](https://kotlinlang.org/docs/gradle-configure-project.html), accessed 2026-10-23.
- Kotlin [Native target support](https://kotlinlang.org/docs/native-target-support.html), accessed 2026-10-23.

These sources describe framework compatibility, not this repository's artifact
trust, native packaging, or application acceptance.

## Kotlin/Native toolchain refresh — 2026-10-24

The KGP 2.3.21 macOS ARM64 compiler archive now has source-checked Maven SHA-256 entries in Gradle verification metadata; its normalized payload matches the JetBrains GitHub release archive across 26,128 regular files. The publisher's SPDX attachment is empty of package records, so licensing remains incompletely mapped. LLVM's publisher sidecar matches, while the LLDB download's SHA-256 conflicts with its publisher sidecar. Full Xcode is also absent (`xcodebuild` unavailable from the selected Command Line Tools). Keep the native acceptance task open until a trusted matching LLDB artifact and compatible Xcode/SDK are available, then run the actual common fixtures and inspect resulting artifacts. See [the toolchain review](review-20261024-kotlin-native-toolchain.md).
