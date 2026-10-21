# Track 4 remaining acceptance ledger

## Final authoritative checkpoint — 2026-10-21

The selected emulator implementation/acceptance and review pass. See
[final acceptance](final-acceptance-evidence.json), [closure audit](closure-audit.md),
[final review](final-review.md) and [handoff](handoff.md). All confirmed findings,
including reboot failure restoration088281b, are fixed. Historical failures and
previous pending checkpoints below are retained; they do not override this final
scoped result. Physical/cloud/release and wider-platform gates remain pending in
the handoff. Archive follows final link/registry validation.

## Preserved chronological record

This is the closure checklist after local review. Earlier plan checkboxes describe
historical slices and must be reconciled against current-source evidence before
marking any phase complete. No item below waives P01–P10 or broadens emulator proof
to physical calls, cloud delivery or release.

| Requirement | Existing evidence to retain | Current closure work |
|---|---|---|
| P01 activation, filtering, minimal permissions | worker-provider and activation-native evidence | Compile/run new false/thrown activation fault tests; render enable/pause/filter/permission flows and verify committed settings/effects. |
| P02 bounded catch-up and source identity | paging, scan, rollover, pending and worker-provider evidence | Rerun applicable current-source worker/store regression; retain multi-page and changed/reset-anchor refusal proof. |
| P03 raw presentation and exact time/duration | source-facts and real-provider evidence | Current full JVM/native regression, including null/withheld/extended types and zero-talk-time real event metadata. No new rendering policy is adopted. |
| P04 committed intent and operation ownership | ownership, provider and lifecycle evidence | Rerun source-bound store/provider/crash boundary cases after local fixes; native counter must show one newly confirmed receipt across recovery/rescan. |
| P05 unique effects, preserve edits/deletions | provider, lifecycle and durable JVM evidence | New wrong/invalid event-ID refusal contracts pass locally; run current APK provider/recovery suite and retain no-recreation evidence. |
| P06 preserving migration, destination isolation | schema1–6 and reapproval evidence | Run current-source preserving migration suite and verify old route effects remain untouched by route changes. |
| P07 historical preview/import/cancel/resume | import, import-native and provider evidence | Render positive preview/confirm/progress/cancel/resume; verify frozen bounds, persisted outcomes and event counts, including re-entry and repeated import. |
| P08 scheduler and recovery | worker-provider and lifecycle evidence | Protected telephony call-end wake, current-source WorkManager ordering/lifecycle regression; keep latency as observed timings. |
| P09 visible status and retention | status-commit, retention, recovery evidence | Run thrown-status retry test; render pending/saved/review/failure states and retention/recovery explanations with authoritative ledger counts. |
| P10 evidence/review/handoff | local review and all named receipts | Full current-source build/lint/APKs, native fixtures/cleanup, remaining UI/call-end acceptance, final complete diff/requirements review, fixes, synchronized handoff and archive. |

## Protected call-end acceptance protocol

Use only the dedicated AVD and synthetic non-emergency fixture identity. Establish
initial provider/permission/work state and a token-owned LOCAL destination. Use
emulator telephony controls to generate a call and its actual terminal state; an
ordinary forged PHONE_STATE intent or a directly enqueued worker is insufficient.
Observe actual source row, protected receiver dispatch where available, WorkManager
execution/status, unique event and durable receipt. Record source/request/verified
observation separately. Verify another catch-up run creates no duplicate. Do not
claim incoming/outgoing/missed telephony parity beyond the call types actually
supported and observed in the emulator. Remove only exact fixture rows/effects and
restore prior emulator state. Never use the attached physical phone as a fallback.

## Positive rendered acceptance protocol

Use a source-bound APK and synthetic local provider fixtures. Capture rendered
controls plus native state before/after actions; programmatically constructing
controllers alone is insufficient. Cover destination selection, independent call
filters, explicit enable and pause, denied/revoked/restored permissions, import
preview cancellation and confirmation, partial import resume, changed/missing event
review, read-only reconciliation, explicit READY insertion approval and withdrawal
on leaving, suppression and payload-retention explanation. Verify that permission
grants alone do not insert/approve, and cancellation/navigation does not authorise
new work. Record exact expected receipt/event counts and cleanup after each flow.
System dialogs/overlays are acceptance blockers until the real interaction succeeds.

## Resume order

Revalidate storage and live guest before changing either. Refresh the full build
receipt first; fallback JVM receipts cannot authorise installs. Execute the expanded
store harness, relevant provider/worker/lifecycle regression, then the positive
telephony/UI cases. Reconcile this ledger and plan tasks with actual results; report
remaining failures explicitly and retain the track active until final closure.

## Latest build/guest status

The reconnected full Android build passes108 JVM tests and both APKs compile; lint
has0 errors/32 warnings/5 hints. See reconnected-build-evidence.json. The old emulator
process is non-responsive and survives TERM/KILL in UEs state. Do not launch another
instance against its AVD while it remains live. Native/UI gates above are unchanged;
new instrumentation is compiled but unexecuted. A Mac restart may be needed before
the dedicated guest can be qualified again.

## Recovery contingency now qualified

The independently named recovery AVD on emulator-5582 boots and responds without a
host restart. All39 store/fault checks pass and both packages are removed. See
recovery-store-evidence.json. The old guest is still stuck; do not touch its writable
files or reuse its ports. Steps1–3 now have this bounded evidence. Broader current-
source runtime acceptance and the source-identity review candidate remain pending.

## Latest delivery/lifecycle status

On the source-identity-fixed APK, provider8/8, worker5/5 and lifecycle11 plus expected
held-insertion termination pass, including verified environment restoration and
fixture/package cleanup. Native source-identity faults3/3 also pass. The earlier
store39/39 receipt is version-bound separately. Main next work: protected call-end
and positive rendered flows, followed by final-source regression/review and closure.
The no-host-restart recovery contingency is operational; do not treat the original
stuck guest as responsive.

## Protected telephony gate now has bounded proof

One emulator-generated incoming missed call progresses through actual Android
RINGING→IDLE, system-delivered PHONE_STATE to the app receiver, background work,
real call row/event and unique receipt before verifier startup. Five native checks,
repeat catch-up and exact cleanup pass; see telephony-evidence.json. Outgoing and
answered telephony remain unclaimed. Next: positive rendered UI journeys and final
regression/review; emulator proof does not replace pending physical/cloud gates.

## Rendered settings checkpoint

The settings permission/filter/confirmation/pause/reopen/revocation/restoration
journey passes through actual touches, with19 retained screenshots and state
readback. System-bar geometry passes on all four screens after fixing native
edge-to-edge overlap. See settings-ui-evidence.json. This is not positive import
or recovery interaction acceptance; those journeys and final regression/review
remain open.

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
