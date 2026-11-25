# Android 17 engineering standards plan

- [x] Enforce Kotlin, Java and Gradle warnings as errors. Run ktlint and Detekt in the required build qualification. [PR #136](https://github.com/edithatogo/CallTrack/pull/136)
- [x] Move active settings to Proto DataStore. Preserve the selected calendar, call source identity, paused policy, worker status and existing Room recovery data during upgrade.
- [x] Refine the four Compose screens with Material 3 Expressive, edge-to-edge insets and labelled controls. The integrated settings, imports, process recovery and accessibility journeys passed on the dedicated Android 17 emulator.
- [x] Wire compile-time Hilt injection into the manual logging data source and ViewModel. Keep the verified dependency graph and corresponding source with the candidate.
- [x] Qualify the Navigation 3 stack, system Back, retained tasks and adaptive forms. The launcher recovery check killed and relaunched the process with the receipt ready for confirmation and zero calendar events. Android accessibility checks passed at three native window sizes. The import journey exercised the real keyboard, 2× right-to-left date fields and focus restoration after calendar selection.
- [ ] Complete the automatic settings, historical import and recovery screen state and coroutine migration, with deterministic tests of permission loss, cancellation and recovery.
- [ ] Verify the completed screen state migration on the dedicated Android 17 emulator and in protected hosted checks. Keep the launcher process-death and queued-approval recovery checks, rendered accessibility and fixture cleanup in each qualification.

Production signing, physical routes and the controlled observation remain in [Track 5](../release_20261020/verification.md). Real app-call attribution and destination readback remain in [Track 23](../android17_call_routing_20261125/plan.md).
