# Kotlin pilot verification

Started2026-10-20; final qualification recorded2026-10-21. Track is active, not complete. Full M5 and M4 acceptance are not
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

## Final bounded qualification

[Source-bound evidence](pilot-evidence.json) records the exact input hashes,
debug/test APK hashes, unsigned release hash, native methods, SDK and cleanup.
Implementation commit7cdd8d4; app input hashes still match after commit and review.

- `python3 scripts/run_slice_build.py --jdk <JDK17> --sdk .local/android-sdk`:
  first mixed build passed in307.5s; added native regression passed packaging/lint
  in54.4s; a Java formatter idempotence correction required the final source-bound
  build, which passed in43.4s. Final36 JVM tests pass; debug/app-test APKs build;
  lint has zero errors/fatal issues and30 existing warnings.
- `sh gradlew --no-daemon --console=plain :app:assembleRelease :app:dependencies
  --configuration releaseRuntimeClasspath buildEnvironment`: passed. Release APK
  is unsigned and minification remains disabled; its embedded Git revision matches
  7cdd8d4. No production-signing or R8
  shrinking acceptance is claimed.
- `python3 scripts/qualify_parity_emulator.py --serial emulator-5580`: five native
  methods passed on verified dedicated API36 emulator. Tests cover synthetic source
  cursor, schema1 preserving upgrade, committed outbox/payload clearing, transaction
  guard and Java-era golden receipt recognition after upgrade/reopen. The last
  scenario refuses reinsertion and preserves a human edit as REVIEW_REQUIRED.
  Both disposable packages were uninstalled and absence read back successfully.
- [Dependency graph](dependency-report.json):84 runtime graph modules, including
  stdlib/common2.3.21, jdk7/jdk8 alignment modules1.8.0 and existing app coroutines1.6.1.
  Build-tool coroutines1.9.0 belongs to the compiler graph, not the app. No SBOM or
  transitive licence audit is inferred from the report.
- `javap -public` confirmed original constructors, primitive/public final fields
  and Java static entry points; Java reflection/golden tests cover that contract.
- Final20 host tests pass, including refusal of Kotlin-stale receipts before any
  device operation. Source/graph/link, private-source and formatter checks pass.
  CI now includes unsigned release packaging and scans all Kotlin roots; no hosted
  CI execution is claimed.

No call/calendar permission was granted and no personal source or actual calendar
was read/written in this native run. The calendar sink in the upgrade test is a
synthetic port; SQLite is native. This is not a real app-package version update,
provider/calendar persistence test, physical call or remote account acceptance.
Remaining21 Java files and M4/full-M5 upgrade/recovery gates stay open. Restoring
Java code/build settings is a development code rollback only; no installed-user
app downgrade or data reset was performed.

## Active-code continuation — 2026-10-21

The dated pilot above remains historical. Current33 active conversions, one Java
exception and removal audit are documented in [handoff](active-migration-handoff.md),
[evidence](active-migration-evidence.json) and [review](review-active-migration-20261021.md).
115 JVM/89 host tests pass. Five native suites (56 invocations), standalone import
UI (five) and private update (two) pass separately; the final matrix is not accepted
because lifecycle/provider/startup ANRs block remaining qualification. Two clean
unsigned builds pass115 tests each and match byte-for-byte on this host/cache.
Manual recovery proves fixture/package absence and recorded environment restoration;
phone service remains unavailable. Full M4/M5 and later frameworks stay pending.
