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

Use the [Android 17 scope](active-scope.json) and [track registry](tracks.md) to select work.

| Track | Remaining work |
|---|---|
| 14 · Android Compose | Carry the qualified screen roots and durable writer into the production acceptance for Tracks 5 and 6. [Track 14 evidence](tracks/compose_app_20261021/implementation-evidence.md#android-17-native-screen-roots-and-durable-delivery) |
| 5 · Release | The signed 0.1.0 candidate reproduced across macOS and Linux. Answered and missed calls passed on the emulator. The physical missed and outgoing cases synced to Google. Complete the remaining physical cases, seven days of observation and GitHub distribution. [Candidate and gates](tracks/release_20261020/verification.md#calltrack-community-010-candidate) |
| 6 · Kotlin | Close K07 against the same supported upgrade, recovery and operational evidence as Track 5. [Track 6](tracks/kotlin_20261020/plan.md) |
| 23 · Call routing | Verify controlled calls from the selected third-party apps and read back their approved calendars. Hosted Android 17 and the preserving dev upgrade passed. [Track 23](tracks/android17_call_routing_20261125/index.md) |

The dedicated Mac AVD, protected-main Mac dispatch and hosted Android 17 qualification passed. The local hook and runner evidence is recorded in [#117](https://github.com/edithatogo/CallTrack/issues/117). The [Renovate dashboard](https://github.com/edithatogo/CallTrack/issues/91) records current dependency updates. The [offline scanner PR](https://github.com/edithatogo/CallTrack/pull/89) requires a qualified upstream image and model plus a trusted isolated Linux x64 runner for CallTrack.

See [deferred tracks](tracks.md#deferred-no-active-implementation-or-automatic-ci) and the [completed track archive](archive/index.md) for historical work.
