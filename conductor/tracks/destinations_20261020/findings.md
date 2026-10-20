# Calendar/service integration findings

Refreshed2026-10-21 at baseline `1b21c87`; activation `90445b7`. Documentation and
source inspection only. No user accounts, calendar contents, device permissions,
settings or external services were accessed or changed. Track4 parity is incomplete;
this discovery does not qualify automatic logging or provider synchronization.

## Actual implementation and gaps

AndroidCalendarSink uses CalendarContract, not a Google API. Its choices require
visible=1, sync_events=1 and access>=contributor. Insertion rechecks availability,
writes private/free/no-alarm event fields, and adds custom app package/URI markers.
Readback checks ownership/privacy policy and preserves modified events. Existing
native slice evidence used a synthetic LOCAL calendar, not Outlook/Google/CalDAV.
The main app has no INTERNET permission or account/token infrastructure.

Choice exposes local row ID, display name and account type; display text is only
the name. Same-name calendars can be ambiguous. A long local calendar ID is not a
portable identity after account removal/recreation, restore or direct-API routing.
SqliteLedger schema2 keys receipts/outbox by source+integer calendar and stores
integer event IDs. It cannot accept opaque Graph/Google/DAV IDs without an explicit
preserving migration. Existing app-owned markers may not survive sync adapters or
remote edits; native/provider correspondence must be measured, never assumed.

## Route and need map

| Need / route | Documented capability | Current acceptance / first next step |
|---|---|---|
| Google calendar on Android | Device provider plus installed account sync | App can target eligible local calendar; cloud write/readback untested |
| Outlook/Microsoft365 on Android | Outlook documents two-way calendar export for supported account types | Candidate existing-provider route; no account setting or tenant eligibility checked |
| CalDAV/Nextcloud on Android | Sync adapter such as DAVx5 connects server calendars to Calendar Provider | Candidate existing-provider route; no adapter install/account/server tested |
| Direct Graph v1.0 | Selected-calendar events and delegated access | Only if native route demonstrably misses a required behavior; no direct client |
| Direct Google Calendar API v3 | Selected-calendar events, custom IDs and private extended properties | Same trigger; avoid parallel writes through both device and API |
| Direct CalDAV | Calendar discovery, object resources, UID/ETag conditional writes | More discovery/auth/ICS/server interop work; no endpoint selected |
| ICS export/subscription | Portable snapshot or feed, depending consumer | Does not establish writable synchronization or round-trip conflict handling |
| CRM/tasks/automation services | Potential later explicit needs | No connector selected; Track11 must establish workflow/value first |

Microsoft's support article lists Microsoft365, Outlook.com and hybrid modern-auth
on-premises mailboxes for two-way export. It does not qualify every tenant/profile.
Turning export off can remove synchronized local copies; route disappearance must
pause work rather than treat all missing events as fresh calls.
[Outlook Android export](https://support.microsoft.com/en-us/outlook/how-do-i-sync-my-outlook-calendar-to-the-default-calendar-app-s).

DAVx5 stores synchronized calendar data in the Android provider rather than owning
a separate calendar store. A calendar front end alone is not a sync adapter. Reuse
an existing authorized sync setup before introducing another engine. Availability,
installation channel and current cost/licensing of an app must be checked when
actually needed; no purchase/install recommendation is committed here.
[DAVx5 architecture](https://manual.davx5.com/introduction.html).

## Provider contract research

Graph v1.0 creates in a specified calendar with delegated Calendars.ReadWrite for
personal/work-school user calendars. No attendees should be supplied: invitations
are sent when attendees are included. Supported mailbox timezones need validation.
transactionId assists duplicate prevention but is not an unlimited exactly-once
contract or permission to skip durable readback. Keep a committed intent and remote
ID; classify a timeout as ambiguous until reconciliation.
[Create event](https://learn.microsoft.com/en-us/graph/api/user-post-events?view=graph-rest-1.0).

Prefer immutable event IDs consistently where supported; they remain scoped to the
mailbox and have documented exceptions, including moves to archive mailboxes.
[Immutable IDs](https://learn.microsoft.com/en-us/graph/outlook-immutable-id).
Graph calendarView delta is scoped to calendar/date window; save opaque links only
after applying a complete page round. Unbounded calendar delta is documented beta,
so is not the baseline. Expired cursor recovery must retain delivery receipts.
[Delta scope](https://learn.microsoft.com/en-us/graph/delta-query-events).
429 requires observing Retry-After; SDK retry middleware must be bounded and audited
for ambiguous writes before use. [Throttling](https://learn.microsoft.com/en-us/graph/throttling).

Google permits client-generated event IDs to help recover successful writes when a
response is lost. IDs must obey the documented alphabet/length constraints; no
contact identifiers in IDs. [Creation](https://developers.google.com/workspace/calendar/api/guides/create-events).
A409 with reason=duplicate is not the same as409 batch conflict; use GET and verify
ownership/payload before accepting an existing ID, never generate a fresh ID merely
to retry the same call. 412 means stale version;403 reasons distinguish permissions
from quota. These are our conservative policies atop the protocol.
[Error semantics](https://developers.google.com/workspace/calendar/api/guides/errors).
Private extended properties may carry an app binding but are not an access-control
or cross-adapter preservation guarantee.
[Properties](https://developers.google.com/workspace/calendar/api/guides/extended-properties).
Invalid sync tokens require rebuilding the remote mirror; preserve local intent,
receipts and review/tombstone policy instead of wiping the delivery ledger.
[Incremental sync](https://developers.google.com/workspace/calendar/api/guides/sync).

Google scope choice depends on selected workflow: owned-calendar events, app-created
calendars or broader event access differ, and listing needs its own least-privilege
assessment. Never default to full calendar/ACL access. Public apps may need Google
verification. [Scopes](https://developers.google.com/workspace/calendar/api/auth).
Use platform-supported native authorization, external browser/PKCE where applicable,
with provider-specific redirects and protected tokens; desktop OAuth instructions
alone do not establish an Android implementation.
[Native/desktop OAuth](https://developers.google.com/identity/protocols/oauth2/native-app).

CalDAV needs authenticated discovery and writable collection selection, not a guessed
URL. Create VEVENT at a stable resource with UID and If-None-Match:*; retrieve server
representation/ETag. Modification/deletion needs current If-Match; a412 causes
read/review, never overwrite. UID conflict,404 and authorization errors need distinct
handling. Server normalization, escaping/timezones and unsupported properties need
interop fixtures. [RFC4791](https://www.rfc-editor.org/rfc/rfc4791.html).
Collection sync support is negotiated; invalid sync tokens require rescan while
retaining delivery bindings. [RFC6578](https://www.rfc-editor.org/rfc/rfc6578.html).

## Libraries versus code we own

| Candidate / observed version | Responsibility it could replace | Cost and disposition |
|---|---|---|
| Retain CalendarContract + existing sync adapter | Avoid adding app-owned cloud/auth client entirely | Lowest new app complexity; provider/cloud mapping still unverified; chosen current route |
| Graph Java SDK6.68.0 observed release | Generated models/request builders, paging/middleware over future handwritten REST | JVM/Android/Kiota/auth transitives; no common KMP assumption; defer until direct need |
| MSAL Android8.5.0 observed release | Native Microsoft token acquisition/cache/broker integration | App registration, redirects, consent and tenant/broker testing; separate iOS library; defer |
| Google API Java client2.9.1 observed release | Base transport/auth integration; generated Calendar v3 client still separate | Not a Calendar service version or KMP library; graph/Android compatibility unresolved; defer |
| dav4jvm main documentation | DAV/XML protocol work instead of custom discovery/request code | Uses Ktor; JVM XML parser still required, not automatically commonMain. JitPack pinned commit/version needed; no version selected; defer |
| Ktor/serialization with narrow REST DTOs | Smaller shared HTTP implementation if KMP later adopted | App-owned retry/auth/JSON compatibility responsibility remains; no demonstrated gap; defer |
| Rust DAV/HTTP/ICS library | Potential reusable parsing component | Track13 must demonstrate advantage and ABI/licence/maintenance fit; no Rust connector adopted |

Graph SDK is MIT; Google Java client is Apache-2.0 at inspected upstream licences.
Resolved transitive/native notices, exact auth/client compatibility, footprint and
security advisories must be audited before adoption. dav4jvm inspected main licence text is MPL-2.0; licence obligations must be checked
at its selected revision before distribution. These are uninstalled candidates,
not a compatible dependency lockfile. No performance or development-time gain is
measured. No temporary dependency exists to silently become production.

Version/maintenance primary sources (observed snapshots, refresh on reopening):

- [Graph6.68.0 release](https://github.com/microsoftgraph/msgraph-sdk-java/releases/tag/v6.68.0), [licence](https://github.com/microsoftgraph/msgraph-sdk-java/blob/main/LICENSE)
- [MSAL8.5.0 release](https://github.com/AzureAD/microsoft-authentication-library-for-android/releases/tag/v8.5.0), [Android overview](https://learn.microsoft.com/en-us/entra/msal/android/)
- [Google client2.9.1 release](https://github.com/googleapis/google-api-java-client/releases/tag/v2.9.1), [licence](https://github.com/googleapis/google-api-java-client/blob/main/LICENSE)
- [dav4jvm current scope](https://github.com/bitfireAT/dav4jvm), [changelog](https://github.com/bitfireAT/dav4jvm/blob/main/CHANGELOG.md), [licence](https://github.com/bitfireAT/dav4jvm/blob/main/LICENSE)

Release activity indicates maintained candidate projects, not proven suitability for
this build. MSAL documentation URL initially failed; the official Android overview
was used instead. AppAuth's latest-release URL gave no selected release; no AppAuth
version/support assertion or dependency decision relies on it.

## Original optional endpoint distinction

The [baseline audit](../../research/baseline-audit.md) maps AndroidCallLogSync's
configurable HTTP POST and Device-Name/Token/Number headers, with cleartext allowed
in that reference. The current successor manual slice does not expose that route.
R12 needs a selected receiver and versioned acknowledgment/idempotency contract;
I13 defines its gates. Do not conflate endpoint success with calendar persistence,
or preserve the old cleartext/header policy merely for feature parity.
