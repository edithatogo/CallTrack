# Track 4 decisions — before implementation

## Durable delivery boundary

Retain the native provider adapters and testable Java core. Add a durable outbox before adding scheduling/import. Commit READY, then IN_FLIGHT before calling insert; commit a known ID immediately on return. A resumed IN_FLIGHT without a marker cannot distinguish a pre-insert crash from a lost-marker/deleted post-insert event, so it must require review. This sacrifices unattended retry for that ambiguous case to preserve the no-duplicate/no-recreation requirement. A READY record is safe to attempt.

Use one process-wide delivery lock for the app's single process plus short SQLite transactions for each state transition. No provider call is hidden inside an uncommitted SQL intent. All workers/UI delivery share this path; multi-process delivery is unsupported unless redesigned with a durable lease and qualified separately.

Store source-content fingerprint separately from frozen event fingerprint: changes in cached contact labels or the device display timezone must not rewrite existing events. A changed source ID/content binding is a source-review condition. After confirmation retain minimal identity/marker/fingerprints/ID, not the original personal event payload. Review/missing-event states also prevent recreation; explicit user resolution must not be guessed.

Preserve v1 receipts when upgrading SQLite; old fingerprints/IDs remain useful, but must be verified before binding to new source signatures. Room migration is evaluated in findings; no destructive table recreation is acceptable.

## User steering

Use emulator call/provider/background tests now. Physical-call acceptance remains pending. Aim for prompt content-triggered work with persisted periodic/reopen catch-up; measure actual latency. Do not claim exact execution deadlines or change the connected phone's power settings for emulator tests.

## Dependencies — initial disposition

Native SQLite remains selected for the bounded outbox migration, subject to actual native tests. Room is a takeover candidate for schema/DAO/migration tooling, not an automatic addition during this behavioural change. WorkManager is already present at 2.7.1; refresh compatibility and fix constraints before selecting the scheduling version. No experimental analytics API or preview library is needed just to observe local test timestamps. Final dependency decisions/versions remain required before scheduling implementation.

## Coordinator/import boundaries to implement

- Use bounded source-ID paging with a stable upper snapshot, persisted per-row progress and anchor signature/reset checks. An unexpected missing/changed anchor pauses for source review rather than trusting a stale numeric checkpoint. Successful/reviewed/filtered rows advance deliberately; permission/provider/invalid-data failures must not silently skip the rest of history.
- Automatic enable/resume defines an explicit future-call boundary. Delays/process death/reboot retain the current activation and catch up; an explicit pause/re-enable starts a new activation rather than silently importing paused history. Manual historical import is separate.
- Every pending intent needs an operation owner/activation or import-session generation before generic background draining is wired. Cancelled imports and old paused activations are not globally resumable. Explicit manual retry may authorise the selected call separately.
- Import preview freezes the selected range/filter/destination and candidate source signatures. Confirmation authorises that snapshot, not a changing query. Revalidate changed candidates, retain progress across interruption, cancel without deleting confirmed events and clear preview personal payload when cancelled/completed.
- Source facts (exact source instant, actual seconds, raw type/presentation) must survive payload minimisation; logical clearing of names/numbers is not a claim of forensic erasure.
- All app delivery currently assumes one Android process; UI pause/settings publication must coordinate with the same delivery lock so a displayed completed pause has no later writes from an old worker.

## Operation scope implementation checkpoint

Schema3 adds nullable opaque operation owner plus nonnegative generation. Existing
schema1 receipts and schema2 pending rows are preserved without inventing ownership.
The legacy unscoped API remains for compatibility/manual callers; new automatic/
import coordination must supply a scoped operation and an allowed callback tied to
its persisted activation/session. Pending work requires the exact owner/generation;
resuming through an unrelated or legacy unscoped caller pauses without changing it.
Confirmed receipts may be verified by another explicitly delivered source operation
without recreating events. Ownership persists after payload purge.

Recheck allowed after the committed IN_FLIGHT boundary as well as before it. A pause
observed at that boundary leaves the ambiguous intent for conservative recovery;
never roll it back to READY. Future coordinator pause publication must hold the same
process delivery lock to serialize its completion with provider insertion. This
engine change is not proof of a wired pause/import/scheduler UI.

## Source-fact/rendering policy and schema4

Preserve nullable raw number presentation from Cursor instead of collapsing it to
a Boolean. New raw-provider records disclose identity only for allowed(1):
restricted(2) renders Private number, payphone(4) Payphone, and unknown(3),
unavailable(5), null/unrecognized values Unknown number. Non-allowed presentation
masks cached name and number in event output. Boolean legacy constructors retain
the previous rendering and source-v1 signatures, so old golden contracts remain
unchanged. Values follow [Android CallLog reference](https://developer.android.com/reference/android/provider/CallLog.Calls).

New durable intents keep exact source milliseconds, actual seconds, raw type and
nullable presentation separately from the event payload. Source facts survive
confirmation/review/suppression and never infer actual talk time from a visual
marker. Where both inputs have raw presentation, a changed presentation enters
SOURCE_CHANGED review even if the compatibility signature remains equal. Older
rows keep facts unknown; no new fact is guessed or old event rewritten.

Schema4 preserves schema1/2/3 data and operation ownership. New facts are nullable
for migrated rows; null presentation is distinct from absent historical facts.
These retained metadata remain sensitive and lack an expiry policy; PRIVACY.md
records that limit. No destructive downgrade/backup promise follows: older APK
readers of schema4 and full recovery/restore policy remain unqualified.


## Bounded source traversal checkpoint

Use canonical nonnegative CallLog IDs with a frozen upper ID and ascending bounded
pages (1–500 rows). Persist each operation's upper/last anchor bindings separately
from delivery receipts in schema5. The coordinator must advance only after handling
a row; a returned page is not completed work. Namespace, upper or processed anchor
changes require review, rather than silently resetting progress. New rows above the
upper bound belong to a later scan. This freezes ID bounds, not provider contents;
historical confirmation still requires persisted candidates and revalidation before
effects. Synthetic cursor/query contracts do not prove every vendor provider honors
limits. No scheduler, import confirmation or automatic UI is enabled by this change.


## Scan delivery coordinator contract

A bounded scan step shares the delivery process lock. Persist snapshot before
handling, recheck each candidate binding before effects, apply activation-time and
incoming/outgoing/missed policy, commit each delivery receipt before checkpoint
advancement. Review receipts are terminal handled rows; permission, destination,
pause or failure stop at the current row. A crash between receipt and checkpoint
replays through delivery verification. Counts describe this batch, not a persisted
user status total. Activation policy/route must be immutable within an operation;
its persisted publication and generation lifecycle remain the next integration task.
The new coordinator is not yet invoked from launcher/background work. No automatic
activation, historical preview or scheduling completion is claimed.


## Activation publication

Use an immutable versioned settings document with generation, enabled state,
destination ID, timezone, future start boundary and incoming/outgoing/missed flags.
Default is absent/disabled; no migration enables logging. Every explicit activation
increments generation and starts at its recorded time; calls during a pause need
explicit historical import. Pause retains the disabled generation, leaving old
pending receipts for explicit recovery rather than assigning them to new work.
Publication and final effect authorization share DELIVERY_LOCK. Workers must match
the full settings document. Generation overflow fails before publication. The
Android adapter uses one synchronous SharedPreferences commit, with a process-wide
fail-closed latch if commit reports failure because SharedPreferences can change
memory before disk success. Restart requires fresh stored readback; no claim of
rollback on disk failure. Existing backup/transfer exclusions cover these settings.
Native persistence and fault injection remain unqualified until runtime tests;
launcher/background invocation and explicit pending-recovery UI remain required.


## Automatic scan rollover integration requirement

A completed frozen checkpoint must not leave automatic work permanently complete.
The next scan must validate the previous source anchor, capture a new upper bound,
and preserve prior delivery receipts and activation ownership. Publication must be
atomic against overlapping workers and pause. Do not relax the existing immutable
checkpoint setter to permit arbitrary rebinding. Source-independent recovery of
pending intents must precede source traversal so deleted source anchors do not hide
an uncertain provider insertion. Rollover and recovery remain unimplemented at this
checkpoint; they are prerequisites to scheduler/UI integration, not a scope deferral.


## Verified automatic snapshot rollover

Add an explicit completed-snapshot extension operation, leaving the ordinary frozen
checkpoint setter strict. Validate old anchors before and after capturing the new
upper bound; require same namespace and nondecreasing upper ID. Preserve last ID/
binding. SQLite commits only if the stored checkpoint exactly matches the expected
completed state, so stale overlapping publication fails. No schema change is needed.
AutomaticDelivery binds one bounded scan to the full persisted activation document
and rechecks authorization under the shared delivery lock. Empty-to-first-call and
later-call scans use this same path. Deleted/changed processed anchors require review.
Source-independent pending recovery, scheduler/UI wiring and historical snapshots
remain required; the new runner is not yet launched automatically by Android.


## Recovery before source traversal

AutomaticDelivery queries at most the requested page size (1–500) pending keys,
restricted to exact operation owner/generation and destination. Only READY,
IN_FLIGHT and ATTEMPTED qualify. Terminal receipts, unowned legacy work, cancelled/
older generations and other destinations remain excluded. Resume each through the
existing durable protocol before reading any source anchor. A nonempty recovery
batch returns incomplete even when all its rows finish; the next invocation checks
remaining work then starts scanning. Terminal review is counted and removed from
future pending selection; permission, pause or provider failure stops the batch.
No source checkpoint advances during recovery. Existing receipt marker/known-ID
verification decides recovery; absence never licenses blind reinsertion. Pending
rows belonging to paused/old operations still need explicit user recovery controls.


## Frozen historical import protocol

Use [from,to) source instants and incoming/outgoing/missed flags, explicit BUILDING,
PREVIEW, CONFIRMED, COMPLETE, CANCELLED and REVIEW phases. Capture an upper source
ID, then append bounded candidate IDs/keys/bindings and scan progress atomically.
This is an observed candidate snapshot, not an atomic provider-wide read. New IDs
above the bound are excluded. Source changes during preview can require review;
every candidate is revalidated before its first effect. Contact labels are not part
of source identity and can refresh at delivery. Preview stores no personal payload.
Only completed preview can be confirmed. Delivery has its own operation identity,
commits the durable receipt before per-candidate progress, and reconciles matching
owned receipts without requiring the source row after a crash. Repeated import uses
existing confirmed receipt verification; no blind recreation of deleted/edited events.
Changed/missing candidates count as review while other candidates can continue.
Cancellation preserves prior effects/receipts, disables future effects and never
implicitly deletes calendar data. A cancelled session cannot be resumed; a new
confirmed preview is required. Interrupted confirmed sessions remain resumable.

Use dedicated SQLite import schema1 for sessions/candidate sets, with existing
SqliteLedger remaining the authoritative delivery outbox. This isolates import
metadata/retention evolution and avoids modifying legacy receipt formats. Sessions
and candidate appends use one transaction with revision comparison; provider effects
are outside this transaction. Pending payload remains governed by the existing
outbox. Native storage/transaction proof and fake calendar-port outcomes are distinct
from real CalendarContract/user UI acceptance. User controls, per-row review detail,
retention/deletion workflow, full native recovery and scheduling remain required.


## Persisted candidate outcomes and denied-source recovery

Import schema2 adds nullable outcome codes. New result codes commit in the same
transaction as session counts/progress; failures leave the candidate unprocessed
and record a retryable outcome. Successful/review results advance one candidate.
The store rejects a progress advance without a candidate outcome and validates the
first unprocessed candidate and counter classification. Failed outcome/progress
transactions roll back both records. Schema1 migration preserves session/candidate
metadata and aggregate progress, leaving prior individual outcomes unknown rather
than fabricating them from totals. Source permission errors during confirmed
delivery become PERMISSION_REQUIRED with unchanged progress; source/provider runtime
errors become FAILED. Existing source-independent receipt recovery remains first.
This creates status data for the later UI; it does not implement the UI or prove
permission revocation behavior of real CallLog/CalendarContract providers.


## Read-only legacy confidence prototype

LegacyEventMatcher accepts at most500 observed events and500 source rows, with
explicit completeness flags and caller-supplied text from a historical rendering
profile. It has no provider/receipt mutation port. Results retain IDs/confidence
only. Nearest-second rounding is based on pinned APK inspection CT-R/CT-E in the
baseline archive; old int-overflow durations and unsupported call types are rejected,
not emulated. Zero-duration legacy tuples have equal endpoints, distinct from the
new one-minute visual marker. Modern marked events are excluded. Matching title/
description can support a candidate; edited text, unavailable identity or missing
evidence lowers confidence. Duplicate events or rounded source collisions are
ambiguous; malformed, duplicate-ID or incomplete observations never become strong.
Even STRONG_CANDIDATE is not proof of ownership, adoption permission or safe dedup.
NONE never authorizes insertion or proves absence of edited legacy records. No
calendar query adapter, automatic adoption, suppression or receipt binding is added.

## Scheduling research refresh (no dependency adoption yet)

Direct official release read2026-10-21 supersedes stale search snippets: WorkManager
[2.12.0 stable](https://developer.android.com/jetpack/androidx/releases/work#2.12.0)
was released2026-09-23, minSdk24. The app still uses2.7.1;2.12.0 is a qualification
candidate, not yet selected. Current minSdk26 meets that stated minimum; actual
artifact/AAR compatibility and transitive graph must be checked with builds/native
work tests. Experimental analytics/listener APIs are unnecessary for initial delivery.
Compare stability/recovery fixes, graph cost and existing WorkManager integration;
retain existing working app until replacement scheduling and recovery are proven.


## WORK-D01 — WorkManager development upgrade and durable worker

- Owner/date: Track4,2026-10-21; requirements P01/P02/P04/P08/P09.
- Existing: WorkManager2.7.1 plus legacy SyncWorker tied to network connectivity,
  old configuration/source checkpoint and unqualified recovery. Legacy UI/worker
  remain isolated; the new durable path uses distinct work names and activation.
- Options: retain2.7.1, implement JobScheduler directly, upgrade existing WorkManager,
  or defer. Direct JobScheduler would duplicate persistence/retry/reboot plumbing;
  keeping the old dependency leaves current-platform behavior unqualified.
- Candidate: work-runtime2.12.0, Apache2.0, official release2026-09-23. Actual AAR
  metadata requires compileSdk35/AGP8.6/minSdk24; current36/8.13.2/26 passes. No
  work-analytics/experimental hooks, KTX, multiprocess or testing helper added.
- Role/decision: upgrade the development dependency and qualify new AutomaticLogWorker;
  retain full production scheduling/reboot/Doze/native-delivery gates. This is not a
  public release or proof of operational latency. Official source:
  [release notes](https://developer.android.com/jetpack/androidx/releases/work#2.12.0).
- Graph cost:91 observed runtime coordinates/47 graph changes vs baseline, including
  WorkManager-internal Room2.7.0, SQLite and coroutines1.9.0; this does not migrate
  app-owned ledgers to Room or adopt coroutine APIs in app code. POM/parent licences,
  exact artifact hashes, graph delta and manifest permissions are in
  work-dependency-report.json. Dependency reports are not a vulnerability audit.
- Implementation: one worker reads the current persisted activation and requires
  matching input generation, recovers pending intents, processes at most8 pages of50,
  and respects system stop through final delivery authorization. Transient failure
  has bounded retries; permission/review stops await another wake. Nonpersonal
  generation/timestamp/result/count status distinguishes execution from receipts.
- Scheduling: ordinary one-shot work with2-second settling delay, unique append-or-
  replace chain per generation, plus15-minute periodic recovery using UPDATE. No
  network constraint. Pause persists before cancelling the app's tagged work.
  Protected phone-IDLE/boot/package-replaced signals only enqueue enabled settings;
  number extras are never read. READ_PHONE_STATE is declared but not granted by code.
  UI activation/reopen hooks remain the next integration; no settings are implicitly enabled.
- Security/privacy: no INTERNET permission in merged manifest. WorkManager contributes
  wake-lock/network-state/boot/foreground-service declarations; no foreground work is
  requested. The app's signature-only dynamic-receiver permission is transitive.
- Migration/rollback: settings and receipt formats remain unchanged; WorkManager owns
  its upgraded database. Do not downgrade that database in place. Development fixture
  uninstall removes scheduler state; any production downgrade requires separate
  qualification while preserving outbox/import data. No clearing app data to roll back.
- Promotion/expiry: retain this candidate only if real worker-to-provider delivery,
  paused/stale generations, process/reboot/reopen/offline/Doze, fixture cleanup and
  package upgrade preserve progress. Reassess graph size/failures at full acceptance.


## Automatic controls and manual-path integration

Expose a separate nonexported automatic settings Activity from the manual launcher.
Enable/apply requires calendar/call-log access, a visible writable destination, at
least one call type and an explicit from-now confirmation. Draft controls do not
change an active policy. Phone-state access is requested for prompt wakes but is
not a prerequisite for periodic recovery. Permission callbacks refresh controls;
they do not enable a policy. Reopening may request recovery for an already-enabled
policy. Pause stays available even if status cannot load. No fallback destination
is selected; unavailable active destinations are labelled explicitly.

ManualDelivery replaces the launcher action's old SliceEngine execution, sharing
DurableDelivery/outbox identity with automatic work. Existing late manual receipts
are already recognised by SqliteLedger's legacy fallback; no new identity is guessed.
All-route counts are read in one SQL snapshot, including unbound older receipts in
review without double counting bound rows. Previously verified counts do not claim
current event existence. UI labels are resource-based in English/German; missing
German strings first failed lint and were supplied before the final passing build.

Remaining review/integration work: monotonic worker status publication across late
old jobs, failed-attempt counts, permission text when a prior automatic policy exists,
draft-state recreation/accessibility, import/recovery controls and full positive
permission/provider/background acceptance. Programmatic Activity state tests are
not a substitute for guarded rendered/manual UI acceptance.
