# Seven-day operational observation

Date:2026-10-22. Status: awaiting prerequisites; zero days/hours claimed.
The user authorised real observation and subsequent review/fixes. This document
prepares execution; it is not an executed seven-day report.

## Starting state and prerequisites

Only emulators are currently visible. Physical-call and remote-route qualification
are not yet complete. Resolve the [physical acceptance procedure](physical-calendar-qualification.md)
for the selected phone and claimed route(s), and confirm the user's observation
scope before starting. A connector default account, elapsed chat time, scheduled
run or emulator test cannot substitute for those prerequisites.

The existing app filters directions, not individual phone numbers. During normal
use, calls in enabled directions may disclose number/name/time/duration to the
selected calendar and its existing sync service. The user must choose normal use
on a specific private destination, or explicitly narrower controlled test sessions.
Do not enable unattended logging while that choice is pending. Controlled-session
results must retain that scope; they do not establish all-day normal-use acceptance.

Bind the cohort to the observed signed APK/certificate and source, selected device/
OS, destination account/calendar alias, sync adapter/version where observable,
logging settings/generation and usage scope. The prepared artifact is the15998ba
candidate with unsigned SHA256
89a548489035a6df8c464cc621e49dfbb0c9d6d16619bbe54f27f78e612d5f86;
verify actual installation identity and the development-signed copy before use.
Production identity/signing remains separate. Do not replace an existing working
installation or select an account implicitly.

The app has one active destination per installation. Bind each operational cohort
to one selected route/calendar. Qualifying Google and Outlook for seven days each
requires separately accounted route windows; alternating calendars during one week
does not establish seven days on both. Do not run duplicate writers or replay the
same receipts to manufacture cross-route coverage.

Private state starts at `.local/release-evidence/observation-20261022/state.json`.
It has null start/end dates and outcome counts, seven pending intervals and accepted
days0. Preserve that journal and supporting receipts; commit only aggregate outcomes
and aliases, never phone numbers, account identifiers or private event content.

## Real time and exposure

Start only after actual prerequisite readback and explicit participant readiness.
Record UTC start plus device time/timezone; earliest final review is start+168 hours.
Use seven successive24-hour UTC intervals, preserving local timezone context.
Do not count calendar date changes, scheduler ticks or seven receipts as seven days.
The readiness/preparation period earns no observation time.

Record actual app exposure and sampling separately. The phone can operate without
USB; disconnection alone is neither app failure nor proof that it kept logging.
Daily evidence and the participant's bounded journal must account for calls and
outcomes. A daily snapshot alone does not prove every call during the preceding day.
Missing evidence stays unknown, not zero. Reconstruct a gap only from retained
source/receipt/event evidence; otherwise extend or restart an explicitly recorded
window with a justified coverage decision, never sum unrelated sessions silently.

A verified zero-call interval may be recorded as such with latency samples empty,
but seven idle days cannot establish call-logging reliability. Sample every enabled/
claimed direction and relevant declared conditions, or retain those exclusions.
Normal-use and controlled-session cohorts must not be combined into a broader claim.

## Daily record

At the agreed daily check, inspect only the selected device/destination and bounded
observation interval. Avoid bulk call-history or calendar export. When the phone or
account is unavailable, record missing readback and ask for participant evidence
only when necessary; do not bypass permissions or alter accounts to gain access.

For each interval record:

- Expected calls by direction from actual source history/participant record, enabled
  filters, explicit exclusions and an opaque private identity to avoid double-counting
  the same call across repeated checks. Preserve denominator provenance.
- App receipt/operation state where observable, local provider event counts/IDs,
  unresolved review/pending work, duplicates and unexplained misses. A UI success
  message is not a durable receipt. Mark an observability gap when fields are absent.
- Remote event readback for each claimed Google/Outlook route in the exact selected
  account/calendar, with the identity/payload/time semantics established in physical
  qualification. Local-only state does not count as remote delivery.
- Actual local and remote delivery delay samples separately, sample counts, median,
  relevant upper percentile and maximum where meaningful; leave statistics unknown
  without samples. Prompt when possible/recover later is not a promised deadline.
- User edits/deletions and their preservation; call direction/duration rendering,
  permission changes, reboot, offline/Doze, screen/background and battery/OEM context
  actually encountered. Explicitly list unsampled conditions and manual accessibility.
- Candidate/configuration changes, incidents, recovery action, evidence references
  and whether the interval can be accepted. Never fill planned rows with defaults
  or overwrite an earlier incident to make the window appear clean.

Do not call, text or invite people automatically. Additional controlled calls require
participant readiness. Do not delete normal-use calendar events as test cleanup.
Preserve human edits and retained receipts, and use only individually identified
agreed test-event cleanup. Do not change battery policy, sync settings or permissions
merely to obtain a pass.

## Incidents, candidate changes and final review

An unexplained miss/duplicate, privacy/data-loss problem or unsafe recovery blocks
stable promotion. Retain redacted evidence, investigate and apply authorised fixes.
Repeat affected controlled acceptance and start a fresh observation window for the
corrected candidate as required by the release runbook. Preserve failed windows.
Source/build/signature/identity or material configuration/OS/adapter changes require
an explicit cohort/requalification decision; do not mix results under the old hash.
Host-only evidence/document changes do not relabel the running candidate.

After at least168 actual elapsed hours, reconcile all seven intervals against their
evidence and resolve pending remote delivery/review work. Final review checks source/
artifact/certificate bindings, denominators, coverage/exclusions, uniqueness, missing
and duplicate events, edit/deletion preservation, recovery, privacy and actual latency.
Prepare a report of passed/failed/unqualified scopes and apply findings. A scheduler
may request this review; it cannot mark it passed because seven days elapsed.
Any material fix triggers affected acceptance and a new qualified window rather than
closing against observations from the preceding build.

Original production signing, hosted/channel/source/support and wider-device gates
remain independent. Passing this window does not publish a release or complete M4
unless every original requirement is satisfied.

## Scheduled follow-up

Use one thread heartbeat at09:00 Australia/Brisbane each day. First recheck this
protocol, the physical baseline and private observation state. Until prerequisites
are real, keep start/end dates null and accepted days0. Check only non-sensitive
readiness metadata; request help on a meaningful readiness change, not every run.

Once the user-selected cohort is active, reconcile available bounded evidence and
record only observed facts. A missed scheduler run is not automatically a missed
phone call, and a completed run is not automatically an accepted day. Notify only
for meaningful readiness/completion/failure or genuinely required user action.
After final outcome review, retire the observation follow-up while retaining the
journal and unresolved release gates. Do not silently restart an observation under
a different candidate or grant consent/permissions through the scheduler.
