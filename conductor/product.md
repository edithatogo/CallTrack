# Product definition

Build a community-maintainable FOSS successor to CallTrack, initially evaluating AndroidCallLogSync as the fork base. Restore dependable automatic call-to-calendar logging on Android, preserve user edits and history, and support calendars independently of provider branding. Kotlin migration is the intended evolution; shared logic/UI, iOS and Rust are research-led options.

The user authorised Conductor incorporation and track creation on 2026-10-20. This authorises the planning structure, not every proposed future feature or dependency. The initial research is input to later discovery, not a final specification. Each track owns mapping, research, requirement refinement and implementation decisions when activated.

Success: call events persist accurately without duplicates, recover from interruptions, and remain understandable and controllable by the user. Community build and contribution instructions are reproducible. Local tests, device persistence, provider synchronisation and operational acceptance are separate evidence.

See [requirements](requirements.md), [roadmap](roadmap.md) and [tracks](tracks.md). Audio recording, surveillance and universal third-party VoIP capture are outside initial scope. Local build and bounded zero-row qualification were initial stages. Current emulator acceptance is recorded below; physical-call, remote calendar and public distribution acceptance remain unqualified.

The [completed manual slice](archive/calendar_slice_20261020/index.md) now provides a development app with explicit calendar choice and a latest-call action. Its native acceptance used a synthetic source, actual LOCAL calendar storage and durable SQLite receipts. This is a foundation for parity, not an automatic logging or public release claim.

The [parity implementation](tracks/parity_20261020/index.md) now passes the selected
API36 emulator requirements P01–P10, including automatic enable/pause/filtering,
durable recovery, historical import and explicit recovery/retention UI. Its
[handoff](tracks/parity_20261020/handoff.md) preserves physical/cloud/release and
wider-platform gates; this is development acceptance, not production availability.
