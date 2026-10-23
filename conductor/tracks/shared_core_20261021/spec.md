# KMP shared core implementation

Track 15; execution stage 4; milestone M7. Status: bounded shared selection/range
slice implemented and JVM/Android-qualified; macOS Kotlin/Native and full track
acceptance remain pending.

## Objective

Extract portable product rules behind compatibility facades; keep one persistence/writer owner. Validate common fixtures on Android/JVM and macOS Kotlin/Native.

## Requirements and discovery

References: R16, R24, R26 in [requirements](../../requirements.md). The user authorised
this roadmap on2026-10-21. Archived research is dated input, not production adoption.

- [Research kmp_20261020](../../archive/kmp_20261020/index.md)

Each owning track must map actual behaviour/APIs/dependencies and app-owned code,
refresh requirements and record findings/decisions before conditional implementation.
Compare native, retained, library replacement and deferral. A dependency may be a
temporary prototype aid or long-term takeover; record its exact role, licence,
transitives, migration, promotion/removal criteria and recovery. No automatic
prototype-version inheritance.

## Implementation and acceptance

- Map Android/JVM dependencies and native target/toolchain constraints; compare retained code, common libraries and minimal native facades for hashing/time/overflow.
- Extract models, mapping, identity/filtering and portable recovery decisions; native adapters own providers, persistence, scheduling, permissions and secrets.
- Keep stored signatures, schemas, namespace and Java contracts unchanged; compare old/shared outputs read-only before switching one owner.
- Run common golden/fault fixtures on Android/JVM and macOS Kotlin/Native, inspect target artifacts and preserve upgrade/recovery; no second ledger or dual writes.

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
