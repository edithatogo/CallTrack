# First-release installation and upgrade baseline

Current policy updated 2026-11-24. Public release remains gated.

## Installation identity and supported starting point

The first public release starts from a fresh installation of CallTrack Community
0.1.0, application ID `io.github.edithatogo.calltrack`, version code 1, on
Android 17. Use the owner-custodied signing certificate and record the full Git
revision, both APK hashes, certificate and corresponding-source bundle hash.

Debug and synthetic upgrade fixtures use the separately installed development
package `org.calltrack.community.dev`, version code 10, name `0.1.0-dev`.

A fresh installation must have no selected calendar, activation policy, receipts
or import sessions. Call-log/phone/calendar access starts denied, automatic logging
is unconfigured, and opening the launcher must not select a destination, request
permissions automatically or create receipts, queued deliveries, scan cursors or
import sessions. Empty application databases created for maintenance and
framework-owned WorkManager storage are allowed. User action
precedes permission grants, destination choice and activation. Database existence alone is not a delivery receipt.

## Supported upgrade contract

| Starting point | First-release policy |
|---|---|
| No installation of permanent package | Supported initial installation once signed/device qualification passes |
| Earlier public version of permanent package | None exists; supported previous-release set is empty |
| Original CallTrack, AndroidCallLogSync, different application ID | No in-place upgrade or private database migration is claimed |
| Unpublished org.calltrack.community.dev builds | Test lineage only; not a supported public upgrade route |
| Same development identity/key,10→11 synthetic release-mode probe | Local compatibility evidence, not an issued previous release |
| Different signing certificate or lower version code | Reject; retain working installation and private state |
| Uninstall/reinstall, clear-data, device transfer | Not a recovery/upgrade route; private ledger loss can leave calendar events behind |

Do not relabel schema fixtures as released APKs. A user deliberately importing
history uses the existing preview/review flow and explicit destination confirmation;
this does not transfer another installation's receipt ownership or promise legacy
CallTrack export compatibility. No automatic replay of surviving calendar events.

Before the second public release, retain the first issued signed APK, public
certificate fingerprint, exact source/notices, version/schema metadata and recovery
instructions as the immutable supported predecessor. Increment versionCode; qualify
real predecessor→candidate replacement under the same application/signing identity,
including receipts/namespaces, edits/deletions, permission loss, ambiguity, paused
operations, migration rollback, newer-schema refusal and wrong-key/downgrade
rejection. Prefer a forward fix preserving receipts over uninstall or blind downgrade.
Channel/key rotation needs its own proved compatibility plan.

## Candidate qualification and retained gates

Freeze clean source before building. Two Mac unsigned builds must match; reproduce
that exact hash in the separate Linux environment and refresh source/notices.
Run the dedicated emulator probe against the development identity and test key.
Check fresh state before and after opening the launcher and prove synthetic 10→11
preservation and controlled install rejections with the historical provider and
database fixtures. If an exact unsigned development fixture is provided, check
that signing preserves every original ZIP entry. Record the signed distribution
candidate's independent fresh-install and launcher evidence.

Android 17 is the current platform qualification scope. Final signed package
identity/certificate, representative physical/OEM and
manual accessibility checks, actual account/remote calendar delivery where claimed,
seven actual observation days, security/support/source delivery and authorised
publication remain separate gates. If permanent identity or release configuration
changes, refreeze/reproduce and repeat affected installation/provider/upgrade tests;
the development certificate is not production acceptance.
