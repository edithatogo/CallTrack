# Review: hosted CI recovery and gate refresh

## Scope

Reviewed the public repository state, exact-candidate GitHub Actions run, uploaded
artifact readback, Track5 release status, Track21 hosted status, and current
Conductor checkpoint. No production application code changed in this review.

## Evidence and findings

- GitHub reports `edithatogo/CallTrack` as `PUBLIC`; the Actions run proves that
  the release workflow now receives a hosted runner.
- Cold Linux workflow run
  [40007294827](https://github.com/edithatogo/CallTrack/actions/runs/40007294827)
  completed successfully. It checked out the tooling and candidate separately,
  selected Python 3.15.1, ran Conductor verification, and built candidate
  `15998ba887e76ea223a6a8ba3430dd640330ed99` with fresh SDK and Gradle directories.
- The uploaded unsigned APK SHA-256 is
  `89a548489035a6df8c464cc621e49dfbb0c9d6d16619bbe54f27f78e612d5f86`, matching
  both the workflow input and the recorded macOS candidate. The receipt reports
  115 JVM tests, zero failures/errors, two lint hints and 23 warnings.
- The receipt's `hosted_ci` and `physical_host_independence` fields remain false.
  They were not overwritten: GitHub provider metadata proves this hosted run, but
  it does not establish an independent physical-host run.
- Track14 application changes are present after the `15998ba` candidate. The
  previous current-candidate/native-matrix claims are narrowed; Track5 keeps its
  release-ready flag false until a fresh source-bound candidate and required
  acceptance are recorded.
- PR #31's hosted qualification run passed. Track21 remains active for its final
  Conductor checkpoint; unrelated Track5 gates are unchanged.

## Corrections applied

Replaced stale statements that the repository was private, Actions were blocked,
and Track21 was waiting for hosted CI. Recorded exact Track5 and Track21 run
identities, artifacts and scope. Preserved the earlier failure history and kept
production signing/key custody, source conveyance, physical calls, selected
calendar routes, observation, and current-app refreeze pending.

## Verification

- Repository-local Python interpreter: 3.15.1.
- `scripts/verify_conductor.py`: passed, 214 documents, 21 tracks, 25 synthetic
  scenarios.
- Both new evidence files and updated metadata parse as JSON.
- `git diff --check`: passed.

## Result

Review passed with the stale status and scope corrections above. Track5 is not
release-ready; Track21's final checkpoint is still open.
