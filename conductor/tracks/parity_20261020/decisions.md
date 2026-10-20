# Track 4 decisions — before implementation

## Durable delivery boundary

Retain the native provider adapters and testable Java core. Add a durable outbox before adding scheduling/import. Commit READY, then IN_FLIGHT before calling insert; commit a known ID immediately on return. A resumed IN_FLIGHT without a marker cannot distinguish a pre-insert crash from a lost-marker/deleted post-insert event, so it must require review. This sacrifices unattended retry for that ambiguous case to preserve the no-duplicate/no-recreation requirement. A READY record is safe to attempt.

Use one process-wide delivery lock for the app's single process plus short SQLite transactions for each state transition. No provider call is hidden inside an uncommitted SQL intent. All workers/UI delivery share this path; multi-process delivery is unsupported unless redesigned with a durable lease and qualified separately.

Store source-content fingerprint separately from frozen event fingerprint: changes in cached contact labels or the device display timezone must not rewrite existing events. A changed source ID/content binding is a source-review condition. After confirmation retain minimal identity/marker/fingerprints/ID, not the original personal event payload. Review/missing-event states also prevent recreation; explicit user resolution must not be guessed.

Preserve v1 receipts when upgrading SQLite; old fingerprints/IDs remain useful, but must be verified before binding to new source signatures. Room migration is evaluated in findings; no destructive table recreation is acceptable.

## User steering

Use emulator call/provider/background tests now. Physical-call acceptance remains pending. Aim for prompt content-triggered work with persisted periodic/reopen catch-up; measure actual latency. Do not claim exact execution deadlines or change the connected phone's power settings for emulator tests.

## Dependencies — initial disposition

Native SQLite remains selected for the bounded outbox migration, subject to actual native tests. Room is a takeover candidate for schema/DAO/migration tooling, not an automatic addition during this behavioural change. WorkManager is already present at 2.7.1; refresh compatibility and fix constraints before selecting the scheduling version. No experimental analytics API or preview library is needed just to observe local test timestamps. Final dependency decisions/versions remain required before scheduling implementation.
