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
