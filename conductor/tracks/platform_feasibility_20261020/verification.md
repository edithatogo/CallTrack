# Verification and evidence boundaries

Baseline `c2b1a5c`; activation `cdd61e8`; date2026-10-21.

Primary Apple Markdown documentation and Microsoft Windows documentation refreshed.
Local inventory: selected CommandLineTools, Swift6.4, macOS SDK27.0, arm64. No new
licence accepted, app installed, role changed, calendar/history queried or permission
requested. No private database or phone backup inspected.

Experiment: `xcrun --sdk macosx swiftc -typecheck -warnings-as-errors -target
arm64-apple-macosx14.0 prototypes/platform-api/EventKitSurface.swift` references
SDK method types without instantiation/invocation or an entry point. Compiler
result is recorded below. No runtime regression tests needed for a compile-only
source artifact; no runtime adapter or production behavior is selected.

No iOS build/SDK, native app packaging/signing, TCC grant, calendar/provider,
file-picker, manual accessibility or actual call/dialer capability was qualified.
The existing desktop prototype's offscreen tests are historical only. Cross-target
fixtures, actual supported workflow and production implementation remain pending.
