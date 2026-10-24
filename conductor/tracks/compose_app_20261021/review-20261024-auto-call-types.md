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
- [x] **Test coverage**: Partial — panel and Activity recreation checks are defined; local rendered execution remains dependent on an available emulator.
- [ ] **Test results**: Initial hosted slice build exposed stale Activity reads of the former CheckBox fields; those references are now replaced with the saved Compose state. A fresh workflow run is pending.

## Findings

The first hosted compile found six unresolved references to the removed CheckBox fields in the asynchronous settings load. They were replaced by a settings-to-Compose-state update, guarded so saved unsaved selections survive Activity recreation. Permission requests, provider reads, activation confirmation and scheduling remain Activity-owned.

The selected tests do not establish permission revocation, full Activity process death, cloud/calendar delivery, or full Track 14 completion.
