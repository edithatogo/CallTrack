# Independent Linux reproduction — 2026-10-22

The corrected development candidate reproduces byte-for-byte on a separate native
x86-64 Linux computer with freshly downloaded tools/dependencies. This satisfies
the independent-environment build comparison; signing, hosted CI and the remaining
original Track5 release gates are separate. See [evidence](linux-reproduction-evidence-20261022.json)
and [review](review-linux-reproduction-20261022.md).

## Qualified builds

Candidate: `94644086159137bab27767b6a1a46727409695b5`.
Unsigned APK SHA256 on both macOS checkouts and the separate Linux host:
`0f672916f06afbc2ec4b0730c1df167d44dcb094b41385cb8a0bfc6b234f48df`.

| Environment | Isolation and observed result |
|---|---|
| macOS ARM64 | Two clean source checkouts; qualified isolated Temurin17.0.20.1+1, existing Mac SDK/cache;115 JVM tests each, zero lint Error/Fatal findings, identical unsigned APKs |
| Separate native Linux x86-64 host | Existing trusted SSH identity; native Intel machine, virtualization probe `none`; independently pulled JDK image absent before the run; no host mounts, privileged mode, published ports, Mac SDK or dependency caches; two-CPU/5GiB disposable container; fresh SDK and Gradle home;115 JVM tests, zero lint Error/Fatal findings; exact APK match |
| Linux tooling checks | A separate repository-only tooling checkout on that Linux host passes all131 Python tests and Conductor verification; no private reference APK/checkouts or Mac dependencies required |

Linux release build/lint/resolution takes590.9s including SDK preparation. Each
build reports23 lint warnings and2 hints. These are115 shared JVM fixtures per
build, not separate unique test populations. Raw Linux XML reports were exported
and reparsed on macOS to verify counts independently of the runner summary.
The runtime graph remains68 rows/63 unique audited artifacts.

Only the exact source commit/tree, runner and public SDK metadata were transferred
for the cold app build. Source history was added **after** that build succeeded,
solely for the tooling suite's historical-schema fixture checks. No compiled Mac
APK, SDK, JDK or Gradle cache was transferred. The Linux JDK image was pulled by
that machine from the official registry. Three Google SDK archives and the pinned
Gradle distribution/Maven dependencies were downloaded inside its fresh container.

Source is unchanged before/after; all500 tracked files remain bound to the exact
commit. The default `/root/.gradle` stays absent; the runner uses its new dedicated
Gradle home. Readback confirms no host mounts. Existing host services/files were
not targets of commands. Only owned test containers are stopped, with receipts and
writable layers retained for inspection. No computer restart occurred.

The generic runner deliberately makes no physical-host attestation itself. That
classification comes from the host-side trusted SSH/hardware/kernel and Docker
readback, not an environment variable or a copied JSON assertion.

## Cold resolution exposed and fixed a real gap

The initial cold5a3a3e4 guest correctly failed strict verification for three missing
metadata checksums: Guava parent33.3.1-jre POM, JUnit BOM5.10.2 module metadata and
coroutines BOM1.8.0 POM. Each actual failed guest download matches a separately
fetched canonical Maven Central file. Add only those exact SHA256 entries; all736
previous entries/hashes remain unchanged, giving739 total. No binary version,
runtime dependency, metadata verification setting or broad trust exception changes.

The failed receipt is retained. A subsequent dirty-tree/warm-cache diagnostic was
stopped after metadata admission and is not acceptance evidence. An emulated
same-hardware build was superseded by the separate native host and stopped without
a completion claim. Their logs remain separate from the qualified source-bound run.

The virtualized diagnostic also exposed a native file-watcher exception. The
single-use Linux recipe uses Gradle8.13's supported `--no-watch-fs`; exact source
hash/status checks still protect input integrity. Neither artifact verification
nor tests are disabled. The separate-host final run has no watcher exception.

## Tool and CI preparation

The JDK image is pinned to
`eclipse-temurin@sha256:67d52fac68c483f8a91dda88d4d0518ca2375d4c8260a9114d37aa49cf11cfac`,
Temurin17.0.20.1+1/Jammy/amd64. Official image provenance and digest-addressed
components are retained; an independent publisher-signature claim is not made.
[Temurin image documentation](https://hub.docker.com/_/eclipse-temurin) describes
its maintained official-image origin. Google SDK sizes/published checksums and
reviewed SHA256 pins are in
[Linux SDK inputs](../../../qualification/release/linux-sdk-inputs.json).
The existing accepted SDK agreement and fresh repository text differ only in
whitespace/line wrapping; no new licence acceptance command or global tool change
occurs. SDK/JDK distributions are not redistributed with the source bundle.

The manual [GitHub workflow](../../../.github/workflows/release-reproduction.yml)
uses separate tooling/candidate checkouts, the pinned image, no cache restore,
read-only repository permission and no persisted checkout credentials. Node24
checkout/upload actions are pinned to freshly verified official release commits;
`actionlint` passes. Linux execution of its build/tooling commands is qualified.
GitHub checkout, provider execution and artifact-upload steps have not been run:
there is no configured remote/repository, and the user's private-repository choice
remains pending. Do not claim a saved workflow as a hosted job or public release.

## Refreshed source candidate and boundaries

The650-member exact-source/licence/notices archive is prepared and fully read back:
`f0e7b1a16bd7d4b2a76414808821ad3a23f581e444d54edad3e219d8cc91b11b`.
It contains the500-file Git snapshot plus the existing63 published dependency
source/POM archives and notices; two archive writes match. All legal payloads
match both Mac APKs, and the independent Linux APK is byte-identical to those.
The inherited artwork replacement and published-source scope remain as recorded
in the [asset/source qualification](assets-source-qualification-20261022.md).
Full upstream dependency rebuilding and final signed/channel source conveyance
are not qualified by this preparation.

Relative to5a3a3e4, only `META-INF/version-control-info.textproto` differs in the APK;
application/runtime payloads are identical. Git metadata is retained, not stripped
or fabricated. Previous native receipts keep their original source/APK binding;
no new Android native/physical/cloud acceptance is asserted. This is an Android
build on Linux, not a Linux companion app or framework adoption.

Production identity/key custody/signing, Android support/OEM/manual accessibility,
physical calls, cloud routes where claimed, hosted CI readback and
seven actual observation days retain their original gates. Track5 remains active.
Raw receipts, exports and source bundle are excluded under
`.local/release-evidence/linux-independent-20261022/`.
