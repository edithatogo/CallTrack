# Kotlin pilot verification

Date: 2026-10-20. Track is active, not complete. Full M5 and M4 acceptance are not
inferred from this pilot.

## Baseline and scope

Five new Java migration contracts passed before any Kotlin/compiler changes with
31 existing contracts (36 total); baseline log is excluded at
`.local/kotlin-evidence/java-contract-baseline.log`. Golden source/event hashes
were independently captured by compiling/running the original Java classes with
JDK17 at f163e4c. Fixtures include surrogate-pair namespace, null/empty values,
Unicode whitespace, restricted identity, zero duration and extended call type.

Three production classes are translated; CallRecord and21 remaining production Java
files are retained. No SQLite schema/version, namespace, source signature version,
package/version, UI, permissions, scheduling or cloud integration changes.

## Integrity and tooling

ktfmt0.59 standalone artifact SHA256 was checked against Maven before formatting;
local dry-run passed. Google Java formatter passed for the new Java contracts.
Kotlin style guide and CI step are committed preparation, not an observed hosted run.

Host suite:20 passed, including a new regression that edits a Kotlin source after
creating a build receipt and verifies native acceptance is refused before any tool
or device access. Existing `run_slice_build.inputs()` already covers all app/src
files, so no hash-scope weakening or new alternate native path was necessary.

Full mixed-language build, graph readback and emulator regression results will be
recorded after completion. No personal call data or calendar access is required.
