# Track 18 refined requirements — 2026-10-24

This track refines R26. A complete evaluation may validly defer Rust without
changing application behavior.

| ID | Requirement | Finding and disposition |
|---|---|---|
| X18-01 | Identify a real consumer or measured deficiency before selecting a Rust boundary. | No measured bottleneck or accepted second parser consumer exists; defer production Rust. |
| X18-02 | Compare existing Kotlin/native code, maintained libraries, Rust and deferral. | Mapped Android identity/import ownership and preview-only macOS Foundation parser; Kotlin/native retained. |
| X18-03 | Refresh framework/toolchain, license, dependency, target and maintenance evidence. | Current Rust host and existing `sha2` graph checked; current UniFFI/KMP plugin and `serde_json` candidates documented with unresolved adoption audits. |
| X18-04 | Preserve identity bytes, receipts, event ownership, human edits and recovery on any future integration. | No app source, persisted contract, schema, dependencies or writer changed. |
| X18-05 | Use a bounded prototype only when it resolves a named uncertainty against an accepted consumer contract. | Not applicable: the only candidate format has no Android exporter and is explicitly a macOS parser prototype. |
| X18-06 | Before promotion, prove full fixtures, hostile bounds, error/panic/cancellation/lifetime behavior and real target/ABI packaging. | No Rust component adopted; all application-boundary gates remain mandatory if reopened. |
| X18-07 | Demonstrate material end-to-end value, including FFI/build/package cost, before replacing Kotlin. | No product benchmark or FFI claim is made; archived synthetic signature timing remains non-decision evidence. |

The prior Track13 Rust research remains useful historical input but is not treated
as current compatibility evidence. This document records the Track18 refresh.
