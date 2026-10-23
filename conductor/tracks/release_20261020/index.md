# Community distribution and release readiness

- [Specification](spec.md)
- [Plan](plan.md)
- [Metadata](metadata.json)

- [Findings](findings.md)
- [Mapped requirements](requirements.md)
- [Decisions](decisions.md)
- [Verification and remaining gates](verification.md)
- [Review](review.md)
- [Hosted CI refresh review](review-hosted-ci-refresh-20261024.md)
- [Preparation evidence](preparation-evidence.json)
- [Resolved dependency report](dependency-report.json)

## Live gate refresh — 2026-10-24

[edithatogo/CallTrack](https://github.com/edithatogo/CallTrack) is public. The
exact unsigned candidate `15998ba` was rebuilt by the
hosted [cold Linux release reproduction](hosted-reproduction-evidence-20261024.json):
the run passed, uploaded its evidence, and produced an APK matching the recorded
macOS SHA-256. This clears the Actions billing/runner-access blocker and qualifies
hosted reproduction for that candidate. The newer mainline includes Track14 app
changes, so the release artifact must be refrozen and requalified before it can
represent the current app. Hosted proof does not qualify a physical host or any
device, signing, calendar, source-conveyance, or operational gate.

Track5 remains not release-ready. The development identity remains selected for
local qualification; production identity and key custody remain pending. The
latest read-only device check found no connected Android phone, so physical-call
and any selected Google/Outlook route acceptance remain pending. Observation
state remains unstarted: dates null, zero accepted days/hours. A physical phone,
participating second phone, selected calendar route(s), and explicit usage scope
are still required before starting that window.

## Independent Linux build checkpoint — 2026-10-22

- [Current independent-build qualification](linux-reproduction-qualification-20261022.md)
- [Cross-host/source/artifact evidence](linux-reproduction-evidence-20261022.json)
- [Current review](review-linux-reproduction-20261022.md)

The9464408 candidate reproduces exactly on a separate native Linux host with no
Mac tool/cache mounts and fresh SDK/Gradle downloads.115 JVM tests per Mac/Linux
build and131 Python tests on both systems pass. The cold run exposed three missing
metadata checksums;739 entries are now enforced without changing old hashes or
runtime dependencies. The source/notices bundle is refreshed. At this dated
checkpoint, hosted GitHub CI was prepared but blocked by the account
billing/spending limit before runner assignment; the blocker was later cleared
as recorded in the live refresh above. Original full-M4 gates stay open.

## Earlier asset/source/notices checkpoint — 2026-10-22

- [Current qualification](assets-source-qualification-20261022.md)
- [Source/artifact evidence](assets-source-evidence-20261022.json)
- [Current review](review-assets-source-20261022.md)

Uncertain inherited artwork is replaced, active translations preserved, and an
exact-source/published-dependency/licence/notices candidate prepared. Two unsigned
5a3a3e4 builds and the source-only archive rebuild pass115 JVM tests each, with
zero lint errors.126 host tests pass. Final source conveyance, signing and the
original independent/native/live/operational gates remain pending.

## Isolated JDK checkpoint — 2026-10-22

- [Isolated JDK qualification](isolated-jdk-qualification-20261022.md)
- [Evidence](isolated-jdk-evidence-20261022.json)
- [Review](review-isolated-jdk-20261022.md)

The isolated vendor-signed JDK resolves the resource-seal blocker for its explicit
build path. The Homebrew installation remains unchanged and its known defect
remains recorded. Original production and independent/live acceptance stay open.

## Earlier licence/tool-trust and schema/provider checkpoint — 2026-10-21

- [Current qualification and remaining gates](licence-tool-upgrade-qualification-20261021.md)
- [Current review](review-licence-tool-upgrade-20261021.md)
- [Expanded canonical build-tool audit](canonical-build-tool-audit-20261021.json)
- [Supplemental source licence declarations](supplemental-tool-licences-20261021.json)
- [SDK/wrapper/Gradle/JDK origin evidence](toolchain-origin-evidence-20261021.json)
- [Enforced verification and real fault tests](dependency-verification-evidence-20261021.json)
- [Schema/provider upgrade evidence](schema-provider-upgrade-evidence-20261021.json)
- [Current trusted unsigned candidate](trusted-unsigned-evidence-20261021.json)

736 trust entries are enforced;239 unique build/runtime/test artifacts match.
15 native schema/provider invocations pass; two clean unsigned builds at5578bf5
match, each with115 JVM tests.121 host tests pass. Full M4 remains open, including
the explicit JDK bundle-resource-seal limitation and original production gates.

## Earlier unsigned checkpoint — 2026-10-21

- [Unsigned qualification evidence](unsigned-evidence.json)
- [Runtime artifact/POM inventory](runtime-artifacts-20261021.json)
- [Resumed review and fixes](review-20261021.md)

At the initial checkpoint two same-host builds matched. Later independent-host
qualification is linked below; production signing and operational acceptance remain pending.

- [Current unsigned Kotlin candidate evidence](unsigned-kotlin-evidence-20261021.json)

## Local release continuation — 2026-10-21

- [Local qualification and remaining gates](local-qualification-20261021.md)
- [Current local review](review-local-qualification-20261021.md)
- [Fresh canonical runtime artifact audit](canonical-runtime-audit-20261021.json)
- [Inherited asset hash audit](assets-audit-20261021.json)
- [Unsigned and corresponding-source checks](local-artifact-checks-20261021.json)

- [Development-key increasing-version native evidence](development-upgrade-evidence-20261021.json)


## Extended emulator checkpoint — 2026-10-22

[Minimum/intermediate acceptance](emulator-matrix-qualification-20261022.md),
[machine evidence](emulator-matrix-evidence-20261022.json) and
[review](review-emulator-matrix-20261022.md) qualify109 native invocations each on
API26/API33 plus11 affected API36 checks, with two deliberate terminations kept
separate.115 JVM/141 host tests and zero lint errors pass. Fixes add explicit
Closeable support for old SQLite helpers and48dp control minima. Real native
reboot/Doze, permission loss, provider readback, imports, simulated missed-call
logging and development10→11 preserving upgrades pass. Fixture root was confined
to the owned API26 userdebug guest, restored and both new guests stopped.

The current app fixes require a new release freeze/source bundle and independent
reproduction;9464408 remains the previously qualified unsigned artifact. Physical/
OEM/manual speech/contrast, cloud, production-signing and actual observation gates
remain. Track5 is active.

- [First-release installation and supported-upgrade baseline](first-release-baseline.md)


## First-release baseline and final local candidate —2026-10-22

[Candidate qualification](first-release-candidate-qualification-20261022.md),
[machine evidence](first-release-candidate-evidence-20261022.json) and
[review/fixes](review-first-release-candidate-20261022.md) qualify15998ba: two Mac
builds and a separate fresh Linux build match;115 JVM/144 host tests pass, as do60
native fresh/upgrade/rendered invocations across26/33/36 and6 expected install
rejections. Source/notices are refreshed; fixtures/packages and new guests are
cleaned up. First public release is fresh-install-first, with no issued predecessor.
Permanent identity/key custody, production signing, physical/cloud/operational,
hosted/source/support/channel gates remain. Track5 stays active.

- [Physical calls and account-calendar qualification procedure](physical-calendar-qualification.md) — prepared; selected physical device, participant and remote routes pending.
- [Physical-route preparation review](review-physical-preparation-20261022.md) — prepared scope only; actual runtime acceptance remains pending.

- [Seven-day operational observation](seven-day-observation.md) — prepared; start not set and zero accepted days.
- [Observation preparation review](review-observation-preparation-20261022.md) — final runtime review remains pending actual seven-day evidence.
