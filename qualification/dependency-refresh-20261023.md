# Dependency refresh — 2026-10-23

## Scope and preserved evidence

Dependency qualification is isolated under `.local/dependency-refresh-20261023/`.
No app was installed, no native test was run, no device/calendar/account was
queried or changed, and no email was sent. The frozen release candidate and
observation evidence retain their original identities and dependency baseline.
Root app/build inputs and Gradle verification metadata have not been upgraded.

## Completed Python qualification

Installed CPython3.15.1 in a task-local directory using uv, without replacing the
system interpreter or pre-existing python3.15 executable. All 144 helper tests
passed, as did Conductor validation (169 documents,20 tracks,25 scenarios).
The test suite's reported device failures are synthetic fault fixtures, not live
ADB operations. Helpers use the standard library and repository-local modules;
there is no pip dependency lock to update.

`.python-version` and all active qualification workflows now select3.15.1.
The setup-python action is pinned to official v6 commit
`ece7cb06caefa5fff74198d8649806c4678c61a1`, read from the GitHub API.
Hosted CI has not run. Cold-Linux future runs now explicitly install3.15.1
using the same pinned setup action; historical receipts and immutable image
references are preserved. The local `.venv` also uses3.15.1. Active commands invoke it explicitly; script
shebangs select3.15.1 through uv. Interpreter readback and144 tests passed again
after the runtime changes. No prior active Python compatibility matrix remains.

## Android and formatting inventory

Stable artifact versions below were read from official Maven metadata, excluding
alpha/beta/RC versions. Availability is not acceptance or promotion.

| Component | Current qualified baseline | Latest stable discovered |
| --- | --- | --- |
| agp | 8.13.2 | [9.4.1](https://dl.google.com/dl/android/maven2/com/android/tools/build/gradle/maven-metadata.xml) |
| appcompat | 1.4.1 | [1.8.0](https://dl.google.com/dl/android/maven2/androidx/appcompat/appcompat/maven-metadata.xml) |
| material | 1.5.0 | [1.14.0](https://dl.google.com/dl/android/maven2/com/google/android/material/material/maven-metadata.xml) |
| work | 2.12.0 | [2.12.0](https://dl.google.com/dl/android/maven2/androidx/work/work-runtime/maven-metadata.xml) |
| fragment | 1.5.1 | [1.9.1](https://dl.google.com/dl/android/maven2/androidx/fragment/fragment/maven-metadata.xml) |
| viewmodel | 2.5.1 | [2.11.0](https://dl.google.com/dl/android/maven2/androidx/lifecycle/lifecycle-viewmodel-ktx/maven-metadata.xml) |
| constraintlayout | 2.1.3 | [2.2.2](https://dl.google.com/dl/android/maven2/androidx/constraintlayout/constraintlayout/maven-metadata.xml) |
| recyclerview | 1.2.1 | [1.4.0](https://dl.google.com/dl/android/maven2/androidx/recyclerview/recyclerview/maven-metadata.xml) |
| transition | 1.4.1 | [1.7.2](https://dl.google.com/dl/android/maven2/androidx/transition/transition/maven-metadata.xml) |
| test-junit | 1.1.3 | [1.3.0](https://dl.google.com/dl/android/maven2/androidx/test/ext/junit/maven-metadata.xml) |
| espresso | 3.4.0 | [3.7.0](https://dl.google.com/dl/android/maven2/androidx/test/espresso/espresso-core/maven-metadata.xml) |
| ktfmt | 0.59 | [0.64](https://repo.maven.apache.org/maven2/com/facebook/ktfmt/maven-metadata.xml) |
| google-java-format | 1.24.0 | [1.37.0](https://repo.maven.apache.org/maven2/com/google/googlejavaformat/google-java-format/maven-metadata.xml) |
| junit4 | 4.13.2 | [4.13.2](https://repo.maven.apache.org/maven2/junit/junit/maven-metadata.xml) |

Kotlin2.4.21 and Gradle9.8.2 are newer than the baseline2.3.21/8.13.
[The Kotlin compatibility table](https://kotlinlang.org/docs/gradle-configure-project.html)
fully supports Kotlin2.4.21 through AGP9.3.1 and Gradle9.7.0.
Consequently AGP9.4.1/Gradle9.8.2 is not a fully supported combination in that
table. This is a support-matrix boundary, not proof those versions cannot build.

## Isolated Android attempt and precise blocker

Created a tracked-source snapshot with AGP9.3.1, Kotlin2.4.21, Gradle9.7.0
(distribution checksum from the official Gradle endpoint), and the latest stable
runtime/test coordinates above. Existing plugin layout was retained with
`android.builtInKotlin=false` and `android.newDsl=false` only in the experiment.
[AGP built-in Kotlin migration](https://developer.android.com/build/migrate-to-built-in-kotlin)
is a separate required migration for eventual adoption; the opt-outs are interim.

Attempted `:app:testDebugUnitTest :app:assembleDebug :app:assembleDebugAndroidTest
:app:lintDebug`. Gradle exited1 during configuration: **138 classpath artifacts
failed strict dependency verification** because the release trust inventory does
not contain the new artifacts. No bypass, lenient verification or automatic
trust promotion was used. Compilation, runtime dependency resolution, lint and
native parity are therefore unqualified for this candidate.

Next: independently audit new plugin artifacts and licences, qualify SDK Build
Tools36.0.0 in a separate SDK (AGP9.3 minimum), migrate the plugin layout, then
run builds/JVM/lint and the affected emulator parity/upgrade matrix. Review new
runtime and formatter sources/licences before promotion. Do not replace the
root pins or reuse old APK acceptance for these changed dependencies.

## JDK patch qualification

Official Temurin releases identify JDK17.0.21+9, JDK25.0.5+8 and JDK27+35.
AGP9.3 documents JDK17 as its minimum/default. JDK27 is not presumed compatible
with the selected Gradle; this task tests the latest17 patch first. The Adoptium
REST assets API returned403; the official GitHub release assets were available.
Downloaded the macOS ARM64 JDK17.0.21+9 archive, verified its published SHA256
`78b08c7f5a0598242d5719c022e19479cb4a3aa45c37c19726a757a469ed1c15`,
and passed `codesign --verify --deep --strict` and `java -version`.
The separate unchanged-source baseline build passed in7m33s: all115 JVM tests,
debug APK, instrumentation APK and lint (no Error/Fatal). The publisher certificate
reads Eclipse Foundation, Inc., TeamIdentifier JCDTMS22B4. Regular modern/slice/
Compose CI now pins17.0.21+9; the upstream JDK11 reference and historical release
receipts remain scoped to their original baseline. No new native or release
reproduction acceptance is claimed. JDK25/27 remain unqualified.

## Review and remaining gates

Python qualification does not qualify a new APK. Newer Android dependencies,
formatters, JDK major versions and hosted CI remain distinct gates. Physical,
remote-calendar, signing and elapsed operational acceptance remain pending under
the existing release protocol. No production dependency is labelled upgraded
solely from a download, version lookup or failed build.
