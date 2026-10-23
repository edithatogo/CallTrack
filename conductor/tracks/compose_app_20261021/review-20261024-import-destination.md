# Review Report: Track14 historical-import destination selector

## Summary

The native historical-import destination spinner now renders as a
lifecycle-disposed ComposeView. Calendar discovery, permissions, the selected
calendar ID, saved-state restoration, and import delivery remain Activity-owned.

## Verification checks

- [x] **Plan compliance**: The `import_calendar_picker` resource ID is retained.
  The Activity supplies labels and fresh opaque tokens; only a current token
  accepted by its Activity-side map changes the selected calendar ID.
- [x] **Effect ownership**: Composition performs no provider reads or writes.
  Selection only updates Activity state. Preview and delivery retain their
  explicit Activity callbacks and confirmation flow.
- [x] **Dependency scope**: No dependency or persisted schema changed; the
  control uses the already-qualified Compose UI/Foundation dependencies.
- [x] **Accessibility and empty state**: The selected label is exposed as the
  button content description. API36 tests covered explicit option selection
  and the disabled no-permission state.
- [x] **Build and tests**: `:app:testDebugUnitTest`, `:app:lintDebug`,
  `:app:compileDebugAndroidTestKotlin`, and the focused API36 instrumentation
  runs passed. Pinned ktfmt0.59 and `git diff --check` passed.

## Findings

No implementation defects found in the reviewed slice.

## Remaining acceptance

Provider-backed option loading/selection and selected-calendar restoration
were not exercised by this bounded component/permission-denied run. Permission
revocation, destination disappearance, TalkBack/Switch Access and wider OS
acceptance remain Track14 gates. The change does not close or archive Track14.
