# Review Report: Track 19 canonical JSON hardening

**Date:** 2026-10-24
**Scope:** Track 16's Swift preview parser and contract checks for the Track 19 schema-v1 prototype.
**Disposition:** Approved as a prototype hardening slice; Track 19 remains in progress.

## Verification checks

- **Plan compliance:** Partial. The parser now rejects duplicate keys, unknown
  fields and alternate JSON encodings; broader import/export/restore and product
  acceptance remain incomplete.
- **Style:** No dedicated Swift formatter is configured; the changed Swift source
  follows the existing file's formatting.
- **New tests:** Yes. The standalone contract harness checks duplicate root
  keys, unknown fields, accepted single line endings and repeated-line-ending
  rejection in addition to the shared golden.
- **Test results:** The Swift 6.4 source harness compiled with warnings treated
  as errors and passed against the shared fixture. SwiftPM/XCTest remains
  unavailable: `swift test` fails before source compilation with XCBuild's
  `Unknown error parsing property list`; direct `swiftc` test-target typecheck
  also cannot load XCTest from the installed Command Line Tools. No XCTest pass
  is claimed. The prior Android JVM suite remains valid because this slice
  changes only Swift/parser documentation.
- **Conductor validation:** Python 3.15.1 `scripts/verify_conductor.py` and
  `git diff --check` pass.

## Findings

The canonical byte comparison addresses Foundation's permissive duplicate-key
decoding without adding a second hand-written JSON parser. It allows the exact
producer format and its golden fixture's single final LF or CRLF. The corruption
checksum is validated before byte-form comparison but remains explicitly
non-authenticating. No finding blocks this bounded prototype slice.

## Remaining limits

No hostile-input fuzz campaign, public schema commitment, Android app export UI,
native file selection, restore authority, production receipts, EventKit writes,
packaging/upgrade, remote calendar or operational result is established. Track19
stays active; Track20/iOS remains behind its prerequisite gate.
