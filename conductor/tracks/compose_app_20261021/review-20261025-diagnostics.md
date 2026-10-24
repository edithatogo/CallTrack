# Review Report: Track 14 hosted UI diagnostics

## Summary

The bounded host diagnostics pass local review and focused tests, but the
minimum/intermediate Android UI matrix remains unaccepted because API26 still
exits during rendered import and API33 cannot consistently expose its
permission-controller controls.

## Verification Checks

- [x] **Plan Compliance**: Partial — only sanitized host-side failure evidence
  was changed; app permissions and provider behavior were not changed.
- [x] **Style Compliance**: Pass — follows the existing Python helper and test
  conventions; no new dependency was added.
- [x] **New Tests**: Yes — activity-owner classification and prefixed Android
  fatal-log parsing have privacy assertions.
- [x] **Test Coverage**: Partial — covers accepted diagnostic formats and
  redaction, while hosted failure evidence is still needed to identify the
  API26 exception source.
- [x] **Test Results**: Passed — 35 `tests.test_parity_ui_safety` tests,
  `scripts/verify_conductor.py` (249 documents, 21 tracks, 25 synthetic
  scenarios), and `git diff --check` passed. Hosted build qualification run
  40131471215 passed all five jobs. Hosted UI run 40131471355 passed API36;
  API26 and API33 failed as described below.

## Findings

### Medium: Hosted permission and import UI acceptance remains unresolved

- **File**: `scripts/qualify_parity_ui.py`
- **Context**: Hosted run
  [40131471355](https://github.com/edithatogo/CallTrack/actions/runs/40131471355)
  on `0a140e7` reports API26's rendered import journey ending without an
  instrumentation result marker. The safe summary now identifies the resumed
  owner as `permission-controller` and the permission-activity launcher as
  `com.google.android.inputmethod.latin`; the exception is `java.net.SocketException`,
  with no process or stack owner retained. API33 again fails to find the
  permission denial control while the app window is active and the permission
  root is unavailable. API36 passed its complete rendered matrix.
- **Suggestion**: Keep the matrix open. Use the latest bounded activity and
  permission-owner evidence to investigate the emulator/test interaction; do
  not change app permission requests or grant the keyboard access based on this
  evidence.

The API26 cause remains unassigned. The keyboard launcher suggests an external
permission flow but does not prove which permission was requested or why the
instrumentation channel closed. No product behavior change is justified.

## Follow-up review: local API26 acceptance — 2026-10-25

The isolated API26 rerun resolved the local harness finding. The four
source-bound journeys (settings, import, recovery and accessibility) now pass
on Android 8.0 / SDK26. Each receipt verifies fixture cleanup and package
removal. APK manifest readback confirms the app and test APK do not request
contacts permission. The local import harness handles only the observed
Gboard-owned contacts prompt, and only with an explicit deny action. The
keyboard test focuses the visible Compose host and enabled app anchor before a
real Tab event, then requires visible app-owned input focus; the traversal
assertion remains enabled.

The final current-build validation passed `tests.test_parity_ui_safety` (35
tests), the 250-document Conductor verifier, pinned Java formatting and
`git diff --check`. No app permission request or runtime behavior changed.
**Finding status: locally resolved; hosted verification pending.** Rerun the
hosted API26/API33/API36 matrix on the pushed revision and retain API33 as an
open issue until that exact run passes. The physical phone, manual
TalkBack/Switch Access, provider, process/lifecycle and operational acceptance
gates remain separate and unqualified.
