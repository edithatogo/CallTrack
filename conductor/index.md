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

## Python infrastructure

- [Track21 — Python3.15 migration and qualification](tracks/python315_20261023/index.md)

## Current checkpoint

### Live gate refresh — 2026-10-23

The selected private successor repository is [edithatogo/CallTrack](https://github.com/edithatogo/CallTrack).
Its seventeen feature/research PRs are open as drafts; the [PR index](../qualification/successor-pr-series-20261023.md)
links each stage. Hosted GitHub Actions jobs fail before runner allocation because
the account billing/spending limit blocks them. This is not a passing hosted CI
result. [Track21](tracks/python315_20261023/index.md) remains locally qualified
on macOS and isolated Linux but open for this hosted gate.

[Track6](tracks/kotlin_20261020/active-migration-handoff.md) has completed the
active Kotlin code migration and selected emulator acceptance; no further Kotlin
conversion is identified. Its full M5 acceptance remains linked to Track5.
[Track5](tracks/release_20261020/index.md) remains not release-ready. The latest
read-only device check found no connected Android device. Physical calls and
selected account-calendar routes remain unqualified; the seven-day observation
is unstarted with null dates and zero accepted days/hours. The user previously
chose to retain the development identity, leaving production signing/key custody
pending.

Track14 has begun under a scoped development-only prerequisite deferral. Its first
read-only automatic-settings status slice now uses lifecycle-bound ComposeViews;
the Activity retains every provider, permission and action effect. Local build,
JVM tests and APK packaging pass. The reviewed status component and its host
Activity tests now pass on the API36 AVD; review fixed the required lifecycle
owner and test-manifest setup. Manual logging, import, recovery and broader
native accessibility/lifecycle acceptance remain. Tracks15–20 are still planned
in the approved order, with iOS last.

Tracks1–4 and research Tracks7–13 are complete, reviewed and archived. The
[Track4 parity implementation](archive/parity_20261020/index.md) passes P01–P10 for
the selected API36 emulator scope with110 JVM/73 host tests and89 successful native
invocations plus one expected process termination. All10 suites use source/APK
binding; fixtures/packages are removed and original environment restored.

[Track5 release qualification](tracks/release_20261020/index.md) now enforces739
reviewed verification entries. The [independent Linux follow-up](tracks/release_20261020/linux-reproduction-qualification-20261022.md)
qualifies9464408 on a separate native x86-64 host with fresh tools/caches, matching
two Mac builds exactly.115 JVM tests per build and131 Python tests on both systems
pass; the source/notices bundle is refreshed. Hosted CI is prepared; current
execution is blocked by the account billing/spending limit before runner assignment.
Original signing, native/platform, conveyance and operational
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

The [extended emulator matrix](tracks/release_20261020/emulator-matrix-qualification-20261022.md)
now passes selected API26/API33 suites and affected API36 recovery/accessibility:
229 native invocations, two expected terminations,115 JVM/141 host tests. It fixed
minimum-platform SQLite scoped use and undersized controls. Physical/OEM/manual
speech/contrast, remote synchronisation and public release remain pending. The
[first-release candidate](tracks/release_20261020/first-release-candidate-qualification-20261022.md)
now refreezes those fixes at15998ba: two Mac and a separate fresh Linux build match;
115 JVM/144 host tests and60 candidate native invocations across26/33/36 pass.
Fresh-install-first support has no issued predecessor; identity/key custody and
production/physical/cloud/operational/channel gates remain. The older9464408
artifact retains its original evidence. See [Track4 handoff](archive/parity_20261020/handoff.md)
and the [archive index](archive/index.md) for precise evidence boundaries.
