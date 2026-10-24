# Track 16 file-selection preview prototype — 2026-10-25

## Scope

Added an AppKit controller with a user-triggered `NSOpenPanel` for one JSON
file. The selected security-scoped URL is read through `CallLogFilePreviewer`,
which rejects non-regular files, checks the 5 MiB size limit before reading,
and caps the actual `FileHandle` read at limit plus one byte to cover a file
that grows after its metadata check. The existing parser continues to enforce
schema, checksum, canonical encoding, record-count, identity, timestamp, and
duration constraints.

The preview shows only total, incoming, outgoing, and missed counts. It does
not show timestamps, phone numbers, or record IDs. A disabled delivery button
and visible copy state that no calendar access or event write occurs. There is
no EventKit import, persistence, receipt activation, external connection, or
automatic file access. This is a prototype only; Android does not yet claim
the version-1 interchange format.

## Verification

- `swift build --build-system native --package-path prototypes/macos-companion
  --target CallTrackCompanionUI` succeeds locally and compiles both the parser
  and AppKit UI target. The default SwiftPM build system still fails while
  initializing its build service on this Command Line Tools-only installation.
- The `CallTrackImport` module and `CallLogImportPreviewController` also
  type-check directly with `-warnings-as-errors` against the installed macOS
  SDK at deployment target macOS 14.
- The direct synthetic parser/file harness passes, including opening one
  regular JSON file, rejecting a directory and oversized file, parsing the
  Android golden fixture, rejecting all 422 single-byte mutations, and
  validating existing schema, identity, timestamp, duration, checksum, and
  10,000-record boundaries.
- `actionlint .github/workflows/macos-companion.yml` passes.
- SwiftPM package tests remain unavailable locally. The native build-system
  path reaches the test target but fails because this Command Line Tools-only
  installation has no `XCTest` module. Full Xcode is absent; the hosted macOS
  workflow builds the AppKit target and runs XCTest.
- The controller has not been rendered in an app or qualified with VoiceOver,
  keyboard navigation, large text, interruption, or package upgrade. Those
  remain open native acceptance tasks.

## Reconsideration

Continue to EventKit only after Track 14's acceptance issue is resolved and the
durable intent, destination ownership, exact readback, uncertainty recovery,
human edit/deletion, and selected-calendar policy are specified and reviewed.
This prototype does not permit or qualify calendar access or delivery.
