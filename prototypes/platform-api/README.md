# Compile-only macOS API surface experiment

This Swift file has no entry point and only references public EventKit method types.
It creates no store and invokes no API: no calendar read/write, permission prompt,
account inspection or phone-history collection occurs. Do not use it as an app
adapter or run it to test permissions.

From the repository root on a Mac with the SDK installed:

```sh
xcrun --sdk macosx swiftc -typecheck -warnings-as-errors -target arm64-apple-macosx14.0 prototypes/platform-api/EventKitSurface.swift
```

Observed toolchain: CommandLineTools, Swift6.4, macOS SDK27.0, arm64. A deployment
target argument checks availability annotations; it is not a macOS14 runtime test.
No executable/installer is emitted. SDK surface compilation does not qualify TCC,
sandbox entitlements, user-facing permission strings, calendar eligibility or
readback. No iOS SDK/build/device or cross-platform sharing is tested.

See the [track](../../conductor/tracks/platform_feasibility_20261020/index.md) for
the conditional iOS default-dialer history route, capability matrix, evidence and
production deferral. This project has no root Gradle integration or new dependency.
Retire this isolated directory when its SDK question is no longer useful; do not
change installed SDKs, calendar state, Android app inputs or global caches.
