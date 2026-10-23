# Track 16 scoped prerequisite deferral — 2026-10-24

## Reason and evidence

The user explicitly directed Track 16 implementation on 2026-10-24 while Track
14 remains in progress and Track 15 remains in progress. Track 14 has only a
reviewed settings/status slice; broader screen migration and acceptance remain.
Track 15 has only bounded JVM/Android call selection and time-range rules; its
macOS Kotlin/Native acceptance remains pending. The host has Swift 6.4 and the
macOS 27 Command Line Tools SDK, but no full Xcode installation. The public
EventKit API surface compiles against a macOS 14 deployment target. No selected
calendar, native permission/readback, package build, UI, or upgrade acceptance
is evidenced.

## Permitted scope

Proceed with requirements/dependency refresh, a bounded Foundation-only
synthetic interchange parser prototype, local contract tests, and documentation
of native acceptance. This does not authorize real calendar access or writes,
does not establish that Android exports this format, and does not complete or
defer Tracks 14 or 15. Do not start Track 17 or claim Track 16 acceptance.

## Consequences and reconsideration

The prototype is not a supported import workflow. Track 16 remains in progress;
native file selection, EventKit permissions/destination selection/write/readback,
durable intent and recovery, UI/accessibility, packaged upgrade, and selected
calendar qualification remain open. Reconsider this deferral after Track 14 and
15 acceptance (including Kotlin/Native fixtures), and when full Xcode and an
explicitly selected synthetic test calendar are available. The sequence verifier
keeps Track 16 unresolved for downstream stages even while this bounded work is
active.
