# Track 3 selected design (before implementation)

- Adopt the qualified fork into the app workspace, retaining GPL source provenance and the modern build configuration. Development identity: org.calltrack.community.dev; release identity/signing remains unselected.
- Use native CalendarContract for provider-neutral calendar discovery/writes. Calendar intents require a user save and cannot underpin future unattended logging. Research wrapper/sync/parser alternatives; no new runtime dependency is selected for this small local-provider slice.
- Separate CallSource, EventMapper, CalendarSink and SyncLedger. A real source adapter reads only the latest row when the user explicitly requests it; native acceptance injects a synthetic source and writes to an app-test-owned local calendar. No live calls or call-log modifications are required.
- Preserve exact source milliseconds; compute duration safely. Zero-duration calls use a one-minute visual marker while retaining actual duration0 in the description. This is an explicit provisional rendering policy for the parity track to review.
- Ledger v1 uses platform SQLite, per-install source namespace/row identity plus destination, event ID, marker and payload fingerprint. Serialize writers; read back actual provider fields before recording success. Reconciliation by an app-owned marker can recover an event inserted before ledger commit. Edited/deleted/ambiguous records require review rather than overwrite/recreate. Full migration/cross-device/retention policy remains in parity.
- Default manual calendar writes are private/free, with no attendees or reminders. New slice performs no network requests. Legacy endpoint code remains retained and disabled. Native calendar persistence and remote-provider sync are different outcomes.
- Test fixtures use an isolated LOCAL calendar account and a per-run token. Test-only calendar provisioning uses sync-adapter parameters solely for that local fixture; normal event sink writes use public application fields/URIs. Remove only matching fixture calendars/events and new test packages.

## Review refinements

- Treat uncertain ADB installs as owned attempts requiring absence readback, and preserve fixture recovery code if cleanup cannot be proved.
- Disable picker/logging during calendar refresh and ignore stale callbacks; preserve explicit choice or placeholder. Use a scroll container for smaller screens.
- Explicitly exclude all app storage domains from cloud backup and device transfer alongside allowBackup=false. Backup/restore qualification remains a later requirement; these are local configuration checks, not an executed transfer test. [Android backup contract](https://developer.android.com/identity/data/autobackup) was refreshed 2026-10-20.
- Add CallLog's documented URI limit to the bounded latest-row query.

- Known insert IDs are attempt receipts, not success flags. Retain them across readback mismatch/provider errors so retry cannot insert blindly; every success still verifies actual provider fields. A true crash before ID receipt commit still relies on the tested provider marker. Track 4 must examine durable pre-insert intents and lost-marker/tombstone cases across restores and external synchronisers.
