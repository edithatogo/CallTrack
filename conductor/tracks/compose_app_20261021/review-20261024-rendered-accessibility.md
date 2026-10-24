# Review: Track 14 rendered accessibility selectors and import actions

Date: 2026-10-24
Scope: Compose action/filter semantics, rendered accessibility inspection,
history destination/session assertions, and the bounded Track 14 emulator UI
qualification harness.

## Result

The Compose controls expose merged labels and roles suitable for assistive
technology and stable test selectors. The rendered test now checks the active
window accessibility tree rather than inferring reachability from backing
Views. Import tests assert destination and prepared-session identity. The UI
harness limits installation to an exact dedicated guest, refuses unhealthy
guests, retains evidence on failure, and dex-compiles only fresh app/test
packages before instrumentation.

No defect was found in the reviewed local changes. Provider access, permission
decisions, import delivery, identity, and persistence remain Activity-owned.
Composition invokes no callbacks; actions run only after enabled explicit taps.

## Verification

- `:app:testDebugUnitTest :app:assembleDebug :app:assembleDebugAndroidTest
  :app:lintDebug` passed with isolated JDK17.0.20.1+1 and SDK36.
- Build receipt reports unchanged inputs and records both APK SHA-256 hashes
  in `build-receipt.json` under local ignored evidence.
- Python3.15.1 `test_parity_targets.py`: 3 passed.
- Python3.15.1 `test_parity_ui_safety.py`: 16 passed.
- `git diff --check` passed.

## Acceptance limits

Rendered instrumentation was blocked by the local emulator. The prior guest
timed out on guest commands, became offline after the ADB server reset, and a
read-only clone exited with status139 and QEMU unresponsive-thread diagnostics.
The affected QEMU process remained in uninterruptible I/O and was left
untouched. No restart was performed. No rendered UI result is claimed.

Re-run on the exact healthy Track 14 emulator before accepting screen-reader
navigation, touch dimensions, rendered selectors, import destination/session
journeys, or broader permission, recovery, OS-version and release parity.
Track14 remains in progress.
