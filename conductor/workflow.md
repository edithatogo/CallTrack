# Project workflow

Customised from the Conductor setup workflow for research-led Android development. User direction on 2026-10-20 takes precedence over treating every future track as a fixed implementation requirement.

## Track lifecycle

Use `[ ]` pending, `[~]` in progress and `[x]` complete. Record evidence and commit references when completing tasks. Metadata distinguishes `new`, active and completed work; a new track is not an approved production design.

1. Read the index, product, stack, track spec/plan and prerequisite outcomes. Refresh stale evidence when the track starts; do not repeat completed mapping without a reason.
2. Map current behaviour, user needs, native APIs, existing dependencies and app-owned code. Document sources, versions, uncertainties and gaps.
3. Research options and define acceptance criteria. Compare native implementation, current implementation, library replacement and deferral. Prototype with synthetic data where this resolves a decision.
4. Record a decision and revised requirements/spec before implementation. Implementation tasks are conditional until this evidence exists. Routine reversible decisions within the authorised scope do not require another approval; ask only for genuinely missing product choices or authority.
5. For meaningful behaviour changes, establish failing regression/contract tests, implement, then refactor. Research/documentation tasks use source/link/traceability checks, not artificial unit tests. Select coverage tools in the build track; critical failure paths matter more than an arbitrary percentage.
6. Verify applicable local, native device and remote provider outcomes separately. A successful build or prototype does not prove production suitability. Preserve working outputs until replacement and recovery pass.
7. Update requirements, stack, dependency decisions, plan and roadmap; record remaining risks and next-track handoff. Commit scoped changes and record the checkpoint. No real phone logs, APK analysis, signing keys or credentials in Git.

## Dependency lifecycle

Use the [decision template](templates/dependency-decision.md). Every owning track assesses maturity, licence, release activity, platform/version support, transitive dependencies, build complexity, testing, performance where material and maintenance cost. Include library takeover of custom code as a first-class option.

Prototype-only dependencies have bounded scope, test data, review/expiry milestone, and removal or promotion criteria. Production promotion requires reproducible builds, acceptance/upgrade tests, data migration and rollback, and replacement of duplicated responsibilities. A library may be useful temporarily even if unsuitable long-term. Conversely, app-owned code is not privileged merely because we wrote it.

## Phase checkpoint

Read the phase outputs and verify cited evidence, requirement links and unresolved questions. For code changes, run the relevant tests and record exact commands/results. For device/provider tests, record actual readback and preserve human edits. A research phase can close with a documented rejection or deferral; implementation may then be marked not applicable with rationale, not falsely completed.

## Execution boundaries

Planning authorisation does not itself install phone apps, grant permissions, create cloud consent, make purchases, publish releases or communicate with third parties. Follow the scope of the active user request. No forced Git reset or overwriting working data. No automatic package upgrades solely to achieve newest versions.

## Commands and style

Build qualification commands and prerequisites are maintained in [qualification README](../qualification/README.md); use the exact toolchain for each variant. Native test harness execution requires a selected authorised device and remains separate from build-only CI. Use the supplied [general style guide](code_styleguides/general.md). The Kotlin track selected the maintained [Kotlin guide](code_styleguides/kotlin.md), including checksum-pinned ktfmt and contract-preserving Java interop.

Research outputs: source-pinned findings, mapped requirements, option comparison, decision and handoff. Implementation outputs: tests, code, migration/recovery evidence and updated documentation. Distribution is a separate release track.
