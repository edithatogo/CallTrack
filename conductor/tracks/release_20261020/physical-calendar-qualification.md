# Physical-call and account-calendar qualification

Date:2026-10-22. Status: prepared; device/participant and account selection pending.
Track5 remains active. The user requested physical-call and claimed Google/Outlook
route qualification. Earlier emulator-only preference is superseded for this work;
actual participation and exact device/calendar selection remain necessary.

## Verified starting point

ADB currently exposes only the two retained emulators. No physical phone is visible.
No physical installation, permission grant, call, account query or calendar write
has occurred in this continuation. The exact15998ba unsigned release hash remains
89a548489035a6df8c464cc621e49dfbb0c9d6d16619bbe54f27f78e612d5f86.
Its existing development-signed release copy was signature-verified and every
original APK entry compared against that frozen unsigned artifact. This prepares
an installable development test copy; it is not production signing acceptance.
Private discovery and artifact receipts are under
`.local/release-evidence/physical-routes-20261022/`.

## Route under test

The app uses Android CalendarContract through AndroidCalendarSink. It does not
implement direct Google Calendar API, Microsoft Graph or OAuth connectors, and
its application manifest has no INTERNET permission. Destination choices require
visible, sync-enabled calendars with contributor-or-higher access. Provider access
and permissions are described by the [Android Calendar Provider documentation](https://developer.android.com/identity/providers/calendar-provider).
A local numeric calendar ID is not a Google/Graph calendar ID.

| Route | Prerequisite | Required outcome before a compatibility claim |
|---|---|---|
| LOCAL | Explicit writable test destination in Android provider | Physical call acquisition and exact local event/receipt readback; no remote claim |
| Google | Selected account's dedicated test calendar visible/writable to this app through Android provider | App-created event in that exact calendar, then matching event in the same account/calendar via independent server/API or web readback |
| Outlook/Microsoft365 | Selected account/calendar exported into Android provider through a supported existing sync route; writable in this app | Same local proof, plus matching event remotely in the selected Outlook/Microsoft365 calendar |

Microsoft documents Outlook for Android calendar export to default calendar apps
and two-way support for Microsoft365/Outlook.com and specified on-premises account
configurations. [Microsoft instructions](https://support.microsoft.com/en-us/outlook/how-do-i-sync-my-outlook-calendar-to-the-default-calendar-app-s).
That documentation does not qualify the user's account, policy, custom test calendar
or the app. An Outlook calendar merely visible in Outlook's own UI is insufficient.
If the test calendar is absent/read-only in this app, record a blocked native route;
do not substitute another calendar or add a direct connector implicitly.
Google likewise documents device/computer sync, with separate sync troubleshooting.
[Google instructions](https://support.google.com/calendar/answer/151674?hl=en).
These references were refreshed2026-10-22 and are prerequisites, not runtime proof.

## Select and preserve the actual installation

1. User connects/unlocks the phone and accepts the computer's debugging prompt.
   Identify one explicitly selected physical serial and record model/API/OS privately.
   Read-only package/version and permission inspection precedes installation. Confirm
   the package's existing state and certificate when present; no clear-data,
   uninstall, downgrade, mismatched-key replacement or changing an existing working
   logger's destination. An existing development installation requires a preservation
   plan before replacement. Emulator harnesses continue to refuse physical devices.
2. Use the exact candidate release test copy with its observed development certificate,
   not a rebuilt HEAD APK, version11 fixture or schema/provider instrumentation APK.
   Fresh installation is appropriate only if that package is absent on the selected
   phone. User grants sensitive permissions through the normal app/Android UI.
   Do not use root, permission allow-list overrides, call-log injection or phone-test
   instrumentation as a substitute for actual user-granted access.
3. Select the user-controlled second phone and dedicated empty test calendar(s).
   Confirm account identity and Android/remote calendar mapping privately. Keep
   existing accounts, sync configuration and user events intact. Creating calendars,
   changing account-wide Outlook export or granting new cloud consent requires
   an understood selected scope; a connector's default identity is not authority
   to use an unrelated account. User handles sign-in/consent if required.
4. Record original automatic state and destinations. Activate only for the deliberate
   test window and controlled test numbers. Do not import historical calls or press
   latest-call logging before a controlled test call exists. Pause if unrelated
   calls arrive. Do not replay receipts into the second route: pause, choose/confirm
   the new destination, then generate new controlled calls after activation.

## Staged acceptance matrix

Start with three controlled calls on the first selected route, then repeat with
new calls for each other route. Prompt the user one call at a time; no automated
outbound calls, texts or invitations. Select ordinary cellular calls first; do not
infer VoIP, dual-SIM or other OEM acceptance from them.

| Case | User action | Pass criterion |
|---|---|---|
| PC01 | Answer an incoming test call for a few seconds | Actual phone history direction/time/duration matches; one local event and durable app receipt; one matching remote event where claimed |
| PC02 | Place and complete an outgoing test call | Same exactness and uniqueness, with outgoing classification |
| PC03 | Let an incoming test call become missed | Source history actually reports missed; one-minute visual marker with body stating zero actual duration and one event, not guessed from a rejected/blocked call |
| PC04 | Pause, perform an agreed control call, then explicitly resume and perform a new call | Paused call produces no delivery; resume does not silently backfill it; new active call delivers once |
| PC05 | Lock screen/background app during a new controlled call | Real call-end wake or later recovery delivers once; actual delay and power/OEM state recorded |
| PC06 | In a scheduled bounded test window, interrupt network then restore | Local persistence and later remote delivery each verified; no duplicate; restore original network state without interrupting unrelated calls/work |
| PC07 | Edit one test event in remote calendar, allow sync, then cause normal app reconciliation | Human changes preserved locally/remotely, no replacement event |
| PC08 | Delete another identified test event remotely, allow sync and reconcile | User deletion preserved; no automatic recreation; receipt/review outcome checked |
| PC09 | Reopen/retry normal app flows for already-delivered calls | Same controlled identities retain one delivery per destination; no duplicate |

PC04–PC08 follow only after the basic route works and the user can participate in
those actions. Avoid changing battery policy merely to obtain a pass: record default
settings first and document any user-selected alternative as a separate condition.
Manual accessibility, reboot, dual-SIM/OEM breadth and seven actual observation
days remain separate coverage; these few calls do not close those gates.

## Evidence and verdicts

For each case retain a private run/case token, exact candidate/signer, route mapping,
activation generation/time, expected call direction, phone-history observation,
local event identifier and app receipt/status, remote event identifier and returned
calendar/account identity, timestamps/timezones, actual local and sync delays,
observed count, edits/deletions and cleanup outcome. Keep phone numbers, names,
account addresses, serials and full event content outside Git. Use aliases and
aggregate outcomes in committed evidence. Never query or export the entire call
history or calendar when a bounded test window/known event ID suffices.

Read actual persistent receipt fields where the existing app UI exposes them; a
transient success/status label alone is not a durable receipt readback. If the
necessary fields are unavailable on the physical build, record an observability
gap and propose bounded diagnostics separately. Do not use root or the destructive
schema-fixture instrumentation package to obtain private evidence.

Remote verification must read the event the app created. Creating an event through
a Google/Graph connector only tests the connector. Read the exact known ID or
search the selected test calendar within the bounded call window and match marker,
title, body, start/end and duration/timezone semantics. A sync flag, account label,
local provider ID or same-phone calendar UI alone is not server-side proof. User
web readback can be recorded as witnessed/manual evidence; it is not API evidence.
If a sync adapter rewrites/removes the app marker, record the observed consequence
and require a passing identity/duplicate test rather than assuming equivalence.

Use explicit outcomes: pending participation, blocked access/route, local-only pass,
remote delivery pending, route/case passed, or failed with retained recovery state.
Measure prompt logging and later recovery separately; agree an observation budget
before the call and mark timeout/delay honestly. No guaranteed delivery deadline.
Only passed case/route combinations can support compatibility claims.

## Cleanup and review

Pause the test activation. Delete only individually identified unedited test events
agreed for cleanup; preserve human-edited test cases unless their deletion is
explicitly selected. Verify removal in both Android and the selected remote calendar.
Retain case IDs and recovery tools if cleanup is uncertain; do not delete an account,
turn off account-wide export, clear data or uninstall to conceal residue. Restore
agreed app/device settings with readback and retain the installed app unless its
removal is explicitly requested. Review actual outcomes and fixes before accepting
physical or remote routes. If production code changes, refreeze/reproduce the
candidate and repeat affected acceptance; new host-only documents do not relabel15998ba.
