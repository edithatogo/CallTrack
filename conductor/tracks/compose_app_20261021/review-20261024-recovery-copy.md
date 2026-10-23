# Review Report: Track14 recovery explanatory copy

## Summary

The recovery introduction and retention-policy explanation now use separate
lifecycle-disposed ComposeViews. Existing localized strings and ordering are
preserved. Permission prompts, recovery queries, retention execution, and
calendar behavior remain in `ReceiptRecoveryActivity`.

## Verification checks

- [x] **Plan compliance:** stable `recovery_intro_panel` and
  `recovery_retention_copy` hosts preserve the existing resources.
- [x] **Effect ownership:** the composable accepts only text and a test tag;
  rendering performs no ledger, calendar, permission, or expiry work.
- [x] **Dependency scope:** no dependency or persisted state changed.
- [x] **Build and tests:** `:app:testDebugUnitTest`, `:app:lintDebug`, Android
  test Kotlin/Java compilation, ktfmt0.59 and `git diff --check` passed. The
  focused panel test and focused API36 Activity text/host test each passed.

## Findings

No implementation defects found in this slice.

## Test limitation

An initial invocation selected the entire fixture-backed `RecoveryUiTest`
class without its required local calendar fixture. Seven unrelated journeys
failed because fixture data or expected controls were unavailable. The new
`recoveryExplanationsRenderWithStableComposeHosts` Activity test was then run
alone and passed; the failed broad invocation is not counted as recovery
journey acceptance.

## Remaining acceptance

Large-font/RTL layout, TalkBack/Switch Access, permission grant/denial/revocation,
provider-backed recovery, and retention execution remain separate Track14
acceptance gates. This copy migration does not close or archive the track.
