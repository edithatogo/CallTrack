# Kotlin migration and dependency reassessment

Milestone: M5. Type: research-led development. Status: planned discovery; implementation scope provisional.

## Objective

Behaviour-preserving migration with upgrade/contract tests and explicit remaining exceptions.

## Scope and mapping questions

- Map Java modules, coupling and dependency/custom-code responsibility before migration
- Refresh Kotlin ecosystem options and test incremental migration; compare replace versus translate
- Specify migration order, persisted-data compatibility and remaining native responsibilities

## Inputs and dependencies

Prerequisites: [release](../release_20261020/index.md). Requirement references: R14, R26 in [requirements](../../requirements.md). Read [baseline](../../research/baseline-audit.md) and [future research](../../research/future-architecture.md) as dated inputs, not settled designs.

## Required outputs

Produce track-local `findings.md` with sources/versions and uncertainties; `requirements.md` with current/proposed behaviour and acceptance; `decisions.md` using the [dependency template](../../templates/dependency-decision.md); and `verification.md` separating prototype, local and runtime evidence. Create these when work occurs, not as fabricated completed reports now.

## Acceptance

Behaviour-preserving migration with upgrade/contract tests and explicit remaining exceptions. Research must include existing dependencies and code we own that a library could replace. Record prototype-only versus long-term role, promotion/removal trigger, migration and rollback. Refresh this spec and plan from findings before conditional implementation.

## Out of scope

No automatic adoption of shortlisted technology, wholesale rewrite, paid service, unqualified platform parity or public release. Cross-track responsibilities remain with their owning tracks. A negative feasibility result or justified deferral is an acceptable research outcome.

## Refined bounded implementation

Independent preparation and a contract-preserving pure-component migration can
proceed before M4. First translate CallSelection, CallIdentity and CallEvent only,
using mixed Java/Kotlin interoperability. Keep schema2, app identity/version, UI,
provider operations and scheduler unchanged. Preserve exact signatures/fingerprints,
nullable public constructor fields, legacy ASCII trim and overflow semantics.
Further app-owned Java migration and the original upgrade/acceptance gate remain
open; no completion/archive from this pilot alone. See [decision](decisions.md).
