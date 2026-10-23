# Compose Multiplatform product and UI discovery

Milestone: M8. Type: research-led development. Status: planned discovery; implementation scope provisional.

## Objective

Decision supported by second-platform prototype; production migration only if justified.

## Scope and mapping questions

- Map iOS/desktop workflows and platform differences without assuming Android capture parity
- Compare Compose Multiplatform with native UI and prototype representative interactions
- Document target scope, accessibility, packaging, state and support acceptance

## Inputs and dependencies

Prerequisites: [compose](../../archive/compose_20261020/index.md), [kmp](../kmp_20261020/index.md). Requirement references: R17, R24 in [requirements](../../requirements.md). Read [baseline](../../research/baseline-audit.md) and [future research](../../research/future-architecture.md) as dated inputs, not settled designs.

## Required outputs

Produce track-local `findings.md` with sources/versions and uncertainties; `requirements.md` with current/proposed behaviour and acceptance; `decisions.md` using the [dependency template](../../templates/dependency-decision.md); and `verification.md` separating prototype, local and runtime evidence. Create these when work occurs, not as fabricated completed reports now.

## Acceptance

Decision supported by second-platform prototype; production migration only if justified. Research must include existing dependencies and code we own that a library could replace. Record prototype-only versus long-term role, promotion/removal trigger, migration and rollback. Refresh this spec and plan from findings before conditional implementation.

## Out of scope

No automatic adoption of shortlisted technology, wholesale rewrite, paid service, unqualified platform parity or public release. Cross-track responsibilities remain with their owning tracks. A negative feasibility result or justified deferral is an acceptable research outcome.

## Android UI prerequisite clarification

The [Compose research decision](../../archive/compose_20261020/decisions.md) retains Views in
the working Android app; a completed research track does not mean an Android
Compose UI exists. At activation, map the actual Android/second-platform workflows
and qualify the UI/state adoption requirements afresh before proposing shared UI.
