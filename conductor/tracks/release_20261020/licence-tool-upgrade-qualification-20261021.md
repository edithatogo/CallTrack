# Track5 licence/tool-trust and schema/provider qualification

Local continuation is reviewed and qualified for the scopes below. Full M4 remains
open. Production signing, physical-device, cloud and operational acceptance remain
pending; no publication or permanent identity change occurred.

## Dependency and licence evidence

The [expanded canonical audit](canonical-build-tool-audit-20261021.json) covers239
unique resolved artifacts across runtime, buildscript, compile, unit/instrumentation
test and actual detached compiler/lint/aapt2 configurations. All binary bytes match
fresh Google Maven/Maven Central downloads. Named configurations alone missed some
compiler and lint tools; the resolution trace now includes them and excludes local
project artifacts without changing Android variant selection.

POM licence discovery follows bounded parent chains and preserves parent URLs/hashes.
Six build-only artifacts still lack effective POM declarations. Their exact published
sources and the pinned Bouncy Castle project licence supply the
[supplemental declarations](supplemental-tool-licences-20261021.json): javax.inject
Apache2, kxml source permissive terms, JDOM source terms and three Bouncy Castle
modules under its project permissive licence. These are declarations and evidence,
not an assertion that every distribution/conveyance obligation is discharged.
Executable classes called UserNotice/License are not counted as legal-notice text.
Archive expansion/depth and HTTPS origin/redirect guards fail closed.

[Third-party runtime notices](../../../THIRD_PARTY_NOTICES.md) list the63 unique
runtime dependencies, retain the Apache2 text and preserve existing GPL/upstream
attribution. Build-tool licences are separate from shipped runtime dependencies.
The inherited asset audit remains valid; public branding/artwork/translation rights
and final corresponding-source/conveyance packaging still need the production
identity/channel decision. No original CallTrack APK code or private data is added.

## Enforced dependency integrity

The [verification evidence](dependency-verification-evidence-20261021.json) records736
artifact/POM/module SHA256 entries independently compared with official repository
bytes or the completed canonical binary audit. Generated cache hashes were candidates
only. The root Gradle policy now enables both artifact and metadata verification;
no broad trusted-artifact exception or metadata bypass is used. The duplicate
prototype policy is retired. See the [procedure](../../../qualification/release/trust/README.md)
and RD04 in [decisions](decisions.md).

The selected macOS debug/instrumentation/release/JVM/lint build passes under strict
verification. Real isolated-checkout injections of an incorrect AGP JAR checksum and
a missing AGP JAR entry both fail with dependency-verification errors; the policy
is restored afterward. The Linux aapt2 JAR is separately downloaded/verified to
prepare the hosted recipe, without claiming Linux execution. Independent-host,
hosted CI and independent publisher signatures remain unobserved. The policy is a
reviewed component/version/artifact/metadata pinning equivalent for these scopes;
no universal configuration lock or vulnerability audit is claimed.

## SDK, wrapper, Gradle and JDK origins

[Toolchain origin evidence](toolchain-origin-evidence-20261021.json) records:

- The wrapper JAR matches an official Gradle bootstrap checksum shared with7.0.2;
  its binary version cannot be inferred from the old7.3.3 distribution URL. No
  wrapper upgrade is performed. The selected8.13 distribution pin matches the
  published SHA256, and every installed Gradle distribution file matches the
  checksum-verified archive.
- All11631 archived SDK payload files match the installed build-tools35.0.0,
  platform36 revision2, platform-tools37.0.1 and command-line-tools19.0. Official
  repository archive sizes/checksums and newly computed SHA256 values are retained.
  The SDK repository still publishes SHA1 archive checksums; this is HTTPS-origin
  evidence and file comparison, not an independently signed provenance claim.
- The installed JDK17.0.21 bottle matches the official Homebrew formula SHA256.
  Of359 JDK files,318 match directly,29 match after reproducing declared prefix
  relocation/ad-hoc signing in excluded copies, and12 have matching complete code/
  data payloads after constraining signature allocation metadata. Every other byte
  remains compared; tampered payloads or invalid boundaries fail the regression tests.
- `Contents/MacOS/libjli.dylib` has a real bundle resource-seal failure. Its code-page
  check and reconstructed payload match, but full resource sealing is **not**
  qualified. The receipt exposes this explicitly. Installed global tools, licences
  and OS settings are unchanged; this continuation does not repair or approve the
  bundle seal. Resolve that packaging qualification in a clean toolchain before
  claiming complete signed-tool/bundle validation.

Canonical downloads are not independent signatures or an independent machine.
External OS/Homebrew runtime libraries are outside the JDK payload manifest; do not
promote it into a whole-host supply-chain or security clearance.

## Native schema/provider upgrade

[Native evidence](schema-provider-upgrade-evidence-20261021.json) passes15 invocations
on the exact dedicated API36 recovery emulator, using the existing development key
and actual package replacement10→11 in release mode:

- Ledger schemas1–5 come from exact historical committed CREATE statements and
  migrate to6. Import schema1 migrates to2, preserving confirmed/cancelled sessions,
  aggregate progress and nullable candidate outcomes without inventing results.
- Five human-edited events retain their content; five deleted events stay deleted.
  Four ambiguous prior insertions reconcile to their existing event IDs, with no
  duplicate. Source keys, signatures, facts/ownership and the schema5 scan checkpoint
  survive. Paused pending receipts cannot replay until explicit test confirmation.
- Permission loss blocks recovery/delivery without altering pending state. After
  restoration, four explicit resumes create exactly four events; repeats create
  none. Removing the destination never selects a fallback or erases receipts.
- An injected migration failure rolls back schema DDL/version and retained payload;
  removing the trigger allows retry. A newer schema7 is rejected without deleting
  its sentinel data. Downgrade/wrong-certificate package updates are also rejected,
  with private-state readback afterward.
- The fixture app is force-stopped between persisted effects and upgrade/recovery.
  Both packages, the disposable rejection key and token-scoped provider fixtures
  are removed. Independent final native readback finds no calendars/events/call
  rows or fixture packages; guest identity, phone service and ADB UID2000 pass.

These are synthetic historical database shapes, not execution of old published
APKs or customer data. They do not migrate the original CallTrack app/package,
prove package-manager interruption mid-commit, remote calendar synchronization,
physical telephony or a new complete rendered/manual-accessibility matrix.

The first import extension used an invalid non-UUID test session identifier; the
native test rejected it and cleanup passed. It is corrected to the actual session
contract; the complete final run passes. A separate pre-install run stopped when
unrelated licence files changed: binding now covers the actual Java/assets/schema
provenance inputs, and a regression proves relevant changes still invalidate it.
All failed receipts remain private evidence, not accepted runs.

## Current artifacts and review

[Two fresh clean release builds](trusted-unsigned-evidence-20261021.json) at5578bf5
pass115 JVM tests each, zero lint errors and identical unsigned APK SHA256:
`b5ba573be2d79e0b4edd9a1c858ac578347f972ddb3e12704185543d7a47a283`.
They share this host/cache and take379.5s total. The source archive includes the
actual enforced policy and retained licences. Both APKs are verified unsigned.

The prior787ae35 APK differs only in `META-INF/version-control-info.textproto`:
the exact Git revision changes. Runtime ZIP payloads are unchanged; source metadata
is retained, never stripped to manufacture equality. The new successful debug
receipt includes the trust-policy hash, and its APK remains byte-identical
`528ca78626b1241b5825173610881540298cb493d699f1b384ef13c27e00c23a`.
The native probe predates policy promotion but uses that same debug APK and
unchanged app source; it is not relabelled as a full current source-manifest matrix.

121 host tests pass, including provenance, source-binding, archive, signature and
trust-failure guards. Additional bound checks pass on all239 retained binary
archives and all12 normalized JDK payloads. See the
[review](review-licence-tool-upgrade-20261021.md) for corrections and final checks.
The inherited115-test and full API36 UI/worker matrix remain separately scoped;
they are not counted as newly executed here.

## Remaining release gates

| Gate | Remaining evidence/decision |
|---|---|
| Production identity/signing | Permanent package/name/icon, custodian/recovery, then authorised production signing and certificate continuity; pending by user choice |
| Licence/conveyance | Inherited artwork replaced and local source/notices candidate prepared on2026-10-22; permanent identity and final signed/channel conveyance remain pending (follow-up linked below) |
| Tool/platform reproduction | Independent native Linux comparison now qualified by the2026-10-22 follow-up; hosted CI and wider platform coverage remain. Isolated JDK seal qualified; original Homebrew defect retained; no whole-host trust claim |
| Supported production upgrades | Real supported release baselines/data and permanent-certificate routes; synthetic local schema/provider acceptance is not that claim |
| Wider acceptance | Minimum/intermediate Android/OEM/manual accessibility, physical calls and real account/cloud readback where claimed |
| Operations/distribution | Seven actual observation days, reporting/channel outcomes and separately authorised publication; none claimed |

Track5 remains active. No production key, phone installation, cloud consent,
external message, upload or computer restart occurred.

## Isolated JDK follow-up — 2026-10-22

[Isolated vendor-signed JDK qualification](isolated-jdk-qualification-20261022.md)
resolves the resource-seal blocker for that explicit alternate build path. It does
not repair Homebrew or complete independent/live/production acceptance.

## Asset/source follow-up — 2026-10-22

[Current asset/source/notices qualification](assets-source-qualification-20261022.md)
replaces uncertain artwork, preserves active translations and qualifies the
5a3a3e4 unsigned/source candidate. This report remains historical for5578bf5 and
its original native evidence; no current-APK native acceptance is inferred.

## Independent-build follow-up — 2026-10-22

[Native separate-host Linux qualification](linux-reproduction-qualification-20261022.md)
qualifies9464408 and the739-entry policy. Earlier736-entry/5578bf5 evidence remains
historical. Hosted CI is prepared, not an observed provider run.
