# Review: Track 17 desktop dependency metadata audit

Date: 2026-10-24
Scope: Resolved `desktopRuntimeClasspath` and `desktopTestRuntimeClasspath` for
the prototype at source revision `21e332527480320d6f6778aac1f427df67b511ca`.

## Result

The export task and audit covered 108 configuration entries and 63 unique
artifacts: 45 runtime and 18 test-only. All 63 artifact hashes matched fresh
canonical HTTPS repository downloads. Every artifact POM had an effective
licence declaration after bounded parent-POM resolution. The runtime set
declared Apache-2.0 throughout. The test-only set included EPL-1.0, BSD,
MIT, and GPL-2.0 with the Classpath Exception. Thirty-seven embedded
licence/notice files were identified and fingerprinted.

The complete per-artifact coordinates, local hashes, upstream hashes, POM
licences, parent-POM evidence, embedded notice paths and runtime/test scopes
are in [dependency-runtime-audit.json](dependency-runtime-audit.json). The
inventory SHA-256 is
`a31124682f283cc1b2367b4b8cac0dd3b522a22648c505d06ac8db19bad44d20`.

No implementation defect was found in the inventory exporter or audit
receipt. This does not establish legal clearance, vulnerability status,
build-tool trust, complete notice-text delivery, or suitability for an Android
or native macOS package. Production adoption remains false and Track17 remains
in progress.

## Verification

- `desktopTest` on JDK17.0.21.0.2+0, Gradle8.13 and the isolated existing
  Gradle home passed all 7 tests (3 shared reducer and 4 rendered desktop).
- The checked-in `scripts/export_track17_inventory.gradle` resolved both named
  configurations offline and produced 108 configuration rows; its inventory
  hash matches the captured audit input.
- `scripts/audit_release_artifacts.py` completed without artifact/POM errors;
  all 63 upstream artifact bytes matched and no effective licence was missing.
- `tests/test_release_audit.py`: 8 passed; `tests/test_verify_conductor.py`:
  19 passed; `scripts/verify_conductor.py`: 231 documents, 21 tracks, and 25
  scenarios passed.
- `git diff --check` passed.

## Remaining limits

The Gradle8.13/Kotlin2.4.21 forward-compatibility warning remains. Runtime and
test JARs were inspected, but Gradle plugins, compiler/build toolchains,
security advisories, extracted notice texts and native/Android package graphs
were outside this bounded audit. Complete those before any distribution or
production adoption. Android target rendering and native macOS packaging still
require their respective SDK/Xcode environments.
