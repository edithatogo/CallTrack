# Destination requirements and contracts

R12/R19/R20/R25 refinement. These are future acceptance contracts, not implemented
multi-destination/provider guarantees. Source facts remain separate from destination
payload/redaction. Track4 delivery foundation must stabilize before integration.

| ID | Current gap / future requirement | Acceptance and owner |
|---|---|---|
| I01 | Native provider availability is not cloud coverage | Selected route/account/calendar qualifies local write/readback and server/client appearance separately; this track reopening |
| I02 | Local integer IDs cannot identify remote routes | Versioned destination binding includes route, device/profile epoch or provider authority/account scope, calendar opaque ID and capability/version; opaque event IDs remain scoped to that binding |
| I03 | Same display names / disappearing accounts | Disambiguate choice, preserve selected identity across refresh, pause on missing/recreated binding and require deliberate rebind; never select a fallback calendar |
| I04 | One durable intent per source and selected destination | Stable operation ID, frozen payload version, committed pre-write receipt; timeout/cancel may leave side effect; reconcile before retry; Track4 + integration reopening |
| I05 | Human edits and ambiguous remote copies | Compare owned binding and normalized expected fields; edits/deletion/multiple matches go to review; no automatic overwrite/recreation or title/time fuzzy ownership |
| I06 | Provider-specific rate/auth errors | Bounded Retry-After/backoff+jitter, classify provider reason codes, preserve intent across restart, separate reauthentication from retry; no silent account/permission expansion |
| I07 | Network metadata and credentials | Platform auth, protected tokens, no embedded secret or PII-bearing logs, validate endpoint/redirect origins and avoid credential forwarding; credential purge separate from receipt retention |
| I08 | Duplicate routes / loops | One writer to each actual remote calendar; disable simultaneous native/direct routes until explicit correspondence/migration proven; never ingest generated events as source calls |
| I09 | Fan-out and routing are independent operations | Future per-destination receipts/redaction/failure status, partial success and cancellation; no all-or-nothing promise across providers; no fan-out implemented |
| I10 | Privacy/event semantics | Selected destination preview, no attendees/reminders by default, actual duration retained, supported timezone mapping/precision/DST and private/free normalization tested; private flag is not ACL security |
| I11 | Cursors, retention and deletion | Page/cursor progress committed safely; cursor reset rebuilds mirror only; distinguish app uninstall/local data purge/credential revoke/explicit remote deletion; no mass remote deletion |
| I12 | Schema and route migration | Preserve schema1/2 receipts and old readers/recovery; cannot cast remote IDs to long; no dual writes during migration; rollback retains ownership/review evidence |
| I13 | R12 optional endpoint output, disabled by default | Named receiver need, HTTPS/auth, versioned schema and acknowledgement, receiver-side idempotency/readback, data minimization and retry/retention contracts tested before enabling |

## Proposed port contract, no new API committed

A destination binding identifies an authorized route and chosen calendar, with a
local capability epoch. List/select validates availability; write accepts an
already committed operation ID and frozen event payload. Return opaque event ID
plus normalized readback/version only after real read. Distinguish locally stored,
remote observed, retry scheduled, reauthentication required, and review required.
A provider ACK or SDK object is not itself durable/remote human-readable acceptance.
Never collapse a transport timeout into a definite failed insertion.

## Provider response matrix (future fixture cases)

| Observation | Proposed safe behavior / case |
|---|---|
| Graph create response lost | Retain same transactionId and committed intent; reconcile known ID/qualified app binding; unavailable/ambiguous evidence stays pending/review rather than blind new create |
| Graph429 | Respect Retry-After; bounded scheduled retry after checking write uncertainty; no UI-blocking sleeps |
| Google409 duplicate reason | GET same ID, compare calendar/ownership/expected normalized payload; mismatch goes review; never choose new ID just to retry |
| Google403 | Inspect reason: insufficient permission pauses; quota reasons back off; no blanket403 retry |
| Google410 syncToken invalid | Rebuild remote mirror without dropping delivery receipts/review/tombstones |
| DAV create timeout /412 | GET stable resource, validate UID and binding; conflict or edited object goes review; no unconditional PUT |
| Any401/auth expiry | Preserve operation, use bounded provider-supported refresh; unresolved failure requires sign-in; no account substitution |
| Any404/410 event missing | Could be deleted, inaccessible, stale route or eventual visibility; do not recreate automatically; distinguish cursor error from event deletion |
| Any canceled write | Caller cancellation is not proof that remote side effect was rolled back; reconcile on resume |

These are specified fixtures, not executed HTTP or SDK contract tests. The
[qualification protocol](qualification.md) requires actual native/provider readback
before promotion. No live connector is justified by documentation support alone.

## R12 endpoint output contract (future, disabled by default)

An optional endpoint is a separate destination, not a calendar provider or an
unrestricted URL callback. First identify a receiver/workflow; no endpoint or
service is selected. The older upstream configurable HTTP POST/Number and Token
header behavior is not approved as the successor contract.

Before implementation agree a versioned request schema containing an opaque
operation/source binding, payload version, timestamp/timezone/actual duration and
explicit redaction policy. Never place phone identity or credentials in URLs or
telemetry. Scope credentials to the selected HTTPS origin, validate redirects and
certificate handling, and define server/client size limits and schema rejection.

Receiver deduplicates by operation ID with a durable payload fingerprint: repeated
same operation returns the existing receipt; same key/different payload reports a
conflict. Specify acknowledgement and query/reconciliation semantics, retention
window and expiry before selecting HTTP codes. A200/202 alone cannot establish
calendar delivery or completed downstream processing. Timeout/cancel after POST
requires reconciliation; no automatic fresh operation ID or blind replay after
receiver deduplication expiry. Test receiver crash/commit/response-loss, schema
upgrade/rejection, auth revoke, rate limits and receipt recovery using synthetic
inputs. No receiver-side tests have run and no Internet capability is enabled.
