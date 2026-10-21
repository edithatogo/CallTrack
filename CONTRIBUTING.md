# Contributing

This is a development fork with manual/automatic logging, historical import and
recovery controls. Native acceptance is scoped to recorded emulator evidence. Check [Conductor](conductor/index.md) before proposing changes.
The current package is disposable; there is no supported public release yet.

## Reproduce the development build

Use a fresh checkout of the exact commit being reviewed. Install an OpenJDK17
runtime, Python3 standard library and Android SDK platform36/build-tools35.0.0.
The wrapper pins Gradle8.13 and its distribution checksum; AGP is 8.13.2.
The Kotlin implementation retains one Java nullable-label bridge and Java interop
tests, pins Kotlin/KGP2.3.21 and targets Java8 bytecode.
Set JAVA_HOME and ANDROID_HOME to those installations. SDK licences must be accepted
by the person operating the build environment. Run from the repository root:

```sh
python3 scripts/run_slice_build.py --jdk "$JAVA_HOME" --sdk "$ANDROID_HOME"
python3 -m unittest discover -s tests -v
python3 scripts/verify_conductor.py
```

No private APK or reference checkout is needed for the root build. Private-source
verification is an optional maintainer audit, not a contributor prerequisite.
Build receipts and logs are excluded under `.local/slice-evidence/`. Check the exit
status, input hashes, test results and artifact hashes together. A successful debug
build establishes neither a production signature nor bit-for-bit reproducibility.
See [qualification](qualification/README.md) for the separate upstream reproducer.

Use google-java-format1.24.0 with `--aosp` for app-owned Java. CI includes the pinned
formatter URL/checksum and exact command. Preserve the upstream notices; contribute
only code and assets you have the right to distribute under compatible terms.
For Kotlin use ktfmt0.59 with `--kotlinlang-style --dry-run --set-exit-if-changed`;
CI pins its Maven URL and SHA256. Follow [Kotlin style](conductor/code_styleguides/kotlin.md).
Describe behaviour changes and their regression evidence in the pull request.
Keep language/UI migrations separate from synchronization behaviour changes.

## Testing and evidence

Prefer pure JVM tests and synthetic inputs; retained Java tests deliberately
exercise public Kotlin fields/static entry points. Meaningful behaviour changes need a
failing contract/regression test before implementation. Documentation needs link,
source and consistency checks. Test the failure path as well as success: lost
provider readback, interrupted writes, changed events, permissions and migrations.
The CI workflow is configured, but no hosted run has been observed in this project.

For the dedicated parity emulator, use the source-bound build followed by:

```sh
python3 scripts/qualify_parity_emulator.py --serial emulator-5582
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
[research proposal](.github/ISSUE_TEMPLATE/research_proposal.md). No remote hosting
is currently configured; these templates are preparation for a future repository.
Include commit/version, OS/API, device or emulator category, synthetic reproduction,
expected/actual behaviour and redacted result counts. Separate local persistence,
cloud synchronization and operational acceptance. Never attach call logs,
calendar dumps, database files, APK analysis, tokens or signing keys.

For a suspected vulnerability, follow [security reporting](SECURITY.md). Source
publication, issue submission and messaging other maintainers are separate actions;
these instructions do not submit anything automatically.

## Isolated Compose research

The [standalone synthetic prototype](prototypes/compose/README.md) is not part of
the root app dependency graph. Its optional build uses
`python3 scripts/qualify_compose_prototype.py build --jdk "$JAVA_HOME" --sdk "$ANDROID_HOME"`.
Device mode accepts only the dedicated emulator; it installs a separate no-data
fixture app and verifies cleanup. It is not a way to try Compose against personal
calls/calendars. Production UI adoption remains a separate behavioural/accessibility
gate; research archive status must not be read as a migrated working screen.
