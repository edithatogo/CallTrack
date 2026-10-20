# Track 1 requirements, acceptance and uncertainties

## Completed discovery deliverables

| Deliverable | Evidence |
|---|---|
| Original controls/defaults/API/format/scan mapping | CT01–CT13 in [findings](findings.md), APK digest in [sources](sources.json) |
| Both FOSS capabilities/dependencies/custom-code map | Pinned ACS/CLC findings and source file digests |
| Differences and successor requirements | Matrix below plus existing R01–R27 inventory |
| Synthetic acceptance catalogue | [fixtures](fixtures.json); proposed acceptance data, not app test results |
| Options, prototype dependency and implementation decision | [decisions](decisions.md) |
| Verification/review and downstream handoff | [verification](verification.md) and [review](review.md) |

## Requirements refinements handed forward

| Requirement | Baseline / change | Concrete acceptance | Owner |
|---|---|---|---|
| R01 enable/type filters | CT02; manual import bypasses enable | Explicit manual versus automatic pause behaviour; independent switches tested on all types | Parity |
| R02 calendar selection | CT03/04 lacks writable filter and consistent fallback | Show only usable writable destinations; missing ID produces clear pending/error state, never another calendar | Calendar slice/parity |
| R03 metadata | CT08/09; ACS omits names; CLC drops number from event payload | Preserve direction/name/number-presentation/duration with safe null fallback; no invented contact identity | Calendar slice/parity |
| R04 timing | CT10 nearest-second rounding; int overflow; CLC zero-length | Preserve original instant in ledger; safe arithmetic; define render policy for zero duration without fabricating actual talk time | Parity |
| R05 history | CT07 full batch versus ACS latest-only initial normal sync | Preview count/range; cancel and resume bounded batches; repeated import yields same event set | Parity |
| R06 identity/recovery | CT06/12 four-row scan and title equality; ACS ID-only progress | Catch up >4 rows; stable namespace+identity; collision/crash/concurrency recovery; source restore/reset recognised | Parity |
| R07 edits/deletions | CT12 title edits duplicate; descriptions ignored | Define field ownership, retain human title/notes, explicit deletion/resync policy; ambiguous legacy matches need review | Parity |
| R08 permission handling | Missing modern APK permission; CLC denial callback ignores grant | Call-only mode must not require SMS; denial/null cursors/revocation recover without writes/crashes | Build/parity |
| R09 background | CT05 blocked receiver; ACS15min scheduler | Record measured latency, reboot/reopen/catch-up; OS scheduling constraints documented, no exact timing claim | Build/parity |
| R10/19 local calendar | CT11 provider API, no direct Google account | One adapter usable for any exposed writable calendar; provider/cloud readback separate | Calendar slice/destinations |
| R11/27 status/privacy | CT13 ignored insert result; ACS retained responses | Distinguish pending/confirmed/failure; redact personal fields; explicit privacy/free-busy/default reminder policy | Parity/release |
| R12 endpoint | ACS test uploads actual history | Synthetic endpoint test by default; explicit preview for real uploads; TLS/auth/idempotency qualified independently | Destinations |
| R13 distribution | Source licences only, no qualified binary | Build and permission evidence before base adoption; retain source/licence provenance; no original APK code reuse | Build/release |
| R14–18/20–26 future | Initial research hypotheses | Owning tracks refresh mapping, research dependencies and document decisions; no automatic adoption | Kotlin/UI/KMP/integration/productivity/Rust |

## Open decisions and evidence still needed

| ID | Uncertainty / proposed starting point | Required resolution | Owner / blocking scope |
|---|---|---|---|
| U01 | Original APK source/authenticity/signature trust not verified | Establish provenance if reuse/distribution is contemplated; behaviour-only findings remain qualified | Release; not discovery blocker |
| U02 | Modern Android install/call-log permissions and boot reliability unknown | Qualified build and bounded device tests; do not bypass OS restrictions to claim support | Build; blocks runtime claims |
| U03 | Zero-duration rendering: preserve actual duration, choose visible marker separately | Compare provider acceptance/display and user preference | Parity; blocks final format |
| U04 | Desired logging latency has no user target yet | Measure feasible foreground/background latency, then document target | Parity; blocks latency acceptance |
| U05 | Rejected/blocked/voicemail/answered-elsewhere and SIM handling | Decide supported types and defaults from modern source API/device evidence | Parity |
| U06 | Edit ownership, intentional delete, destination change, contact rename | Record explicit conflict/deletion policy with preview; no silent overwrite/recreation | Parity |
| U07 | Legacy collisions/time rounding/contact-label quirks | Read-only import matching prototype with synthetic events, confidence and ambiguity report | Parity |
| U08 | Empty and null providers, huge histories and restored IDs | Fixtures plus qualified provider/device testing; no inferred catch-up guarantee | Build/parity |
| U09 | Outlook/work-profile calendar exposure and tenant restrictions unknown | Select a test calendar only when executing slice; native then cloud readback | Calendar slice/destinations |
| U10 | Dependency compatibility/transitive licences and build costs unresolved | Build inventory then owning-track evaluation; shortlist ≠ adoption | Build/release/later tracks |
| U11 | iOS full cellular-history acquisition not established | Current supported API/entitlement research and feasibility prototype | Platform feasibility |
| U12 | Retention, backup, redaction and event privacy defaults unresolved | Threat/data lifecycle and provider sharing tests; local-first starting point | Parity/release |

No open item is hidden behind a “complete” runtime status. This track's acceptance is the completed mapping, test catalogue and assigned uncertainty register; operational tests belong to the tracks named above.
