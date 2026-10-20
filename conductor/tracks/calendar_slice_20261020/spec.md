# Call-to-calendar vertical slice

Milestone: M2. Type: research-led development. Status: planned discovery; implementation scope provisional.

## Objective

One event accurately persisted and read back, with denied-permission behaviour demonstrated.

## Scope and mapping questions

- Map call source and writable-calendar contracts, including Outlook-backed device calendars
- Compare direct provider APIs with maintained wrappers; prototype one synthetic/controlled event
- Define source identity, ledger boundary and persistence/readback acceptance

## Inputs and dependencies

Prerequisites: [build](../build_20261020/index.md). Requirement references: R02–R04, R08, R10, R19 in [requirements](../../requirements.md). Read [baseline](../../research/baseline-audit.md) and [future research](../../research/future-architecture.md) as dated inputs, not settled designs.

## Required outputs

Produce track-local `findings.md` with sources/versions and uncertainties; `requirements.md` with current/proposed behaviour and acceptance; `decisions.md` using the [dependency template](../../templates/dependency-decision.md); and `verification.md` separating prototype, local and runtime evidence. Create these when work occurs, not as fabricated completed reports now.

## Acceptance

One event accurately persisted and read back, with denied-permission behaviour demonstrated. Research must include existing dependencies and code we own that a library could replace. Record prototype-only versus long-term role, promotion/removal trigger, migration and rollback. Refresh this spec and plan from findings before conditional implementation.

## Out of scope

No automatic adoption of shortlisted technology, wholesale rewrite, paid service, unqualified platform parity or public release. Cross-track responsibilities remain with their owning tracks. A negative feasibility result or justified deferral is an acceptable research outcome.
