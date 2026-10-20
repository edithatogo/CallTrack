# Track 4 findings — 2026-10-20, work in progress

## Baseline refresh and gap map

Original CallTrack static sources remain pinned in the Track 1 archive; no new APK extraction is necessary. Its automatic scan examined four rows, used title/time equality, rounded source time and had weak edit recovery. Track 3 deliberately replaced those behaviours with exact instants, presentation-aware mapping and verified local event receipts, but remained manual/latest-row and did not commit a pre-insert intent.

| Area | Current implementation | Track 4 required change |
|---|---|---|
| Enable/type switches | Manual action only | Explicit automatic enable/pause and independent incoming/outgoing/missed filters |
| Source traversal | AndroidCallLogSource consumes latest row | Bounded paging and durable progress, no four-row cap; source-reset guard |
| Display | Cached name/number/unknown, withheld masking; exact milliseconds; 1-minute zero marker | Keep actual zero duration explicit; qualify midnight/long durations; freeze existing event display across contact/timezone changes |
| Delivery | Known-ID receipt after insert; marker recovery | Pre-insert committed intent, conservative ambiguous recovery, shared UI/worker path |
| History | None | Range preview/confirmation, bounded import, cancel/resume and duplicate-safe repetition |
| Legacy calendar events | No adoption | Read-only confidence/ambiguity matching prototype; no guessed ownership |
| Schema | SQLite v1 receipts | Preserving migration; retain v1 IDs/fingerprints; outbox payload minimisation |
| Scheduling | Legacy worker disabled; no new automatic schedule | Prompt content-change trigger plus persistent periodic/reopen catch-up, measured execution |
| Status | Last manual result | Pending/confirmed/review/error counts without personal diagnostics |

## Current supported contracts and options

Sources refreshed 2026-10-20:

- [CallLog.Calls](https://developer.android.com/reference/android/provider/CallLog.Calls): supported fields include row identity, cached identity/presentation, duration and source timestamps. Call-only operation need not introduce contacts/SMS access. Extended types must not be silently collapsed into missed calls.
- [Work request guide](https://developer.android.com/develop/background-work/background-tasks/persistent/getting-started/define-work): periodic work has a 15-minute minimum and system-dependent execution. It is a recovery mechanism, not a prompt-delivery guarantee.
- [WorkManager Constraints.Builder](https://developer.android.com/reference/androidx/work/Constraints.Builder): API24+ content URI triggers offer a native prompt-work candidate, already above this app's min26. Test rearming and recovery rather than assuming observer persistence.
- [Unique work policies](https://developer.android.com/reference/androidx/work/ExistingWorkPolicy): KEEP does nothing while matching work remains incomplete. Rearming a running watch requires deliberate chaining/replacement semantics; a periodic fallback covers gaps.
- [WorkManager releases](https://developer.android.com/jetpack/androidx/releases/work): current stable listed as 2.12.0 (2026-09-23), versus inherited 2.7.1. This is a compatibility candidate, not an adoption. Its experimental analytics APIs are unnecessary for bounded timestamp receipts; runtime/SDK/transitive compatibility remains to be qualified before selection.
- [Room overview](https://developer.android.com/training/data-storage/room) and [releases](https://developer.android.com/jetpack/androidx/releases/room): stable listed as 2.8.5 (2026-09-09). Java-only projects can use annotationProcessor; Kotlin/KSP is not a prerequisite. Room could take over query validation, DAO/schema tooling and migrations, but does not solve the cross-store provider effect by itself. Native SQLite retains the simple existing store/migration boundary for the first outbox slice. Reassess before introducing import/session tables; do not use this decision to exempt app-owned SQL from replacement review.
- [Emulator console](https://developer.android.com/studio/run/emulator-console): supports simulated GSM calls. Emulator results must be labelled; they do not establish physical telephony/OEM behaviour.

## Dependency and environment disposition

No new app dependency is selected yet. Native SQLite is part of the platform and needs no added runtime licence. Existing AndroidX dependency provenance remains in Track 2. Room/updated WorkManager licences, exact artifacts, SDK constraints and transitive graph must be checked when actually adopted; latest-version discovery is not build qualification.

Emulator tool 37.2.12 installed using the already-accepted SDK terms. API36 Google APIs ARM64 image revision7 is available, but installation stopped on the unaccepted android-sdk-arm-dbt-license. The user explicitly authorised accepting this specific licence; its identified SDK Manager prompt was then accepted and image installation is running. No other new licence was accepted. Ordinary JVM/build work continues independently.

## Test evidence so far

- New durable core contract compile first failed due to missing DurableDelivery; initial 11 contracts then passed alongside 16 prior slice contracts (27 total); committed-payload resume without a source row adds a twelfth contract (28 total).
- Tests cover committed intent ordering, interruption before insert, marker recovery after insert, lost-marker ambiguity, deleted-event protection, cached label/timezone stability, reused source IDs, pause/denial and overlapping instances.
- Android store contracts first failed to compile because outbox() did not exist. A preserving schema2/outbox implementation built successfully with unchanged inputs; native execution remains pending. Native migration/reopen and full scheduling/import acceptance are not yet claimed.
