# CallTrack successor roadmap — draft

Date: 2026-10-20. Planning authorised by user. Working direction: evaluate AndroidCallLogSync for a GPL-compatible fork, restore useful CallTrack capabilities, then migrate incrementally to Kotlin. Jetpack Compose, Kotlin Multiplatform (KMP) and Compose Multiplatform remain conditional stages, as requested.

Conductor planning structure is established; this roadmap does not establish completed track work or approved release scope. No public fork, app installation, calendar write or runtime compatibility claim has been made. Local reference checkouts are available for inspection.

## Evidence-led milestones

| Milestone | Deliverable/tasks | Dependencies | Exit gate |
|---|---|---|---|
| M0 — Baseline and requirements | Inventory APK/resources/manifest/API calls; map both FOSS apps; separate observed behaviour from enhancements; resolve audit unknowns; create synthetic fixtures and traceability | None | R01–R18 reviewed, sources pinned, unresolved details explicitly recorded; initial static mapping is complete, dynamic validation pending |
| M1 — Fork feasibility and build baseline | Reproduce chosen upstream revision; qualify Java/JDK/Gradle/SDK compatibility; select package/signing identity; establish GPL notices and contributor build; compare repair cost with smaller fresh implementation | M0 | Repeatable build plus install/launch on test device; permission access demonstrated; fork-versus-rebuild decision recorded |
| M2 — End-to-end vertical slice | Introduce small source/mapper/calendar/ledger boundaries; retain working Java; select test calendar; transform one controlled call into one event | M1 | Native event readback matches system call data; no external endpoint needed; denied-permission path works |
| M3 — CallTrack parity and reliability | R01–R11: type switches, automatic operation, contact/unknown handling, historical preview/import, duplicate recovery, edit protection, background catch-up | M2 | Controlled incoming/outgoing/missed matrix passes; retry/crash/import tests prove no duplicates; offline/reboot/permission tests recorded |
| M4 — Community alpha and dependable release | Documentation, contribution guide, issue templates, CI, reproducible build recipe, release signing/upgrade policy, privacy/data lifecycle, device support matrix and distribution qualification | M3 | Installable source-matched alpha, upgrade test and recovery path; longer real-device observation before stable label |
| M5 — Kotlin migration | Convert pure mapping and ledger logic first, then repositories/workers and remaining app-owned Java; Kotlin tests/coroutines where appropriate; avoid simultaneous UI redesign | M4, may prepare isolated work after M3 | Same acceptance suite and persisted-data upgrade tests pass; app-owned Java migrated or remaining exceptions documented |
| M6 — Jetpack Compose (conditional) | Prototype settings/status screen; migrate screens incrementally alongside Views; accessibility, navigation and state restoration | M5 | Feature parity and UI tests pass; no syncing regression; remove old UI only after replacement qualification |
| M7 — KMP core (conditional) | Choose concrete second-platform use case; extract models, mapping, dedup rules and orchestration where portable; implement native adapters | M5; independent of M6 | Shared fixtures run on Android and second target; permission/calendar/background capability matrix demonstrates useful achievable scope |
| M8 — Compose Multiplatform (conditional) | Share suitable UI/state after second-platform prototype; retain platform-specific permissions and acquisition screens | M6 + M7 | Useful second-platform workflow demonstrated; packaging/accessibility/upgrade tested; Android remains dependable |

No calendar estimates yet: M1 determines build and permission constraints. Track milestones by exit evidence rather than technology adoption alone. M3 delivers the functional objective; M5 fulfils the Kotlin direction; M6–M8 require demonstrated value.

## Architecture evolution

Initial: Android call-log provider → mapping → Android calendar provider, with durable sync ledger and native scheduling.

Kotlin stage: migrate behaviour-preserving components behind those boundaries. New isolated modules may use Kotlin earlier if justified; no wholesale rewrite prerequisite for parity.

KMP stage: share suitable domain logic; keep Android call-log/contacts/calendar access and scheduling in Android adapters. Desktop could offer an import/review companion; iOS is only a feasibility candidate. Do not assume iOS can expose Android-equivalent system call history. Require supported API evidence before promising automatic iOS parity. KMP does not grant platform permissions or bypass OS restrictions.

Compose stage: Jetpack Compose is the Android UI toolkit; Compose Multiplatform extends the model to other targets and uses Jetpack Compose artifacts on Android. They are related layers, not three successive mandatory rewrites. Prefer supported stable versions selected when implementing; pin compatible versions and validate upgrades.

## Acceptance matrix for parity release

Run synthetic domain tests and bounded real-device tests with a dedicated test calendar. Include known/unknown/withheld numbers, incoming/outgoing/missed calls, zero/long duration, rapid successive calls, filtering, locale/timezone change, import twice, overlapping workers, crash after calendar insertion, destination removal, edited/deleted events, permission revocation, offline, process death, reboot, Doze and reopen after force-stop. Source row IDs after restore/reset require explicit tests. Native calendar persistence and remote provider sync are separate outcomes. Never commit real call logs, credentials, signing keys or personal calendar dumps.

Proposed release practice: a small documented 7-day device observation period after the controlled suite; this is a proposed gate, not elapsed or completed evidence. Record observed latency, misses, duplicate count and recovery behaviour.

## Boundaries and recovery

Keep current working outputs and released APKs until replacement passes tests. Use a new development application ID and isolated calendar where practical. Record exactly which events a test creates; cleanup must target those events only. Signed production updates require continuity of signing identity; rollback must account for data-schema compatibility rather than blindly reinstalling an older APK. Preserve human edits and issue history.

Play Store eligibility for call-log access needs separate qualification; an APK build does not establish store approval. GitHub/F-Droid distribution are candidates, not confirmed acceptance. Keep original CallTrack analysis as behavioural evidence; do not copy or publish its extracted implementation without established rights.

The user's SharePoint structured-document preference was considered: these are maintained repository specifications tied to source revisions, not generated Word deliverables. Use Markdown as the project source of truth; reassess the preferred route if a formal report or document pack is requested.

## Supporting material

- [Consolidated requirements](requirements.md)
- [Baseline audit and source anchors](research/baseline-audit.md)
- [Android Calendar Provider](https://developer.android.com/identity/providers/calendar-provider): direct calendar integration and permissions; provider cloud sync remains separate.
- [Incremental Compose migration](https://developer.android.com/develop/ui/compose/migrate/strategy): coexistence with Views supports screen-by-screen migration.
- [Compose Multiplatform and Jetpack Compose](https://kotlinlang.org/docs/multiplatform/compose-multiplatform-and-jetpack-compose.html): shared UI relationship and platform-specific boundaries.

Next action: finish M0's unresolved APK behaviour mapping and agree the acceptance defaults, then run M1 before creating the public fork or committing to a large refactor.

## Expanded roadmap tracks (2026-10-20)

See [future features and architecture](research/future-architecture.md) and R19–R27. Existing milestone IDs remain stable.

- **M0 extension:** map proposed features separately from recovered behaviour. Static follow-up confirms all three call-type defaults are true in `CalltrackUtils`; calendar enumeration queries ID/name without an access-level selection predicate. Modern writable-calendar checks are an improvement, not demonstrated legacy behaviour.
- **M1 addition:** dependency compatibility inventory and small iOS capability spike before locking architecture. Keep AndroidCallLogSync fork decision evidence-based. An initial library survey is recorded; each owning track must refresh research and qualify versions/builds when activated.
- **M2/M3 addition:** qualify Google and Outlook-backed device calendars through the same adapter where available; add privacy/free-busy defaults, redaction choices and conservative legacy import preview. Provider sync must be read back separately.
- **Post-M3 integration track:** direct Microsoft Graph first if needed, then Google/CalDAV adapters for demonstrated gaps. Gate on authentication, duplicate recovery, edits/deletions, throttling and timezone tests. This track need not wait for Compose or KMP.
- **Post-M4 productivity track:** notes, tags, contact timeline, callback reminders, routing rules and portable backups, followed by optional project/time summaries and CRM/task adapters. No automatic expansion into recording/transcription.
- **Before M5/M7:** optional bounded Rust-versus-Kotlin component experiment with explicit build/maintenance/performance criteria. Kotlin/KMP remains the default core; Rust is not a prerequisite for multiplatform support.
- **M7/M8 clarification:** capability-aware iOS/manual-import companion is a valid first second-platform outcome. Android-equivalent automatic historical cellular tracking is not promised. Desktop companion is an alternative second-target use case.

## Conductor execution policy (2026-10-20)

The [track registry](tracks.md) now owns execution. These inventories are planning hypotheses and dated evidence, not fully specified future implementations. Each later track must map, understand, research and document its requirements and dependency options when activated. Dependencies can be temporary prototype aids, enduring replacements for existing libraries, or replacements for app-owned/native code. Decisions require explicit purpose, evidence, promotion/removal criteria and recovery. See [workflow](workflow.md). All tracks remain pending.
