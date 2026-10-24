# Review: Hosted API26/API33 UI matrix repair

## Summary

The local review found two platform-assumption defects in the rendered test
harness. The patch fixes the selectors and records a bounded test-only permission
restoration route. Hosted API26/API33 acceptance remains pending a fresh matrix
run; API36 passed on the pre-fix source.

## Findings and fixes

### API26 import confirmation action

Run [40101679905](https://github.com/edithatogo/CallTrack/actions/runs/40101679905)
reached the real import confirmation dialog, but the test could not find the
standard `button1` resource ID. The test now taps the exact localized,
enabled clickable confirmation label and still requires the import session to
complete with the selected calendar and expected saved count. Touch injection
continues to reject controls owned by an unexpected package.

### API33 permission denial control

The API33 run reported only
`permission_deny_and_dont_ask_again_button`. Android's PermissionController
source defines that control as a permanent denial action, separate from the
ordinary denial button ([layout](https://android.googlesource.com/platform/packages/modules/Permission/%2B/774cf953ac2fac2b1f18226b5682121a76a97299/PermissionController/res/layout/grant_permissions_material3.xml),
[strings](https://android.googlesource.com/platform/packages/modules/Permission/%2B/5f7a5ab82383bff23dba080ed8ed7fc52b7c2242/PermissionController/res/values/strings.xml)).
The selector now recognizes both known denial IDs. When the platform exposes
only the permanent-denial action, the test verifies the visible denial and
that logging remains inactive, then uses `UiAutomation.grantRuntimePermission`
on the disposable emulator to continue the independent settings/pause journey.
This is not evidence of user-driven API33 regrant; the API36 run continues to
exercise the rendered re-request-and-grant route.

## Verification

- Source commit: `ac42f2a` (`test(track14): handle platform dialog controls`).
- JDK17.0.21.0.2 / qualified SDK36 source-bound build passed using the repository
  `.venv` CPython3.15.1. `:app:testDebugUnitTest`, app and instrumentation APK
  assembly, and `:app:lintDebug` all completed; the input receipt remained
  unchanged. Instrumentation APK SHA-256:
  `f3a471049c8c935abe43ca59ac7ceea4393504908a1ab3203c12751ca6992dd8`.
- `tests.test_parity_ui_safety`: 21 tests passed.
- `scripts/verify_conductor.py`: 248 documents, 21 tracks and 25 synthetic
  scenarios passed.
- Pinned google-java-format1.24.0, `actionlint`, and `git diff --check` passed.
- Hosted run 40101679905, before this fix: API36 passed; API26 and API33 failed
  at the selectors described above. The full matrix must pass on the new source
  before the hosted acceptance task can close.

## Remaining limits

### Follow-up matrix results

Subsequent hosted runs 40103198399, 40104130410, 40105121398, 40106212006,
40107406647 and 40108385701 show API33 and API36 passing on the latest
source, while API26 continues to fail in the import journey. The final run
40108385701 failed while `ImportUiTest.renderedRangePreviewCancelConfirmAndRepeat`
was waiting for the app-owned confirmation action. Safe diagnostics showed an
active PermissionController dialog categorized as `contacts`; the app's
calendar and call-log permissions were already granted, and phone-state was
denied. The app manifest does not request `READ_CONTACTS`. The harness did not
grant Contacts or inject input into the system dialog. The result therefore
does not establish whether this is a platform/test-environment artifact or an
app flow defect. API26 remains an open acceptance blocker and requires an
identified, reproducible permission sequence before another behavior change is
made.

No API26/API33 result is inferred from the local build or API36. Physical-device,
manual TalkBack/Switch Access, provider-account, remote-calendar and operational
acceptance remain separate gates.

## Diagnostic rerun

Run [40116441898](https://github.com/edithatogo/CallTrack/actions/runs/40116441898)
on commit `0abcd8787b0388f003f1a6d3b86e1a3e120d346a` passed API36 but failed
API33 and API26. API33 failed in
`renderedPermissionsFiltersConfirmPauseAndReopen` because the expected
`permission_allow_button` was absent. API26 again reached
`ImportUiTest.renderedRangePreviewCancelConfirmAndRepeat` with a PermissionController
dialog classified as `contacts`, with calendar and call-log grants present and
phone-state denied. The run did not include the allowlisted declared-permission
field in its public failure summary, so it still cannot attribute the Contacts
request to either installed package. The source-bound build qualification on
the same commit passed as run `40116441902`. Keep permission behavior unchanged
until the diagnostic is made observable or another bounded evidence source
identifies the requesting package; API26/API33 acceptance remain open.

The next diagnostic revision reads declared permissions from the already
validated app and instrumentation APKs using `aapt`, and retains only
`READ_CONTACTS`, `READ_CALL_LOG`, `READ_PHONE_STATE`, `READ_CALENDAR` and
`WRITE_CALENDAR` in failure summaries. This is independent of system-dialog
labels and instrumentation exception formatting. Focused Python safety tests
and local APK manifest readback pass; hosted API26/API33 rerun remains required.

Run [40117601783](https://github.com/edithatogo/CallTrack/actions/runs/40117601783)
on `5fdff4d` passed API36 and failed API26/API33. The API26 diagnostic now
reads permissions from the actual validated APKs: the app declares
`READ_CALENDAR`, `READ_CALL_LOG`, `READ_PHONE_STATE` and `WRITE_CALENDAR`; the
instrumentation APK declares none of the allowlisted permissions. The active
dialog text classifier still says `contacts`, so neither APK manifest
declares `READ_CONTACTS`, and the origin of the permission UI remains unknown.
API33 again failed to locate the deny control. The next test-only fix searches
all visible accessibility windows for known PermissionController-package
controls and retains package ownership checks before touch. Its hosted matrix
is pending. A local Gradle compile could not start because the host has JDK11;
Android Gradle Plugin requires JDK17. Direct Java 8-compatibility compilation
of the changed helper against API36 `android.jar` and cached JUnit, the pinned
Java formatter, and the Python safety suite passed.

## Latest bounded diagnostic

The follow-up diagnostic commit `b5c2a90` adds a sanitized app-label visibility
state and aggregates only known PermissionController roots and allowlisted
permission control IDs across visible accessibility windows. The exact-source
build qualification passed in runs `40121249281` and `40121253294`; the PR
qualification also passed `slice`, Compose prototype and multiplatform UI jobs.
On hosted UI run [40121253324](https://github.com/edithatogo/CallTrack/actions/runs/40121253324),
API26 again failed in `renderedRangePreviewCancelConfirmAndRepeat`. Its bounded
summary reports a PermissionController prompt classified as `contacts`, with
app-label visibility `not-visible`, calendar and call-log grants present,
phone-state denied, and no `READ_CONTACTS` declaration in either validated
APK. It lists only known permission control IDs; no call-log data or dialog
text is retained. This still does not identify the component that initiated
the prompt, so no permission or product-flow change is justified.

The matrix completed with API33 and API36 passing and API26 failing, so the
matrix as a whole is not accepted. API26's unresolved permission prompt remains
the blocker. Keep the PR open and do not merge. The two passing platform results
are valid for this source revision; they do not replace API26 acceptance.
