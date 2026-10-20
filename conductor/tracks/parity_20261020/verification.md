# Track 4 verification — in progress

## Foundation checkpoint

Core implementation checkpoint 9fc2466 plus subsequent raw-type/filter changes. Source-bound build passed with 31 JVM tests (16 Track 3 regressions, 12 durable delivery contracts, 3 filter/range contracts), zero lint errors and 30 warnings. Host safety/documentation suite: 19 tests passed. [Foundation receipt](foundation-evidence.json) binds exact inputs and app/test APK hashes to four passed API36 emulator instrumentation invocations.

Native checks: synthetic cached-presentation cursor, preserving v1→v2 migration, committed in-flight state and confirmed payload clearing across database reopen, and rejection of outbox writes inside an uncommitted caller transaction. App/test packages were removed and absence verified. This phase used private fixture databases only: no calendar rows or call-log rows were written, no physical calls were made and no physical phone was used.

Dedicated AVD: calltrack-parity-api36-20261020, emulator-5580, API36 Google APIs ARM64 revision7 with Emulator37.2.12 and HVF/software graphics. Its first boot took several minutes with observable guest I/O and execution; a live process was retained until boot completed. First-boot timing is not an app delivery-latency result.

## Not yet accepted

- Native fault injection/process-death recovery through actual calendar/provider/outbox transactions; current durable crash tests are JVM fixtures.
- Durable retained source facts and operation ownership/generation; raw presentation distinctions and subsequent migration.
- Source paging/reset guards, automatic pause/type/settings behaviour, historical preview/import/cancel/resume and legacy matching.
- Prompt/periodic scheduling, process/reboot/Doze/offline/permission recovery matrix and measured event latency.
- Full user UI/status and review/recovery actions, final Conductor review/fixes/archive.
- Physical-call acceptance remains pending by user direction. Remote provider delivery and production release remain separate scopes.

The track stays in progress. Foundation success does not qualify automatic CallTrack parity or complete the original objective.

## Schema3 operation-ownership checkpoint

New JVM tests first fail to compile on missing Operation/scoped overloads. The
implementation then passes39 JVM tests (15 durable delivery), including pending
owner/generation rejection, legacy caller rejection, same-owner recovery, cross-owner
confirmed receipt verification, invalid operation identity and cancellation at the
committed boundary. Source-bound build/debug APK/test APK/lint pass with unchanged
inputs; lint0 errors/30 warnings. Java formatting and25 host safety tests pass.

Dedicated emulator-5580/API36 harness passes7 native checks including schema1→3,
schema2→3 pending preservation and operation identity across reopen/payload purge.
The harness grants no call/calendar permissions and touches only synthetic own-app
fixture databases. Both app/test packages removed with readback. Source/artifact
hashes and exact native methods are in ownership-evidence.json; full local logs
remain under .local/parity-owner-* and .local/parity-evidence.

No source-fact/raw-presentation retention, automatic/import/scheduler/controller
integration or full P01–P10 acceptance follows. Track4 remains active and unarchived.

## Schema4 source facts and presentation checkpoint

Tests first fail on missing raw-presentation constructor/fields and SourceFacts.
Green source-bound build passes42 JVM tests, app/test APK packaging and lint
(0 errors/30 warnings), with unchanged inputs. Legacy Java source/event goldens
remain intact. Tests distinguish seven presentation inputs, retain zero actual
duration independently from the one-minute marker, and review a presentation
change without changing the legacy signature or inserting again.

Dedicated API36 emulator harness passes10 native invocations, including synthetic
Cursor null/extended presentation and duration beyond32-bit range, schema1/2/3→4
preservation, source-fact retention after reopen/payload purge, and prior owner/
receipt contracts. No call/calendar permission granted or personal provider query.
Both test packages removed and read back absent. Exact hashes/methods are in
source-facts-evidence.json; detailed snapshots remain excluded in .local/parity-evidence.
25 host safety tests and Java/Kotlin formatting checks pass; privacy map updated.
This does not qualify paging/import/scheduling/UI or complete P01–P10.
