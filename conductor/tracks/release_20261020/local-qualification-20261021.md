# Track5 earlier local release checkpoint — 2026-10-21

Superseded for current trust/schema/provider scope by the
[latest qualification](licence-tool-upgrade-qualification-20261021.md); retained as history.

Full M4 is incomplete. The selected development identity and unsigned production
configuration remain unchanged. Local preparation is not production approval.

## Qualified artifacts and source

The two retained clean builds at787ae35 still produce unsigned APK SHA256
`117909cbb95f70faac43c905ad6957daf746fa7713027786cb794b3f3b8945c4`.
Each passed115 JVM tests and lint with zero errors/69 warnings/two hints. Those
builds are reused because app/build inputs did not change during this continuation;
they were not rerun or relabelled as newer source builds. Both exact APKs now have
fresh SDK apksigner missing-signature readback. Signed output and unrelated tool
errors are rejected by the strengthened qualification runner.

A corresponding-source Git archive of that exact candidate contains546 entries,
including LICENSE, wrapper, configuration and source; excluded local evidence/keys
are absent. Its hash and signature readbacks are in
[artifact checks](local-artifact-checks-20261021.json). The archive is retained in
`.local/release-evidence/kotlin-candidate-787ae35-source.tar`; it is not uploaded.
No claim of complete distribution-source/toolchain sufficiency follows from this
structural archive check.

## Runtime dependencies and assets

The [fresh repository audit](canonical-runtime-audit-20261021.json) compares all63
unique runtime artifacts represented by68 resolved rows (five duplicate rows) to
Google Maven/Maven Central HTTPS downloads: all bytes match. Three renamed AARs
use published Gradle module metadata binding filename, URL and SHA256. All63 POMs
were downloaded.62 directly declare Apache2 licences; listenablefuture1.0 has no
POM licence declaration, but its exact published sources JAR contains an Apache2
header, separately hashed in artifact checks. Six embedded licence entries are
hashed, including nested classes.jar contents.

This closes the repository-byte comparison and declaration-discovery work, not
redistribution/legal clearance. No dependency lock or verification metadata is
silently enabled. Build plugins, their transitives, wrapper/SDK/tool distribution
trust, required notices and conveyance obligations still require the full D01/D02
audit. Repository agreement is not an independent signature or a vulnerability
scan; no complete SBOM or security clearance is claimed.

The [asset audit](assets-audit-20261021.json) hashes23 main resource/artwork files
against audited upstreamaaa56443:18 unchanged, two modified strings files and
three app-owned XML resources. Retained icons, colours/themes and translation
provenance are explicit. Unchanged upstream bytes do not establish separate
artwork/translation rights or authority to reuse the name/icon publicly. Permanent
branding remains a user decision before production release.

## Development upgrade scope

The probe builds exact committed app source with a recorded, excluded Gradle DSL
overlay: versionCode10→11, release mode, existing debug certificate. It does not
change the committed version/application ID or introduce production signing.
Synthetic private state covers paused activation, filters, namespace and
READY/IN_FLIGHT/REVIEW/CONFIRMED schema6 receipts, signatures, nullable facts,
retained payloads and event IDs. It force-stops the fixture app before upgrade.
The [final native evidence](development-upgrade-evidence-20261021.json) passes:
four instrumentation invocations, successful10→11 replacement and two expected
install rejections. State readback passes after upgrade and after each rejection.
Both packages and the disposable key are removed. Independent final readback finds
no installed fixture packages, calendars, events or call rows; the dedicated guest
remains healthy and nonroot (ADB UID2000). Downgrade and wrong-key failures must return their exact Android
rejection reason, followed by private-state readback; unrelated failures cannot pass.
A one-day disposable test-only certificate is used only for wrong-key rejection and
removed automatically. Both owned development packages must be absent before
installation and removed with readback afterward. No provider permissions/writes.

This does not establish real old-schema production upgrades, edited/deleted
provider events across upgrade, replay/recovery execution after upgrade, process
kill during package-manager installation, uninstall/restore, independent-host
reproduction or physical/cloud/operational acceptance. Existing115 JVM and API36
native recovery results remain separate, source-matched evidence, not those new
release-upgrade scenarios.

## Reviewed verification

`python3 -m unittest discover -s tests -v`:105 passed. The earlier transient
full-suite check ran while newly linked handoff/review documents were being written
and failed two document-copy/link tests; after those documents existed, the full
suite passed without weakening verification. Conductor verification passes152
documents,20 tracks and25 synthetic scenarios; private sources were not rechecked.
`git diff --check` passes. No app inputs changed; the retained115 JVM/two-clean-build
and selected89-success-plus-one-expected-termination native matrix are explicitly
reused rather than counted as new tests.

## Remaining sequence

| Gate | Next evidence | Disposition |
|---|---|---|
| D01/D02 trust and conveyance | Audit build/tool transitives and required notices; resolve inherited artwork/translation rights or replace with qualified assets; qualify locks/verification against reviewed provenance | Open; runtime-byte and source-header work above is complete |
| D02/D05 reproduction | Independent environment/contributor run and hosted workflow, preserving exact inputs, tools and artifacts | Pending; local same-host builds are not this evidence |
| D03 production continuity | Permanent package/name/icon, key custodian/recovery and channel choice, then production signing/verification | Pending by user choice; no production key created |
| D07 supported upgrade/recovery | Real supported historical schemas, human-edited/deleted provider events, interrupted writes and recovery across upgrade; then production-certificate continuity | Private-state development upgrade is bounded evidence only; full gate open |
| D04/D06 supported platforms | Minimum/intermediate Android qualification, physical/OEM calls and manual accessibility; account-backed calendars and remote readback where claimed | Selected API36 acceptance retained; additional scope pending |
| D08/D09 operation/channel | Seven actual selected-device observation days, private reporting route, explicit publication/channel execution | Pending; zero observation days or publication claimed |

Track5 stays active. Do not archive on a successful local build, byte comparison or
private-state probe. No phone install, cloud consent, external message, upload or
computer restart is part of this continuation.
