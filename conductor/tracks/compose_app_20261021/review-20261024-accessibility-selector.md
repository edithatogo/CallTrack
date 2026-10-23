# Review Report: Track 14 Compose test-tag accessibility selector

## Summary

The selector fix aligns Android accessibility lookup with the raw resource names Compose exposes for test tags while preserving package checks before injected touch; the broader Track 14 screen and accessibility acceptance remains open.

## Verification Checks

- [x] **Plan Compliance**: Partial — repaired the rendered selector failure without moving provider effects into Compose or changing persistence and delivery behavior.
- [x] **Style Compliance**: Pass — pinned ktfmt 0.59 and Google Java Format 1.24.0 checks pass.
- [x] **New Tests**: Existing rendered settings and Compose action-panel tests cover the regression.
- [x] **Test Coverage**: Partial — verifies API 36 rendered settings and action semantics; does not qualify full historical delivery, other Android versions, lifecycle restoration, RTL or assistive technology.
- [x] **Test Results**: Passed — JVM tests, lint, instrumentation Kotlin compilation, all three `AutomaticSettingsTest` cases and `HistoricalImportActionsPanelTest` passed on the API 36 emulator.

## Findings

### Medium — Compose test tags were not discoverable by the rendered UI helper

- **File**: `app/src/androidTest/java/com/markobl/calllogsync/slice/RenderedUi.java`
- **Context**: Compose exposes `testTagsAsResourceId` tags as raw resource names, but the helper matched only package-qualified Android resource IDs. The historical import action therefore appeared missing even though it was rendered.
- **Suggestion**: Accept exact raw test-tag IDs alongside package-qualified Android IDs, and retain the existing package verification before touch injection.
- **Resolution**: Applied. `HistoricalImportCreateActionPanel` now exposes tags through parent semantics. The focused settings test confirms the action is discoverable and remains disabled without access.

The synthetic import delivery journey is not accepted by this review. Its setup and rendered phases were attempted in separate Gradle invocations, and the second invocation did not retain the target app's Calendar permission; it stopped before preview or delivery. No physical-call, user-calendar, remote-calendar or operational acceptance is claimed.
