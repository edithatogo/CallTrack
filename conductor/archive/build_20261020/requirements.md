# Build requirements and remaining uncertainties

| ID | Acceptance | Evidence / outcome |
|---|---|---|
| B01 | Reproduce pinned upstream without source edits | Original assemble/lint passes; clean rebuild identical APK hash; R13 |
| B02 | Select coherent modern toolchain | AGP8.13.2/Gradle8.13/JDK17/compile-target36/build-tools35.0.0; actual build receipts; R26 |
| B03 | Establish bounded Android17 device feasibility | Launch plus zero-row denied/granted/revoked tests with test-only identity; R08 |
| B04 | Compare fork repair with fresh build | Configuration-only modernisation, reusable scaffolding and retained defects mapped in decisions; proceed with fork |
| B05 | Qualify formatting/static analysis/local CI commands | AOSP formatter1.24.0 check; Android lint; Python tests/document validator; prepared hosted matrix |
| B06 | Keep source, signing and package identities distinct | Pinned source/revision; original GPL retained; disposable test IDs; debug certificate receipt; production identity/key deferred |
| B07 | Preserve recovery and privacy | Source reference unchanged, no personal data queries, test-only packages removed; prepared-copy overwrite guards |
| B08 | Recheck final repair and review fixes | Final repaired clean build/device evidence and formal review; required before archive |

## Handoff, not hidden completion claims

- **Calendar slice:** build on this qualified fork configuration; choose app-local source layout and development identity without claiming ownership of upstream package. Add contacts/calendar permissions only for the bounded feature. R02–R04/R19 still require actual event readback.
- **Parity:** permission UX, denied/regrant recovery, real call records, boot/Doze/force-stop handling, concurrency and checkpoint logic remain unproven. A zero-row provider test is not real logging acceptance. Retain F01–F25.
- **Dependency owners:** most runtime libraries remain old. Re-evaluate WorkManager, networking, auth/token handling, persistence and UI libraries with contract/upgrade tests. Fragment1.5.1 and ViewModel KTX2.5.1 overrides expire/review at the next UI/dependency migration; remove only when a selected version transitively supplies compatible checks and acceptance passes.
- **Release:** select final namespace/application ID and key custody/signing lineage; test minimum OS/API26 if retained, distribution requirements, vulnerability/licence inventory and full dependency verification. Debug signing and sideload permission grant do not establish release readiness.
- **Modern targets:** compile/target37, AGP9 and Android17-specific API adoption remain separate decisions. Running a target36 app on OS37 does not qualify SDK37 compilation.
- **CI:** workflow syntax/commands are locally checked; hosted execution awaits a repository with Actions enabled. No remote run is claimed.
