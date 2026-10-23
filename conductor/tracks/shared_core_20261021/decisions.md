# Track 15 decisions — 2026-10-23

## K15-D01: Bound the first common extraction

**Decision:** Use the existing Kotlin 2.3.21 compiler in an isolated KMP shared
module for the deterministic call-type selection and half-open timestamp-range
rules. Keep `CallSelection` as the existing app facade. Do not move persisted
identity, event hashing/rendering, receipts, SQLite, providers or scheduling in
this slice.

**Why:** These two rules are currently pure, deterministic and framework-free.
They offer a meaningful JVM/common contract without introducing hash/date
libraries or changing any durable identity. The larger candidates still depend
on byte-level SHA-256/string/time contracts and native writer/recovery boundaries
that are not yet independently proven cross-target.

**Dependency status:** Kotlin Multiplatform plugin is reused from the existing
Kotlin Gradle Plugin 2.3.21 classpath; no new production runtime library is
selected. `kotlin.test` is a test-only candidate and must pass strict dependency
verification before use. No candidate framework has been promoted because it is
newer or described as bleeding-edge.

**Compatibility:** Existing `CallSelection` JVM/Java surface and invalid-range
validation remain. No signature, namespace, schema, ledger ownership, event
fingerprint or calendar side effect changes. Android callers remain routed via
the app facade; the common module does not write destinations.

**Rollback:** Remove the shared module and dependency and restore the two current
method bodies in `CallSelection`. No stored state or user data migration is
needed.

**Acceptance status:** This decision authorizes the bounded local implementation
only. `production_adopted` stays false until common fixtures pass on JVM/Android
and macOS Kotlin/Native and the retaining/parity/upgrade gates are evidenced.
The host has no selected full Xcode, so Native acceptance is currently blocked.
