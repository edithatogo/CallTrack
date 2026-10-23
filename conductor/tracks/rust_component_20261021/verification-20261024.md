# Track 18 verification — 2026-10-24

## Rust host prototype

Using the installed host toolchain `rustc 1.99.1` / `cargo 1.99.1`, the existing
isolated identity prototype was rebuilt into ignored `.local/rust18-reuse`:

```sh
cargo test --locked --manifest-path prototypes/rust-identity/Cargo.toml --target-dir .local/rust18-reuse
cargo fmt --check --manifest-path prototypes/rust-identity/Cargo.toml
cargo clippy --locked --all-targets --manifest-path prototypes/rust-identity/Cargo.toml --target-dir .local/rust18-reuse -- -D warnings
cargo tree --locked --manifest-path prototypes/rust-identity/Cargo.toml
```

Results: all 3 existing Rust tests passed, formatting passed, Clippy passed with
warnings denied, and the locked tree contains the prototype plus `sha2 0.11.1`
and 9 transitive packages. Cargo metadata declares permissive MIT/Apache-2.0
terms for the external packages; `sha2` declares Rust1.88 minimum. This is a
host prototype refresh, not Android/Apple build proof, FFI qualification,
complete licence/security review or a product benchmark. No prototype source or
Cargo lock changed.

## Decision and boundary

No parser prototype was added because the only available JSON schema belongs to
a preview-only macOS source and has no Android exporter or supported format
claim. No production Rust library, dependency, target, app build, provider,
permission, data, identity, receipt or calendar writer changed. There is no
app-level failing regression to add for a deferred feature. A conductor
regression test verifies that a completed justified Track18 deferral does not
allow Track19 while Tracks14–17 remain unresolved.

Full macOS package, emulator, remote-calendar, operational and seven-day tests
are not applicable to a no-integration Rust decision and were not attempted.
