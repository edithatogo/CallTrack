# Rust component evaluation and conditional integration

Track 18; execution stage 7; milestone M8-component. Status: reviewed and complete as a justified deferral of production Rust.

## Objective

Evaluate one demonstrated parsing or portable-backup need against Kotlin/KMP/native alternatives. Adopt a narrow component only after application-level benefit; justified deferral is a valid decision.

## Requirements and discovery

References: R26 in [requirements](../../requirements.md). The user authorised
this roadmap on2026-10-21. Archived research is dated input, not production adoption.

- [Research rust_20261020](../../archive/rust_20261020/index.md)

Each owning track must map actual behaviour/APIs/dependencies and app-owned code,
refresh requirements and record findings/decisions before conditional implementation.
Compare native, retained, library replacement and deferral. A dependency may be a
temporary prototype aid or long-term takeover; record its exact role, licence,
transitives, migration, promotion/removal criteria and recovery. No automatic
prototype-version inheritance.

## Implementation and acceptance

- Identify an actual consumer/bottleneck and baseline; refresh crate/toolchain/licence/transitive maintenance evidence and compare owned Kotlin, libraries and deferral.
- Prototype bounded import parsing or a selected backup format only if it resolves the named uncertainty; compare UniFFI and direct bindings including Kotlin/Native access, not JVM-only bindings.
- Test full input contracts, hostile bounds/fuzzing, panic/error/cancellation/lifetime handling and target/ABI packaging; measure end-to-end including FFI copies/build cost.
- Promote only with documented material benefit and retirement of duplicate responsibility, preserving receipts/old readers/recovery; otherwise record deferral/removal and leave Kotlin selected.

Preserve golden identity/rendering/null/time/overflow fixtures and human edits.
Separate shared tests, rendered tests, native provider readback, cloud outcomes and
operational observation. No schema/signature/namespace change by incidental
extraction; qualify any necessary later migration explicitly. Review, fix and
verify before closure; no research-only or compiler-only production acceptance.

## Boundaries

No automatic signing/publication/cloud consent/physical-device authority. Retain
working outputs until parity/recovery. Windows/Linux are outside this sequence.
iOS implementation is stage9 and cannot be advanced past unresolved stages1–8.
A justified deferral must state reason, evidence, consequences and reconsideration
trigger; it cannot turn unavailable features into supported claims.
