# Rust and reusable component evaluation

Milestone: Before M5/M7 decisions. Type: research-led development. Status: planned discovery; implementation scope provisional.

## Objective

Adopt/retain/reject/defer decision based on contracts, packaging and material benefit; no mandatory Rust rewrite.

## Scope and mapping questions

- Identify concrete cross-application consumers and current custom/native components potentially replaceable by libraries
- Research maintained Rust libraries, UniFFI target support, licences and packaging versus Kotlin/KMP alternatives
- Benchmark one bounded component when justified; document temporary versus production role, exit criteria and costs

## Inputs and dependencies

Prerequisites: [build](../build_20261020/index.md). Requirement references: R26 in [requirements](../../requirements.md). Read [baseline](../../research/baseline-audit.md) and [future research](../../research/future-architecture.md) as dated inputs, not settled designs.

## Required outputs

Produce track-local `findings.md` with sources/versions and uncertainties; `requirements.md` with current/proposed behaviour and acceptance; `decisions.md` using the [dependency template](../../templates/dependency-decision.md); and `verification.md` separating prototype, local and runtime evidence. Create these when work occurs, not as fabricated completed reports now.

## Acceptance

Adopt/retain/reject/defer decision based on contracts, packaging and material benefit; no mandatory Rust rewrite. Research must include existing dependencies and code we own that a library could replace. Record prototype-only versus long-term role, promotion/removal trigger, migration and rollback. Refresh this spec and plan from findings before conditional implementation.

## Out of scope

No automatic adoption of shortlisted technology, wholesale rewrite, paid service, unqualified platform parity or public release. Cross-track responsibilities remain with their owning tracks. A negative feasibility result or justified deferral is an acceptable research outcome.

## Bounded experiment selected2026-10-21

Compare a Rust SHA256 source-signature trial with the actual current Kotlin
CallIdentity/Java CallRecord sources, using the four existing Java golden fixtures.
Use standalone projects outside root settings. Measure warmed host-only signature
throughput and record toolchains/input hashes; this excludes FFI/mobile/battery and
cannot establish production advantage. Rust trial accepts valid Unicode strings
and raw-type constructor semantics only; full Java UTF-16/null/legacy API parity
must not be inferred. No production integration or UniFFI adoption is selected.
