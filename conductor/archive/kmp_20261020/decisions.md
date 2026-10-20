# Dependency decision KMP-D01

- Owning track/date/refresh: M7, 2026-10-21. Evidence: [findings](findings.md),
  [requirements](requirements.md), [verification](verification.md).
- Problem: R16/R24/R26 need useful shared logic without destabilizing receipts or
  assuming iOS capture parity. Current owners are JVM mapping, Java orchestration,
  SQLiteOpenHelper, Android providers and Views.
- Options: retain current boundaries (chosen); improve custom JVM code in Track6;
  native companion with duplicated small mapper; KMP extraction with native
  adapters; Room/SQLDelight takeover; Rust/UniFFI core; defer production sharing.
- Versions/licences/matrix: candidate observations and primary links in findings;
  none is a selected compatible artifact graph. Existing Kotlin2.3.21 and Android
  build stay unchanged. Native/Xcode compatibility has not been checked locally.
- Intended role: KMP is a future production candidate, not adopted or installed.
  Shared models/mapping/rules are the first possible scope. Native adapters remain
  responsible for source acquisition, calendars, scheduler, permissions and secrets.
- Costs: compiler targets and Native tooling/packaging; public API/Swift interop;
  replacement of Java cryptography/overflow/locks; migration and database-driver
  semantics; resolved licences and cross-target CI. A second ledger or sync core
  would increase risk and is rejected as an adoption pattern.
- Prototype hypothesis: identical mapping across JVM/Native could reduce future
  duplication. No prototype run now: second-platform scope and source contracts
  are unresolved, and a duplicate toy mapper would not settle those questions.
  No measured speed/size/energy/maintenance result is claimed.
- Contract/error/security/upgrade tests: S01–S08 define gates, including frozen
  identities, uncertain inserts, cancellation, transaction rollback, human edits,
  native packaging and private synthetic fixtures.
- Decision: **retain current implementation; defer production KMP extraction**.
  Close this research phase with justified deferral. R16/M7 implementation and
  equivalent target-test exit gate remain unmet; this is not rejection of KMP.
- Expiry/review trigger: reopen when Track4 stabilizes source/delivery contracts,
  Track6 qualifies the extraction boundary and Track12 selects a feasible workflow.
  A Rust finding may inform the choice but no Rust experiment is a prerequisite.
  Refresh versions at reopening; do not promote this dated shortlist automatically.
- Migration/coexistence/rollback: none performed. Future extraction first uses a
  compatibility facade with identical Java API/hashes, parallel comparison without
  dual writes, then one ledger owner. Preserve old DB/receipt read capability and
  backup recovery; no destructive schema downgrade. Remove superseded code only
  after upgrade/recovery parity. Prototype isolation/removal precedes promotion.
- Actual validation: source inspection and documentation/host checks only; no new
  runtime dependency, module, database write, device install or network capability.
- Handoff owners: Track12 platform capability, Track10 providers/auth, Track13
  Rust component evidence, Track9 UI discovery. Research completion does not waive
  their work or establish a second-platform application.
