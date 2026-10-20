# Compose experiment verification

Date2026-10-21. Research and isolated prototype only. No production Compose screen
is adopted. Final decision is retain Views and defer production migration with
explicit reopening gates; M6 production delivery remains unmet.

## Baseline and failing contracts

Input e6607c0, track start8e9289a, implementation b10aa28. Policy tests first failed
compilation on missing PreviewState, then3 contracts passed after implementation.
The new sensitive-permission host contract first failed its expected early-denial
assertion before the guard was added. Documentation's good-link test exposed an
isolated fixture that omitted prototypes; its copy now excludes build/cache outputs
while including the source tree, without weakening the verifier.

## Build and local results

Standalone command:

```sh
python3 scripts/qualify_compose_prototype.py build --jdk <JDK17> --sdk .local/android-sdk
```

The source-bound receipt includes prototype sources/configuration, harness and root
wrapper hashes, successful app/test APK packaging and unchanged inputs. Three JVM
policy tests pass. Android lint has zero errors/fatal issues and7 warnings.
[Dependency report](dependency-report.json) records106 debug graph module coordinates
and primary POM declarations. It is not a full licence audit or APK contents SBOM.
The standalone project is not included in root settings; all74 previously qualified
root application input hashes remain unchanged against Kotlin pilot evidence.
The working app's36 JVM/five native tests were not rerun or relabelled as Track7
acceptance. Its already recorded results remain bounded to that prior revision.

Host suite:25 tests pass, including physical-device refusal, stale-source refusal,
unrelated AVD refusal, install-timeout cleanup/absence and forbidden permission
refusal before installation. Kotlin formatting checks pass; CI adds an isolated
build job and style scanning across prototype roots. Hosted CI has not been run.

## Native failure history and fixes

First run: four tests passed; screenshot capture failed because instrumentation
runs under the target app's UID, not the test package's private-data owner. Capture
and run-as collection now use the target package. Both packages were removed.
Second run: five tests passed, but manual image inspection found a system ANR
dialog obstructing the preview. This result is explicitly not accepted as visual
proof. Active-accessible-window assertions were added, the observed emulator
system dialog was dismissed with Wait, and the source-bound build/tests were rerun.
No ANR setting was disabled and no emulator restart or physical device was used.

## Final guarded result: blocked, not accepted

The first foreground-guard rerun failed all5 tests on the Android system window.
After verifying no development/prototype packages remained, one dedicated-guest
reboot completed and dumpsys reported no ANR since boot. A subsequent run was
blocked by a Bluetooth-keeps-stopping system dialog. Closing that observed dialog
and rerunning did not produce a qualified foreground window: all5 guarded tests
still failed on package `android`; final XML identifies “System UI isn’t responding”.
No warning/guard was suppressed. Final window
XML and retained receipts document the obstruction; both packages were removed
and their absence read back after every native attempt.

[Final evidence](evidence.json) therefore records3 JVM/25 host passes, successful
source-bound packaging/lint, and **5/5 guarded native failures**. The earlier5-test
semantics pass is rejected as visual acceptance. No unobstructed prototype or
production UI acceptance is claimed. This environmental gate must be resolved and
the unchanged strict tests rerun before any adoption. Native test failure is an
input to the documented research deferral, not converted into a passing release
or a discarded test. A green semantics assertion alone never establishes an
unobstructed user workflow.

## Acceptance limits

The native scenario uses a dedicated API36 emulator and separate package IDs,
no call/calendar/network permission, real Compose rendering/Activity recreation,
synthetic callbacks and fixture destination IDs. Native data is disposable private
UI state only. There is no calendar/provider/ledger readback, real permission flow,
call capture, account sync, supported-minimum-OS proof or performance benchmark.
Automated accessibility/large-font/RTL checks do not replace manual service use.
The source prototype's saved access flag must never become production permission
truth. Full process-death/operation reconstruction is not proven by recreation.

Prototype adoption/expiry/removal and production reopening gates are in decisions.
Research closure with justified deferral is permitted by the original spec and
workflow; it is not a claim that research/prototypes implement production screens.
