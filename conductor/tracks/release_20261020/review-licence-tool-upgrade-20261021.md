# Review Report: Track5 licence/tool-trust and schema/provider continuation

## Summary

Local audit, enforced dependency verification and synthetic native upgrade qualification pass with fixes; full M4 remains incomplete.

## Verification Checks

- [ ] **Plan Compliance**: Partial - requested local scope delivered; original production/independent/platform/operational acceptance remains open.
- [x] **Style Compliance**: Pass - native Gradle verification, standard-library audit tools, formatted Java qualification fixtures; no new app/runtime dependency or architecture migration.
- [x] **New Tests**: Yes - source-pinned schema provenance, trust-policy source binding, metadata/HTTPS/archive boundaries, Mach-O payload constraints, explicit resource-seal limits and relevant-fixture binding.
- [x] **Test Coverage**: Partial - actual local package/schema/provider/recovery and strict build failures; published old APKs, physical/cloud/manual UI and production signing remain unqualified.
- [x] **Test Results**: Passed -121 host tests;15 successful native invocations and two expected install rejections; two fresh clean release builds each pass115 JVM tests, zero lint errors and unsigned equality. Final document check recorded in plan.

## Findings

### High — Generated checksums alone did not constitute trust approval (fixed)

- **File**: `gradle/verification-metadata.xml`, `scripts/audit_gradle_verification.py`.
- **Context**: Enabling cache-generated values would merely bless whatever was downloaded.
- **Suggestion**: Applied independent canonical comparison for all736 binary/POM/module entries, then exercised strict builds and actual wrong/missing AGP trust entries before promotion. Metadata verification stays enabled, no broad trust exception is configured, and the policy hash is included in source receipts.

### Medium — Named configurations missed detached tools and generic tracing disturbed project variants (fixed)

- **File**: `qualification/release/tooltrust.init.gradle`, `resolution-trace.init.gradle`.
- **Context**: Compiler/lint/aapt2 resolutions were incomplete; resolving all default artifact views also caused ambiguity for the app's own project variants.
- **Suggestion**: Observe detached module-component artifacts while excluding project outputs before artifact selection. Actual traced build passes and239 unique binary artifacts match canonical downloads. Failed traces remain retained.

### Medium — Gradle's local AAR filenames differ from published filenames (fixed)

- **File**: Dependency-verification policy.
- **Context**: The initial prototype rejected three already verified renamed Android AARs.
- **Suggestion**: Bind the exact Gradle-resolved alias and published URL name to the same canonical binary hash; preserve module-metadata evidence. Strict positive and negative tests pass; no verification bypass is used.

### Medium — Fixture identity and binding caused false failures (fixed)

- **File**: `ReleaseUpgradeProviderTest.java`, `scripts/qualify_release_upgrade.py`.
- **Context**: A synthetic import ID violated the real UUID contract. Binding every file in the release qualification directory also conflated independent licence work with consumed native test inputs.
- **Suggestion**: Use deterministic valid UUIDs and bind only actual Java/assets/schema-provenance inputs. Relevant edits still invalidate preflight; unrelated licence edits do not. The final15-invocation run passes, including import1→2 preservation and cleanup.

### Medium — Audit notice and archive handling needed narrower boundaries (fixed)

- **File**: `scripts/audit_release_artifacts.py`, `audit_release_toolchain.py`.
- **Context**: Classes called UserNotice/License are not legal notices. Nested archives, malformed load commands, signature allocation metadata and downgraded redirects needed explicit rejection paths.
- **Suggestion**: Restrict notice filenames/types, bound archive expansion/depth/downloads, require HTTPS redirects and constrain Mach-O normalization to exact signature/LINKEDIT tail allocation. Payload changes remain hashed. Host regressions pass; retained239 artifacts and12 normalized payloads satisfy the stricter checks.

### Medium — JDK bundle resource sealing remains unqualified (retained limitation)

- **File**: `toolchain-origin-evidence-20261021.json`.
- **Context**: Homebrew relocation explains the JDK byte differences, but the installed MacOS/libjli bundle reports missing resource sealing. Code-page verification and reconstructed code/data match do not establish complete resource-signature validity.
- **Suggestion**: Keep separate true/false fields and expose the limitation in CLI/docs. Do not modify global tools or claim a repaired seal. Qualify a clean toolchain/bundle before a complete signed-tool claim; current payload-origin comparison is narrower.

## Scope and final decision

Reviewed the implementation from80e0566 through the continuation commits350ce3d,
3daa8ef,b1ed08b,caf8d8c,5578bf5 and final review fixes recorded in plan. Examined
scripts, Gradle init/policy data, Java fixtures, host regressions, provenance hashes,
exact native outputs and cleanup readback. No application source, schema/version,
identity, runtime dependency or UI change occurred. Strict verification is a build
policy change; fresh builds and receipts include it.

Local requested scope passes with fixes. Remaining production identity/signing,
licence/conveyance, independent/platform, JDK bundle-seal, physical/cloud and
operational gates are neither waived nor grounds for archive. The new unsigned
candidate retains its exact revision metadata; previous runtime/UI evidence is
reused only with the documented APK/payload equivalence, not promoted into a newly
run full native matrix. No unresolved implementation defect was found in this
bounded review.

Official checks/interpretation references: [Gradle checksums](https://gradle.org/release-checksums/),
[Gradle8.13 dependency verification](https://docs.gradle.org/8.13/userguide/dependency_verification.html),
[Apache/GPL compatibility](https://www.apache.org/licenses/GPL-compatibility),
[GNU corresponding-source discussion](https://www.gnu.org/licenses/gpl-faq.html#CorrespondingSource),
and [Bouncy Castle pinned licence](https://github.com/bcgit/bc-java/blob/r1rv79/LICENSE.html).
These references support the audit interpretation, not a blanket legal or publication approval.
