# Future features, destinations and dependency strategy

Research date: 2026-10-20. Status: recommendations for review, not implementation commitments or tested integrations. Complements the static baseline mapping; legacy runtime mapping remains incomplete.

## Recommended product expansion

| Priority | Feature | Benefit | Acceptance / boundary |
|---|---|---|---|
| Parity+ | Destination health, pending queue and last successful reconciliation | Make silent omissions visible | Distinguish device persistence from cloud confirmation; retry never duplicates |
| Parity+ | Choose calendar, private/free events, no attendees/reminders by default | Avoid exposing call details or blocking availability | Validate actual provider fields and sharing behaviour; private is not an access-control guarantee |
| Parity+ | Redaction options and exclusion lists | Control which details leave the phone | Preview before enabling; exclude contacts/numbers or redact titles independently per destination |
| Parity+ | Migration preview from legacy CallTrack events | Retain years of history | Conservative matching with confidence/ambiguity review; never mass-delete or overwrite unmatched events |
| Next | Work/personal routing by SIM, contact group, time and manual override | Separate personal and work activity | Rule precedence and dry-run preview; respect work-profile boundaries |
| Next | Searchable contact timeline, notes, tags and outcomes | Useful call journal beyond calendar entries | Notes survive contact renaming, retries and migrations |
| Next | Callback reminders and unresolved missed-call list | Follow-up workflow | User-controlled reminders; avoid treating every missed call as actionable |
| Next | CSV/JSON/ICS export, backup and restore | Portability and recovery | Round-trip tests; export is not live bidirectional sync; secure handling of personal data |
| Next | Batch review and corrections; manual call entry | Fix omissions and support limited platforms | Track manual/imported/device source explicitly |
| Later | Time summaries, project/client attribution and timesheet exports | Professional use | Preserve actual duration; billing rounding is a separate editable policy; no automatic invoicing |
| Later | Multiple destinations and desktop companion | Access and organisation across devices | Per-destination mappings and origin markers; prevent sync loops and mirrored duplicates |
| Later | Optional CRM/task integrations, e.g. Microsoft To Do or a selected CRM | Turn calls into follow-ups | Only select connectors after concrete user need and API/access qualification |
| Exploratory | AI-assisted notes classification or summaries of user-written notes | Reduce manual administration | Opt-in, reviewable, clear source provenance; no inferred call transcript from metadata |

Audio recording/transcription, default-dialler replacement, spam screening and team surveillance are separate products/scopes, not implied extensions. Do not make them dependencies of a call-to-calendar app.

## Destination architecture

Call acquisition and destination selection must be independent. Normalised call record → event mapping/rules → per-destination outbox/ledger → adapter. Avoid a generic plugin platform until actual adapters justify it.

1. **Android device calendar first:** list writable calendars via CalendarContract, not Google-branded account assumptions. Google, Outlook/Exchange or CalDAV-backed calendars may work when the device exposes them. Installed calendar apps alone are insufficient.
2. **Outlook/Microsoft 365 qualification early:** Microsoft documents two-way Android calendar export for supported Outlook accounts. Verify account settings, destination visibility, permissions and create/edit readback in Outlook itself. Tenant policies may prevent this route; do not imply settings changes or consent.
3. **Direct Microsoft Graph connector later if needed:** delegated access, platform OAuth flows/token protection, selected calendar, idempotency and throttling handling. Graph create-event transactionId can help reduce duplicate retries; retain our own ledger. v1.0 calendar-view delta is date-window scoped; unbounded calendar delta is documented as beta and is not the baseline. Qualify shared calendars, personal/work accounts and on-premises cases separately.
4. **Direct Google Calendar connector only for a demonstrated gap:** avoid duplicating the device sync path to the same calendar. Implement provider-specific token recovery and conflict handling.
5. **CalDAV/Nextcloud:** prefer existing device calendar sync first. A direct connector requires protocol discovery, UID/ETag conflict handling and interoperability tests; ICS files alone do not provide this.
6. **Apple calendars:** native EventKit for available writable calendars; write-only access does not support arbitrary calendar browsing/reconciliation. Evaluate the actual permission mode needed. iCloud via native calendar access and direct CalDAV are different routes.

Start with one-way creation of app-owned call events and readback/conflict detection. General two-way editing and multi-destination fan-out are later work. A calendar edit must never alter the phone's source call log. Distinguish delete-from-app, delete-generated-events and retention expiry explicitly.

## iOS product feasibility

KMP is appropriate for shared models, transformations, rules, persistence where supported and network connectors. Compose Multiplatform may share UI; SwiftUI remains a valid native alternative if iOS interaction needs dominate.

This research did not establish a supported public iOS API for Android-equivalent historical cellular call-log access. CallKit call-state observation is not proof of full history, phone-number access or durable background capture. Therefore the iOS acceptance baseline must be manual entry/import, review, notes, reminders and calendar integration, until a supported automatic acquisition route is demonstrated. App-owned VoIP or business telephony integrations are possible research tracks, not a general solution for every cellular/WhatsApp/Teams call. No framework, including Rust, changes OS access rules.

Bring a small iOS feasibility spike forward to M1 while keeping iOS implementation after Android parity. Gate any parity claim on current Apple documentation, entitlement/app-review requirements and real-device tests. Produce a capability matrix that labels unavailable functions honestly.

## Dependency shortlist

These are candidates, not a compatible version lockfile. Select stable mutually compatible versions at M1/M5; do not inherit upstream's old dependency versions or indiscriminately choose alpha releases.

| Layer | Preferred candidate | Why / qualification |
|---|---|---|
| Domain and async | Kotlin, kotlinx.coroutines/Flow | Direct Android fit; structured cancellation and testable state; platform adapters stay native |
| Shared logic | Kotlin Multiplatform | Reuse across Android/iOS; add only after stable boundaries and a second-target test |
| UI | Jetpack Compose / optional Compose Multiplatform | Incremental Views migration; shared UI where useful, not mandatory for shared logic |
| Persistence | Room, evaluating its KMP support | Typed database access and migration testing; prove target support and crash/reconciliation semantics |
| Preferences | DataStore where target support fits | Versioned small configuration; not a credential vault or replacement for relational ledger |
| HTTP | Ktor client + kotlinx.serialization | Shared connector logic with platform engines; redact logs; provider-specific retries still required |
| Background work | WorkManager on Android; native iOS scheduling adapter | Do not hide materially different execution guarantees under a misleading common API |
| Calendar/contact access | CalendarContract / EventKit and native contact APIs | Native permission and account behaviour; no cloud API required for local calendar mode |
| Number parsing | Google libphonenumber for Android/JVM initially | Better than manual +→00 rewriting; its Java library is not automatically a common KMP dependency; preserve raw/withheld/extension data |
| Authentication | Provider-supported native OAuth libraries or reviewed platform browser PKCE flow | Verify Android/iOS support separately; no embedded client secret; Keychain/Keystore-backed credential handling |
| Quality | kotlin.test/JUnit, provider contract tests, database migration tests, Compose UI tests, Android lint | Highest return is deterministic failure/upgrade testing, not dependency count |

Compare alternatives only for identified gaps (for example SQLDelight versus Room if target/SQL requirements differ). Start with constructor injection; a dependency-injection framework is not necessary for a small app. Do not make two database stacks, two HTTP stacks and two sync cores mandatory.

## Rust assessment

**Recommendation: Kotlin-first shared core; optional Rust spike, not a mandated rewrite.** Kotlin is already memory-safe at the application level; Rust does not automatically fix duplicates, token handling, permissions or scheduling. Phone call volumes do not yet suggest a computational bottleneck.

Potentially valuable Rust components: a mature audited parser used for large imports; a well-supported encrypted archive component; or a sync/normalisation engine genuinely shared with non-Kotlin applications, CLI tools and services. These require a concrete library review, not a generic Rust preference. Never invent cryptography.

UniFFI generates Kotlin and Swift bindings for Rust libraries. Its Kotlin support must not be equated with a turnkey commonMain/Kotlin-Native binding: verify the exact generated targets and use target-specific adapters. UniFFI does not solve native binary packaging. A Rust core also requires Android ABI builds, Apple device/simulator builds, symbols, FFI lifetime/cancellation/error handling, crash diagnostics and dependency/security maintenance.

| Choice | Likely development effort here | Cross-application value | Recommendation |
|---|---|---|---|
| Kotlin/KMP core | Lowest incremental effort from Android direction | Android, iOS and JVM reuse with native adapters | Default |
| Rust core + Kotlin/Swift bindings | Higher initial/build/debug effort | Strong when several non-Kotlin consumers truly exist | Benchmark one bounded component first |
| Flutter / React Native replacement | New language/runtime and migration work | Shared UI, still native call/calendar modules | No demonstrated advantage for this Java→Kotlin project |
| Tauri desktop/mobile shell | Web UI plus native integration and Rust tooling | Potential future desktop companion | Not selected; no Android background-call advantage established |

Rust adoption gate: same contract fixtures in Kotlin and Rust; measured development/build time, binary size, memory/battery and throughput on realistic and large imports; Android/iOS CI builds; licence compatibility; maintained releases; FFI cancellation/error tests; at least one concrete reuse benefit or measured deficiency. Retain the simpler implementation if the advantage is not material. Do not use both KMP and Rust as competing owners of the same domain logic.

## Research sources

- [Google KMP support and Jetpack availability](https://developer.android.com/kotlin/multiplatform?authuser=0000)
- [KMP source-set and platform structure](https://kotlinlang.org/docs/multiplatform/multiplatform-discover-project.html)
- [Compose Multiplatform relationship](https://kotlinlang.org/docs/multiplatform/compose-multiplatform-and-jetpack-compose.html)
- [Room KMP setup](https://developer.android.google.cn/kotlin/multiplatform/room?hl=en)
- [Ktor platform engines](https://ktor.io/docs/client-engines.html)
- [Google libphonenumber](https://github.com/google/libphonenumber)
- [UniFFI supported bindings and scope](https://mozilla.github.io/uniffi-rs/latest/)
- [Microsoft Outlook Android calendar sync](https://support.microsoft.com/en-us/outlook/how-do-i-sync-my-outlook-calendar-to-the-default-calendar-app-s)
- [Graph create event](https://learn.microsoft.com/en-us/graph/api/user-post-events?view=graph-rest-1.0)
- [Graph calendar-view delta](https://learn.microsoft.com/en-us/graph/delta-query-events)
- [Apple EventKit access](https://developer.apple.com/documentation/eventkit/accessing-the-event-store)
- [Apple CXCall](https://developer.apple.com/documentation/callkit/cxcall)
