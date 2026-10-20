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
