# macOS import prototype

This is a preview-only Foundation parser and AppKit file-selection prototype
for synthetic data. It is not a packaged app, supported import format, or
Android-compatible exporter. The AppKit controller opens a user-selected JSON
file, validates it under the parser's byte/count limits, and displays only
aggregate direction counts. It never displays call times, phone numbers, or
record identifiers. The AppKit preview keeps calendar delivery visibly
disabled and makes no EventKit request or calendar read/write. A separate
`CallTrackDelivery` module now contains an unconnected EventKit adapter and
delivery-coordinator prototype, exercised only with a fake in-memory gateway.

The contract and scope are recorded in `conductor/tracks/macos_companion_20261021/`.
The package targets macOS 14 and uses Foundation, AppKit, UniformTypeIdentifiers,
EventKit, and Apple's SQLite library through a local system module; it adds no
third-party dependency. The ledger stores a minimal durable intent before any
possible EventKit write, and recovery never retries an unknown outcome
automatically. A future host app must include
`NSCalendarsFullAccessUsageDescription` before requesting full access; write-only
access is insufficient for destination selection and exact readback.

Run `swift build --package-path prototypes/macos-companion --target
CallTrackCompanionUI`, `swift run --package-path prototypes/macos-companion
delivery-ledger-contract`, and `swift test --package-path
prototypes/macos-companion` with a qualified full Xcode toolchain. The ledger
harness exercises SQLite persistence, deduplication, interruption recovery,
uncertain writes, and human-edit/deletion preservation using a fake provider.
On the current Command Line Tools-only host, the default SwiftPM build service
fails to initialize; the deprecated native build path successfully builds the
parser, AppKit, and delivery targets, but XCTest is unavailable. These checks do
not replace rendered app acceptance, EventKit TCC, or native calendar readback.
