# Android 17 engineering standards plan

- [x] Enforce Kotlin, Java and Gradle warnings as errors. Run ktlint and Detekt in the required build qualification. [PR #136](https://github.com/edithatogo/CallTrack/pull/136)
- [x] Move active settings to Proto DataStore. Preserve the selected calendar, call source identity, paused policy, worker status and existing Room recovery data during upgrade.
- [x] Refine the four Compose screens with Material 3 Expressive, edge-to-edge insets and labelled controls. The integrated settings, imports, process recovery and accessibility journeys passed on the dedicated Android 17 emulator.
- [x] Wire compile-time Hilt injection into the manual logging data source and ViewModel. Keep the verified dependency graph and corresponding source with the candidate.
- [~] Qualify the Navigation 3 stack, back handling, retained tasks and adaptive forms across the supported app journeys.
- [ ] Complete the remaining screen state and coroutine migration, with deterministic tests of permission loss, cancellation and recovery.
- [ ] Verify the exact integrated build, rendered accessibility and recovery on the Android 17 emulator, then complete protected hosted checks.

Production signing, physical routes and the controlled observation remain in [Track 5](../release_20261020/verification.md). Real app-call attribution and destination readback remain in [Track 23](../android17_call_routing_20261125/plan.md).
