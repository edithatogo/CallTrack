# Rust/reusable-component findings

Refreshed2026-10-21; baseline `cf18986`, activation `a0da851`. This is a bounded
host experiment and adoption assessment, not a Rust application rewrite.

## Consumers and responsibilities

Only the Android development app is a current production-direction consumer. The
desktop Compose experiment, proposed macOS/iOS companion, import CLI and possible
other community applications are potential consumers, not committed independent
users of a shared Rust library. The experiment's Rust executable is a test harness,
not proof of a second real product. No app performance bottleneck has been profiled.

| Component | Keep/native/Kotlin alternative | Rust/library alternative and scope |
|---|---|---|
| Source signatures / event fingerprints | Existing Kotlin/JVM mapper and JCA hashing; optimize locally if a measured gap appears | RustCrypto sha2 could implement hashing; persisted encoding contracts and ABI cost dominate adoption gate |
| Import/export CSV | Maintained JVM serializer (Track11) or bounded native implementation | csv crate candidate replaces handwritten parsing; does not decide schema/formula/restore trust policies |
| ICS | Native calendar editor or maintained JVM parser | ical crate candidate; recurrence/timezone/resource-limit/fuzz/interop coverage still required |
| Encrypted portable archive | Reviewed platform/library encryption after recovery design | age candidate provides a defined file format; does not solve key recovery or safe restore; no custom cryptography |
| Outbox/dedup engine | Existing Java/Kotlin state/SQLite with preserving migration | Rust state engine adds duplicated ownership and FFI/cancellation risk; no demonstrated justification |
| Provider/auth/scheduler | Native Android/EventKit/OS/auth libraries | Rust wrappers cannot grant permissions, entitlements or background guarantees; retain native boundaries |

## Candidate/library refresh

| Candidate / observed version | Role / maintenance and licence evidence | Disposition |
|---|---|---|
| sha2 0.11.1 | RustCrypto SHA256; MIT OR Apache-2.0, declared Rust1.88 minimum in resolved metadata | Exact pinned prototype-only dependency, no app adoption |
| UniFFI0.32.2 | Kotlin/Swift/Python binding generation; shipping binaries is separate | Documentation researched; not installed/generated/packaged |
| csv1.4.0 | Reader/writer, StringRecord versus ByteRecord behavior | Uninstalled candidate; full import/schema/error contracts and exact licence graph at trial |
| ical0.11.0 | ICS parser; rendered docs retain old0.7 examples | Documentation freshness mismatch is a qualification concern; no serializer/recurrence fidelity implied |
| age0.12.1 | Specified encrypted-file interoperability; docs label pre1.0 crate versions beta/testing | Defer production backup dependency; independent format/key/recovery/security review required |

UniFFI's Kotlin Gradle documentation uses JNA and coroutines for async integration;
this is not proof of a commonMain/Kotlin-Native binding. Generated Swift support
has documented Swift6/Sendable caveats. Verify exact generator/runtime compatibility,
errors, panic boundaries, object lifetime, cancellation and target consumers. Do
not copy old Gradle snippets into the current AGP build without qualification.

Android requires target/NDK/API/ABI build and native library packaging; Apple needs
separate device/simulator binaries/framework/module integration and signing tests.
A host cargo binary proves none of those. The installed rustup target list contains
macOS arm64, Linux x64 and wasm32, but only macOS host was built here; installed
target presence is not a successful cross build. No NDK/iOS toolchain was installed.

## Bounded source-signature experiment

The Rust trial mirrors the **raw-type constructor / valid-Unicode subset** of
CallIdentity.signature; it uses UTF-16 code-unit lengths and SHA256 over UTF-8.
Three Rust tests cover4 existing Java goldens, invalid identity/time rejection and
null-versus-empty number distinction. Tests first failed on the unimplemented body,
then passed. It does not implement CallEvent/fingerprint, ledger, FFI or full API.

The independent JVM baseline compiles the actual current CallIdentity.kt and
CallRecord.java sources, checks the same4 goldens and measures the same four cases.
No copied replacement is substituted for the existing JVM implementation. Both
consume result bytes via a checksum; Rust also uses black_box. Root sources and
prior projects remain unchanged. Standalone Kotlin2.3.21/JDK17/Gradle8.13 and
Rust1.99.1 were observed. Cargo.lock pins10 external packages with registry checksums;
[dependency metadata](dependency-report.json) records declared licences/MSRV.

After discarding initial overlapping build/run observations, final runs were
sequential,10000 warmup calls and5 samples of10000 calls per process. Results are
in evidence.json. These are exploratory warmed host observations, not a statistically
controlled benchmark or universal language comparison. No FFI, cold-start, battery,
allocations/memory, Android/iOS ABI, APK size or real import/provider latency measured.
The JVM implementation does per-byte String.format, unlike Rust formatting; compare
a preserving Kotlin formatting optimization before attributing benefit to language.
No build-time advantage is inferred from different cache/toolchain timings.

## Compatibility gap and adoption result

Rust str cannot represent Java unpaired UTF-16 surrogates. A lossy conversion can
change persistent identities, so a production bridge must preserve exact input
semantics or explicitly reject/version unsupported cases without losing receipts.
The current Java legacy constructor may preserve arbitrary direction strings;
the trial derives labels from raw type and does not cover that API. Null-object
exception ordering, caller APIs and event mapping are also unproved. Four matching
hashes therefore do not establish a drop-in replacement or all-data migration.

**Retain Kotlin/JVM/native ownership; defer Rust production adoption.** The tested
component is faster in this host run, but no complete parity, target package, actual
app bottleneck or second production consumer justifies a native boundary now.
Retain the isolated trial for a later measured reuse need; do not promote it by
accident. Parser/archive libraries may be better future candidates than a competing
sync engine, after selected Track11 workflow and real dependency comparison.

## Primary sources, refreshed2026-10-21

- [sha2 0.11.1](https://docs.rs/sha2/0.11.1/sha2/): maintained hash implementation, algorithms/licences/backends; no claim of audit or end-to-end app security.
- [UniFFI0.32.2](https://docs.rs/uniffi/0.32.2/uniffi/), [guide scope](https://mozilla.github.io/uniffi-rs/latest/), [Kotlin Gradle](https://mozilla.github.io/uniffi-rs/latest/kotlin/gradle.html), [Swift bindings](https://mozilla.github.io/uniffi-rs/latest/swift/overview.html).
- [Rust Android targets](https://doc.rust-lang.org/rustc/platform-support/android.html) document toolchain requirements, not a successful app build.
- [csv1.4.0](https://docs.rs/csv/1.4.0/csv/), [ical0.11.0](https://docs.rs/ical/0.11.0/ical/), [age0.12.1](https://docs.rs/age/0.12.1/age/).

UniFFI's GitHub releases route did not list a release; the published crate docs
provided the observed version. A Kotlin overview URL failed; linked official
configuration/Gradle pages were used instead. No undocumented target-support
assumption relies on failed pages. Uninstalled candidate licences/transitives and
security status remain adoption gates, not completed audits. Production distribution
requires a notice/licence review beyond metadata; no crypto/key-management guarantee
or complete vulnerability assessment is claimed.

Final sample medians: Rust0.954 microseconds/signature; JVM22.006 microseconds/signature. Large JVM sample variation and host noise remain visible in raw samples; no product speedup is claimed.
