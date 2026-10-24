# Review Report: Track 14 automatic call-type controls

**Date:** 2026-10-24
**Scope:** Replace three automatic logging call-type checkboxes with a lifecycle-disposed Compose panel.
**Disposition:** Approved for this UI slice; broader Track 14 acceptance remains open.

## Summary

The Compose panel preserves explicit user choices and accessibility selectors while leaving calendar selection, permissions, activation confirmation, and scheduling in the Activity.

## Verification checks

- [x] **Plan compliance**: Partial — the call-type control group is migrated; remaining Views and end-to-end acceptance stay in the original Track 14 plan.
- [x] **Style compliance**: Pass — uses the existing Foundation-only panel pattern and existing resource IDs as Compose test tags.
- [x] **New tests**: Yes — state/callback, semantic role, 48 dp touch target, large-text RTL, disabled-state, Activity host/initial-state, and selection-preserving Activity recreation regressions.
- [x] **Test coverage**: Partial — hosted qualification builds compile and package the new Android instrumentation tests but do not run them; local rendered execution remains dependent on an available emulator.
- [x] **Test results**: After fixing the stale Activity state references and Compose test-rule import, both hosted `slice` jobs and both platform qualification matrices passed for commit `c163c53`. Kotlin/Java formatting, the Python 3.15.1 verifier and 19 targeted Conductor tests passed locally. The Android instrumentation tests compiled and packaged but were not executed.

## Findings

Review fixes: the first hosted compile found six unresolved references to removed CheckBox fields in the asynchronous settings load; the next run found the Compose test-rule import was from the wrong package. Both fixes are committed. The settings-to-Compose-state update is guarded so saved unsaved selections survive Activity recreation. Permission requests, provider reads, activation confirmation and scheduling remain Activity-owned.

The selected tests do not establish permission revocation, full Activity process death, cloud/calendar delivery, or full Track 14 completion.
