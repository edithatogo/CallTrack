# Java/Kotlin and dependency mapping

Date/evidence refresh: 2026-10-20. Input checkpoint f163e4c; track start4e8148a.
M3/M4 remain open. This track's bounded conversion is not release acceptance.

## Responsibility and migration map

Root app is one Android module with24 production Java files before migration.

| Group | Existing code / responsibility | Coupling and decision |
|---|---|---|
| Pure mapping/selection | CallEvent, CallIdentity, CallSelection | First bounded Kotlin conversion; preserve hashes, Java ABI and null/trim rules |
| Source model | CallRecord | Remains Java while Track4 evolves raw presentation/source facts; convert after contracts settle |
| Orchestration | SliceEngine, DurableDelivery | Manual receipt vs durable outbox state machines; retain Java until automatic/import and operation-owner contracts qualify |
| Storage | SqliteLedger | Schema2 preserving v1; owns receipts/outbox and transaction boundary; any Room takeover needs explicit migration and crash tests |
| Native provider adapters | AndroidCallLogSource, AndroidCalendarSink | Permission/provider/cursor/ContentValues semantics stay Android-specific even in Kotlin/KMP |
| Current UI | CalendarSliceActivity | Executor/lifecycle/selection state; retain Views and Java until behaviour stable; Kotlin conversion separate from Compose redesign |
| Inherited legacy | AutostartReceiver, Config, LogFragment, LogItem, LogItemRecyclerViewAdapter, MainActivity, MainFragment, RandomString, SettingsFragment, Sync, SyncResult, SyncResultRunner, SyncResultType, SyncWorker |14 files; old activity unexported/boot disabled, but code and libraries remain packaged. Audit reachability and replace/remove after proven feature parity, rather than spending effort translating unused screens/endpoint |

Twenty-one Java files remain after the first three conversions. These are explicit
remaining migration work, not permanent exceptions or a declaration of completed M5.
Test sources may remain Java deliberately to exercise interoperability.

## Kotlin/toolchain choices

The [Kotlin release page](https://kotlinlang.org/docs/releases.html) lists2.4.21 as
current stable at this refresh. Compare stable candidates, not EAP language features.
The [KGP matrix](https://kotlinlang.org/docs/gradle-configure-project.html) places
2.3.20–2.3.21 within Gradle7.6.3–9.3.0/AGP8.2.2–9.0.0 support; Kotlin/JVM target
must match Java1.8. The [Android compiler matrix](https://developer.android.com/build/kotlin-support)
requires R8 8.13.19 for Kotlin2.3 versus9.1.29 for2.4. Existing AGP8.13.2 bundles
8.13.19. Select2.3.21 for a bounded compatible development migration; a newer
compiler is not sufficient reason to change all build tooling concurrently.

Kotlin2.4's standard-library support policy is documented separately; no equivalent
support-window promise is made for2.3 here. Revisit the whole compatible toolchain
before release rather than turning this pilot version into a permanent constraint.

[Java interop guidance](https://kotlinlang.org/docs/java-to-kotlin-interop.html)
supports JVM annotations for Java static calls and fields. Retain `@JvmStatic` and
`@JvmField` where existing callers depend on them; avoid data-class-generated
semantics, default-argument overload churn and accidental nullable-to-nonnull
constructor changes. Source/binary tests matter beyond syntax conversion.

## Replace versus translate

| Responsibility | Native/current | Candidate takeover / decision |
|---|---|---|
| Event policy, stable identity | Small owned pure functions | Translate directly; no library encodes our length-prefix/hash and zero-duration policy |
| Provider integration | CalendarContract/CallLog adapters | Retain platform APIs; a wrapper cannot grant permissions or supply a cross-store transaction |
| Ledger SQL/migrations | SQLiteOpenHelper | Room is a serious takeover candidate with schema/DAO checks, owned by Track4; avoid simultaneous SQL and language migration |
| Preferences | SharedPreferences | DataStore could replace state storage; it must preserve namespace/calendar identity and serialize changes, not just rename fields |
| Executors/status | Single executor and synchronous ports | Coroutines/Flow could replace cancellation/state delivery after lifecycle and process recovery contracts exist; cancellation alone is not provider rollback |
| Legacy JSON | Gson2.9.0 | kotlinx.serialization candidate once endpoint contract is selected; generated serializer/field/null semantics need fixtures; no network path added |
| Views/navigation | Java/XML | Kotlin first, Compose later; no UI plugin or navigation rewrite here |
| Shared hashing/domain | java.security and models | Keep JVM implementation; KMP/Rust tracks evaluate portability and library takeover with common fixtures |

Candidates above are surveyed options without selected versions. Refresh versions,
licences, transitive graph, maintenance and compatibility at their owning adoption
point; they are not newly installed dependencies. No Rust/FFI/KMP or backend is
needed to prove the mixed-language boundary.

## Persistence and regression hazards

Source signatures use UTF16 string lengths and UTF8 SHA256, with a versioned prefix.
Changing Kotlin string trimming to Unicode rules would change some event titles and
stored fingerprints. Java's trim removes chars≤U+0020; preserve that deliberately.
Keep integer overflow detection, zero-duration rendering, null/empty distinction,
withheld identity masking, static methods and public fields. No source reset,
SQLite schema/version, package/version or destination-marker change is allowed in
this slice. A source-equivalent model is more valuable than an idiomatic rewrite
that changes persisted identity.

Golden fixtures are captured from pre-migration Java and exercised from Java tests.
The existing31 JVM contracts and native preserving-store tests are regression inputs,
not proof of whole-app Kotlin migration, real calls or cloud synchronization.
