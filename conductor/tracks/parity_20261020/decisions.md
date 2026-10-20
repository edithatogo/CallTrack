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
