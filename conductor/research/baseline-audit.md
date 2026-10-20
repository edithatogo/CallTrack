# Baseline audit

Date: 2026-10-20. Status: static inspection; no app installed or runtime tested.

## Evidence and reproducibility

- CallTrack supplied on connected phone: `/sdcard/Download/CallTrack_1.12_APKPure.apk`.
- Local private copy: `.local/apk/CallTrack_1.12.apk`; SHA-256 `425d580a4947cc97036ad3e4351b9487b5a563dcf956e04d7f55fece8a542b25`.
- Manifest: package `app.calltrack`, version 1.12 / code 13, minSdk 14; no explicit targetSdk attribute. APK origin/authenticity not independently established.
- Androguard manifest, resource and Dalvik instruction inspection: `.local/apk/inspection.txt`. Raw APK and extracted instructions remain locally excluded from Git; do not redistribute them as project source. No original source licence established.
- AndroidCallLogSync checkout: `.local/AndroidCallLogSync`, commit `aaa56443a294aa61bad48b620427e53f9521c5fe`, GPL-3.0. Latest commit observed 2025-10-28. Main declares 1.0.6; latest GitHub release observed v1.0.4. Release binary and main are not interchangeable evidence.
- Call-Log-to-Cal checkout: `.local/Call-Log-to-Cal`, commit `ccf3166a8e3ffc55b7df02236ba20d5ed737f132`, MIT. Latest commit observed 2016-05-11; no published releases.
- Source clones are reference material, not a selected or published fork. Phone reported Android 17 / API 37 earlier in this session.

## CallTrack feature and API reconstruction

Evidence below is static implementation evidence, not proof of behaviour on modern Android. Method names refer to `app.calltrack` classes in the local instruction report.

| Behaviour | Evidence | Interpretation |
|---|---|---|
| Enable/disable | `CalltrackReceiver.onReceive`, preference resources | Default enabled read; disabled receiver path exits |
| Type filters | `CalltrackUtils` constructor and copy methods | Independent incoming, outgoing and missed switches |
| Destination selection | `CalltrackPreferenceActivity.loadCalendarList` | Selected calendar ID stored as preference; exact eligibility rules still to test |
| Automatic capture | Manifest PHONE_STATE receiver; `onReceive` | On idle, sleeps 5 seconds then calls `copyNewCallsToCalendar`; replace blocking receiver design |
| Historical import | Menu resources; `copyAllCallsToCalendar` | “Sync Entire Call Log”; imports available device history, not deleted history |
| Call metadata | Both copy methods; `CallRecord` constructor | Reads number, cached contact name/type, direction, date, duration |
| Titles and descriptions | `CallRecord` | Direction prefix, name or number fallback; unknown-number handling; description contains number and duration in seconds |
| Timing | `roundTimeToSecond`, `CallRecord` | Start rounded to seconds; end = start + duration seconds × 1000; zero-duration yields zero-length event |
| Event creation | `createEvent`, copy methods | Calendar ID, title, description, dtstart, dtend, default timezone; CalendarContract event insertion |
| Duplicate check | `isEventInCalendar` | Calendar ID + start + end + title equality; title edits can invalidate matching |
| Error feedback | Copy methods, resources | Error messages exist; robust durable retry not established |

No custom public HTTP API is apparent in the inspected manifest or main classes. The relevant API map is Android `CallLog.Calls` → transformation → `CalendarContract.Events`, with `TelephonyManager`/PHONE_STATE triggering, SharedPreferences configuration and calendar observation. Legacy content URI strings are present but do not prove active use. The manifest has phone-state, contacts and calendar permissions, no INTERNET permission and no explicit READ_CALL_LOG permission. Modern permissions must be designed afresh.

Remaining reconstruction tasks: decode preference defaults and calendar eligibility exactly; trace new-call scan termination and five-insert cap; inspect missed/zero-length display; confirm contact-label semantics; run controlled legacy-compatible emulator fixtures if practical. Do not reproduce legacy defects merely to match implementation.

## FOSS capability and implementation map

| Area | AndroidCallLogSync | Call-Log-to-Cal |
|---|---|---|
| Language/UI | Java, XML Views, AndroidX, View Binding | Java, XML Views, legacy Support libraries |
| Build | compile/target 32, min 26, AGP 7.2.2 | compile/target 23, min 14, AGP 2.1.0-beta3 |
| Acquisition | CallLog provider; ascending IDs beyond checkpoint | Call/SMS lists with export actions |
| Automation | WorkManager periodic request 15 minutes plus manual work; OS timing is not exact | Manual per-entry export in inspected path |
| Output | HTTP POST to configurable endpoint | Google Calendar API OAuth, primary calendar hard-coded |
| Payload | JSON array: ID, NUMBER, TYPE, DATE (epoch ms), DURATION (seconds) | Calendar Event; call duration in description, start=end |
| Contact handling | No contact names in upload payload; empty numbers skipped; + rewritten to 00 | Contact name and number presented |
| Progress | Last call ID checkpoint; special -1 starts at latest record | No durable deduplication established in export path |
| Retry concern | Worker reports success even on sync failure | Exceptions surfaced through old AsyncTask flow |
| Networking | Device-Name/Token/Number headers; HTTP 200 success; cleartext allowed | Direct Google API; old dependencies and account handling |
| Configuration | Endpoint testing, sync enable, device metadata, runtime call-log permission | Google account selection and manual export |
| Fit | Preferred candidate for acquisition/scheduling reuse; calendar output missing | Reference for features; not selected as build base |

Source anchors: [Sync.java](https://github.com/MarkoBL/AndroidCallLogSync/blob/aaa56443a294aa61bad48b620427e53f9521c5fe/app/src/main/java/com/markobl/calllogsync/Sync.java), [SyncWorker.java](https://github.com/MarkoBL/AndroidCallLogSync/blob/aaa56443a294aa61bad48b620427e53f9521c5fe/app/src/main/java/com/markobl/calllogsync/SyncWorker.java), [SettingsFragment.java](https://github.com/MarkoBL/AndroidCallLogSync/blob/aaa56443a294aa61bad48b620427e53f9521c5fe/app/src/main/java/com/markobl/calllogsync/SettingsFragment.java), [CalendarActivity.java](https://github.com/JianLoong/Call-Log-to-Cal/blob/ccf3166a8e3ffc55b7df02236ba20d5ed737f132/app/src/main/java/io/github/jianloong/calllogtocal/CalendarActivity.java), [CallLogRVAdapter.java](https://github.com/JianLoong/Call-Log-to-Cal/blob/ccf3166a8e3ffc55b7df02236ba20d5ed737f132/app/src/main/java/io/github/jianloong/calllogtocal/CallLogRVAdapter.java).

Callvent remains a commercial feature reference (notes, reminders, tags, batch save), not a reusable code baseline. Its documented calendar action requires saving; no unattended parity established. [Developer documentation](https://callvent.app/en/).
