# Track 4 closure audit — accepted emulator scope

The frozen-build matrix and final review pass for the selected emulator scope.
The eight-step goal is reconciled below. Earlier receipts remain historical and
separately bound; current proof is final-acceptance-evidence.json. Archive and
registry readback are the final administrative closure checks.

## Requirement-to-proof map

| Requirement and explicit clauses | Implementation and test evidence to inspect | Current gate |
|---|---|---|
| P01 enable/pause; three independent filters; no contacts/SMS; explicit activation boundary; history confirmation | ActivationControl/AndroidActivationStore and AutomaticScheduleControl; activation/settings JVM/native tests; WorkerProviderTest verifies 125-row filter/catch-up, repeat, pause/reactivation and denied unrelated permissions; PositiveUiTest touches permission, each filter and activation/cancellation/pause controls; ImportUiTest confirms history separately | Passed — final-acceptance-evidence.json and final-review.md; scope limits retained |
| P02 arbitrary bounded catch-up; persisted progress; namespace/reset/reused-ID refusal | PagedCalls captures/validates upper and processed anchors before/after pages; ScanDelivery revalidates individual candidates and advances only after handling; SqliteLedger commits checkpoint/rollover separately; source namespace false/thrown commits fail closed; paging/scan/automatic JVM and native source/store/worker tests | Passed — final-acceptance-evidence.json and final-review.md; scope limits retained |
| P03 exact instants, raw presentation/null/withheld/unknown; long/zero duration; source facts separate from marker duration | CallRecord, Kotlin CallEvent/CallIdentity and preserving Java golden contracts; Math exact arithmetic; one-minute marker explicitly says zero actual duration; source-fact native migrations and real-provider boundary tests | Passed — final-acceptance-evidence.json and final-review.md; scope limits retained |
| P04 pre-effect committed intent; no blind ambiguous reinsertion; known-ID retention; owner/generation isolation | DurableDelivery commits READY then IN_FLIGHT before insert, persists ATTEMPTED ID before readback; no enclosing SQL transaction allowed; ambiguous missing marker goes REVIEW; AutomaticDelivery and HistoricalImport enforce ownership; fault/JVM/store and held-insertion lifecycle tests | Passed — final-acceptance-evidence.json and final-review.md; scope limits retained |
| P05 unique effects, overlap serialization; marker/ID recovery; preserve edits and intentional deletion | Shared single-process delivery monitor; exact returned/read ID and fingerprints; AndroidCalendarSink rejects policy/marker/content edits without mutation; no update/delete provider port in production recovery; provider/worker/lifecycle and RecoveryUiTest verify outcomes | Passed — final-acceptance-evidence.json and final-review.md; scope limits retained |
| P06 preserve old receipts; conservative identity binding; destination isolation | SqliteLedger schema1–6 preserving migrations retain legacy receipts and nullable unknown facts/owners; binding requires original fingerprint agreement; source/calendar composite key; route changes never move/delete old effects | Passed — final-acceptance-evidence.json and final-review.md; scope limits retained |
| P07 range preview/count; confirm/cancel/progress/resume; idempotency and actual partial effects; read-only legacy prototype | HistoricalImport freezes route/range/filter/source signatures; SqliteImportStore atomically commits candidate outcome plus progress with revision checks; ImportRange and matcher JVM tests; real provider import tests; rendered 120-item Stop/reopen/resume test checks exact counts and cleared stale feedback | Passed — final-acceptance-evidence.json and final-review.md; scope limits retained |
| P08 reopen/guest reboot/process death; denial/revocation; unavailable/read-only calendars; offline/Doze; measured scheduling/execution/delivery | WorkManager2.12.0 scheduling, protected AutomaticWakeReceiver, generation checks and bounded wake admission; native held-lock red/green; LifecycleProviderTest and telephony harness observe effects before verifier; worker/provider permission/route cases; native recovery check with WRITE denied | Passed — final-acceptance-evidence.json and final-review.md; scope limits retained |
| P09 visible pending/saved/review/failed counts; nonpersonal diagnostics; private/free/no reminders; payload minimisation and explanations | DeliveryCounts includes late legacy without double counting; WorkStatus generation-scoped committed results and failure-attempt count, with false/thrown uncertainty hidden; AndroidCalendarSink explicitly sets privacy/availability/alarm policy; schema6 storage-age expiry and receipt tombstones; rendered settings/import/recovery screenshots and native readback | Passed — final-acceptance-evidence.json and final-review.md; scope limits retained |
| P10 meaningful test-first matrix; build/CI/artifact binding; scoped recovery; review/fixes; handoff and archive only after closure | 110 JVM tests; host harness safety tests; build receipt input/APK hashes; serial native matrix; failure receipts and cleanup recovery; code review and requirement reconciliation; project docs and dependent-track handoff | Passed — final-acceptance-evidence.json and final-review.md; scope limits retained |

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

Screen callback/lifecycle paths, relevant harness/CI and changed-file scope,
current receipts/screenshots and all plan checkboxes are reconciled in
final-review.md. The accepted scope and residual external gates are explicit.

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

The completed frozen matrix confirms all10 suites with cleanup; exact receipt,
input/APK and screenshot hashes are verified in final-acceptance-evidence.json.

## Packaged permission readback and resolved harness correction

Actual debug APK aapt permissions include WorkManager normal WAKE_LOCK,
ACCESS_NETWORK_STATE and FOREGROUND_SERVICE plus the app's signature receiver
permission, beyond the root manifest's call/phone-state/calendar/boot declarations.
No INTERNET, READ_CONTACTS, READ_SMS, RECORD_AUDIO or WRITE_CALL_LOG is present.
This is packaged readback, not an inference from source declarations alone.

The earlier failed reboot receipt reports emulator_restored verified/unchanged
while ADB was offline, because reboot did not mark EmulatorEnvironment.changed.
The recorded manual recovery subsequently verified original settings and cleanup;
the current passing lifecycle run also has a verified doze_restore readback.
The failure path is now corrected in088281b: reboot is tracked before ADB
dispatch; a simulated uncertain Awake→Asleep reboot restores the original state.
The correction was applied after the frozen matrix and changes no app/test input.

## Final reconciliation

All10 frozen-build suites pass:89 successful native invocations and1 expected
termination. Current source/APK/receipt/UI hashes are verified. Settings enable,
completed import, recovery confirmation and expiry screenshots were inspected.
Post-run readback verifies a responsive dedicated guest, both development packages
absent, empty call/calendar providers and restored original environment.
H01 reboot restoration is fixed in088281b with73 host tests; it affects failure
restoration only and does not alter app/test inputs. All confirmed findings are
closed in final-review.md.110 JVM tests and format/lint checks pass.

The earlier intermediate pending statements above are historical review notes,
not additional unfulfilled gates. Final handoff is handoff.md. Physical/cloud/
production and wider-platform gates remain assigned there, rather than waived.

## Goal objective eight-step audit

| Step | Outcome and authoritative evidence |
|---|---|
| 1 independent responsive guest | Passed: recovery AVD/emulator-5582, independent writable files/ports; final post-acceptance readback confirms qemu/AVD/boot response. No claim that the old instance was repaired. |
| 2 qualified environment/build | Passed: qualified JDK17/SDK36 build,110 JVM tests, unchanged source/APK hashes across all10 native suites; final artifact hashes rechecked. |
| 3 native store/fault tests | Passed:39 native methods with cleanup and package removal, plus3 source-identity faults and1 held-lock budget regression. |
| 4 provider/worker/recovery regression | Passed: provider8, worker5, rendered import5/recovery7; exact effects, IDs/counts, repeat, edits/deletion and resume assertions. |
| 5 lifecycle recovery | Passed:11 successful native invocations plus expected held-insertion process kill; force-stop/reopen, guest reboot, process death, offline/Doze and original environment restoration. |
| 6 actual call-end trigger | Passed: one emulator incoming missed call; system uid1000 IDLE variants delivered to receiver; background unique effect/receipt observed before verifier. |
| 7 rendered positive UI | Passed: settings5/import5/recovery7 and40 PNG/tree pairs; current final settings/import/reapproval/expiry frames inspected. |
| 8 review/fixes/handoff/archive | Review, all confirmed fixes and handoff pass; archive/registry readback is the final administrative action. |
