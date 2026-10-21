# Dependency verification qualification

The canonical policy is `gradle/verification-metadata.xml`; the prototype copy was
retired after qualification. Gradle enforces artifact and repository-metadata
SHA256 checks in ordinary builds. The original736 entries were compared against fresh official
repository bytes or the completed239-artifact canonical binary audit before
promotion. Generation from a cache was only a candidate, never the trust decision.

The selected macOS debug/instrumentation/release/JVM/lint build passes under strict
verification. An intentionally wrong AGP JAR checksum and a missing AGP JAR entry
both fail before app compilation; the original policy is restored afterward.
The Linux aapt2 JAR is downloaded and checked for the hosted build recipe, but
Linux/hosted execution is still unobserved. Windows has no qualified build claim.

No broad trusted-artifact exceptions or disabled metadata checks are introduced.
Publisher signature verification is not enabled; verification uses approved SHA256
and canonical HTTPS repository provenance;
independent publisher signatures and independent-host reproduction are not claimed.
A new dependency/version/classifier or changed repository metadata needs reviewed
origin/licence evidence before changing the policy. Do not blindly regenerate trust
entries to dismiss a verification failure.

Dependency locks are not added: the reviewed equivalent for these configurations
is the exact component/version, artifact and POM/module checksum allow-list. This
is not a universal graph lock or a vulnerability audit. New platform/task scopes
must be qualified explicitly; an unknown artifact fails closed.

Recheck a candidate policy with `scripts/audit_gradle_verification.py`, using the
Track5 canonical build-tool audit as binary evidence and a new output directory.
Keep all failed receipts. Use source-bound clean builds and applicable native tests
after material changes. The Gradle file is also included in debug source receipts,
so policy edits invalidate old native-build preflight receipts.


## Cold Linux metadata extension — 2026-10-22

A genuinely cold Linux resolution rejects three metadata files not requested by
the earlier warm-cache macOS runs: Guava parent33.3.1-jre POM, JUnit BOM5.10.2
module metadata and coroutines BOM1.8.0 POM. Each failed guest download matches a
fresh canonical Maven Central HTTPS download. Add only those three SHA256 entries;
all736 previous entries are unchanged, giving739 total. Metadata verification
remains enabled, without trust exceptions or automatic checksum generation.
These are metadata for already resolved build dependencies, not new app libraries.
Initial failure and diagnostic overlay results remain distinct from final clean
source/cold-cache acceptance. The subsequent Track5 Linux evidence owns the result.
