# Review Report: Track 4 local fault and scheduling checkpoint

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
