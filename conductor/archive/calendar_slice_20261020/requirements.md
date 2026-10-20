# Track 3 concrete acceptance

- S01: a visible writable-calendar picker, no automatic fallback to another destination, stable saved choice.
- S02: call source maps ID/namespace, timestamp, direction, duration and name/number presentation; no SMS/contacts permission required for the cached-name path.
- S03: correct mapped event title/description/start/end/timezone; safe null/overflow handling and explicit zero-duration policy.
- S04: native sink creates an event in a selected writable calendar and reads the actual fields back.
- S05: a reopened ledger recognises the same event; duplicate retry and crash-gap recovery do not blindly insert again.
- S06: missing permissions and unavailable destination produce recoverable outcomes without writes/crashes.
- S07: preserve edits; missing/changed/ambiguous prior events require review.
- S08: native acceptance uses synthetic data and an isolated local calendar, reports provider categories without account names, and verifies fixture/package cleanup.
- S09: map Outlook-backed device-calendar support and limits; demonstrate the generic adapter without assuming any account is exposed or cloud-synced.
- S10: tests before implementation, native readback, formal review/fixes, Conductor handoff and archive.

Actual unattended logging, real-call acceptance, normal permission-consent UX, remote Outlook/Google sync, cross-device identity and production distribution remain downstream. This track is an actual local-provider vertical slice with a usable manual action, not a claim of full CallTrack parity.
