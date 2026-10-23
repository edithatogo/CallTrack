# Private CallTrack successor PR series — updated 2026-10-24

The user selected [edithatogo/CallTrack](https://github.com/edithatogo/CallTrack)
as the private successor repository. Its main branch holds the original roadmap
commit. The following twenty-three draft PRs are stacked in history order; each
compares with the preceding feature branch and keeps the local reviewed commits.
Merge in order if and when each PR passes its applicable review and acceptance.

| PR | Scope |
|---|---|
| [#1](https://github.com/edithatogo/CallTrack/pull/1) | Capability mapping and build qualification |
| [#2](https://github.com/edithatogo/CallTrack/pull/2) | Manual call-to-calendar slice |
| [#3](https://github.com/edithatogo/CallTrack/pull/3) | Durable and Kotlin foundations |
| [#4](https://github.com/edithatogo/CallTrack/pull/4) | Android Compose research |
| [#5](https://github.com/edithatogo/CallTrack/pull/5) | KMP shared-core research |
| [#6](https://github.com/edithatogo/CallTrack/pull/6) | Shared UI desktop prototype |
| [#7](https://github.com/edithatogo/CallTrack/pull/7) | Calendar integration mapping |
| [#8](https://github.com/edithatogo/CallTrack/pull/8) | Journal/productivity mapping |
| [#9](https://github.com/edithatogo/CallTrack/pull/9) | Platform feasibility |
| [#10](https://github.com/edithatogo/CallTrack/pull/10) | Rust evaluation |
| [#11](https://github.com/edithatogo/CallTrack/pull/11) | Durable synchronization and recovery |
| [#12](https://github.com/edithatogo/CallTrack/pull/12) | Release groundwork and future roadmap |
| [#13](https://github.com/edithatogo/CallTrack/pull/13) | Active Kotlin migration |
| [#14](https://github.com/edithatogo/CallTrack/pull/14) | Release trust and schema/provider upgrades |
| [#15](https://github.com/edithatogo/CallTrack/pull/15) | Assets, Linux reproduction and emulator matrix |
| [#16](https://github.com/edithatogo/CallTrack/pull/16) | Final local candidate and observation preparation |
| [#17](https://github.com/edithatogo/CallTrack/pull/17) | Python3.15.1 and toolchain qualification |
| [#18](https://github.com/edithatogo/CallTrack/pull/18) | Incremental Android Compose automatic-status slice |
| [#19](https://github.com/edithatogo/CallTrack/pull/19) | KMP shared call-type selection and time-range rules |
| [#20](https://github.com/edithatogo/CallTrack/pull/20) | macOS preview-only bounded import prototype and Track 16 gates |
| [#21](https://github.com/edithatogo/CallTrack/pull/21) | Track 17 shared Compose Multiplatform presentation prototype |
| [#22](https://github.com/edithatogo/CallTrack/pull/22) | Track 18 refreshed Rust evaluation; production adoption deferred |
| [#23](https://github.com/edithatogo/CallTrack/pull/23) | Track 19 Android/macOS evidence reconciliation and conditional acceptance matrix |

GitHub readback confirmed PRs1–22 as OPEN drafts. PR20 is stacked on the Track15
`codex/track15-kmp` head; it contains only preview parsing and bounded
documentation, with native macOS/EventKit acceptance still pending. Its hosted
workflow failed before any job step or runner allocation; no hosted build result
is claimed.
PR22 is stacked on PR21's `codex/track17-shared-ui` head. Its local Rust and
Conductor checks pass. Hosted CI failed before any job step or runner allocation
because the repository account billing or spending limit blocks hosted jobs;
no hosted acceptance is claimed.
PR23 is stacked on PR22's `codex/track18-rust-evaluation` head. It records only
the bounded Track19 evidence reconciliation; cross-platform native, packaging,
upgrade/recovery and selected calendar-route qualification remain open. Its
hosted checks failed before job steps; no hosted acceptance is claimed.
PR21 is stacked on PR20's `codex/track16-macos` head. It contains the bounded
common presentation/reducer and synthetic desktop rendering tests; it does not
integrate an Android target or native macOS app. Its hosted workflow failed
before any job step or runner allocation because the repository account billing
or spending limit blocks hosted jobs; local desktop and Conductor checks passed.
PR18 is based on Track17's
`codex/python-315-toolchain` head. PR19 is stacked on PR18's
`codex/track14-compose` head and retains the reviewed Track15 slice. Its hosted
Actions attempt failed before any job step or runner started because the account
billing/spending limit remains unresolved; no hosted CI pass is claimed.
The separately portable Gradle checksum change is open against
the original upstream as [MarkoBL/AndroidCallLogSync#3](https://github.com/MarkoBL/AndroidCallLogSync/pull/3).

Hosted GitHub Actions did not execute: checks failed before runner allocation
with an account billing/spending-limit annotation. No hosted acceptance is
claimed. Physical calls, selected remote calendar routes, production signing
and seven-day operational observation retain their independent gates. Private
APK, device, account and observation data remains outside the Git repository.
