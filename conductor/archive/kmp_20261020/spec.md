# KMP shared core and second-platform requirements

Milestone: M7. Type: research-led development. Status: planned discovery; implementation scope provisional.

## Objective

Useful shared-core decision and equivalent contract tests on chosen targets.

## Scope and mapping questions

- Map target-platform user workflows and portable/native boundaries
- Refresh KMP storage/network/auth libraries and compare app-owned code, native adapters and Rust findings
- Specify common contracts and second-target fixture/packaging acceptance

## Inputs and dependencies

Prerequisites: [kotlin](../../tracks/kotlin_20261020/index.md), [platform_feasibility](../../tracks/platform_feasibility_20261020/index.md). Requirement references: R16, R24, R26 in [requirements](../../requirements.md). Read [baseline](../../research/baseline-audit.md) and [future research](../../research/future-architecture.md) as dated inputs, not settled designs.

## Required outputs

Produce track-local `findings.md` with sources/versions and uncertainties; `requirements.md` with current/proposed behaviour and acceptance; `decisions.md` using the [dependency template](../../templates/dependency-decision.md); and `verification.md` separating prototype, local and runtime evidence. Create these when work occurs, not as fabricated completed reports now.

## Acceptance

Useful shared-core decision and equivalent contract tests on chosen targets. Research must include existing dependencies and code we own that a library could replace. Record prototype-only versus long-term role, promotion/removal trigger, migration and rollback. Refresh this spec and plan from findings before conditional implementation.

## Out of scope

No automatic adoption of shortlisted technology, wholesale rewrite, paid service, unqualified platform parity or public release. Cross-track responsibilities remain with their owning tracks. A negative feasibility result or justified deferral is an acceptable research outcome.

## Refined research disposition, 2026-10-21

[Decision KMP-D01](decisions.md) retains current code and defers extraction until
stable Track4/6 boundaries and Track12 workflow qualification. [S01–S08](requirements.md)
define the later common contracts and equivalent-target test/packaging acceptance.
Research may close on this justified deferral; M7 production and equivalent target
execution remain unmet. No prototype or common module is selected in this phase.
