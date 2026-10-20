# iOS and desktop acquisition feasibility

Milestone: Early research. Type: research-led development. Status: planned discovery; implementation scope provisional.

## Objective

Evidence-backed capability matrix and useful second-platform scope; rejection/deferral is valid.

## Scope and mapping questions

- Research current supported iOS/desktop call, calendar, import and background APIs
- Map manual/import/companion workflows separately from automatic capture and platform entitlement limits
- Prototype only the uncertain capability with synthetic data and document feasible requirements

## Inputs and dependencies

Prerequisites: [baseline](../baseline_20261020/index.md). Requirement references: R24 in [requirements](../../requirements.md). Read [baseline](../../research/baseline-audit.md) and [future research](../../research/future-architecture.md) as dated inputs, not settled designs.

## Required outputs

Produce track-local `findings.md` with sources/versions and uncertainties; `requirements.md` with current/proposed behaviour and acceptance; `decisions.md` using the [dependency template](../../templates/dependency-decision.md); and `verification.md` separating prototype, local and runtime evidence. Create these when work occurs, not as fabricated completed reports now.

## Acceptance

Evidence-backed capability matrix and useful second-platform scope; rejection/deferral is valid. Research must include existing dependencies and code we own that a library could replace. Record prototype-only versus long-term role, promotion/removal trigger, migration and rollback. Refresh this spec and plan from findings before conditional implementation.

## Out of scope

No automatic adoption of shortlisted technology, wholesale rewrite, paid service, unqualified platform parity or public release. Cross-track responsibilities remain with their owning tracks. A negative feasibility result or justified deferral is an acceptable research outcome.
