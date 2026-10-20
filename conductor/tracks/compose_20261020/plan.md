# Plan: Android Compose discovery and migration

Conditional research completed with justified production deferral. Implementation
b10aa28. This is not completion of M6 production migration or native UI acceptance.
Original spec/workflow explicitly allow a negative result or justified deferral.

## Phase1: Discovery and mapping

- [x] Map active/legacy screens, action/permission/accessibility/state boundaries — findings.md
- [x] Compare current tooling, compatible pinned candidates and dependency takeover — decisions.md, dependency-report.json
- [x] Specify coexistence, UI adoption and removal criteria — requirements C01–C08
- [x] Refine experiment scope before implementation; preserve working app
- [x] Phase checkpoint — primary sources,74 unchanged working-app inputs, explicit unresolved gates

## Phase2: Experiment and decision

- [x] Compare retain Views, ComposeView, Activity rewrite, navigation/DI/MVI and deferral
- [x] Define isolated standalone experiment and dependency promotion/expiry/removal
- [x] Failing policy contracts before model; implement and pass3 contracts
- [x] Run actual build/emulator/automated accessibility experiment and inspect image; retain failures
- [x] Add strict foreground-window guard after obstructed-image false acceptance; apply storage-owner/permission/fixture fixes
- [x] Record measured outcome: build/lint and25 host tests pass; final5/5 guarded UI tests fail on Android system windows after bounded recovery; both packages removed
- [x] Final decision: retain Views and defer adoption, with explicit reopening conditions
- [x] Phase checkpoint — verification.md and evidence.json separate observed success/failure

## Phase3: Conditional production implementation

- [x] Not applicable: justified deferral under original spec; no production screen adopted
- [x] Root code/data/dependencies preserved; standalone prototype can be removed independently
- [x] Phase checkpoint: no provider/real-permission/manual accessibility or unobstructed UI claim; native failures remain visible

## Phase4: Handoff and review

- [x] Update requirements/roadmap/stack and Kotlin/multiplatform handoffs; M6 production remains pending
- [x] Review per-file changes and apply findings; retain environment gate rather than suppressing it
- [x] Record review commit and final document/source/host checks — bc9fee8
- [x] Research handoff ready for user-authorised archive; production reopening gates retained

## Review fixes

- [x] Task: Record applied foreground-window, screenshot-owner, permission-isolation and fixture corrections — b10aa28 / bc9fee8

## Reopening checkpoint

Reopen production M6 after stable M3/M5 state contracts, a usable test environment,
real permission/provider/restoration regressions, refreshed dependency qualification
and manual TalkBack/Switch Access/localization acceptance. Existing strict native
checks must pass before any adoption; do not recycle the rejected semantics-only
pass or obstructed screenshot as acceptance. Prototype remains prototype-only.

Review record: bc9fee8. Research complete with justified deferral; native acceptance
remains blocked and must pass on reopening. Archive is authorised by the user.
