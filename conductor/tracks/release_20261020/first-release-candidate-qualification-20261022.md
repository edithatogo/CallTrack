# First-release baseline and final local candidate qualification

Date:2026-10-22. Track5/M4 remains active. This closes the authorised local
baseline/candidate work; it does not approve production signing or distribution.

## Installation and upgrade decision

The [baseline](first-release-baseline.md) selects fresh installation for the first
public release. No issued predecessor exists: the supported previous-public-release
set is empty. Original CallTrack, AndroidCallLogSync, different package/certificate
and unpublished development builds have no claimed in-place public upgrade route.
Do not uninstall a working installation to bypass certificate or migration failures.

Keep org.calltrack.community.dev,10/0.1.0-dev for local qualification. Permanent
application identity, first public version/channel and production key custody/
recovery remain unselected. This development certificate is not a production key.
Before a second public release, retain the first issued signed APK, certificate,
source/notices and schemas as its immutable, actually tested predecessor.

First-install procedure for the eventual qualified signed package: verify the
chosen channel's recorded artifact/certificate; install through the supported
Android installer; open and observe denied/unconfigured state; grant access through
explicit app actions; choose a writable destination and deliberately enable the
wanted logging policy. Check its visible status. An unsigned APK is not an
installable public release. Changes to identity, signing or release configuration
require affected qualification again before that procedure is advertised.

## Frozen candidate

| Item | Exact binding |
|---|---|
| Source | 15998ba887e76ea223a6a8ba3430dd640330ed99 |
| Unsigned release APK SHA256 | 89a548489035a6df8c464cc621e49dfbb0c9d6d16619bbe54f27f78e612d5f86 |
| Source/notices archive SHA256 | 5c73dd887d29f0542721e11fe1caaa0b2cb45b68014653322bd6b5aa564dddd6 |
| Application and schemas | org.calltrack.community.dev;10/0.1.0-dev; ledger6/import2 |
| SDK | min26/target36; build tools35.0.0 |
| Toolchain | Isolated qualified Temurin17.0.20.1+1 on Mac; digest-pinned Temurin17 Linux image; AGP8.13.2/Gradle8.13/Kotlin2.3.21 |
| Verification policy |739 enforced entries; SHA256 7f381e1b92bcd31f060bd3c4b378067b2f7eafc1d073aff3f4b532428d602329 |

Two clean Mac checkouts match byte-for-byte. A new container on the separate
native x86-64 Linux host, with no mounts, ports or privilege and fresh SDK/Gradle
home, produces the same APK. Each release build passes115 JVM tests and has zero
lint Error/Fatal issues. Linux's copied APK and exported test/lint reports were
read back independently. All144 host tests pass on Mac and Linux. Hosted CI has
not run; the prepared workflow can be supplied this revision/hash after repository
upload/execution is authorised.

The source bundle contains658 members, including508 tracked source files and63
published dependency source/POM coordinates. Archive readback, repeatability and
APK notice correspondence pass. Public source delivery and full upstream dependency
rebuilding remain unqualified. Historical source-only rebuild5a3a3e4 retains its
original scope; it is not relabelled as a new source-only archive rebuild.

## Native candidate acceptance

The probe signs a copy of this exact unsigned APK with the existing development
key, verifies the certificate and every original ZIP entry, and installs that
release payload as version10. The increasing-version11 overlay is a separately
hashed future-compatibility fixture. It is not the frozen unsigned candidate.

| Emulator API | Exact-payload fresh/upgrade invocations | Rendered fresh/reopen invocations | Expected install rejections | PNG/tree pairs |
|---|---:|---:|---:|---:|
|26|17|3|2|1|
|33|17|3|2|1|
|36|17|3|2|1|
|Total|51|9|6|3|

Fresh state has no selected destination, activation policy, receipts, outbox,
scan cursors or import sessions; call/phone/calendar access stays denied. Actual
launcher/window readback shows the app, without a permission dialog, and the
manual logging action disabled. Reopening preserves this state. Empty databases
created by retention maintenance are allowed and their delivery tables are checked
empty. Native provider/schema tests preserve human edits/deletions, ambiguous
attempts and paused work through10→11, including schema1–5→6/import1→2, rollback/
retry, unsupported-newer-schema refusal, permission loss and explicit exactly-once
resume. Wrong-key and downgrade rejection preserve private state.

All token-scoped LOCAL fixtures and both packages are removed with readback;
disposable wrong-key fixtures are gone. Both newly launched API26/API33 guests
are stopped, with transport and QEMU absence checked. Shell UID remains2000;
no ADB root was used in these candidate tests. Original API36 guests/server remain.

The earlier229-invocation development matrix and134 screenshot/tree pairs remain
separately bound to debug APK983892c6 and their original test variants. All current
production/build inputs match that matrix, and the current debug APK is unchanged.
This is not a new full229-case run against the unsigned release. See the
[machine evidence](first-release-candidate-evidence-20261022.json) for artifact,
certificate, receipt and private evidence hashes.

## Review, probes and recovery

[Review and fixes](review-first-release-candidate-20261022.md) cover cleanup retention,
the corrected empty-state contract and rendered-capture ordering. Draft29c6211 and
a8c7f9f artifacts are retained without promotion. The first API26 test wrongly
required no ledger file after launch; the existing app legitimately creates an
empty maintenance ledger. Its failed probe is retained;15998ba checks actual rows.
The first rendered probe captured the launcher after instrumentation teardown;
capture now precedes teardown and all three final runs pass. Neither failure
required a production app change.

API33 had a System UI boot ANR before installation. Closing that system dialog's
app restarted System UI; normal launcher, phone service, boot flag and absent app
were read back before qualification. No computer or guest reboot was needed.
A draft Linux run's streaming client stayed open after its successful build;
independent receipt/artifact readback retained its outcome, then the owned stopped
container/client were retired. The final run returned exit0 as well as passing
independent readback. Only task-owned containers were stopped; existing services
were preserved.

## Remaining gates

- Permanent identity/name and production key custodian, recovery/access and channel continuity.
- Production-signed artifact/source/certificate binding and affected native installation tests.
- Representative physical/OEM/ABI and manual speech/contrast/accessibility acceptance; real calls.
- Actual account-backed remote calendar outcomes where claimed; LOCAL evidence is separate.
- Seven actual observation days: zero are claimed here.
- Repository/hosted CI, validated support/security route, source conveyance and authorised channel publication.

No issued predecessor is required before the first release because none exists;
actual first→second public release migration becomes mandatory before the second.
This does not waive preserving-schema or rejected-install tests, which pass locally.
No production key, public repository, phone installation, cloud consent or
publication was created. Track5 is not complete or archived.
