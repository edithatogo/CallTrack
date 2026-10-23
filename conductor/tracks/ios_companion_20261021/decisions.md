# Track 20 decisions — 2026-10-24

## IOS-D20-01: Readiness research only until the iOS-last gate opens

**Status:** scoped prerequisite deferral; no iOS application implementation or
dependency adoption.

Preserve the approved order. Research can refresh iOS API, permission and
toolchain requirements while the stage-1–8 outcomes remain open, but it does not
count as an iOS stage disposition. Do not add an iOS target, Swift/Kotlin app
code, Compose iOS UI, package, dependency or signing configuration in this
scope.

For a later implementation, keep SwiftUI + native EventKit adapters as the
candidate boundary around the qualified KMP core. Reassess shared Compose screens
only after Track 17 and Track 19 qualify a useful common workflow. Compare
EventKitUI user-mediated creation with direct access levels against exact
readback, batch review and recovery requirements; do not choose calendar access
level until the selected APIs and OS support matrix are verified. No cloud route
or automatic call acquisition is implied.

Reconsider after Tracks 1–8 pass or have explicit justified dispositions, a
qualified interchange contract and shared core/UI exist, and full Xcode plus a
selected simulator/device test scope are available. Refresh the exact current
compatibility matrix, dependency graph and release/signing authority then.
