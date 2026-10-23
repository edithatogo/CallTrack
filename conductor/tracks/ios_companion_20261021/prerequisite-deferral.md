# Track 20 scoped prerequisite deferral — 2026-10-24

## Reason and evidence

The user explicitly requested Track 20. The approved sequence still requires
stages 1–8 to pass or have explicit justified dispositions before iOS product
implementation. That gate is not met: Tracks 5/6 retain release/acceptance work;
Tracks 14–17 remain in progress; Track 19 is a bounded evidence reconciliation,
not cross-platform qualification. Track 18's Rust deferral is resolved, but it
does not resolve Track 19. The local host has Command Line Tools only;
`xcodebuild` requires full Xcode, `simctl` is unavailable, and no Xcode app was
found in `/Applications`.

## Permitted scope

This record permits only Track 20 requirements, platform API and dependency
readiness research, a decision and conditional acceptance handoff. It authorizes
no iOS application source, project, package, dependency or signing setup; no
calendar permission request, provider operation, cloud account or publication.
It does not claim that iOS implementation has begun or that any prior stage has
passed. Remove this scoped record and begin iOS implementation only after the
approved stage 1–8 gate is satisfied.

## Consequences

Track 20 remains incomplete, production adoption is false and every earlier
unresolved stage remains unresolved. No iOS feature, build, simulator, device,
calendar or release acceptance is claimed. The user-approved iOS-last ordering
remains in force.

## Reconsideration trigger

Recheck the stage dispositions, current Kotlin/Compose/Xcode compatibility, full
Xcode/simulator availability and selected iOS test scope after Track 19 is
qualified. Then remove this scoped record, refresh the implementation plan and
proceed with native adapters only after review of the final requirements and
dependency decision.
