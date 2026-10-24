# Track 16 findings — 2026-10-24

## Requirements and behavior map

| Need | Existing evidence / owner | Track 16 decision and remaining acceptance |
|---|---|---|
| Deliberate import, preview and approval | Track 16 R19/R24/R26; current Android modules do not define a desktop interchange export | Prototype a versioned, metadata-minimal JSON document and preview parser. No Android exporter currently claims this schema; interoperability is not accepted. Native file selection and review UI remain. |
| Calendar destination | macOS EventKit surface probe in Track 12; Android provider ledger is separate | Use native EventKit as the eventual adapter; preserve platform-local ownership. Permission, writable destination discovery/selection and cancellation remain unqualified. |
| One writer and safe retries | Android `CalendarSink`/ledger patterns are implementation evidence, not a portable receipt contract | Do not insert events in this slice. Before writes, define durable intent/identity, readback comparison, uncertain-write recovery, and edit/delete preservation with golden contracts. |
| Imported history authority | Requirements and Track 15 handoff state foreign receipts do not authorize replay | Parsed records are preview values only. No receipt import, activation, sync, or write occurs. |
| Packaging/accessibility/recovery | Track 16 acceptance criteria | Requires full Xcode project build, native app lifecycle, file-picker/TCC tests, EventKit readback, VoiceOver/focus checks, and preserving upgrade/recovery. All pending. |

## Host and API evidence

- Swift 6.4 is available through Command Line Tools.
- `xcode-select -p` selects `/Library/Developer/CommandLineTools`; no full Xcode app is installed.
- The EventKit method-reference probe type-checks against the installed SDK at a macOS 14 deployment target. This proves API declaration visibility only.
- `swift test --package-path prototypes/macos-companion` fails before compilation with `Could not initialize build system` / `Unknown error parsing property list`. The Command Line Tools-only SDK also lacks the `XCTest` module, so the package test target cannot be type-checked here.
- The production parser source type-checks with warnings-as-errors against the macOS 14 target. A checked-in direct Swift harness compiles with that source and passes preview, duplicate-ID, unsupported-version, negative/overflowing timestamp, duration overflow, record-count, and oversized-input checks. This is focused contract evidence, not package or native app acceptance; see [parser boundary review](review-20261024-parser-boundaries.md).
- No EventKit permission was requested, no calendar was enumerated, and no event was written.

## Supported boundary

The local prototype accepts only schema version 1 JSON, at most 5 MiB and 10,000 records, UUID record/export identities, three direction values, epoch milliseconds, and nonnegative integer durations whose millisecond/end-time arithmetic does not overflow. It intentionally excludes phone numbers, contact names, notes, account/calendar IDs, and foreign receipts. Unknown/extra JSON fields are ignored by `JSONDecoder`; malformed records fail closed. Limits are conservative prototype guardrails, not a published format guarantee.
