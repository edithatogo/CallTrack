# macOS import and calendar companion

Track 16; execution stage 5; milestone M7-companion. Status: pending implementation.

## Objective

Provide deliberate supported-file import, preview, review and selected-calendar delivery using native file selection and EventKit. No automatic device synchronization.

## Requirements and discovery

References: R19, R24, R25, R26 in [requirements](../../requirements.md). The user authorised
this roadmap on2026-10-21. Archived research is dated input, not production adoption.

- [Research platform_feasibility_20261020](../../archive/platform_feasibility_20261020/index.md)
- [Research destinations_20261020](../../archive/destinations_20261020/index.md)
- [Research productivity_20261020](../../archive/productivity_20261020/index.md)

Each owning track must map actual behaviour/APIs/dependencies and app-owned code,
refresh requirements and record findings/decisions before conditional implementation.
Compare native, retained, library replacement and deferral. A dependency may be a
temporary prototype aid or long-term takeover; record its exact role, licence,
transitives, migration, promotion/removal criteria and recovery. No automatic
prototype-version inheritance.

## Implementation and acceptance

- Map a bounded supported import format/provenance, preview/approval, capability and privacy requirements before choosing parser/storage dependencies.
- Implement bounded file selection/parsing with no write during preview; imported foreign receipts remain inactive and never confer replay authority.
- Implement EventKit permission/list/select/insert/exact-readback with durable intent and one destination writer; denial/write-only cannot be called reconciled.
- Qualify interruption/restart/uncertain insert, edited/deleted events, missing/recreated calendar, scoped cleanup, native accessibility and packaged upgrade/recovery using synthetic data.

Preserve golden identity/rendering/null/time/overflow fixtures and human edits.
Separate shared tests, rendered tests, native provider readback, cloud outcomes and
operational observation. No schema/signature/namespace change by incidental
extraction; qualify any necessary later migration explicitly. Review, fix and
verify before closure; no research-only or compiler-only production acceptance.

## Boundaries

No automatic signing/publication/cloud consent/physical-device authority. Retain
working outputs until parity/recovery. Windows/Linux are outside this sequence.
iOS implementation is stage9 and cannot be advanced past unresolved stages1–8.
A justified deferral must state reason, evidence, consequences and reconsideration
trigger; it cannot turn unavailable features into supported claims.
