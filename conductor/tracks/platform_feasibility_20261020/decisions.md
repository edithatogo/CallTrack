# Dependency/capability decision PLAT-D01

- Owner/date/refresh: Track12,2026-10-21; R24/F01–F12; [findings](findings.md),
  [verification](verification.md).
- Problem: select useful second-platform scope without assuming Android call-history
  access or treating generic cross-platform frameworks as capability providers.
- Existing implementation: Android native providers/ledger and Views; isolated
  desktop Compose sample; no KMP core, Apple adapter or second-platform product.
- Options: ordinary manual/import companion; eligible iOS default-dialer route;
  app-owned VoIP; Windows capability-based acquisition; native versus shared
  UI/domain adapters; defer. Reject private database/backup scraping.
- Versions/licences/matrix: installed Swift6.4/macOS SDK27.0/arm64 observed, probe
  macOS14+ signatures only. iOS26+ history docs and entitlement conditions recorded.
  No third-party library/version/graph, iOS SDK or signed product selected; existing
  compiler terms unchanged. Refresh all target/library licence/ABI matrices at trial.
- Intended role: native platform SDKs are candidate adapter boundaries. A small
  compile-only probe is retained as a research artifact, not a runtime dependency.
  KMP/Compose/Rust/client adoption stays with its existing decision gates.
- Prototype hypothesis/effort: can this installed macOS SDK type-check the needed
  EventKit permission/list/read references without touching calendar data? One
  source file and compiler invocation; no runtime functions called. Result and
  input/output hashes in verification/evidence. No user-data fixtures or permission.
- Replacement scope/cost: native adapter could replace future handwritten permission/
  calendar glue; no adapter exists to retire now. Shared code cannot replace native
  TCC/sandbox, scheduling, entitlement, packaging or role-specific acquisition.
- Contract/error/privacy/upgrade: F01–F12 retain native denial/revocation, uncertain
  write, ID change, human edit, role-loss, import trust and cross-target test gates.
- Decision: **recommend ordinary manual/import companion as useful limited scope;
  defer production implementation**. macOS is the first native experiment candidate,
  with iOS ordinary companion next after tooling/device acceptance. This does not
  select a production target or satisfy Track8/9 runtime prerequisites.
- Conditional alternate: current default-dialer history support corrects older broad
  assumptions, but role/entitlement/test-region/coverage/product-cost uncertainty
  keeps it separate and deferred; neither reject all iOS capture nor promise parity.
- Expiry/removal: refresh Apple/Windows API policy and eligibility at reopening;
  remove compile-only probe if it stops resolving an SDK question. Do not silently
  promote it to a working permission adapter. No new cache cleanup is necessary.
- Migration/rollback: no app/schema/settings/default-role migration performed.
  Future companion must preserve Track10/11 identity/receipt/recovery contracts;
  one destination writer and no automatic old-event replay. Retire only isolated
  experiment files; leave Android app, existing calendars and global tools intact.
- Actual validation: documentation, SDK surface and host/link checks only; no iOS/
  Windows runtime, phone-history read, entitlement grant, signing, file picker,
  calendar write/readback, native accessibility or cloud sync acceptance.
- Follow-up: reopen after concrete workflow/platform choice and Track4/5 stability;
  Track8 domain/Swift consumer, Track9 UI and Track10 calendar contracts must all be
  qualified for chosen targets. Optional dialer research needs its own explicit scope.
