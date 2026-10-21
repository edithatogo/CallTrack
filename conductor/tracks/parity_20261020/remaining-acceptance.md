# Track 4 remaining acceptance ledger

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
