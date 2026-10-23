# Review: Track 14 recovery cards and actions

Date: 2026-10-24
Scope: Compose presentation for recovery cards, explicit actions, refresh,
retention and pagination. Historical import controls and whole-track acceptance
remain open.

## Result

The recovery Activity now supplies bounded immutable card and navigation state to
Compose Foundation panels. The Activity remains the sole owner of receipt and
calendar reads/writes, permission requests, confirmations, retention, existing
effect checks, suppression, reapproval and event opening. Existing recovery
automation IDs remain addressable, including the native empty-state TextView.
No persistence schema, receipt identity, dependency or synchronization rule
changed.

## Review and fixes

- Recovery actions remain explicit; rendering does not invoke callbacks or
  provider work. Buttons use the shared 48 dp Compose button treatment and expose
  button semantics.
- The first pass retained full receipt objects inside Compose card state. Review
  removed those objects from the presentation model: Compose now has formatted
  details, eligibility and opaque per-render tokens, while an Activity-private
  map resolves actions. Replaced/stale tokens are ignored instead of applying
  to whichever record later occupies the same row.
- Suppression and reapproval continue to use the Activity's existing native
  confirmation dialogs and permission checks.
- Refresh, retention and paging state is Activity-produced and represented by
  immutable Compose state. Busy/ineligible actions are disabled.
- The retention policy text remains before the clear-expired action, matching
  the previous screen order.
- Opening a previously recorded event remains enabled while recovery is busy,
  matching the original read-only action; the regression assertion covers this.
- A first compile found an invalid explicit import of Compose's internal
  `weight` implementation; it was removed. Instrumentation compilation then
  found missing test imports/constructor arguments, which were corrected.
- The initial unscoped `RecoveryUiTest` run failed because the synthetic fixture
  token was omitted. No provider fixture was created by that failed run. The
  tests were rerun individually with a unique token and scoped cleanup.
- A broader `AutomaticSettingsTest` invocation passed settings and recovery-open
  checks but failed the existing historical-import selector assertion because
  `import_create` was not found by `RenderedUi`. This is recorded as an open
  Track14 import acceptance issue and does not change this recovery slice's
  result. A test-tag parent wrapper did not resolve the selector; that unrelated
  experiment was reverted.

## Verification

- `:app:compileDebugKotlin :app:compileDebugAndroidTestKotlin` passed with JDK
  17.0.21+9 and the SDK36 toolchain after the compile fixes.
- All four `RecoveryCardsPanelTest` cases passed on the dedicated synthetic
  API36 emulator.
- Fixture-backed `RecoveryUiTest` journeys passed for explicit reapproval,
  permission denial/cancel, queued operation cancellation on lifecycle leave,
  read-only checks, edited/deleted effects, suppression, retention and fixture
  cleanup. Only a synthetic LOCAL calendar was used.
- `openingRecoveryDoesNotCreateReceiptsOrEnableLogging` passed on the emulator;
  settings opening also passed in the wider selected test class.
- Pinned ktfmt0.59 formatting and `git diff --check` passed.

## Limits

This review closes only the recovery cards/actions presentation slice. Historical
import controls, accessibility/tree-selector investigation, font scaling, RTL,
TalkBack/Switch Access, process recreation, permission revocation beyond the
bounded recovery journeys, destination disappearance, other Android versions,
physical calls, remote calendars, release qualification and operational
observation remain unaccepted. Track14 remains in progress.
