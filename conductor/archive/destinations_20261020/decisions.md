# Dependency decision INT-D01

- Owner/date/refresh: Track10,2026-10-21. Requirements R12/R19/R20/R25,
  I01–I13. Evidence: [findings](findings.md), [verification](verification.md).
- Problem: additional calendar/provider coverage must not create duplicate events,
  competing ledgers or unnecessary credentials/consent.
- Existing implementation: CalendarContract/AndroidCalendarSink plus schema2
  SQLite receipts. Existing sync adapters may already satisfy provider coverage;
  no tested shortfall yet justifies a new cloud client.
- Options compared: retain native provider (chosen); wrap existing native code;
  external device sync adapter; direct Graph/Google/CalDAV SDK; narrow REST client;
  Rust component; defer direct implementation.
- Versions/licences/matrix: findings records observed SDK/auth releases and upstream
  licences; no artifact graph, Android/KMP compatibility, licence/notice closure or
  server interoperability is qualified. No runtime dependency is selected.
- Intended role: retain existing production-development route; direct clients are
  deferred candidates. No prototype-only dependency was downloaded or installed.
- Replacement scope: SDK could take over future transport/models/auth boilerplate,
  DAV library discovery/XML; none replaces app-owned intent/receipt/edit-protection
  semantics or grants OS/provider access. Do not add a second ledger.
- Prototype hypothesis/effort: mocked REST could check serialized requests but cannot
  resolve the actual native-route gap, consent, account coverage or remote marker
  survival. No prototype is selected; specify cases first and execute at reopening
  against an explicitly selected synthetic test destination with end-to-end readback.
- Contract/error/security/upgrade tests: I01–I13 and qualification protocol define
  route identity, uncertain writes,409/412/auth/throttle/cursor handling, source and
  data migration, privacy and loop tests. These are acceptance specifications only.
- Decision: **retain native-provider-first route; defer direct connectors and
  multi-destination implementation**. Track4 is incomplete and no actual coverage
  gap/end-to-end service evidence exists. Research closure does not deliver R12's
  provider qualification or R19/R20/R25 future functionality.
- Review/expiry: reopen after Track4 stabilizes delivery and a named required
  destination fails native-route acceptance, or a companion has a demonstrated
  direct-only need. Refresh releases, auth/tenant constraints and service behavior.
  Temporary connector trials must be isolated, synthetic and retired or promoted
  with explicit evidence; none exists now.
- Migration/coexistence/rollback: no settings, schema, account or code migration
  performed. Future route changes freeze old writes, reconcile existing receipts,
  prove remote correspondence and preserve old IDs; re-enable only one writer.
  No destructive downgrade or synced-event deletion is an implied rollback.
- Actual validation: source/documentation/host/link checks only. No permission grant,
  OAuth consent, service request, real calendar write or remote sync acceptance.
- Handoff: Track4 must preserve route/operation ownership when widening schemas;
  Track8 must not freeze integer IDs into common contracts; Track11 owns CRM/tasks
  needs and Track12 owns native companion capability. Reopen INT-D01 before adding
  network/auth dependencies or enabling new routing/fan-out.

R12 endpoint implementation is also deferred: no receiver/workflow has been selected.
I13 requires receiving-side idempotency and a versioned HTTPS/auth contract; the
legacy HTTP/header behavior is not selected. No endpoint prototype exists.
