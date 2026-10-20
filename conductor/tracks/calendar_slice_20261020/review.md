# Review Report: Call-to-calendar vertical slice

## Summary

Review passed with all five findings fixed; final source-matched build/native acceptance and cleanup passed.

## Verification Checks

- [x] **Plan Compliance**: Yes — discovery, decisions, implementation, device evidence, recovery and handoff complete within the bounded manual slice.
- [x] **Style Compliance**: Pass for new Java under pinned AOSP formatter; inherited source preserved.
- [x] **New Tests**: Yes — mapping/retry/readback/recovery contracts, synthetic native fixture and host safety tests.
- [x] **Test Coverage**: Yes for the bounded manual synthetic slice and its recovery paths; live calls, background parity, remote sync and release acceptance are downstream.
- [x] **Test Results**: Passed — 16 JVM tests, 16 host tests, five final instrumentation invocations, zero lint errors, formatting/link/source checks.

## Findings

### High — Uncertain installation outcome bypassed package cleanup

- **File**: `scripts/qualify_slice_device.py`, install/cleanup lifecycle.
- **Context**: If ADB timed out after installing, the attempted package was absent from the cleanup list. The harness could incorrectly report package removal without checking it.
- **Suggestion**: Record ownership before attempting install; attempt each uninstall independently and require native package absence readback. Regression first failed in `.local/slice-review-red.log`; fix passes host regression.

### Medium — Asynchronous calendar refresh can apply stale selection

- **File**: `app/src/main/java/com/markobl/calllogsync/slice/CalendarSliceActivity.java`, refresh lifecycle.
- **Context**: Overlapping refresh callbacks can apply an old saved selection after a newer selection/request; picker stays active during refresh.
- **Suggestion**: Disable selection/logging while loading, invalidate stale callbacks and retain the explicit placeholder when the destination is missing. Fixed; saved/missing destination UI checks passed on final native artifacts.

### Medium — Backup flag alone is insufficient for device transfer policy

- **File**: `app/src/main/AndroidManifest.xml`, application backup configuration.
- **Context**: Android documentation states some manufacturers allow device transfer despite allowBackup=false. This is inappropriate before restore/source-identity semantics are qualified; lint also requests explicit data extraction rules.
- **Suggestion**: Add explicit cloud/device transfer exclusions for all app domains. No backup or device-transfer operation should be executed on personal data for this slice.

### Low — Call provider limit should use its documented URI parameter

- **File**: `app/src/main/java/com/markobl/calllogsync/slice/AndroidCallLogSource.java`, query construction.
- **Context**: Consuming only the first row is safe at the mapper boundary, but relying on generic bundle limit support is weaker than the provider's documented LIMIT_PARAM_KEY.
- **Suggestion**: Add the CallLog-specific URI limit; retain deterministic date/ID sorting. Personal call-source runtime acceptance remains Track 4.

### High — Uncertain readback could permit a blind duplicate retry

- **File**: `app/src/main/java/com/markobl/calllogsync/slice/SliceEngine.java`, insert/readback/receipt ordering.
- **Context**: An accepted insert followed by missing/mismatched readback or a provider exception left no known-ID receipt. If marker lookup also failed, a second attempt could insert again.
- **Suggestion**: Persist the known insert ID as an attempt receipt before verification, commit it even when readback fails, and require actual field verification on every later success. Regression proved two inserts before the fix (`.local/slice-readback-red.log`); all 16 expanded JVM contracts now pass directly. Final Android build and native receipt passed.

## Review scope and evidence

Initial implementation commit: b2e105d (80 files, 4,111 inserted lines including adopted upstream). Review is iterative: verify inherited files by hash against the audited source, then examine each new adapter/domain/UI/test/harness file and manifest/build/CI changes. The reference comparison found only manifest and strings changed among pre-existing main-source files; seven slice classes are new. No original CallTrack implementation is incorporated.

No subagent review, hosted CI, store approval, cloud sync, actual call reading or user-consent walkthrough is claimed. Final dispositions, tests and commit pointers will be added after verification.

## Final dispositions

- 66ca1e7 fixes uncertain installation cleanup, stale calendar refresh, explicit transfer policy and provider-specific query limit.
- a7e54a5 fixes uncertain readback receipt retention, with regressions first failing and then passing.
- b2e105d is the initial source adoption/manual-slice implementation checkpoint.
- Final evidence.json and verification.md record all native and local results. No critical/high/medium findings remain open for this track. Residual production/parity limitations are explicitly handed to their owning tracks.
