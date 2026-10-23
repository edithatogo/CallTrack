# Track 15 refined requirements — 2026-10-23

| ID | Requirement | Acceptance evidence |
|---|---|---|
| K15-1 | Common code uses platform-neutral types and preserves raw CallLog numeric semantics. | Shared code has no Android/JVM framework imports; 1/2/3 map independently and unknown values remain excluded. |
| K15-2 | Call time filtering remains half-open and rejects invalid ranges. | Common tests cover inclusive start, exclusive end, invalid/negative ranges and `Long` boundaries without overflow. |
| K15-3 | Existing Java/Kotlin callers and persisted behavior remain compatible. | Existing `CallSelection` Java-visible constructor, fields and methods remain; established identity/rendering fixtures and JVM suite pass. |
| K15-4 | Native adapters remain authoritative for acquisition, persistence, scheduling, permissions, secrets and destination writes. | Dependency inspection confirms the shared module has no platform services and no second ledger/writer. |
| K15-5 | The same common rules are validated on Android/JVM and macOS Kotlin/Native before KMP completion. | JVM/Android test evidence plus macOS arm64 Kotlin/Native test and artifact evidence; Native remains pending without Xcode. |
| K15-6 | No production adoption is claimed until parity, preserving upgrade/recovery and target acceptance pass. | Track remains in progress with `production_adopted=false` until all required evidence is recorded. |

Unsupported in this slice: shared hashing/event rendering, timezone conversion,
database, cross-device synchronization, call-history access outside Android,
and iOS implementation.
