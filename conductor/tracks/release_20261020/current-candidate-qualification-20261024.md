# Current unsigned candidate qualification — 2026-10-24

## Frozen candidate

Candidate source revision: `ae39ef9e109158191b33d7047e93bc3ae39a8fbd`.

The unsigned development APK is `org.calltrack.community.dev`, version
`0.1.0-dev` (version code 10), SHA-256
`22e93744536a08ce8b94050c412e6bd3d5a6839a040d0fd513f25658af961180`.
Two clean same-host checkouts produced byte-identical APKs. Both passed 119
release JVM tests (zero failures, errors or skips). Each release lint report has
zero Error/Fatal findings, 26 Warnings and 3 Hints. The selected local toolchain
was OpenJDK 17.0.21.0.2, Android platform 36 and Build Tools 35.0.0. These builds
shared the dependency cache and therefore do not establish independent-host
reproduction.

The exact commit was also built by the hosted [cold Linux workflow run
40081001647](https://github.com/edithatogo/CallTrack/actions/runs/40081001647).
The runner used a fresh SDK and Gradle home, passed all 119 release JVM tests,
reported zero lint Error/Fatal findings, and produced the same APK SHA-256. This
qualifies the hosted Linux reproduction for this source revision. It does not
claim a separate native physical Linux host; the workflow receipt retains
`physical_host_independence=false`.

## Dependency and corresponding-source evidence

The runtime inventory contains 96 resolved rows and 92 unique artifacts. All 92
artifact bytes match their canonical Google Maven or Maven Central publication,
and all 92 POMs resolve to at least one licence declaration. This is repository
integrity and declaration evidence, not legal clearance or trusted build-tool
provenance.

A deterministic 877-file source/licence/notices archive was generated and read
back twice. It binds the source snapshot to the candidate commit, includes
checksum-verified published source and POM archives for all 92 runtime
coordinates, and retains the app's required licence/notices payload. Eighty-seven
source archives contain Java/Kotlin sources; five are valid metadata-only source
archives and are explicitly identified in the manifest. The archive SHA-256 is
`f63334fd2002a01ee18eb93b7242057b007eecf6c0ddba7f954350f866733b38`.
The archive remains a local qualification artifact; it has not been published,
and published dependency sources do not prove binary-from-source reproduction.

## Fixes made

The first current-candidate Linux attempt exposed that the inventory task tried
to select an Android artifact variant for the in-repository KMP `shared-core`
project. Inventory now filters to external Maven components; first-party code is
already included in the source snapshot. The refreshed audit now covers the
Compose/KMP runtime graph.

The next attempt completed the Linux build but found that the runner still
expected the previous candidate's 115 JVM tests. The current frozen candidate
has 119; the acceptance check now requires 119 with no failures, errors or
skips, and a focused parser test covers aggregation of all JUnit result files.
The successful hosted run used the updated check. The candidate APK hash also
includes the exact Git revision in Android version-control metadata, so every
candidate is compared against its own frozen revision.

The implementation and evidence changes are in [PR #73](https://github.com/edithatogo/CallTrack/pull/73).
The separate PR checks passed on the code commit; final PR checks are rerun after
this evidence update.

## Remaining release gates

Track5 remains not release-ready. Production application identity and signing-key
custody remain undecided; no production signing or publication occurred. The
emulator is offline and no physical phone is connected, so physical-call and
selected Google/Outlook route acceptance remain pending. The user has not selected
usage/calendar scope or participant readiness; seven-day observation remains
unstarted with null dates and zero accepted days/hours. Existing device,
calendar, upgrade, support and observation evidence is not promoted to this new
candidate where the source revision differs. These gates require their actual
authorised environment and participation.

Detailed machine-readable results: [candidate build evidence](candidate-build-evidence-20261024.json),
[canonical runtime audit](canonical-runtime-audit-20261024.json), and
[`runtime-sources.json`](../../../qualification/release/runtime-sources.json).
