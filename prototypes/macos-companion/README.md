# macOS import prototype

This is a preview-only Foundation parser and AppKit file-selection prototype
for synthetic data. It is not a packaged app, supported import format, or
Android-compatible exporter. The AppKit controller opens a user-selected JSON
file, validates it under the parser's byte/count limits, and displays only
aggregate direction counts. It never displays call times, phone numbers, or
record identifiers. Calendar delivery is visibly disabled; the prototype does
not request EventKit access, read a calendar, store foreign receipts, or write
an event.

The contract and scope are recorded in `conductor/tracks/macos_companion_20261021/`.
The package targets macOS 14 and uses Foundation, AppKit, and
UniformTypeIdentifiers; no external dependencies are declared. Run `swift build
--package-path prototypes/macos-companion --target CallTrackCompanionUI` and
`swift test --package-path prototypes/macos-companion` with a qualified full
Xcode toolchain. On the current Command Line Tools-only host, the default
SwiftPM build service fails to initialize; using its deprecated native build
path successfully builds the parser and AppKit target, but XCTest is unavailable.
The AppKit target also type-checks with warnings as errors against the installed
SDK. The checked-in
`Verification/ParserContractHarness.swift` can be compiled with the Foundation
sources using `xcrun --sdk macosx swiftc -warnings-as-errors -target
arm64-apple-macosx14.0 ...` to exercise synthetic parser and selected-file
contracts; this does not replace package tests or rendered app acceptance.
