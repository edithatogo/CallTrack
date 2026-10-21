# Emulator recovery and remaining acceptance — 2026-10-21

## Diagnosis and repair

The failed runs remain historical failures. Boot completion was observed with no
phone service, provider queries timed out and fixture processes reported startup
ANRs. An isolated network-simulator experiment also failed; its host crash report
shows QEMU's hang detector firing while network-channel setup waited. It is not an
accepted launch recipe or evidence of an app-code defect.

Host diagnosis found severe resource pressure: about55.8GiB of swap and internal
free space falling below1GiB. There were many duplicate idle MCP/REPL/desktop helper
processes parented by this chat's CLI. Scoped retirement preserved the newest exact
command instance, excluded main GUI/browser applications and checked parent/command
identity before signals. An orphaned simulator from an already-stopped owned AVD
also needed termination; its disappearance was verified. This was manual repair,
not a recurring process-killing job or a repository-wide cleanup policy. No computer
restart, browser closure, user-data deletion or physical-phone testing was used.

Resource headroom recovered to over29GiB of internal free space. A fresh dedicated
AVD was created on PortableSSD using the existing API36 Google APIs ARM64 image and
Pixel7 profile. The original AVD/data home remains intact; it is not claimed repaired.
The fresh guest passed repeated boot/phone/calendar readiness checks. Multiple
variables changed, so these outcomes do not isolate a single root cause or claim a
permanent fix to the desktop's helper-process lifecycle.

## Harness fix

Commit826ccaf adds a shared, read-only preflight after exact emulator/AVD validation:
boot completion **and** the phone service must be present before lifecycle,
telephony or rendered-UI fixtures can be installed. Three regressions reproduce
boot=1/phone-missing: they fail on the old harness and pass with the fix. All92 host
tests pass. No app source, APK, provider-write logic, time budget or native assertion
changed. This gate detects the observed broken startup; it is not proof that every
Android service is healthy.

## Reproduction

Qualified recovery home: `.local/clean-emulator-avds`, pointing to the excluded
PortableSSD cache. SDK: `.local/android-sdk`; emulator37.2.12. Existing licence
acceptance is retained. Never reuse recorded process IDs as cleanup instructions.
Before stopping anything, identify current ownership, preserve needed diagnostics,
and verify provider/fixture cleanup. Existing packages must remain protected by the
harness guards; a failed cleanup is not permission for blind uninstallation.

```sh
env ANDROID_AVD_HOME="$PWD/.local/clean-emulator-avds" \
  ANDROID_HOME="$PWD/.local/android-sdk" \
  .local/android-sdk/emulator/emulator \
  -avd calltrack-parity-api36-recovery-20261021 -port 5582 \
  -no-window -no-audio -no-boot-anim -no-snapshot -gpu swiftshader
```

Use this only when the selected port/AVD is not running. This launch preserves data;
it does not wipe the original or new guest. Inspect host headroom, duplicate helper
ownership and stale emulator/network processes before repeating a failed launch.
Do not close unrelated applications or disable global services to conceal a failure.
The [Android network-simulator documentation](https://developer.android.com/studio/run/emulator-networking-advanced)
provides flag context; the nondefault instance experiment was rejected here.

Run the tracked lifecycle, telephony and UI harnesses with `--serial emulator-5582`,
matching the successful root build receipt/APKs. The current local matrix driver
records exact receipt reuse; earlier accepted suites are reusable only while all
app inputs and APK hashes match. Keep one native harness active at a time. Preserve
failure diagnostics before cleanup changes the condition. For startup ANRs, collect
referenced traces before package removal if access permits; a crash summary alone
does not establish an app-code cause.

## Acceptance

The remaining four suites pass. The [current evidence](active-migration-evidence.json)
and [handoff](active-migration-handoff.md) record raw receipt hashes, exact reuse and
final native cleanup/readiness proof. The consolidated matrix passes89 successful
invocations plus one expected termination;92 host tests pass. Full M4/M5, production
signing, physical/cloud/OEM/manual accessibility and operational observation retain
their separate gates.
