# Community distribution and release readiness

- [Specification](spec.md)
- [Plan](plan.md)
- [Metadata](metadata.json)

- [Findings](findings.md)
- [Mapped requirements](requirements.md)
- [Decisions](decisions.md)
- [Verification and remaining gates](verification.md)
- [Review](review.md)
- [Preparation evidence](preparation-evidence.json)
- [Resolved dependency report](dependency-report.json)

## Independent Linux build checkpoint — 2026-10-22

- [Current independent-build qualification](linux-reproduction-qualification-20261022.md)
- [Cross-host/source/artifact evidence](linux-reproduction-evidence-20261022.json)
- [Current review](review-linux-reproduction-20261022.md)

The9464408 candidate reproduces exactly on a separate native Linux host with no
Mac tool/cache mounts and fresh SDK/Gradle downloads.115 JVM tests per Mac/Linux
build and131 Python tests on both systems pass. The cold run exposed three missing
metadata checksums;739 entries are now enforced without changing old hashes or
runtime dependencies. The source/notices bundle is refreshed. Hosted GitHub CI
is prepared but awaits the repository choice; original full-M4 gates stay open.

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
