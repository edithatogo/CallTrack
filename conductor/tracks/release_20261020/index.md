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

## Current licence/tool-trust and schema/provider checkpoint — 2026-10-21

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

Two clean same-host builds match; production signing, independent-host
reproduction and original operational acceptance remain pending.

- [Current unsigned Kotlin candidate evidence](unsigned-kotlin-evidence-20261021.json)

## Local release continuation — 2026-10-21

- [Local qualification and remaining gates](local-qualification-20261021.md)
- [Current local review](review-local-qualification-20261021.md)
- [Fresh canonical runtime artifact audit](canonical-runtime-audit-20261021.json)
- [Inherited asset hash audit](assets-audit-20261021.json)
- [Unsigned and corresponding-source checks](local-artifact-checks-20261021.json)

- [Development-key increasing-version native evidence](development-upgrade-evidence-20261021.json)
