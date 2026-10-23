# Track 18 decisions — 2026-10-24

## RUST-D18-01: Retain Kotlin/native ownership; defer production Rust

**Status:** justified deferral; no production Rust dependency or code adopted.

The active app has no measured parser/throughput bottleneck. The only plausible
portable parser consumer is a preview-only macOS JSON prototype, and the Android
app does not export its schema. There is no accepted shared format, production
macOS target, or demonstrated second consumer. Keep identity/receipt, call
selection, import policy, provider, persistence, permission, calendar and
scheduling ownership in their existing Kotlin/native layers.

The archived Rust signature trial remains isolated evidence. Its microbenchmark
does not establish application benefit and its implementation does not preserve
the complete Java UTF-16 / legacy-constructor contract. Do not integrate it or
modify stored signatures.

Do not prototype another Rust parser against a schema with no Android producer.
Keep the macOS Foundation JSON parser and its synthetic bounded contract as a
prototype. Reopen only after Track16/19 establish a real app workflow, a selected
portable format and at least one actual Kotlin consumer. Then refresh parser and
binding versions; pin one representative golden/hostile corpus; compare native
Kotlin and Rust through an actual Android plus Apple application boundary,
including FFI copies, error mapping, cancellation/lifetime, resource bounds,
binary size, build reproducibility, licence/notices and recovery. Adopt only if
complete compatibility and a material end-to-end benefit are demonstrated.

The current third-party UniFFI KMP plugin offers Kotlin/Native generation but
raises the required Android toolchain to AGP9/Gradle9.6.1/Kotlin2.4; it is not
compatible with the currently qualified app build without a separate migration.
Its current release history is active, but the README's support-version paragraph
is stale relative to its changelog. No plugin or runtime was installed.

This decision closes the Rust adoption evaluation only. It does not complete or
defer Tracks14–17, qualify Track19, or unlock Track20/iOS.
