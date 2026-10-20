# Findings: CallTrack and FOSS capability mapping

Status: completed static capability mapping. Source identity and file digests are in [sources](sources.json). Findings describe the supplied APK and pinned checkouts only. No installed app, phone call log, calendar contents or remote endpoint was used. A decompiler is an interpretation aid; manifest/resource values and critical branches/constants were checked against Dalvik instructions. Original APK authenticity and licence remain unestablished.

## Evidence locators

- **CT-M**: private `.local/apk/inspection.txt`, manifest lines 1–20; package/version/minSdk/permissions/receiver.
- **CT-P**: private `preferences.xml`; resources in `inspection.txt` lines 23–56; `CalltrackPreferenceActivity.loadCalendarList`, `onCreate`, `showErrorAndExit`, menu/run methods.
- **CT-R**: `CallRecord.<init>` instructions lines 58–215; title/description and timestamp arithmetic.
- **CT-S**: `CalltrackReceiver.onReceive` lines 588–611; `CalltrackUtils` constructor, `copyAllCallsToCalendar` and `copyNewCallsToCalendar` lines 613–973.
- **CT-E**: `createEvent`, `isEventInCalendar`, `roundTimeToSecond` lines 974–1060.
- **ACS**: `app/src/main/java/com/markobl/calllogsync/` in AndroidCallLogSync commit `aaa56443a294aa61bad48b620427e53f9521c5fe`.
- **CLC**: `app/src/main/java/io/github/jianloong/calllogtocal/` in Call-Log-to-Cal commit `ccf3166a8e3ffc55b7df02236ba20d5ed737f132`.

Private files are reconstruction inputs, not redistributed original application source. Recreate using Androguard AnalyzeAPK, `get_android_manifest_xml`, resource strings, AXMLPrinter on `res/xml/preferences.xml`, DEX class/method instruction iteration and `get_source`; record the same APK digest before comparing. Decompiled numeric representations must be interpreted as typed DEX constants: the rounding divisor's 64-bit bits decode to double 1000.0, not the decimal integer printed by the decompiler.

## Original CallTrack: feature and requirement map

| ID / evidence | Observed implementation | Successor treatment |
|---|---|---|
| CT01 / CT-M | APK 1.12/code13, minSdk14, no explicit targetSdk. READ_PHONE_STATE, READ_CONTACTS, READ_CALENDAR, WRITE_CALENDAR; no explicit READ_CALL_LOG or INTERNET | Historical facts only; modern install/permission feasibility belongs to build track |
| CT02 / CT-P, CT-S | Enable switch and three independent type switches default true; auto path checks enable; manual history path applies type switches but does not check enable | Preserve separate controls, specify manual-import/pause semantics explicitly |
| CT03 / CT-P | Calendar list queries ID/name without writable/access-level filter, excludes null names. XML default ID is 1; list summary fallback is first calendar; write fallback is also 1. Missing/stale ID can give misleading summary | Select writable destination explicitly; never silently switch to another calendar |
| CT04 / CT-P | No-calendars dialog disables logging on acknowledgment and finishes; calendar table observer refreshes list; activity requests forced/expedited calendar sync | Recoverable modern state needed; sync request is not proof of remote sync |
| CT05 / CT-S | PHONE_STATE receiver: when enabled and TelephonyManager idle, sleeps five seconds then scans | Behaviour intent: unattended logging; replace blocking receiver; no exact latency promise |
| CT06 / CT-S | Auto scan sorts date descending, starts counter at 1, increments for each examined row, loops while counter <5: at most FOUR examined rows. Already-present and filtered rows consume the limit. No durable checkpoint | Correct earlier ambiguous five-insert description; recover all eligible missing rows |
| CT07 / CT-S | Full import queries all available rows date descending, applies filters/dedup, buffers event values and performs one bulkInsert; progress dialog, no range/preview/cancel logic found | Full import is baseline; bounded batches, preview/range/cancellation are enhancements |
| CT08 / CT-R | Prefixes: “Call From”, “Call To”, “Missed Call From”. Cached contact name preferred, number fallback, short (<3 chars) or null number labelled unknown in title | Preserve useful output semantics, not fragile fallback bugs |
| CT09 / CT-R | Description adds cached number-type label via literal array [Work, Home, Mobile, Other, Other, Other], number, duration seconds. Null number later concatenated unguarded | Label mapping can be inaccurate; validate contact metadata and handle null safely |
| CT10 / CT-R, CT-E | Start rounds to nearest second (not truncation). End adds duration×1000 using int multiplication before long conversion; zero-duration event has equal start/end; current device timezone stored | Use safe arithmetic; define zero-duration display; timezone testing required |
| CT11 / CT-E | Events write calendar_id, title, description, dtstart, dtend, eventTimezone via CalendarContract; no explicit attendee/privacy/availability/reminder fields | Local calendar path is provider-neutral; defaults must be explicit and verified |
| CT12 / CT-E | Dedup uses calendar ID, rounded start/end and title. Description ignored. No source-to-event ledger or within-batch identity | Title edits or contact renames can duplicate; distinct same-second calls can collide; preserve edits with stronger identity |
| CT13 / CT-S | Query/construct work outside insertion exception handler; bulkInsert result ignored; empty/no-new paths can leave cursor unclosed. No boot catch-up receiver, persistent retry, file export, notes or audio API in inspected app classes | Document gaps; do not promise error recovery from existing behaviour |

No custom public CallTrack HTTP endpoint or external developer API was identified in the inspected application classes/manifest. Relevant integration is platform providers plus broadcasts and preferences. This is not a claim about every historic app version, unused resources or external service ever associated with the name.

## Platform API/data contract

| Component | CallTrack | AndroidCallLogSync | Call-Log-to-Cal |
|---|---|---|---|
| Input | CallLog.Calls: number,type,duration,name,date,numbertype | CallLog.Calls ID,number,type,date,duration | CallLog.Calls cached name/type/date/duration/number; SMS content provider and PhoneLookup |
| Trigger | PHONE_STATE → idle → sleep5s | WorkManager periodic15min + one-time work, boot registration | UI AsyncTask fetch; user export clicks |
| Output | CalendarContract.Events bulkInsert | HTTP POST JSON array to user endpoint | Google Calendar v3 events.insert(primary) |
| Output fields | Calendar ID/title/description/start/end/timezone | ID integer, NUMBER string, TYPE integer, DATE epoch ms, DURATION seconds | Summary from name, description duration, start=end; SMS description is body |
| Identity/progress | Calendar/time/title equality, no checkpoint | Last numeric source ID in SharedPreferences | No export ledger/dedup in inspected path |
| Credentials | No network login in inspected APK | Device token/name/optional number headers | GoogleAccountCredential using CALENDAR scope; account selection/GET_ACCOUNTS |

## AndroidCallLogSync capabilities and limits

| Source | Capability / gap |
|---|---|
| Config, Sync | Sync defaults off. Endpoint required to enable. First -1 checkpoint becomes highest ID minus1, so normal first run uploads latest row, not full history; no date-bounded import UI |
| Sync | Ascending IDs > checkpoint; no call-type filtering, no name/SIM payload. Number sanitation replaces + with00, retains digits/#/*, drops other characters; null can throw; empty sanitised number skipped |
| Sync, SyncWorker | Checkpoint advances only in posted success callback. Worker returns success regardless of result; callback is posted to main handler after joined network thread. Periodic rerun may retry but WorkManager retry/backoff is not signalled. Overlapping manual/periodic jobs can replay IDs; no remote idempotency contract enforced |
| Sync | Reads all selected rows into one JSON payload. `more` is read after cursor exhaustion and does not implement paging. A batch with only skipped numbers does not advance checkpoint in normal path |
| SettingsFragment | Endpoint change/test calls sync with last ID0 and Test-Run header, reading actual available history. No synthetic test payload. Endpoint recipient must implement any dry-run semantics; app does not enforce them |
| Sync | POST headers Device-Name, Device-Token, Device-Number and optional Test-Run; Content-Type/Accept JSON; connection/read timeout25s; only HTTP200 treated as success, response text preserved |
| Manifest, SettingsFragment, RandomString | HTTP and HTTPS accepted; cleartext enabled; token in private preferences and visible/copyable in settings, generated with java.util.Random. Do not treat it as a qualified secure authentication design |
| LogItem, MainFragment, LogFragment | Status/history UI; newest100 log entries in preferences; server response/error can be retained. Redaction/retention review required |
| SettingsFragment, Config | Name/number settings, configuration lock, two-step reset clears config/checkpoint/log and disables sync; battery/unused-app settings guidance; permission request for READ_CALL_LOG |
| AutostartReceiver | Registers work on boot intents; declared receipt/boot registration is not runtime background proof |

## Call-Log-to-Cal capabilities and limits

CallLogFragment reads all rows newest first, uses cached contact name or “Unknown Number”, labels incoming/outgoing/missed in UI. Unknown call-type values have no switch default. CallLogRVAdapter launches a per-call CalendarActivity; start/end equal source date, duration only in description. CalendarActivity inserts into `primary`, uses broad CALENDAR scope, colour ID11, old Google account flow, connectivity checks and API exception UI. No automatic logging or export dedup found. Displaying number in the export screen does not mean it is included as a structured event field; exported description is only duration.

SmsLogFragment lists all SMS, looks up contacts with per-number cache, shows inbox/sent/draft labels; SmsLogRVAdapter passes SMS body as calendar description. PermissionActivity gates initial entry on SMS/contacts/call-log permissions; callback launches MainActivity without checking grantResults. Thus optional SMS cannot be adopted unchanged into a minimal call-only permission model.

## Dependency and custom-code inventory

Versions are pinned source facts, not recommended production choices; resolved transitive graph is deferred until a build is qualified.

| Project / role | Declared dependencies/tooling | App-owned responsibilities to reconsider |
|---|---|---|
| ACS build | AGP7.2.2, Gradle7.3.3; compile/target32,min26; Java8 | Build qualification and current SDK adaptation |
| ACS scheduling | work-runtime2.7.1 | SyncWorker's success/checkpoint/concurrency semantics; library alone does not provide exactly-once effects |
| ACS UI | appcompat1.4.1, Material1.5.0, constraintlayout2.1.3, navigation-fragment/ui2.4.1, legacy-support-v4 1.0.0, recyclerview1.2.1, preference1.2.0; View Binding | Settings/config duplication, manual log UI |
| ACS data/network | Gson2.9.0, org.json/platform HttpURLConnection, SharedPreferences | Serialization duplication, raw HTTP, token generator, source query, log storage and ID checkpoint |
| ACS tests | JUnit4.13.2, AndroidX JUnit1.1.3, Espresso3.4.0 declared; no test files found in checked-out app/src | Meaningful contracts must be introduced; declared dependencies are not passing tests |
| CLC build/UI | AGP2.1.0-beta3, Gradle2.10, buildTools23.0.2; compile/target23,min14; Support v4 23.1.1/appcompat23.2.1/cardview23.0.+/recyclerview23.0.+ | Old AsyncTask lifecycle, custom SlidingTabLayout/Strip and ProgressBarCircular |
| CLC permissions/network | easypermissions0.1.5, play-services-identity8.4.0, google-api-client-android1.20.0, calendar v3-rev125-1.20.0, Gson2.4 | Google account/API path, manual DTO maps, null/denial handling |
| CLC tests | JUnit4.12; example unit/instrumentation files | Existing example tests do not establish call/export behaviour |
| CallTrack APK | Platform framework references; no separate third-party library identified in small DEX inspection | Receiver, mapping, cursor lifecycle and calendar queries need modern design; original source not licensed for reuse |

Both FOSS source trees were clean at inspection. Main ACS declares1.0.6 while previously observed latest release wasv1.0.4: no release binary was inspected here; no equivalence or current release freshness claim follows. CLC's MIT file and ACS's GPLv3 file are recorded; dependency licence compatibility remains a release/build task. Callvent remains commercial feature inspiration only, outside these two FOSS source audits.

## Completion boundary

Mapping is complete for the supplied APK's app classes/resources and both pinned projects' implemented flows/build declarations. Build success, installed-device behaviour, provider cloud sync, original binary authenticity, store eligibility, performance and future dependency selection remain unverified and are assigned in [requirements and uncertainties](requirements.md). Completing discovery does not claim completion of those later tracks.
