# Verification and evidence boundaries

Research baseline `2d2de24`, activation `9968b1b`, date2026-10-21.

- Red: `JAVA_HOME=<JDK17> bash ./gradlew -p prototypes/multiplatform-ui test
  --console=plain` failed with only unresolved CompanionScreen after correcting a
  test import. No screen existed yet. Initial direct wrapper execution also failed
  because it is not executable; documented commands use bash consistently.
- Green: same command plus `jar` passed. Three desktop rendered UI tests, zero
  failures/errors/skips: destination gating, preview invalidation and capability
  disclosure. Headless Java/offscreen renderer; no visible window or native app
  acceptance was observed. Build duration21 seconds includes warm caches.
- Runtime graph command: `dependencies --configuration runtimeClasspath` passed.
  Full local logs under excluded `.local/multiplatform-ui-evidence/`; tracked
  source-bound hashes and graph digest in evidence.json.
- Host suite initially found an incomplete documentation link to this verification
  file during authoring. File now exists; final host/link results recorded below.

JAR build is not an installer or self-contained distribution. No iOS framework,
shared Android/iOS UI execution, manual accessibility, native packaging, provider,
permissions, persistence, import parser or cloud sync was tested. No production
runtime code or root build configuration changed. Existing Android evidence is
historical and does not qualify shared UI. No phone/emulator installation occurred.

Final review rerun:4 desktop UI tests passed including reselect-without-stale-preview;
25 host tests passed. ktfmt0.59 non-mutating check and git diff whitespace check
passed; Conductor97 documents/13 tracks/25 scenarios and private-source digests
passed. Source/test-report hashes refreshed after review. No root runtime or
production build selection changed; product identity/production stack/guidelines
therefore need no adoption change. Research outcomes are in requirements/roadmap.
