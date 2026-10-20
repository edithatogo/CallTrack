# Future destination qualification protocol

This is a test specification, not an executed acceptance report. It requires a
selected authorized test account/calendar and capability readback; none was selected
here. Prefer a disposable synthetic calendar and never query real phone call logs.
Physical-call acceptance remains pending per user direction.

1. Bind the exact app revision/artifact/toolchain and current device/profile. List
   only needed calendar metadata through the app's authorized route; verify selected
   account and writable collection without logging account identifiers publicly.
   Installed Outlook/calendar UI alone does not qualify provider visibility.
2. Confirm the native route already exposes the required destination. For Outlook,
   establish supported account/export/profile/tenant conditions. For CalDAV, establish
   existing sync adapter/server collection. If unavailable, record the precise gap.
   No purchase, new consent or settings change is presumed by this protocol.
3. Preview one unmistakably synthetic call event: actual zero/nonzero duration,
   timezone, redacted identity, private/free/no-alarm and no attendees. Commit intent,
   insert through the selected route, reopen ledger and read the exact event ID.
4. Independently reopen the selected remote calendar through its supported client
   or API. Confirm one event, fields and expected privacy/reminder behavior; record
   elapsed sync time separately from local persistence. If unavailable/delayed,
   local pass remains local-only; do not invent a delivery-time guarantee.
5. Edit notes/time/privacy remotely and allow native round trip. Check normalized
   fields and ownership marker survival. Retry/restart the original operation:
   it must not overwrite edits or insert another event. Missing or stripped markers
   are a documented reconciliation gap, never permission for fuzzy adoption.
6. Exercise offline/reconnect, account permission revocation, destination removal,
   same-name calendars and same local ID after account recreation in isolated test
   state. Work must pause or reconcile, never switch to a default or reset history.
7. Only if direct route is justified, qualify provider auth/SDK graph and the
   response fixtures in requirements. Test response-loss after accepted insertion,
   throttling, expired tokens/cursors, conditional-write conflicts and readback
   normalization. Observe actual server counts; fakes cannot qualify remote semantics.
8. For route migration/fan-out, establish exact correspondence before any second
   writer. Test preserved schema1/2 receipts and rollback/restart, per-route payload
   redaction, partial success and no export→import loop. No source call-log mutation.
9. Cleanup only explicitly identified synthetic events/accounts created for this
   test, after confirming unchanged fingerprints. Preserve edited/ambiguous objects
   for review. Verify removal both locally and remotely; disabling sync or deleting
   local app data is not evidence of safe remote cleanup.

Record per route: source/artifact hashes, platform/account category (redacted),
authorization/capability result, local event/ledger readback, remote readback, edit
round trip, recovery/cleanup result and unresolved exceptions. Never commit tokens,
calendar contents, personal identifiers or service payload logs.
