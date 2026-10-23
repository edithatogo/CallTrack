# macOS import prototype

This is a preview-only Foundation parser experiment for synthetic data. It is
not an app, supported import format, or Android-compatible exporter. It never
opens a file picker, requests EventKit access, reads a calendar, or writes an
event. Foreign identities remain inactive and are not receipts.

The contract and scope are recorded in `conductor/tracks/macos_companion_20261021/`.
The package targets macOS 14 and uses only Swift/Foundation; no external
dependencies are declared. Run `swift test --package-path prototypes/macos-companion`
with a qualified full Xcode toolchain. On the current Command Line Tools-only
host, SwiftPM fails during build-system initialization before source compilation
and XCTest is unavailable. The checked-in `Verification/ParserContractHarness.swift`
can be compiled with the production source using `xcrun --sdk macosx swiftc
-warnings-as-errors -target arm64-apple-macosx14.0 ...` to exercise focused
synthetic contracts; it does not replace the package test target.
