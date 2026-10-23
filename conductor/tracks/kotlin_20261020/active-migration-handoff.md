# Active Android Kotlin migration handoff — 2026-10-21

## Implemented and reviewed

All33 active Java classes are converted, with36 production Kotlin files including
the original three pilot models. One deliberately retained Java bridge preserves
nullable Choice.toString ABI. Fourteen unreachable inherited Java files,12 obsolete
resources and seven unused direct dependency declarations are removed. Three
still-consumed Material/AppCompat transitives retain qualified versions through
compatibility constraints. All68 retained runtime artifacts match prior inventory
hashes; this is continuity evidence, not trusted licence/provenance clearance.

Schema6/import2, identities, signatures, namespaces, operation ownership, receipts,
SQLite transactions, Views, one-writer locking and scheduling remain. The retained
javac fallback is retired. Kotlin conversion does not introduce Compose, KMP,
macOS, Rust, iOS or a new synchronization engine.

Implementation source:51c43f0 (component commits e7126a2–b30540d). Evidence/tooling
fixes include b93480d and cae25f9. See [review](review-active-migration-20261021.md),
[acceptance evidence](active-migration-evidence.json), [inventory](migration-inventory-20261021.json),
[removal audit](legacy-removal-audit.json) and [runtime inventory](runtime-artifacts-20261021.json).

## Qualified scope

- 115 JVM and89 host tests pass; debug/test packaging and lint pass.
- Two isolated clean unsigned release builds at787ae35 pass115 JVM tests each,
 zero lint errors (69 warnings) and produce identical APKs. Both use the same host
 and cache; independent environment/signing remain unqualified.
- Five source-matched native suites pass56 invocations: store39, source identity3,
 wake budget1, provider8 and worker5.
- Standalone rendered import UI passes five invocations, including confirmation,
 cancellation, duplicate prevention and stop/reopen/resume. The120-candidate resumed
 fixture ends119 verified/one conservative review, with122 exact provider events
 including three earlier fixture events.
- Same-version debug-key replacement passes two private-state instrumentation tests;
 paused activation, namespace and receipt states survive. This is not a production
 increasing-version/provider/cloud upgrade claim.

These are separate scopes; they are not a full final matrix pass. Earlier
pre-constraint runs and Track4 historical acceptance do not substitute for current
missing suites. The debug collector fix was run against real unchanged app inputs;
both debug/test APK hashes remain identical and stale root release output is excluded.

## Native blocker and recovery

The first final lifecycle run verified the intended process termination and actual
reboot, then a calendar query timed out. Restoration found the guest phone service
missing. Its fixture cleanup/package removal passed; a later explicit environment
restoration retry passed. After a data-preserving, no-snapshot restart of only the
recovery AVD, readiness checks passed, but fixture setup and cleanup reported
Process crashed. Exit-info records startup ANRs with state=empty, alongside guest
Bluetooth/radio crash logs. No app-code root cause is established. Referenced ANR
trace files were unavailable after package cleanup; they are not claimed captured.

The failed runs remain failed. On retry, packages were initially preserved as
required by the cleanup guard. Direct native readback then proved calendars, events
and call rows entirely empty before/after removal of only the two provisioned
packages. Final readback confirms no fixture packages/rows, matching recorded
power/network baseline and non-root ADB. Phone service is still unavailable; this
is not a healthy acceptance environment. Raw recovery/diagnostic hashes are in the
evidence file. No physical phone or computer restart was used.

## Resume sequence

1. Restore a healthy isolated API36 guest without changing the computer's boot
   state. Capture startup/ANR diagnostics before package removal if failure repeats;
   compare a fresh guest or an internal-storage AVD if needed. Do not infer an app
   defect or waive it from unrelated system crashes.
2. Rerun full lifecycle, telephony, rendered settings and rendered recovery on the
   exact current APKs, with provider readback and cleanup. Existing passed receipts
   can be reused only while all inputs/APKs match. Any app change requires rebuilding
   and requalifying affected/native matrix scope.
3. Close remaining Track5 release gates: permanent identity/key custody (currently
   intentionally pending), trusted dependency/licence/assets, independent/hosted
   reproduction, supported production upgrades/recovery and seven actual observation
   days. Physical/cloud/OEM/manual accessibility outcomes remain separate.
4. Close original full M5 only after its M4/operational gates. Then proceed through
   successor Tracks14–20 in the [approved sequence](../../future-architecture.md).
   No deferrals have been invented to skip prerequisites; iOS remains last.

Track6 remains in progress and is not archived. Later frameworks are planned
successor implementation tracks, not implemented capabilities.
