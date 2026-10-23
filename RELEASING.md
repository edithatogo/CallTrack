# Release qualification runbook

Status: development only. Track5 has prepared this protocol; M4 has not passed.
No release candidate, production key, public repository or supported release is
established. Track4 parity is the prerequisite. Public release is outside this
track's scope; use a separately authorised execution after these gates pass.

## Candidate and source evidence

1. Complete Track4 and review its exact acceptance evidence. Record every open
   limitation; physical-call acceptance is still pending by user choice. Emulator
   evidence cannot establish OEM telephony or cloud-account behaviour.
2. Select the permanent application ID, name/icon, initial version and distribution
   route. The current `org.calltrack.community.dev`, versionCode10/0.1.0-dev is a
   disposable development identity. Do not overwrite the old CallTrack installation
   or claim its signing/branding rights. Audit inherited assets/translations.
3. Freeze a clean Git commit. Build from a fresh checkout with JDK17, Gradle8.13,
   AGP8.13.2, Kotlin/KGP2.3.21, compile/target36 and build-tools35.0.0, or explicitly qualify any
   changes. Follow CONTRIBUTING.md and preserve all test/build evidence. No private
   reference checkout or APK may be required for the root build.
4. Capture the actual releaseRuntimeClasspath graph and all artifact licences and
   notices. Audit repositories, resolved versions and plugin/tooling dependencies
   as well as direct declarations. Review dependency locks and verification
   checksums against trusted upstream provenance before enabling them. Do not
   treat auto-generated checksums as independent trust or a dependency report as
   an SBOM. Retained legacy code remains part of the shipped artifact audit.
5. Build the unsigned release variant on two clean isolated environments from
   that source. Compare artifacts; investigate nondeterminism and document tools,
   OS, dependency graph and hashes. Two clean same-host builds of the current
   Kotlin app match; independent-environment reproduction remains pending. If claiming F-Droid
   upstream-signature reproducibility, test its exact recipe/comparator and the
   build-tools35 signing compatibility issue identified in Track5 findings.
6. Keep source archive/commit, build scripts/configuration, toolchain manifest,
   resolved artifact checksums/licences, unsigned and signed APK hashes, application
   ID/version/SDK, signer SHA256, merged manifest, test results and review together.
   Source-bound debug receipts are useful inputs, not a production release receipt.
   Publish corresponding source and licence material with the chosen conveyance
   route when distribution is authorised; exclude credentials and private evidence.

## Unsigned same-host qualification

After committing the source, run:

```sh
python3 scripts/qualify_release_build.py --output .local/release-evidence/UNIQUE-RUN \
  --jdk "$JAVA_HOME" --sdk "$ANDROID_HOME"
```

The output directory must not exist. The runner refuses dirty source, clones the
exact commit twice without local hardlinks, builds/tests/lints the release variant,
records raw dependency reports and APK badging, verifies the expected unsigned
signature result with apksigner, and compares unsigned APK SHA256.
It does not sign, install, upload, or use private reference inputs. Failure leaves
a failed receipt; keep it and use a fresh output directory after investigating.
Both checkouts share the host and dependency cache. A byte match is same-host
repeatability only, not independent-environment reproduction, checksum trust,
licence clearance, upgrade acceptance or production readiness. The current
development identity remains unchanged.

## Local development upgrade probe

```sh
python3 scripts/qualify_release_upgrade.py --serial emulator-5582 \
  --output "$PWD/.local/release-evidence/UNIQUE-UPGRADE-RUN"
```

Only the exact dedicated emulator is accepted; both development packages must be
absent. Requires the existing debug key and successful source-matched debug/test
receipt. A clean committed-source checkout uses a recorded temporary Gradle
`finalizeDsl` overlay to build release mode at versionCode11 with that existing
key. Committed app configuration stays versionCode10 and unsigned for release.
The probe seeds synthetic private state at10, force-stops its app, upgrades in
place to11, reads back state, then verifies downgrade and wrong-certificate
rejections followed by unchanged state. The latter uses a one-day disposable
fixture key removed on exit; it is never a production signing key. Cleanup checks
both package removals. No phone permissions or provider writes occur.

This covers paused settings, source namespace and READY/IN_FLIGHT/REVIEW/CONFIRMED
receipts in schema6. It does not cover old schema1/2 production upgrades, human-edited
provider events across upgrades, recovery execution after upgrade, uninstall/restore,
permanent identity/key custody, physical devices or cloud synchronization. Use the
full matrix below before supported release acceptance.

## Local runtime repository and asset audit

```sh
python3 scripts/audit_release_artifacts.py \
  --inventory conductor/tracks/kotlin_20261020/runtime-artifacts-20261021.json \
  --output .local/release-evidence/UNIQUE-RUNTIME-AUDIT
```

The output must be new. The audit downloads each unique runtime artifact and POM
from canonical Google Maven/Maven Central HTTPS endpoints, resolves renamed files
through hash-bound published module metadata, compares bytes, and hashes embedded
notices (including classes.jar). A mismatch/error retains the receipt and exits
nonzero. Repository agreement is not independently signed provenance, verification
of build plugins/SDK/tools, a complete SBOM or legal clearance. Current results and
remaining licence/asset gates are in the Track5 local qualification handoff.

## Qualified local trust and schema/provider upgrade continuation

Ordinary builds now use the reviewed `gradle/verification-metadata.xml` allow-list,
with binary and repository-metadata verification enabled. The policy is included
in source-input receipts. See the [trust procedure](qualification/release/trust/README.md)
and Track5 continuation evidence for canonical downloads and positive/negative
Gradle tests. Independent-host/hosted execution and publisher signatures remain
separate claims. The SDK/JDK comparison checks payloads against official archives;
one JDK bundle resource-seal defect remains recorded, without changing global tools.

Add `--provider-schema` to the development upgrade probe to include source-pinned
ledger1–5→6 and import1→2 fixtures, human-edited/deleted LOCAL events, ambiguous
insert recovery, permission loss, explicit resume, migration fault rollback and
newer-schema preservation. Only Java/assets/schema-provenance fixture inputs are
bound, so independent licence audit work does not invalidate a native build. The
fixtures are synthetic historical database shapes, not published old APKs or real
customer data. All test permissions and provider effects belong to disposable
packages on the dedicated emulator; cleanup is mandatory.

## Signing and channels

Choose key custodian, offline recovery copy/access procedure and channel policy
before creating the first production key. Record public certificate fingerprints
in release metadata; never put private keys/passwords in Git, command arguments,
logs or issue attachments. Keep CI unsigned until key custody and protected signing
are deliberately configured. Distinguish Play upload key from app signing key.
Prefer a consistent signing identity across intended update routes; separately
signed channels require an explicit migration strategy rather than assumed updates.

Verify each signed candidate using the pinned tool:

```sh
"$ANDROID_HOME/build-tools/35.0.0/apksigner" verify --verbose --print-certs CANDIDATE.apk
```

Compare the observed certificate against the recorded intended identity and inspect
package/version with the SDK tooling. The command checks signatures, not functional
readiness. Do not publish debug-signed output. Do not rotate keys without a tested
OS/channel compatibility plan. Android's [apksigner reference](https://developer.android.com/tools/apksigner)
describes verification and signing options.

Before choosing a channel, refresh the dated policy findings. Play permission
review, F-Droid inclusion/build review and Android developer verification are
separate gates; none is established by local installation. Do not submit forms,
register identities or upload binaries merely to test a hypothesis.

## Upgrade, failure and recovery matrix

Use dedicated synthetic calendars/data and the exact previous/candidate signed
versions. Preserve the working installation and a recovery route until acceptance.

| Scenario | Required observed result |
|---|---|
| Same identity/key, increasing version | Existing selected destination, namespace and receipts survive; no reimport/duplicate |
| Schema1 and schema2 fixture upgrade | Receipts preserved; changed/missing user events never overwritten/recreated automatically |
| Pending insert at upgrade/process death | At most one event or explicit review; durable attempt evidence survives |
| Paused/cancelled operation | Candidate does not resume it without explicit user action |
| Permission revoked / destination lost | Visible recoverable state; no fallback calendar or hidden writes |
| Wrong certificate / incompatible older schema | Controlled rejection or documented forward recovery; no data wipe |
| Uninstall / reinstall / device transfer | Document namespace/ledger loss and surviving events; no claim of safe automatic restore |

Do not recommend blind APK downgrade or uninstall/reinstall as rollback. Prefer a
forward fix retaining the ledger. Any supported rollback must prove schema and
signing compatibility on a synthetic copied fixture. Portable encrypted backup and
cross-device identity migration are not implemented and cannot be used as assumed
recovery. Calendar cleanup must target known fixture events only and preserve edits.

## Support matrix and alpha observation

The declared floor is API26, not tested support. Before advertising a supported
range, qualify the minimum, an intermediate version and the current target on
emulators, then representative real devices/OEM restrictions. Record exact source,
OS/API, device category, calendar/provider category, native results, cleanup and
limits. Test LOCAL and account-backed calendars separately; verify the actual
remote account if cloud sync is claimed. Avoid publishing device serials/accounts.

After the controlled suite passes, observe seven actual days on selected authorised
real devices. This is a proposed stable-promotion gate, not time already elapsed.
Use synthetic or user-controlled calls only when authorised. Record expected/seen
call counts by direction, event count, duplicate/miss/review count, prompt/recovery
latency distribution and maximum, permissions, reboot/Doze/offline/force-stop
recovery, battery context and explicit unsampled cases. “Prompt when possible;
recover later if delayed” is the user preference, not a guaranteed deadline.

A stable label needs zero unexplained misses/duplicates, preserved edits/deletions,
verified recovery and no unresolved privacy/data-loss issue. If a failure occurs,
retain redacted evidence, fix it, rerun the affected controlled cases and start a
new observation window for the candidate. Seven days alone is not proof of quality.

## Publication handoff

Contributor instructions, issue templates, security contact, privacy policy,
supported-version/device table and changelog must accurately describe the candidate.
Configure and test a private security reporting route before inviting reports.
A maintainer must review the artifact/source/certificate and remaining limitations.
Record actual channel readback and download verification only after an authorised
publication. Keep previous artifacts/source and issued history; never replace a
published binary silently. An archived planning/research track is not release
approval or operational acceptance.

## Current source/licence candidate bundle

The asset replacement/provenance is in [ASSET_PROVENANCE.md](ASSET_PROVENANCE.md).
The preferred editable vector sources and active English/German strings are tracked;
uncertain inherited launcher/store artwork and unused endpoint strings are removed.
The new development label does not choose a permanent release identity.

After freezing the source and completing matching unsigned builds:

```sh
python3 scripts/prepare_release_bundle.py --fetch-sources .local/release-source-cache
CALLTRACK_RUNTIME_OUT="$PWD/.local/release-runtime.json" \
  sh gradlew --no-daemon --console=plain \
  -I qualification/release/runtime-inventory.init.gradle releaseBundleRuntimeInventory
python3 scripts/prepare_release_bundle.py --output .local/release-evidence/UNIQUE-SOURCE-BUNDLE \
  --build-receipt .local/release-evidence/MATCHING-BUILDS/receipt.json \
  --runtime-inventory .local/release-runtime.json --source-cache .local/release-source-cache
```

Use the explicitly qualified isolated JDK path through process-local JAVA_HOME,
and the separately licensed SDK through ANDROID_HOME. No global setting changes.
The bundle rejects dirty/mismatched source, changed runtime graphs, incomplete or
modified source/POM inputs, missing APK notices and private/unsafe archive paths.
It includes the Git snapshot, exact-source revision, build/qualification scripts,
preferred vector sources, licences/notices and published source/POM material for
all63 runtime coordinates. Every payload is hashed and read back after archiving;
two independently written archive files must match. It never includes Git history,
private `.local` inputs, signing keys, SDK/JDK archives or original CallTrack APKs.

[Bundle contents and limitations](qualification/release/BUNDLE_README.txt) distinguish
app rebuilds using audited binaries from unqualified upstream dependency rebuilds.
This prepares a source-conveyance candidate. Final signed-artifact binding and
actual source delivery/download through the chosen channel remain required when
that conveyance is authorised; no publication or permanent trademark decision is
implied. Retain previous source/artifact evidence rather than relabelling it.


## Cold Linux reproduction

The manual `.github/workflows/release-reproduction.yml` workflow builds an exact
candidate using the digest-pinned Temurin17 Linux image, new SDK downloads and a
new Gradle user home. It uses separate tooling and candidate checkouts and uploads
only the unsigned APK and evidence. No cache restore, signing credential, device,
cloud-account integration or SDK licence acceptance command is configured.
The SDK terms must already be authorised for the builder; package origins/sizes,
Google checksums and reviewed SHA256 values are in
`qualification/release/linux-sdk-inputs.json`.

Use `scripts/qualify_linux_build.py --help` for a direct Linux runner. Its output
directory must not exist; a dirty/wrong candidate, reused output, wrong SDK origin,
archive traversal, changed checksum or differing APK fails closed. Retain failures
and audit newly requested dependency metadata against canonical origins; never
turn off verification or blindly generate checksums to obtain a green build.

A Docker/Linux guest with no host mounts and fresh SDK/JDK/Gradle dependencies
provides a separate software environment, but still shares this physical computer.
It does not establish a second physical host, hosted CI or independent contributor
execution. A prepared workflow is not a hosted run: retain GitHub job/run/artifact
readback only after an authorised repository upload and workflow execution.
