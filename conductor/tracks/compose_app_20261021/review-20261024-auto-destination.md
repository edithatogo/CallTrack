# Review Report: Track 14 automatic destination selector

**Date:** 2026-10-24
**Scope:** Replace the automatic settings calendar Spinner with a Compose choice panel.
**Disposition:** Approved for this UI slice; provider-backed runtime and full Track 14 acceptance remain open.

## Summary

The Activity still owns calendar discovery, permission checks, current provider IDs, and activation effects; Compose displays labels and returns an opaque per-load selection token.

## Verification checks

- [x] **Plan compliance**: Yes for the selector slice. The existing `auto_calendar_picker` accessibility selector is retained, and choice tokens map back to the latest Activity-owned provider result.
- [x] **Style compliance**: Pass — uses the existing Foundation-only action button and lifecycle-disposed ComposeView patterns.
- [x] **New tests**: Yes — explicit token selection and Activity host/placeholder semantics; the existing provider-backed UI journey continues to exercise selecting a calendar before activation.
- [x] **Test coverage**: Partial — provider UI journey and new Compose test are defined; this slice alone does not qualify permission revocation, provider churn, or activation persistence.
- [ ] **Test results**: Pending hosted Android instrumentation compilation and workflow checks.

## Findings

No defect found in static review. Stale or superseded option tokens are ignored when they are not present in the current loaded choice list. The screen still cannot select a calendar until the Activity has permission and a provider choice.

Native rendered execution remains pending when no Android emulator is available locally.
