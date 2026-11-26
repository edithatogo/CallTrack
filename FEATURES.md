# CallTrack feature status — 26 November 2026

The **shipped app** is a *development build* targeting **Android 17 (API 37)**. A passing emulator or source test does not mean a signed public release. Consult [the active scope](conductor/active-scope.json), [Conductor tracks](conductor/tracks.md) and [release gates](RELEASING.md); historical plans are not evidence that a feature has shipped.

| Capability | Code | Qualification / limitations |
|---|---|---|
| Manual log-latest-call to selected writable calendar | Implemented | Native CallLog/CalendarContract, explicit action, receipt readback; permission gating |
| Automatic call-log capture | Implemented | Incoming/outgoing/missed filters; enable/pause; WorkManager prompt and periodic catch-up; best-effort, not exact scheduling |
| Call sources and calendar routing | Implemented in the dev app | Phone calls and app calls shared with Android 17. Choose one or more writable calendars per source in **Automatic logging settings** and review the routes before applying. [Track 23](conductor/tracks/android17_call_routing_20261125/index.md) |
| Call event mapping | Implemented | Direction, start/end/duration, timezone, name or privacy-preserving fallback, private/free, no invitations |
| Historical range import | Implemented in the dev app | Select phone calls or a participating app and a writable calendar. The read-only preview and saved session bind the selected source and destination. Earlier sessions retain phone-call scope. |
| Durable delivery and reconciliation | Implemented | Original source identities, per-destination deduplication, pending attempts, uncertain write review, edit/deletion protection; no silent duplicate insert |
| Recovery and retention | Implemented | Existing event readback, review/suppress/reapprove, bounded payload retention, no auto-overwrite |
| Android permissions/lifecycle | Implemented | Read-call-log, phone state and calendar permissions, revocation, process-death/reboot/Doze/offline recovery tested synthetically |
| Android UI | Implemented | Native Compose roots for manual logging, automatic settings, import and recovery. [Track 14](conductor/tracks/compose_app_20261021/implementation-evidence.md) |
| Android Calendar Provider integration | Implemented | Select writable local or account-backed calendars exposed in Android. The calendar service handles sync. The Outlook cloud route awaits a writable exported test calendar and independent readback. |
| Debug/privacy hardening | Implemented | No raw call fields in worker input/logs; backup/data extraction exclusions; synthetic tests and code/security workflows |
| Room 3.0.3 databases | Implemented | Suspend DAOs and preserving migrations for both existing database files. [Track 22](conductor/tracks/room3_android17_20261124/plan.md) |

## Planned active Android 17 work

| Priority | Work / owner | Acceptance |
|---|---|---|
| P0 | FOSS first release and privacy/legal suitability — [release track](conductor/tracks/release_20261020/index.md) | Production package/signing/key custody, code/source/licence, `READ_CALL_LOG` distribution eligibility, physical/OEM and cloud route acceptance and observation |
| P1 | Qualify Android 17 call routes — [Track 23](conductor/tracks/android17_call_routing_20261125/plan.md) | A controlled call and provider readback establish coverage for each selected app and nominated calendar |

Android's attributed history confirms which installed apps share calls. CallTrack
finds candidates such as Messenger through targeted package visibility and apps
such as Google Meet through the standard callback. Automatic routes apply to
calls after confirmation. Historical imports use an explicitly selected source
and the existing preview, confirmation and recovery flow.

## Deferred product possibilities (not implemented in the Android app)

Contact notes/outcomes and callback workflow, tagging, reminders, CSV/ICS export, SMS support, portable backup/import beyond the existing synthetic export prototype, direct Microsoft Graph/Google/CalDAV APIs, per-provider conflict routing, macOS/iOS companions, KMP/Compose Multiplatform, Windows/Linux desktop, Rust components and hosted endpoint integrations remain **roadmap-only**.

The active platform contract remains Android 17 only; older Android versions are retained **only as historical upgrade fixtures**, not a development or support matrix. See [requirements](conductor/requirements.md) for original R01–R27 provenance and [historical roadmap](conductor/roadmap.md) for dated research.
