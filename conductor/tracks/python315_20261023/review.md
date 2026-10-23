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
