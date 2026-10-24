# Track 14 review: automatic settings navigation

## Scope

The automatic settings screen's Import and Recovery navigation buttons now use a
lifecycle-disposed Compose panel. Stable `open_imports` and `open_recovery`
accessibility/resource tags remain; the Activity still owns both `startActivity`
callbacks.

## Review findings

No blocker was found in the bounded change. The Compose test verifies that
rendering dispatches no navigation and that each destination callback runs only
after an explicit click. The Activity contract test checks the Compose host and
waits for both accessible actions without opening either destination.

## Verification

- ktfmt0.59 and google-java-format1.24.0 checks: passed.
- Python3.15.1 `test_verify_conductor.py`: 19 tests passed.
- Python3.15.1 `scripts/verify_conductor.py`: passed, 244 documents, 21 tracks,
  25 synthetic scenarios; `git diff --check` passed.
- Hosted GitHub Actions runs `40077733636` and `40077737812`: all qualification,
  slice, Compose prototype and multiplatform UI checks passed. Android development
  slice build and unsigned release packaging passed; instrumentation was compiled
  but not run on an emulator.
- Emulator rendering and navigation acceptance: pending emulator execution.

## Remaining acceptance

Track14 remains open for full rendered, lifecycle, accessibility and broader
acceptance. Hosted compile/package evidence will not be counted as emulator
navigation proof.
