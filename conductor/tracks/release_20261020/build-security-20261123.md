# Build dependency security qualification — 2026-11-23

The prior resolved root buildscript classpath contained 13 components matching
49 distinct OSV advisory IDs. The verification metadata is an allowlist covering
multiple historical builds, not a lockfile; the 55 IDs reported when scanning
that earlier entire file do not all describe its selected graph. Historical
entries remain in place for their existing qualification scopes.

The candidate updates the Kotlin and Compose compiler plugins together to
2.4.21, and uses a Netty 4.1 BOM plus minimum constraints for affected Guava,
Commons Compress, jose4j, Bouncy Castle and JDOM build-time transitives. The
constraints permit newer compatible selections. The Kotlin 2.4.21/Gradle 8.13/
AGP 8.13.2 combination is within JetBrains' documented compatibility range.
Google's Kotlin table lists a D8/R8 minimum that differs from the version embedded
in AGP 8.13.2, so the candidate uses the R8 project's documented override with
D8/R8 9.5.33. The release dex task's classloader selected that verified version.
The release does not enable minification; a minified release needs its own R8
qualification.

AGP's bundled lint emitted Kotlin 2.4 metadata incompatibility errors despite a
successful Gradle exit. Lint 9.5.2 avoided those errors but skipped the existing
Lifecycle custom-check registry. The documented lint override to 9.3.3 avoids
both diagnostics. A deliberately unsafe `repeatOnLifecycle` fixture failed lint
on both the baseline and the candidate. A separate `whenStarted` unsafe-view
fixture was not detected on either, so that detector's behavior is not established
by this qualification. No lint checks are suppressed. The clean final lint report
contains zero errors, 29 warnings and three hints.

AGP resolves the lint engine in detached configurations, separate from the root
buildscript. The candidate applies minimum constraints to the lint component
metadata for Guava, Commons Lang, HttpClient/HttpMime and Bouncy Castle. The
app and shared-core lint tasks actually received the patched JARs; all 68 selected
lint coordinates returned no OSV matches at the time of the query. The broader
selected root/app Gradle configuration inventory found no OSV matches for 263
coordinates, and the separately queried R8 override and release runtime graph
had no matches. These results do not cover all shared-core configurations,
Android SDK components, shaded components or prototypes. The inventory explicitly
reports the absent named `lintClassPath`; the detached lint graph is recorded
separately.

The [audit receipt](build-security-audit-20261123.json) binds the final Gradle
verification metadata to 245 added artifacts whose SHA-256 values were checked
against fresh bytes from their canonical Maven Central or Google Maven origins.
Existing artifact hashes and verification settings were preserved; this is not
a claim of publisher-signature verification. An initial network attempt had 23
failures, retained in the receipt; a later IPv4/TLS audit verified those artifacts.
The first hosted build stopped because two metadata artifacts were missing from
the verification file. A separate fresh-home local run identified two more;
all four were independently checked before inclusion. A second fresh Gradle home
then passed strict dependency verification, release assembly, lint, 119 release
JVM tests and three shared-core JVM tests. The repo-local Python 3.15.2 host
suite passed 184 tests. Hosted API 26/33/36, accessibility, recovery and other
existing acceptance checks remain required before integrating the candidate.

References: [Kotlin Gradle compatibility](https://kotlinlang.org/docs/gradle-configure-project.html),
[Android Kotlin/D8 compatibility](https://developer.android.com/build/kotlin-support),
[using newer lint](https://googlesamples.github.io/android-custom-lint-rules/usage/newer-lint.md.html),
and the [R8 replacement procedure](https://r8.googlesource.com/r8/+/refs/heads/main/README.md#replacing-r8-in-android-gradle-plugin).
