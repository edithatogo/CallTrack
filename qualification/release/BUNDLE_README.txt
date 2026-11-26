CallTrack Community source, licences and notices

SOURCE_REVISION.txt identifies the exact clean candidate. MANIFEST.sha256.json
binds every payload file except the manifest itself. A separate receipt binds the
archive and the corresponding unsigned APK. Release metadata identifies the
published signed APK and its signing certificate.

Contents:
- source/: the exact tracked Git tree, including preferred app/vector source,
  build scripts, wrapper, dependency verification policy, qualification tests,
  contributor instructions, GPL licence, attribution and modification notices.
- dependency-sources/: published source JARs and POMs for the runtime inventory,
  with URLs, SHA256 and matching audited runtime binary hashes. Source archives
  for metadata-only runtime artifacts may contain only metadata and licence text.
  Copyright and licence headers remain intact.
- licences/: convenient copies of app notices and retained legal documents.
- candidate-runtime-inventory.json: newly resolved candidate artifact names/hashes.

Build/modification:
Read source/CONTRIBUTING.md, source/qualification/README.md and source/RELEASING.md.
Use the toolchain selections in source/README.md, including Android SDK platform
37.1 and Build Tools 36.0.0, under the builder's Android SDK licence.
The wrapper downloads the SHA256-pinned Gradle distribution; normal Maven
resolution must satisfy the committed dependency-verification policy.
sh gradlew :app:assembleRelease produces an unsigned app.
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

For each published binary, use the source archive from the same GitHub Release.
