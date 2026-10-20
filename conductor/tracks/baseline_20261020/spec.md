# CallTrack and FOSS capability mapping

Milestone: M0. Type: research-led development. Status: static mapping deliverables implemented; final review pending.

## Objective

Traceable baseline and acceptance specification; unverified behaviour remains labelled.

## Scope and mapping questions

- Complete APK preference, API, event-format and scan-limit mapping; distinguish static inference from runtime proof
- Map both pinned FOSS sources including dependencies and custom code responsibilities
- Define synthetic parity fixtures, user outcomes and unresolved requirement decisions

## Inputs and dependencies

Prerequisites: None. Requirement references: R01–R18 in [requirements](../../requirements.md). Read [baseline](../../research/baseline-audit.md) and [future research](../../research/future-architecture.md) as dated inputs, not settled designs.

## Required outputs

Produce track-local `findings.md` with sources/versions and uncertainties; `requirements.md` with current/proposed behaviour and acceptance; `decisions.md` using the [dependency template](../../templates/dependency-decision.md); and `verification.md` separating prototype, local and runtime evidence. Create these when work occurs, not as fabricated completed reports now.

## Acceptance

Traceable baseline and acceptance specification; unverified behaviour remains labelled. Research must include existing dependencies and code we own that a library could replace. Record prototype-only versus long-term role, promotion/removal trigger, migration and rollback. Refresh this spec and plan from findings before conditional implementation.

## Out of scope

No automatic adoption of shortlisted technology, wholesale rewrite, paid service, unqualified platform parity or public release. Cross-track responsibilities remain with their owning tracks. A negative feasibility result or justified deferral is an acceptable research outcome.

## Execution clarification

This is a mapping/documentation track. Its conditional application implementation phase is not activated: no app/library/data migration is selected here. Complete that phase by documenting disposition and verifying evidence. Runtime tests and actual dependency adoption remain downstream work, not undisclosed unfinished tasks in this track.
