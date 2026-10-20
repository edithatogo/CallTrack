# Journal/productivity slice requirements

R18/R21–R23/R27. All features are proposed; ordering is provisional until user
priority and example workflow validation. No retention interval, billing policy,
reminder precision, cloud service or automatic sharing default has been selected.

| ID | Slice and acceptance contract | Status / gate |
|---|---|---|
| J01 | Notes/outcomes/tags have independent immutable annotation identity, version and source provenance; edit/reopen/replay/contact rename/source deletion preserves user content | First proposed slice; persistence/migration/retention acceptance required |
| J02 | Search and filter only chosen local journal fields; stable pagination, Unicode/diacritic/emoji/query escaping, empty/no-result/error and index deletion/rebuild tests | Start with measured simple query; adopt FTS/AppSearch only for demonstrated gap |
| J03 | Export selected records with explicit fields/redaction, encoding, format/schema version and provenance; cancellation/partial writes do not report success | Portable data proposal; SAF + consumer round-trip tests required |
| J04 | Restore previews additions/conflicts/skips, preserves human versions and atomic recovery, rejects malformed/oversize/unsupported input; reimport same archive does not duplicate notes | No production restore before identity and rollback tests |
| J05 | Restore never reinstalls tokens, sends messages, schedules reminders or writes calendars automatically; destination IDs require explicit rebind after device/account change | Track10 route contracts; read-only staged import before side effects |
| J06 | Callback state open/snoozed/done/cancelled is deliberate; missed call alone creates no task; notifications opt-in with redacted lockscreen content | Permission-denied/reboot/timezone/DST/delay/cancel tests; no exact timing promise |
| J07 | Rules: versioned ordered precedence, exclusions before output, explicit fallback and conflict explanation; dry-run shows every destination/redaction result | User examples and Track4/10 ownership/fan-out gates; no cross-profile bypass |
| J08 | Summaries retain actual duration separately from display/charge rounding; period/timezone/DST, zero-duration marker and overlap semantics explicit | No counting one-minute visual markers as actual call time; rounding and attribution policy remain user choices |
| J09 | Retention distinguishes source facts, annotations, delivery receipts, search index, export copies, keys and calendar events; erase/recovery scope is visible and tested | No automatic cascading erase or indefinite retention selected; Track5 lifecycle gate |
| J10 | Contact timeline preserves cached-vs-current labels and identity provenance; withheld/unknown numbers and recycled numbers never silently merge people | No new contact/SMS permission by default; no identity inference |
| J11 | Export/backup is not sync; CSV/ICS do not promise full-fidelity restore; a machine-readable archive must version notes/tags/identity/config separately | Encryption/key-recovery and partial-write/restore tests before calling it a backup |
| J12 | SMS, CRM, cloud AI, attachments and automatic billing are deferred; each needs separate supported-API, user-value, disclosure and data-lifecycle decision | Not CallTrack parity or implied by library availability |

## Proposed first slice acceptance scenarios (not executed)

Create a synthetic annotation on a source call, edit/tag it, reopen the store,
replay delivery and rename/delete the source/contact: annotation remains unchanged
and is visibly linked to its original provenance. Conflicting user edits are
reviewable, never replaced by event regeneration. Explicit annotation deletion
removes derived search entries and defines recovery/export consequences without
removing the source or remote event. If no source remains, journal ownership still
has an explicit policy rather than an accidental foreign-key cascade.

Search samples must cover quotes, SQL wildcard and FTS syntax, accents, RTL, emoji,
null/withheld identity and long text. Use parameterized queries and bounded output.
A search index can always be rebuilt from canonical annotations; privacy erasure
must include it. Measure at realistic volume before adding another storage engine.

For routing dry-run, two rules matching one synthetic call show deterministic
precedence; exclusion wins under the selected policy; no destination write occurs.
Changing a rule does not retroactively fan out old calls without explicit preview.
Keep operation IDs and receipts stable through retries and later rule edits.

For exports, distinguish exact machine data from spreadsheet display. Quoting alone
is not a spreadsheet execution policy. Text beginning with formula-control prefixes,
quotes/newlines/Unicode and huge fields need chosen consumer-specific safe handling
and exact round-trip tests; do not silently mutate canonical notes to make a CSV.
ICS preserves actual duration metadata separately from visual event length; no
attendees/alarms should be added merely by exporting a calendar snapshot.

Structured time reports, if later selected, should first evaluate maintained Word
templates and the user's preferred SharePoint structured-document route. Record
selection and calculations stay in authoritative data layers. This research creates
repository requirements, not a new report generator or tenant workflow.

## Restore trust and receipt reconciliation

Imported records are untrusted input even when syntactically valid. Validate schema,
size, provenance and record identity before a transactional staged merge. Exported
receipt IDs/hashes are historical evidence, not authority to mark remote delivery
confirmed, suppress a new source call, overwrite an event or adopt foreign content.
Keep imported delivery bindings inactive until the selected destination/account is
explicitly rebound and ownership/readback reconciles; ambiguity remains review.

Tests must include forged/conflicting receipt IDs, another installation's namespace,
reused local calendar IDs, deleted/edited remote events and a second import of the
same archive with divergent annotations. Preserve both authored versions for review
and roll back interrupted local merge without auto-replay, notifications or remote
cleanup. Authentication/encryption of an archive does not prove current remote state.
