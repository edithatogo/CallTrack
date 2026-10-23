# Distribution and readiness findings

Evidence refresh: 2026-10-20, source checkpoint 28b4180 (track start 4b31e39).
Current local readback: clean tree at start; no Git remotes configured. No public
repository, hosted CI, release tag, upload, signing key or store acceptance exists.

## Channel comparison

| Route | Evidence and conclusion | Remaining work |
|---|---|---|
| Local source/debug build | Available today for development, with source-input receipts | Finish M3 before alpha claims; debug signatures are not production identity |
| Source hosting and direct signed APK | Candidate initial community channel; preserves source/release association | Select host/identity, publish matching source/notices and tested signed artifact; no upload authorised by this track |
| F-Droid | Candidate after functional release and licensing audit | Fresh fork ID/branding, source recipe, maintained useful app, dependency/asset audit and review; no acceptance inferred |
| Google Play | Uncertain for the app's actual core use case | Demonstrate eligible call-log permission use and declaration/review; do not add a token dialer/CRM role merely to claim eligibility |

[Play policy](https://support.google.com/googleplay/android-developer/answer/10208820?hl=en)
restricts call-log access to permitted core functions and reviewed exceptions. The
listed automation/cross-device exceptions do not establish that calendar logging
qualifies. The page announces a January2027 change to phone-call account
verification, not permission for this app. Refresh before any submission.

[F-Droid inclusion policy](https://f-droid.org/docs/Inclusion_Policy/) requires FLOSS
source/dependencies/toolchain and meaningful maintained functionality. Forks need
fresh application identity and corresponding branding. It lists a three-month
active-development expectation with exceptions; this new fork has no established
exception. A public source recipe and policy review remain necessary.

[Android developer verification](https://developer.android.com/developer-verification)
is another distribution variable: current material describes regional enforcement
from September2026 for participating stores on certified devices. Recheck route,
region and supported installation method before publishing. This is not evidence
that every direct APK or this host's ADB workflow is blocked. No account registration
or identity submission was performed.

## Licensing and source boundary

The adopted upstream is pinned to aaa56443a294aa61bad48b620427e53f9521c5fe.
[NOTICE](../../../NOTICE.md) identifies its GPLv3 source and our changes; the
[local GPL text](../../../LICENSE), particularly sections4–6, is the release
baseline. Preserve notices, identify modifications, and make exact corresponding
source/build scripts available with conveyed object code using the selected GPL
compliance route. Do not infer “or later” solely from a licence appendix example.
The old CallTrack APK/extracted implementation has no established source licence
and must stay excluded. The MIT comparison app is research input, not incorporated.
Assets, translations, third-party notices and all resolved transitives still need
artifact-level provenance review; a Maven coordinate is not licence proof.

## Reproducibility and signing

Current JDK17/Gradle8.13/AGP8.13.2/SDK36/build-tools35.0.0 can build debug artifacts.
Receipts match selected build inputs, not the whole release source archive. No
independent bitwise comparison or release signing exists. The last Track4 receipt
predates whitespace-only formatter changes; it must not certify the current tree.

[F-Droid reproducibility guidance](https://f-droid.org/docs/Reproducible_Builds/)
describes upstream-signature verification and metadata including allowed signing
keys. It flags build-tools35+ apksigner compatibility with apksigcopier. Our
35.0.0 build tools therefore need a bounded signing/reproduction check before that
route; do not silently downgrade the app toolchain. Inspect differences rather
than stripping APK content until hashes happen to agree.

[Android signing guidance](https://developer.android.com/studio/publish/app-signing)
explains identity continuity for updates and the distinction between upload and app
signing keys. Select custody/channel strategy before the first production install.
No rotation or downgrade compatibility is assumed.

## Dependency and app-owned inventory

Direct declarations inspected in app/build.gradle:

| Dependency | Version | Role / reassessment owner |
|---|---|---|
| Work runtime | 2.7.1 | Retained legacy scheduling; Track4 researches replacement/version |
| AppCompat / Material / ConstraintLayout | 1.4.1 / 1.5.0 / 2.1.3 | Current Views; Compose track evaluates takeover |
| Navigation fragment / UI | 2.4.1 / 2.4.1 | Legacy navigation; Compose track |
| Legacy support / RecyclerView / Preference | 1.0.0 / 1.2.1 / 1.2.0 | Inherited UI; remove only after reachability/parity audit |
| Gson | 2.9.0 | Legacy endpoint serialization; destinations/Kotlin reassess |
| Fragment / lifecycle-viewmodel-ktx | 1.5.1 / 2.5.1 | Interim compatibility overrides; revisit with Work/UI upgrades |
| JUnit / AndroidX test ext / Espresso | 4.13.2 / 1.1.3 / 3.4.0 | Tests only; separate from shipped runtime |

Declared versions are not necessarily selected versions. The [resolved release
runtime report](dependency-report.json) is captured separately in this track; it is
not an audited SBOM. Kotlin stdlib1.6.21 (with jdk7/jdk8 modules1.6.0) and
coroutines1.6.1 already appear transitively through retained libraries. App-owned
source remains Java; “Kotlin migration later” does not mean Kotlin-free artifacts.
AndroidX/Material/Gson generally publish permissive licences, but per-artifact
licence text and transitive attribution remain release gates, not blanket clearance.

App-owned mapping/provider adapters, SliceEngine, SQLite ledger, DurableDelivery,
CallIdentity and source-bound Python harnesses were inspected. Retain the harnesses;
replace hand-maintained release dependency inventory with a qualified SBOM tool only
when a candidate graph settles. Room is assessed in Track4 for SQLite takeover;
Kotlin/Compose/KMP/Rust are not release prerequisites or new dependencies here.

## Privacy and supported devices

[Lifecycle](../../../PRIVACY.md) maps actual data/retention and missing controls.
No network permission does not prevent provider-mediated disclosure. The current
manual action uses SliceEngine; tested DurableDelivery is not yet wired into it.
Minimum SDK26 is a build constraint, not tested support. API36 emulator foundation
and earlier API37 manual-provider checks cover different revisions/scopes. No
OEM, minimum-OS, current full parity, cloud account or live-call support claim can
be made. See [runbook](../../../RELEASING.md) for qualification and observation.
