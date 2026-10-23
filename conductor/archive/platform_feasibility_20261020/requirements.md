# Platform requirements and qualification gates

R24 refined into capability-aware scope, not Android/iOS feature parity.

| ID | Required behavior / acceptance | Current status |
|---|---|---|
| F01 | Distinguish ordinary companion, eligible default dialer, app-owned VoIP and desktop source adapters; explain unavailable capabilities | Matrix documented; no runtime source adapter |
| F02 | Ordinary companion supports deliberate manual/supported-file import with provenance, bounded parsing, preview and no default side effects | Recommended scope, native picker/parser tests pending |
| F03 | EventKit readback route handles full/write-only/denied/restricted/revoked states truthfully; write-only never labeled reconciled | Compile-only macOS API surface; native permissions/read/write untested |
| F04 | Destination binding/receipt handles account/calendar ID changes and human edits; device persistence distinct from remote sync | Track10 I01–I13 gates; no EventKit delivery engine |
| F05 | Foreground interrupted work resumes conservatively; native expiry/cancel/sleep/restart tests; no exact background timing promise | Required before integration |
| F06 | UI works with native keyboard/back/focus/file dialogs, large text, localization, VoiceOver and narrow/wide layouts | Track9 offscreen test is insufficient; native/manual acceptance pending |
| F07 | iOS device/simulator builds, signing/usage strings/entitlements and min-OS behavior qualified independently from macOS | No iOS build or install; no new entitlement/consent |
| F08 | Default-dialer research requires current entitlement/account/test-region eligibility, supported OS/device and explicit product scope | Future conditional route; user in Australia does not establish an EU test setup |
| F09 | Dialer history tests establish start-of-coverage, role changes, lost notifications, retention/paging/status/duration/identity semantics; no pre-role history promise | Documentation only; no call/history API invoked |
| F10 | Windows acquisition requires qualified app type/capabilities, actual populated history/fields and store/runtime evidence | API documented, Windows acceptance pending; not universally unavailable |
| F11 | Restore/import keeps foreign receipts inactive, local annotations independent, tokens excluded and calendar writes deliberate | Track11 J01–J12/restore trust gates; no implementation |
| F12 | One source-bound fixture corpus and native adapter contract per supported target; separate compile/UI/provider/cloud/manual receipts | No common KMP core or cross-target suite exists |

## Proposed first native workflow

Recommend macOS manual/synthetic-file preview and later EventKit integration because
this host can inspect SDK surfaces without installing new tooling. This is a
research recommendation, not a user-validated target commitment. Scope one platform
and one calendar adapter, retain Android unchanged. A comparable iOS companion
follows only with native project/device/simulator and privacy/runtime qualification.
Do not require becoming a dialer merely to offer an ordinary companion.

Native acceptance must use isolated synthetic inputs and selected test calendar:
permission denial/revocation, explicit destination selection, insert+exact readback,
restart/ambiguous insertion, remote edit preservation, source-independent recovery,
calendar removal/rename, export reimport and verified cleanup. A system editor's
save callback or write-only API return must not count as app-owned readback.

## Default-dialer route is separate research

Inspect current entitlement/region requirements before any eligible-device trial;
no region/account spoofing or default-role change is authorized by this document.
If later chosen, test incoming/outgoing/missed/canceled/withheld/multiple-party calls
on controlled devices and the actual RecentConversation attributes, while keeping
source IDs/provenance distinct from Android CallLog rows. Never mark conversations
read just by logging them, place calls, record audio or infer complete history from
observed notifications. Eligibility, distribution and duration/coverage guarantees
remain unresolved. Production dialer functionality is outside initial app scope.
