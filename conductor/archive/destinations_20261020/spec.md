# Calendar and service integration discovery

Milestone: Post-M3. Type: research-led development. Status: planned discovery; implementation scope provisional.

## Objective

Provider-specific evidence and contracts; implement only justified connectors with end-to-end readback.

## Scope and mapping questions

- Map Outlook/Google/CalDAV needs and native-provider coverage before direct connectors
- Research Graph/Google/CalDAV APIs, auth libraries, maintained clients and temporary connector prototypes
- Specify conflict, destination identity, rate-limit, deletion and loop-prevention requirements

## Inputs and dependencies

Prerequisites: [parity](../../tracks/parity_20261020/index.md). Requirement references: R12, R19, R20, R25 in [requirements](../../requirements.md). Read [baseline](../../research/baseline-audit.md) and [future research](../../research/future-architecture.md) as dated inputs, not settled designs.

## Required outputs

Produce track-local `findings.md` with sources/versions and uncertainties; `requirements.md` with current/proposed behaviour and acceptance; `decisions.md` using the [dependency template](../../templates/dependency-decision.md); and `verification.md` separating prototype, local and runtime evidence. Create these when work occurs, not as fabricated completed reports now.

## Acceptance

Provider-specific evidence and contracts; implement only justified connectors with end-to-end readback. Research must include existing dependencies and code we own that a library could replace. Record prototype-only versus long-term role, promotion/removal trigger, migration and rollback. Refresh this spec and plan from findings before conditional implementation.

## Out of scope

No automatic adoption of shortlisted technology, wholesale rewrite, paid service, unqualified platform parity or public release. Cross-track responsibilities remain with their owning tracks. A negative feasibility result or justified deferral is an acceptable research outcome.

## Research disposition,2026-10-21

[INT-D01](decisions.md) retains existing CalendarContract coverage and defers direct
connectors until a measured native-route gap and stable Track4 contracts justify one.
[I01–I13](requirements.md) and the [qualification protocol](qualification.md) define
provider-specific contracts and future end-to-end evidence. Research may close on
this justified deferral. Provider/cloud acceptance remains unmet; no connector or
mock prototype is selected. No app permissions or dependency changes are needed.
