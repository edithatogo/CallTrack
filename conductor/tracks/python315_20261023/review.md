# Local review — Python runtime unit

Reviewed all active CI job configurations, local setup/current commands, helper
entry points, import audit and safe handover evidence. This is local review, not
independent hosted/container acceptance or Android release review.

Findings fixed:

1. Compose prototype still documented a bare system-python command. It now uses
   the selected repository-local interpreter.
2. CI checked only a version tuple, permitting a non-local prefix. All four jobs
   now execute a guard for actual version, final release, prefix and executable.
3. Cold Linux created its environment outside the tooling checkout. Environment
   creation now follows checkout and lives inside qualification-tools/.venv;
   exported interpreter path and subsequent calls agree.
4. An initial guard could accept a prerelease with the same version triple. A
   seventh fault test and explicit final-release check fix that gap.

Seven focused tests and the151-test full helper suite pass. Required current-main
integration proof is next. Hosted/container runtime evidence is missing: no Git
remote/PR/authorised hosted dispatch is configured. Do not call the entire track
or durable goal complete based on configuration inspection alone.

5. Linux runtime execution exposed missing historical schema commits in an
   archive-only/shallow checkout. Full-history checkout and a regression fix the
   actual configured CI path; the original schema-provenance test stays intact.
   Full Git history in the isolated container reproduces the151-test pass.

## Final local review refresh — 2026-10-23

The successor repository target is now selected and private PR #17 is open. The
configured workflow was dispatched, but GitHub reports an account
billing/spending-limit error before any runner starts. The isolated Linux
container and macOS each pass152 tests at code revision442e680; hosted CI remains
the only Python-track execution blocker.

Track21 Task10 is complete for the locally available Linux container and for
recording the precise hosted blocker. Task11 is complete for requirement review
and corrections. Task12 and track closure remain pending until hosted CI evidence
is available. The goal is not marked complete. The original release, physical,
calendar and seven-day observation gates remain separate.
