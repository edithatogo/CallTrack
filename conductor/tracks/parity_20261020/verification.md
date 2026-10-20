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


## Existing-effect recovery controls checkpoint

ReceiptRecovery has no insert port; known-ID/unique-marker checks can reconcile an
existing effect even when its original operation is inactive. READY without event ID
is not inserted. Suppression is explicitly confirmed, clears pending payload and
retains source/event identity without calendar deletion. Five core regressions cover
never-attempted, inactive ownership, missing/ambiguous effects, denied readback and
preserved discovered ID. Recovery Activity exposes bounded metadata pages, read-only
calendar access and optional recorded-event viewing. Native tests verify empty
screen opening and25 mixed current/legacy receipts across state changes.

The Android pager regression initially failed on repeated rows. A host SQL probe and
native red receipt identified lost affinity on COALESCE-derived time with string
cursor binding; verified INTEGER casts correct all numeric cursor comparisons.
One attempted wrapped-string replacement was detected as a no-op; that build was
intentionally interrupted before retrying the verified correction. Marker result
materialisation now caps at two; formatted locale resources replace concatenation.
Full source-bound build87 JVM tests/APKs/lint and25 host tests pass;32 API36 native
checks pass and packages are removed. Exact inputs/debug artifacts/native outputs
and red receipt digest are in recovery-evidence.json. Real read-only provider and
rendered positive UI are not qualified here. Next: separate explicit reapproval for
inactive never-attempted work, payload-retention policy/controls and persistence
faults, then full positive provider/worker/UI/reboot/reopen/offline/Doze/latency matrix.
Final full-track review/fixes/archive remain outstanding.


## Explicit READY reapproval checkpoint

ReadyReapproval validates the exact user-reviewed frozen READY snapshot, calendar
permissions/availability and current confirmation under the delivery lock. Fresh
manual ownership preserves source facts/payload identity; old operations cannot
resume it. Attempted, suppressed, changed or incomplete intents are rejected. The
recovery Activity separates original-event insertion preview/confirmation from
existing-effect checks. Permission grants do not approve insertion, and leaving
withdraws pending authorization. Four test-first core contracts cover fresh owner,
refusal, crash-after-insert/no second approval and denied/withdrawn confirmation.
Native SQLite/port verification covers owner/facts across reopen and suppression
clearing a non-null READY payload while retaining metadata and duplicate protection.

Full source-bound build91 JVM tests/APKs/lint and25 host tests pass;33 API36 native
checks pass and packages are removed. Exact inputs/debug artifacts/native outputs
in reapproval-evidence.json. A broad German copy replacement was corrected before
the final build so suppression does not imply it can be undone by reapproval.
Native insertion here uses a fake calendar port, not CalendarContract; positive
rendered permission/confirmation/lifecycle remains pending. Next implement age-based
payload retention and remaining storage faults, then full real-provider/worker/UI/
reboot/reopen/offline/Doze/latency acceptance and final review/fixes/archive.


## Storage-age retention checkpoint

Schema6 commits payload storage timestamps and preserves original age across pending
transitions. Existing schema1–5 payloads receive seven days from migration because
their age is unknown. Access checks and bounded maintenance clear expired bodies;
READY becomes EXPIRED_READY so automatic delivery cannot insert it. Confirmed import
can collect a matching fresh source under its current operation. Attempted metadata,
known IDs, hashes/facts and calendar effects remain available for reconciliation.
Three test-first JVM contracts pass (94 total). Two new native checks verify bounded
clearing/read expiry, preserved age and recovery metadata, and schema5 migration
with preserved checkpoints/receipts and exact grace boundary. All35 API36 checks,
25 host tests and source-bound build/APKs/lint pass; packages removed. Lint0 errors,
32 warnings/5 hints includes a new plural-candidate copy warning for retention count.
Exact sources/debug artifacts/native output in retention-evidence.json. Clearing is
logical/opportunistic, not forensic or deadline-guaranteed. Positive rendered UI,
real-provider delivery/background lifecycle and persistence-fault gates remain open.
Full-track review/fixes/archive is still pending.


## Uncertain execution-status commit checkpoint

An injected false commit outcome after Android cache update reproduced a saved-status
read despite uncertain durability. The native regression failed on that exact assertion,
with packages removed; red receipt digest retained in status-commit-evidence.json.
An initial test compile used unavailable assertThrows in the older Android JUnit
runtime; existing try/catch assertion style corrected the fixture before the red run.
A shared process uncertainty set now blocks status reads across store instances until
complete publication commits successfully. Repeated attempts retain their token and
do not inflate failure counts. Thrown commit leaves the same fail-closed state.
All36 API36 checks,94 JVM tests,25 host tests and source-bound debug build/lint pass;
packages removed. Lint0 errors/32 warnings/5 hints. This injects an uncertain outcome
using real Android preferences; it does not prove physical disk exhaustion, restart,
or process-death recovery. Those faults and real-provider/worker/UI/reboot/reopen/
offline/Doze/latency gates remain open before final full-track review and archive.


## Real calendar-provider durable acceptance checkpoint

New ParityProviderTest runs only against a token-scoped LOCAL calendar with synthetic
call objects and READ_CALL_LOG denied. SQL trigger faults prove READY failure has no
provider effect, ATTEMPTED failure leaves recoverable IN_FLIGHT plus exactly one real
event, and CONFIRMED failure retains a committed known ID. A separate SQLite connection
observes committed IN_FLIGHT before provider insertion. Reopen recovers marker/known
ID without another event. Actual edited/deleted events become review without overwrite
or recreation; read-only destination blocks a new intent. Zero-talk-time metadata and
visual marker, withheld identity and absent reminders/attendees pass actual readback.

Historical preview writes no events, partial saved count matches effects, reopen
finishes12 events, repeat import keeps12 and cancellation after one new event keeps13
with the remaining candidate unattempted. An actual interrupted effect is then kept
across instrumentation runs; the harness revokes WRITE_CALENDAR and read-only recovery
verifies the unique existing marker/ID and purges the body without insertion.

All8 provider instrumentation checks,94 JVM tests,32 host tests and the source-bound
build pass. Calendar cleanup checks both account rows and their event rows; both APKs
are removed. Host fault tests cover target refusal, cleanup failure, uncertain install,
failed build and restoring write access before recovery after read-only phase failure.
Exact input/APK/native output and tool hashes in provider-evidence.json. No production
code changed in this checkpoint. Trigger faults are not physical disk exhaustion or
actual abrupt process death. Synthetic sources do not qualify actual CallLog/positive
WorkManager delivery. Rendered UI, background restart/reboot/reopen, offline/Doze and
latency acceptance, remaining storage faults and final full-track review/archive stay
open. Physical calls and real cloud delivery remain pending as previously scoped.


## Positive CallLog/provider/WorkManager checkpoint

WorkerProviderTest uses only the dedicated emulator. Setup verifies an empty call
provider, creates a token-owned LOCAL calendar and inserts explicitly tagged synthetic
call rows using test-only shell READ/WRITE_CALL_LOG identity, withdrawn before worker
execution. The app has READ_CALL_LOG only; contacts/SMS/audio/write-call permissions
remain denied.125 mixed incoming/outgoing/missed/blocked rows plus one pre-activation
row exercise multiple50-row pages. Incoming/missed-only policy creates exactly63
verified events, with a complete durable checkpoint and no pending/review rows. Repeat
work creates none. Paused work creates none; fresh outgoing-only activation excludes
paused/older rows and adds exactly one. A single subsequent outgoing call adds one
more (65 total), and two outstanding repeat work requests converge without another
event. They do not independently prove simultaneous execution. Revoked READ_CALL_LOG
in a subsequent instrumentation run produces PERMISSION_REQUIRED with65 unchanged.

Observed on this API36 emulator: mixed backlog request→verified observation52159ms;
source→verified68753ms including fixture preparation/grant/startup; single new-call
request→verified3499ms and source→verified4341ms. These include scheduling, execution
and final readback observation, not an insertion timestamp or delivery deadline.
Native output records worker publication separately. No physical-device inference.

All5 worker instrumentation checks,94 JVM tests,39 host tests, source-bound debug
build and lint pass (0 errors/32 warnings/5 hints). Cleanup pauses/cancels fixture
work, removes only exact tagged calls and verifies their absence, then verifies
calendar/event removal and both package removals. Evidence is source/APK/tool bound
in worker-provider-evidence.json. Provider-only prior checkpoint is1c9fffe; current
helper changes only expose its test fixture utilities at package scope. No production
runtime code/dependency changed. Reboot/force-stop/reopen, offline/Doze, rendered UI,
remaining persistence faults and full-track review/fixes/archive are still open.


## Lifecycle recovery acceptance checkpoint

The native receiver-isolated launcher regression failed with expected2 events but1
before CalendarSliceActivity resume requested recovery. The fix schedules only a
previously enabled committed policy, without changing generation/boundary. API35+
force-stop/reopen separately passed via its unstopped-package boot signal; that was
not mistaken for an independent Activity wake. An initial verifier killed the app
between confirmed receipt and status publication; the host now observes both before
starting instrumentation. A stale immediate Wi-Fi restore read also failed; a
regression demonstrates asynchronous readback and bounded polling fixes it.

Broadcast acknowledgement now covers both WorkManager operation futures within one
monotonic8-second budget before finishing goAsync. Three JVM regressions were red
before implementation and pass. Two earlier90-second boot observation windows had
no fourth effect; diagnostics retained granted READ_CALL_LOG, an unstopped package
and no scheduled job, with broadcast history showing receiver dispatch about85s
late. The final observation budget is180s; this is neither a product deadline nor
proof that only one of those changes explains earlier outcomes.

The final source-bound lifecycle run passes11 instrumentation checks plus one
explicitly terminated held insertion: launcher resume, force-stop/reopen, actual
process death after CalendarContract insert/before ID persistence, preserved unique
marker/receipt recovery, actual guest boot-ID change and background recovery before
verification startup, no-active-network local delivery,40s forced deep-IDLE deferral
past a30s staged eligibility delay, and background recovery after unforce without
starting the app first. Six expected real events/receipts are verified and preserved
until scoped cleanup. Native source→verified observations:18275ms first reopen,
8087ms isolated resume,18778ms terminated-insertion recovery,180134ms reboot including
guest boot/platform dispatch,9941ms offline and60867ms Doze including deferral. These
are observations, not insertion timestamps or physical-device guarantees.

All97 JVM/50 host tests and full debug build/lint pass. Airplane/Wi-Fi/mobile-data,
stay-awake, battery override and idle/screen settings match their original snapshot;
all tagged calls/calendar events and both packages are removed. lifecycle-evidence.json
binds exact sources/debug artifacts, native outputs, tools and failed-check digests.
Native regression of the earlier store/control suite follows separately. Protected
emulator call-end wake and positive rendered UI, remaining activation/status fault
contracts, diagnostic delivery-count semantics and final full-track review/archive
remain open. In the held recovery, a worker reported2 verification actions for one
recovered+rescanned event; ledger count3 and exact event ID prove unique effects,
so no unique/new-event claim is inferred from that action counter.


Final lifecycle regression: all36 earlier API36 store/settings/disabled-stale worker
checks pass on the same source-bound build, with packages removed. Exact outputs
are included in lifecycle-evidence.json alongside the11 lifecycle checks and the
expected process termination. All97 JVM/50 host tests pass; lint0 errors/32 warnings/
5 hints. No fixture packages or changed emulator settings remain. Full Track4 review
and archive are not complete while the explicit remaining gates above are open.
