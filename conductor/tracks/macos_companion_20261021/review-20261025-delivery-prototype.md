# Track 16 delivery prototype review — 2026-10-25

## Scope and outcome

Reviewed the new EventKit gateway, single-writer coordinator, SQLite intent
ledger, synthetic provider harness, XCTest cases, workflow change and the
delivery contract. This is a local code review of an isolated prototype. No
packaged host app, TCC prompt, selected calendar or provider write was tested.

One concrete issue was found: the initial ledger path validation used
`FileManager.attributesOfItem`, which follows a symlink and could secure the
target while SQLite later opened the link path. It was replaced with `lstat`,
`O_NOFOLLOW`, descriptor validation and `fchmod`; a regression test now verifies
that a symlink database path is rejected without changing its target.

The reviewed flow persists `write_attempted` before invoking the provider,
requires an explicit confirmation value, verifies the exact event marker and
payload after insertion, and does not reinsert an uncertain operation. Stable
foreign identity with changed payload fails closed. Reconciled event edits and
deletions remain terminal and are not silently overwritten or recreated.

## Verification

- `swift build --build-system native --package-path prototypes/macos-companion --target CallTrackCompanionUI` — passed.
- `swift build --build-system native --package-path prototypes/macos-companion --target CallTrackDelivery` — passed.
- `swift run --build-system native --package-path prototypes/macos-companion delivery-ledger-contract` — passed; synthetic recovery reports one insert after unknown outcome.
- `swift format lint --recursive --strict` on the new delivery and harness sources/tests — passed.
- `python3 scripts/verify_conductor.py` — passed (253 documents, 21 tracks, 25 synthetic scenarios).
- `actionlint .github/workflows/macos-companion.yml` — passed.
- `python3 -m json.tool conductor/tracks/macos_companion_20261021/metadata.json` and `git diff --check` — passed.
- `swift test --build-system native --package-path prototypes/macos-companion` — could not run tests because this computer has only Command Line Tools and no `XCTest` module. The hosted macOS workflow must qualify both XCTest targets before this slice can be accepted.

## Remaining gates

The AppKit preview remains disconnected from delivery. Track 16 still needs
explicit approval UI, packaged-host usage description, native permission and
selected-calendar readback/recovery, rendered accessibility and packaged
upgrade testing. Android exporter compatibility is not established. Track 14
and 15 acceptance remains a prerequisite for advancing the roadmap, and Track
17 remains locked. Keep `native_eventkit_accepted` and app packaging acceptance
false.
