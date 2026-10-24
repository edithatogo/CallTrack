# Review Report: Track 14 hosted API36 UI acceptance

## Summary

The merged source-bound API36 hosted run now passes settings, historical import,
recovery and rendered accessibility journeys with cleanup, package-removal and
emulator-restoration checks; broader Android versions and manual/device gates
remain open.

## Verification Checks

- [x] **Plan Compliance**: The scoped hosted fallback ran the four planned
  synthetic journeys and recorded only API36 x86_64 evidence.
- [x] **Style Compliance**: Workflow YAML passed `actionlint`; changed Kotlin and
  Java passed their pinned formatter checks; `git diff --check` passed.
- [x] **New Tests**: Existing native instrumentation tests were run against the
  exact PR head; safe host/tag and focus-selector fixes were added after the
  first attempts exposed failures.
- [x] **Test Coverage**: Settings (5 methods), import (5), recovery (7) and
  accessibility (2) each passed. The hosted output included 44 screenshot
  hashes across the suite receipts. This does not prove TalkBack/Switch Access,
  font/RTL breadth, API26/API33 or physical behavior.
- [x] **Test Results**: Passed on PR head
  `abecf4db8141f03bfd1b75b1b2d8119d645f204b`, merged as `7ef258896184f6f1a9b77bf4ce59181399fa4b4d`.
  GitHub Actions run [40095495432](https://github.com/edithatogo/CallTrack/actions/runs/40095495432)
  passed all four suites; every suite reported fixture cleanup and package
  removal verified. The qualifier also rejects a passing result if emulator
  environment restoration fails.

## Findings and fixes

### Medium — calendar-picker Compose host and action shared an Android resource ID

- **File**: `app/src/main/res/values/ids.xml`
- **Context**: Rendered lookup returned the non-focusable ComposeView host rather
  than the enabled picker action.
- **Resolution**: Assigned distinct `auto_calendar_picker_panel` and
  `import_calendar_picker_panel` host IDs; retained the existing child test tags.
  Hosted accessibility then passed that control.

### Medium — recovery focus acceptance targeted the host container

- **File**: `app/src/androidTest/java/com/markobl/calllogsync/slice/PositiveUiTest.java`
- **Context**: `recovery_refresh` is the system-bars host ID; keyboard-focus
  acceptance must query the actual `recovery_refresh_action` button.
- **Resolution**: Changed only the accessibility anchor to the button tag and
  retained the host ID for layout checks. The complete API36 journey passed.

## Scope boundary

The workflow now adds source-bound API26/API33 hosted acceptance alongside its
API36 run. That matrix is pending and is intentionally not represented as
completed evidence here. The local ARM64 emulator failure, physical-device and
real-call acceptance, provider-backed destination restoration, manual
assistive-technology checks, remote-calendar routes and seven-day observation
remain separate gates.
