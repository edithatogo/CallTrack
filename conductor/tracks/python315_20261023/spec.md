# Specification

## Objective and requirements

1. Inventory every active Python development, build, test, automation, CI,
   container and helper path; identify actual interpreter, constraints, locks,
   dependencies, native bindings and ownership. Reuse completed work from96d0c72.
2. Require explicitly selected repository-local Python3.15.1 for every active
   path. Remove older defaults and compatibility runners; keep immutable history
   and deliberate synthetic version fixtures. A manifest or installed binary
   alone is insufficient. Guard actual interpreter/prefix and report readback.
3. Qualify supported latest Python dependencies in isolation, fix incompatibilities
   and align current instructions/constraints/locks. Standard-library-only active
   helpers require no invented pip lock or additional runtime dependency.
4. Preserve active processes and hand over only after terminal state. Retain the
   inactive APK-analysis environment/history; reactivation requires separate
   Python3.15.1 dependency qualification. Preserve source history/private data.
5. Run meaningful positive/fault tests, full helper suite and documentation checks;
   review/fix findings, then verify the actual integrated main HEAD. Retain
   commit-bound post-integration readback. No passing pre-commit run is labelled
   post-integration proof. Record hosted/container evidence separately.
6. Keep real blockers explicit. No email/external communication, native device,
   calendar, account, permission, production signing or observation changes.
   Do not rewrite frozen APK, historical research or historical test receipts.

## Acceptance

Every active path is accounted for, runtime readback passes, negative tests reject
wrong Python and non-local environments, current instructions select the qualified
runtime, tests pass on the integrated commit and review findings are fixed.
Hosted CI/container runtime proof is distinct: absent remote/authorised execution
remains an open evidence gate, not a green result. A local success does not close
Track5 release gates. No migration completion is claimed while required evidence
is missing.

## Out of scope

Android runtime/AGP dependency promotion (including the138-artifact trust gap),
production release, physical/cloud/operational acceptance, cross-platform product
features, new remote/publication or activating archived APK-analysis tooling.
