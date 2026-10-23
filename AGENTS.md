# Repository instructions

## Dependency and runtime versions

Prefer the newest qualified stable version for application, provider, tooling
and build dependencies as well as runtimes. Use another version when a concrete
compatibility or task requirement calls for it; choose the newest suitable
version and record the reason, affected scope and qualification evidence. A
necessary alternate is not a policy failure and needs no additional approval.
Keep security and functional acceptance gates intact.

## Python runtime selection

Prefer the newest qualified stable Python version. `.python-version` is the
active selection and currently names Python 3.15.1. A different version is
allowed when a concrete compatibility or task requirement calls for it; choose
the newest suitable version, record the reason and scope in the active Conductor
evidence, and verify its actual repo-local interpreter and relevant tests.

Use `.python-version` for local setup and CI selection. Run project helpers with
`.venv/bin/python` after creating `.venv` from that selection. The runtime guard
checks the actual interpreter against the manifest and rejects global or
prerelease interpreters. Preserve working environments and in-flight operations;
do not downgrade or rebuild unnecessarily. See
[`conductor/python-runtime-policy.md`](conductor/python-runtime-policy.md).
