# Track 16 scoped prerequisite deferral — 2026-10-24

## Reason and evidence

The user explicitly directed Track 16 implementation on 2026-10-24 while Track
14 remains in progress and Track 15 remains in progress. Track 14 has only a
reviewed settings/status slice; broader screen migration and acceptance remain.
Track 15 has only bounded JVM/Android call selection and time-range rules; its
macOS Kotlin/Native acceptance remains pending. The host has Swift 6.4 and the
macOS 27 Command Line Tools SDK, but no full Xcode installation. The public
EventKit API surface compiles against a macOS 14 deployment target. No selected
calendar, native permission/readback, package build, UI, or upgrade acceptance
is evidenced.

## Permitted scope

Proceed with requirements/dependency refresh, a bounded Foundation-only
synthetic interchange parser prototype, local contract tests, and documentation
of native acceptance. This does not authorize real calendar access or writes,
does not establish that Android exports this format, and does not complete or
defer Tracks 14 or 15. Do not start Track 17 or claim Track 16 acceptance.

## Consequences and reconsideration

The prototype is not a supported import workflow. Track 16 remains in progress;
native file selection, EventKit permissions/destination selection/write/readback,
durable intent and recovery, UI/accessibility, packaged upgrade, and selected
calendar qualification remain open. Reconsider this deferral after Track 14 and
15 acceptance (including Kotlin/Native fixtures), and when full Xcode and an
explicitly selected synthetic test calendar are available. The sequence verifier
keeps Track 16 unresolved for downstream stages even while this bounded work is
active.

## Evidence and scope refresh — 2026-10-25

Track 15's expanded common selection fixture now passes on JVM/Android locally
and on hosted macOS ARM64 Kotlin/Native (3 tests, zero skips/failures/errors);
PR #79 merged as `e6070cc`. Track 15 remains in progress because its broader
acceptance is sequenced behind Track 14.

Track 14 PR #78 remains open. Hosted API 33 and API 36 UI jobs pass; API 26
fails at import confirmation with an unexpected PermissionController window
whose displayed text was classified by the test helper as `contacts`. The app
manifest requests no Contacts permission. That text is a UI observation, not
proof that `READ_CONTACTS` was requested; Android 8.0 assigns `READ_CALL_LOG`
to the Phone permission group. The local API 36 emulator is offline, so no
local rerun is claimed. See [PR #78](https://github.com/edithatogo/CallTrack/pull/78)
and the [AOSP Android 8.0 manifest](https://android.googlesource.com/platform/frameworks/base/%2B/android-8.0.0_r1/core/res/AndroidManifest.xml).

Under the current user instruction to continue locally actionable work, a
bounded AppKit file-selection and aggregate-preview prototype was added. It
uses the existing preview parser, limits reads to 5 MiB, shows no call times,
phone numbers, or row IDs, and keeps calendar delivery disabled. The SwiftUI
path was not adopted because the installed Command Line Tools lack the SwiftUI
macro plugin; the AppKit target type-checks locally instead. Full Xcode,
rendered accessibility/lifecycle/package acceptance, EventKit authorization,
an explicitly selected synthetic calendar, durable write/recovery design, and
Track 14 completion remain prerequisites for broader implementation. No
EventKit permission was requested and no calendar was read or written.
