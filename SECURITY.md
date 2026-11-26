# Security and support status

No public supported release is available.
Development snapshots have no security-update service-level commitment. Do not use
them for unattended routine logging until the release gates pass.

If you discover a suspected vulnerability, report it privately through
[GitHub's private vulnerability reporting](https://github.com/edithatogo/CallTrack/security/advisories/new).
Use a synthetic minimal reproduction and do not include real call data, calendar
content, credentials or databases. Do not open a public issue containing exploit
details that expose others. For ordinary bugs, submit only redacted metadata using
the prepared template. Private reporting is enabled; an end-to-end report delivery
test has not yet been performed.

Maintainer triage should record affected commit/version, acquisition or calendar
boundary, minimum permissions, impact, containment, fix and regression test. Audit
any release artifact that shipped the issue, update the supported-version table
when one exists, and document an upgrade that preserves the ledger and user edits.
Do not instruct users to erase app data to resolve a synchronization failure.

## Repository security checks

| Required GitHub check | Coverage |
| --- | --- |
| `workflow-lint` | OpenGrep for the Android app and Python tooling, actionlint for GitHub Actions, and ShellCheck for the security and fuzz runners. |
| `jazzer-call-data` | Coverage-guided fuzzing of call-to-event conversion, restricted identities and historical import dates with synthetic input. |
| `privacy-and-secrets` | Gitleaks and Presidio through the pinned shared action. Pull requests scan introduced commits for secrets and changed tracked text for supported personal identifiers. Scheduled scans audit reachable secret history and current tracked text. |
| `trufflehog-history` | TruffleHog scans fetched Git history. Credential verification is disabled to keep potential secrets local. Findings and scan errors fail the check. |
| `CodeQL` and language analyses | Static analysis across the enabled languages, including Java and Kotlin. |

Run OpenGrep on Apple Silicon or Linux x64 with `sh scripts/run-opengrep.sh`.
The runner needs `curl`, `jq` and a SHA-256 utility. It verifies the pinned
release checksum on every run and caches the executable under `.local`.
The rules check Android manifest protections, direct network permission, app
logging and Python shell execution. Kotlin logging uses OpenGrep's text
matching because its Kotlin parser rejects some valid syntax in the app.
Strict mode and finding failures are enabled. The runner prints rule IDs,
paths and line numbers while keeping raw scanner output out of CI logs.
[OpenGrep 2.0.0](https://github.com/opengrep/opengrep/releases/tag/v2.0.0)

The local fuzz runner and shared privacy documentation provide setup commands
and describe the Gitleaks and Presidio scan scopes. The shared action runs
the pinned Presidio 2.2.366 compatibility build on isolated Python 3.15.2.
[Jazzer runner](fuzz/README.md) · [Shared privacy checks](https://github.com/edithatogo/repository-standards/blob/23fe1fb4f99021b1a721f63dea09a2369ba8845d/docs/privacy-secrets-standard.md)

## Offline scanner

The [offline workflow](.github/workflows/offline-ai-security.yml) uses the
[published Apple Container profile](https://github.com/edithatogo/repository-standards/pull/191).
The Linux x64 scanner runs against the exact commit and event base with its
network disabled and a read-only policy and checkout. A single-use official
GitHub runner verifies the result for the same run attempt, policy and image.
Use a runner only for the verified job identified by the run and attempt
labels. The regular GitHub security, Android accessibility and recovery checks
continue to run on their existing triggers.

## Optional staged private-key check

With `pre-commit` 4.7.0 installed in the repository's `.venv`, run the SHA-pinned
private-key header check on staged text without installing a Git hook:

```sh
PATH="$PWD/.venv/bin:$PATH" PRE_COMMIT_HOME="$PWD/.local/pre-commit" .venv/bin/pre-commit run detect-private-key
```

This optional check supplements the independent shared Presidio/Gitleaks gate;
it does not replace its configuration or broader coverage.
