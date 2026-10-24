# Track 15 toolchain audit review — 2026-10-24

## Scope and conclusion

Reviewed the new Kotlin/Native compiler trust entries and the supporting publisher artifacts. The exact Maven archive and POM hashes are recorded in Gradle verification metadata and the track audit. The Maven archive's member payload matches the independently published JetBrains release tarball. This qualifies the pinned compiler distribution checksum for local build resolution; it does not qualify the native toolchain or release.

## Evidence checked

- Maven Central compiler archive SHA-256: `ae4c8d0d0c18a9dd3ac6a9eb1222c1b902f78ac0996de634f7a771faf5e6e583` (256,454,474 bytes). Maven SHA-1 sidecar: `c557c413016b263592b053226bb6a69f7b31d5b4`.
- Maven Central POM SHA-256: `b76ad8c3146787c2e38a059e03419a5e766451e787eefad08b365626daeade3c`; published SHA-1 sidecar matched; POM declares Apache-2.0.
- JetBrains GitHub release tarball SHA-256 and published sidecar: `5418391a3b94d7bff6794668c8e813ebc471f46e04f828a35e88469c367efa4d`. Its compressed bytes differ from Maven Central, but the 26,128 regular-file entries match by path, size, and SHA-256.
- JetBrains release SPDX attachment is valid SPDX 2.3 document metadata but lists no packages or relationships. The bundle README includes a third-party licence inventory; its source-wide scope does not prove binary applicability or legal clearance.
- LLVM secondary download matches its publisher sidecar. LLDB secondary download SHA-256 `069193359103d4e4a4653f236e7a963e266a4a366905e858d4a68e701f43866e` does not match publisher sidecar `6c97f0a4755b83282b037800ac4c0591ebb8e7a3517bb53924bcb61684243d45`; treat the downloaded LLDB archive as untrusted pending resolution.
- `xcode-select -p` selects `/Library/Developer/CommandLineTools`; `xcodebuild -version` fails because full Xcode is absent. Kotlin/Native 2.3.21's `konan.properties` sets `minimalXcodeVersion=12.5`; its macOS ARM64 toolchain entries reference `xcode_26.0.1_17A400`.

## Review finding and disposition

**High — macOS Kotlin/Native acceptance is blocked.** Do not mark the common fixture task, native phase checkpoint, or Track 15 complete. The toolchain's resolved secondary LLDB download has an unresolved publisher-side checksum discrepancy, and the required full Xcode/Apple SDK are not selected. No native test result is claimed.

The strict Gradle metadata adds only the compiler artifact and POM hashes verified from Maven Central, preserves earlier entries, and grants no trust exception. Release readiness remains false. Revisit native acceptance when the LLDB mismatch is resolved and a compatible Xcode/SDK is available; rerun the native fixtures and inspect produced test artifacts.
