# Consolidated requirements — draft

Source vocabulary: CT = supplied CallTrack static evidence; ACS = AndroidCallLogSync source; CLC = Call-Log-to-Cal source; NEW = proposed quality/product requirement, not legacy functionality. See [audit](research/baseline-audit.md).

| ID | Requirement | Origin | Milestone | Acceptance evidence |
|---|---|---|---|---|
| R01 | Enable/pause automatic logging; filter incoming/outgoing/missed independently | CT | M3 | Each type enabled/disabled produces expected count; pausing prevents writes |
| R02 | Select an available writable device calendar; handle removal/read-only changes | CT + NEW | M2–M3 | Test calendar selection survives restart; inaccessible destination gives recoverable status |
| R03 | Automatic calendar events with direction, name/number/unknown fallback and duration | CT | M3 | Controlled calls agree with system log and actual calendar readback |
| R04 | Correct event timestamps/timezone; explicit zero-duration display policy | CT + NEW | M3 | Seconds, midnight, timezone changes and zero-duration fixtures pass |
| R05 | Optional historical import with date range, preview, cancellation and progress | CT + NEW | M3 | Repeated import creates no duplicate; interruption resumes safely |
| R06 | Stable identity, durable event mapping and retry recovery | CT weak dedup + ACS checkpoint + NEW | M3 | Concurrent triggers and crash between event insert/checkpoint yield one event |
| R07 | Preserve user-edited events and intentional deletions; explicit resync policy | NEW | M3 | Editing title/notes does not cause overwrite or duplicate; deleted event not silently recreated |
| R08 | Runtime permissions, revocation and missing-provider recovery | ACS + NEW | M2–M3 | Denial/regrant and calendar loss produce clear state, no crashes |
| R09 | Background reliability and catch-up | CT + ACS + NEW | M3 | Reboot, process death, offline and Doze tests; after force-stop, reopening catches up |
| R10 | Local calendar mode needs no project backend/account | CT | M2–M3 | Writes work offline; external calendar sync is separately verified and attributed to provider |
| R11 | Explain sync state and failures; redact diagnostics | ACS + NEW | M3 | Pending/succeeded/failed distinguishable; logs omit numbers/names/tokens |
| R12 | Optional endpoint output with documented versioned contract | ACS | Later optional | TLS, authentication, retry and receiving-side idempotency tested; disabled by default |
| R13 | FOSS distribution and contributor reproducibility | User | M1/M4 | Licence notices, build instructions, CI, source/release matching and signing/update policy |
| R14 | Incremental Kotlin migration with preserved behaviour/data | User | M5 | Before/after parity fixtures and upgrade tests pass |
| R15 | Optional Compose UI migration | User | M6 | Screen-by-screen accessibility, state restoration and navigation parity |
| R16 | Optional KMP shared processing core | User | M7 | Shared contract suite passes on Android and selected second target |
| R17 | Optional Compose Multiplatform interface | User | M8 | Demonstrated second-platform user workflow; Android regression suite passes |
| R18 | Notes, tags, reminders, CSV/ICS export, SMS support | CLC/Callvent inspirations | Later candidates | Separate prioritisation; not required for CallTrack parity |

## Proposed contract boundaries

- `CallSource`: records with source namespace, source ID, timestamp, direction, duration seconds, number presentation, optional name and SIM identity. A call-log row ID alone is not globally stable across restore/devices.
- `EventMapper`: pure transformation into title, description, start/end instants and presentation metadata. Locale/timezone and zero-duration policy explicit.
- `CalendarSink`: list writable calendars, insert/read events and return destination identity; no dependence on privileged sync-adapter fields.
- `SyncLedger`: source-to-event mapping, payload fingerprint, last-written fingerprint, pending/succeeded/failed/conflict states and reconciliation. An insert and ledger write span different stores: design recovery for that gap.
- `Scheduler`/`PermissionGateway`: native platform adapters. Trigger observation for responsiveness; periodic reconciliation for recovery; avoid promising exact background timing.
- `EndpointSink`: optional later adapter, isolated from local calendar path.

These are internal design proposals, not recovered CallTrack APIs. Keep boundaries small; do not build a generic plugin framework in advance.

## Decisions to resolve during milestones

Define logging latency target, retained history and diagnostic retention; select test/destination calendar; decide zero-duration rendering, rejected/blocked/voicemail treatment, multi-SIM scope, calendar deletion/resync semantics and imported legacy-event matching. Explicitly exclude audio recording and guaranteed third-party VoIP capture from parity. Optional SMS must not expand initial permissions.

## Future-state additions (2026-10-20)

The following are proposed extensions, not changes to the minimum parity release. See [architecture and feature evaluation](research/future-architecture.md).

| ID | Requirement | Stage | Acceptance |
|---|---|---|---|
| R19 | Provider-neutral device calendar support, including Outlook where exposed | M2/M3 qualification | Event created locally and verified in actual provider; unsupported account/profile reported clearly |
| R20 | Direct Microsoft Graph destination if local route is insufficient | Integration track after M3 | Personal/work test accounts, token refresh/revocation, throttling, timezone and duplicate-retry suite |
| R21 | Rules for work/personal routing, exclusions and redaction | Post-parity | Dry-run preview and deterministic precedence; no unintended cross-profile data movement |
| R22 | Notes, outcomes, contact timeline and callback workflow | Post-parity | User edits persist independently of regenerated event fields |
| R23 | Portable export/restore and conservative legacy migration | M3 preview; fuller post-parity | Round-trip, ambiguous matches and restore identity tests |
| R24 | Capability-aware iOS companion | Early feasibility; M7/M8 implementation | Supported manual/import workflow first; automatic capture only if separately demonstrated |
| R25 | Per-destination mappings, conflict policies and loop prevention | Integration track | Same underlying calendar reached by two routes cannot silently duplicate events |
| R26 | Dependency and Rust feasibility evaluation | M1 planning; bounded spike before M5/M7 | Compatible version matrix and tests; adopt Rust only for demonstrated benefit |
| R27 | Retention, private/free event defaults and redacted diagnostics | M3/M4 | Provider readback and privacy controls tested; deletion scopes explicit |

## Conductor execution policy (2026-10-20)

The [track registry](tracks.md) now owns execution. These inventories are planning hypotheses and dated evidence, not fully specified future implementations. Each later track must map, understand, research and document its requirements and dependency options when activated. Dependencies can be temporary prototype aids, enduring replacements for existing libraries, or replacements for app-owned/native code. Decisions require explicit purpose, evidence, promotion/removal criteria and recovery. See [workflow](workflow.md). Tracks 1–3 are complete; Tracks4–6 are active and seven tracks remain unstarted.

## Track 1 mapping outcome

Static mapping completed; see [findings](archive/baseline_20261020/findings.md), [refined acceptance and assigned uncertainties](archive/baseline_20261020/requirements.md) and [25 synthetic acceptance scenarios](archive/baseline_20261020/fixtures.json). CallTrack automatic scan is four examined rows; endpoint tests in ACS read real history; neither behaviour is a successor requirement. R01–R27 remain an evolving inventory: later-track evidence and unresolved product policies are explicit in U01–U12.

## Track 2 build outcome

R08/R13/R26 now have a bounded reproducible development build and call-log permission qualification path. Consult [build requirements and limits](archive/build_20261020/requirements.md) and actual [verification](archive/build_20261020/verification.md). No production signing, store acceptance, minimum-OS qualification, real call capture or calendar sync follows from this baseline.

## Track 3 bounded outcome

[Slice verification](archive/calendar_slice_20261020/verification.md) supplies bounded evidence for R02–R04/R06–R08/R10/R19/R27: explicit calendar selection, pure mapping, actual LOCAL event readback, reopened SQLite receipts, retry/crash-gap recovery, edit preservation and denied/revoked permissions. It does not complete their broader M3 requirements. The manual source adapter is implemented but personal call acquisition, background operation, historical import, lost-marker/tombstone/restore policies and remote sync remain unqualified. Known insert IDs are retained on uncertain readback; receipt presence alone is never success.

## Track 5 preparation outcome

R13/R27 now have contributor instructions, a data-lifecycle map, issue/PR templates,
channel research, resolved dependency inventory and a release/upgrade/observation
protocol. [D01–D09](tracks/release_20261020/requirements.md) retain the unmet gates.
This does not establish a source-matched release candidate, store eligibility,
production signing, retention controls or operational acceptance.

## Track 6 bounded migration outcome

R14/R26 now have a Java/Kotlin responsibility map and conditional pure-component
conversion, with Java golden contracts for persisted signatures, JVM interop and
legacy null/whitespace/overflow semantics. [K01–K07](tracks/kotlin_20261020/requirements.md)
separate this bounded qualification from the remaining21 Java files, prerequisite
release acceptance and whole-app migration/upgrade requirements. No Compose/KMP
or coroutine architecture adoption follows from adding Kotlin source.
