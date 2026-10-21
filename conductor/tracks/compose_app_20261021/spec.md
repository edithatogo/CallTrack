# Jetpack Compose Android implementation

Track 14; execution stage 3; milestone M6. Status: pending implementation.

## Objective

Extract authoritative screen state and actions; migrate settings/status, manual logging, import and recovery in that order. Compose must never acquire write authority through rendering.

## Requirements and discovery

References: R15, R26 in [requirements](../../requirements.md). The user authorised
this roadmap on2026-10-21. Archived research is dated input, not production adoption.

- [Research compose_20261020](../../archive/compose_20261020/index.md)

Each owning track must map actual behaviour/APIs/dependencies and app-owned code,
refresh requirements and record findings/decisions before conditional implementation.
Compare native, retained, library replacement and deferral. A dependency may be a
temporary prototype aid or long-term takeover; record its exact role, licence,
transitives, migration, promotion/removal criteria and recovery. No automatic
prototype-version inheritance.

## Implementation and acceptance

- Refresh controller/lifecycle/permission mapping and Compose/toolchain/transitive compatibility; choose stable pinned versions from actual evidence.
- Implement settings/status with ComposeView coexistence and explicit disposal; no provider effects in composition.
- Migrate manual logging, historical import and recovery screen by screen; preserve IDs, destination binding, operation ownership and saved-state semantics.
- Qualify real permission/revocation, destination disappearance, cancellation/recreation/process restore, font/RTL, TalkBack/Switch Access and no synchronization regression; remove replaced Views only after parity.

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
