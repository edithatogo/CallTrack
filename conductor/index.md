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

[Track5 release preparation](tracks/release_20261020/index.md) remains active for
source-matched candidate, upgrade, signing/distribution and operational gates.
[Track6 Kotlin migration](tracks/kotlin_20261020/index.md) retains its bounded
three-component pilot; remaining conversions/removal and M4 prerequisites stay
open. Native providers/Views/SQLite remain selected. Research Tracks7–13 do not
adopt production Compose/KMP/shared UI/direct cloud connectors/Rust or establish
second-platform runtime parity.

Physical calls, remote provider synchronization, wider Android/OEM/manual
accessibility qualification and public release remain pending. See [Track4 handoff](archive/parity_20261020/handoff.md)
and the [archive index](archive/index.md) for precise evidence boundaries.
