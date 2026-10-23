# Track 14 refined requirements

| ID | Requirement | First-slice acceptance |
|---|---|---|
| C14-1 | Status presentation is a pure rendering of supplied immutable state. | Compose screen has no Android provider, permission, scheduling, database or worker dependency. |
| C14-2 | Compose and Views coexist incrementally. | A lifecycle-disposed `ComposeView` replaces only the four read-only status labels; the surrounding controls retain their existing IDs and callbacks. |
| C14-3 | Existing operational semantics remain authoritative. | Activity continues loading/formatting activation state, counts, worker status and action feedback; no recomposition initiates work. |
| C14-4 | Status remains accessible. | State label exposes polite live-region semantics; text and test tag are available to UI tests. |
| C14-5 | Trust and acceptance evidence remain honest. | Every added dependency is pinned through reviewed metadata checksums; native rendered acceptance is reported separately from JVM/build tests. |
| C14-6 | Recovery cards and controls keep effect ownership in the Activity. | Compose receives bounded immutable presentation state and opaque per-render tokens; receipt/calendar objects stay Activity-owned, stale tokens are ignored, and recovery host IDs remain addressable. |
| C14-7 | Rendered UI selectors resolve both native Android resource IDs and Compose test-tag resource IDs. | Accessibility traversal finds namespaced Android IDs and raw IDs exposed by `testTagsAsResourceId`, then verifies the selected action's package before injecting touch. |
| C14-8 | Historical import filters expose accessible date labels and checkbox semantics, with usable touch targets at large font scale and RTL layout. | Compose instrumentation verifies date content descriptions, checkbox role/state, visible controls, and a minimum 48 dp call-type target under 2x font scale and RTL. |
| C14-9 | Recovery empty-state presentation is a pure rendering of Activity-supplied query state. | The stable `recovery_empty` host renders the localized message only when the Activity's completed recovery query reports no records; it performs no ledger/calendar work. |
| C14-10 | Historical import history is rendered from Activity-supplied immutable session labels, while session identity and resume authority remain Activity-owned. | The stable `import_history` host shows localized title, bounded ordered rows or the empty message; only tapping a current opaque row token loads its mapped session. |
| C14-11 | Historical import destination selection may move to Compose without moving provider or destination authority out of the Activity. | The `import_calendar_picker` Compose host displays Activity-loaded choices; selecting a current opaque token updates the Activity-owned calendar ID. Permission state, saved destination ID, and later import confirmation remain Activity-owned. |
| C14-12 | The historical-import permission-access action may move to Compose while runtime permission requests remain platform- and Activity-owned. | A lifecycle-disposed Compose host exposes an accessible 48 dp action and dispatches no request during composition; an explicit tap enters the existing Activity `requestPermissions` path and leaves the Android system dialog in charge of consent. |
| C14-13 | The historical-import explanation may move to Compose without changing the Activity-selected time zone or starting import work. | The stable `import_intro_panel` renders the same localized, zone-formatted text supplied by the Activity and creates no session or provider effect. |
| C14-14 | Recovery explanations remain read-only when moved to Compose. | Stable intro and retention-copy hosts render the existing localized resources supplied by the Activity; Compose performs no permission, ledger, calendar or retention work. |
| C14-15 | Automatic-settings introduction and permission access can migrate independently of policy controls. | Stable Compose hosts render the localized introduction and accessible access action; only an explicit tap calls the existing Activity permission request, while destination, filters and activation remain Activity-owned. |

Historical-import controls and any remaining recovery parity work stay gated on
their own lifecycle, accessibility and behavior acceptance. Completed slices do
not close the remaining Track14 migration or its broader acceptance matrix.
