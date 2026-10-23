# Review: Track 14 import-filter accessibility and layout

Date: 2026-10-24
Scope: Automated semantics and layout checks for Compose import filters at
large font scale and RTL. This does not qualify a live screen reader.

## Result

The filter component exposes its date inputs through the existing accessible
content descriptions and exposes each call type as a checked checkbox with a
48 dp minimum row height. The test composes the component at 2x font scale and
RTL, then verifies the date controls and checkbox remain displayed, the
checkbox semantics report the checkbox role and checked state, and the target
height meets the 48 dp minimum.

## Review and fixes

- A first test run invoked `fetchSemanticsNode()` inside `runOnIdle`, which is
  itself a Compose synchronization operation. The test failed with the
  framework's main-thread synchronization guard. The semantics lookup and
  assertions now run from the test thread.
- No product behavior, dependency, calendar authority, persistence or
  permission behavior changed.
- The new evidence checks labels and layout at the Compose semantics/layout
  layer; it does not simulate TalkBack navigation or Switch Access scanning.

## Verification

- `:app:compileDebugAndroidTestKotlin` and the focused
  `HistoricalImportFiltersPanelTest` on the API36 emulator passed (2 tests).
- Pinned ktfmt0.59 Kotlin-language formatting and `git diff --check` passed.

## Remaining limits

Live TalkBack/Switch Access, platform font scaling outside the component test,
RTL across other screens, permission revocation, destination loss, process
recovery and the broader Android version matrix remain pending. Track14 remains
in progress.
