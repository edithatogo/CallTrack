# Review Report: Track14 automatic-settings introduction and access action

## Summary

The automatic-settings intro and permission-access button now render through
separate lifecycle-disposed ComposeViews. The existing `auto_permissions`
resource ID is preserved, and the Activity still owns the Android permission
request. Destination selection, call-type filters, activation confirmation,
and scheduling remain in place.

## Verification checks

- [x] **Plan compliance:** the stable intro/access hosts render the existing
  localized strings.
- [x] **Effect ownership:** no request occurs during composition; an explicit
  tap invokes only the supplied Activity callback.
- [x] **Accessibility selector:** the intro tag is exposed as a resource ID,
  and the access action uses the established 48 dp Compose button semantics.
- [x] **Behavior preservation:** the API36 Activity test confirms the intro,
  access action and status hosts render while activation remains disabled and
  no destination is selected without access.
- [x] **Build and tests:** `:app:testDebugUnitTest`, `:app:lintDebug`, Android
  test Kotlin/Java compilation, two focused API36 instrumentation tests,
  ktfmt0.59 and `git diff --check` passed.

## Findings and fixes

The first Activity assertion could not find the intro test tag in Android's
accessibility tree. The intro panel now uses the project's
`testTagsAsResourceId` semantics, and both the component and Activity tests
pass. No remaining findings.

## Remaining acceptance

This slice does not test permission grant, denial or revocation through the
system dialog, or qualify provider behavior, full settings parity,
large-font/RTL, TalkBack/Switch Access, lifecycle restoration or other OS
versions. These remain Track14 gates.
