# Coverage-guided call-data fuzzing

`CallDataFuzzer` exercises the compiled Android app's call-to-event conversion,
restricted-identity handling, stable signatures and import-date parsing with
Jazzer/libFuzzer. It uses synthetic input only. This bounded run supplements the
existing Android, accessibility and recovery acceptance tests; it does not fuzz
the Android providers or prove the absence of defects.

CI uses the SHA-256-pinned Jazzer 0.30.0 Linux release, the existing Gradle
dependency verification and JDK 25. To reproduce locally, verify the appropriate
[Jazzer release](https://github.com/CodeIntelligenceTesting/jazzer/releases/tag/v0.30.0),
set `JAZZER_HOME`, `JAVA_HOME`, `ANDROID_HOME` and a private `FUZZ_OUTPUT_DIR`,
then run `sh fuzz/run-jazzer.sh`. Each run creates a fresh corpus and retains any
crashing input and Java reproducer in its output directory. Treat crash inputs as
untrusted and review them before committing a minimized regression fixture.
