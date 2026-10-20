# KMP findings

Inspected 2026-10-21 at baseline `a781396`; research only. No shared module,
second-platform build, dependency resolution or native packaging was executed.
Track6 has three converted components and 21 remaining Java production files.
Track4's full source/import/scheduler contracts and Track12's platform feasibility
remain open. Track7 retains Views. These are prerequisite gaps, not KMP defects.

## Workflow and ownership map

| Workflow | Candidate common responsibility | Native responsibility / uncertainty |
|---|---|---|
| Android automatic capture | Selection, mapping, reconciliation decisions | CallLog permission/query, paging, WorkManager and lifecycle; automatic integration still pending |
| Android manual latest-call | Deterministic event mapping and receipt comparison | Current Java Activity/provider/SQLite adapters; preserve working implementation |
| iOS manual/import companion (proposed) | Validated import preview, mapping, deduplication, journal models | Entry/import UI, file access, EventKit, consent and background limits; Track12 must qualify useful scope |
| Desktop companion (alternative) | Same import/preview contracts | File/calendar adapters, packaging and credential store; no desktop cellular capture assumed |
| Future cloud destination | Destination-neutral intent/result and conflict policy | Provider authentication, account selection, throttling and native HTTP engine; Track10 owns qualification |

An iOS companion is a proposed product, not a selected test target. JVM desktop
can provide fast contract feedback but does not qualify Kotlin/Native or iOS.
No framework grants OS call-history access; Track12 owns the capability conclusion.

## Existing code to preserve or replace

- CallSelection's independent type switches and half-open ranges are portable
  logic, but its parameter is the Java CallRecord. Moving the file alone fails
  commonMain isolation. Unknown raw types must remain unclassified.
- CallRecord holds source facts and uses UTF-16 length-prefixed keys. Preserve raw
  type/account facts; its identityAllowed Boolean is a present limitation, not a
  permanent future presentation model. Track4 owns refinement/versioning.
- CallIdentity and CallEvent use JVM MessageDigest, StandardCharsets, Locale,
  Objects and checked Math arithmetic. Common code needs reviewed hashing/encoding
  and overflow equivalents, with byte-identical golden contracts. Do not substitute
  hashCode, Unicode trim or platform string-length conventions.
- DurableDelivery's process lock and synchronous store/provider calls cannot be
  translated into a cross-platform concurrency guarantee. A single writer per
  destination, committed intent before side effect, uncertain-insert recovery and
  human-edit protection need explicit transactions and cancellation tests.
- SqliteLedger schema2 stores receipts/outbox; SQL migration is a separate risk.
  A library must take over the existing ledger after parity, not add a second
  independent ledger. Calendar/source adapters stay native. Current app needs no
  network or account tokens; sharing does not justify adding either.

## Refreshed primary sources and candidates

These observations are documentation snapshots, not a tested compatible lockfile.
Versioned URLs are preferred below. Live sources may change; recheck on reopening.

| Candidate / observed version | Role and replacement | Cost / disposition |
|---|---|---|
| Kotlin/KMP2.4.21 current stable per compatibility guide; existing Kotlin2.3.21 | Common models/mapping; target compilers | Retain current compiler; defer KMP. 2.3.20–21 matrix lists Gradle7.6.3–9.3/AGP8.2.2–9.0/Xcode26.0; 2.4.20–21 lists Gradle through9.7/AGP through9.3.1/Xcode26.4. This does not prove Android shrinker or host compatibility |
| Room guide currently shows room3 3.1.0-alpha01 + SQLite2.7.1 | Typed schema/DAO could replace handwritten ledger SQL | Preview example is not a stable recommendation. Adds KSP, target processors/drivers and migration work; defer |
| SQLDelight2.1.0 versioned documentation | SQL-first typed alternative replacing ledger SQL | Platform drivers/compiler plugin and schema migration; compare only if Room misses a required transaction/target behavior |
| DataStore KMP setup (unversioned) | Could replace small configuration preferences | Not an outbox or credential vault; release version and target graph must be pinned at actual trial |
| Ktor3.6.0 observed release + serialization1.12.0-RC observed release | Shared connector HTTP/DTOs, replacing future duplicate provider clients | RC not selected; compiler/runtime compatibility and engines unresolved. No current network gap; defer |
| coroutines1.11.0 observed release | Structured asynchronous orchestration | Existing app resolves older transitive coroutines; cancellation cannot undo an external insert. No upgrade selected |
| MSAL native Android/iOS libraries | Provider-supported token acquisition | Native adapters, app registration/broker/cache differences; no common Kotlin MSAL assumption; no consent performed |
| Rust/UniFFI | Potential future reusable parser/engine | Binding/build/ABI cost; Track13 has no measured result yet. No performance bottleneck demonstrated; no competing core selected |

AndroidX, Kotlin ecosystem and SQLDelight candidates use Apache-2.0 upstream
licensing; this is a family-level shortlist, not an artifact/transitive notice audit.
Exact artifact licences, native SQLite notices and redistribution obligations must
be captured with the resolved graph before adoption. MSAL/UniFFI exact version,
licence and platform support remain to be pinned by their owning experiments.
No downloaded prototype dependency exists to expire or remove.

Sources refreshed 2026-10-21:

- [KMP compatibility/version and Android plugin migration](https://kotlinlang.org/docs/multiplatform/multiplatform-compatibility-guide.html)
- [Platform stability](https://kotlinlang.org/docs/multiplatform/supported-platforms.html)
- [Native targets/hosts](https://kotlinlang.org/docs/native-target-support.html)
- [Room KMP setup and transaction/API differences](https://developer.android.com/kotlin/multiplatform/room)
- [DataStore KMP](https://developer.android.com/kotlin/multiplatform/datastore)
- [SQLDelight2.1.0](https://sqldelight.github.io/sqldelight/2.1.0/multiplatform_sqlite/)
- [Ktor3.6.0 release](https://github.com/ktorio/ktor/releases/tag/3.6.0) and [platform engines](https://ktor.io/docs/client-engines.html)
- [Serialization1.12.0-RC](https://github.com/Kotlin/kotlinx.serialization/releases/tag/v1.12.0-RC)
- [Coroutines1.11.0](https://github.com/Kotlin/kotlinx.coroutines/releases/tag/1.11.0)
- [MSAL supported library families](https://learn.microsoft.com/en-us/entra/identity-platform/msal-overview)
- [UniFFI scope](https://mozilla.github.io/uniffi-rs/latest/)
- [AndroidX licence](https://github.com/androidx/androidx/blob/androidx-main/LICENSE.txt)
- [SQLDelight licence](https://github.com/sqldelight/sqldelight/blob/main/LICENSE.txt)

No latency, memory, energy, size, native ABI or development-time advantage was
measured. Maintenance activity is indicated by current documentation/releases,
not a warranty of support or a completed dependency security audit.
