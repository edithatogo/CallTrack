# Track 3 verification — completed 2026-10-20

## Authoritative evidence

Implementation b2e105d; review fixes 66ca1e7 and a7e54a5. Final build passed with unchanged inputs; 16 JVM contracts and 16 host tests passed. Final Android17/API37 native acceptance passed and fixture/package cleanup was verified. [Machine-readable evidence](evidence.json) binds the actual input hashes and app/test APK hashes to the native receipt. Lint: zero errors, 30 inherited warnings. New Java formatting and git diff checks passed.

## Acceptance matrix

| Acceptance | Evidence / remaining check |
|---|---|
| S01 explicit picker, stable choice, no fallback | Final native saved-choice restoration and missing-destination placeholder passed |
| S02 bounded source, cached identity and presentation | Source adapter reviewed; synthetic MatrixCursor passed without call-log grant; personal source acquisition remains downstream |
| S03 event mapping and safe time handling | 16 JVM contracts passed, including precision, withheld identity, zero marker and overflow |
| S04 native persistence/readback | Initial synthetic 65-second event retained exact start/end/fields in LOCAL calendar |
| S05 durable retry and crash gap | Initial native reopened SQLite ALREADY_LOGGED; inserted-without-receipt event recovered exactly once |
| S06 denied/unavailable destination | Initial denied/revoked calendar runs, nonexistent and read-only destination passed |
| S07 preserve edits | Native title edit caused REVIEW_REQUIRED and retained edited title; JVM missing/changed/ambiguous cases passed |
| S08 isolation and cleanup | All three native runs removed fixture/packages; category-only report: local1 (test fixture), Google3, Microsoft-like0, other0 |
| S09 Outlook contract | Source-linked capability mapping in findings; no Outlook setting or remote account operation performed |
| S10 test-first/review/archive | Initial javac red then green contracts; host regression red then green; final source-bound build/native verification passed; reviewed and archived |

## Limits and handoff

- Synthetic source injection proves mapper/provider/ledger flow, not live cellular call acquisition or user-consent dialog acceptance. No personal call rows were read, no call-log permission granted and no calls made.
- LOCAL provider persistence does not establish cloud sync. Microsoft-like account categories are heuristics, not provider eligibility or endpoint readback.
- Manual action only. Track 4 owns automatic/background operation, history/import preview, call-type settings, restore identity, tombstones for deleted crash-gap events, data migration and complete parity.
- Full localisation, screen-reader/user acceptance, older-device matrix, signing/upgrades and hosted CI are not claimed here. The scroll/picker/status controls are an initial native UI.
- No runtime framework dependency was added. Direct provider and SQLite choices are reassessed at M3/M5 against actual requirements.
- Schema1 refuses unknown migrations rather than dropping receipts. Disposable fixture/package removal is the verified development rollback; production downgrade/data rollback remains release work.

## Reproduction

Use the root README and scripts/run_slice_build.py, followed by scripts/qualify_slice_device.py with an explicitly selected authorised device. Host tests: python3 -m unittest discover -s tests -v. Conductor verifier: python3 scripts/verify_conductor.py --private-sources when private references are available.

## Review regressions

- Uncertain ADB install: host regression first failed, then passed after tracking attempted package ownership and requiring absence readback. Host suite now 16 tests.
- Uncertain event readback: regression first showed two inserts. Fix retains the known insert ID before verification; every success re-reads actual fields. Expanded 16 JVM contracts pass directly. Final Android build and source-matched device proof passed.
- Explicit transfer exclusions and refresh-generation guard compiled/linted and passed the UI-review native run. The provider category report contains counts only; no account/calendar names were exported.

## Final review disposition and recovery

All five findings in review.md are fixed and committed. The final native run executed five instrumentation invocations: denied calendar access, synthetic cursor mapping, local provider/SQLite/UI roundtrip, explicit fixture absence check and revoked-access denial. Readback assertions include exact millisecond start/end, payload/marker, private/free policy, no reminders/attendees, reopened ledger retry, crash-gap recovery, edit preservation and read-only/nonexistent destinations.

Native acceptance uses the same mapper, sink and SQLite implementation as the manual app, with an injected synthetic source. Its category report was local1 (the temporary fixture), Google3, Microsoft-like0, other0. No Microsoft-like writable calendar was exposed by this query; this is not an Outlook installation/account inventory. No cloud outcome is claimed.

Both development packages were removed without keeping app data. The LOCAL fixture account was removed and queried for absence before uninstall. A future failure retains exact token/evidence and cleanup instrumentation until scoped recovery succeeds.

A true crash before a known insert ID is committed still depends on provider-marker retention. M3 must assess durable pre-insert intents, lost markers, deleted-event tombstones, concurrent workers and restore identity. Normal uncertain readback now retains the ID and prevents blind reinsert.
