# Verification and handoff

## Scope

Mapping/documentation of the supplied APK and two pinned FOSS source trees. No successor application code changed and no app tests/build were executed. This does not prove Android 17 compatibility, remote calendar sync or deployment acceptance.

## Checks

- Androguard4.1.4 decoded manifest/preferences/resources and app-class instructions; critical scan bound, rounding divisor and duration arithmetic checked against DEX instructions. The automatic counter starts1 and checks <5 after increment: four examined rows. The double bit pattern decodes to1000.0.
- Source checkouts read at pinned revisions and clean status; dependency declarations, permissions, configuration, acquisition, scheduler, HTTP response and export paths inspected.
- `python3 scripts/verify_conductor.py --private-sources` validates document links, prerequisites, metadata/fixture structure, requirement references, private APK digest and all recorded source digests. See final check record below.
- `git diff --check` checks whitespace. Application behaviour tests are not applicable to a documentary mapping deliverable. The 25 fixtures are future acceptance scenarios, not executed app tests.

## Downstream handoff

Build track: prove modern build/install/permissions, inspect resolved dependencies and test fork viability before choosing it. It must not activate ACS endpoint testing with personal history as a synthetic probe.

Calendar slice: writable destination contract, exact event format, safe unknown-number handling, local/native event readback before cloud claims.

Parity: use F01–F25 and U03–U08/U12 to resolve formatting, pause/import, nonstandard call types, dedup, legacy collisions and human-edit/deletion policies. Do not retain the four-row cap, int overflow or title-only identity.

Destinations: synthetic test payload, TLS/auth/token storage and per-provider retry semantics. Platform/Kotlin/Rust/UI tracks retain research ownership; no dependency adoption was preselected.

## Evidence limits

The completed deliverable is a source-grounded static map and assigned uncertainty register. Original binary authenticity, original UI on a running device, provider quirks, current release freshness, platform performance and native/cloud execution remain open under named later tracks. No actual phone logs, credentials or raw extracted APK implementation have been committed.

## Implementation check record

`python3 scripts/verify_conductor.py --private-sources`: PASS, 56 Markdown documents, 13 tracks, 25 scenarios; private APK and source digests verified. `git diff --check`: PASS. No application tests applicable.
