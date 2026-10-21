# Compose Multiplatform Android and macOS UI

Track 17; execution stage 6; milestone M8. Status: pending implementation.

## Objective

Share suitable presentation state and screens after useful native companion workflow exists. Preserve native permission, file, calendar and platform navigation boundaries.

## Requirements and discovery

References: R17, R24, R26 in [requirements](../../requirements.md). The user authorised
this roadmap on2026-10-21. Archived research is dated input, not production adoption.

- [Research multiplatform_ui_20261020](../../archive/multiplatform_ui_20261020/index.md)
- [Research compose_20261020](../../archive/compose_20261020/index.md)

Each owning track must map actual behaviour/APIs/dependencies and app-owned code,
refresh requirements and record findings/decisions before conditional implementation.
Compare native, retained, library replacement and deferral. A dependency may be a
temporary prototype aid or long-term takeover; record its exact role, licence,
transitives, migration, promotion/removal criteria and recovery. No automatic
prototype-version inheritance.

## Implementation and acceptance

- Map Android/macOS screen differences, controller ownership, keyboard/focus/accessibility and packaging requirements; refresh Compose Multiplatform/native graph.
- Implement shared immutable presentation state and explicit actions, then share rendering without duplicate production owners or automatic effects.
- Retain native prompts/pickers/adapters and platform navigation where needed; qualification determines each shared boundary.
- Run rendered Android/macOS workflows, native keyboard/focus/large-text/RTL/manual accessibility, package/update/restoration checks before removing old rendering.

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
