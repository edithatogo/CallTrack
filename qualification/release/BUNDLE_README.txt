CallTrack development source, licences and notices bundle

LOCAL PREPARATION ONLY. No signing, installation, upload or publication occurred.
SOURCE_REVISION.txt identifies the exact clean candidate. MANIFEST.sha256.json
binds every payload file except the manifest itself. A separate receipt binds the
archive and the corresponding unsigned APK. Retain both with the candidate.

Contents:
- source/: the exact tracked Git tree, including preferred app/vector source,
  build scripts, wrapper, dependency verification policy, qualification tests,
  contributor instructions, GPL licence, attribution and modification notices.
- dependency-sources/: published Java/Kotlin source JARs and POMs at all63 runtime
  coordinates, with URLs, SHA256 and matching audited runtime binary hashes.
  Copyright and licence headers remain intact in their original source files.
- licences/: convenient copies of app notices and retained legal documents.
- candidate-runtime-inventory.json: newly resolved candidate artifact names/hashes.

Build/modification:
Read source/CONTRIBUTING.md, source/qualification/README.md and source/RELEASING.md.
Use JDK17, Gradle8.13, AGP8.13.2, Kotlin2.3.21, SDK36/build-tools35.0.0 and an Android
SDK licensed separately by the builder. The wrapper downloads the SHA256-pinned
Gradle distribution; normal Maven resolution must satisfy the reviewed736-entry
verification policy. sh gradlew :app:assembleRelease produces an unsigned app.
No private reference APK/checkouts, production key or credentials are needed.

The build uses audited prebuilt library artifacts. Published source JARs are
included for modification and inspection; a complete upstream build environment
and a binary-from-source rebuild for every third-party library are NOT qualified
by this bundle. System/general-purpose build tools (SDK/JDK/Gradle distributions)
are not redistributed. Gradle wrapper licence/notice is retained separately.

This app is a GPLv3 fork; runtime libraries retain their own Apache2 terms and
copyrights. See source/LICENSE, source/NOTICE.md, source/THIRD_PARTY_NOTICES.md,
source/ASSET_PROVENANCE.md and source/MODIFICATIONS.md. No historical CallTrack
APK artwork/code or unsupported permanent trademark rights are conveyed.

An eventual binary conveyance must provide corresponding source through the
chosen distribution route and verify actual access/download, final signed-artifact
correspondence and any channel-specific terms. This local candidate is not that
channel outcome. Production signing, physical/cloud and operational gates remain.
