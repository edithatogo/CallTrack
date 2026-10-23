# Upstream contribution assessment — 2026-10-23

## Actual repositories and scope

The CallTrack working repository is source-derived from AndroidCallLogSync but
was initialised with independent Git history; it has no configured remote.
It was not previously a GitHub fork. The user requested actual upstream PRs.
A verified GitHub fork now exists at
[edithatogo/AndroidCallLogSync](https://github.com/edithatogo/AndroidCallLogSync),
whose parent is MarkoBL/AndroidCallLogSync. Its contribution checkout is private
local tooling under `.local/upstream-pr-20261023/checkout`, separate from this
working repository and its native/observation state.

Upstream's current README describes call-log POST delivery to a user-specified
HTTP endpoint, including its JSON/header contract. This successor instead uses
selected-calendar delivery, its own receipts/recovery/UI and development identity.
The new Python/Conductor helpers do not exist in upstream. Replacing the upstream
product with the successor, or labelling the Python/JDK/Conductor changes as
portable upstream app features, would misrepresent their scope. Those changes
need a selected successor repository for review; no such target is selected yet.

## Opened upstream PR

[MarkoBL/AndroidCallLogSync#3](https://github.com/MarkoBL/AndroidCallLogSync/pull/3)
is OPEN, non-draft, into main from
`edithatogo:codex/verify-gradle-distribution`, commit
`622da28e9482a8860e7bb3158fad60ccf97ef0a8`.
Only `gradle/wrapper/gradle-wrapper.properties` changes: the existing7.3.3
distribution is pinned to Gradle's published SHA256
`b586e04868a22fd817c8971330fec37e298f3242eb85c374181b12d637f80302`.
A freshly downloaded ZIP matched that digest. Separate fresh wrapper caches
proved the correct hash starts7.3.3 and a deliberately incorrect hash fails
verification before execution, using the existing wrapper JAR and JDK17.
The local test probes use a file URL; the committed URL is unchanged.
No Android build/native device acceptance is claimed for this configuration fix.

The PR's state/base/head/commit/single-file diff were read back and it is attached
to this chat. The fork/PR contain upstream code and the checksum fix; no CallTrack
private journal, observation evidence, APK, account or device data was pushed.
No PR has been opened for the independent Python/JDK/Conductor feature set.
