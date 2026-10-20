# Compose discovery and current-screen map

Evidence refresh2026-10-21; source baseline e6607c0; track start8e9289a.

## Screen/state inventory

| Surface | Existing responsibility | Compose acceptance/migration requirement |
|---|---|---|
| CalendarSliceActivity intro/access button | Explicit calendar permission request; onResume rechecks permission and refreshes only destinations | Rendering/recomposition, launch and permission grant must never read/log a call; OS permission remains authoritative |
| Destination Spinner | Placeholder, writable choices, persisted calendar ID; no fallback when saved ID disappears | Stable IDs across refresh/reorder; lost destination clears selection; no automatic alternative calendar |
| Latest-call button | Disabled while busy/refreshing, no selection or missing calendar permission; call permission grant requires another tap | One explicit action, captured destination, no effects from recomposition/recreation; controller owns operation lifecycle |
| Status TextView | Polite live region; ready/working/permission/no calendar/failure/review messages | Semantic live region, readable errors and no personal diagnostics; retain all SliceEngine result mappings |
| Async controller | Single executor, refresh generation guard, destroyed-Activity callback checks | Compose does not replace durable delivery, serialize operations or grant permission; derive state from authoritative controller |
| Legacy Views/fragments | Old MainActivity, Main/Log/Settings screens, endpoint controls and navigation remain packaged but old entry disabled | Audit reachability and remove only after actual parity; no reason to translate old endpoint/UI blindly |

The current app has one active manual screen, not a navigation graph for automatic
logging/import/review. Those M3 flows are unfinished. Screen-state refactoring should
follow stable operation-owner/cancellation/refresh contracts, not invent them here.
Current application strings include English-only development messages; production
localization and TalkBack/Switch Access acceptance are not established.

## Tooling research

[Official migration guidance](https://developer.android.com/develop/ui/compose/migrate/strategy)
supports incremental coexistence. ComposeView can replace a small section while
retaining an Activity/controller, with an explicit composition disposal strategy.
A new whole Compose Activity would increase simultaneous lifecycle/permission changes.
Our judgement: use an isolated experiment now, then a bounded status/selection
integration only after controller/state contracts and actual accessibility acceptance.

The [BOM guide](https://developer.android.com/develop/ui/compose/bom) currently shows
stable2026.09.00 and explains that compiler versions are separate. The experiment
pins BOM2026.03.00: UI1.10.5 and Material3 1.4.0. [Compiler setup](https://developer.android.com/develop/ui/compose/compiler)
uses the Kotlin Compose compiler plugin; pin2.3.21 to the already qualified Kotlin
family rather than changing compiler/AGP alongside UI research. Activity-compose
1.12.4 AAR declares compileSdk36/minAGP8.9.1; UI1.10.5 declares compileSdk35/minAGP8.6.0.
Our compile36/AGP8.13.2 clears those metadata floors. Actual resolution/compilation
and transitive selections are separate evidence, not inferred from metadata alone.

[Accessibility testing guidance](https://developer.android.com/develop/ui/compose/accessibility/testing)
distinguishes automation from manual service use. Material controls/semantics, radio
roles, live regions, scroll reachability and automated checks are useful evidence;
TalkBack, Switch Access and human task completion still require manual acceptance.
The experiment confines API34+ accessibility checks to the dedicated API36 emulator.

## Library takeover comparison

| Option | Replaces | Remaining owned/native work | Decision |
|---|---|---|---|
| Keep Views | No new dependency | Existing widget wiring/controller; qualify full M3 UI | Retain working app for now |
| ComposeView + Material3 | Layout, some accessibility semantics and manual widget updates | Permission launchers, selected-calendar persistence, generation/cancellation, outbox | Candidate staged integration after gates |
| Compose Activity + ViewModel | Wider state/rendering lifecycle | Same platform/persistence work; ViewModel does not survive process death by itself | Defer broader lifecycle migration |
| Navigation Compose/DI/MVI library | New multi-screen routing/state scaffolding | Still needs real M3 workflows and dependency migrations | No demonstrated requirement for this one-screen experiment |
| Compose Multiplatform | Some later UI/state reuse | Android acquisition and permission/calendar adapters stay native | Later track; not conferred by Android Compose prototype |

New AndroidX graph can conflict with the root app's old lifecycle/navigation/Fragment
overrides. The standalone project measures its own graph without upgrading those
working dependencies. No package, source namespace, SQLite schema, event marker,
engine or root runtime dependency change occurs. No speed/battery/size win is claimed.

## Prototype limits and exit gates

Synthetic calendar choices and simulated access are explicit. Saved fixture access
is not an OS permission truth; real integration must recheck on resume/revocation.
RememberSaveable stores small UI state, not a sync queue, permission, event receipt
or running operation. Counter restoration proves no repeated synthetic action only.
Production busy/review status must be reconstructed from the durable controller.
No provider or actual call behaviour is proven by a synthetic button callback.

A justified retain/defer outcome can close this research track under its original
spec, without claiming adopted screens or completed production M6. Reopen the
migration decision after M3/M5 stabilization, manual accessibility acceptance and
an explicit production integration plan. Do not silently inherit prototype versions.

## Measured prototype observations

Three policy tests and Compose compilation/package/lint checks pass. Resolved debug
graph contains106 module coordinates, UI1.10.5, lifecycle-runtime2.9.4, Activity
Compose1.12.4 and stdlib2.3.21. That lifecycle differs from the root app2.5.1
selection, supporting separate qualification instead of copying dependencies.
The report is a module/constraint inventory, not shipped-binary or licence clearance.

The first native run had a screenshot-path error despite four passing UI tests.
Capture was corrected to the running target's private storage. The next run passed
five tests, but image inspection found a system ANR dialog over the app. This
exposed a verification gap: semantics/accessibility checks alone can pass while
a system window obstructs the user. Tests now require the prototype to own the
active accessible window, as well as inspecting the final image. The emulator's
observed system dialog was dismissed with Wait, without changing ANR settings or
restarting it. This is environment evidence, not an app performance attribution.
Final unobstructed results are recorded in verification after the guarded rerun.
