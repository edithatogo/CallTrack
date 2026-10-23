# Inventory and verification

## Reused evidence

Commit96d0c72 is integrated on main: [dependency refresh](../../../qualification/dependency-refresh-20261023.md)
records144 Python helper tests, Conductor169 documents/20 tracks/25 scenarios,
Python3.15.1 actual local environment, and JDK17.0.21+9 checks/115 JVM tests/APK
builds/lint. Those tests are not rerun solely to recreate historical evidence.
Private receipts remain under `.local/dependency-refresh-20261023/` and are not
copied into Git. Frozen APK SHA25689a548489035a6df8c464cc621e49dfbb0c9d6d16619bbe54f27f78e612d5f86
was rechecked unchanged. No new APK qualification is claimed by this track.

## Current surface inventory

| Surface | Current selection | Required follow-through |
| --- | --- | --- |
| Local helper/test commands | `.venv/bin/python`, actual3.15.1 | Version/prefix guard passes; integrated-HEAD proof in progress |
| Script shebangs | uv selects3.15.1; local `.venv` present | Real guard invocation and entry-point readback |
| Regular CI three jobs | setup-python exact3.15.1, local `.venv` | Actual guard configured; hosted execution pending |
| Cold Linux container CI | Immutable Temurin image, setup-python3.15.1 | Environment moved inside qualification-tools checkout with actual guard; Linux/hosted proof pending |
| Python dependencies/locks | Standard library and repository-local modules; `.python-version`3.15.1 | Audit actual imports; no pip lock invented |
| Compose prototype instructions | Found stale `python3` helper example | Fixed to explicit `.venv/bin/python` |
| Observation heartbeat | Agent-driven readiness protocol; no explicit older Python command | Follow workflow runtime requirement; do not alter observation/native state |
| `.local/analysis-venv` | Inactive historical APK analysis,3.15.0/Androguard | Preserve history; never reactivate without separate3.15.1 qualification |
| Earlier one-off release runner | JDK qualification process reached terminal exit0 | Environment handover completed; no running Python task found in this repo at audit |

No configured Git remote exists. No PR or hosted CI result is available. Do not
create/push a remote or claim the CI container executed based on YAML alone.

## Current review findings

Stale Compose command and missing repo-prefix runtime enforcement identified;
fixes/positive and fault tests are in progress. Source-only review is not hosted,
container, device or operational proof.

## First concrete implementation unit

Added `scripts/verify_python_runtime.py` and seven runtime-admission tests. The
initial red run failed because the guard did not exist; the implemented guard
passes the real3.15.1 repo `.venv` and rejects old minor/patch, prerelease, stale
manifest, system prefix and external executable. Seven focused tests pass.
Full helper suite:151 tests pass (6.169s). Conductor:173 documents/21 tracks/25
scenarios before the review document was added. No device actions occurred.

AST import audit of52 active helper/test files found no third-party import roots.
There are no Python package metadata/lock/container recipe files requiring a
runtime constraint change. The existing Temurin container job installs exact
Python3.15.1 and creates the tooling checkout's own `.venv`; all four CI jobs now
invoke the actual runtime guard. This is configured-path/source review, not an
observed hosted/container run. The local final-runtime guard reports
`/Users/doughnut/Documents/calltrack/.venv/bin/python` and prefix ending in `.venv`.
The existing analysis environment is inactive and retained solely with historical
APK-analysis evidence. It is not an alternative active runner.

The previous one-off JDK build runner reached terminal exit0 before this unit;
no environment was replaced under it. No running Python helper belonging to this
repository was found at the inventory point (apart from the inventory command).

Private receipts: `track21-import-audit.json`, `runtime-guard-red.log`,
`track21-preintegration-tests.log`, `python-runtime.json` in
`.local/dependency-refresh-20261023/`. Current-main post-integration proof will be
recorded separately with the actual commit after integration.

## Integrated implementation proof

Integrated commit256eff0e9df11d2269c705797832d10fd969b99f on main passed the real
runtime guard,151 helper tests and Conductor174 documents/21 tracks/25 scenarios.
The receipt proves unchanged HEAD, clean tracked worktree, no remote configured
and unchanged native app/Gradle inputs since96d0c72. Private exact-command logs
and `track21-current-head.json` retain timestamps and actual interpreter readback.
A documentation-only checkpoint records this result; the final current-HEAD
receipt is refreshed after that checkpoint without editing historical receipts.

Remaining gate: actual hosted/container execution of the configured Python3.15.1
jobs has not occurred. No Git remote/PR/hosted dispatch is configured or authorised.
The durable goal remains active and this track remains in_progress. Do not create
an external repository or mutate the retained Linux host/container/native bindings
to manufacture that proof. Android's138-artifact trust qualification is a separate
Track5 dependency concern, not a Python test failure.

## Linux execution and CI finding

The previously qualified physical Linux host's Docker engine is live. A new
owned container `calltrack-python315-20261023`, without mounts/ports/privilege,
uses the retained Temurin image digest from release reproduction. No retained
release container or native device is modified. Installed the existing workflow's
Linux prerequisites only inside this container, then disconnected its network.
The official actions/python-versions Ubuntu22.04 x64 CPython3.15.1 archive
(release3.15.1-39619321210) matched GitHub asset SHA256
`d5089a4a7ab6cea9664fb26cdae5c6379f880edc368a5be81999455c25cda556`.
Actual local-prefix runtime guard passed on `/work/calltrack/.venv/bin/python`.

The first archive-only snapshot correctly failed a historical-schema test:
`git show` could not resolve frozen schema commits absent from that snapshot.
This exposed the regular CI jobs' default shallow-checkout bug. Full history
was transferred in a local Git bundle; HEAD and tree matched the original source
exactly. The unchanged151-test suite and Conductor then passed on Linux.
Qualification CI now requests full history on all three checkouts. A regression
failed against the old configuration and passes with the fix. No historical
schema assertion was weakened and no interpreter compatibility fallback was used.

The new regression raises the suite count to152. Final integrated-head Mac/Linux
rechecks and safe owned-container stop are recorded in private receipts after
commit. This is actual Linux container execution using the same Python binary
release family as setup-python, not a hosted GitHub Actions run or an execution
of its installation action. Hosted dispatch remains unavailable for this separate
CallTrack history until its contribution target is selected. The original-upstream
checksum PR is [assessed separately](../../../qualification/upstream-contribution-assessment-20261023.md)
and does not establish Python CI acceptance for this repository.

## Final code qualification

Code commit442e680b403ac5038ef118303f5c44eea1402a8c passed152 tests, actual runtime
readback/guard and Conductor174 documents/21 tracks/25 scenarios on both macOS
and the isolated Linux container. Both used explicit repo-local Python3.15.1.
Linux HEAD and tree13dc4faa55d2d6d0c24ec5dc8d36845628fcb515 matched the local
integrated commit exactly. The container had no network during tests, no mounts,
no published ports and no privileged mode; after terminal success it was stopped
and read back as exited. Original release containers and devices were untouched.
Private `final-mac.json`, `final-linux-receipt.json`, logs and final container-state
readback remain under `.local/dependency-refresh-20261023/linux-python315/`.

The user's request for actual PRs exposed the previously unpublished successor
history. The original-upstream fork and checksum PR now exist, but they do not
supply a hosted target for the separate calendar application/Python tooling. A
successor-target question is pending after explaining the product difference.
No further hosted Python acceptance or successor PR publication can be performed
without that selection. Track remains in_progress; the durable goal is blocked
on that repeatedly verified external prerequisite, rather than marked complete.
