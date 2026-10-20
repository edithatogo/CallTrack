# Dependency decision RUST-D01

- Owner/date/refresh: Track13,2026-10-21; R26/X01–X10; [findings](findings.md),
  [verification](verification.md), [evidence](evidence.json).
- Problem: determine whether Rust reuse materially improves current development,
  robustness or portability before adding ABI/FFI/packaging maintenance.
- Existing app-owned code: CallIdentity/CallRecord signature inputs and JCA SHA256;
  event/ledger/provider responsibilities remain out of experiment scope.
- Options: retain current Kotlin/JVM (chosen); optimize preserving Kotlin formatting;
  later KMP sharing; Rust library+UniFFI; reuse parser/archive crate for a selected
  import workflow; defer. No mandatory Rust or whole-domain rewrite.
- Candidate versions/licences/matrix: sha2=0.11.1 pinned prototype-only,10 external
  packages locked; declared permissive dependency licences/MSRV in report. Actual
  compiler1.99.1/macOS arm64; no minimum-compiler test. UniFFI/csv/ical/age observations
  and unresolved target/licence/security gates in findings; no such crates installed.
- Intended role/replacement: Rust hashing trial replaces only a synthetic subset of
  signature computation in its own executable. JVM baseline compiles actual current
  sources. Nothing replaces app code, JCA, ledger, native APIs or production runtime.
- Costs: native build/release tooling, ABI/FFI copies/errors/lifetime, Swift/Kotlin
  consumer APIs, target packaging/security/debugging and new dependency maintenance.
- Hypothesis/results: same4 golden signatures on host, with exploratory throughput
  measurement. Results are source-bound; no target/FFI/product-speed inference.
  Tests failed before implementation, then passed. Full UTF-16/legacy parity absent.
- Contract/security/error/upgrade gates: X01–X10, especially unchanged stored IDs,
  panic/error/cancellation and hostile-input/restore safety. Hash equality is not
  cryptographic identity protection, privacy compliance or complete system correctness.
- Decision: **retain Kotlin/JVM/native implementation and defer production Rust**.
  Host speed difference is not sufficient without full contracts, measured product
  need/consumer reuse and native packaging. Research closes with bounded evidence.
- Expiry/promotion/removal: revisit when a named non-Kotlin consumer or measured
  parser/formatting/throughput deficiency exists. Refresh candidates then. Remove
  isolated projects and ignored build products if no useful trial remains; do not
  clear global caches. Promotion needs all material gates and retirement of duplicate
  responsibilities; benchmark source must not become a parallel production engine.
- Migration/coexistence/rollback: no app/schema/dependency migration performed.
  Future API facade first preserves all signatures and receipts with read-only
  comparison; one owner/writer only. Never lossy-convert old identifiers or cast
  provider IDs. Retain old readers and recovery path before retiring code.
- Actual validation: host tests/build/format/Clippy and source/document checks only;
  no Android/Apple package, UniFFI binding, iOS runtime, FFI perf, real data or app
  lifecycle acceptance. No production target, permission or dependency changed.
- Follow-up: Track6 may investigate preserving formatter optimization if profiling
  justifies it; Track8 owns shared-core contracts; Track11 selected import/backup
  scope and Track12 consumer feasibility may justify later reuse. No prerequisite
  forces Rust adoption before those tracks can proceed.
