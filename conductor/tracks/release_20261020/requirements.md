# Release requirements and acceptance mapping

Date: 2026-10-22. R13/R27 are only partially satisfied. Discovery and contributor
preparation can proceed before M3; release acceptance cannot. No requirement is
waived by documenting a deferral.

| ID | Current behaviour | Required evidence and owner | Disposition |
|---|---|---|---|
| D01 / R13 | Source provenance, GPL text and root build instructions exist | Contributor guide plus exact release source, notices/assets/transitive licence audit; release owner | Uncertain artwork removed; active translations preserved; exact app source,63 published dependency source/POM archives and notices candidate prepared/read back; final signed/channel corresponding-source conveyance remains pending; full upstream dependency rebuilding unqualified |
| D02 / R13 | Debug source-input/APK receipts; no bitwise comparison | Clean committed source, resolved dependencies/checksums, toolchain, unsigned two-environment comparison, final signed artifact hash/certificate; release owner | 9464408 Mac/native separate-host Linux unsigned builds match exactly with fresh Linux tools/caches;115 JVM tests each;739 verification entries; independent-environment comparison passes, production identity/signing pending |
| D03 / R13 | Disposable package, debug certificate; no signing config/remote | Choose owned package/name/icon, key custody/recovery and channel continuity; same-key upgrade plus unsupported-key rejection | Development identity retained by user choice; development-key upgrade and disposable wrong-key rejection probe; permanent identity/production key pending |
| D04 / R13 | Track4 selected API36 emulator P01–P10 accepted | All M3 requirements, controlled emulator suite, real-call and provider-specific evidence where claimed | Selected API26/API33 store/provider/worker/UI/accessibility/lifecycle/telephony and development upgrades pass; affected API36 checks pass; physical/OEM/ABI/manual speech/contrast and cloud acceptance pending |
| D05 / R13 | CI definitions, no observed hosted runs | Fresh contributor build and hosted workflow at candidate commit; supported API/device coverage | Fresh repository-only native Linux host build/tooling checks pass; hosted workflow prepared/validated, actual GitHub run and wider supported-device coverage pending |
| D06 / R27 | Private/free/no reminders, backup exclusions; receipt retention | Actual payload retention, user-visible consent/review, logical vs forensic deletion and reset safeguards | Track4 retention/review/reset controls accepted on selected emulator; forensic erasure unclaimed |
| D07 / R13 | No production upgrade/downgrade test | Same-identity migration with edited/deleted events, interrupted operations and recovery; no blind downgrade | Development10→11 schema1–5→6/import1→2 and LOCAL edited/deleted/ambiguous/recovery paths pass15 native invocations on each of API26/API33; real supported production baselines/permanent-certificate routes pending |
| D08 / R13 | No alpha observation | After controlled acceptance, seven actual days on selected devices; record denominators, misses/duplicates and latency, no personal content | Proposed gate, zero elapsed days claimed |
| D09 / R13 | Play/F-Droid/Git hosting not configured | Dated channel policy check, permission declaration if applicable, signing/build recipe and real channel outcome | Research complete; submissions deferred |

Use [release runbook](../../../RELEASING.md) for execution. Stop promotion on any
unresolved duplicate/data-loss/privacy issue, failed cleanup, source mismatch,
unsupported migration or missing evidence. “Builds successfully”, “passes emulator
store tests” and “ready for routine automatic logging” are distinct claims.

Current runtime fixes at6f3381c have separate source-bound emulator evidence. The
unsigned release and source bundle at9464408 retain their original binding;
refreeze and independently reproduce the newer source before release promotion.
See [matrix acceptance](emulator-matrix-qualification-20261022.md).
