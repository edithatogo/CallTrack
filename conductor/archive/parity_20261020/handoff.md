# Track 4 handoff — accepted development scope

## Delivered

Automatic enable/pause with independent incoming/outgoing/missed filters and an explicit future activation boundary; bounded source paging/reset refusal; durable pre-effect intents and owner/generation isolation; exact time/raw presentation/source facts; preserved edits/deletion; historical preview/confirm/cancel/progress/resume; duplicate-safe repeats; visible counts/status and explicit read-only recovery, never-attempted reapproval, suppression and seven-day payload expiry.

SQLite ledger schema6 and import schema2 preserve earlier data. WorkManager2.12.0 is adopted for development scheduling; app-owned persistence remains SQLiteOpenHelper. The three-component Kotlin2.3.21 pilot remains in place. No production Compose/KMP/Rust, Room/DataStore API, direct cloud connector or endpoint is added.

## Evidence and timing

[Final acceptance](final-acceptance-evidence.json) binds the source/build/APKs and10 suites: store39, source identity3, wake budget1, provider8, worker5, lifecycle11 plus expected process kill, telephony5, settings5, imports5, recovery7. Total89 successful native invocations plus1 intentional termination;110 JVM and73 host tests pass;40 screenshot/tree pairs are retained locally by hash. Guest remains responsive; development packages and call/calendar fixtures are absent; original power/network state is verified.

Observed worker single-call source→verified3,542ms and request→verified3,036ms;63-event batch source→verified58,640ms; actual emulator missed-call source→verified14,662ms. Lifecycle observations range up to220,378ms including reboot/preparation/verification. These are observations, not execution deadlines or physical/OEM guarantees. The last worker report can be a later zero-delivery recheck; global receipt counts are separate and authoritative for stored state.

Earlier failed/uncertain runs and scoped recovery remain in the evidence history. Unattributed process exits/ANR did not recur in the final fresh matrix; monitor under later qualification rather than claiming a proven cause or universal absence.

## Next owners and outstanding gates

- Track5 resumes its D01–D09 candidate/upgrade/privacy/signing/distribution and operational gates using this M3 evidence. No release is signed or published. Qualify physical calls and a supported Android/OEM matrix, manual accessibility/locale/night/older-API behavior and longer observation before a stable claim.
- Physical incoming/outgoing/answered/missed-call acceptance remains pending at the user's choice. Actual telephony here is one emulator incoming missed call; other source types have synthetic native/JVM coverage.
- Native LOCAL calendar writes do not prove Google/Outlook/CalDAV cloud synchronization, account exposure, work-profile access, direct Graph/OAuth or store eligibility. R19 remains provider-exposure qualification; R23 export/restore/full legacy adoption remains later work beyond the read-only matcher.
- Track6 continues its remaining Java/adapter/store/worker/Activity conversions and justified legacy removal after M3/M4 gates. Re-inventory current sources: Track4 enlarged the Java set beyond the original pilot. Preserve signatures/fingerprints, schema migrations, source facts, operation ownership and this same acceptance suite; do not combine a UI rewrite with data migration.
- Tracks7–13 research dispositions remain unchanged. Reopen their adoption gates with a concrete need and refreshed compatibility; no automatic technology adoption follows this archive.

## Reproduction and recovery

Use the qualified JDK17/SDK36/build-tools35.0.0 via scripts/run_slice_build.py, host unittest discovery and verify_conductor.py. Run parity harnesses only against the explicitly qualified dedicated emulator with absent development packages and matching build/APK hashes. UI modes are settings/imports/recovery; store modes include source-identity/wake-budget. The retained local final-matrix driver/receipts identify exact commands and hashes; they are test artifacts, not a new production runtime.

On failed cleanup retain the installed instrumentation and original fixture token, inspect live state, then perform scoped cleanup and verify readback before uninstalling. Reboot uses the environment mutation owner so an uncertain outcome triggers restoration. Never restart a guest solely because an observation expired. Computer restart is not an unblock option. The recovery AVD/ports are independent of the old stuck guest; no old writable files were reused or removed.

Pending personal payload expiry is best effort on access/maintenance, not forensic erasure; IDs/hashes/source facts/tombstones and calendar events remain. In-place production downgrade/restore is not qualified by disposable schema fixtures. Keep signing and user data recovery gates with Track5.
