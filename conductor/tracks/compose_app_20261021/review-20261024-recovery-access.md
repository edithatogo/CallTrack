# Track 14 review: recovery calendar access action

## Scope

The recovery screen's calendar-access button now renders in a lifecycle-disposed
ComposeView. The Activity still owns the `READ_CALENDAR` runtime permission
request. The panel only dispatches its callback after an explicit click.

## Review findings

No blocker was found in the bounded change. The new component test verifies the
callback does not run during composition and runs once after a user click. The
Activity contract test checks the stable Compose host and rendered action while
opening the screen without triggering a permission prompt.

The former Activity helper that created View buttons was only used for this
control and was removed. The other recovery actions remain independently owned
by their existing Compose panels.

## Verification

- ktfmt0.59 and google-java-format1.24.0 checks: passed.
- Python3.15.1 `test_verify_conductor.py`: 19 tests passed.
- Python3.15.1 `scripts/verify_conductor.py`: passed, 243 documents, 21 tracks,
  25 synthetic scenarios; `git diff --check` passed.
- Hosted GitHub Actions runs `40076808071` and `40076812557`: all qualification,
  slice, Compose prototype and multiplatform UI checks passed. Android development
  slice build and unsigned release packaging passed. Instrumentation sources were
  compiled but not executed on an emulator.
- Emulator execution, including grant/deny/revocation: pending selected local
  emulator availability; compiling instrumentation sources is not runtime proof.

## Remaining acceptance

Track 14 remains open for full rendered behavior, permission/revocation,
lifecycle/accessibility parity, and remaining release-linked acceptance. This
slice does not claim real permission flow or release readiness.
