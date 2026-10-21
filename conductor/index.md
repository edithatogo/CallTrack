# Project Context

## Definition

- [Product Definition](product.md)
- [Product Guidelines](product-guidelines.md)
- [Tech Stack](tech-stack.md)
- [Requirements inventory](requirements.md)
- [Roadmap](roadmap.md)
- [Approved future implementation sequence](future-architecture.md)

## Workflow

- [Workflow](workflow.md)
- [Code Style Guides](code_styleguides)
- [Dependency decision template](templates/dependency-decision.md)

## Tracks

- [Tracks Registry](tracks.md)
- [Tracks Directory](tracks)
- [Completed Track Archive](archive/index.md)

## Research inputs

- [Baseline audit](research/baseline-audit.md)
- [Future architecture](research/future-architecture.md)

## Current checkpoint

Tracks1–4 and research Tracks7–13 are complete, reviewed and archived. The
[Track4 parity implementation](archive/parity_20261020/index.md) passes P01–P10 for
the selected API36 emulator scope with110 JVM/73 host tests and89 successful native
invocations plus one expected process termination. All10 suites use source/APK
binding; fixtures/packages are removed and original environment restored.

[Track5 release qualification](tracks/release_20261020/index.md) now enforces736
audited dependency entries and qualifies239 canonical binary artifacts,15 native
schema/provider upgrade invocations and two clean unsigned5578bf5 builds with115
JVM tests each. The [asset/source follow-up](tracks/release_20261020/assets-source-qualification-20261022.md)
qualifies the5a3a3e4 source/notices candidate, two clean115-test builds and a
source-only rebuild;126 host tests pass. It remains active for original production,
independent/platform, licence-conveyance and operational gates; an [isolated vendor-signed JDK](tracks/release_20261020/isolated-jdk-qualification-20261022.md)
now resolves the bundle-resource-seal blocker for that explicit build path; the
existing Homebrew defect remains recorded.
[Track6 Kotlin migration](tracks/kotlin_20261020/index.md) now has36 production Kotlin files and one documented Java nullable-label
bridge after33 active conversions and14 unreachable inherited-file removals.
Code review/fixes and emulator recovery are recorded; the final selected API36
matrix passes89 successful native invocations plus one expected termination, with
115 JVM/92 host tests and verified cleanup. See the [current handoff](tracks/kotlin_20261020/active-migration-handoff.md).
M4-linked full acceptance remains open. Native providers/Views/SQLite remain selected. Research Tracks7–13 do not
adopt production Compose/KMP/shared UI/direct cloud connectors/Rust or establish
second-platform runtime parity.

Physical calls, remote provider synchronization, wider Android/OEM/manual
accessibility qualification and public release remain pending. See [Track4 handoff](archive/parity_20261020/handoff.md)
and the [archive index](archive/index.md) for precise evidence boundaries.
