# Track 14 implementation checkpoint — 2026-10-23

## Historical import date and call-type filters — 2026-10-24

Historical import's date range and incoming/outgoing/missed controls now render
in a lifecycle-disposed `ComposeView` hosted at the new stable
`import_filters` ID. Existing `import_from`, `import_to`, and call-type tags,
default values and `Activity` saved-state keys remain. The Activity remains the
owner of parsing, destination selection, permission checks, session creation,
preview/confirmation and delivery. Rendering and filter edits do not create
import sessions or write calendar events.

The new Activity Compose instrumentation test covers initial controls,
explicit date input and call-type toggle, Activity recreation, saved filter
state, and disabled import actions/no session creation. The component test
separately checks that only explicit input emits state changes. The former
accessibility-tree helper failed to synthesize the checkbox tap reliably on
this emulator; the final acceptance uses Compose's instrumented semantics and
pointer input at the Activity level, which passed. The legacy helper was
restored unchanged apart from retaining its existing keyboard-dismiss helper.

With the selected JDK17 and SDK36/API36 emulator, the focused command
`bash ./gradlew :app:testDebugUnitTest :app:lintDebug
:app:compileDebugAndroidTestKotlin :app:connectedDebugAndroidTest
-Pandroid.testInstrumentationRunnerArguments.class=com.markobl.calllogsync.slice.AutomaticSettingsTest,com.markobl.calllogsync.slice.HistoricalImportActivityComposeTest,com.markobl.calllogsync.slice.HistoricalImportFiltersPanelTest`
passed. All five selected instrumentation tests passed; unit tests, lint and
Android-test Kotlin compilation also completed successfully. Pinned ktfmt0.59
formatting, the 19-test Conductor suite, `scripts/verify_conductor.py` (218
documents, 21 tracks and 25 synthetic scenarios), and `git diff --check` passed. The exact
scope, review findings and remaining acceptance limits are recorded in
[the import filters review](review-20261024-import-filters.md).

## Historical import accessibility, large font and RTL — 2026-10-24

The Compose filter test now verifies the date fields' accessible content
descriptions, checkbox role and on state, and a minimum 48 dp row height under
2x font scale and RTL layout direction. It also asserts the date fields and
call-type control remain displayed. A first test attempt incorrectly called a
Compose synchronization assertion from inside `runOnIdle`; moving the semantics
read to the test thread fixed the test-harness misuse. The selected test then
passed both filter component cases on the API36 emulator. This is Compose
semantics and layout evidence, not a live TalkBack or Switch Access session.
See [the accessibility review](review-20261024-import-accessibility.md).

## Recovery empty-state Compose slice — 2026-10-24

The recovery empty-state `TextView` was replaced with a lifecycle-disposed
`ComposeView`, retaining the `recovery_empty` resource ID. The Activity still
sets the visible state only after its bounded ledger page returns; the Compose
panel only renders the supplied localized message. The Activity test verifies
the host type and waits for the localized message after the ledger query; the
panel test verifies hidden and visible states without callbacks. The selected
JDK17/SDK36 run of
`:app:testDebugUnitTest :app:lintDebug :app:compileDebugAndroidTestKotlin
:app:connectedDebugAndroidTest` passed all eight selected API36 instrumentation
tests. This completes the remaining native presentation inside the recovery
list; recovery intro and permission prompts stay Activity/native-owned. See
[the recovery empty-state review](review-20261024-recovery-empty.md).

## Historical import session history Compose slice — 2026-10-24

The history heading, at most ten session buttons and localized empty message
now render in a lifecycle-disposed `ComposeView` retaining `import_history`.
The Activity still queries `SqliteImportStore`, decides the page/cursor,
formats labels and maps random per-query UI tokens back to session IDs. Stale
tokens are ignored, and the existing `loadSession` path remains the only resume
action. Rendering a row does not read or write provider data, confirm an import
or resume delivery.

The API36 `calltrack-track14-api36` emulator run used the qualified JDK
17.0.21+9 and SDK36. The focused command
`:app:testDebugUnitTest :app:lintDebug :app:compileDebugAndroidTestKotlin
:app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.markobl.calllogsync.slice.HistoricalImportHistoryPanelTest,com.markobl.calllogsync.slice.HistoricalImportHistoryActivityTest,com.markobl.calllogsync.slice.AutomaticSettingsTest`
passed. All six selected instrumentation tests passed, including empty and
populated panel behavior, an actual rendered history-row touch that loads one
synthetic SQLite session, and the Activity empty-host assertion. JVM tests,
lint, Android-test Kotlin/Java compilation, ktfmt0.59, google-java-format1.24.0
AOSP mode and `git diff --check` passed.

The fixture-backed `ImportUiTest` was also invoked without its required
`fixtureToken` and calendar permission setup; it failed before fixture setup
and is not counted as acceptance. The new standalone history Activity test
uses a synthetic database session and no CallLog/calendar permissions. Wider
font/RTL, TalkBack/Switch Access, permission/revocation, process restoration,
other Android versions, full provider journeys and release acceptance remain
open. See [the import history review](review-20261024-import-history.md).

## Change

The automatic settings screen now renders its four read-only state/counts/action
feedback/last-run labels through lifecycle-disposed ComposeViews. The original
Android view IDs are retained. The Activity remains the sole source of formatted
state and owns every provider, permission, scheduler, confirmation, navigation
and asynchronous action. Compose has no app effects. Manual logging, import and
recovery screens remain Views and are not claimed migrated.

## Local verification

Using the selected JDK17.0.21+9 and Android SDK36:

- `bash ./gradlew :app:compileDebugKotlin :app:compileDebugAndroidTestKotlin`
  passed after fixing the first instrumentation-test assertion import.
- `bash ./gradlew :app:testDebugUnitTest :app:assembleDebug :app:assembleDebugAndroidTest`
  passed; existing app JVM tests and both app/test APK packaging completed.
- Final repeat of the five focused tasks passed after formatting the Kotlin
  files with the pinned ktfmt0.59.
- `.local/dependency-refresh-20261023/python/cpython-3.15.1-macos-aarch64-none/bin/python3.15
  -m unittest discover -s tests -p test_gradle_verification_policy.py` passed
  all3 tests, including rejection of renamed variant artifacts without their
  allowlisted module metadata.
- The same repo-local Python3.15.1 runtime ran
  `-m unittest discover -s tests -p test_verify_conductor.py` (12 tests, pass)
  and `scripts/verify_conductor.py` (180 documents,21 tracks,25 scenarios,
  pass). Review corrected the scoped-deferral sequence gate: Track14 may proceed
  locally with its linked record while Track15 remains blocked.
- The focused rendered test `AutomaticStatusPanelTest` passed on the API36
  `calltrack-parity-api36-recovery-20261021` emulator (1 test). It verifies the
  supplied text, status tag and polite live-region semantics.
- Exact selectors:
  `bash ./gradlew :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.markobl.calllogsync.slice.AutomaticStatusPanelTest`
  passed1 test, and the same command with
  `class=com.markobl.calllogsync.slice.AutomaticSettingsTest` passed3 tests.
- After the Activity lifecycle fix,
  `bash ./gradlew :app:compileDebugKotlin :app:compileDebugAndroidTestKotlin :app:testDebugUnitTest :app:assembleDebug :app:assembleDebugAndroidTest`
  passed. A focused Kotlin/Android-test Kotlin compile also passed after the
  final deprecated-callback annotation and formatting.
- `AutomaticSettingsTest` passed on the same emulator (3 tests), including
  opening settings without enabling logging, plus the existing import/recovery
  no-side-effect checks. Its first host run exposed a real crash: the platform
  `Activity` did not provide a `ViewTreeLifecycleOwner` to `ComposeView`. The
  Activity now extends AndroidX `ComponentActivity`, and content is set after
  the view hierarchy attaches. The rerun passed all three tests.
- The first isolated rendered-test invocation exposed a test-only manifest
  omission for `ComponentActivity`; adding that activity declaration allowed
  the focused component test to run and pass. Failure output remains in local
  Gradle test reports.
- The selected emulator was shut down cleanly after the focused runs. These
  results qualify API36 only and do not cover permission revocation, process
  restoration, font/RTL, TalkBack/Switch Access, manual logging, import or
  recovery migrations.
- No physical device is attached. Physical calls and selected calendar routes
  remain pending; this work does not alter the observation journal.

## Dependency trust and limits

The newest official BOM 2026.09.00 resolved Compose1.12.1 and was rejected after
AGP8.13.2 reported requirements for AGP9.1 and compile SDK37. The app remains on
its qualified AGP8.13.2 / compile SDK36 toolchain and selects BOM2026.03.00
(Compose UI/Foundation1.10.5). Gradle's candidate generation added no
unaudited trust exceptions: all193 new artifact/metadata entries were compared
with canonical HTTPS repository bytes; all67 new binary artifacts matched, and
their POMs declared Apache-2.0 or BSD-3-Clause. Forty-two embedded notice files
were recorded in the local audit receipt. POM metadata and byte agreement are
not independent signatures, legal clearance, or release approval. The original
739 verification entries were preserved exactly; the policy now has940 entries.
See [metadata audit](dependency-metadata-audit.json) and
[runtime/licence metadata audit](dependency-runtime-audit.json). The direct
ComponentActivity runtime dependency introduced8 additional module/POM entries;
all8 match canonical Google Maven repository bytes in the
[follow-up audit](dependency-componentactivity-audit.json).

The audit initially failed on direct Maven paths for Gradle-renamed Android
variant artifacts. The checker now resolves a variant only when the canonical
`.module` metadata hash is itself allowlisted and its variant binds the exact
artifact hash. The initial failed attempt remains in local `.local` evidence.

## Remaining Track 14 work

Migrate manual logging, historical import and recovery incrementally; execute
the rendered and existing settings tests on a healthy qualified emulator; test
permissions/revocation, lifecycle recreation, font scaling, RTL and accessibility;
verify no provider behavior changed; then complete review and handoff. Tracks5
and6 remain open under the scoped deferral. Track14 is not complete.

## Rendered accessibility selectors and import actions — 2026-10-24

Custom Compose action buttons now expose merged button semantics, their visible
or explicit accessibility label, and stable resource-ID-compatible test tags.
Historical-import call-type filters expose a merged checkbox role, label,
checked state and test tag. Tests cover disabled actions, absence of callback
effects during composition, explicit enabled taps, and filter semantics at
large font scale/RTL. The rendered accessibility test now inspects
`AccessibilityNodeInfo` from the active window, checking actual visibility,
clickability, focusability, label and 48 dp bounds instead of traversing the
underlying View tree.

The import instrumentation assertions now verify the selected destination and
that selected/stopped history rows resolve to the prepared session identity.
The UI harness refreshes accessibility nodes before querying them and avoids
waiting for global idle when tapping an ongoing stop action. The Track 14 UI
qualifier accepts only the exact `calltrack-track14-api36` guest, keeps that
allowlist separate from other harnesses, and compiles only the two freshly
installed app/test packages before instrumentation.

The local build command `:app:testDebugUnitTest :app:assembleDebug
:app:assembleDebugAndroidTest :app:lintDebug` completed successfully using the
isolated JDK17.0.20.1+1 and SDK36. The build receipt records
`inputs_unchanged=true`, app APK SHA-256
`53fd751266e25d08e168c6aff359eba25cd8b9ea3b4a8928d80a236d167aad4c`, and
instrumentation APK SHA-256
`47d4d55ca30cf6c6fe5347e3341c8c1f61a4f6e54ff94298f4bf30598ac3d161`.
The focused Python3.15.1 harness tests passed (3 target-selection and 16 UI
safety tests), as did `git diff --check`.

Rendered instrumentation did not run. ADB first reported the existing
`emulator-5554` guest as online, but guest property/console requests timed out;
after an ADB-server reset without restarting the computer, the guest was
offline. A read-only clone on port5556 exited with status139 and QEMU
unresponsive-thread diagnostics, then also went offline. The existing QEMU
process is blocked in uninterruptible I/O; it was left untouched. This is a
local host/emulator blocker, not an application test result. The exact native
acceptance and broader permission, process-recovery, accessibility and Android
version-matrix gates remain pending. See the
[rendered accessibility review](review-20261024-rendered-accessibility.md).

## Manual logging presentation slice — 2026-10-24

The manual “Log latest call” action and feedback moved into a small Compose
Foundation panel hosted by a lifecycle-disposed ComposeView. The picker and
permission request remain native. `CalendarSliceActivity` still owns the
explicit tap callback, call-log permission check, asynchronous delivery, result
mapping and state updates; composition performs no provider access or writes.
The original native action was removed and the new host has a stable resource
ID. The panel exposes a button role, disabled semantics, a 48 dp minimum touch
height and polite live feedback.

The regression test was added before implementation and initially failed to
compile because the panel and state type were absent. It now compiles and checks
that the action is disabled until a destination is available and only invokes
delivery after an explicit tap. The repo-local JDK 17.0.21+9/SDK 36 run of
`:app:testDebugUnitTest :app:compileDebugAndroidTestKotlin` passed (50
actionable tasks; final rerun after formatting executed 6, with 44 up-to-date).
`:app:assembleDebug :app:assembleDebugAndroidTest` also passed (86 actionable
tasks; 9 executed, 77 up-to-date). The Kotlin sources were formatted with the
pinned ktfmt 0.59. The Conductor verifier fault suite passed 19 tests, and
`scripts/verify_conductor.py` validated 211 documents, 21 tracks and 25
synthetic scenarios. No AVD was initially present. A local API 36 profile
was created using the installed arm64 image, but it remained `offline` during
this checkpoint; the rendered regression has therefore not run. See
[slice review](review-20261024-manual-logging.md). This checkpoint does not
qualify lifecycle recreation, permission revocation, RTL/font scaling or
TalkBack/Switch Access.

## Historical import status slice — 2026-10-24

The import summary and feedback are now separate lifecycle-disposed ComposeViews
with their original `import_summary` and `import_feedback` resource IDs. Compose
Foundation `BasicText` renders Activity-owned strings with polite live-region
semantics. The selected destination, date fields, call-type filters, history
navigation, preview/confirmation dialogs, bounded delivery loop, stop/cancel
behavior and saved session remain in `HistoricalImportActivity`; this slice
does not change import authority or persistence.

`HistoricalImportStatusPanelTest` checks summary/feedback rendering and state
updates. The first focused compile failed because the panel types were missing;
after implementation, `:app:compileDebugKotlin :app:compileDebugAndroidTestKotlin`
passed. The final targeted run of `:app:testDebugUnitTest
:app:compileDebugAndroidTestKotlin :app:assembleDebug
:app:assembleDebugAndroidTest` passed: 91 actionable tasks, 15 executed and 76
up-to-date. After all slice records were added, the 19-test Conductor fault
suite passed and `scripts/verify_conductor.py` validated 213 documents, 21
tracks and 25 synthetic scenarios. The rendered panel test and existing
synthetic import effect tests remain unexecuted because the new API 36 emulator profile
does not reach `adb online`.

## Receipt recovery feedback slice — 2026-10-24

The read-only recovery feedback text moved into a lifecycle-disposed
ComposeView with the existing `recovery_feedback` ID and polite live-region
semantics. All record reads, calendar lookups, check/suppress/reapproval actions,
confirmation dialogs, retention operations and pagination remain in
`ReceiptRecoveryActivity`; composition performs no recovery or calendar work.

The regression test initially failed because `RecoveryStatusPanel` did not
exist. After implementation and ktfmt 0.59 formatting, the selected JDK17.0.21+9
/ SDK36 run of `:app:testDebugUnitTest :app:compileDebugAndroidTestKotlin
:app:assembleDebug :app:assembleDebugAndroidTest` passed (91 actionable tasks;
15 executed, 76 up-to-date). Rendered feedback and synthetic recovery workflows
remain unexecuted because the API36 emulator profile stays offline. See the
[slice review](review-20261024-recovery-status.md).

## Recovery cards and actions — 2026-10-24

Recovery cards/details and the check, suppress, reapproval, open-event, refresh,
retention and pagination controls now render in lifecycle-disposed ComposeViews.
`ReceiptRecoveryActivity` still owns formatted database/provider reads,
permission requests, native confirmation dialogs, all receipt/calendar effects
and operation sequencing. The existing `recovery_refresh`, `recovery_feedback`,
`recovery_rows` and `recovery_empty` view IDs remain; new card, retention and
navigation hosts are identified by stable IDs. Compose state contains only
formatted display details, action eligibility and opaque per-render tokens; the
Activity holds the receipt/calendar mapping and ignores stale tokens. No
dependency, persistence or receipt identity changed.

The new `RecoveryCardsPanelTest` verifies card state, busy/ineligible controls,
the read-only event-open availability during background work, and that card,
management and pagination callbacks only fire on explicit taps. All four panel
tests passed in the final focused API36 emulator run. The provider-backed
`RecoveryUiTest` methods passed with one bounded synthetic LOCAL calendar token:
fixture preparation, queued-approval withdrawal on leave/return, permission
cancel then explicit reapproval, read-only existing-effect check, edit/deletion
and suppression, retention, and exact fixture cleanup. No physical phone, phone
call, user calendar or remote account was used. The initial unscoped Gradle run
without `fixtureToken` failed as expected by the provider test guard; the
qualified methods were rerun individually with the required synthetic token.

JDK17.0.21+9 compiled production and instrumentation Kotlin against SDK36.
Formatting used the checksum-verified pinned ktfmt0.59 artifact. The wider
`AutomaticSettingsTest` run passed its automatic-settings and recovery-open
checks but exposed a separate existing `HistoricalImportActivity` selector
failure (`import_create` was not found by the rendered accessibility helper).
That import-control migration/test issue remains open and is not attributed to
the recovery-card changes. Font/RTL, TalkBack/Switch Access, recreation/process
restore, destination disappearance, minimum/intermediate OS matrix and full
Track14 acceptance remain pending. See the [recovery cards review](review-20261024-recovery-cards.md).

## Compose test-tag accessibility selector — 2026-10-24

The rendered `AutomaticSettingsTest` exposed that Compose maps
`testTagsAsResourceId` tags to raw accessibility resource names, while the
test helper recognized only package-qualified Android resource IDs. The import
create-action panel now has a parent semantics node enabling tag exposure, and
`RenderedUi` matches either the raw tag or the package-qualified resource ID.
The helper still checks the target package before injecting any touch.

The selected JDK17.0.21+9 / SDK36 run passed `:app:testDebugUnitTest
:app:lintDebug :app:compileDebugAndroidTestKotlin` and all three
`AutomaticSettingsTest` methods, including no-preview/no-confirm behavior on
opening the historical import screen. `HistoricalImportActionsPanelTest` also
passed on the API36 AVD. The fixture-backed delivery journey remains pending;
its setup and UI phases were attempted in separate Gradle invocations, which
did not preserve the emulator app's Calendar permission. See the [review](review-20261024-accessibility-selector.md).

## Historical import destination selector — 2026-10-24

The historical-import native Spinner is now a lifecycle-disposed ComposeView
retaining `import_calendar_picker`. The Activity continues querying writable
calendars, checking permissions, saving/restoring the selected calendar ID,
and handling all previews and imports. Compose sees labels and fresh opaque
tokens only; the Activity maps current tokens back to IDs and ignores stale
callbacks. The component uses the existing Compose UI/Foundation stack without
adding dependencies. Its selected value is exposed as the button's accessible
label; when no choices are available, the control is disabled and shows the
localized unavailable message.

On API36 `calltrack-track14-api36`, `HistoricalImportDestinationPanelTest`
passed both enabled selection and unavailable-state checks. Its explicit tap
returned only the current opaque token, and the selected label was exposed in
the accessibility tree. `HistoricalImportActivityComposeTest` verified the
stable Compose host and disabled no-permission state. The combined focused
`HistoricalImportDestinationPanelTest`, `HistoricalImportHistoryPanelTest`
and `HistoricalImportActivityComposeTest` run passed five instrumentation
tests; after the accessibility-label adjustment, the destination component's
two tests passed again. JVM tests, lint and Android-test Kotlin/Java compilation
passed. Pinned ktfmt0.59 and `git diff --check` passed.

Provider-backed calendar selection, saving/restoring an actual selected
calendar, permission revocation, destination disappearance, TalkBack/Switch
Access and broader lifecycle/OS acceptance remain pending. No import was
previewed or delivered in these selector tests. See the
[destination selector review](review-20261024-import-destination.md).

## Historical import permission-access action — 2026-10-24

The native permission-access button now renders in a lifecycle-disposed
ComposeView with stable `import_access_panel` host ID. Its only callback is an
explicit tap that enters the existing Activity `requestPermissions` flow for
Call Log and Calendar access. Compose does not request, inspect or grant
permissions. The Android system dialog remains the consent surface; no
dependency or persistence changes were made.

On API36 `calltrack-track14-api36`, the focused suite passed six tests across
the access component, destination selector, import history and Activity
Compose host. The access component test verified no request during composition
and exactly one callback on explicit tap. The Activity test verified the host
and action are rendered in the no-permission state. `:app:testDebugUnitTest`,
`:app:lintDebug`, Android-test Kotlin/Java compilation, pinned ktfmt0.59 and
`git diff --check` passed.

This does not qualify a system permission grant/denial/revocation journey or
provider behavior. Those remain pending with physical, live-accessibility and
OS-matrix gates. See the [access action review](review-20261024-import-access.md).

## Historical import explanatory text — 2026-10-24

The native intro TextView now renders through a lifecycle-disposed ComposeView
with a stable `import_intro_panel` host. The Activity still formats the
localized explanation with its selected time-zone ID and supplies the final
text to a read-only composable. The Compose panel starts no import or provider
work, and no dependency, session or persisted state changed.

On API36 `calltrack-track14-api36`, the focused suite passed seven
instrumentation tests across the intro, access action, destination selection,
history, and Activity host. The tests verified exact zone-formatted intro
visibility, enabled/disabled destination behavior, explicit opaque-token and
permission callback dispatch, and an empty session store when the Activity is
opened. `:app:testDebugUnitTest`, `:app:lintDebug`, Android-test Kotlin/Java
compilation, pinned ktfmt0.59 and `git diff --check` passed.

Provider-backed calendar selection, permission grant/denial/revocation,
TalkBack/Switch Access, wider font/RTL and OS matrix acceptance remain pending.
See the [intro review](review-20261024-import-intro.md).

## Recovery explanatory copy — 2026-10-24

The recovery introduction and retention-policy TextViews now render through
separate lifecycle-disposed ComposeViews with stable `recovery_intro_panel`
and `recovery_retention_copy` host IDs. The Activity still supplies the exact
localized resource text and owns permission prompts, recovery queries, expiry
work and calendar access. No dependency or persisted data changed.

On API36 `calltrack-track14-api36`, the focused pure-panel test verified both
strings and the focused Activity journey verified exact resource text and
both host IDs. `:app:testDebugUnitTest`, `:app:lintDebug`, Android-test
Kotlin/Java compilation, ktfmt0.59 and `git diff --check` passed. An initial
full `RecoveryUiTest` class run without its fixture failed seven fixture-backed
journeys; it is recorded as an invalid broad invocation rather than passing
acceptance. See the [recovery copy review](review-20261024-recovery-copy.md).

Screen-reader, large-font/RTL, permission and provider journeys, retention
execution, and wider OS acceptance remain pending.

## Automatic settings introduction and access — 2026-10-24

The automatic-settings introduction and permission-access Button now render
through lifecycle-disposed ComposeViews, preserving `auto_permissions` and
adding the `auto_intro_panel` host. The Activity supplies localized copy and
keeps the four-permission system request in its explicit callback. No request
occurs during composition. Calendar selection, filters, activation
confirmation and scheduling remain unchanged.

On API36 `calltrack-track14-api36`, the pure Compose test verified the
intro/access rendering and that only an explicit tap invokes the callback.
The focused existing Activity journey verified the exact intro copy and both
Compose hosts while confirming logging stayed disabled and no destination
was selected. The first rendered-tag assertion exposed that Compose tags were
not represented as resource IDs; `testTagsAsResourceId` was added and the
focused tests passed on rerun. `:app:testDebugUnitTest`, `:app:lintDebug`,
Android-test Kotlin/Java compilation, ktfmt0.59 and `git diff --check` passed.

System permission grant/denial/revocation, provider access, full settings
parity, accessibility and lifecycle acceptance remain pending. See the
[settings access review](review-20261024-settings-access.md).

## Current-main local verification refresh — 2026-10-24

Rechecked the merged Track14 source at `046624cf1a8b286f27eb08386e3da23071fac9c3`
in a clean managed worktree. The source-bound `scripts/run_slice_build.py`
receipt reports exit code 0, unchanged inputs and matching debug app and
instrumentation APK hashes. It used qualified JDK17.0.21.0.2 and Android SDK36;
the local receipt and Gradle log are retained under ignored `.local/` evidence.
The accompanying direct Gradle run passed `:app:testDebugUnitTest`,
`:app:compileDebugAndroidTestKotlin`, both debug APK assemblies and
`:app:lintDebug` (123 actionable tasks, all executed). The JVM report contains
119 tests, with zero failures, errors or skips. Lint reports 26 warnings and
3 hints, with no lint errors. Python3.15.1 `tests.test_parity_targets` and
`tests.test_parity_ui_safety` passed all 19 tests, and
`scripts/verify_conductor.py` accepted 246 documents, 21 tracks and 25
synthetic scenarios. `git diff --check` passed.

The current ADB readback lists only `emulator-5554` as offline; no healthy
authorized emulator is available for a new rendered run. The earlier
source-bound API36 rendered receipts remain evidence for their exact earlier
candidate and are not promoted to this candidate. Provider-backed destination
selection/restoration, permission revocation, process/lifecycle recovery,
TalkBack/Switch Access and the broader OS matrix therefore remain pending.
This refresh does not close Track14 or change any physical, remote-calendar or
release gate.

## SSD reattachment and isolated emulator retry — 2026-10-24

The PortableSSD reattached at `/Volumes/PortableSSD`. Its Android SDK responds
as ADB 1.0.41 / platform-tools 37.0.1; Android platform 36 and build-tools 35.0.0
are readable. Python 3.15.1 ran from the selected user runtime, and the selected
JDK reports 17.0.21.0.2. The existing build receipt still matches current-main
source inputs and both debug APK hashes.

The prior emulator process remains in macOS state `UEs` with 0.0% CPU, an offline
ADB guest and an open console port. Its launch record identifies a read-only
cloned test instance; it was left untouched. To avoid its AVD and locks, a
fresh Pixel 6 / API 36 AVD with a separate `/tmp` data directory was created. Its
hardware and image settings match the authorized AVD. Emulator 37.2.12 found the
system image and usable Hypervisor.Framework, but its QEMU main loop and CPU
threads became unresponsive and the process exited before Android boot. The
local macOS diagnostic records `EXC_BAD_ACCESS`/`SIGSEGV` in
`android::crashreport::CrashReporter::GenerateDumpAndDie` after the emulator
hang detector fired. No app or instrumentation package was installed and no UI
acceptance suite ran. This is a reproduced host/emulator failure, not an app
test failure; the diagnostic stays local and is not committed.

Added [a scoped hosted API36 UI workflow](../../../.github/workflows/track14-hosted-ui.yml)
to run the same synthetic settings, import, recovery and accessibility
journeys on a fresh Pixel6 emulator. It builds source-bound app/test APKs on
GitHub's Linux runner, requires the four suites to verify fixture cleanup and
package removal, and prints only suite outcomes, tested methods and screenshot
hashes. Hosted execution is pending. It cannot establish local ARM64 emulator
health, physical calls, remote calendar behavior or operational acceptance.
Local `actionlint`, Conductor integrity and the 19 Python 3.15.1 parity-target/UI
safety tests pass.

The first hosted attempt stopped in emulator-package setup before building the
app: `/dev/kvm` existed, but the ephemeral runner account lacked access. The
workflow now gives only the job account ownership of that runner-local device,
checks read/write access, and requires `emulator -accel-check` to pass before
continuing. The affected CI run is not emulator acceptance; the corrected run
must build, boot and pass all four suites before results are recorded.

On the next hosted attempt, KVM acceleration and the source-bound build passed,
and the API36 emulator booted successfully in about 52 seconds. The final AVD
identity comparison failed because the emulator console response included a
carriage-return line ending. The workflow now normalizes that response before
comparison and avoids printing raw emulator logs into public Actions output.
The following hosted attempt passed boot and identity verification. Rendered
settings, import and recovery journeys passed with cleanup and package-removal
readback, but accessibility failed in
`renderedAccessibilityLabelsTargetsAndKeyboardFocus`. Its full instrumentation
output remains in the ignored runner-local receipt and was not retained in the
public log, so the failing assertion is not yet known. Added a safe failure
summary that reports app test source frames and, for unlabeled controls, only
the resource ID and widget class. It suppresses rendered text and descriptions.
The failing control is the automatic incoming-call checkbox (`auto_incoming`,
`android.view.View`). Standard `toggleable` semantics alone did not resolve the
Android accessibility node, so the row now explicitly clears and sets its
checkbox role, checked state, content description and test identity while
retaining the toggle action. On the next run the settings screen advanced, then
the same unlabeled-node check found `import_incoming` on the historical import
screen. The import call-type filters had the same custom clickable pattern and
now receive the same explicit checkbox semantics; the Compose test asserts
their label too. JDK17/SDK36 unit tests and Android-test Kotlin compilation
passed before this import-panel change; its focused build/test and the hosted
native rerun are pending. A later hosted run advanced through the settings and
import call-type controls, then failed because a screen anchor did not expose
keyboard focus. The failing anchor was `import_calendar_picker`, which is
disabled until a calendar is available; the test had incorrectly demanded
keyboard focus from disabled controls. The assertion now requires focus only
for enabled anchors, while the traversal still requires visible keyboard focus
after Tab. Safe ID/state diagnostics remain in place. The hosted repository-slice
job also caught a ktfmt formatting issue in the new import-filter modifier; it
was formatted with the workflow's pinned ktfmt0.59 checksum, and the subsequent
full formatting check passed. The corrected hosted UI run is pending. No full
accessibility parity is claimed yet.

## Hosted API36 acceptance and wider OS-matrix follow-up — 2026-10-24

The public GitHub Actions run [40095495432](https://github.com/edithatogo/CallTrack/actions/runs/40095495432)
tested PR head `abecf4db8141f03bfd1b75b1b2d8119d645f204b`, later merged to
`main` as `7ef258896184f6f1a9b77bf4ce59181399fa4b4d`. A fresh Pixel 6 API36
x86_64 guest passed the settings, historical-import, recovery and accessibility
suites. The sanitized output identifies Android16 / SDK36, all four suite
outcomes, tested methods and 44 screenshot hashes. Every suite receipt reports
fixture cleanup and test-package removal; the qualifier also gates success on
restoring the emulator's original display/font configuration.

The first passing run followed two targeted accessibility repairs. The automatic
and import picker buttons had resource IDs colliding with their ComposeView
hosts; separate `*_calendar_picker_panel` host IDs let UI lookup target the
interactive tags. Hosted accessibility then passed those controls. The next
run passed the picker and showed the recovery accessibility anchor had named
the host `recovery_refresh` instead of the button `recovery_refresh_action`;
only the keyboard-focus anchor changed. The subsequent complete four-suite run
passed.

To extend the same source-bound journeys to minimum/intermediate Android, the
hosted workflow now has API26, API33 and API36 x86_64 entries. API26 uses the
Pixel2 profile; API33 and API36 preserve Pixel6. `sdkmanager --list` on the
qualified SDK confirmed the corresponding Google APIs x86_64 system images
exist. The matrix is pending hosted execution; no result is inferred from the
API36 run.

After refreshing to merged `origin/main`,
`scripts/run_slice_build.py` completed with exit code0, unchanged input hashes
and exact current-source binding across151 app/build inputs. It used the
qualified JDK17.0.21.0.2 and SDK36, passed all119 JVM tests with zero
failures/errors/skips, assembled the debug app and instrumentation APKs and
completed lint. The receipt records app APK SHA-256
`2f505e791555f95fb664feb680abba8908cb0eb7d87d0cd898c5f16a35ea583a` and
instrumentation APK SHA-256
`f6cf4f6872268e2808acdf648081b8f361b4beea39e375d8d82019c501e8403b`.
`actionlint`, the Conductor verifier (248 documents,21 tracks,25 scenarios),
formatting and `git diff --check` pass.

The phone remains absent from ADB and the only local guest is still offline.
This evidence does not qualify API26/API33 UI, local ARM64 emulator health,
manual TalkBack/Switch Access, physical calls, real account calendars or
operational observation.
