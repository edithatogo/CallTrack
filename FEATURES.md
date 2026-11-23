# CallTrack feature status — 24 November 2026

The **shipped app** is a *development build* targeting **Android 17 (API 37)**. A passing emulator or source test does not mean a signed public release. Consult [the active scope](conductor/active-scope.json), [Conductor tracks](conductor/tracks.md) and [release gates](RELEASING.md); historical plans are not evidence that a feature has shipped.

| Capability | Code | Qualification / limitations |
|---|---|---|
| Manual log-latest-call to selected writable calendar | Implemented | Native CallLog/CalendarContract, explicit action, receipt readback; permission gating |
| Automatic call-log capture | Implemented | Incoming/outgoing/missed filters; enable/pause; WorkManager prompt and periodic catch-up; best-effort, not exact scheduling |
| Call event mapping | Implemented | Direction, start/end/duration, timezone, name or privacy-preserving fallback, private/free, no invitations |
| Historical range import | Implemented | Date/type filters, bounded preview, explicit confirmation, pause/cancel, session history and resumption |
| Durable delivery and reconciliation | Implemented | Original source identities, per-destination deduplication, pending attempts, uncertain write review, edit/deletion protection; no silent duplicate insert |
| Recovery and retention | Implemented | Existing event readback, review/suppress/reapprove, bounded payload retention, no auto-overwrite |
| Android permissions/lifecycle | Implemented | Read-call-log, phone state and calendar permissions, revocation, process-death/reboot/Doze/offline recovery tested synthetically |
| Android UI | Partially migrated | Compose screens and panels for manual/automatic/import/recovery; some legacy Activity/View and custom controls remain |
| Local device-calendar integration | Implemented | No CallTrack server/account or direct internet permission; Google/Outlook account sync depends on installed calendar provider |
| Debug/privacy hardening | Implemented | No raw call fields in worker input/logs; backup/data extraction exclusions; synthetic tests and code/security workflows |
| Room 3.0.3 production databases | **In progress — PR #121** | Replacing legacy SQLiteOpenHelper in same original database files; **not accepted until schema/receipt migration and native acceptance pass** |

## Planned active Android 17 work

| Priority | Work / owner | Acceptance |
|---|---|---|
| P0 | Room 3.0.3 receipts and import sessions — [issue #115](https://github.com/edithatogo/CallTrack/issues/115), [PR #121](https://github.com/edithatogo/CallTrack/pull/121) | Existing schema1–6/schema1–2 migration, KSP/strict checksums, no double writer, native Android17 interruption and upgrade proofs |
| P0 | FOSS first release and privacy/legal suitability — [release track](conductor/tracks/release_20261020/index.md) | Production package/signing/key custody, code/source/licence, `READ_CALL_LOG` distribution eligibility, physical/OEM and cloud route acceptance and observation |
| P1 | Remove leftover hybrid/custom UI — [issue #116](https://github.com/edithatogo/CallTrack/issues/116) | Standard native Compose components and accessible controls, zero regression in rendered native journeys |
| P1 | Local Apple Silicon Android 17 emulation — [issue #117](https://github.com/edithatogo/CallTrack/issues/117) | Verified official self-hosted runner + dedicated API37 AVD, exact app/source receipts and cleanup |
| P1 | Replace temporary required status aliases — [issue #119](https://github.com/edithatogo/CallTrack/issues/119) | Update GitHub main protection and remove obsolete legacy-named compatibility jobs |

## Deferred product possibilities (not implemented in the Android app)

Work/personal calendar rules, contact notes/outcomes and callback workflow, tagging, reminders, CSV/ICS export, SMS support, portable backup/import beyond the existing synthetic export prototype, direct Microsoft Graph/Google/CalDAV APIs, per-provider conflict routing, macOS/iOS companions, KMP/Compose Multiplatform, Windows/Linux desktop, Rust components and hosted endpoint integrations remain **roadmap-only**. There is no accepted release or functional promise for them.

The active platform contract remains Android 17 only; older Android versions are retained **only as historical upgrade fixtures**, not a development or support matrix. See [requirements](conductor/requirements.md) for original R01–R27 provenance and [historical roadmap](conductor/roadmap.md) for dated research.
