# Track 4 closure audit — in progress

Closure is unproven until the frozen-build matrix and final review below pass.
The eight-step goal remains intact. Earlier receipts are historical, separately
source/APK bound; they are not combined into a claim about the current build.

## Requirement-to-proof map

| Requirement and explicit clauses | Implementation and test evidence to inspect | Current gate |
|---|---|---|
| P01 enable/pause; three independent filters; no contacts/SMS; explicit activation boundary; history confirmation | ActivationControl/AndroidActivationStore and AutomaticScheduleControl; activation/settings JVM/native tests; WorkerProviderTest verifies 125-row filter/catch-up, repeat, pause/reactivation and denied unrelated permissions; PositiveUiTest touches permission, each filter and activation/cancellation/pause controls; ImportUiTest confirms history separately | Final store, worker and settings/import UI receipts required |
| P02 arbitrary bounded catch-up; persisted progress; namespace/reset/reused-ID refusal | PagedCalls captures/validates upper and processed anchors before/after pages; ScanDelivery revalidates individual candidates and advances only after handling; SqliteLedger commits checkpoint/rollover separately; source namespace false/thrown commits fail closed; paging/scan/automatic JVM and native source/store/worker tests | Final source-identity/store/worker receipts required |
| P03 exact instants, raw presentation/null/withheld/unknown; long/zero duration; source facts separate from marker duration | CallRecord, Kotlin CallEvent/CallIdentity and preserving Java golden contracts; Math exact arithmetic; one-minute marker explicitly says zero actual duration; source-fact native migrations and real-provider boundary tests | Final store/provider and current JVM results required |
| P04 pre-effect committed intent; no blind ambiguous reinsertion; known-ID retention; owner/generation isolation | DurableDelivery commits READY then IN_FLIGHT before insert, persists ATTEMPTED ID before readback; no enclosing SQL transaction allowed; ambiguous missing marker goes REVIEW; AutomaticDelivery and HistoricalImport enforce ownership; fault/JVM/store and held-insertion lifecycle tests | Final store/lifecycle proof required |
| P05 unique effects, overlap serialization; marker/ID recovery; preserve edits and intentional deletion | Shared single-process delivery monitor; exact returned/read ID and fingerprints; AndroidCalendarSink rejects policy/marker/content edits without mutation; no update/delete provider port in production recovery; provider/worker/lifecycle and RecoveryUiTest verify outcomes | Final provider/worker/lifecycle/recovery UI receipts required |
| P06 preserve old receipts; conservative identity binding; destination isolation | SqliteLedger schema1–6 preserving migrations retain legacy receipts and nullable unknown facts/owners; binding requires original fingerprint agreement; source/calendar composite key; route changes never move/delete old effects | Final store/provider and migration assertions required |
| P07 range preview/count; confirm/cancel/progress/resume; idempotency and actual partial effects; read-only legacy prototype | HistoricalImport freezes route/range/filter/source signatures; SqliteImportStore atomically commits candidate outcome plus progress with revision checks; ImportRange and matcher JVM tests; real provider import tests; rendered 120-item Stop/reopen/resume test checks exact counts and cleared stale feedback | Final provider/import UI receipts and retained matcher prototype proof required |
| P08 reopen/guest reboot/process death; denial/revocation; unavailable/read-only calendars; offline/Doze; measured scheduling/execution/delivery | WorkManager2.12.0 scheduling, protected AutomaticWakeReceiver, generation checks and bounded wake admission; native held-lock red/green; LifecycleProviderTest and telephony harness observe effects before verifier; worker/provider permission/route cases; native recovery check with WRITE denied | Final lifecycle/telephony/worker/provider/recovery UI receipts required; earlier crash and reboot-window failures remain retained |
| P09 visible pending/saved/review/failed counts; nonpersonal diagnostics; private/free/no reminders; payload minimisation and explanations | DeliveryCounts includes late legacy without double counting; WorkStatus generation-scoped committed results and failure-attempt count, with false/thrown uncertainty hidden; AndroidCalendarSink explicitly sets privacy/availability/alarm policy; schema6 storage-age expiry and receipt tombstones; rendered settings/import/recovery screenshots and native readback | Final store/UI/provider/worker proof plus screenshot inspection required |
| P10 meaningful test-first matrix; build/CI/artifact binding; scoped recovery; review/fixes; handoff and archive only after closure | 110 JVM tests; host harness safety tests; build receipt input/APK hashes; serial native matrix; failure receipts and cleanup recovery; code review and requirement reconciliation; project docs and dependent-track handoff | Matrix running; full review/handoff/archive incomplete |

## Source inspection recorded in this pass

- Delivery/recovery: DurableDelivery, ManualDelivery, ReceiptRecovery, ReadyReapproval,
  AndroidCalendarSink. Checked commit-before-effect order, known-ID preservation,
  exact ID checks, content/privacy edit refusal, marker ambiguity, source binding,
  suppression/expiry refusal and absence of automatic overwrite/delete paths.
- Source/progress: CallRecord, CallEvent/CallIdentity Kotlin model, AndroidCallLogSource,
  PagedCalls, ScanDelivery, AutomaticDelivery. Checked canonical ID/range bounds,
  source signature/raw-presentation binding, per-row revalidation, frozen/extended
  anchors, scoped pending recovery and exclusion of unsupported call types.
- Persistence/import: SqliteLedger, HistoricalImport and SqliteImportStore. Checked
  schema1–6 preservation, old/null metadata treatment, independent SQL commits,
  immutable route/snapshot, monotonic progress, revision comparison, candidate
  order and atomic outcome/count commits. Retention clears payload without erasing
  identities or inferring source age from old call dates.
- Policy/status: ActivationControl, AndroidActivationStore, ActivationCommitGuard,
  AutomaticScheduleControl/AutomaticScheduling, AutomaticLogWorker,
  AutomaticWakeReceiver, WorkStatus/AndroidWorkStatusStore and PayloadRetention.
  Checked generation authority, false/thrown commit uncertainty, publication/order,
  bounded admission and acknowledgements, backlog/retry limits, sanitized status,
  failure counters and absence of caller fields in work input/output/logging.
- Source manifest: call/phone-state/calendar/boot permissions; no network, contacts,
  SMS or audio permission. Old boot receiver is disabled; old activity is not
  exported; new settings/import/recovery screens are not exported. Current
  launcher and protected system-wake actions remain deliberate entry points.

Screen callback/lifecycle paths, harness/CI coverage, remaining changed-file scope,
final screenshots, current native receipts and all plan checkboxes still need final
reconciliation. This inspection log is not a completed review recommendation.

## Completion boundaries

Physical incoming/outgoing/answered/missed calls, remote Google/Outlook sync,
production signing/store admission, wider Android/OEM qualification and community
operation remain outside the emulator acceptance selected by the user. They must
stay explicitly pending in the handoff. No host restart is an unblock option.
The old stuck guest's state is independent of the qualified recovery guest.

## Additional review/verification readback

- Current full Java AOSP and Kotlin/Compose-prototype ktfmt dry-run checks pass
  using the qualified JDK17. The first Kotlin invocation used the host JDK11
  and failed before formatter execution; this was a tool-runtime mismatch, not
  a source-format failure. No formatter modified frozen inputs.
- Current lint XML:0 errors,32 warnings,5 hints. Current JVM XML:110 tests,
  0 failures/errors/skips. Hosted CI is not claimed: workflow source is inspected
  and pins action/tool identities, verifies host/Conductor checks and compiles
  app/test plus unsigned release; native acceptance remains explicitly selected.
- Screen callback inspection confirms background database/provider operations,
  stale/destroyed-view callback guards, explicit permission tap-again, no implicit
  destination fallback, import onPause stop, durable session restoration and
  permanent queued recovery-approval withdrawal after pause. Native insets cover
  system bars/cutouts/IME; older API/night rendering is not inferred from API36.
- Diagnostics inspection finds only constant WAKE_ENQUEUE_UNCONFIRMED log calls
  in the new production slice, with no exception text, phone/account fields,
  password/client-secret/API-key handling. Work data contains generation/status
  and counts, not caller payloads. Source hash/facts and displayed user-owned
  calendar labels are intentionally distinct from diagnostic output.

Frozen matrix currently confirms store39, source-identity3, wake-budget1 and
provider8 with cleanup; worker and later suites remain in flight. Final P01–P10
closure, exact receipt inspection and screenshots remain unproven.

## Packaged permission readback and remaining harness correction

Actual debug APK aapt permissions include WorkManager normal WAKE_LOCK,
ACCESS_NETWORK_STATE and FOREGROUND_SERVICE plus the app's signature receiver
permission, beyond the root manifest's call/phone-state/calendar/boot declarations.
No INTERNET, READ_CONTACTS, READ_SMS, RECORD_AUDIO or WRITE_CALL_LOG is present.
This is packaged readback, not an inference from source declarations alone.

The earlier failed reboot receipt reports emulator_restored verified/unchanged
while ADB was offline, because reboot did not mark EmulatorEnvironment.changed.
The recorded manual recovery subsequently verified original settings and cleanup;
the current passing lifecycle run also has a verified doze_restore readback.
Nevertheless the harness failure path must be corrected before final review closes:
track reboot as an environment mutation before ADB dispatch, and add an uncertain-
reboot regression that changes awake state, fails, then restores the original.
Defer harness edits until the live frozen matrix completes; no current native
proof or production source is to be changed underneath it.
