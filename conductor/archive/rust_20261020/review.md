# Review Report: Rust and reusable component evaluation

## Summary

The isolated subset/host measurements support retaining Kotlin and deferring production Rust, with full parity and native packaging gates explicit.

## Verification Checks

- [x] **Plan Compliance**: Yes for bounded research/experiment; production adoption deferred.
- [x] **Style Compliance**: Pass; rustfmt/Clippy warnings denied and ktfmt0.59.
- [x] **New Tests**: Yes;3 Rust tests and4 actual JVM-source golden assertions.
- [x] **Test Coverage**: Partial; valid-Unicode/raw-constructor signature subset only, no FFI/mobile/whole-mapper acceptance.
- [x] **Test Results**: Passed:3 Rust tests,4 JVM golden assertions,25 host tests, rustfmt/Clippy/ktfmt,118 document/13 track/25 scenario Conductor checks and private digests.

## Findings

### Low: Benchmark provenance needed exact tool builds and measured artifact hashes

- **File**: `conductor/tracks/rust_20261020/evidence.json` (toolchains/source_inputs)
- **Context**: Major tool versions and source hashes were recorded, but JVM patch/vendor and exact measured binary/class artifacts improve later reproducibility and prevent mistaking a rebuild for the measured run.
- **Suggestion** (applied): add complete compiler/JDK/host details and SHA256 for the existing measured release binary and JVM classes, without rerunning or replacing the observations.

```diff
+ toolchain_details: exact rustc/cargo/Java/macOS/architecture readback
+ artifact_sha256: measured Rust binary and JVM compiled classes
```

Scope `a0da851^..512edaf`:364 non-Conductor lines across9 files including lockfile.
Reviewed sources/build/README files individually; lock/metadata checked for exact
versions, checksums and declared licences. No Android/app source dependency was
changed. No unsafe application code, secrets, personal fixtures, network clients,
phone/provider access or FFI integration added. Full Java compatibility limitations
are intentional experiment boundaries, not waived production gates. Micro-results
are not a controlled language/product-speed comparison. No unresolved finding.
