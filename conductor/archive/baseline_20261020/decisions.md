# Track 1 decisions

## D01 — Behavioural reconstruction, not original-code reuse

Evidence: supplied APK hash and CT01–CT13. Retain observed useful requirements; replace fragile scan/dedup/receiver behaviour. Native provider integration is the initial design candidate. Original source licence/authenticity are unresolved, so no extracted implementation is adopted. No data migration executed. Recovery: private analysis remains excluded and no device/calendar state was changed.

## D02 — AndroidCallLogSync remains a provisional fork candidate

Compared retaining upstream, modernising CLC, a new implementation and deferral. ACS is the newer source base with WorkManager and permission/config scaffolding; however calendar support, robust identity/retry and metadata are substantial missing work. CLC supplies manual export/SMS concepts but has older build/auth/UI dependencies. Keep both pinned references; the build track must compare repair costs with fresh implementation before actual adoption. No source tree copied into the successor and no production dependency selected.

## D03 — Temporary analysis dependency

Androguard4.1.4 in excluded `.local/analysis-venv` is a **prototype-only analysis tool**, not an Android runtime dependency. It resolves manifest/resources and DEX branch interpretation. The current track used private DAD decompilation as an aid and checked critical points against instructions; the double constant rendering illustrates why decompilation alone is insufficient. Licence/transitive distribution review is unnecessary for shipping an APK that does not include this tool; do review it if later bundling the analysis environment. Review trigger: next legacy audit. Removal: delete the isolated environment after private evidence no longer needs regeneration; retain the original user APK and evidence by default. Promotion: no production role proposed. A second decompiler/emulator is deferred unless contradictory evidence arises. No network/call/calendar mutation prototype required for this static task.

## D04 — Conditional implementation phase disposition

This track adopts documentary corrections and acceptance fixtures only. Application behaviour, data-schema migration and library replacement are deferred to their owner tracks, as the specification permits. Behaviour-changing red/green tests and device/provider rollback are **not applicable to this research deliverable**. Fixture validation checks completeness/traceability and calculated expectations, not application conformance. No build/test result is claimed for either FOSS app.

## D05 — Later dependency research stays in later tracks

Map replacements of both existing libraries and app-owned code: HTTP client/JSON handling, durable ledger, token handling, number formatting, UI, migration/import and scheduler orchestration. Preserve native permission/provider adapters where appropriate. Kotlin/KMP, Room/Ktor/Compose and optional Rust are hypotheses only. Each track refreshes compatibility/licence evidence, compares keep/native/custom/library/defer, and specifies temporary expiry or long-term adoption, migration, tests and rollback using the project decision template. No generic framework or early Rust prototype justified by current mapping workload.
