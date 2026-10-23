# Isolated JDK qualification — 2026-10-22

Scope: resolve the local JDK resource-seal blocker using an isolated archive,
without repairing/re-signing Homebrew, changing global defaults or restarting.
The historical Homebrew audit remains valid within its stated payload-only scope.
This qualification does not complete Track5 or establish independent-host builds.

## Selection and origin

Eclipse Temurin17.0.20.1+1, macOS ARM64 JDK, is a bounded build-tool alternative
within the existing JDK17 baseline. The publisher's release API selected this
archive on2026-10-22. It is not a claim that its patch level supersedes the installed
Homebrew17.0.21, nor a permanent default or newest/security certification.
Reassess supported patches before a later candidate.

[Adoptium archive guidance](https://adoptium.net/installation/archives) documents
archive extraction and SHA256 checking. Exact release source:
[Temurin17.0.20.1+1](https://github.com/adoptium/temurin17-binaries/releases/tag/jdk-17.0.20.1%2B1).
The Adoptium API returned HTTP403; the official GitHub release API/downloads worked.
No authentication, consent or package installation was needed.

Raw downloads, checksum, release metadata, extraction manifest, signature output,
fault experiment and build receipts remain under excluded
`.local/release-evidence/isolated-jdk-20261022/`. Public hashes and results are in
[isolated JDK evidence](isolated-jdk-evidence-20261022.json).

## Verification procedure

1. Download the exact macOS ARM64 `.tar.gz` and its `.sha256.txt` from the linked
   publisher release over HTTPS. Require a matching archive SHA256 before extraction.
2. Extract locally with Python tarfile's `data` filter. Compare every regular
   extracted archive file against its archive bytes; retain a SHA256 manifest.
3. Run `codesign --verify --deep --strict` against the intact bundle, then require
   `-R '=anchor apple generic and certificate leaf[subject.OU] = "JCDTMS22B4"'`.
   Retain developer identity, certificate chain, timestamp and sealed-resource count.
   No ad-hoc re-signing or resource-ignore exception is used.
4. Copy the bundle to an excluded disposable location, change `Contents/Home/release`
   and require strict verification to reject it. Remove that copy.
5. Run the extracted `Contents/Home/bin/java -XshowSettings:properties -version`.
   Run Gradle8.13 `--version` with process-local `JAVA_HOME`; confirm launcher and
   daemon select the isolated JDK. Keep inherited legal notices in the archive.
6. Clone frozen candidate5578bf5 locally and invoke its unchanged
   `scripts/qualify_release_build.py --output <new-directory> --jdk <isolated-home>
   --sdk <existing-sdk>`. This creates two further clean checkouts, builds release,
   runs release JVM tests/lint, verifies unsigned status and compares both APKs.
   Compare their hashes with the existing5578bf5 candidate; retain VCS metadata.
7. Recheck bundle sealing and its file manifest after builds. Compare the installed
   Homebrew JDK's359 files with its historical audit to prove it was not modified.

`spctl --assess --type execute` returns3 for both bundle and java launcher:
“the code is valid but does not seem to be an app”. This is retained as a separate
Gatekeeper application-assessment limitation, not silently treated as acceptance.
Strict signature/resource sealing and the explicit Apple-chain/publisher requirement
are the acceptance checks for this command-line build-tool scope. Notarization,
Gatekeeper app admission and detached GPG verification are not claimed.

## Boundaries and recovery

Use the isolated path explicitly through `--jdk`; no shell profile, JAVA_HOME
persistent setting, Homebrew package or system Java registration changes.
The old JDK resource-seal defect is still reproducible and its359 file hashes
remain unchanged. The blocker is resolved for builds using the qualified alternate
path; the existing Homebrew installation has not been repaired.

The existing Gradle8.13, AGP8.13.2, SDK36/build-tools35.0.0 and736-entry dependency
verification policy remain selected. Same-host/shared-cache comparisons do not
satisfy D02/D05 independent reproduction. No emulator, physical phone, cloud,
production signing, operational observation or public release acceptance is added.

## Accepted results

All459 regular extracted files match the checksum-verified archive. Strict bundle
verification and the explicit publisher requirement pass before and after builds;
a modified sealed resource is rejected. The extracted manifest is unchanged.

Two clean5578bf5 release builds take177.0s total. Each passes115 JVM tests with
zero failures/errors/skips and lint with zero errors/fatal findings (69 warnings,
2 hints). Both unsigned APKs exactly match the prior audited candidate:
`b5ba573be2d79e0b4edd9a1c858ac578347f972ddb3e12704185543d7a47a283`.
No artifact metadata was removed or rewritten to obtain equality.

121 host tests pass. See the [review](review-isolated-jdk-20261022.md). No production
source or build policy change was necessary.
