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

## Follow-up review: API26 local and hosted acceptance — 2026-10-25

The scoped Gboard change is safe: it denies the contacts prompt only when the
launcher is exactly Gboard, and readback confirms `READ_CONTACTS` remains
denied. Both app and test APK manifests omit contacts permission. The keyboard
journey retains its real Tab traversal assertion and injects the key through
Android's input dispatcher.

One local source-bound run passed settings, imports, recovery and accessibility
on API26 with cleanup, package removal and emulator restoration. A later
accessibility-only rerun against the same test APK failed at the same
keyboard-focus assertion. The local result is therefore not repeatable.

Hosted run [40140921320](https://github.com/edithatogo/CallTrack/actions/runs/40140921320)
on `81779f9` passed API36. API26 passed settings/import/recovery but failed
keyboard-focus traversal after device-level Tab injection. API33 failed to
locate the permission control with the permission root unavailable and app
permissions not fully granted. Build-qualification runs 40140916374 and
40140921324 passed. Focused Python safety tests (35), the 250-document
Conductor verifier, pinned Java formatting and `git diff --check` pass.

**Finding status: unresolved.** Do not merge PR #78 while the hosted matrix is
red. Next work should diagnose why API26 focus is non-repeatable across local
and hosted emulators and why API33's native permission controls are inaccessible
before weakening either acceptance assertion. Physical calls, manual
TalkBack/Switch Access, provider, process/lifecycle and operational acceptance
remain separate open gates.

Hosted rerun [40142395588](https://github.com/edithatogo/CallTrack/actions/runs/40142395588)
on `c4c8be9` passes API33 and API36. API26 still fails keyboard-focus
traversal after device-level Tab input. Both build-qualification runs
40142392059 and 40142395580 pass; API26 verifies fixture cleanup and package
removal. The API33 failure is resolved for this run. Keep PR #78 open for the
repeatable API26 focus issue; do not merge while that check is red.

## Hosted matrix rerun `4d512bf` — 2026-10-25

Hosted UI run [40143215905](https://github.com/edithatogo/CallTrack/actions/runs/40143215905)
passed API36. API26 again passed settings, imports and recovery, then failed
`renderedAccessibilityLabelsTargetsAndKeyboardFocus` at the visible app-owned
keyboard-focus assertion; fixture cleanup and package removal passed. API33
failed `renderedPermissionsFiltersConfirmPauseAndReopen`: the next
`permission_allow_button` could not be found, with `permission-root=unavailable`
and calendar permissions still denied. This repeats the earlier inaccessible
permission-controller symptom after one successful API33 run. Build
qualification runs 40143215932 and 40143212903 passed. No app permission
requests or acceptance assertions were changed. Treat both API26 focus and
API33 permission-controller interaction as unresolved and keep PR #78 open.
