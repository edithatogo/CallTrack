# Development data lifecycle

Observed implementation as of 2026-10-20; this is not a published store privacy
policy or a claim that all planned retention controls exist. See the
[release readiness track](conductor/tracks/release_20261020/index.md).

The current launcher reads the latest call only after an explicit action and
permission grant. It reads row ID, time, duration, type, cached name, number,
number presentation and phone-account ID. It does not request contacts, SMS,
microphone or location access. The manifest omits INTERNET. Calendar software may
still synchronize an event to the chosen account and disclose it to that calendar's
members. Private/free event flags do not encrypt content or revoke calendar access.

| Location | Data and purpose | Retention/deletion behaviour now |
|---|---|---|
| Android call-log provider | Source call metadata | Owned by the system; this app does not delete source rows |
| Private `slice` preferences | Random source namespace and selected calendar ID | Retained across restart; no in-app reset workflow |
| Private `calltrack-ledger.db`, schema2 | Source/destination keys, event IDs, fingerprints and outbox state | Retained for duplicate prevention and recovery; no age-based expiry |
| Outbox personal payload | Frozen event title/description/time data for interrupted delivery | DurableDelivery clears payload on confirmed/review/suppressed transitions; pending payload has no time-based expiry |
| Selected calendar provider | Title, description, start/end, timezone and app marker; name/number may be included | Remains after uninstall; calendar/provider controls cloud retention and deletion |
| Local developer evidence | Toolchain paths, hashes, synthetic fixture receipts and test output | `.local` excluded from Git; no automated expiry implemented |

The current launcher uses SliceEngine receipts, not the new DurableDelivery path.
Schema1 receipts survive migration to schema2. Clearing an outbox payload is logical
removal from that row; it is not forensic erasure of SQLite pages/backups. Hashes and
source/event identifiers remain potentially linkable metadata, not anonymized data.
Legacy preferences/code remain packaged and need a release audit even though the
old activity is unexported and boot receiver disabled. Do not enable its endpoint
"test": upstream behaviour can upload history.

Backup and device-transfer rules explicitly exclude app data; those declarations
are not a cross-device recovery guarantee. Clearing app data or uninstalling loses
receipts/namespace while calendar events can remain, creating a duplicate risk on
later import. Do not use either action as a normal retry or downgrade procedure.
Revoking access prevents future permitted operations; it does not retract events
already shared with the calendar. No automatic delete-all or remote erasure is
implemented. Manually review the exact destination/provider copies before deletion.

Before an alpha: implement and test pending-payload retention and user-visible
recovery/deletion choices in Track4; verify source namespace/reset handling; audit
merged release permissions and logging; explain calendar sharing before first write;
verify actual backup/restore behaviour. Do not publish a policy promising those
controls before their runtime evidence exists. Direct Graph/Google/CalDAV adapters
would require a fresh lifecycle and consent review in the destinations track.
