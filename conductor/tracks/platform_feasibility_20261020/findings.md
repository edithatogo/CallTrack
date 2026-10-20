# iOS and desktop feasibility findings

Refreshed2026-10-21 at baseline `c2b1a5c`; activation `cdd61e8`. Public documentation,
local SDK inventory and a compile-only surface probe. No phone/call history, private
system databases, calendar accounts or developer-account entitlements were read.
No app installation, signing, permission request, call placement or default change.

## Important correction to earlier research

Apple now documents LiveCommunicationKit ConversationHistoryManager (iOS/iPadOS26+).
An entitled **default dialer** can access recent cellular history from when it
became default. The preparation guide specifies an EU-registered developer account
and a test device located in the EU to test default-dialer behavior. This is
conditional documented support, not unavailable everywhere and not Android-style
unrestricted prior-history access. No project eligibility or deployment beyond
those documented test conditions is established.
[Default dialer guide](https://developer.apple.com/documentation/livecommunicationkit/preparing-your-app-to-be-the-default-dialer-app),
[entitlement](https://developer.apple.com/documentation/bundleresources/entitlements/com.apple.developer.dialing-app).

History records expose UUID/date/direction/duration/handles/status/read state.
Their semantics, completeness, retention, missed/blocked/withheld/SIM mapping and
behavior across default changes require real eligible-device tests. Do not copy
Android type codes or treat isRead as a callback outcome.
[History manager](https://developer.apple.com/documentation/livecommunicationkit/conversationhistorymanager),
[record attributes](https://developer.apple.com/documentation/livecommunicationkit/conversationhistorymanager/recentconversation).

CallKit CXCallObserver separately observes active calls; CXCall exposes call UUID
and state flags. These APIs do not establish a historical number/duration archive
or unattended durable capture. App-owned VoIP metadata differs from all other
apps' calls. [Observer](https://developer.apple.com/documentation/callkit/cxcallobserver),
[call attributes](https://developer.apple.com/documentation/callkit/cxcall).

## Capability matrix

D=documented candidate, C=conditional, N=not established by inspected public APIs,
T=compile-only local surface checked. None means operationally accepted.

| Platform/workflow | Acquisition | Calendar/import/background | Outcome |
|---|---|---|---|
| Ordinary iOS companion | Manual entry or deliberate supported export import D; general cellular archive N | Native document picker and EventKit D; BG tasks scheduled by OS, no universal prompt timing | Recommended limited iOS scope after native tests; no product built |
| iOS26+ default dialer | Recent cellular history C from default-role activation, entitlement/test-region gates | Separate dialer lifecycle/default-loss/background and history tests needed | Separate future research option, outside initial passive logger scope |
| iOS app-owned VoIP/business telephony | Own service events C, provider permission/contract dependent | CallKit/LiveCommunicationKit integration is not other-app history access | Not initial scope; no VoIP service selected |
| macOS companion | Manual/import D; general cellular/FaceTime history N | AppKit user picker/EventKit D/T; native sandbox/TCC and sleep/restart behavior untested | Recommended first native integration experiment on this host; not a committed production target |
| Windows companion | Manual/import candidate; PhoneCallHistory APIs C | Capability/store/data-population/app-type gates; no Windows runtime here | Do not claim Windows has no history API; also do not promise linked-phone/other-app coverage |
| Linux desktop | Manual/import and selected cloud/CalDAV route candidates | No single universal native calendar/acquisition route qualified here | Further platform-specific qualification if needed; no universal impossibility claim |

Windows PhoneCallHistoryManager exposes a history store with phoneCallHistory /
phoneCallHistorySystem capability requirements. Capability and Store approval rules
depend on app/package type. API presence does not establish meaningful records for
a desktop or arbitrary linked phone/VoIP services; no Windows host tested.
[API](https://learn.microsoft.com/en-us/uwp/api/windows.applicationmodel.calls.phonecallhistorymanager?view=winrt-26100),
[capability rules](https://learn.microsoft.com/en-us/windows/apps/package-and-deploy/app-capability-declarations).

## Calendar and foreground workflow

EventKit write-only cannot read events, even those created by the app, and returns
a virtual calendar for calendar listing. It therefore cannot satisfy our selected
calendar/reconciliation/readback contract. A foreground iOS system editor may allow
a user-mediated save without granting the app full access, but that is a separate
manual-save capability with no app-side verification guarantee. Full event access
is needed if the app must read/reconcile; request only when that feature is chosen.
Usage descriptions and macOS sandbox calendar entitlement apply.
[Event store access](https://developer.apple.com/documentation/eventkit/accessing-the-event-store).

Proposed useful workflow: user selects a supported synthetic archive or enters a
call manually → validate schema/provenance/time → preview/redact → choose a writable
destination → deliberate save → read back with qualified permission → retain an
operation receipt. Never silently turn an ICS file into live sync, re-import
remote app events as calls, or replay restored receipt IDs as delivery proof.
Track10/11 contracts apply. No parser, permission prompt or provider adapter exists
in the desktop UI experiment; its4 offscreen tests remain historical evidence only.

UIKit document picker and AppKit NSOpenPanel are native input candidates. The
actual file capability, security-scoped access lifetime, cancellation, partial
reads, resource limits and inaccessible-provider behavior need native fixtures.
[UIKit picker](https://developer.apple.com/documentation/uikit/uidocumentpickerviewcontroller),
[AppKit picker](https://developer.apple.com/documentation/appkit/nsopenpanel).

Apple background work is time-limited and scheduled by the system. A durable
foreground operation may need deferred recovery; neither BG tasks, push, KMP nor
Rust grants constant execution or guaranteed near-real-time call capture.
[Background strategies](https://developer.apple.com/documentation/backgroundtasks/choosing-background-strategies-for-your-app).

## Dependency and ownership comparison

| Option | Responsibility and cost | Disposition |
|---|---|---|
| Native EventKit/AppKit/UIKit with Swift | Permission/calendar/file boundaries; OS SDK, no third-party wrapper; requires native lifecycle/privacy/packaging tests | Preferred first adapter experiment; no production adoption |
| Native UI + later KMP domain | Could reuse mapping/rules without shared UI | Reopen Track8's exact identity/encoding/storage/Swift-consumer tests before extraction |
| Compose Multiplatform + platform adapters | Could replace duplicated layouts/state wiring | Track9 desktop sample is not iOS/native integration; preserve Track7/8/9 deferrals |
| Direct Graph/Google/CalDAV | Could cover calendars outside native store | Track10's demonstrated-gap/auth/server acceptance gates; no new client selected |
| Rust/UniFFI native wrapper | Could share a proven parser/engine | Adds binaries/FFI/cancellation/licence maintenance; cannot create platform capability; Track13 evaluation only |
| Private system database/backup scraping | Would bypass supported app data boundaries and create brittle extraction | Rejected as product architecture; no such file or backup accessed |

Installed host: CommandLineTools selected, Swift6.4, macOS SDK27.0, arm64 host. Full
Xcode/iOS simulator tooling is not selected or qualified. These are observed local
compiler inputs, not a supported release matrix or minimum-OS runtime test.
A compile-only Swift probe references modern macOS14+ EventKit permission/list/read
signatures without invoking them. Typechecking proves surface availability only.
No third-party dependency/ABI, entitlement, native packaging or TCC grant follows.

No library was installed or resolved. Platform SDKs remain governed by installed
Apple toolchain terms; no new licence acceptance was performed. If KMP/Compose/
Rust or API clients are later selected, pin exact versions/transitives, licences,
min-OS/compiler/SDK/ABI support and security notices at that trial. No performance,
size, battery or maintenance advantage is measured.

Apple HTML pages require JavaScript and web Markdown fetch rejected content-type;
public .md variants were successfully retrieved directly and saved only in excluded
`.local/platform-evidence/`. Conclusions use the actual Markdown content, not a
missing web-rendered page. Primary documentation is a dated snapshot; refresh
entitlement/region/OS policy before any default-role experiment.
