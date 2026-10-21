# Review Report: Track5 independent Linux reproduction

## Summary

The corrected candidate passes an independent physical-host cold build and exact
macOS comparison; the prepared hosted workflow and remaining release gates retain
separate acceptance requirements.

## Verification Checks

- [x] **Plan Compliance**: Partial for full Track5; complete for independent-host build preparation/execution, with hosted CI awaiting repository authority.
- [x] **Style Compliance**: Pass; bounded host-only scripts, native Gradle verification and existing Conductor evidence conventions.
- [x] **New Tests**: Yes; five Linux boundary regressions plus an actual cold strict-verification failure and clean corrected run.
- [x] **Test Coverage**: Selected release/JVM/lint/runtime resolution; no shared host caches or source mounts. No Android native matrix, signing, calendar/cloud or operational acceptance claim.
- [x] **Test Results**: Passed;115 JVM fixtures per Mac/Linux build, exact unsigned APK match, zero lint Error/Fatal findings;131 Python tests on macOS and native Linux; Conductor and workflow lint pass.

## Findings

### [Medium — fixed] Warm-cache evidence omitted three metadata trust entries

- **File**: `gradle/verification-metadata.xml`.
- **Context**: The initial cold source failed for parent/BOM metadata that cached Mac resolution did not exercise. Verification correctly rejected the files; this was an availability/reproducibility gap, not permission to weaken verification.
- **Suggestion applied**: Compare each failed guest download with fresh canonical Maven Central bytes, add only three exact SHA256 entries, preserve all736 old entries and requalify clean source/new caches.739 entries are now enforced; binary/runtime selections remain unchanged.

### [Low — fixed] File watching is unnecessary and unreliable in the emulated probe

- **File**: `scripts/qualify_linux_build.py`.
- **Context**: Native watcher polling failed in the virtualized diagnostic. A single-use cold build does not need a persistent watcher.
- **Suggestion applied**: Use supported `--no-watch-fs`; retain full source hash/status checks and strict dependency verification. The accepted native-host run completes without the exception.

### [Low — fixed] Documentation-test fixture omitted workflow files

The final reporting pass added a valid link to the prepared workflow. The isolated
Conductor test fixture copied documentation but omitted `.github`, producing a
false broken-link failure. Copy the existing workflow directory into the fixture;
retain actual broken-link rejection and rerun all131 host tests. This reporting
fixture change does not alter the frozen application build or its receipts.

## Final scope/readback

SDK downloads require approved HTTPS origins, exact size/hash and safe extraction;
existing output/cache paths are refused. Candidate SHA must be exact and clean;
source files, tests, lint, runtime graph, unsigned status and APK bytes are checked.
The physical-host claim uses independent host-side readback, not runner self-report.
Original failures and cancelled exploratory runs are retained and never counted as
successful builds. Only owned containers are stopped; no public upload, signing,
new SDK agreement, computer restart or phone/provider action occurs.

The current workflow defaults point at the frozen9464408 candidate and its actual
Mac APK hash. Tooling and candidate revisions are intentionally distinct; later
documentation/workflow updates do not relabel the artifact. No unresolved local
implementation finding remains; hosted CI and original full-M4 gates stay open.
