# Review Report: Track5 local release continuation

## Summary

Local release tooling and evidence reviewed with fixes; original M4 remains incomplete.

## Verification Checks

- [ ] **Plan Compliance**: Partial - local unsigned/development qualification only; original D01–D09 acceptance retained.
- [x] **Style Compliance**: Pass - standard-library scripts, native Gradle/SDK tooling, no production runtime dependencies or app changes.
- [x] **New Tests**: Yes - signature classification, canonical/module URL binding, mismatch/evidence preservation, emulator-only target, exact rejection reason and cleanup fault paths.
- [ ] **Test Coverage**: Partial - actual development upgrade/private-state readback plus host guards; production/provider-preserving upgrades remain separate.
- [x] **Test Results**: Passed -105 host tests;152-document/20-track/25-scenario verifier; four new native invocations, successful development10→11 replacement, two expected install rejections and cleanup readback. Existing115 JVM tests per unsigned build and prior native matrix are reused, not rerun.

## Findings

### Medium — Unsigned state was asserted without a signature-tool gate (fixed)

- **File**: `scripts/qualify_release_build.py`.
- **Context**: The runner labelled its artifact unsigned based on its build task/output path. A later signing configuration could invalidate that claim.
- **Suggestion**: Applied SDK apksigner readback; verified signatures and unrelated tool errors fail closed. Both retained exact candidate APKs have the expected missing-signature result. Two regression tests cover classification.

### Medium — Gradle resolved filename is not necessarily the repository filename (fixed)

- **File**: `scripts/audit_release_artifacts.py`.
- **Context**: Three KMP-published Android AARs are renamed by Gradle module metadata; naive canonical URLs returned404. The first failed audit is retained.
- **Suggestion**: Resolve the unique published file matching name and SHA256 through the fresh module metadata; reject unsafe/ambiguous paths. The corrected full63-artifact audit matches. Mismatch/error exits nonzero and retains evidence.

### Medium — Guest/package ownership needed rechecking after building (fixed)

- **File**: `scripts/qualify_release_upgrade.py`.
- **Context**: A long build separates preflight from installation. A changed guest or new existing installation must not be overwritten.
- **Suggestion**: Recheck exact dedicated AVD, healthy boot/phone service and both absent packages immediately before installing. Cleanup acts only on attempted owned fixture packages, continues after failures and requires absence readback. Host fault tests cover cleanup failures.

### Medium — Current release status was obscured by historical documentation (fixed)

- **File**: `RELEASING.md`, Track5 `verification.md`, `requirements.md`.
- **Context**: Current headings still described the110-test Java candidate and reproduction as unrun despite later115-test Kotlin qualification.
- **Suggestion**: Add a current checkpoint and retain explicitly historical records; distinguish same-host equality, repository-byte agreement, development signing and full production readiness.

## Evidence boundaries

Implementation commits547c869,b3774ac,77f1533 plus the final review-fix commit in the
plan. Reviewed all changed scripts/tests and the source/artifact/asset manifests;
no app source/build input was changed. The initial version overlay failed before
installation because projectsEvaluated ran after DSL finalization. It was corrected
to the AGP finalizeDsl callback and a fresh evidence directory; no failure was erased.
See [AGP API source](https://android.googlesource.com/platform/tools/base/+/381f2e356d50b65eb28fc6ef97c629eaed3afe08/build-system/gradle-api/src/main/java/com/android/build/api/variant/AndroidComponentsExtension.kt).

No unresolved implementation finding authorises production promotion. Remaining
licence/tool-trust, independent reproduction, supported provider/schema upgrade,
physical/cloud and operational gates are requirements, not waived warnings.

## Final decision

Pass for the bounded local continuation, with the documented fixes applied.
[Native upgrade evidence](development-upgrade-evidence-20261021.json) records the
exact77f1533 probe, overlay/artifact/certificate/log hashes and independent final
native readback. The final cleanup refactor only exposes the same reverse-order
uninstall/absence-readback loop for three fault tests; the actual native run used
the pre-refactor equivalent. The audit CLI nonzero-exit correction is likewise
covered separately from the retained successful full audit. No full-M4 acceptance,
independent-host build, supported production upgrade or publication is inferred.
