# Rust source-signature experiment

Standalone, unpublished host experiment. It handles valid Unicode strings and the
raw-type CallRecord constructor subset only. It is **not a drop-in replacement**:
Java unpaired UTF-16, arbitrary legacy direction, object/null exceptions, event
mapping and all native/FFI packaging remain unqualified. No app uses this crate.

From the repository root, using the installed Rust toolchain:

```sh
cargo test --locked --manifest-path prototypes/rust-identity/Cargo.toml --target-dir prototypes/rust-identity/build
cargo fmt --check --manifest-path prototypes/rust-identity/Cargo.toml
cargo clippy --locked --all-targets --manifest-path prototypes/rust-identity/Cargo.toml --target-dir prototypes/rust-identity/build -- -D warnings
cargo run --release --locked --manifest-path prototypes/rust-identity/Cargo.toml --target-dir prototypes/rust-identity/build
```

Three tests include the four source-signature goldens already used by the app's
Java migration contracts. The binary measures five batches of10000 signatures after
10000 warmup calls, consuming outputs via checksum and black_box. Only synthetic
constants are used; no files, contacts, call logs or network clients are read.
Cargo may fetch the checksum-pinned build dependencies; the program has no network
operations. Keep build output in the ignored build directory as shown.

For comparison run the [actual JVM-source baseline](../rust-jvm-baseline/README.md)
sequentially, after the Rust process exits. These are exploratory host measurements,
not controlled statistically or a Kotlin/Native, Android, FFI, battery, footprint or
application-throughput benchmark. Different formatting implementations matter.

sha2=0.11.1 is prototype-only; Cargo.lock pins its graph. Observed compiler1.99.1;
sha2 declares minimum1.88, which was not independently tested here. Release/MSRV,
licence/notice/security and native ABI support must be rechecked before any adoption.
The project itself follows the root GPLv3 licence; dependency terms remain separate.

[Track decision](../../conductor/tracks/rust_20261020/decisions.md) retains Kotlin and
defers production Rust. Revisit on a demonstrated consumer/performance need; retire
both isolated experiments/build outputs if no longer useful. No global cache or
Android app cleanup/migration is needed.
