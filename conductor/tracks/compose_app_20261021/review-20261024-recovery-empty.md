# Review: Track 14 recovery empty state

Date: 2026-10-24
Scope: Move the recovery empty-state text presentation from `TextView` to
Compose. Data and effect ownership remain in the Activity.

## Result

`ReceiptRecoveryActivity` now hosts the localized empty-state message in a
lifecycle-disposed `ComposeView` while preserving the `recovery_empty` view ID.
The Activity supplies visibility from the completed, bounded ledger query; the
composable only renders the message. The permission button, ledger read,
calendar labels, recovery actions and pagination remain Activity-owned.

## Review and fixes

- The empty state is initially hidden, matching the former `TextView.GONE`
  behavior. It becomes visible only when the Activity receives an empty page;
  a nonempty page hides it.
- The compatibility ID remains on the `ComposeView`. The Activity-level
  no-side-effect test verifies the host type and waits for the localized
  empty-state message after the Activity's ledger query completes.
- A component test checks both visibility states and confirms composition
  itself has no callback/effect path.
- D14-4 supersedes D14-3's earlier decision to retain a native empty-state
  view; the reason and rollback are recorded in the decision log.
- No dependency, permission, database, receipt, calendar or synchronization
  behavior changed.

## Verification

- `:app:testDebugUnitTest`, `:app:lintDebug`,
  `:app:compileDebugAndroidTestKotlin`, and the focused API36 emulator tests
  passed. The selected `RecoveryCardsPanelTest` and `AutomaticSettingsTest`
  instrumentation cases passed (8 tests total).
- Pinned ktfmt0.59 Kotlin-language formatting and `git diff --check` passed.

## Remaining limits

Permission revocation, destination disappearance, cancellation/process
recovery, live TalkBack/Switch Access, broader font/RTL checks, other Android
versions, physical calls, remote calendars, release qualification and
operational observation remain open. Track14 remains in progress.
