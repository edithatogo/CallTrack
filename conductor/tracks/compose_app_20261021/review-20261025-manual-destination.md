# Review: Manual calendar destination Compose slice

Date: 2026-10-25  
Scope: launcher manual-log calendar access and destination controls. This review
does not close Track14's broader screen parity, physical-device or operational
acceptance.

## Findings and decisions

- Replaced the manual screen's `Button` and `Spinner` with a
  lifecycle-disposed `ComposeView` and explicit Compose controls. Kept
  `requestPermissions`, calendar provider reads, selection persistence and
  delivery in `CalendarSliceActivity`.
- Preserved the `slice_calendar_picker` accessibility resource ID and the
  existing `slice` preference key `calendar`.
- Bound provider options to fresh opaque tokens for each asynchronous provider
  read. The Activity maps only the current token set to provider IDs and ignores
  missing tokens, permission loss and actions during refresh or delivery.
- Preserved saved-selection restoration and missing-destination behavior. Added
  a safe label for null/blank provider display names.
- Added Compose coverage for explicit access and destination callbacks and stale
  selection tokens. Extended provider instrumentation and the hosted settings
  journey to choose a visible fixture calendar and verify the stored ID without
  invoking logging.
- The first hosted matrix run failed all three APIs at an assertion that
  compared a button's `contentDescription` to its label. The acceptance harness
  now checks the unchanged preference after provider loading and verifies the
  selected option's visible text separately; it does not infer failure from a
  platform-specific accessibility property.
- Review found the rendered helper's touch selectors were private. Added narrow
  visible-ID/text touch methods for the new acceptance path; no application
  control bypasses accessibility discovery or actual touch injection.

## Verification

- JDK17.0.21.0.2 / SDK36: `:app:testDebugUnitTest`, `:app:lintDebug`,
  `:app:compileDebugAndroidTestKotlin`,
  `:app:compileDebugAndroidTestJavaWithJavac` and
  `:app:assembleDebugAndroidTest` passed.
- Pinned google-java-format1.24.0 and ktfmt0.59 passed.
- Python3.15.1: 54 `tests.test_parity_ui_safety` and
  `tests.test_verify_conductor` tests passed; `scripts/verify_conductor.py`
  passed (254 documents, 21 tracks, 25 synthetic scenarios).
- `git diff --check` passed.
- Instrumentation sources compile. Rendered runtime for this source revision
  remains pending because the only enumerated local emulator is offline; the PR
  hosted API26/API33/API36 matrix is the next required acceptance evidence.
- Hosted run [40156597838](https://github.com/edithatogo/CallTrack/actions/runs/40156597838)
  failed on API26/API33/API36 at the initial `contentDescription` comparison;
  fixture cleanup and package removal passed on each. The assertion fix is in
  the follow-up commit and requires a fresh matrix before acceptance.

## Remaining gates

The PR must pass build qualification and the hosted rendered API26/API33/API36
journeys. Manual physical-device permission, provider, screen-reader and
operational acceptance remain distinct from emulator evidence.
