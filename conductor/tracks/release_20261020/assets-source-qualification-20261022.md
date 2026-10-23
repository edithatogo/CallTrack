# Asset provenance and source/notices candidate — 2026-10-22

The uncertain inherited launcher/store artwork is removed. The source/notices
candidate is prepared for the selected development identity; publication and full
M4 remain pending. [RD05](decisions.md) records the decision and boundaries.

## Asset and attribution outcome

[Asset provenance](../../../ASSET_PROVENANCE.md) and the
[SHA256 inventory](../../../qualification/release/assets-provenance.json) cover24
current non-code main-source files. Eleven inherited PNGs, the inherited vector
foreground/background and adaptive-icon wrappers are removed/replaced. The new
geometric vector is original preferred XML under GPLv3, without tracing or an
external font/icon input. No store artwork is supplied for an unselected channel.

Forty-four unused inherited endpoint keys are removed from each locale. Apart
from the replaced launcher label, all114 current English and97 German strings
remain identical to the pre-change source. The earlier project-authored German
translations are preserved. Functional upstream themes/colour configuration retain
the upstream GPL basis; the unchanged licence was also compared with its fresh
[pinned upstream source](https://github.com/MarkoBL/AndroidCallLogSync/blob/aaa56443a294aa61bad48b620427e53f9521c5fe/LICENSE).

The label is “CallTrack Community (dev)” in both locales. Package
`org.calltrack.community.dev`, version10/0.1.0-dev, declared minSdk26/targetSdk36, namespaces, schemas
and production Kotlin/Java code remain unchanged. The label/icon are a development
replacement, not permanent trademark clearance or a production identity decision.
Upstream attribution and dated modification notices remain prominent in
[NOTICE.md](../../../NOTICE.md) and [MODIFICATIONS.md](../../../MODIFICATIONS.md).
The old audit remains historical and is not rewritten as rights clearance.

## Licence and published-source material

All63 exact runtime coordinates have published Java/Kotlin source JARs and POMs,
with source and POM SHA256 values in the
[source catalogue](../../../qualification/release/runtime-sources.json). They were
downloaded from the canonical Google Maven/Maven Central paths. Existing source
copyright headers remain in the unmodified archives. The fresh68-row runtime
inventory (63 unique artifacts) exactly matches the audited binary names/hashes.
No runtime dependency or verification-policy change occurs.

Both APKs contain11 `assets/open_source/` files: GPL/Apache texts, project and
third-party notices, the retained-notice index and six exact legal documents from
runtime artifacts. Twelve binary/source origins map to those six retained files.
Every file is compared byte-for-byte with both built APKs. The source bundle also
retains Gradle wrapper licence/notice material separately from runtime terms.
Authentic licence text whitespace is retained; whitespace-only flags in those
copied documents are not “fixed” by changing the audited text.

## Exact candidate and bundle

Source candidate: `5a3a3e470ab0955714a1db33d6196063400ed5b1`.
Two clean unsigned builds use the qualified isolated Temurin JDK17 and existing
strict736-entry policy. Each passes115 JVM tests, no failures/errors/skips, and
lint with zero Error/Fatal findings (23 warnings,2 hints). Both unsigned APKs:
`e3780da6e9162d29fd35553e8a7362b876857d016cc398ebea1ee26e67b4bc8a`.
These intentionally supersede the old artifact because resources/notices changed;
no old native receipt is relabelled as acceptance of this APK.

The643-member source/notices tar contains the exact current Git snapshot,
preferred editable resources, build scripts/tests/trust policy, licences/notices,
63 published source JARs/POMs, source revision and hashed payload manifest.
It exports no Git history, superseded PNG bytes, `.local` evidence, APK, key or
SDK/JDK distribution. The checked-in Gradle wrapper remains an explicitly licensed
build-tool bootstrap. Two separately written tar files match, and full member/hash
readback passes. Source bundle SHA256:
`a2003bba5e6242e6ec3504230c272f149cc188b9195950cee34a1d1cd0b0ee76`.

The real CLI rejects four cases: wrong source revision, missing runtime artifact,
missing dependency source and an APK missing the GPL text. Temporary fault APKs
are removed. Five host regressions also cover unsafe/private paths, cache tampering,
external symlinks, invalid source archives and deterministic output.126 host tests
pass. See [review](review-assets-source-20261022.md) and
[public evidence](assets-source-evidence-20261022.json) for final acceptance results.

## Source-only reconstruction

The tar was extracted under a new PortableSSD cache directory outside any Git
repository. Its source builds without private reference APKs/checkouts and passes
115 JVM tests and lint with zero Error/Fatal findings (23 warnings,2 hints). All
493 tracked source files remain unchanged after this build. The output is verified
unsigned. Its APK ZIP payloads differ only in
`META-INF/version-control-info.textproto`, because the source archive lacks Git
metadata. No metadata is stripped or fabricated; no bitwise equality is claimed
for that source-only APK. The application/runtime payloads match the frozen
candidate. This still shares the host, SDK/JDK and dependency cache and does not
satisfy independent-host qualification.

## Remaining scope

[Bundle procedure](../../../qualification/release/BUNDLE_README.txt) and
[release runbook](../../../RELEASING.md) explain reconstruction and packaging.
The build uses audited prebuilt library artifacts. Published source JARs/POMs are
included for modification and inspection; complete upstream dependency build
systems and binary-from-source reproduction are not qualified here. Do not promote
this preparation into an unconditional claim that every future conveyance's GPL
corresponding-source obligations are discharged.

Final signed-artifact/source binding and actual source download/access through the
chosen distribution channel remain required when authorised. Production
identity/signing, independent/hosted/platform, physical/cloud and seven-day
operational gates remain. No native matrix, phone installation, cloud action,
publication, key creation, restart or global tool change occurs in this task.

Raw download/source/build/bundle/fault receipts remain excluded under
`.local/release-evidence/asset-source-inputs-20261022/`,
`.local/release-evidence/asset-source-builds-20261022/` and
`.local/release-evidence/asset-source-bundle-20261022/`.


## Cold-build successor — 2026-10-22

[Independent Linux qualification](linux-reproduction-qualification-20261022.md)
adds three canonically verified metadata checksums after a real cold-cache failure,
then freezes9464408. The exact source/notices bundle is refreshed for that commit;
its runtime APK payload is unchanged except Git provenance. This report and its
5a3a3e4 archive remain historical, not relabelled as the new artifact.
