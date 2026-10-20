# Project Context

## Definition

- [Product Definition](product.md)
- [Product Guidelines](product-guidelines.md)
- [Tech Stack](tech-stack.md)
- [Requirements inventory](requirements.md)
- [Roadmap](roadmap.md)

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

Tracks 1–3 are complete, reviewed and archived. The [manual call-to-calendar slice](archive/calendar_slice_20261020/index.md) passed its source-bound build, synthetic native provider/SQLite/UI acceptance and cleanup. [Track 4 parity and durable synchronisation](tracks/parity_20261020/index.md) is in progress: the durable logging/import core, WorkManager runner and explicit automatic settings have bounded API36 emulator evidence; import/recovery UI, status hardening and full positive delivery/background acceptance remain pending. [Track 5 release preparation](tracks/release_20261020/index.md) is in progress with contributor/lifecycle/release guidance; candidate and operational acceptance remain gated on M3. [Track6 Kotlin migration](tracks/kotlin_20261020/index.md) is active with a bounded pure-component conversion and preserved Java/data contracts; full migration and prerequisite release acceptance remain open. [Track7 Compose research](archive/compose_20261020/index.md) is complete and archived with a retain-Views/defer-adoption decision. Its prototype builds, but guarded native UI acceptance remains blocked by emulator system dialogs; no production Compose UI is adopted. [Track8 KMP research](archive/kmp_20261020/index.md) is complete and archived; it retains current code and defers shared-core adoption; equivalent target tests remain pending. [Track9 shared-UI discovery](archive/multiplatform_ui_20261020/index.md) is complete with an isolated desktop prototype and deferred production adoption. [Track10 integration discovery](archive/destinations_20261020/index.md) is complete with native-provider-first contracts and deferred direct connectors. [Track11 productivity discovery](archive/productivity_20261020/index.md) is complete with candidate slices and deferred implementation; user priority remains provisional. [Track12 platform feasibility](archive/platform_feasibility_20261020/index.md) is complete with a corrected conditional-dialer capability matrix and compile-only EventKit probe; native runtime remains unqualified. [Track13 Rust evaluation](archive/rust_20261020/index.md) is complete with an isolated host experiment and retain-Kotlin/defer-Rust decision. All original tracks have been activated; Tracks4–6 remain incomplete. Automatic logging, live-call acceptance, provider cloud sync and production release remain later outcomes.
