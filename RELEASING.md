# Release qualification runbook

Status: development only. Track5 has prepared this protocol; M4 has not passed.
No release candidate, production key, public repository or supported release is
established. Track4 parity is the prerequisite. Public release is outside this
track's scope; use a separately authorised execution after these gates pass.

## Candidate and source evidence

1. Complete Track4 and review its exact acceptance evidence. Record every open
   limitation; physical-call acceptance is still pending by user choice. Emulator
   evidence cannot establish OEM telephony or cloud-account behaviour.
2. Select the permanent application ID, name/icon, initial version and distribution
   route. The current `org.calltrack.community.dev`, versionCode10/0.1.0-dev is a
   disposable development identity. Do not overwrite the old CallTrack installation
   or claim its signing/branding rights. Audit inherited assets/translations.
3. Freeze a clean Git commit. Build from a fresh checkout with JDK17, Gradle8.13,
   AGP8.13.2, Kotlin/KGP2.3.21, compile/target36 and build-tools35.0.0, or explicitly qualify any
   changes. Follow CONTRIBUTING.md and preserve all test/build evidence. No private
   reference checkout or APK may be required for the root build.
4. Capture the actual releaseRuntimeClasspath graph and all artifact licences and
   notices. Audit repositories, resolved versions and plugin/tooling dependencies
   as well as direct declarations. Review dependency locks and verification
   checksums against trusted upstream provenance before enabling them. Do not
   treat auto-generated checksums as independent trust or a dependency report as
   an SBOM. Retained legacy code remains part of the shipped artifact audit.
5. Build the unsigned release variant on two clean isolated environments from
   that source. Compare artifacts; investigate nondeterminism and document tools,
   OS, dependency graph and hashes. This has not been run. If claiming F-Droid
   upstream-signature reproducibility, test its exact recipe/comparator and the
   build-tools35 signing compatibility issue identified in Track5 findings.
6. Keep source archive/commit, build scripts/configuration, toolchain manifest,
   resolved artifact checksums/licences, unsigned and signed APK hashes, application
   ID/version/SDK, signer SHA256, merged manifest, test results and review together.
   Source-bound debug receipts are useful inputs, not a production release receipt.
   Publish corresponding source and licence material with the chosen conveyance
   route when distribution is authorised; exclude credentials and private evidence.

## Signing and channels

Choose key custodian, offline recovery copy/access procedure and channel policy
before creating the first production key. Record public certificate fingerprints
in release metadata; never put private keys/passwords in Git, command arguments,
logs or issue attachments. Keep CI unsigned until key custody and protected signing
are deliberately configured. Distinguish Play upload key from app signing key.
Prefer a consistent signing identity across intended update routes; separately
signed channels require an explicit migration strategy rather than assumed updates.

Verify each signed candidate using the pinned tool:

```sh
"$ANDROID_HOME/build-tools/35.0.0/apksigner" verify --verbose --print-certs CANDIDATE.apk
```

Compare the observed certificate against the recorded intended identity and inspect
package/version with the SDK tooling. The command checks signatures, not functional
readiness. Do not publish debug-signed output. Do not rotate keys without a tested
OS/channel compatibility plan. Android's [apksigner reference](https://developer.android.com/tools/apksigner)
describes verification and signing options.

Before choosing a channel, refresh the dated policy findings. Play permission
review, F-Droid inclusion/build review and Android developer verification are
separate gates; none is established by local installation. Do not submit forms,
register identities or upload binaries merely to test a hypothesis.

## Upgrade, failure and recovery matrix

Use dedicated synthetic calendars/data and the exact previous/candidate signed
versions. Preserve the working installation and a recovery route until acceptance.

| Scenario | Required observed result |
|---|---|
| Same identity/key, increasing version | Existing selected destination, namespace and receipts survive; no reimport/duplicate |
| Schema1 and schema2 fixture upgrade | Receipts preserved; changed/missing user events never overwritten/recreated automatically |
| Pending insert at upgrade/process death | At most one event or explicit review; durable attempt evidence survives |
| Paused/cancelled operation | Candidate does not resume it without explicit user action |
| Permission revoked / destination lost | Visible recoverable state; no fallback calendar or hidden writes |
| Wrong certificate / incompatible older schema | Controlled rejection or documented forward recovery; no data wipe |
| Uninstall / reinstall / device transfer | Document namespace/ledger loss and surviving events; no claim of safe automatic restore |

Do not recommend blind APK downgrade or uninstall/reinstall as rollback. Prefer a
forward fix retaining the ledger. Any supported rollback must prove schema and
signing compatibility on a synthetic copied fixture. Portable encrypted backup and
cross-device identity migration are not implemented and cannot be used as assumed
recovery. Calendar cleanup must target known fixture events only and preserve edits.

## Support matrix and alpha observation

The declared floor is API26, not tested support. Before advertising a supported
range, qualify the minimum, an intermediate version and the current target on
emulators, then representative real devices/OEM restrictions. Record exact source,
OS/API, device category, calendar/provider category, native results, cleanup and
limits. Test LOCAL and account-backed calendars separately; verify the actual
remote account if cloud sync is claimed. Avoid publishing device serials/accounts.

After the controlled suite passes, observe seven actual days on selected authorised
real devices. This is a proposed stable-promotion gate, not time already elapsed.
Use synthetic or user-controlled calls only when authorised. Record expected/seen
call counts by direction, event count, duplicate/miss/review count, prompt/recovery
latency distribution and maximum, permissions, reboot/Doze/offline/force-stop
recovery, battery context and explicit unsampled cases. “Prompt when possible;
recover later if delayed” is the user preference, not a guaranteed deadline.

A stable label needs zero unexplained misses/duplicates, preserved edits/deletions,
verified recovery and no unresolved privacy/data-loss issue. If a failure occurs,
retain redacted evidence, fix it, rerun the affected controlled cases and start a
new observation window for the candidate. Seven days alone is not proof of quality.

## Publication handoff

Contributor instructions, issue templates, security contact, privacy policy,
supported-version/device table and changelog must accurately describe the candidate.
Configure and test a private security reporting route before inviting reports.
A maintainer must review the artifact/source/certificate and remaining limitations.
Record actual channel readback and download verification only after an authorised
publication. Keep previous artifacts/source and issued history; never replace a
published binary silently. An archived planning/research track is not release
approval or operational acceptance.
