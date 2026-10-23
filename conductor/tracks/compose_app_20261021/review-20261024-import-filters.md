# Review: Track 14 historical import filters

Date: 2026-10-24
Scope: Migrate date range and call-type filters from native views to Compose.
The rest of Track 14 and its platform/accessibility gates remain open.

## Result

The import date and call-type controls now live in a lifecycle-disposed
`ComposeView`. The Activity retains filter state and remains the authority for
range parsing, destination and permissions, import sessions, confirmations and
calendar delivery. Existing filter view IDs and saved-state keys remain; the
filter host has the stable ID `import_filters`.

## Review and fixes

- The Compose UI receives immutable filter state and reports only explicit
  user edits. It performs no provider or persistence work.
- Date strings continue to use the existing `ImportRange.parse` validation
  when the user requests a preview. Type defaults and Activity recreation
  state are retained.
- Call-type rows expose checkbox semantics and at least a 48 dp touch target.
- The Activity test covers rendered initial state, date entry, changing one
  type, recreation, persisted values, disabled actions and absence of an import
  session. The component test verifies no state callback occurs during initial
  composition and that explicit edits emit state.
- `AutomaticSettingsTest` now checks that import summary, actions and filters
  have their Compose hosts; the stronger Activity Compose test replaces its
  prior accessibility-tree assertions for disabled actions.
- The old `RenderedUi` touch injection was unreliable for the new Compose
  checkbox on this emulator. The experiment using shell `input tap` did not
  improve it and was reverted. Activity-level Compose pointer-input testing
  passed, so the helper was not changed for this slice.
- No dependency, provider, persistence, receipt, calendar event or release
  identity changed.

## Verification

- `:app:testDebugUnitTest`, `:app:lintDebug`,
  `:app:compileDebugAndroidTestKotlin` and the selected API36 emulator tests
  passed. Five selected instrumentation tests passed, including the Activity
  recreation case.
- ktfmt0.59 Kotlin-language style check and `git diff --check` passed.

## Remaining limits

This closes only the import filter migration slice. Other Android OS levels,
permission revocation and restoration, destination disappearance, cancellation
recovery, large font/RTL, TalkBack/Switch Access, full import/recovery journeys,
physical calls, remote calendars, release qualification and operational
observation remain separate acceptance gates. Track14 remains in progress.
