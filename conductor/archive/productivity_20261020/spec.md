# Call journal and productivity requirements

Milestone: Post-M4. Type: research-led development. Status: planned discovery; implementation scope provisional.

## Objective

Prioritised validated feature slices and dependency decisions before each implementation.

## Scope and mapping questions

- Map notes, tags, reminders, routing, search, exports and timesheet workflows with user priorities
- Research reusable search/export/backup/rules components and existing native facilities
- Document prioritised feature specs, privacy/data ownership and acceptance; defer unneeded features

## Inputs and dependencies

Prerequisites: [release](../../tracks/release_20261020/index.md). Requirement references: R18, R21–R23, R27 in [requirements](../../requirements.md). Read [baseline](../../research/baseline-audit.md) and [future research](../../research/future-architecture.md) as dated inputs, not settled designs.

## Required outputs

Produce track-local `findings.md` with sources/versions and uncertainties; `requirements.md` with current/proposed behaviour and acceptance; `decisions.md` using the [dependency template](../../templates/dependency-decision.md); and `verification.md` separating prototype, local and runtime evidence. Create these when work occurs, not as fabricated completed reports now.

## Acceptance

Prioritised validated feature slices and dependency decisions before each implementation. Research must include existing dependencies and code we own that a library could replace. Record prototype-only versus long-term role, promotion/removal trigger, migration and rollback. Refresh this spec and plan from findings before conditional implementation.

## Out of scope

No automatic adoption of shortlisted technology, wholesale rewrite, paid service, unqualified platform parity or public release. Cross-track responsibilities remain with their owning tracks. A negative feasibility result or justified deferral is an acceptable research outcome.

## Refined research disposition,2026-10-21

[PROD-D01](decisions.md) defers production features pending selected user workflow,
retention/recovery decisions and Track4/5 acceptance. [J01–J12](requirements.md)
map candidate slices and future validation. Ordering is provisional unless a user
answer is recorded. Research may close with this justified deferral; no journal or
validated product-feature acceptance is claimed and no prototype is selected.
