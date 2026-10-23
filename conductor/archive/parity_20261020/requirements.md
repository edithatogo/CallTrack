# Track 4 concrete requirements — final emulator acceptance revision 2

User steering: emulator acceptance for now; physical-call acceptance remains pending. Desired timing is prompt logging when Android permits, with durable recovery after delays. No fixed background deadline is promised.

| ID | Required behaviour and evidence |
|---|---|
| P01 | Enable/pause automatic logging and independently filter incoming/outgoing/missed calls; call-only operation requires no SMS/contacts access. Automatic starts from an explicit activation boundary; historical work needs preview/confirmation. |
| P02 | Catch up arbitrary missed batches, not four/latest rows. Bounded source paging, persisted progress and source identity/reset checks must prevent stale ID checkpoint skips. |
| P03 | Preserve exact instants and raw number presentation; known/unknown/withheld/null mapping with raw number-presentation retained; safe long duration; visible zero-duration marker explicitly describes actual zero talk time. Retain the original source instant and actual duration separately from rendered marker duration in durable metadata. Record final rendering policy and tests. |
| P04 | Commit an insertion intent before crossing into the calendar provider. READY may be attempted; interrupted IN_FLIGHT with no recoverable marker becomes review, never blind reinsertion. Known ID is retained across readback errors. Pending intent ownership/generation must prevent a cancelled import or paused activation from being resumed by unrelated automatic work. |
| P05 | Recover marker/known-ID attempts, serialize overlapping workers, preserve edits and intentional deletion. A confirmed receipt never implies current event existence; verify actual data. Never overwrite/recreate changed/missing events automatically. |
| P06 | Preserve Track 3 receipts through a tested schema migration. Bind source identity conservatively; changed/reused IDs require review. Destination changes do not move/delete old events. |
| P07 | Historical range preview, count, confirmation, cancellation, progress and resume; repeat import is idempotent. Partial batches reflect actual effects. Legacy-event matching is a read-only confidence/ambiguity prototype with no blind adoption. |
| P08 | Native scheduling and recovery across restart/reboot/reopen, permission denial/revocation, unavailable/read-only calendars, offline and Doze. Measure latency and distinguish scheduler request, execution and verified delivery. |
| P09 | Visible pending/saved/review/failed state with counts; personal caller fields and account names are excluded from diagnostics. Private/free events and no reminders remain defaults. Minimise retained payload and explain receipts/tombstones/retention. |
| P10 | Meaningful test-first JVM/native/emulator matrix, build/CI inputs and artifacts, scoped fixture recovery, review/fixes, updated Conductor handoff and archive only after all track gates pass. |

Physical calls, real Outlook/Google remote delivery, production signing/store eligibility and long-term community observation are not represented by emulator results. These remain explicitly pending as requested; this does not defer the automatic logging/import/recovery implementation itself.

Default extended call-type policy proposed for discovery: incoming/outgoing/missed supported and enabled independently; blocked/rejected/voicemail/answered-elsewhere/unknown types excluded from automatic output until explicitly specified. Preserve raw source fields; do not reinterpret blocked as missed. Multi-SIM identity is metadata, not an independent duplicate key.

P01–P10 are reconciled in [closure audit](closure-audit.md) and [final review](final-review.md). This records development acceptance without claiming the explicitly deferred physical/cloud/release outcomes.
