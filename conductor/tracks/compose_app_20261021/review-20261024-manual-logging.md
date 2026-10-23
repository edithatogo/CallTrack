# Review: Track 14 manual logging presentation slice

Date: 2026-10-24
Scope: Compose presentation for the explicit “Log latest call” action and
feedback in `CalendarSliceActivity`. Historical import, recovery, full Track 14
acceptance and release qualification remain open.

## Result

The manual action is now rendered by a Compose Foundation panel inside a
lifecycle-disposed ComposeView. The native calendar picker and permission
requests remain in the Activity. The Activity remains the only owner of call
log/calendar access, asynchronous delivery, permission checks and result
mapping. Composition has no provider or synchronization effects.

## Review and fixes

An initial Material 3 implementation would have added a broad transitive graph
and contradicted decision D14-1, which limits the qualified production surface
to Compose UI/Foundation and the test harness. That dependency and its generated
trust entries were removed. The final panel uses Foundation `clickable` with a
button role, disabled semantics, a 48 dp minimum target and polite status
feedback. The Activity now extends AndroidX `ComponentActivity` so the
ComposeView can use the lifecycle owner required by its disposal strategy.

The focused regression was added first and initially failed because the panel
symbols did not exist. It now verifies that delivery is disabled without a
selected destination and occurs only after a tap. No further source-level
findings remain in this slice.

## Verification

- `:app:testDebugUnitTest :app:compileDebugAndroidTestKotlin` passed with JDK
  17.0.21+9 and SDK 36: 50 actionable tasks; final post-format run executed 6
  and 44 were up-to-date.
- `:app:assembleDebug :app:assembleDebugAndroidTest` passed: 86 actionable
  tasks, 9 executed and 77 up-to-date.
- The focused Conductor verifier fault suite passed 19 tests; the full verifier
  accepted 211 documents, 21 tracks and 25 synthetic scenarios.
- `ManualLogPanelTest` compiles but has not run on an emulator. No AVD was
  configured, so a local API 36 arm64 profile was created from the installed
  image; it remained `offline` during this review.
- The changed Kotlin files were formatted with pinned ktfmt 0.59 and
  `git diff --check` passed.

## Limits

This is a presentation migration and compile-level review, not rendered UI
acceptance. Permission grant/revocation, destination disappearance, Activity
recreation, process restoration, visual contrast across themes, font scaling,
RTL, TalkBack/Switch Access and provider regression checks remain pending. The
historical import and recovery screens remain Views. Track 14 stays in progress.
