# Track 4 verification — in progress

## Foundation checkpoint

Core implementation checkpoint 9fc2466 plus subsequent raw-type/filter changes. Source-bound build passed with 31 JVM tests (16 Track 3 regressions, 12 durable delivery contracts, 3 filter/range contracts), zero lint errors and 30 warnings. Host safety/documentation suite: 19 tests passed. [Foundation receipt](foundation-evidence.json) binds exact inputs and app/test APK hashes to four passed API36 emulator instrumentation invocations.

Native checks: synthetic cached-presentation cursor, preserving v1→v2 migration, committed in-flight state and confirmed payload clearing across database reopen, and rejection of outbox writes inside an uncommitted caller transaction. App/test packages were removed and absence verified. This phase used private fixture databases only: no calendar rows or call-log rows were written, no physical calls were made and no physical phone was used.

Dedicated AVD: calltrack-parity-api36-20261020, emulator-5580, API36 Google APIs ARM64 revision7 with Emulator37.2.12 and HVF/software graphics. Its first boot took several minutes with observable guest I/O and execution; a live process was retained until boot completed. First-boot timing is not an app delivery-latency result.

## Not yet accepted

- Native fault injection/process-death recovery through actual calendar/provider/outbox transactions; current durable crash tests are JVM fixtures.
- Durable retained source facts and operation ownership/generation; raw presentation distinctions and subsequent migration.
- Source paging/reset guards, automatic pause/type/settings behaviour, historical preview/import/cancel/resume and legacy matching.
- Prompt/periodic scheduling, process/reboot/Doze/offline/permission recovery matrix and measured event latency.
- Full user UI/status and review/recovery actions, final Conductor review/fixes/archive.
- Physical-call acceptance remains pending by user direction. Remote provider delivery and production release remain separate scopes.

The track stays in progress. Foundation success does not qualify automatic CallTrack parity or complete the original objective.

## Schema3 operation-ownership checkpoint

New JVM tests first fail to compile on missing Operation/scoped overloads. The
implementation then passes39 JVM tests (15 durable delivery), including pending
owner/generation rejection, legacy caller rejection, same-owner recovery, cross-owner
confirmed receipt verification, invalid operation identity and cancellation at the
committed boundary. Source-bound build/debug APK/test APK/lint pass with unchanged
inputs; lint0 errors/30 warnings. Java formatting and25 host safety tests pass.

Dedicated emulator-5580/API36 harness passes7 native checks including schema1→3,
schema2→3 pending preservation and operation identity across reopen/payload purge.
The harness grants no call/calendar permissions and touches only synthetic own-app
fixture databases. Both app/test packages removed with readback. Source/artifact
hashes and exact native methods are in ownership-evidence.json; full local logs
remain under .local/parity-owner-* and .local/parity-evidence.

No source-fact/raw-presentation retention, automatic/import/scheduler/controller
integration or full P01–P10 acceptance follows. Track4 remains active and unarchived.

## Schema4 source facts and presentation checkpoint

Tests first fail on missing raw-presentation constructor/fields and SourceFacts.
Green source-bound build passes42 JVM tests, app/test APK packaging and lint
(0 errors/30 warnings), with unchanged inputs. Legacy Java source/event goldens
remain intact. Tests distinguish seven presentation inputs, retain zero actual
duration independently from the one-minute marker, and review a presentation
change without changing the legacy signature or inserting again.

Dedicated API36 emulator harness passes10 native invocations, including synthetic
Cursor null/extended presentation and duration beyond32-bit range, schema1/2/3→4
preservation, source-fact retention after reopen/payload purge, and prior owner/
receipt contracts. No call/calendar permission granted or personal provider query.
Both test packages removed and read back absent. Exact hashes/methods are in
source-facts-evidence.json; detailed snapshots remain excluded in .local/parity-evidence.
25 host safety tests and Java/Kotlin formatting checks pass; privacy map updated.
This does not qualify paging/import/scheduling/UI or complete P01–P10.


### Schema5 paging foundation

Six new JVM regressions exercise51 rows over bounded pages, frozen upper bound,
deleted/reused/reset anchors, raw presentation, malformed pages, empty snapshots
and maximum IDs. Native synthetic query/cursor and private SQLite contracts pass,
including monotonic immutable checkpoints across reopen and v4 migration. Total:
48 JVM/25 host/13 native checks; exact inputs/APKs/methods in paging-evidence.json.
Fresh source-bound build and lint pass; fixture packages removed. No live source
query or user UI/background scheduling acceptance is inferred. Frozen candidate
preview and effect-time revalidation remain unimplemented integration requirements.


## Scan coordinator checkpoint

Bounded source scans now connect to DurableDelivery and SqliteLedger checkpoint
ports. Five test-first coordinator regressions pass: multi-page activation/type
policy, crash after receipt before progress, pause/destination stops, extended type
exclusion and changed candidate rejection. Source-bound build passes53 JVM tests,
app/test APK compilation and lint;25 host tests pass. Exact inputs/artifacts in
scan-evidence.json. No new native coordinator runtime proof is claimed. Persisted
activation settings/generation publication, source-independent pending recovery,
frozen historical sessions, scheduler/UI and full acceptance remain required.


## Activation control checkpoint

Four test-first controller regressions pass: absent/disabled default, pause across
controller recreation, new generation/boundary invalidating old workers, publication
failure/overflow and full policy match. Versioned Android SharedPreferences adapter
compiles, with process fail-closed latch for failed commit. Source-bound build passes
57 JVM tests, APK assembly and lint;25 host tests pass. Exact inputs/artifacts in
activation-evidence.json. Native persistence/fault tests remain pending. Next qualify
store readback and connect settings to scanner/scheduler/UI, then frozen imports,
legacy matching and full acceptance. This is not completed automatic logging.


## Native activation checkpoint

Two isolated Android settings tests pass: committed document exists on disk,
adapter recreation reads selected activation/pause/type settings, generation2
rejects generation1 workers, and malformed/future documents fail closed. Fixture
preferences deleted in finally. Full native regression matrix15/15 passes on API36;
test packages removed. Source-bound build57 JVM tests/APKs/lint and25 host tests
pass. Exact inputs/artifacts/native output in activation-native-evidence.json.
Reopen remains within one instrumentation process; no process-death or disk-failure
injection claim. Next implement verified automatic scan rollover and bounded
source-independent recovery before scheduler/UI; full Track4 remains open.


## Automatic rollover checkpoint

Completed scans can now extend to newer upper bounds while preserving processed
anchors. Activation-bound AutomaticDelivery calls the bounded scanner only for the
current full policy; stale/paused workers stop. Two test-first PagedCalls extension
regressions plus two runner regressions pass (empty/first/later calls, no replay,
changed/deleted anchors and pause). SQLite native test verifies compare-and-update,
stale publication rejection and reopen preservation. Full source-bound build61 JVM
tests/APKs/lint and25 host tests pass;16 API36 native checks pass and packages are
removed. Exact inputs/artifacts/native outputs in rollover-evidence.json. No Android
scheduler/UI/full-runner native acceptance inferred. Next integrate bounded pending
recovery before source traversal, then frozen historical import and scheduling/UI.
Full-track review/fixes/archive remain outstanding.


## Source-independent pending recovery checkpoint

AutomaticDelivery recovers bounded owner/generation/destination-scoped pending
intents before touching call-source state. A test-first crash-after-insert regression
verifies recovery without any source access or duplicate insert. Native SQLite query
test verifies limits, terminal-state exclusion, unrelated owner/generation/destination
exclusion, reopen and review removal. Source-bound build62 JVM tests/APKs/lint and
25 host tests pass;17 API36 native checks pass with packages removed. Exact inputs/
artifacts/native outputs in pending-evidence.json. No full native calendar recovery,
scheduler or UI acceptance inferred. Next implement frozen historical preview/
confirmation/progress/cancel/resume and read-only legacy matching, then scheduling,
user controls and complete emulator acceptance. Full review/fixes/archive remain open.


## Historical import foundation checkpoint

HistoricalImport implements bounded observed candidate preview/count, explicit
confirmation, cancellation, resumable per-candidate delivery and crash replay via
existing receipts. SqliteImportStore schema1 commits session/candidates atomically,
rejects stale revisions and keeps delivery outside its transaction. Three test-first
JVM regressions cover read-only/frozen/range preview, changed-source review and
crash-before-progress/cancellation. Two native SQLite/port tests cover partial
reopen, repeat import without duplicate, injected append rollback and stale update
rejection. Full source-bound build65 JVM tests/APKs/lint and25 host tests pass;
19 API36 native checks pass, with fixture databases and packages removed. Exact
inputs/artifacts/native outputs in import-evidence.json. Native calendar is a fake
port here; no real CalendarContract/import UI/process-kill acceptance inferred.
Next: per-candidate review details and permission/recovery tests, read-only legacy
matcher, qualified scheduling and user settings/import/status controls, then full
emulator acceptance and final review/fixes/archive. P07 is not closed by this core.


## Candidate outcome checkpoint

Import schema2 persists per-candidate result codes atomically with session progress.
Schema1 migration preserves totals/progress and leaves individual legacy outcomes
unknown. A new test-first denied-source regression verifies permission outcome,
unchanged progress and successful retry. Two native tests verify preserving schema1
migration and rollback of both outcome/progress after injected session-update failure.
Full source-bound debug build66 JVM tests/APKs/lint and25 host tests pass;21 API36
native checks pass and packages are removed. Exact inputs/debug artifacts/native
outputs in import-outcome-evidence.json. Actual CallLog permission revocation,
CalendarContract delivery and outcome UI remain unqualified. Next implement read-only
legacy matching, qualified scheduling, user controls/status/import/review flows and
complete native acceptance. Final full-track review/fixes/archive remain required.


## Legacy matcher checkpoint

Five test-first matcher regressions verify nearest-second/zero-duration tuples,
strong/possible/ambiguous outcomes, duplicate event IDs, rounded source collisions,
incomplete/malformed inputs, unknown presentation, edited text, old overflow rejection
and modern-marker exclusion. Results retain only IDs/confidence and expose no write
port. Full source-bound debug build71 JVM tests/APKs/lint and25 host tests pass;
formatting and Conductor/private-digest checks pass. Exact inputs/debug artifacts and
pinned APK inspection digest in legacy-evidence.json. No live calendar/provider or
receipt adoption is inferred. Next qualify WorkManager and connect scheduling plus
settings/import/status/recovery UI, then full native failure/background acceptance.
Final full-track review/fixes/archive remain required.


## WorkManager development checkpoint

WorkManager2.12.0 resolves and passes actual AAR metadata checks against the retained
SDK/toolchain. WORK-D01 records option comparison, graph/licence/artifact review and
rollback limits in work-dependency-report.json. AutomaticLogWorker connects persisted
activation to the durable runner, bounded8x50 pages, system-stop authorization and
bounded retry; prompt2-second settling requests plus15-minute periodic recovery use
no network constraint. Phone-IDLE/boot/package-replaced receiver only schedules an
enabled activation and never reads number extras. READ_PHONE_STATE is declared but
not granted. Launcher UI/reopen integration remains pending.

Two test-first retry-policy regressions and final-intent worker-stop regression pass.
Three actual WorkManager native tests verify disabled/stale jobs finish PAUSED without
permissions and prompt/periodic requests persist with NOT_REQUIRED network constraint.
Full source-bound build74 JVM tests/APKs/lint and25 host tests pass;24 API36 native
checks pass and packages are removed. Exact inputs/debug artifacts/native outputs
in work-evidence.json. Positive worker/provider, reboot/Doze/offline/reopen/latency and
UI acceptance remain open. Next wire user settings/status/import/recovery controls,
then full native acceptance and final review/fixes/archive.


## Explicit controls checkpoint

Automatic settings/status screen and manual launcher entry are implemented with
explicit enable/apply confirmation, pause, type filters and no fallback calendar.
Manual action now shares the durable path; two test-first JVM contracts verify
manual/automatic receipt reuse and missing/denied source behavior. Native tests
verify disabled-on-open Activity state and all-route counts including late legacy
receipts. English/German resources pass lint after fixing missing translations.
Full source-bound build76 JVM tests/APKs/lint and25 host tests pass;26 API36 native
checks pass and packages are removed. Exact inputs/debug artifacts/native outputs
in controls-evidence.json. This is underlying Activity state, not rendered/manual
UI or positive activation/provider/background acceptance. Next fix worker status
monotonicity and permission wording/failed counts, add import/recovery user flows,
then positive worker/calendar and restart/reboot/offline/Doze/latency acceptance.
Final full-track review/fixes/archive remain outstanding.


## Status hardening checkpoint

Generation-checked publication prevents late old-activation jobs from replacing
current status. Recorded failure attempts are generation-scoped and one attempt
token cannot increment twice. English/German UI labels distinguish attempts from
failed calls and warn that earlier counters can be incomplete. Legacy codes are
sanitised to UNKNOWN; manual permission text acknowledges an existing automatic
policy resuming when access returns. Worker status-write failures have bounded
retries and explicit unverified output. Two test-first JVM status contracts and two
native preference contracts pass for stale rejection, reopen and legacy fallback.
Full source-bound build78 JVM tests/APKs/lint and25 host tests pass;28 API36 native
checks pass and packages are removed. Exact inputs/debug artifacts/native outputs
in status-evidence.json. Disk-failure/commit uncertainty and process-death proof
remain required; adapter recreation is not process restart. Next implement import/
recovery user flows and draft/accessibility checks, then positive worker/provider,
reboot/reopen/offline/Doze/latency acceptance and final review/fixes/archive.


## Historical import controls checkpoint

HistoricalImportActivity exposes date range/destination/types, bounded read-only
preview, saved-profile confirmation, progress, stop/resume, separate cancellation
and database-backed saved-session paging. Captured timezone and exclusive end are
shown; skipped local dates first failed a regression and now reject rather than
shift. DST/invalid-range contracts pass. Stop reaches final intent authorization;
confirmed work stays resumable. Native tests verify25 saved sessions over bounded
pages and screen opening without preview creation/confirmation. Initial compile
caught an out-of-scope callback ticket; scoped error handling corrected it.
Full source-bound build82 JVM tests/APKs/lint and25 host tests pass;30 API36 native
checks pass and packages are removed. Exact inputs/debug artifacts/native outputs
in import-ui-evidence.json. Lint0 errors/32 warnings/5 hints includes locale text
concatenation cleanup for later UI review. Positive rendered confirmation/provider,
stop/cancel/recreate interaction and accessibility remain unqualified. Next add
recovery/retention controls and UI hardening, then persistence faults and complete
positive worker/provider/reboot/reopen/offline/Doze/latency acceptance. Final full-track
review/fixes/archive remain outstanding; P07 is not closed by default-state tests.
