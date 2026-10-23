# Python runtime selection policy

Prefer the newest qualified stable Python version for CallTrack tooling, in
keeping with the general dependency/version policy in [workflow](workflow.md).
The repository's `.python-version` is the single active selection; it currently
selects Python3.15.1, which has passed local macOS/Linux and hosted qualification.
This selection is a qualified default, not a mandatory minimum or a prohibition
on other versions.

When compatibility or a concrete task requires another version, choose the
newest suitable stable patch and record in the owning Conductor track:

- the compatibility/task reason and affected paths;
- the selected manifest version and actual interpreter path/version;
- dependency/tool behavior and focused test/build results;
- whether the selection is temporary or intended to remain.

Create `.venv` from the manifest and invoke `.venv/bin/python` explicitly. The
runtime guard compares the actual interpreter with `.python-version`, requires
a final release, and rejects non-repository environments. GitHub Actions reads
the same version file. Python helper shebangs use uv's version-file discovery so
the selection follows the repository file; regular qualification commands still
use the explicit `.venv` interpreter.

Do not downgrade or rebuild an existing qualified environment merely to match a
new preference, and do not replace an interpreter underneath an in-flight
operation. Retain previous environment and dependency receipts as history. A
version exception does not waive security, functional, packaging, or release
acceptance.

The CallTrack seven-day observation heartbeat has no Python-version constraint,
so this policy change does not require a prompt change. The separate Power
Platform follow-through automation already reflects the user's 24 October
runtime clarification; no other conflicting CallTrack automation was found.

## Verification of this policy update — 2026-10-24

The active `.python-version` remains `3.15.1`; no qualified environment was
downgraded or replaced. A fresh worktree-local `.venv` based on the already
qualified CPython3.15.1 passed the runtime guard and all162 repository tests.
The Conductor verifier passed for227 documents,21 tracks and25 synthetic
scenarios.

The alternate-selection path was exercised separately: uv read a temporary
`3.14.9` version file and selected the installed CPython3.14.9 interpreter; a
temporary repository-local environment using that interpreter passed the
runtime guard. This verifies selection/admission only. The full CallTrack
tooling and build suite remains qualified against the active3.15.1 selection;
no project-wide3.14.9 qualification or runtime switch is claimed.
