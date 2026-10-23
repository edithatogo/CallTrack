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

Historical-import controls and any remaining recovery parity work stay gated on
their own lifecycle, accessibility and behavior acceptance. Completed slices do
not close the remaining Track14 migration or its broader acceptance matrix.
