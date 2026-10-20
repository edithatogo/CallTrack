# Community distribution and release readiness

Milestone: M4. Type: research-led development. Status: discovery and community preparation in progress; release candidate blocked on M3.

## Objective

Source-matched release candidate, contributor instructions and recorded operational acceptance.

## Scope and mapping questions

- Research distribution eligibility, licensing and reproducible release/signing options
- Map contributor needs, dependency inventory and privacy/data lifecycle
- Specify alpha observation, update/migration/rollback and supported-device criteria

## Inputs and dependencies

Prerequisites: [parity](../parity_20261020/index.md). Requirement references: R13, R27 in [requirements](../../requirements.md). Read [baseline](../../research/baseline-audit.md) and [future research](../../research/future-architecture.md) as dated inputs, not settled designs.

## Required outputs

Produce track-local `findings.md` with sources/versions and uncertainties; `requirements.md` with current/proposed behaviour and acceptance; `decisions.md` using the [dependency template](../../templates/dependency-decision.md); and `verification.md` separating prototype, local and runtime evidence. Create these when work occurs, not as fabricated completed reports now.

## Acceptance

Source-matched release candidate, contributor instructions and recorded operational acceptance. Research must include existing dependencies and code we own that a library could replace. Record prototype-only versus long-term role, promotion/removal trigger, migration and rollback. Refresh this spec and plan from findings before conditional implementation.

## Out of scope

No automatic adoption of shortlisted technology, wholesale rewrite, paid service, unqualified platform parity or public release. Cross-track responsibilities remain with their owning tracks. A negative feasibility result or justified deferral is an acceptable research outcome.

## Refined execution scope (2026-10-20)

Discovery selected native tooling and repository documentation/templates without
new dependencies. Findings and D01–D09 define the candidate gates. Contributor and
release preparation may proceed before M3. Candidate signing, reproduction, upgrade
and operational acceptance are deferred until their prerequisites exist. This
refinement does not waive the original objective or permit completion/archive on
documents alone. See [verification](verification.md) for the current boundary.
