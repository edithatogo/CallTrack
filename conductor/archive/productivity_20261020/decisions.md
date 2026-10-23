# Dependency decision PROD-D01

- Owner/date/refresh: Track11,2026-10-21. R18/R21–R23/R27; J01–J12;
  [findings](findings.md), [verification](verification.md).
- Problem: useful journal/productivity features must preserve authored content and
  avoid silently expanding retention, permissions, cloud disclosure or app scope.
- Existing code assessed: SQLiteOpenHelper receipt/outbox, CallRecord/CallEvent
  mapping and calendar edit protection; none is an annotation store. Calendar notes
  are user/provider-owned, not an app journal to rehydrate automatically.
- Options: retain narrow current app; separate small annotation repository with
  native queries; Room/FTS takeover; AppSearch derived index; maintained CSV/ICS
  serializer; SAF/native reminder facilities; general rules/crypto/runtime libraries.
- Versions/licences/matrix: findings records stable/preview candidates, current
  versions and unresolved artifact compatibility/licence gates. No resolved graph
  or performance claim; no production or prototype dependency selected.
- Intended role: all new dependencies deferred candidates; native facilities first
  when they meet actual requirements. Libraries may replace app-owned SQL/parsing,
  but may not become a competing ledger, canonical journal or scheduling owner.
- Prototype hypothesis/effort: a toy note/search/export implementation would not
  validate product priority, retention/recovery policy or real migration. No
  prototype selected; run a bounded synthetic one only after a slice/workflow and
  ambiguous dependency need are identified. No fake results presented as acceptance.
- Contracts/error/security/upgrade: J01–J12 define note durability, deterministic
  routing, denied reminders, partial export, corrupt/duplicate restore and index
  purge cases. These are specifications, not executed behavior.
- Decision: **defer production productivity features and dependency adoption**;
  retain current app and record prioritized candidate slices. Track4/5 completion
  and validated user workflow examples gate implementation. Research closure does
  not imply validated personal priority or delivered R18/R21/R22/R23/R27 features.
- Temporary dependency expiry: none exists. At reopening refresh releases and
  native support; isolate any trial and remove it if it cannot demonstrate material
  benefit. Promotion needs actual source-bound tests, migration/recovery, notices
  and removal of duplicated responsibilities; cross-platform support is separate.
- Migration/coexistence/rollback: none performed. Future annotation schema is
  versioned and independent of receipt payload purge. Preserve existing schema1/2
  identities and user edits. Rehearse downgrade/recovery without data loss; never
  restore secrets or auto-recreate external events. Do not enable backup implicitly.
- Evidence status: source/documentation/host checks only; no journal/backup/search/
  reminder runtime acceptance, notifications, account changes or personal data used.
- Follow-up: reopen for chosen first slice after user workflow/retention decision
  and Track4/5 gates; Track10 routes, Track12 companion, Track13 parser/crypto reuse
  evidence inform later design without becoming mandatory dependencies.

Review refinement: restored receipt metadata remains inactive historical evidence
until destination/ownership reconciliation; imported hashes do not grant remote
write authority or establish current delivery. No restore engine is implemented.
