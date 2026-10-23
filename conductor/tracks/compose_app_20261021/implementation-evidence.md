# Track 14 implementation checkpoint — 2026-10-23

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
