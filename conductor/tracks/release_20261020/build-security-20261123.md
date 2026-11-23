# Build dependency security qualification — 2026-11-23

The resolved root buildscript classpath on the prior candidate contained 13
components matching 49 distinct OSV advisory IDs. The verification metadata is
an allowlist covering multiple historical builds, not a lockfile; the 55 IDs
reported when scanning that entire file therefore do not all describe the
currently selected graph. Historical entries remain in place for their existing
qualification scopes.

The candidate updates the Kotlin and Compose compiler plugins together to
2.4.21, and uses a Netty 4.1 BOM plus minimum constraints for affected Guava,
Commons Compress, jose4j, Bouncy Castle and JDOM build-time transitives. The
constraints permit newer compatible selections. The Kotlin 2.4.21/Gradle 8.13/
AGP 8.13.2 combination is within JetBrains' documented compatibility range.
Google's Kotlin table lists a D8/R8 minimum that differs from the version embedded
in AGP 8.13.2, so the candidate uses the R8 project's documented override with
D8/R8 9.5.33. The release does not enable minification; a minified release needs
its own R8 qualification.

The [audit receipt](build-security-audit-20261123.json) binds the final Gradle
verification metadata to 160 added artifacts whose SHA-256 values were checked
against fresh bytes from their canonical Maven Central or Google Maven origins.
Existing artifact hashes and verification settings were preserved. An initial
network attempt had 23 failures, retained in the receipt; a later IPv4/TLS audit
verified the affected artifacts. The selected root buildscript graph and release
runtime graph returned no OSV matches at the time of the query. The broader
selected Gradle configuration inventory found no OSV matches for 263
coordinates, and the separately queried R8 override had no match. The
inventory reports that `lintClassPath` was absent and records local projects
separately; it does not establish coverage of every lint dependency, shared-core
configuration, Android SDK component, shaded component or prototype.

The local release build, lint, 119 release JVM tests and three shared-core JVM
tests passed. Kotlin 2.4.21 produced 17 additional `Recycle` lint warnings at
existing cursor queries chained directly to `use`; those diagnostics remain
visible and have not been suppressed. This observation does not prove why lint
reported them. Hosted API 26/33/36, accessibility, recovery and other existing
acceptance checks remain required before integrating the candidate.

References: [Kotlin Gradle compatibility](https://kotlinlang.org/docs/gradle-configure-project.html),
[Android Kotlin/D8 compatibility](https://developer.android.com/build/kotlin-support),
and the [R8 replacement procedure](https://r8.googlesource.com/r8/+/refs/heads/main/README.md#replacing-r8-in-android-gradle-plugin).
