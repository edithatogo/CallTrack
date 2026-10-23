# Shared-core requirements and acceptance

R16/R24/R26 refinement; proposed contracts, not delivered multiplatform behavior.

| ID | Current behavior / gap | Required acceptance before adoption | Owner |
|---|---|---|---|
| S01 | Java source model; Kotlin mapper still JVM-dependent | Dependency-free common DTO boundary; raw presentation/type/account facts preserved with versioned compatibility adapter | Track4 + reopened M7 |
| S02 | Persisted signatures/fingerprints have Java golden tests | Same synthetic fixtures yield identical UTF-8 bytes, UTF-16 lengths, SHA256, ASCII trim, null and checked-overflow results across targets | Reopened M7 |
| S03 | Process lock and Android SQLite ledger | Crash/cancellation at pre-insert, insert/receipt gap and readback; known-ID recovery; edits/deletion/ambiguity go to review without replacement | Track4 + reopened M7 |
| S04 | No selected feasible companion or native framework | Track12 capability matrix selects useful workflow and explicit target; unsupported acquisition labeled; no permission grant assumed | Track12 |
| S05 | No KMP resolved graph or native artifact | Pinned compiler/plugin/driver/engine graph, licence notices, reproducible target builds and Swift/JVM API smoke consumers | Reopened M7 |
| S06 | Existing schema2 receipts and public Java API | Preserve existing receipts/update path; version future changes; old/new readers and rollback tested before retiring implementation | Track6 + reopened M7 |
| S07 | No network in current slice | Native credentials and engine boundary; timeout/throttle/cancellation/expired-token tests; no PII logging or embedded secret | Track10 |
| S08 | No shared UI or production KMP | Isolated trial with exit trigger; delete duplicated prototype before adoption; production migration remains separately gated | Track9 + reopened M7 |

## Contract suite specification (not executed here)

First extract the existing Java golden fixtures from KotlinMigrationContractTest
without reinterpreting their expected hashes. Add empty/null/ASCII-whitespace and
Unicode-space, supplementary UTF-16 characters, delimiter-bearing IDs, unknown raw
types, zero duration, maximum Long overflow and half-open range cases. Include
unpaired-surrogate encoding policy before calling UTF-8 equivalence proven.

Use the same serialized synthetic input/expected-output fixture corpus in JVM and
commonTest. Proposed execution order: JVM fast feedback, macosArm64 Native engine
check, then Android library consumer and iosSimulatorArm64 framework/Swift smoke
consumer if Track12 selects iOS. macOS tests alone do not satisfy iOS packaging.
Do not name any target qualified before its compiler and runner receipts exist.

Storage adapter tests must use actual target SQLite transactions with reopen,
rollback, v1/v2 upgrade and post-crash recovery; in-memory fake tests alone cannot
qualify durability. Concurrency must cover two racing deliveries and cancellation
after external insertion. Native scheduling is capability-specific, with prompt
logging when possible and durable catch-up, rather than a universal timing promise.

Target acceptance receipts must bind source commit/input hashes, versions, target,
exact command, counts/results and artifact SHA256. Distinguish build, contract,
packaging, device/provider and human workflow acceptance. Physical-call acceptance
stays pending per user direction. No such cross-target receipts exist in this track.
