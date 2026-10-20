# Android Compose discovery and migration

Milestone: M6. Type: research-led development. Status: planned discovery; implementation scope provisional.

## Objective

Evidence-backed UI decision; adopted screens match behavioural and accessibility acceptance.

## Scope and mapping questions

- Map current screens, accessibility, state and navigation requirements
- Research current Compose tooling/libraries versus retaining Views; prototype bounded screens
- Specify staged coexistence, UI acceptance and replacement/removal criteria

## Inputs and dependencies

Prerequisites: [kotlin](../kotlin_20261020/index.md). Requirement references: R15, R26 in [requirements](../../requirements.md). Read [baseline](../../research/baseline-audit.md) and [future research](../../research/future-architecture.md) as dated inputs, not settled designs.

## Required outputs

Produce track-local `findings.md` with sources/versions and uncertainties; `requirements.md` with current/proposed behaviour and acceptance; `decisions.md` using the [dependency template](../../templates/dependency-decision.md); and `verification.md` separating prototype, local and runtime evidence. Create these when work occurs, not as fabricated completed reports now.

## Acceptance

Evidence-backed UI decision; adopted screens match behavioural and accessibility acceptance. Research must include existing dependencies and code we own that a library could replace. Record prototype-only versus long-term role, promotion/removal trigger, migration and rollback. Refresh this spec and plan from findings before conditional implementation.

## Out of scope

No automatic adoption of shortlisted technology, wholesale rewrite, paid service, unqualified platform parity or public release. Cross-track responsibilities remain with their owning tracks. A negative feasibility result or justified deferral is an acceptable research outcome.

## Refined experiment scope (2026-10-21)

Use a standalone synthetic Compose project to resolve UI/state/semantics and
dependency questions. No production screen is adopted by building a prototype.
Make a final evidence-backed retain/adopt/defer decision after tests; adopted
screens still require behavioural/accessibility parity. A justified deferral can
close the research track under its existing out-of-scope policy, with production
work and reopening triggers explicitly retained. See [decision](decisions.md).
