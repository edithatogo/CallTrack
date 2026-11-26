# Android 17 call routing specification

Automatic settings let the user choose no calendars, one calendar, or several writable calendars for phone calls and for each participating app. Changing the routes requires explicit confirmation and starts a new logging period. Existing automatic settings keep their destination and period on upgrade. All other directions and pause controls retain their semantics.

Use the Android 17 unified call log, its call account component and VoIP UUID, and targeted Android package visibility for discovery. Distinguish an installed app from a record observed in the native history. The user's Android integration settings control whether calling apps share records. An unattributed integrated row is ineligible for a route. Calendar writes use CalendarContract with the existing durable receipts, recovery and WorkManager. Give each destination an independent scan cursor so it can progress through another destination's failure.

Manual latest-call logging and existing historical sessions retain their phone-call source. New historical previews select one source and one writable calendar. Persist the call source and the calendar identity with the frozen preview. Display the source before confirmation and resume from the saved scope. Older session documents retain phone-call selection and their original recovery contract. Use attributed Android history for app calls and exclude other apps and carrier calls from an app preview. Keep native accessibility, schema migration, physical route and cloud readback gates. Device qualification uses controlled calls and authorised calendars.

Use the shared Calendar Provider for local and account-backed calendars. Existing guarded inserts verify the selected calendar identity before automatic delivery or delivery from a new historical import. For Outlook, qualify a writable calendar exported by Outlook for Android and read back a synthetic event in the selected remote calendar.

New automatic routes bind to native account and calendar identity columns. CalendarContract asserts the writable identity and inserts the event in one provider batch. A replacement calendar row with a reused numeric ID needs its own approval.

Source references are [Android unified call history](https://developer.android.com/develop/connectivity/telecom/call-log-integration), [Android account attribution](https://source.android.com/docs/core/connect/third-party-call-apps#call-log-entries-from-third-party-calling-apps) and [Android package visibility](https://developer.android.com/training/package-visibility/declaring).
