# Active Android Kotlin migration handoff — 2026-10-21

The emulator blocker is resolved for the selected acceptance path. All ten suites
now have matching-source/APK acceptance. Original full M4/M5 release and operational
gates remain open; Track6 stays in progress and is not archived.

## Implemented and reviewed

All33 active Java classes are converted, with36 production Kotlin files including
the original pilot models. One Java bridge preserves nullable Choice.toString ABI.
Fourteen unreachable inherited Java files,12 obsolete resources and seven unused
direct dependency declarations are removed. Three still-consumed transitives retain
qualified versions through constraints. All68 retained runtime artifacts match the
prior inventory; this is continuity evidence, not licence/trust clearance.

Schema6/import2, identities, signatures, namespaces, receipts, operation ownership,
SQLite transactions, Views, one-writer locking and scheduling remain. The retained
javac fallback is retired. No production Compose/KMP/macOS/Rust/iOS is introduced.
App source remains51c43f0; emulator preflight fix826ccaf changes tooling only.

## Qualified scope

- 115 JVM tests pass at unchanged app inputs;92 host tests pass after the repair.
- The consolidated ten-suite native matrix passes89 successful invocations plus
  one expected process termination. Lifecycle12 (11 successful plus the expected
  kill), telephony5, settings UI5 and recovery UI7 were executed after repair.
  Earlier exact-build store39/source-identity3/wake-budget1/provider8/worker5 and
  import UI5 receipts were reused explicitly:61 invocations. This is not a claim
  that every suite was freshly executed on the new guest.
- The native lifecycle suite verifies process death, actual guest reboot, offline
  and Doze recovery; telephony uses an emulator call and protected system wake.
  Rendered UI checks include permissions, cancellation, explicit approval, human
  edits/deletions and retention. Eighty screenshot/text artifacts are hashed;
  settings and recovery previews were visually reviewed.
- Two clean unsigned release builds at787ae35 pass115 JVM tests each,zero lint
  errors (69 warnings), and produce identical APKs on this host/cache. They are
  not independent-environment or production-signing evidence.
- Same-version debug-key replacement passes two separate private-state tests for
  paused activation, namespace and receipt states. This is not a production
  increasing-version/provider/cloud upgrade claim.

See [current evidence](active-migration-evidence.json), [repair/runbook](emulator-recovery-20261021.md)
and [current review](review-emulator-recovery-20261021.md). The [initial partial checkpoint](partial-migration-evidence-20261021.json)
and [initial review](review-active-migration-20261021.md) preserve failed-run history.

## Recovery and final readback

Resource pressure, duplicate idle chat-owned helpers, an orphaned simulator and
unhealthy guest services were observed. Scoped helper retirement and a fresh
API36 Google APIs ARM64 Pixel7 AVD on PortableSSD supplied a working path without
restarting the computer. Original AVD data is preserved, not claimed repaired.
The nondefault simulator experiment failed and is not the qualified launch recipe.
A single root cause or permanent desktop-helper lifecycle fix is not claimed.

The harness now rejects boot=1/phone-service-missing before fixture installation.
Three meaningful regressions fail before the fix and pass afterward; native time
windows/assertions remain unchanged. Exact emulator identity checks still precede
this read-only guard. No physical phone was used.

Final readback verifies no fixture packages, calendar/event/call rows, matching
recorded power/network state, non-root ADB and an available phone service. No
failed receipt is relabelled as passed. If resource pressure recurs, inspect current
ownership and readiness; never reuse recorded process IDs or blindly remove data.

## Next gates

1. Complete remaining Track5 release qualification: permanent identity/key custody
   (intentionally pending), trusted dependency/licence/assets, independent/hosted
   reproduction, supported production upgrade/recovery and seven actual observation
   days. Physical/cloud/OEM/manual accessibility outcomes remain separate.
2. Close original full M5 only after its M4/operational gates. Code migration and
   selected emulator acceptance are complete; this does not waive original gates.
3. Continue successor Tracks14–20 in the [approved sequence](../../future-architecture.md).
   No prerequisite deferrals were invented; iOS remains last.


## Minimum-platform follow-up — 2026-10-22

Track5's [extended emulator matrix](../release_20261020/emulator-matrix-qualification-20261022.md)
found and fixed an API26 Kotlin use/SQLiteOpenHelper interface mismatch at6f3381c.
Both helpers explicitly implement Closeable; schemas, identities, receipts and
existing Views remain. Selected API26/API33 native worker/UI/lifecycle/upgrade
cases pass alongside115 JVM fixtures. Historical API36 receipts retain their
original source binding. The current application fixes require release refreezing
and independent reproduction; original M4-linked/full-M5 gates remain open.
