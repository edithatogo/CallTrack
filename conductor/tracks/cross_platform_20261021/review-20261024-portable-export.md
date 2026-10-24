# Review Report: Track 19 prototype interchange slice

**Date:** 2026-10-24
**Scope:** Android JVM encoder and macOS Swift preview-parser contract, bounded to the synthetic schema-v1 interchange fixture.
**Disposition:** Approved as prototype-only evidence; Track 19 remains in progress.

## Review findings

- Android serializes only opaque deterministic row UUIDs, direction, timestamp,
  duration, export UUID, record count and corruption checksum. Contact name,
  phone number, account identity, raw source namespace/row ID and calendar
  receipts are not part of the format.
- Both implementations enforce 10,000 records and the parser/producer enforce
  the 5 MiB document boundary. The producer rejects unknown directions,
  malformed source IDs, duplicate row identities and time overflow. The parser
  rejects duplicate IDs, invalid/overflowing time, count mismatch and checksum
  mismatch.
- The checksum protects against accidental corruption only. It provides no
  authenticity, authorization or replay protection. Foreign IDs/exports do not
  become calendar receipts. Call timing and direction remain sensitive.
- No Android UI action or file-writing path invokes the encoder. The schema is
  not a public/stable app format, and the macOS parser remains preview-only.

## Validation

- `JAVA_HOME=/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home`
  with `ANDROID_HOME=/Users/doughnut/Documents/calltrack/.local/android-sdk`
  and isolated `GRADLE_USER_HOME=/Users/doughnut/Documents/calltrack/.local/gradle-track17`
  `./gradlew --no-daemon --console=plain :app:testDebugUnitTest` — **passed**.
- Swift 6.4 source harness compiled with
  `xcrun --sdk macosx swiftc -warnings-as-errors -target arm64-apple-macosx14.0`
  and ran against `app/src/test/resources/call-log-v1.json` — **passed**.
- `swift test` — **not run to test execution**; SwiftPM failed initializing
  XCBuild with `Unknown error parsing property list` before source compilation.
  The direct source harness does not establish XCTest or native application
  acceptance.
- `uv run --python 3.15.1 python scripts/verify_conductor.py` — **passed**:
  233 documents, 21 tracks, 25 synthetic scenarios.
- `git diff --check` — **passed**.

## Remaining gates

Track 19 remains in progress. SwiftPM/XCTest must be rerun when its local build
system is available. Define schema evolution and hostile/ambiguous JSON rules;
add fuzzing and restore semantics; integrate only after an explicit product
workflow is approved; qualify intentional file selection, packaged builds,
upgrades, native EventKit/provider readback, selected remote routes and recovery.
No KMP/Native parity or iOS readiness is claimed. Track 20 remains locked until
all prior stages are accepted or explicitly deferred.
