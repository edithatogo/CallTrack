# Verification: build qualification

Final structured results, artifact digests and device output are in [evidence](evidence.json). Private complete logs are retained under `.local/build-evidence`; personal phone/call records were not queried. Reproduction commands are in [qualification README](../../../qualification/README.md).

## Executed sequence

1. Run original Gradle7.3.3 wrapper/JDK11; initial failure was missing SDK location.
2. Provision SDK32/build-tools30.0.3; run original assembleDebug, testDebugUnitTest, lintDebug and dependencies: successful, tests NO-SOURCE,0 lint errors/25 warnings.
3. Run `python3 scripts/run_qualification.py --variant upstream --jdk <JDK11> --sdk <SDK> --clean`: successful; APK hash identical to first original build.
4. Prepare modern target36 copy with namespace/Gradle/AGP/JDK changes; build app and test APK, lint: successful, but nine Fragment custom lint checks skipped. Record as incomplete analysis until repair.
5. Cold launch modern app on Android17; run denied/granted/revoked zero-row instrumentation checks; all passed; test packages removed. This pre-repair receipt remains separately identified.
6. Add Fragment1.5.1 targeted override; repeat clean build, lint, artifact checks and device checks on repaired version. Final outcomes recorded in evidence before completion.
7. Run tooling fault-injection tests, document/source checks, formatter and whitespace checks; review and archive only after these and artifact/device cleanup are proven.

## Limits

Same-host repeatability is narrower than independent-host reproducibility. Native launch/provider permission evidence is narrower than real logging or calendar sync. Tests deliberately read no call-history rows and change no calendar data. No app unit-test coverage exists in the inherited source. Lint warnings remain tracked; passing lint does not certify application security. CI definition is prepared locally; no hosted run executed.

## Handoff

Proceed with calendar_slice using the selected fork/toolchain. Use the denied/granted harness as a bounded provider check; add actual feature tests in that track. Revisit source identity, defaults, app lifecycle, token/HTTP safety and background behavior mapped in Track1 before enabling sync on personal data. Release controls remain with the release track.

## Final verification record

Both final modern build receipts returned0; the fresh-preparation repeat produced the identical app and test APK hashes as the repaired clean build. The final device transcript tests exactly those hashes. All three permission cases passed and both test packages were removed with successful package-list absence checks. The final source-input digests still match current files. Modern lint:0 errors/30 warnings, no ObsoleteLintCustomCheck registry skip. Original lint:0 errors/25 warnings. Same-host repeatability was verified for both variants.

`python3 -m unittest discover -s tests -v`:12 tests passed. Formatter, YAML matrix parse, Conductor/source-digest validation and `git diff --check` passed. Inherited Android app unit task remains NO-SOURCE; instrumentation runs are the bounded runtime evidence. Exact commands/artifact hashes/source fingerprints/log digests are preserved in evidence.json.
