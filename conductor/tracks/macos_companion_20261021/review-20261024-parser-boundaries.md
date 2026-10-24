# Track 16 parser boundary review — 2026-10-24

## Scope

Reviewed only the permitted preview-parser prototype under the active Track 16
prerequisite deferral. No native file picker, EventKit access, persistent state,
calendar write, or product import claim was added.

## Finding and change

The direct synthetic harness already covered malformed input, duplicate call
identity, unsupported versions, byte limits, and duration overflow. It did not
exercise negative timestamps, timestamp addition overflow, or the record-count
limit. Added those boundary cases to both the XCTest suite and the standalone
contract harness. The repeated record in the over-limit fixture deliberately
confirms the parser enforces the count limit before identity iteration.

## Verification

Ran the harness with the available Command Line Tools SDK:

```text
xcrun --sdk macosx swiftc -warnings-as-errors -target arm64-apple-macosx14.0 \
  prototypes/macos-companion/Sources/CallTrackImport/CallLogImport.swift \
  prototypes/macos-companion/Verification/ParserContractHarness.swift \
  -o /tmp/calltrack-parser-contract
/tmp/calltrack-parser-contract
PASS: preview, identity, version, timestamp, duration, count, and size contracts
```

This is source/harness evidence only. SwiftPM/XCTest, full Xcode, app UI, native
file selection, EventKit permission/provider readback, restart recovery, and
packaged upgrade acceptance remain pending. Track 16 stays in progress and the
existing prerequisite deferral remains in force.
