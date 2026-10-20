# Fork feasibility and build qualification

Milestone: M1. Type: research-led development. Status: planned discovery; implementation scope provisional.

## Objective

Reproducible build, bounded device evidence and documented fork decision.

## Scope and mapping questions

- Reproduce upstream build and assess modern SDK/permission constraints
- Compare fork repair with fresh implementation; inventory library versus custom-code replacement opportunities
- Qualify JDK/Gradle/AGP/SDK, formatting and CI commands; record package/signing strategy

## Inputs and dependencies

Prerequisites: [baseline](../baseline_20261020/index.md). Requirement references: R08, R13, R26 in [requirements](../../requirements.md). Read [baseline](../../research/baseline-audit.md) and [future research](../../research/future-architecture.md) as dated inputs, not settled designs.

## Required outputs

Produce track-local `findings.md` with sources/versions and uncertainties; `requirements.md` with current/proposed behaviour and acceptance; `decisions.md` using the [dependency template](../../templates/dependency-decision.md); and `verification.md` separating prototype, local and runtime evidence. Create these when work occurs, not as fabricated completed reports now.

## Acceptance

Reproducible build, bounded device evidence and documented fork decision. Research must include existing dependencies and code we own that a library could replace. Record prototype-only versus long-term role, promotion/removal trigger, migration and rollback. Refresh this spec and plan from findings before conditional implementation.

## Out of scope

No automatic adoption of shortlisted technology, wholesale rewrite, paid service, unqualified platform parity or public release. Cross-track responsibilities remain with their owning tracks. A negative feasibility result or justified deferral is an acceptable research outcome.
