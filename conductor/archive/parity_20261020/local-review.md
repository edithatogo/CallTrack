# Review Report: Track 4 local fault and scheduling checkpoint

## Final authoritative checkpoint — 2026-10-21

The selected emulator implementation/acceptance and review pass. See
[final acceptance](final-acceptance-evidence.json), [closure audit](closure-audit.md),
[final review](final-review.md) and [handoff](handoff.md). All confirmed findings,
including reboot failure restoration088281b, are fixed. Historical failures and
previous pending checkpoints below are retained; they do not override this final
scoped result. Physical/cloud/release and wider-platform gates remain pending in
the handoff. Archive follows final link/registry validation.

## Preserved chronological record

## Summary

Two correctness findings are fixed with local regressions; full Track 4 acceptance remains incomplete pending Android build and native/UI evidence.

## Verification Checks

- [ ] **Plan Compliance**: Partial - P01–P10 retain native and rendered acceptance gates.
- [x] **Style Compliance**: Pass - general guide, Java formatter and diff whitespace checks for this change.
- [x] **New Tests**: Yes - four activation-commit and three scheduling contracts; three fallback safety tests; three native fault methods prepared.
- [ ] **Test Coverage**: Partial - pure coordination/guard logic runs locally; Android wiring and new instrumentation remain uncompiled/unexecuted.
- [x] **Test Results**: Passed - 103 local JVM tests and 53 host tests. This excludes SchedulingCommitTest and is not a complete Android test run.

## Findings

### High — thrown activation commit could expose uncertain cached authority (fixed locally)

- **File**: `app/src/main/java/com/markobl/calllogsync/slice/AndroidActivationStore.java`
- **Context**: Previously only a false commit result latched uncertainty. A thrown outcome after cache mutation could leave enabled settings readable across adapters. A pure extraction of that existing policy failed the thrown-outcome regression.
- **Applied fix**: Shared ActivationCommitGuard latches before invoking the write, clears only on confirmed success and rejects subsequent reads/writes after uncertainty. Serialization errors before any write do not poison authority. The process-wide restart requirement is retained.
- **Remaining proof**: Native false/thrown cache-mutation tests must compile/run in separate instrumentation processes. This is not physical disk-exhaustion proof.

### High — policy publication and scheduling request could interleave (fixed locally)

- **File**: `app/src/main/java/com/markobl/calllogsync/slice/AutomaticScheduling.java`
- **Context**: Activation/reconciliation read or published policy under the delivery lock but submitted work afterward. An older caller could replace periodic work for a newer activation; pause cancellation could follow a newer enable. Both ordering regressions fail with the prior split lock scope.
- **Applied fix**: AutomaticScheduleControl holds the shared delivery lock through publication/read and request submission/cancellation. Android scheduling delegates to it. Awaiting asynchronous operation acknowledgements remains outside that lock. If enqueue throws, the committed enabled policy is retained for later reconciliation.
- **Remaining proof**: WorkManager enqueue/cancellation persistence and lifecycle regression must be rerun on the source-bound APK; local callbacks prove invocation order, not native persistence.

## Scope and limits

Reviewed the current diff and its activation/status stores, coordinator, worker reporting, scheduling/wake receiver, receipt recovery/reapproval and permission/confirmation call sites. No caller/account fields were added to diagnostics and no permissions, schema, dependencies or remote effects were introduced. This is a bounded review checkpoint, not an assertion that every earlier Track 4 line received final review.

Native harness now includes false/thrown activation commits and thrown status publication with retry accounting. It still refuses physical targets, wrong AVDs, stale source/APK receipts and existing packages. Its native fault checks are individually launched so the deliberate process-wide activation latch does not contaminate another check.

## Remaining acceptance sequence

1. Revalidate reconnected storage and the existing dedicated emulator; do not infer health from an old process handle.
2. Full Gradle/JVM/lint/debug and test APK build on current sources. Refresh exact artifact receipts.
3. Run the store/fault harness (39 methods), then applicable worker/lifecycle/provider regression. Verify counter correction in native recovery and scoped cleanup.
4. Qualify protected emulator call-end wake and positive rendered settings, destination/permission, import and recovery confirmations/cancellation. Existing negative UI checks do not satisfy these flows.
5. Reconcile every P01–P10 gate, finish full-track review/fixes and handoff, then archive. Physical calls, cloud sync and release remain separate pending outcomes.

## Durable event identity follow-up

Review of HistoricalImport/PagedCalls and the durable effect boundary found an
inconsistent event identity check: ReceiptRecovery required exact IDs, while
DurableDelivery accepted matching calendar/fingerprint from a wrong-ID readback or
an invalid-ID marker result. Two fault-port regressions returned SAVED/RECOVERED
before the fix. Verification now requires a valid event ID and exact requested-ID
readback; mismatches retain the known ID, become REVIEW and never reinsert.105 local
JVM tests pass. This is hostile/faulty-port proof, not evidence that Android's actual
CalendarContract adapter produced a wrong ID. Native current-source regression is
still pending. Exact red/green/source evidence: event-identity-local-evidence.json.

The [remaining acceptance ledger](remaining-acceptance.md) maps P01–P10 to existing
evidence and current closure work, including positive rendered/telephony protocols.
Final full-track review remains pending those results.

## High — uncertain source namespace reused from cache (fixed and native-verified)

AndroidCallLogSource previously threw on its first failed identity commit but then
returned the cached UUID to another adapter. Native false-commit-after-cache-update
regression reproduced that exact behavior. The process-wide synchronized namespace
latch now blocks reuse after false or thrown writes; successful committed namespace
reuse remains unchanged. Three native checks pass, with package cleanup. This
protects source binding without changing IDs, schema or dependencies; app-process
restart reloads disk state. See source-identity-evidence.json. Broader runtime
regression is still pending on this latest APK.

## High — enforced edge-to-edge system bars obscure content (fixed and native-verified)

Real screenshots and a native geometry regression reproduced first content42px
inside136px of system status/cutout space. Shared ActivityInsets applies actual
insets and readable icon appearance to the four Views screens. Native geometry
checks across all four pass; settings interactions and reviewed screenshots pass.
No dependency, storage or scheduling behavior changed. Legacy API26–29 and night
appearance are compiled, not native-qualified here. See settings-ui-evidence.json.

## Open rendered import feedback finding

The expanded import suite passes its data/touch assertions, but visual inspection
of import-resumed-complete.png shows the stale “The run stopped” feedback below
Session complete/120 verified/0 review. HistoricalImportActivity.perform sets
import_blocked when stopped is non-null and does not clear feedback after later
success. Treat full positive import UI acceptance as incomplete until a native
regression checks the final feedback, the stale message is cleared and the
source-bound suite is rerun. This finding does not contradict the verified123
events or fixture cleanup.

### Completion-feedback remediation in progress

Added a native final-feedback assertion. First rerun timed out at63/120 verified
with work still active;150-second observation is justified by measured progress.
Second rerun failed during initial Activity launch, before the assertion, with
instrumentation45-second launch timeout and input-dispatch ANR. Both scoped
cleanup/package removal/environment restoration passed. Local receipts:
import-feedback-native-red.json and import-feedback-extended-native.json under
.local/parity-evidence. Neither is a confirmed failing final-feedback assertion.
The original retained completed screenshot remains the direct visual defect proof.
Applied minimal feedback reset after a successful result; native verification and
ANR diagnosis remain open. No claim of completed import UI acceptance.

## Fixed-source rendered import closure

Commit52b52cd passes all five native import methods, including empty final
feedback. User Stop at3 saved, explicit reopen/resume to120 saved/0 review,123
real events including earlier3. Cleanup/packages/environment restored. Final
completed PNG inspected: no stale stop message. See imports-ui-evidence.json.
The earlier timeout and launch ANR remain historical, not silently relabelled as
failing feedback assertions; this run did not reproduce either. Recovery UI and
final-source regression/P01–P10 review remain pending.

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

## Final-pass wake deadline review candidate

AutomaticWakeReceiver sets an8-second deadline; reconcileCommitted first acquires
the delivery monitor through requests/control.reconcile, before awaitCommitted
checks remaining time. A contended monitor may exceed the advertised wake budget.
Existing scheduling tests exercise enqueue futures, not lock acquisition. This
needs a bounded held-lock regression and disposition before final review closure;
no native broadcast timeout is claimed from inspection alone.

## Final worker observation failure

Provider8/8 passes on daf794a. The worker suite fails at its60-second observation
window in the first prompt catch-up; cleanup/packages pass. Receipt:
final-worker-timeout-evidence.json. Original failure does not retain scheduler
state or live receipt counts. Added nonpersonal timeout state/attempt/count
diagnostics before retry; observation window and product code are unchanged.

### Worker diagnostic disposition

The unchanged60s diagnostic run reports RUNNING, attempt1,61 verified/1 pending/
0 review before cleanup. This is concrete progress near the expected63 events;
retain final-worker-progress-evidence.json. Observation is extended to120s, without
changing exact delivery/idempotency/filter/pause assertions or product code.
No exact prompt-delivery deadline is adopted. Native rerun remains required.

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

## Lifecycle preparation crash — unresolved attribution

First lifecycle pass after974a66d exits during prepareReopenFixtureWithoutScheduledWork
with instrumentation “Process crashed”, before any lifecycle transition. Scoped
cleanup/packages pass; environment restore reports unchanged/verified. See
lifecycle-preparation-crash-evidence.json. Later crash-buffer read shows only old
Bluetooth service crashes, not an app exception; post-uninstall exit-info is empty.
This does not establish cause. Added package exit-info/meminfo to existing
pre-cleanup diagnostics for a bounded fresh-fixture retry; product inputs unchanged.

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

## Medium — reboot failure can skip environment readback (open harness finding)

The failed reboot observation receipt records restoration verified/unchanged even
with ADB offline, because the harness dispatches reboot outside the environment
mutation owner. Later manual recovery verified cleanup/settings, and the current
full native lifecycle pass verifies restoration after Doze, so no unverified
recovery is being promoted to success. Before review closure, put reboot through
EmulatorEnvironment with changed=True set before dispatch; test a reboot command
that changes Awake→Asleep then raises, and require actual restore/readback.
Wait until the live frozen matrix finishes before editing shared harness code.

## Reboot restoration finding resolved

EmulatorEnvironment now owns reboot dispatch and marks changed before calling ADB.
New host regressions require a qualified snapshot and simulate Awake→Asleep followed
by an uncertain reboot exception, then require actual restoration/readback of the
original state. The two new tests first failed on the missing mutation-owner API;
after implementation all73 host tests pass. The earlier native offline/unchanged
receipt remains the integration evidence motivating this correction.
Native matrix remains valid for the unchanged app/test APKs and normal lifecycle
path; this post-matrix correction affects failure restoration only.
