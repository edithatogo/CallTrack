# Track 14 review: automatic settings actions

## Scope

This incremental slice replaces the automatic settings screen's Enable, Pause
and Refresh Views with a lifecycle-disposed Compose panel. The Activity continues
to own enable confirmation, permission/destination checks, scheduling, busy state
and refresh coalescing. No provider or scheduling operation runs during
composition.

## Review findings

No code-level blocker was found in this bounded diff. Enable remains disabled
until a destination, required access and at least one call type are selected;
Pause remains disabled while an operation is busy; Refresh remains available
because the existing Activity queues a refresh request while busy. The stable
`auto_enable`, `auto_pause` and `auto_refresh` accessibility/resource tags are
preserved.

The component tests verify that disabled Enable does not dispatch, enabled Enable
dispatches only after an explicit click, and Pause/Refresh callbacks run only after
explicit clicks. The Activity test waits for all three Compose accessibility nodes,
then verifies the host, rendered enabled states and absence of automatic activation.

## Verification

- `ktfmt0.59 --kotlinlang-style` on changed Kotlin sources: passed.
- `google-java-format1.24.0 --aosp` on the changed Java test: passed.
- `git diff --check`: passed.
- Python3.15.1 `test_verify_conductor.py`: 19 tests passed.
- Python3.15.1 `scripts/verify_conductor.py`: passed, 242 documents, 21 tracks,
  25 synthetic scenarios.
- Hosted GitHub Actions runs `40075923959` and `40075933266`: all qualification,
  slice, Compose prototype and multiplatform UI checks passed. The slice job built
  and verified the Android development artifact and packaged the unsigned release
  variant. Android instrumentation sources compiled as part of the build but were
  not executed on an emulator.
- Local Android SDK/emulator unavailable. Green hosted compile/package checks do
  not establish rendered emulator acceptance.

## Remaining acceptance

Emulator execution of the focused component and Activity tests, provider-backed
destination behavior, permission/revocation and lifecycle/accessibility parity
remain open under Track 14. No track-level completion or release qualification
is claimed by this slice.
