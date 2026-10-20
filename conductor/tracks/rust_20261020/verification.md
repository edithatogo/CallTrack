# Verification and evidence boundaries

Baseline `cf18986`; activation `a0da851`; date2026-10-21.

Red cargo test:3 tests failed on unimplemented signature body. Green:3 Rust tests
passed, including4 Java golden signatures; empty binary/doc suites are not extra
coverage. No production behavior changed. Actual Kotlin CallIdentity/Java CallRecord
sources compile in the standalone JVM baseline and its4 golden assertions pass.

Final benchmarks run sequentially after initial overlapping build/run observations
were discarded.10000 warmup calls,5 samples of10000 calls; checksums match. Raw
samples, source hashes and commands are retained in evidence.json. Host micro-results
are not production/FFI/target superiority or minimum-runtime acceptance.

Rust fmt and Clippy with warnings denied pass. Exact sha2 pin/Cargo.lock and
resolved declared licence/MSRV inventory recorded. Initial release build4.32s and
initial JVM task27s used different cache/build paths; they are not comparable
build/productivity benchmarks. No cold-cache cleanup was performed.

No UniFFI binding, native mobile build, app package/ABI migration, private phone
source, live provider or production runtime change. No memory/battery/size/fuzz or
complete security audit. Valid-Unicode raw-constructor subset only; full Java UTF-16,
legacy constructor, exception and event/ledger parity remain open.

Host/link/format checks and formal review results follow after execution.
