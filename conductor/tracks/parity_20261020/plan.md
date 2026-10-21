# Plan: Parity and durable synchronisation

Track active: schema6 implementation and rendered settings/import/recovery have bounded native evidence. Final-source regression, complete P01–P10 review, handoff and archive remain required. Historical checkboxes below await final evidence reconciliation.

## Phase 1: Discovery and mapping

- [x] Task: Refresh legacy parity matrix and missing/zero-duration/contact behaviour
- [~] Task: Research durable outbox, deduplication and legacy-migration approaches including replacement libraries
- [~] Task: Specify edit/deletion, restart, offline, import and privacy semantics before implementing
- [~] Task: Write findings and refine track requirements/acceptance with source provenance
- [ ] Task: Phase Verification & Checkpoint (refer to ../../workflow.md)

## Phase 2: Options and dependency decision

- [ ] Task: Compare retained/native/custom implementation with dependencies and deferral
- [ ] Task: Run a bounded prototype only where it resolves uncertainty; identify temporary dependencies and expiry criteria
- [ ] Task: Record decision, update spec and implementation tasks; propagate any changed prerequisite assumptions
- [ ] Task: Phase Verification & Checkpoint (refer to ../../workflow.md)

## Phase 3: Conditional implementation and verification

- [ ] Task: If adopted, write meaningful failing contract/regression tests before changing behaviour
- [ ] Task: Implement the selected bounded change and any data/config migration; remove superseded code only after parity
- [ ] Task: Verify tests plus applicable device/provider readback and rollback; otherwise document justified deferral
- [ ] Task: Phase Verification & Checkpoint (refer to ../../workflow.md)

### Implementation slices still required

- [x] Durable intent engine, source-independent resume, v1 preservation and native persistence tests (31 JVM/19 host tests; four API36 native invocations passed; packages removed)
- [x] Operation owner/generation and preserving schema3 migration (39 JVM/25 host/7 API36 native checks; packages removed)
- [x] Retained source facts/raw presentation, preserving schema4 migration (42 JVM/25 host/10 API36 native checks; packages removed)
- [ ] Bounded source paging, checkpoint/anchor validation, filter/settings policy and reset handling
- [ ] Frozen historical preview, confirmed bounded import, cancel/resume and partial-progress handling
- [x] Read-only legacy confidence/ambiguity prototype (71 JVM/25 host tests and source-bound debug build; no live query or adoption)
- [ ] Prompt scheduling plus periodic/reboot/reopen catch-up using a qualified WorkManager version
- [ ] User settings, destination/permission flows, pending/review/error status and explicit recovery policy
- [ ] Full emulator parity/background/failure matrix with exact artifact and cleanup evidence

## Phase 4: Handoff

- [ ] Task: Update consolidated requirements, stack and dependency decisions with actual outcomes
- [ ] Task: Document residual gaps, next-track inputs and checkpoint evidence
- [ ] Task: Phase Verification & Checkpoint (refer to ../../workflow.md)

## Execution checkpoints

Start: c4c8c02. User selected emulator tests initially (physical-call acceptance pending), prompt logging when possible with recovery after delays. Concrete P01–P10 requirements and pre-insert intent design are recorded before implementation.

First foundation: 11 new pure Java durable-delivery contracts passed with 16 prior slice contracts. Native store contracts first failed to compile on missing outbox() before schema2 implementation. Android build and emulator migration tests remain pending. Automatic scheduling, paging/import, UI/status and full parity matrix are still required; this foundation is not track completion.

Foundation build passed (27 JVM tests; app/test APKs and lint, unchanged inputs), followed by a twelfth durable-core contract for source-independent resume (28 pure Java tests passing). Three native store tests are compiled: v1 preservation/migration, committed state and payload clearing across reopen, and nested-transaction rejection. Emulator image installation is running after explicit user approval of android-sdk-arm-dbt-license.

## Resume checkpoint

Native foundation receipt: foundation-evidence.json. Current source has a whitespace-only formatter follow-up after that exact receipt; refresh the build receipt with the next implementation slice before installing another APK. Core remains manual in the user UI; DurableDelivery is not yet wired to automatic/import work.

Dedicated emulator is live: emulator-5580 / calltrack-parity-api36-20261020; host process 60763 (unified exec session 88739 at creation). Always inspect current process/device state before reuse; do not restart from a stale session or timeout alone. Store tests removed both development packages. The physical phone is not used for Track 4. SDK/image licences and user timing/emulator choices are recorded in findings/decisions.

Next: retain source facts/raw presentation and operation-owner generation in the durable model, with a preserving schema evolution and tests; then implement bounded source traversal/reset guards, import snapshots/cancel/resume, legacy matching, scheduler and user UI. Full emulator recovery matrix, final review/fixes/archive remain required.

## Integration research handoff (nonblocking)

[Track10 contracts](../../archive/destinations_20261020/requirements.md) identify future route
identity, epoch/rebinding and opaque remote-ID migration requirements. Retain current
receipt compatibility; no direct connector/schema expansion is adopted here.
Provider-cloud qualification and native marker survival remain unverified.

## Current ownership checkpoint

Schema3 ownership change verified in ownership-evidence.json. Next implement raw
presentation/exact source metadata retention, then bounded source traversal, frozen
import sessions, legacy matching, scheduling and UI. Final full-track review/archive
remain required; do not treat this checkpoint as parity completion.

## Current source-facts checkpoint

Source facts/presentation verified in source-facts-evidence.json. Next implement
bounded source traversal with stable snapshot/anchor reset handling, then frozen
historical sessions and automatic scheduling/UI integration. Prior resume sections
are historical; use this checkpoint and fresh device/process checks. Full review/
fixes/archive remain outstanding.


## Current paging checkpoint

Schema5 bounded paging/checkpoint foundation verified in paging-evidence.json:
48 JVM tests,25 host tests,13 API36 native invocations; package cleanup passed.
The initial native build exposed unavailable JUnit assertThrows; local assertion
helpers fixed compilation without changing test dependencies. Source-bound build,
formatting and lint pass (0 errors,30 warnings,5 hints). Frozen bounds, anchor
validation, cursor limits and preserving v4 migration are implemented. Filter/
activation settings and coordinator advancement are still required before P02 can
close. Next integrate scan delivery and frozen import sessions, then scheduler/UI.
Full-track review/fixes/archive remain outstanding.


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

- `5c2b849`: storage-age retention, schema6 preserving migration and expiry evidence.
- `68a9ecf`: fail-closed uncertain status publication with native red/green evidence.


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

- `1c9fffe`: actual calendar-provider persistence boundaries, edits/deletion, import and read-only recovery acceptance.
- `ea86831`: actual tagged CallLog/WorkManager delivery, filters, activation boundaries, revocation and observed latency.


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

- `ca132a4`: launcher resume recovery, bounded scheduler acknowledgement, native process-death/reboot/offline/Doze acceptance and restored emulator state.


## Local counter correction while qualified storage is disconnected

Recovery followed by a source rescan previously reported two delivery actions for
one newly confirmed receipt. Two targeted regressions failed before the fix; the
scan now excludes already CONFIRMED receipts while still counting an ATTEMPTED
known-ID receipt that becomes confirmed without another insertion. The state read
and delivery share DELIVERY_LOCK. This diagnostic count describes newly confirmed
receipts, not necessarily newly inserted events. No schema/provider policy changes.

Local javac --release8 compilation and96 pure JVM tests pass, including11 targeted
scan/automatic contracts;50 host tests pass. Current Kotlin source hashes match the
last successful Android build before reusing its retained Kotlin classes. Exact
compiled Java hashes, dependency URLs/digests and red/green output are recorded in
counter-local-evidence.json. SchedulingCommitTest is excluded from this fallback.
This is not a Gradle build, APK qualification, lint or emulator runtime proof.

PortableSSD is absent from diskutil; the qualified SDK and Gradle-cache symlinks
are dangling. Gradle failed before compilation, which is infrastructure failure,
not the regression red. User directed continuing local work and will reconnect
later. No global paths or emulator process were replaced. After reconnect: inspect
the existing guest/process/storage first, refresh the complete source-bound build,
then qualify native counter/fault contracts and the remaining protected call-end
wake/rendered UI gates. Full-track review and archive remain open.

- `2da53cc`: locally verified receipt-count correction; complete Android/native requalification pending storage reconnect.


## Local activation fault and scheduling review checkpoint

Two local review findings are fixed: thrown activation commit uncertainty now blocks
reads/writes process-wide, and policy publication plus scheduling/cancellation
requests share DELIVERY_LOCK to prevent stale ordering across callers. Four guard
and three coordination contracts pass; the old policies fail one and two behavioural
regressions respectively.103 pure JVM tests and53 host tests pass. See
[local review](local-review.md) and local-fault-evidence.json for exact scope, sources
and red/green results. No schema/dependency/permission changes.

Three native fault methods are prepared and added to the39-method store harness:
false/thrown activation commits after actual cache mutation and thrown status commit
followed by successful retry. Android adapters/new instrumentation are not compiled
or run. The JVM fallback excludes SchedulingCommitTest, uses retained Kotlin classes
with unchanged source hashes, and is not full Gradle/lint/APK/native qualification.

Android build, native counter/fault/worker/lifecycle regression, protected call-end
wake, positive rendered UI and final full-track acceptance/review/archive remain
pending. User directed local work while PortableSSD is disconnected; no further
hardware permission is requested. Resume sequence is in local-review.md.

- `72d352c`: local activation-fault and scheduling-order review fixes,103 JVM/53 host checks, prepared native fault tests; Android/native qualification pending.


## Local durable identity review checkpoint

Two test-first faulty-port regressions showed matching payload/calendar could
confirm a wrong or invalid event ID. DurableDelivery now checks valid identity and
exact readback ID, matching the recovery contract.105 local JVM tests pass; no
Android/native execution is inferred. See event-identity-local-evidence.json and
local-review.md. P01–P10 closure work and positive UI/telephony protocols are mapped
in [remaining acceptance](remaining-acceptance.md). Hardware-dependent gates and
final full-track review/archive remain open.

- `aa0802b`: exact event-ID verification, two faulty-port regressions,105 JVM/53 host tests and P01–P10 remaining acceptance ledger; native qualification pending.


## Reconnected toolchain build checkpoint

PortableSSD is mounted again and the qualified SDK/Gradle cache resolve. The full
source-bound Gradle run passes108 JVM tests (0 failures/errors/skips), debug and
Android-test APK assembly, and lint (0 errors,32 warnings,5 hints). Inputs remain
unchanged throughout the build;53 host tests also pass. New activation/status fault
instrumentation is now compiled, but not executed. Exact input/artifact digests,
results and guest-health evidence are in reconnected-build-evidence.json.

The existing dedicated emulator PID60763 is still present in UEs state. A bounded
guest shell probe and the connected console greeting both time out. Sampling exits
138 without a report; TERM and KILL leave the process present. ADB's device label
is not guest-health proof. No replacement guest is launched against the same AVD,
no AVD reset is performed and current fixture cleanup cannot be read back. A host
restart may be needed to clear the stuck process. After it clears, revalidate the
AVD/storage/guest before installing the exact verified APKs. Store/fault/provider/
worker/lifecycle, protected call-end and positive UI acceptance plus final full-track
review/archive remain open. Earlier disconnected-storage limits are historical.


## No-host-restart recovery and native fault acceptance

Created a fresh dedicated API36 Google APIs ARM64 AVD named
calltrack-parity-api36-recovery-20261021 with independent writable storage. Original
PID60763/AVD remain untouched; recovery uses supported console5582/ADB5583 ports.
Initial5590 launch was stopped cleanly after help confirmed the supported range.
First boot432461ms included active ART/graphics compilation; responsive console,
qemu identity and sys.boot_completed=1 were verified before source-bound install.

All39 native store/control/fault checks pass on the unchanged successful108-test
Android build, including false/thrown activation cache mutations and thrown status
commit/retry accounting. Both packages removed.55 host checks and Conductor link
validation pass. Exact source/APK/native output/AVD/tool evidence is recorded in
recovery-store-evidence.json; harness target support is a3267ce. The shared exact
allowlist still refuses physical devices, unrelated AVDs and stale builds.

The computer-restart blocker is bypassed, not falsely reported as cleared: the old
process remains stuck. Broader provider/worker/lifecycle, protected telephony and
positive rendered acceptance plus full review/archive remain open. A source-namespace
commit-uncertainty candidate was noticed during read-only review; reproduce it with
native preferences faults before deciding whether another fix is required.


## Native source-identity uncertainty review fix

A real SharedPreferences proxy committed its cache then reported a false identity
write. Native red showed the next AndroidCallLogSource adapter reused the cached
namespace despite uncertain persistence; both packages were removed. The source
namespace monitor now latches before writing and refuses reads after false/thrown
outcomes until app-process restart (not computer restart). Existing committed IDs
and schema remain unchanged. Native false/thrown and successful-reuse checks all
pass on the source-bound latest APK; packages removed.108 JVM/55 host tests and full
debug/test APK/lint pass (0 errors,32 warnings,5 hints). Exact red/green/source/APK
evidence is in source-identity-evidence.json. The prior39 native store suite remains
separately bound to the pre-identity-fix APK; do not merge those into a claim that42
checks ran on one APK. Wider current-APK provider/worker/lifecycle acceptance is
next; protected telephony, positive UI and final full-track review/archive remain.

- `a3267ce`: exact isolated AVD target support; `a8eedb2`:39 native store/fault checks with cleanup on recovery guest.

- `7d19775`: native-red/green source-identity uncertainty fix;3 native checks/cleanup and108 JVM/55 host tests. Latest APK provider regression8/8 with fixture/package cleanup passes; recovery-provider-evidence.json. Positive worker/lifecycle follow.


## Latest recovery guest delivery regression

After the source-identity fix, the latest source-bound APK passes all8 real-calendar
provider checks and all5 positive CallLog/WorkManager checks. Both harnesses verify
fixture cleanup and package removal. Exact source/APK/native outputs/tool hashes
are in recovery-provider-evidence.json and recovery-worker-evidence.json. These
cover durable boundaries, edits/deletion, import/recovery, real tagged-source
paging/filtering/activation/repeat-work/revocation; they do not qualify protected
telephony wake or rendered positive UI. Lifecycle regression is running separately
on emulator-5582. Full Track4 review/archive remains open.


## Latest recovery guest lifecycle acceptance

The source-identity-fixed APK passes11 lifecycle checks plus one deliberately killed
held insertion. Reopen, receiver-isolated Activity resume, real process death after
provider insertion/before returned-ID persistence, actual guest reboot/background
recovery before verifier startup, offline and forced Doze recovery pass. Six unique
effects/receipts are verified. Settings restoration, tagged fixture removal and
both package removals pass. Exact source/APK/output/tool binding is in
recovery-lifecycle-evidence.json. Guest reboot took78674ms; this is not a Mac restart.

Published delivered snapshots for event totals1–6 are0,1,0,0,1,1. Repeated work can
publish a later zero; these snapshots do not isolate the recovered receipt's original
worker counter. None reproduces the earlier inflated2; exact transition-count
semantics are proven by local regressions, with native unique effects separately
verified. Do not call these snapshots a native count of newly inserted events.

Latest-APK provider8/8, worker5/5 and lifecycle11+expected termination pass. The
39-store run remains separately bound to its earlier APK, with3 namespace fault
checks on the latest APK. Reconcile applicable store regression after final source
freeze. Protected call-end wake, positive rendered UI, full P01–P10 audit/review/fixes
and archive remain required. The recovery guest is ready for those next tests; the
original stuck guest is still isolated and untouched.


## Protected emulator telephony call-end acceptance

TelephonyProviderTest uses no source-row insertion: it starts from an empty call
provider and token-owned LOCAL calendar, enables committed policy and explicitly
cancels all initial work to terminal state before the host simulates one fictional
inbound call (+12025550109). Android registry RINGING then IDLE is observed. Before
any verifier instrumentation or Activity launch, a unique event and fresh completed
status appear. Broadcast history at the recorded call-end wall time proves system
uid1000 delivered IDLE PHONE_STATE to AutomaticWakeReceiver (two permitted variants;
privileged variants are correctly denied). Actual missed-call row141, zero talk
time, matching durable receipt/fingerprint/ID and repeat catch-up without duplicate
pass. Source→verified observations14273ms and21219ms (second includes explicit repeat
catch-up) are not insertion timestamps or deadlines. Five native fixture/acceptance/
cleanup methods pass; exact fixture call/calendar/events and both packages removed.

The first console acknowledged call creation but gsm list omitted the real ringing
call. Its original console-only cleanup claim was insufficient. Radio diagnosis
found RINGING; explicit cancel ended it and produced the exact fixture row140. A
scoped ID/date/number deletion followed by three empty provider readbacks and actual
radio IDLE proves corrected cleanup. An initial deletion-output assertion expected
a message that this content tool does not print; deletion itself had succeeded and
readback proved absence. The harness now uses actual Android call state, cancels
the fixture regardless of the misleading console list, and requires a processed
exact fixture row before failure cleanup. Unknown termination preserves cleanup
code. Host regression covers hidden console calls, uncertain creation, existing
call refusal and cleanup ordering;61 host tests pass.

Exact source/APK/native/broadcast proof, red and explicit cleanup correction are in
telephony-evidence.json. No product behavior changed in this slice. This qualifies
one supported incoming missed-call path, not answered/outgoing physical telephony
or cloud sync. Remaining major gates: positive rendered settings/permission/import/
recovery journeys, final-source regression and full P01–P10 review/fixes/archive.

- `d2221f9`: protected actual emulator missed-call end→system receiver→background work→real event/receipt, five native checks and cleanup;61 host checks. Console-only cleanup red is explicitly superseded by exact-row/actual-radio readback. Positive rendered UI and final review remain open.


## Rendered settings and system-inset acceptance

PositiveUiTest/RenderedUi use visible accessibility nodes and actual injected
touches, never direct app click handlers. Five native methods pass: fixture setup,
actual top-content/system-bar geometry across settings/manual/import/recovery,
settings journey, calendar revocation/restoration and cleanup.19 PNG screenshots
and matching accessibility trees are pulled with hashes before uninstall. The
settings-enabled and recovery-geometry screenshots were visually inspected. Exact
source/APK/native/artifact/tool/red binding is in settings-ui-evidence.json.

Settings journey proves permission denial then grants, grants alone do not activate,
fixture destination selection, each call filter, all-filters-off disabled enable,
activation confirmation cancellation then acceptance, persisted selected policy,
pause and reopen, and calendar permission revocation/restoration without enabling.
All effects remain0 as expected for an empty source. Both packages/calendar fixture
are removed; original emulator environment is verified restored.67 host tests and
full108-JVM/APK/lint build pass.

Native red found real Android16 edge-to-edge overlap: first content42px inside a
136px system area. ActivityInsets applies system/cutout padding to all four native
ScrollViews and selects readable day/night system icon appearance, without new
dependencies or a UI framework change. The API26–29 branch/night appearance compile
but are not runtime-qualified by this API36/day run. UI driver red also exposed
accessibility preceding rendered permission frames; it now waits for idle/actual
dialog progression and retries the same intended choice. A transient permission
controller may restore one revoked permission automatically when its sibling is
already granted; driver accepts verified restoration without inventing another
Allow button. These driver failures were not reported as app acceptance passes.

Remaining: positive historical preview/confirmation/cancel/progress/resume/repeat,
read-only existing-effect checks, explicit READY approval/cancellation/withdrawal,
edited/deleted review/suppression/retention UI, final-source regression and complete
P01–P10 review/fixes/archive. Earlier provider/worker/lifecycle/telephony receipts
remain separately source-bound before this presentation-only fix.

- `1c4c76c`: native-red/green system-inset fix, rendered settings/permissions/filter/activation/pause/reopen/restoration acceptance, five native methods/19 screenshots/cleanup;108 JVM and67 host tests. Positive import/recovery journeys remain open.

## Rendered import acceptance checkpoint

The expanded import UI suite passes five native methods on the API36 recovery
guest. Actual touches cover edited date boundaries, read-only preview, confirmation
cancellation, cancellation while the keyboard remains visible, explicit import
and duplicate-free repetition. A persisted 120-item import is stopped by the user
at two saved items, reopened and explicitly resumed to120 saved/0 review; native
readback shows123 events including the earlier three. Tagged fixtures, packages
and emulator settings were cleaned/restored. See imports-ui-evidence.json for
source/APK binding and ten PNG/tree pairs. The completed screenshot was inspected.

The original native failure exposed keyboard-obscured controls; API30+ inset
padding now includes the IME. This is separate from recovery UI acceptance, which
remains pending, along with final-source regression and the P01–P10 full review.

Visual review qualification: the completed import screenshot retains stale stop
feedback. Import UI data/touch checks passed, but full UI acceptance remains open
until that finding is fixed and rerun; see local-review.md.

## Completion-feedback follow-up

- [~] Native final-feedback assertion and successful-result feedback reset.
  Full Android build passes (72s, unchanged inputs);69 host tests and Conductor
  validation pass. Native reruns before the fix failed at active63/120 timeout
  and Activity-launch/input-dispatch ANR respectively, not at the new assertion.
  Both cleaned/restored. Fixed-source native acceptance is running; no completion
  claim. See local-review.md and .local/parity-evidence/import-feedback-* files.

## Fixed-source rendered import closure

Commit52b52cd passes all five native import methods, including empty final
feedback. User Stop at3 saved, explicit reopen/resume to120 saved/0 review,123
real events including earlier3. Cleanup/packages/environment restored. Final
completed PNG inspected: no stale stop message. See imports-ui-evidence.json.
The earlier timeout and launch ANR remain historical, not silently relabelled as
failing feedback assertions; this run did not reproduce either. Recovery UI and
final-source regression/P01–P10 review remain pending.

## Rendered recovery checkpoint

Recovery UI passes six native methods on the dedicated API36 guest, with10 PNG/
accessibility-tree pairs. Actual touches prove permission restoration alone does
not approve insertion; preview cancellation creates no event; explicit approval
creates one verified event and clears payload. Existing-effect check works with
WRITE_CALENDAR denied. Human edit survives, deletion is not recreated, suppression
is explicit/cancellable and keeps receipt identity; explicit retention ages a
fixture payload and removes it while preserving metadata. Cleanup, package removal
and environment restoration pass. See recovery-ui-evidence.json. Queued approval
withdrawal across leaving/returning remains an open review candidate; final-source
regression/full P01–P10 audit and archive remain pending.

## Queued recovery approval withdrawal — native red/green

Native fault reproduced READY expected/CONFIRMED actual after real confirmation,
Home, return to same Activity and release of a held executor. The old resumed
boolean revived stale consent. approvalSession now changes onPause; each positive
confirmation captures its own token and the delivery supplier requires that token
as well as resumed. Already-crossed effects remain tracked, not falsely undone.
The final suite passes7 native methods including withdrawal, fresh permission/
preview/cancel/approve, read-only check, edit/deletion/suppression and retention.
Cleanup/packages/environment restored; actual preview dialog PNG inspected.
See recovery-withdrawal-red-evidence.json and recovery-ui-evidence.json.
Full Android build passes56s;70 host tests and Conductor validation pass.
Final-source store/provider/worker/lifecycle/telephony/settings/import regression
and full P01–P10 review/handoff/archive remain outstanding.

## Final-source regression started

On daf794a app/test inputs, store39/39 and source-identity3/3 native checks pass;
both suites remove development packages. See final-store-evidence.json and
final-source-identity-evidence.json. Earlier UI/settings/import/provider/worker/
lifecycle/telephony evidence remains separately source-bound; final regression
and full review are not yet complete. Metadata now correctly records schema6.

## Provider/worker final-pass checkpoint

Provider8/8 and worker5/5 pass with scoped cleanup/package removal. See
final-provider-evidence.json and final-worker-evidence.json. Worker observation
was extended60→120s only after native diagnostics showed RUNNING/61 verified/
1 pending/0 review at60s; both original failures remain retained. Exact counts,
filter/idempotency/pause/reactivation assertions are unchanged. Full Android
build passes61s and70 host tests pass. Test-only inputs differ from prior store
receipt; production app remains daf794a. Wake-lock acquisition deadline candidate
in local-review.md must receive a held-lock fault test and disposition before
review closure. Lifecycle/telephony/UI final pass and complete audit remain open.

## Wake admission deadline — native red/green

The held-lock native boundary test fails before the fix: a100ms scheduling budget
cannot return even within1s while the delivery monitor remains held. No actual
broadcast ANR is claimed. A bounded admission executor now permits one running
and one queued request, rejecting overload as unconfirmed. The receiver caller
waits only its remaining budget; timed-out tasks are cancelled/purged. Deadline/
interruption permission is rechecked under the delivery lock before reading and
scheduling policy, preventing expired admission from publishing later work.
An expired budget cannot acknowledge an empty request list.

Native held-lock test now passes while the monitor remains held; both red/green
packages removed. See wake-budget-red-evidence.json and wake-budget-evidence.json.
Build passes110 JVM tests,0 failures/errors/skips;71 host tests pass. No new runtime
dependency or schema change. Periodic/reopen recovery remains the fallback for
unconfirmed/overloaded wakes. Full lifecycle/telephony/current-source acceptance
and P01–P10 audit/review/handoff/archive remain required.

## Lifecycle reboot observation and recovered cleanup

The retry passes preparation, launcher recovery and expected held-insertion death,
then aborts on wait-for-device45s before the intended180s boot loop. The guest
continues booting, becomes responsive and completes boot without another restart.
The retained cleanup fixture then passes; original power/network state is restored
and verified, both packages removed. Original run stays failed; see
lifecycle-reboot-observation-evidence.json and lifecycle-reboot-recovery-evidence.json.
Removed the premature45s connection gate: expected offline observations now use
the existing180s boot loop. Added pre-cleanup exit-info/meminfo for unexplained
process exits.71 host tests pass. Full current-source acceptance remains pending.

## Frozen-build closure audit in progress

closure-audit.md maps every P01–P10 clause to implementation, tests and remaining
proof. Current-source static review covers delivery/source/import/persistence,
policy/status, manifest, screen callbacks and CI/tooling boundaries. No additional
confirmed production issue in these inspected paths. Matrix session70804 remains
authoritative for runtime progress; final receipt inspection and complete review/
handoff/archive are still open. No plan completion checkbox is inferred from
this interim review.
