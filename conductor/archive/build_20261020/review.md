# Review Report: Fork feasibility and build qualification

## Summary

The fork has a repeatable build and bounded Android17 launch/permission evidence; dependency-analysis and harness-reporting findings were fixed before archive.

## Verification Checks

- [x] **Plan Compliance**: Yes — original and modern build qualification, fork comparison, dependency/custom-code decisions, tooling/CI commands, package/signing strategy and bounded device checks are documented.
- [x] **Style Compliance**: Pass — general style guide, product privacy principles and source provenance reviewed; new Java passes pinned AOSP formatter.
- [x] **New Tests**: Yes — seven qualification guard tests plus three native permission scenarios; existing five document-verifier tests retained.
- [x] **Test Coverage**: Partial — qualified build/guard/provider boundaries tested; inherited app unit-test task is NO-SOURCE and full logging/calendar/background behaviour is not claimed.
- [x] **Test Results**: Passed — see final evidence and verification record; no lint errors or skipped Fragment custom registry after repair.

## Findings resolved

### Medium — Modern lint skipped inherited Fragment checks

- **File**: generated modern `app/build.gradle`, reproduced by `scripts/prepare_qualification.py`.
- **Context**: The old Fragment1.4.1 custom registry was incompatible with new lint. Upgrading Fragment to1.5.1 then exposed duplicate classes against ViewModel KTX2.3.1.
- **Fix**: Pin interim Fragment1.5.1 and align ViewModel KTX2.5.1. Clean build, lint and native checks repeated; record expiry/review triggers rather than endorsing these old versions indefinitely.

### Medium — Device report could retain passed status when cleanup failed

- **File**: `scripts/qualify_device.py`, finally/receipt logic.
- **Context**: Empty output from a disconnected ADB path check could be mistaken for package absence, or a passed test result could coexist with unsuccessful cleanup.
- **Fix**: Verify successful package-list command and exact package absence, mark overall result failed when cleanup is unverified, and raise. A fault-injection test simulates disconnect and proves the failure status.

### Medium — Build identity needed stronger artifact linkage

- **File**: `scripts/run_qualification.py` and `scripts/qualify_device.py`.
- **Context**: Package identity alone did not prove the installed bytes came from a successful qualified build.
- **Fix**: Record input/source/artifact digests, check clean audited reference, require a successful build receipt and matching APK hashes before installation; inspect APK package identity and refuse replacing any existing test package.

### Low — Preparation depended on a live checksum lookup

- **File**: `scripts/prepare_qualification.py`.
- **Context**: Fetching the official checksum on every preparation added avoidable network dependence and left the exact expected digest out of source.
- **Fix**: Pin the verified Gradle8.13 SHA256 in preparation code; wrapper verifies the downloaded distribution. Fresh preparation reproduces changed build/manifest inputs.

### Low — Documentation verifier's test fixture omitted new linked root docs

- **File**: `tests/test_verify_conductor.py`.
- **Context**: Track verification now links to qualification/README outside conductor; the temporary test tree copied only conductor and therefore reported a false broken link.
- **Fix**: Copy the small qualification tree into the isolated test root; production link checks remain enabled.

## Scope and method

Reviewed from track-start2c3964a through implementationeec3a32 and subsequent fixes. Included Conductor documents because these are required deliverables, plus all new Python/Java/CI/provenance files. Checked source pinning, non-destructive guards, no-row provider selection, launch evidence, failure propagation, cleanup, signing boundaries, fixture tests, resolved dependencies and archive-link continuity. GPL text and upstream-source attribution retained. Self-review only; no independent reviewer or hosted CI run claimed.

Remaining lint warnings, old runtime library versions, real call capture, normal permission UX, background recovery, minimum-OS support and release identity/security/licence approval have explicit downstream owners. They are not silently promoted to proven production behaviour. No unresolved blocking finding remains in this qualification scope.
