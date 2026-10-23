# Track 18 findings — refreshed 2026-10-24

## Actual consumers and code ownership

| Area | Current implementation | Rust opportunity and boundary |
|---|---|---|
| Android call identity | `app/src/main/kotlin/com/markobl/calllogsync/slice/CallIdentity.kt` uses JCA SHA-256; stable values contribute to persisted receipts and duplicate prevention | Existing Rust trial matches only the valid-Unicode raw-constructor subset. No measured app bottleneck and no reason to cross FFI for this operation. Do not change identity bytes or receipt ownership. |
| Android historical import | Native CallLog query, Kotlin mapping, preview, explicit confirmation, SQLite ledger and CalendarContract writes | Android provider access, user confirmation, persistence and calendar writes must remain native. No Rust layer should own this workflow. |
| macOS import | `prototypes/macos-companion` is a Foundation-only JSON preview parser with strict byte/row/time bounds and synthetic XCTest cases | It is not a packaged app or supported import format. No Android exporter emits this schema, and no committed second production consumer exists. Rust `serde_json` is a possible later parser only after format and producer acceptance. |
| Shared Kotlin/UI | Track15 has a limited common selection/time-range core; Track17 has a desktop-only UI prototype. Neither is native cross-platform acceptance. | A Rust library now would introduce a competing parser/core before KMP contracts and a second actual consumer are established. |

## Refreshed candidates and compatibility

- The isolated Rust identity experiment remains unpublished and is not linked by
  the app. On this host, Rust/Cargo `1.99.1` builds its locked graph. A clean
  isolated `cargo test --locked` passed 3 tests; `cargo fmt --check` and
  warnings-denied Clippy passed. The graph contains the existing prototype's
  `sha2 0.11.1` plus 9 transitives; Cargo metadata declares MIT/Apache-2.0 for
  those crates, while `sha2` declares Rust 1.88 minimum. This is not a new
  dependency proposal or complete licence/security review.
- `serde_json 1.0.154` is a maintained candidate for a *future selected JSON
  contract*, not adopted here. Its upstream describes typed/byte-oriented JSON
  parsing and offers MIT OR Apache-2.0. No exact Rust target graph, hostile-input
  test, licence inventory or security review was run because there is no agreed
  producer/consumer contract to implement.
- Mozilla UniFFI provides official Swift and Kotlin/JVM binding generation; its
  documentation does not itself ship cross-target binaries. The old Android
  Gradle guide uses JNA and coroutines and has an open upstream report that the
  documented `libraryVariants` route is outdated for current AGP.
- A separate UniFFI Kotlin Multiplatform plugin has a signed v1.3.2 release
  (2026-10-09), and its changelog records fixes for Kotlin/Native cinterop and
  Kotlin 2.4.x. Its current build requirements are Gradle 9.6.1+, Kotlin 2.4+,
  and AGP 9.x for Android. The project's qualified app baseline is Gradle 8.13,
  Kotlin 2.3.21 and AGP 8.13.2; the Track17 desktop prototype uses Kotlin 2.4.21
  but still Gradle 8.13 and no Android target. Adoption therefore requires a
  separately qualified build migration and native ABI packaging. Its README's
  older status paragraph still mentions UniFFI 0.28.3, while its changelog and
  recent releases move beyond that; exact tag-pinned documentation and generated
  code need direct acceptance before relying on it.
- The existing Swift parser uses Foundation `JSONDecoder`, no third-party
  runtime and a deliberate preview-only contract. Kotlin can retain native
  parsing or choose a maintained Kotlin serialization route once an Android
  exporter exists. No evidence yet shows that another language reduces total
  maintenance, unsafe-input risk or end-to-end import cost.

## Decision-relevant gaps

The first genuine candidate is sharing a future portable import format between
Android and Apple platforms. Today that candidate lacks an Android exporter, a
supported format/version decision, an accepted consumer, and a qualified KMP
native build. Replaying the archived SHA benchmark would not answer those gaps.
The proper next evidence is a stable cross-platform format and a Kotlin/KMP
consumer; then compare Kotlin/native parsing, a maintained library, and a bounded
Rust parser using the same hostile/golden fixtures and actual app integration.

The complete archived findings remain at
`../../archive/rust_20261020/findings.md`; this refresh does not supersede its
UTF-16 identity caveats, benchmark limits, ABI risks, or production deferral.
