# Shared UI requirements

R17/R24 refinement. Trial acceptance differs from production promotion.

| ID | Required behavior | Trial / later acceptance |
|---|---|---|
| U01 | Explicit destination choice before preview/action | Rendered desktop test: disabled initially, enabled after deliberate sample selection |
| U02 | Changed destination invalidates stale preview | Rendered test: clearing destination removes preview and disables action; real refresh/removal/race cases later |
| U03 | Honest source and capability display | Rendered test: sample-only boundary, unavailable capture, no save-to-calendar action |
| U04 | Native permission/provider ownership | Future Android/iOS denial, revocation, calendar disappearance and callback lifecycle tests; no real permission/provider is implemented here |
| U05 | Durable state and edit preservation | Future state adapter must reuse Track4/8 contracts, survive process restart/cancellation and protect human edits; ephemeral sample preview is not persistence |
| U06 | Accessible interactions | Future manual keyboard/tab focus, screen reader, large text, contrast, RTL/localization and narrow/wide window tests per target; semantics assertions alone insufficient |
| U07 | Platform packaging/support | Future Apple arm64 device/simulator, Swift consumer and lifecycle; desktop installer/launch/upgrade/remove and native notices; no shipping artifact now |
| U08 | Controlled dependency adoption | Pin/resolve graph; source-bound results; review expiry at Track12/8 reopening; retire prototype or replace duplicate responsibilities after parity |

Before production, test double-click/busy states, failure/retry/review messages,
back/close during pending work, timezone/import validation, stale callbacks,
recreation and source provenance using the actual controller. Test supported
capabilities independently; unavailable cellular history is never synthesized.
A successful desktop screen does not fulfill the Android+iOS shared-UI gate.

The experiment uses constant synthetic entry counts and a sample destination.
It performs no actual import, validation, deduplication, provider query or write.
No call identities, contact names, calendar tokens or personal exports are inputs.
