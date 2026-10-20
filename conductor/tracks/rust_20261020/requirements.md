# Rust adoption and reusable-component requirements

R26 refinement. Production gates remain open despite a passing subset experiment.

| ID | Adoption gate | Current evidence |
|---|---|---|
| X01 | Name independent consumers or a profiled deficiency requiring shared native work | One current Android app; other consumers hypothetical, no app bottleneck |
| X02 | Exact persisted identity/encoding/overflow/error/legacy-call contracts on each consumer |4 raw-constructor valid-Unicode goldens only; invalid UTF-16/legacy directions/full mapping unproved |
| X03 | Compare retained/improved Kotlin/native/library options before rewrite | Host signature comparison; preserving Kotlin formatting improvement not benchmarked |
| X04 | Resolve target-specific generator/runtime/ABI/cancellation/lifetime/thread/error/panic boundaries | No UniFFI bindings or FFI execution; docs only |
| X05 | Reproducible Android and chosen Apple device/simulator packaging/upgrade/debug symbols and source-bound tests | Host macOS binary only; no native app packages |
| X06 | Measure end-to-end cold/warm latency, CPU/memory/battery, distribution size and real/large synthetic workload | Exploratory warmed host micro-measurement only; no product advantage claim |
| X07 | Pin Rust/MSRV, lock/checksums, features, libraries/licences and notices; track maintenance/security changes | sha2 exact pin + lock + metadata; no complete security/notice audit |
| X08 | Parser/archive trials handle hostile/oversize/malformed input and corruption with fuzz/resource limits and consumer interoperability | Candidate research only; no parser/crypto acceptance |
| X09 | Preserve schema1/2 receipts and a single ledger owner, version unsupported data, reversible migration without dual writes | No production migration or competing ledger introduced |
| X10 | Prototype expiry/removal or promotion is explicit; memory safety does not prove permissions/durability/privacy | Prototype-only; reopen on concrete use/deficiency and complete gates |

Before bridge adoption, compare exact UTF-16 code units and UTF-8 replacement policy
with Java for unpaired surrogates; never silently use String::from_utf16_lossy.
Test arbitrary legacy direction, null receiver/object/fields, delimiters, large
lengths, signed range limits, supplementary Unicode, constructor validation order
and full stored source/event fingerprints. A stricter input API is not compatible
merely because it is easier to bind. Any rejection must preserve existing history
and provide explicit migration/review, not a fresh identity that duplicates events.

Use the same golden input/expected corpus through Kotlin and Swift bindings,
including allocation ownership, callback races, cancellation after side effects,
panic/error translation and repeated load/unload/recreation. Swift concurrency
compatibility must be qualified on actual selected compiler flags. KMP and Rust
must not own competing domain/delivery state; choose one authoritative component.
