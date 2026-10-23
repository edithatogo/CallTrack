# Track 14 prerequisite deferral

Date: 2026-10-23

## Scope

Proceed with local implementation and controlled emulator/build work for the
Compose migration while Track 5 and Track 6 remain open. This is a development
scope decision, not acceptance of their release or operational gates.

## Reason and evidence

- Track 6 completed active-code Kotlin migration and selected API 26/33/36
  emulator suites, but full M5 acceptance remains linked to Track 5.
- Track 5 has no selected production signing identity/key custodian; GitHub
  hosted CI is blocked before runner allocation by the account billing or
  spending limit; physical-call and selected calendar-route evidence is
  pending; and seven-day observation has not started.
- On 2026-10-23, `adb devices -l` returned no devices and `emulator` was not on
  PATH. No native Compose acceptance is claimed by this deferral.

## Consequences

Track 14 may add locally qualified Android source and tests, but the app is not
release-ready, no production candidate is promoted, and Track 5/6 remain
unfinished. Emulator-only evidence will remain distinct from physical calls,
remote calendar outcomes, cloud consent and operational observation. Tracks
15–20 remain ordered; iOS cannot start before Tracks 1–8 have passed or have
explicit justified dispositions.

## Reconsideration trigger

Before Track 15 adopts shared abstractions, and before any release/candidate
promotion, refresh the Track 5 and Track 6 handoffs. Resolve or explicitly
disposition the linked gates and obtain actual native UI acceptance on a
qualified emulator. This scoped deferral does not waive or complete them.
