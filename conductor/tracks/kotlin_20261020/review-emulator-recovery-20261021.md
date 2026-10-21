# Review Report: Track6 emulator recovery and remaining acceptance

## Summary

The requested emulator acceptance work passes after scoped infrastructure repair and a tested preflight fix, while original release/operational gates remain open.

## Verification Checks

- [x] **Plan Compliance**: Yes for this resumption - no computer restart; remaining four suites executed; failed evidence and original AVD data preserved.
- [x] **Style Compliance**: Pass - small shared read-only boundary, existing Python conventions; no app source/rendering change.
- [x] **New Tests**: Yes - three negative preflight regressions fail on the old harness and pass with826ccaf.
- [x] **Test Coverage**: Yes for changed preflight/call sites - lifecycle, telephony and UI rejection before installation; physical/wrong-target and cleanup safety checks remain covered.
- [x] **Test Results**: Passed -92 host tests; four remaining native suites (28 successful invocations plus one expected termination); consolidated exact-build matrix89 successful plus one expected termination.115 JVM/unsigned build results remain valid at unchanged app inputs.

## Findings

### Medium — Boot completion did not establish required service availability (fixed)

- **File**: `scripts/parity_health.py` (Lines L4–L10), called from lifecycle/telephony/UI preflight.
- **Context**: The old harness could install fixtures into a guest reporting boot=1 while the phone service was absent; process startup and cleanup then failed. Target validation and immutable APK binding alone did not detect that condition.
- **Suggestion**: Require both boot completion and the phone service after exact dedicated-AVD validation and before installation. Fixed826ccaf. Regression tests verify that the observed phone-missing condition cannot install fixtures. Existing time windows, native assertions, permissions and cleanup guards are unchanged.

## Review and evidence assessment

Reviewed the complete small diff against e92de40, including the helper, all three
call sites and their tests. No hardcoded secrets, personal-call logging, physical
serial access, broad process termination or calendar writes are introduced. The
helper only reads readiness; missing services fail closed before mutation. It is
not represented as proof that every Android component is healthy.

Infrastructure repair is documented separately from application behavior. Host
resource pressure and many stale helpers were observed; only owned duplicate helper
commands were retired with identity checks, keeping newest copies and excluding
main GUI/browser applications. The original stuck AVD/data was preserved. A fresh
AVD on PortableSSD passed actual native acceptance. The failed namespace experiment
and prior failures remain failures. This does not establish a permanent solution to
the desktop's helper-process lifecycle or a single root cause.

Validated source/APK binding and recorded reuse:61 earlier matching invocations,
29 newly executed (28 successes and one deliberate process termination). The
lifecycle receipt includes verified actual reboot/held termination. Every suite
reports package/fixture cleanup and restoration; final direct native readback
confirms empty providers/packages, baseline power/network state, non-root ADB and
phone-service availability. Eighty screenshot/text artifacts are hashed; rendered
settings and recovery previews show readable controls and explicit approval.

No unresolved Critical/High code finding remains in this repair scope. No app
source or APK changed, so rerunning unchanged JVM/release packaging was unnecessary;
115 JVM tests and the identical two-clean-checkout unsigned artifacts retain their
original bounded evidence. The complete host suite was rerun after documentation.
Original M4/full-M5, production identity/signing, independent trust/reproduction,
physical/cloud/wider-device/manual accessibility and real observation gates remain.
Track6 is not archived and later frameworks are not qualified by this repair.
