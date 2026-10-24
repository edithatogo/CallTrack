# Track 19 prototype interchange contract — schema v1

**Status:** Frozen for the synthetic Android/JVM-to-Swift preview prototype at
source revision `b3e8717fda6239b2a30c556d8f013121c3ff9f7f` (PR #64). This is not
a stable public format, an app feature, or production adoption.

## Document shape

The Android producer emits compact UTF-8 JSON with this exact member order and
decimal spelling:

```json
{"schema_version":1,"export_id":"00000000-0000-4000-8000-000000000001","record_count":0,"calls":[],"content_sha256":"<64 lowercase hexadecimal characters>"}
```

The example's export ID/checksum are illustrative. The repository's
`app/src/test/resources/call-log-v1.json` golden fixture contains two synthetic
rows and is authoritative for the shared producer/parser bytes.

| Member | Type and rule |
|---|---|
| `schema_version` | JSON integer exactly `1`. A future incompatible shape requires a new version. |
| `export_id` | Lowercase, hyphenated UUID string. Android generates a random UUID per export; tests may inject one. It identifies this export only and grants no replay authority. |
| `record_count` | Nonnegative base-10 integer exactly equal to `calls.length`. |
| `calls` | At most 10,000 call objects, sorted by `started_at_ms`, then lowercase `id` string. |
| `calls[].id` | Lowercase UUIDv8 string deterministically derived from the Android source-install namespace and numeric source-row ID as specified below. |
| `calls[].direction` | Exactly `incoming`, `outgoing` or `missed`; unknown Android call types fail closed. |
| `calls[].started_at_ms` | Nonnegative signed-64-bit Unix epoch milliseconds, copied from the source row without timezone conversion. |
| `calls[].duration_seconds` | Nonnegative signed-64-bit whole seconds, copied from the source row. End-time arithmetic must not overflow. |
| `content_sha256` | Lowercase SHA-256 hex over the canonical UTF-8 bytes of the object through `calls`, excluding this member and its preceding comma. It detects accidental corruption only; it is not a signature or authenticity proof. |

Each call object has the exact key order `id`, `direction`, `started_at_ms`,
`duration_seconds`. Root fields have the exact order shown above. The parser
accepts the canonical document with no final line ending, one LF, or one CRLF.
Other whitespace, number spellings, duplicate keys and unknown fields fail
closed. The checksum is computed before this byte-form check and cannot confer
identity, authorization or destination ownership.

## Portable identity derivation

The Android source key is the existing `CallRecord.key()` value:
`<namespace.length>:<namespace><id.length>:<id>`, where Kotlin `String.length`
counts UTF-16 code units. The source row ID must be a canonical nonnegative
decimal `Long`. Hash the UTF-8 bytes of
`org.calltrack.export.v1\0` followed by that source key with SHA-256. Take the
first 16 digest bytes, set the UUID version nibble to `8` and the RFC variant
bits to `10`, then render lowercase UUID text. This is deterministic only within
the source namespace and row identity. It excludes the raw namespace and row ID
from the document; it is an identifier, not a secret or cryptographic
authentication token.

The producer fails closed for null collection/row, invalid source ID, duplicate
derived IDs, unsupported direction, timestamp/duration overflow, more than
10,000 rows, or output larger than 5 MiB. The Swift consumer enforces the same
5 MiB/10,000-row bounds and rejects negative/overflowing time, duplicate
portable IDs, count mismatch, unsupported versions, invalid checksum and
noncanonical JSON.

## Data and authority boundary

The prototype carries call direction, time, duration, a random export ID and
derived row IDs. It omits names, phone numbers, presentation/account fields,
raw source IDs, calendar IDs, destination identities and all calendar receipts.
Call timing/direction remain sensitive personal data. This encoder does not
query call history, select rows, save/share a file, call a calendar API or
initiate delivery. `CallLogImporter.preview` only returns parsed preview data.
No foreign export/record ID authorizes receipt import or event replay.

Version 1 is strict and prototype-only. Any production proposal must refresh
requirements, version evolution, data minimization/consent, authenticity needs,
restore semantics, compatibility and licence/dependency review before adoption.
This prototype has no checksum key, signature, encryption, migration or
durable-receipt behavior.

## Exact qualification matrix

| Boundary | Current evidence | Status |
|---|---|---|
| Android producer | `CallLogExport.kt`, shared synthetic golden, and four JVM tests; `:app:testDebugUnitTest` passed locally with JDK 17.0.21.0.2, Gradle 8.13, AGP 8.13.2, Kotlin/KGP 2.3.21, compile SDK 36/build tools 35.0.0. The complete hosted matrix passed on the merged PR #64 source. | Prototype encoder test only; not invoked by the app. |
| Swift preview | `CallLogImport.swift` and direct source harness compiled with Swift 6.4.0.34.1 for `arm64-apple-macosx14.0`; shared golden and contract/fault cases passed. | Source-level preview only. Full Xcode is not selected; SwiftPM/XCTest fails before source compilation in XCBuild, and XCTest is absent from the installed Command Line Tools. |
| Kotlin/Native shared rules | Track 15's bounded KMP/JVM logic has separate evidence. | No macOS Kotlin/Native target pass; full Xcode prerequisite remains open. |
| Android UI/file export and rendered acceptance | No Android app action calls the encoder; no file chooser/export action is implemented. | Not implemented/accepted. |
| Native macOS app and file selection | No packaged companion or selected-file native workflow. | Not implemented/accepted. |
| Calendar/provider delivery | No CalendarContract or EventKit call is made by this prototype. | No native event readback or remote synchronization claim. |
| Recovery and upgrades | No durable portable receipt, restore operation or packaged cross-platform update exists. | Not qualified. |

Hosted CI validates the repository build matrix, not these missing product and
native capabilities. Track 19 remains active; Track 20/iOS remains locked behind
the approved sequence.
