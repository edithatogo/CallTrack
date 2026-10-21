# Review Report: Track6 active Android Kotlin migration

## Summary

Active-code conversion is implemented and reviewed; final native acceptance is blocked by startup ANRs/provider availability, with original release/operational gates still open.

## Verification Checks

- [ ] **Plan Compliance**: Partial - approved active-code migration is implemented; final native qualification and original M4/full-M5 gates remain open.
- [x] **Style Compliance**: Pass - pinned ktfmt Kotlin convention check, explicit nullability and deliberate JVM interoperability; lint has zero errors.
- [x] **New Tests**: Yes - pre-conversion expanded Java contracts, private-state replacement safety, retired fallback and evidence-scope/timeout regressions.
- [ ] **Test Coverage**: Partial -115 JVM/89 host tests pass; five native suites (56 invocations), standalone import UI (five) and private update (two) pass; lifecycle and remaining suites are pending. Physical/cloud/production/manual accessibility/operational outcomes are separate.
- [ ] **Test Results**: Partial -115 JVM/89 host tests and the recorded native scopes pass; full lifecycle acceptance failed. See active-migration-evidence.json. Two clean unsigned builds pass115 tests each with identical APKs.

## Findings

### Medium — Removing direct declarations lowered retained transitive versions (fixed)

- **File**: `app/build.gradle` (dependency constraints)
- **Context**: Retiring inherited screens removed unused direct API declarations, but Material/AppCompat still consumed ConstraintLayout/RecyclerView/Transition. Resolution silently selected2.0.1/1.1.0/1.2.0 instead of qualified2.1.3/1.2.1/1.4.1.
- **Suggestion**: Preserve prior selections with compatibility constraints, not restored unused API ownership. Fixed51c43f0. Actual runtime readback confirms all three selections; all68 retained runtime artifacts match the prior release inventory. Reassess constraints when their consumers retire.

### Medium — Debug receipt included a stale release APK (fixed)

- **File**: `scripts/run_slice_build.py` (`debug_artifacts`)
- **Context**: Recursive output collection could include an older release APK although the command built only debug/test. Native acceptance always checks only debug/test APKs, so their binding remains valid. The old extra release entry provides no current release proof.
- **Suggestion**: Restrict collection to debug and debug instrumentation outputs; qualify release separately. Fixedcae25f9 with a stale-release regression. Current release evidence is the two clean unsigned checkouts, not the old root release file.

### Medium — Outer UI timeout prevented native diagnostics (fixed)

- **File**: `scripts/qualify_parity_ui.py` (`command`)
- **Context**: A rendered partial-import stop/reopen/resume test includes setup plus a150-second bounded completion wait. The host180-second cap could terminate it without reporting the app assertion/result. The earlier failed run restored its environment and removed fixtures/packages; no app assertion root cause was established.
- **Suggestion**: Allow300 seconds for whole instrumentation invocations, retain partial stdout/stderr on timeout, keep native assertions/waits unchanged. Fixed51c43f0;10 UI harness safety tests pass, including timeout diagnosis/cleanup. Source-matched import rerun passes, with119 verified candidates and one conservatively reviewed interruption boundary out of120; exact event count is122 including three earlier fixture events.

## Scope and reasoning

Review covers the47-file pre-conversion inventory,33 active conversions and14 inherited removals, including models/identity/presentation, activation/delivery/import/recovery state machines, SQLite stores, native adapters, scheduling and four Views Activities. Historical pilot review remains separate. Full diff exceeds300 lines and was reviewed in component batches under the user's approved implementation/review/fix request.

Preserved contracts include schema6/import2 and JSON names/order, namespaces/signatures, exact arithmetic/ASCII trim/UTF16 rendering, nullable Java fields, stale early returns, transaction boundaries and the global one-writer effect lock. Activation commit uncertainty remains latched; queued recovery approvals are withdrawn after pause; cancellation does not confer replay authority. Work payloads/status logging contain no personal call fields. Manifest removal affects only previously unexported/disabled inherited components; native permissions/application identity remain stable.

The one Java NullableCalendarLabel bridge intentionally preserves nullable legacy Choice.toString behaviour. Kotlin Any.toString cannot express that contract; removal requires an explicit consumer contract change and preserving tests. Package-private helpers widen to public JVM bytecode visibility for unmangled Java callers, with no new exported Android authority. No new framework or scheduling/persistence takeover is introduced.

Lint retains69 warnings and two hints: unused legacy resources/translation candidates, older dependency/wrapper notices, optional KTX, obsolete SDK checks and icon metadata. Applying Unicode trim would change persisted fingerprints; convenience KTX must not discard commit-result uncertainty. Automatic newest-version upgrades are outside this preserving migration. Remaining asset/licence and accessibility qualification stay release gates.

No Critical/High app defect is currently established. Review fixes do not qualify public release, physical calls, remote synchronization, independent trust or operational observation. Track6 stays active until original full acceptance passes; later implementation successors remain pending in the approved iOS-last sequence.

## Open acceptance blocker

Final lifecycle acceptance failed first on a post-reboot calendar query timeout and
missing guest phone service, then on process-startup ANRs after a scoped AVD restart.
No app-code cause is established; the full native matrix is not accepted. The retry
initially preserved packages when instrumentation cleanup failed. Explicit manual
readback subsequently proved all provider rows empty, removed only provisioned
packages, rechecked absence and restored non-root ADB with recorded power/network
state matching. Phone service remains unavailable. No source workaround or relaxed
assertion is justified from these diagnostics. Remaining lifecycle/telephony/settings/
recovery qualification and original M4/full-M5 gates prevent closure or archival.
See active-migration-handoff.md for the exact resume sequence and evidence limits.
