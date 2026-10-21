# Release decisions

Date/evidence refresh: 2026-10-20. Owning track: release_20261020. Requirements R13,
R27 and D01–D09. These decisions govern preparation, not release approval.

## RD01 — Retain native build/evidence tooling; defer signing automation

- Problem/current implementation: Python standard-library source-bound receipts,
  Gradle wrapper/AGP and SDK tools already cover development builds. No release
  artifact, custody arrangement, stable package or completed M3 exists.
- Options: retain/improve current scripts; native Gradle tasks/apksigner; Fastlane
  or a release plugin; full custom signing/publishing service; defer automation.
- Version/compatibility: JDK17, Gradle8.13, AGP8.13.2, SDK36/build-tools35.0.0.
  Retained Gradle/Android tooling is the qualified build baseline; no extra library
  or service is installed. apksigner/F-Droid comparator compatibility is unqualified.
- Role/replacement: retain development scripts; use SDK signature verification at
  candidate qualification. Do not build another signing framework around missing
  identity/acceptance. Fastlane/plugin versions and transitives are deliberately
  unselected; no endorsement inferred.
- Cost/maintenance: native commands minimize coupling; third-party automation adds
  credential handling, plugin/runtime updates and channel maintenance before there
  is a demonstrated bottleneck. No ABI/FFI change.
- Prototype: none warranted; live signing/upload would not resolve the missing
  functional evidence. Read-only dependency graph extraction supplies inventory.
- Contracts/security/tests: preserve exact source hashes, build/test exits and
  artifact/certificate readback; no credentials in logs. Meaningful signing,
  wrong-key/upgrade and channel tests belong to the candidate gate.
- Decision: retain native/development tooling, defer production automation.
- Expiry/promotion: revisit after Track4 acceptance and identity/custodian selection;
  promote automation only if repeated manual steps justify it and signing/recovery
  tests pass. Temporary dependency removal: none added.
- Migration/coexistence/rollback: current debug identity remains isolated; no key or
  data migration performed. Preserve previous artifacts and prefer forward fixes.
- Actual status/owner: documentation and graph inspection only; release owner must
  execute the candidate protocol. No reproducible signed release claim.

## RD02 — Inventory before introducing an SBOM/licence dependency

- Problem/current custom code: build.gradle plus hand-maintained direct inventory
  cannot establish all shipped transitives or their licences.
- Options: retain manual audit only; native Gradle dependency report plus per-POM/
  artifact audit; CycloneDX/SPDX generation plugin; external scanner; defer adoption.
- Candidate versions/licences: no plugin version selected. Refresh maintained
  Gradle8.13-compatible generators and licences at candidate freeze. Compare
  generated omissions, platform BOMs, tools vs runtime, included licence texts and
  transitive build complexity against a native report; JSON output alone is not
  accuracy proof.
- Role/replacement: native report retained now, candidate generator could replace
  manual dependency enumeration, not provenance/legal judgement or asset audit.
- Prototype/evidence: resolved release graph, recorded separately; no generator
  installed and no synthetic SBOM completeness claimed.
- Decision: defer additional dependency until graph stabilises in Track4. This
  avoids pinning a tool that cannot yet establish the required candidate evidence.
- Promotion/removal: prove a fixture graph with deliberate transitive and licence
  gaps, compare resolved candidate artifacts, pin/checksum tool and output schema;
  remove prototype if incomplete or unmaintained. No runtime dependency/ABI cost.
- Migration/rollback: add metadata generation separately from app behaviour; retain
  raw reports and source inputs, never rewrite historical inventories.
- Follow-up: release owner after M3; no licence clearance or security scan inferred.

## RD03 — Keep dependency locking/verification as a qualified release gate

[Gradle locking](https://docs.gradle.org/current/userguide/dependency_locking.html)
records resolved versions; [dependency verification](https://docs.gradle.org/current/userguide/dependency_verification.html)
checks artifacts against configured trust metadata. Compare both with today's
explicit direct pins and wrapper checksum: they cover different inputs. Current
versioned8.13 documentation URLs were inaccessible through the web tool, so do not
copy current-manual syntax into this build untested. Adopt only after testing the
actual wrapper, clean resolution and trusted checksum review across all used
configurations. Track4 dependency changes make a speculative lock snapshot costly.
No lock, verification metadata or new plugin is introduced here; candidate promotion
requires them or an explicit reviewed equivalent with all resolved artifacts pinned
and verified. Preserve recovery copies of approved dependency evidence.

## RD04 — Audited strict verification as the local pinning equivalent (2026-10-21)

Retain Gradle's native verification rather than add an SBOM/licence/runtime plugin
or a second dependency resolver. Initial generated XML was a prototype: compare
all entries with fresh official repository files and the completed canonical
binary audit before promotion.736 artifact/POM/module entries pass; ordinary
builds now enforce them. Metadata verification remains enabled, signatures are
not asserted, and no broad trusted-artifact exception is configured. Real wrong
AGP checksum and missing-entry failures prove rejection in an isolated checkout.

This is the reviewed equivalent to speculative per-configuration lockfiles for
the qualified scopes: allowed component versions, artifacts and repository metadata
are immutable without an explicit policy update. It does not claim a universal
graph lock, vulnerability scan, independent publisher signature or unobserved
platform coverage. Native Gradle8.13 interfaces are actually exercised; no new
production dependency is added. Future new tasks/platform classifiers require
source/licence refresh, origin checks, strict build and native acceptance where
material. Do not disable verification as recovery; preserve the failed receipt
and resolve the exact missing/changed input.

Selected macOS build scopes pass; a canonical Linux aapt2 download prepares CI
without claiming its execution. The duplicate prototype XML is retired; one
canonical root policy remains. Current app source/native Views/SQLite schemas and
versions remain unchanged. Publication, permanent signing and original M4 gates
remain separate. Archive-based tool evidence is read-only; global tools and licence
acceptances are not changed. The JDK payload comparison reproduces Homebrew
relocation/signature allocation and records its remaining bundle resource-seal
failure rather than silently claiming full resource-signature validity.
