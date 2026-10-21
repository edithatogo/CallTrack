# Review Report: Track 4 parity and durable synchronisation

## Summary

P01–P10 pass for the explicitly selected API36 emulator scope after review fixes; archive is recommended with the physical/cloud/release boundaries in the handoff retained.

## Verification Checks

- [x] **Plan Compliance**: Yes — concrete requirements and all eight unblock steps are reconciled in closure-audit.md and final-acceptance-evidence.json; final handoff is present.
- [x] **Style Compliance**: Pass — general guide, full Java AOSP/Kotlin formatter dry runs under JDK17, whitespace and Conductor checks.
- [x] **New Tests**: Yes — durable/fault/ownership/migration/scheduling/status and rendered UI regressions supplement the preserved contracts.
- [x] **Test Coverage**: Yes within the selected development scope — 110 JVM tests,73 host tests and89 successful native invocations plus one expected insertion-process termination, across10 suites. Native counts include fixture setup/cleanup.
- [x] **Test Results**: Passed — frozen current app/test APKs, all native receipt/input/artifact hashes,40 PNG/tree pairs and post-run package/provider/environment readback verified. Lint has0 errors,32 warnings,5 hints. Hosted CI execution is not claimed.

## Findings and applied fixes

| Finding | Disposition and evidence |
|---|---|
| Recovery/rescan could count an already confirmed receipt again | 2da53cc; transition-count regressions and current worker/lifecycle/provider readback. Last worker-attempt counts are distinct from authoritative all-destination receipt counts. |
| Thrown activation commit could expose uncertain cached authority; scheduling could interleave with newer policy | 72d352c; process-wide commit latch plus shared publication/scheduling lock; native false/thrown faults and JVM ordering tests. |
| Durable delivery accepted a wrong/invalid event identity | aa0802b; require valid exact IDs and fingerprint/calendar agreement; fault-port regressions and native provider checks. |
| Failed namespace commit could be reused from Android's cache | 7d19775; process-wide uncertainty latch; false/thrown/reuse native cases. |
| System bars and keyboard obscured native controls | 1c4c76c and1422b6c; system/cutout/IME insets; native geometry, real touches and retained screenshots. |
| Completed import retained stale stop feedback | 52b52cd; reset on successful result; current rendered resume asserts empty feedback,120 verified/0 review/123 events including earlier3. |
| Leaving/returning revived queued insertion approval | daf794a; per-confirmation token invalidated onPause; native red READY expected/CONFIRMED actual, then native green and fresh-approval acceptance. |
| Wake deadline excluded waiting for the delivery lock | 974a66d; bounded admission and caller deadline, expired/interrupted admission refusal; native held-lock red/green and JVM contracts. |
| Short ADB reconnect gate aborted before intended boot observation | 4cc00f8; offline observations share the180-second loop; full current lifecycle passes. |
| Reboot failure could skip environment restoration readback | 088281b; mutation owner marks reboot before dispatch; uncertain power-state-change regression restores original state. This failure-only harness correction follows the native matrix without changing APK inputs. |

The worker's60-second observation failure is retained: RUNNING/61 verified/1 pending/0 review justified120 seconds; exact effect assertions were preserved. Earlier unattributed process exits/ANR are retained, not relabelled as fixed product defects. The fresh complete matrix passes; pre-cleanup diagnostics now capture exit/memory state if failures recur.

## Scope and decision

Reviewed current reachable Track4 delivery/source/import/persistence, policy/status, scheduling/receiver, native screen callbacks, manifest, relevant tests/harnesses, dependency decision and build/CI wiring against the general guide and concrete requirements. Current mixed Kotlin model contracts are included. Other tracks' optional prototypes and retained legacy reachability removal are not promoted into this acceptance.

No confirmed review finding remains open. Accept the bounded development implementation and archive after documentation/registry validation. Physical calls, remote provider readback, wider Android/OEM/manual accessibility qualification, release identity/signing/store admission and longer operational observation remain explicit later gates. See [handoff](handoff.md).
