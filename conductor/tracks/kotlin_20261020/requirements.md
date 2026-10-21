# Kotlin requirements and remaining migration gates

R14/R26, refined 2026-10-20. Original whole-track acceptance remains unchanged.

| ID | Current/proposed behaviour | Acceptance |
|---|---|---|
| K01 | Three pure Java components become Kotlin behind existing Java callers | Same constructors/public fields/static methods; golden source/event hashes, Unicode/ASCII/null/overflow fixtures pass |
| K02 | Add explicit Kotlin compiler to existing Android build | Pin compatible KGP/runtime; Java/Kotlin target1.8; debug/test APKs, release compilation, duplicate checks and lint pass |
| K03 | Preserve data compatibility | No schema/signature/namespace/version changes; preserving SQLite/emulator regression at new source-bound APK; no app-data clearing |
| K04 | Preserve verification integrity | Harness hashes include every Kotlin source; CI checks Kotlin style and does not depend on private APK/reference inputs |
| K05 | Document responsibility takeover | Compare native/owned/library/deferral; each new tool/dependency has role, promotion/removal and rollback evidence |
| K06 | Finish app-owned Java migration | Refresh the dated21-file pilot inventory to47 (33 active/14 inherited); convert stabilized active code and remove/retain justified legacy exceptions after parity; test same data/contracts |
| K07 | Full M5 handoff | M4 prerequisite, supported preserving update/recovery evidence, no unqualified operational claims; record residual Java exceptions and next Compose/KMP inputs |

K01–K05 can have bounded local/native evidence while K06/K07 remain open. The user
has authorised emulator tests; physical-call acceptance remains pending. No new
public distribution, signing or user-consent surface is implied.

## Current continuation status

K01–K05 pilot evidence remains historical. K06 active conversion/removal is
implemented:36 Kotlin files plus one justified nullable-label Java bridge. K03/K06
private-state preserving update and selected native suites pass after dependency
review; final lifecycle/telephony/settings/recovery acceptance remains blocked by
guest startup ANRs/provider availability. K07 still requires original release/supported operational evidence; code
conversion alone does not close M5. See the approved future implementation sequence.
