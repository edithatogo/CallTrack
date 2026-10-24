# Verification and readiness boundary

## Current unsigned candidate — 2026-10-24

See [candidate qualification](current-candidate-qualification-20261024.md),
[candidate evidence](candidate-build-evidence-20261024.json),
[canonical runtime audit](canonical-runtime-audit-20261024.json) and
[review](review-current-candidate-20261024.md). Source revision
`ae39ef9e109158191b33d7047e93bc3ae39a8fbd` produced the same unsigned APK in
two clean same-host builds and hosted cold-Linux run 40081001647. Each reports
119 JVM tests, zero failures/errors/skips and zero lint Error/Fatal findings.
The refreshed runtime audit covers92 artifacts; source/notices packaging covers
all92 coordinates and passed full archive readback/repeatability.

The hosted workflow does not prove independent native physical-host acceptance.
The archive has not been published; licence/legal clearance, dependency-from-source
rebuild, production signing/key custody, physical calls, selected calendar routes
and seven-day operational observation remain pending. `release_ready=false`.

## Current trust and schema/provider continuation — 2026-10-21

See [current qualification](licence-tool-upgrade-qualification-20261021.md) and
[current review](review-licence-tool-upgrade-20261021.md). Native fixture scope
passes15 invocations with provider/package/key cleanup; source-pinned ledger1–5→6
and import1→2 are qualified on the dedicated emulator.736 independently compared
binary/POM/module trust entries now fail closed in ordinary builds, with actual
wrong/missing-entry negative tests.239 unique artifact binaries match repositories.
The toolchain's payload-origin checks pass with an explicit unqualified JDK bundle
resource seal.121 host tests and current document verification pass.

Two fresh clean5578bf5 unsigned builds match,115 JVM tests each,zero lint errors,
69 warnings/two hints. Production signing, independent/hosted reproduction, complete
source/asset conveyance, supported real release baselines, physical/cloud and
operational acceptance remain pending. No full-M4/archive claim follows.

## Earlier local continuation — 2026-10-21

See [local qualification handoff](local-qualification-20261021.md),
[repository audit](canonical-runtime-audit-20261021.json),
[asset hash audit](assets-audit-20261021.json) and
[artifact/source checks](local-artifact-checks-20261021.json).
The current unsigned Kotlin candidate is787ae35, not the historical6b353f4 below.
Two same-host builds match with115 JVM tests each and zero lint errors; their exact
APKs are rechecked unsigned. The [new native upgrade/rejection evidence](development-upgrade-evidence-20261021.json)
passes four instrumentation invocations with development signing, two controlled
rejections, preserved private state and verified cleanup;105 host tests pass. Full D01–D09 and release_ready=false remain.

## Historical unsigned checkpoint — 2026-10-21

Unsigned development qualification and review pass: see [current review](review-20261021.md),
[source/artifact evidence](unsigned-evidence.json) and [runtime inventory](runtime-artifacts-20261021.json).
Two clean checkouts at6b353f4 produced identical unsigned APKs in230.9s total,
110 release JVM tests each andzero lint errors (32 warnings,5 hints).78 host tests
pass. The user selected retaining development identity and leaving production
signing pending. No runtime dependency or app change, native release acceptance,
publication or key generation occurred.89 runtime artifact hashes/POM declarations
are inventoried; per-artifact trust/licence/asset clearance remains open.

The sections below are the retained preparation checkpoint, not current M3
status. Track4 is now archived for selected-emulator acceptance. The original
D01–D09 release gates stay open where not actually observed.

## Preparation history — 2026-10-20

Date: 2026-10-20. This track implements community preparation documents/templates,
not app behaviour. No new runtime or build plugin dependency is added. No APK was
installed, permission granted, call made, account registered or artifact uploaded.

## Current readback

- Track4 metadata/plan confirm unfinished automatic scheduling, paging/import and
  full recovery; its bounded store-foundation evidence cannot satisfy M3/M4.
- Root package/version/build/manifest, SQLite schema2, manual launcher path,
  preferences, backup exclusions, legacy code and source provenance inspected.
- No Git remote configured. Hosted CI, public repository and reporting route absent.
- Native Gradle releaseRuntimeClasspath report completed successfully in24s.
  [Recorded requested/selected modules](dependency-report.json) carry the private
  report SHA256. This is a graph inventory, not a per-artifact licence or binary
  contents audit. It includes existing Kotlin/coroutines transitives despite Java
  app-owned source.
- Official dated channel/signing/dependency sources are cited in findings/decisions.
  Versioned Gradle8.13 documentation could not be opened with the web tool; current
  docs were used for concepts only, not untested configuration changes.

## Required before release acceptance

D01–D09 in requirements remain explicit gates. No signed release candidate,
independent bitwise reproduction, upgrade/rollback execution, provider-cloud
acceptance, physical-call acceptance or seven-day observation is claimed. User
chose emulator tests and left physical-call acceptance pending. Stable identity/key
custody and a reporting channel are genuine future choices, not guessed settings.

## Preparation verification results

[Evidence summary](preparation-evidence.json) binds the new successful build receipt
and artifact hashes. Current app inputs were checked against that receipt after
building: unchanged. Build/test APKs and lint succeeded in101.7s; 31 JVM contracts
passed with zero failures/errors. Lint has zero errors/fatal issues; warning count
is recorded in the summary. No native test ran in Track5 and this build does not
replace the earlier Track4 native receipt.

`python3 -m unittest discover -s tests -v`: 19 passed. The first run exposed the
verifier test's fixed root-document copy list; it omitted newly linked release
files. The fixture now copies root Markdown documents and retains LICENSE. A
follow-up run caught a stale variable in that correction; it was fixed before the
final passing run. No verifier checks were relaxed. Fault-injection tests print
expected failure receipts while the unittest suite itself passes.

`python3 scripts/verify_conductor.py --private-sources`: passed; 13-track graph,
25 baseline synthetic scenarios, document links and retained private input hashes.
A separate Markdown link pass covered root/community/GitHub documents as well as
Conductor. `git diff --check` passed. No external hosted CI or public reporting
configuration was tested.

The track stays active until its original release acceptance is satisfied;
preparation alone is not grounds for completion or archive.
