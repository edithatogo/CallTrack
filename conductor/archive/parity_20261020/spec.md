# Parity and durable synchronisation

Milestone: M3. Type: research-led development. Status: implemented and accepted for the selected API36 emulator scope; production/physical/cloud gates remain with the handoff.

## Objective

Parity suite, crash/retry and background-recovery evidence; preserved edits and no duplicate creation.

## Scope and mapping questions

- Refresh legacy parity matrix and missing/zero-duration/contact behaviour
- Research durable outbox, deduplication and legacy-migration approaches including replacement libraries
- Specify edit/deletion, restart, offline, import and privacy semantics before implementing

## Inputs and dependencies

Prerequisites: [calendar_slice](../../archive/calendar_slice_20261020/index.md). Requirement references: R01–R11, R23, R27 in [requirements](../../requirements.md). Read [baseline](../../research/baseline-audit.md) and [future research](../../research/future-architecture.md) as dated inputs, not settled designs.

## Required outputs

Produce track-local `findings.md` with sources/versions and uncertainties; `requirements.md` with current/proposed behaviour and acceptance; `decisions.md` using the [dependency template](../../templates/dependency-decision.md); and `verification.md` separating prototype, local and runtime evidence. Create these when work occurs, not as fabricated completed reports now.

## Acceptance

Parity suite, crash/retry and background-recovery evidence; preserved edits and no duplicate creation. Research must include existing dependencies and code we own that a library could replace. Record prototype-only versus long-term role, promotion/removal trigger, migration and rollback. Refresh this spec and plan from findings before conditional implementation.

## Out of scope

No automatic adoption of shortlisted technology, wholesale rewrite, paid service, unqualified platform parity or public release. Cross-track responsibilities remain with their owning tracks. A negative feasibility result or justified deferral is an acceptable research outcome.

## Execution steering

Use [concrete acceptance](requirements.md) and [decisions](decisions.md). The user selected emulator acceptance initially and explicitly left physical-call acceptance pending. Prompt logging when possible with eventual recovery is the latency direction; measure actual results. Implementation still includes automatic logging, historical preview/import and durable recovery rather than treating documentation alone as parity.
