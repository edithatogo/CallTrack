# iOS companion implementation — last

Track 20; execution stage 9; milestone M9. Status: scoped readiness research only; product implementation remains pending stages 1–8.

## Objective

Only after stages1–8 pass or have explicit justified deferrals, reuse qualified core/UI for an ordinary iOS manual/import/review/calendar companion. No Android-equivalent automatic capture promise.

## Requirements and discovery

References: R16, R17, R24, R26 in [requirements](../../requirements.md). The user authorised
this roadmap on2026-10-21. Archived research is dated input, not production adoption.

- [Research platform_feasibility_20261020](../../archive/platform_feasibility_20261020/index.md)
- [Research kmp_20261020](../../archive/kmp_20261020/index.md)
- [Research multiplatform_ui_20261020](../../archive/multiplatform_ui_20261020/index.md)

Each owning track must map actual behaviour/APIs/dependencies and app-owned code,
refresh requirements and record findings/decisions before conditional implementation.
Compare native, retained, library replacement and deferral. A dependency may be a
temporary prototype aid or long-term takeover; record its exact role, licence,
transitives, migration, promotion/removal criteria and recovery. No automatic
prototype-version inheritance.

## Implementation and acceptance

- Verify all earlier stage dispositions before iOS implementation; then refresh supported iOS APIs, Xcode/Kotlin/Compose compatibility, signing/device and distribution requirements.
- Implement native Apple file/calendar/permission/lifecycle adapters around shared core and suitable shared screens; deliberate preview/approval before effects.
- Qualify simulator and authorised device separately, including signing/usage strings, denial/revocation, interruption/process restore, exact EventKit readback and native accessibility.
- Test packaged update/recovery and platform capability disclosure; investigate any future automatic acquisition/default-role route as separate explicitly scoped research with current eligibility evidence.

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

Current implementation boundary: [scoped prerequisite record](prerequisite-deferral.md).
The [readiness report](readiness.md) refines the later implementation choices but
does not resolve a stage-1–8 gate, adopt a dependency or authorize iOS source code.
