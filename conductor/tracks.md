# Tracks registry — current Android 17 scope (2026-11-24)

**Authority:** [active-scope.json](active-scope.json) · [native architecture](architecture-android17.md). The [former nine-stage execution order](future-architecture.md) is a historical evidence ordering only. A deferred track is not active merely because an old plan has unchecked items.

- [~] **Track 6 — Kotlin native migration / retaining source contracts**: [Track](tracks/kotlin_20261020/index.md) — verify exact Android 17 source/release integration.
- [~] **Track 5 — Android 17 release qualification**: [Track](tracks/release_20261020/index.md) — retain signing, physical call/account, preservation and distribution gates.
- [~] **Track 14 — Native Jetpack Compose UI completion**: [Track](tracks/compose_app_20261021/index.md) — remove residual hybrid/custom controls only after Android 17 rendered parity; [issue #116](https://github.com/edithatogo/CallTrack/issues/116).
- [~] **Track 22 — Room 3.0.3 preserving migration**: [Track](tracks/room3_android17_20261124/index.md) — Room owns both preserved databases. Complete suspend DAO parity and remove the bridge. [issue #115](https://github.com/edithatogo/CallTrack/issues/115).

## Deferred: no active implementation or automatic CI

- [ ] **Track 15 — Kotlin Multiplatform shared core**: [Deferral](tracks/shared_core_20261021/deferral.md)
- [ ] **Track 16 — macOS companion**: [Deferral](tracks/macos_companion_20261021/deferral.md)
- [ ] **Track 17 — Compose Multiplatform shared UI**: [Deferral](tracks/shared_ui_20261021/deferral.md)
- [x] **Track 18 — Rust component evaluation**: [Track](tracks/rust_component_20261021/index.md) — production Rust previously deferred.
- [ ] **Track 19 — Consolidated cross-platform qualification**: [Deferral](tracks/cross_platform_20261021/deferral.md)
- [ ] **Track 20 — iOS companion**: [Deferral](tracks/ios_companion_20261021/deferral.md)

Completed baseline/parity/python research and historical Android 26/33/36 evidence remain in [archive](archive/index.md). Older Android versions are *not supported targets* in active CI; their schema upgrade fixtures still protect existing user data. Python runtime selection comes from `.python-version`, not copied prose.

**Evidence discipline:** link exact immutable historical receipts or Actions run artifact, identify source SHA and test command, and keep one current checkpoint per accepted change. Do not replicate large JSON test logs as a second mutable evidence copy.
