# Track 3 findings — 2026-10-20

## Contracts and source provenance

Pinned upstream remains aaa56443a294aa61bad48b620427e53f9521c5fe (GPL-3.0); root adoption uses the Track 2 modern toolchain. Root source is a development fork; reference clones remain unchanged.

[Calendar provider guide](https://developer.android.com/identity/providers/calendar-provider), [Events contract](https://developer.android.com/reference/android/provider/CalendarContract.Events), and [CallLog contract](https://developer.android.com/reference/android/provider/CallLog.Calls), consulted 2026-10-20, define the native adapter boundaries. Calls expose row identity, cached name, presentation, date and seconds duration. Cached names do not require a separate contacts lookup. The manual source will sort by date/ID descending and consume one row. Restricted presentation masks cached identity. No historical sweep is introduced here.

Normal event inserts use public application fields; custom app package/URI form the recovery marker. Sync-adapter-only fields are excluded. Calendar selection requires explicit choice, visibility and contributor-or-higher access. Provider operations run off the UI thread. A dedicated LOCAL test calendar gives native persistence evidence without cloud writes. Fixture provisioning alone uses sync-adapter parameters; its matching account name AND LOCAL account type scope cleanup.

[Microsoft Outlook Android support](https://support.microsoft.com/en-us/outlook/how-do-i-sync-my-outlook-calendar-to-the-default-calendar-app-s), consulted 2026-10-20, documents optional two-way calendar export for supported accounts. Therefore the same native adapter can address an Outlook-backed device calendar when it is exposed and writable. Account presence, policy eligibility, enabled export and remote completion must be verified separately. This track does not alter Outlook settings or infer cloud success.

## Options and dependencies

| Option | Scope / evidence | Decision |
|---|---|---|
| Calendar intents | Supported user-confirmed event form; delegates save | Retain as future UX option; insufficient for readback/recovery and future automatic logging |
| Native CalendarContract | Required local-provider operations; existing platform API, no added runtime library | Adopt for bounded slice |
| Existing upstream HTTP sync | Posts history to external endpoint; no local calendar sink | Retain source provenance, disable legacy entry points in this dev app |
| [ical4android](https://github.com/bitfireAT/ical4android) | Repository explicitly superseded by synctools | Reject adding superseded dependency |
| [synctools](https://github.com/bitfireAT/synctools) | DAVx5 sync/iCalendar tooling, broader than one local event | Defer; integration track must evaluate licence, pinned version, Android build and takeover scope if CalDAV needed |
| [ical4j](https://github.com/ical4j/ical4j) | iCalendar model/parser; 4.x requires Java11+, not a substitute for Android permission/provider access | Defer until ICS import/export need; no speculative parser dependency |
| SQLiteOpenHelper | Native persistence sufficient for identity/event receipts and writer transaction | Adopt v1; Kotlin/parity tracks compare Room and migration benefits |
| Room/KMP persistence | More tooling/schema features but requires new build/runtime choices | Defer comparison to durable-sync/Kotlin/KMP tracks |

No prototype dependency is installed. The prototype is the actual slice, exercised through synthetic ports and a bounded native fixture. The adopted native components are reviewed at M3 (background recovery/restore identities) and M5 (migration versus library replacement). Retain v1 ledger data; do not destructively recreate it during future migrations. A decision to defer a wrapper is not a claim that its entire dependency tree has been qualified.

## Acceptance boundaries

Exact millisecond starts and safe duration arithmetic replace legacy rounding/overflow behaviour. Zero-duration events provisionally occupy one minute while describing zero actual duration. Per-install namespace plus row ID and destination identify a receipt. Before success, compare actual provider fields. A provider marker closes the insert-before-ledger gap; conflicting, missing or edited records require review. This is single-device manual operation, not cross-device deduplication or full CallTrack parity.

Native evidence, tests, package cleanup and remaining gaps are recorded in verification.md and evidence.json.
