# Contributing

This is a development fork with manual/automatic logging, historical import and
recovery controls. Native acceptance is scoped to recorded emulator evidence. Check [Conductor](conductor/index.md) before proposing changes.
Debug builds use the disposable development package. See the [release gates](RELEASING.md)
for the distribution variant.

## Reproduce the development build

Use a fresh checkout of the exact commit being reviewed and the versions in the
[build guide](README.md#build-and-qualification). Install Build Tools 36.0.0 and
35.0.0 for native qualification. Use the checked-in Gradle wrapper.
The Kotlin implementation retains one Java nullable-label bridge and Java interop
tests. The app uses JVM 11 bytecode with minimum Android API 37.
Read the selection with `python_version="$(cat .python-version)"`, install it
with `uv python install "$python_version"`, then create `.venv` using
`uv venv --python "$python_version" .venv`. Verify `.venv/bin/python -V` and run
`.venv/bin/python scripts/verify_python_runtime.py`.
No third-party Python packages are needed for the helper/test suite. All active
qualification workflows, including cold Linux reproduction, read `.python-version`
and create a local virtual environment. Python 3.15.2 is the currently qualified
default; another stable version may be selected for a specific compatibility or
task need and must be qualified against the relevant behavior. Historical receipts retain their original
interpreter. See the [runtime policy](conductor/python-runtime-policy.md) and
[dependency refresh](qualification/dependency-refresh-20261023.md)
for historical isolated Android upgrade evidence.

Set JAVA_HOME and ANDROID_HOME to those installations. SDK licences must be accepted
by the person operating the build environment. Run from the repository root:

```sh
.venv/bin/python scripts/run_slice_build.py --jdk "$JAVA_HOME" --sdk "$ANDROID_HOME"
.venv/bin/python -m unittest discover -s tests -v
.venv/bin/python scripts/verify_conductor.py
```

No private APK or reference checkout is needed for the root build. Private-source
verification is an optional maintainer audit, not a contributor prerequisite.
Build receipts and logs are excluded under `.local/slice-evidence/`. Check the exit
status, input hashes, test results and artifact hashes together. A successful debug
build establishes neither a production signature nor bit-for-bit reproducibility.
See [qualification](qualification/README.md) for the separate upstream reproducer.

Use google-java-format 2.0.0 with `--aosp --cleanup=false` for app-owned Java. CI includes the pinned
formatter URL/checksum and exact command. Preserve the upstream notices; contribute
only code and assets you have the right to distribute under compatible terms.
Use the strict Compose check before opening a pull request.

```sh
./gradlew detekt lintDebug testDebugUnitTest verifyRoborazziDebug assembleDebug --stacktrace
```

The build uses JDK 25. Ktlint and the general Detekt CLI use a Gradle-detected
JDK 21 because their IntelliJ parsers call a JVM API deprecated in JDK 24.
Set `CALLTRACK_ANALYSIS_JAVA_HOME` to the JDK 21 home if Gradle does not
discover it. The `detekt` task includes ktfmt, ktlint, general Kotlin rules
and the Compose and formatting rule sets. See the [Kotlin style guide](conductor/code_styleguides/kotlin.md).

Change AndroidX and Kotlin versions in `gradle/libs.versions.toml`. The Compose
BOM selects Material 3 and the Android Compose libraries. Renovate groups these
updates and waits three days. The weekly audit runs
`./gradlew lintDebug -PstrictVersions=true` to check the published versions.

For an intentional visual change, record the affected screenshot with
`./gradlew :app:recordRoborazziDebug --tests '*ManualCalendarScreenshotTest'`.
Review the image in `app/src/test/screenshots`, commit it with the UI change,
then run `./gradlew verifyRoborazziDebug`. The required GitHub Actions
`slice` check runs screenshot verification on every pull request.

Describe behaviour changes and their regression evidence in the pull request.
Keep language/UI migrations separate from synchronization behaviour changes.

## Testing and evidence

Prefer pure JVM tests and synthetic inputs; retained Java tests deliberately
exercise public Kotlin fields/static entry points. Meaningful behaviour changes need a
failing contract/regression test before implementation. Documentation needs link,
source and consistency checks. Test the failure path as well as success: lost
provider readback, interrupted writes, changed events, permissions and migrations.
Link the relevant hosted CI runs and exact candidate revision with the change.

For the dedicated parity emulator, use the source-bound build followed by:

```sh
.venv/bin/python scripts/qualify_parity_emulator.py --serial emulator-5582
```

This requires the exact dedicated AVD described in Track4, and refuses physical or
unrelated devices. This command covers store contracts; provider, worker, lifecycle, telephony and
rendered UI use separate harnesses in the [qualification instructions](qualification/README.md). Keep physical-call acceptance pending as directed by the
user. The separate `qualify_slice_device.py` harness grants calendar permissions
and creates a synthetic LOCAL calendar; run it only on an explicitly authorised
test device. Follow its fixture recovery receipt if cleanup fails. Never target a
personal calendar or use real call history as a test fixture.

## Issues and reviews

Use the [bug template](.github/ISSUE_TEMPLATE/bug_report.md) or
[research proposal](.github/ISSUE_TEMPLATE/research_proposal.md).
Include commit/version, OS/API, device or emulator category, synthetic reproduction,
expected/actual behaviour and redacted result counts. Separate local persistence,
cloud synchronization and operational acceptance. Never attach call logs,
calendar dumps, database files, APK analysis, tokens or signing keys.

For a suspected vulnerability, follow [security reporting](SECURITY.md). Source
publication, issue submission and messaging other maintainers are separate actions;
these instructions do not submit anything automatically.

## Compose UI tests

Compose coverage lives in the app suites. Run the affected source-bound tests and
retain the applicable device, accessibility and recovery acceptance gates.

## Dependency trust changes

Ordinary builds enforce `gradle/verification-metadata.xml`, including repository
metadata. Unknown or altered entries fail closed. See the
[qualified trust procedure](qualification/release/trust/README.md) before changing
dependencies or regenerating checksums. A cache-generated checksum is not approval.
The current evidence covers selected macOS scopes; hosted/Linux execution remains
an independent qualification gate.
