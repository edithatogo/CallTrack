# Plan: Android Compose discovery and migration

Research-led conditional track. Start8e9289a, input e6607c0. Original scope permits
an evidence-backed defer/reject outcome; no production screen adoption is assumed.

## Phase1: Discovery and mapping

- [x] Map active/legacy screens, permission/action boundaries, accessibility and state — findings.md
- [x] Compare current tooling and dependencies versus retaining Views — decisions.md C01
- [x] Specify staged coexistence, UI acceptance and removal criteria — requirements C01–C08
- [x] Write findings and refine experiment scope before implementation
- [~] Phase checkpoint — source and result evidence being verified

## Phase2: Options and dependency decision

- [x] Compare Views, ComposeView, Activity rewrite and scaffolding-library takeover
- [x] Define isolated synthetic prototype, pinned compatibility and expiry/promotion/removal
- [x] Establish failing policy contracts before model implementation — missing PreviewState compile failure recorded
- [~] Run bounded build/emulator/accessibility experiment and record measured findings
- [ ] Record final retain/adopt/defer decision with unresolved gates
- [ ] Phase checkpoint

## Phase3: Conditional production implementation

- [ ] Based on final evidence, adopt bounded integration with regression tests or document justified deferral as not applicable
- [ ] Preserve root code/data and verify rollback boundary; no production replacement without parity
- [ ] Phase checkpoint — do not claim provider or manual accessibility proof from prototype

## Phase4: Handoff and review

- [ ] Update consolidated requirements/roadmap/stack with actual outcome and future inputs
- [ ] Review all changes, run tests, apply fixes and record exact evidence
- [ ] Complete/archive only the conditional research track when its decision/handoff gates pass
