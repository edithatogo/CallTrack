# Release requirements and acceptance mapping

Date: 2026-10-21. R13/R27 are only partially satisfied. Discovery and contributor
preparation can proceed before M3; release acceptance cannot. No requirement is
waived by documenting a deferral.

| ID | Current behaviour | Required evidence and owner | Disposition |
|---|---|---|---|
| D01 / R13 | Source provenance, GPL text and root build instructions exist | Contributor guide plus exact release source, notices/assets/transitive licence audit; release owner | Source/notices, runtime/build-tool declarations and origin audits delivered; inherited asset rights and final corresponding-source/conveyance clearance pending |
| D02 / R13 | Debug source-input/APK receipts; no bitwise comparison | Clean committed source, resolved dependencies/checksums, toolchain, unsigned two-environment comparison, final signed artifact hash/certificate; release owner | 5578bf5 clean unsigned builds match;115 JVM tests each;736 binary/metadata entries enforced and fault-tested; independent environment/production identity pending |
| D03 / R13 | Disposable package, debug certificate; no signing config/remote | Choose owned package/name/icon, key custody/recovery and channel continuity; same-key upgrade plus unsupported-key rejection | Development identity retained by user choice; development-key upgrade and disposable wrong-key rejection probe; permanent identity/production key pending |
| D04 / R13 | Track4 selected API36 emulator P01–P10 accepted | All M3 requirements, controlled emulator suite, real-call and provider-specific evidence where claimed | Selected emulator accepted; physical/cloud/wider-platform acceptance pending |
| D05 / R13 | CI definitions, no observed hosted runs | Fresh contributor build and hosted workflow at candidate commit; supported API/device coverage | Local tooling tested; external build pending |
| D06 / R27 | Private/free/no reminders, backup exclusions; receipt retention | Actual payload retention, user-visible consent/review, logical vs forensic deletion and reset safeguards | Track4 retention/review/reset controls accepted on selected emulator; forensic erasure unclaimed |
| D07 / R13 | No production upgrade/downgrade test | Same-identity migration with edited/deleted events, interrupted operations and recovery; no blind downgrade | Development10→11 schema1–5→6/import1→2 and LOCAL edited/deleted/ambiguous/recovery paths pass15 native invocations; real supported production baselines/permanent-certificate routes pending |
| D08 / R13 | No alpha observation | After controlled acceptance, seven actual days on selected devices; record denominators, misses/duplicates and latency, no personal content | Proposed gate, zero elapsed days claimed |
| D09 / R13 | Play/F-Droid/Git hosting not configured | Dated channel policy check, permission declaration if applicable, signing/build recipe and real channel outcome | Research complete; submissions deferred |

Use [release runbook](../../../RELEASING.md) for execution. Stop promotion on any
unresolved duplicate/data-loss/privacy issue, failed cleanup, source mismatch,
unsupported migration or missing evidence. “Builds successfully”, “passes emulator
store tests” and “ready for routine automatic logging” are distinct claims.
