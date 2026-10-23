# Security and support status

No public supported release or hosted repository/reporting channel is configured.
Development snapshots have no security-update service-level commitment. Do not use
them for unattended routine logging until the release gates pass.

If you discover a suspected vulnerability, retain a synthetic minimal reproduction
locally. Do not open a public issue containing call data, calendar content,
credentials, databases or exploit details that expose others. When a repository is
published, its owner must configure and test a private reporting route before
inviting reports; no inbox or GitHub private-reporting capability is implied here.
For ordinary bugs, submit only redacted metadata using the prepared template.

Maintainer triage should record affected commit/version, acquisition or calendar
boundary, minimum permissions, impact, containment, fix and regression test. Audit
any release artifact that shipped the issue, update the supported-version table
when one exists, and document an upgrade that preserves the ledger and user edits.
Do not instruct users to erase app data to resolve a synchronization failure.
