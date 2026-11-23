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
