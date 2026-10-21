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

[Track5 release qualification](tracks/release_20261020/index.md) now enforces739
reviewed verification entries. The [independent Linux follow-up](tracks/release_20261020/linux-reproduction-qualification-20261022.md)
qualifies9464408 on a separate native x86-64 host with fresh tools/caches, matching
two Mac builds exactly.115 JVM tests per build and131 Python tests on both systems
pass; the source/notices bundle is refreshed. Hosted CI is prepared but awaits the
repository choice. Original signing, native/platform, conveyance and operational
requirements remain; the [isolated JDK](tracks/release_20261020/isolated-jdk-qualification-20261022.md)
remains qualified while the original Homebrew defect is retained as history.
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
