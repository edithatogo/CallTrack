# Project Context

> **Current decision (2026-11-24):** [Android 17-only native architecture](architecture-android17.md) · [machine-readable active scope](active-scope.json) · [active/deferred tracks](tracks.md). Older milestone and platform roadmaps below are historical; do not use them as permission to resume deferred projects.


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

## Python infrastructure

- [Track21 — Python3.15 migration and qualification](tracks/python315_20261023/index.md) — completed and reviewed 2026-10-24; archived in the completed-track index.
- [Current Python runtime selection policy](python-runtime-policy.md) — newest qualified stable version preferred; compatibility-based alternatives are allowed and must be documented and qualified.

## Current checkpoint

Use the [approved sequence](future-architecture.md) and [track registry](tracks.md)
to select work. Each track owns its current checklist and links its dated evidence.

| Track | Current work |
|---|---|
| 6 · Kotlin | Active conversion is complete. Verify the remaining supported upgrade and operational requirements with the Track 5 release evidence. [Plan](tracks/kotlin_20261020/plan.md) |
| 5 · Release | Select the exact candidate and work through the remaining release gates. [Verification](tracks/release_20261020/verification.md#active-release-gates) |
| 14 · Android Compose | Qualify import and recovery parity and the remaining device and accessibility acceptance. [Plan](tracks/compose_app_20261021/plan.md) |
| 15 · Shared core | Use the common fixture receipts for the extracted rules. Complete the remaining track acceptance in the approved sequence. [Plan](tracks/shared_core_20261021/plan.md) |
| 16 · macOS companion | Connect one packaged native file-to-calendar journey and qualify the delivery and recovery cases. [Plan](tracks/macos_companion_20261021/plan.md) |
| 17 · Shared UI | Use a qualified native companion journey to choose the first real presentation to share. [Plan](tracks/shared_ui_20261021/plan.md) |

Track 18 records the [Rust deferral](tracks/rust_component_20261021/deferral.md).
[Consolidated qualification](tracks/cross_platform_20261021/plan.md) precedes
[iOS](tracks/ios_companion_20261021/plan.md). Earlier milestone receipts are in the
[completed track archive](archive/index.md).
