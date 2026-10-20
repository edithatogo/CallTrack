# Build and dependency decisions

## D-B01 — Proceed with the fork

Status: adopted development direction; public release/fork publication not performed. Owner: build → calendar slice/parity. Compared retaining original unchanged, a bounded modernised fork, starting anew, or deferral. Pinned upstream builds and the modern copy launches with usable call-log permissions under bounded tests. The small build repair plus existing UI/config/worker scaffolding favours incremental reuse. A fresh app would still need all parity/recovery work and recreate this scaffolding; no measured benefit justifies that rewrite now.

Preserve the reference and use explicit build transformations as reproducible fork preparation. Next track can bring the selected source into the application workspace with the same provenance. Rollback: discard a generated copy, retain source and receipts; no user data migration took place. Original APK code is not adopted. GPLv3 and NOTICE are retained; dependency licence/vulnerability qualification is still needed before distribution.

## D-B02 — Modern development toolchain

Role: adopted qualification/development candidate, not a production-ready release stack. AGP8.13.2, Gradle8.13 with official SHA256, JDK17, compile/target36/build-tools35.0.0. The original AGP7.2.2/Gradle7.3.3/JDK11/SDK32 remains a reference reproducer only. Remove JCenter and add explicit namespace in generated modern copy. No blanket runtime library upgrade. SDK37/AGP9 was considered from official compatibility docs; not needed for the current bounded target36 test. Review on new platform requirements or before release. Native call/calendar API and WorkManager semantics still need later acceptance.

## D-B03 — Interim Fragment compatibility replacement

Role: temporary compatibility override for development. `androidx.fragment:fragment:1.5.1` replaces inherited1.4.1 to restore lint checks skipped by modern lint. Align `androidx.lifecycle:lifecycle-viewmodel-ktx:2.5.1` with ViewModel2.5.1 selected by Fragment, following the observed duplicate-class failure. Both are interim overrides and must be reviewed together. Evidence is the reproducible ObsoleteLintCustomCheck diagnostic and final lint rerun. Existing other runtime dependencies are retained for comparison, not endorsed indefinitely. Review at Compose/dependency migration; promote only if supported version policy and behavioural tests justify retention, otherwise use a newer coherent UI stack or remove Fragments after verified Compose parity. Rollback by removing override in an isolated copy; no data schema changes.

## D-B04 — Prototype/testing dependencies

google-java-format1.24.0 AOSP mode, JDK17: development-only formatting, no Android runtime role. Record JAR digest and reuse for new Java; do not mass-format upstream. AndroidX test dependencies inherited from upstream provide runner/JUnit assertions for the test APK; do not ship the test APK as app functionality. Python scripts use stdlib; analysis environment from Track1 is not a build/runtime prerequisite. SDK command-line tools19 are local provisioning tools, not production dependencies. Review tooling versions on toolchain upgrades. Temporary build copies live under excluded `.local`; preserve receipts and source identity before regenerating/removing them.

## D-B05 — Identity, signing and implementation scope

Use `org.calltrack.qualification` and `.test` only for controlled qualification; refuse replacing installed packages. Default upstream signing is a local Android Debug certificate; its public fingerprint is recorded, private key excluded. No permanent public application ID or release key is chosen. No legacy app/data replacement and no signing lineage continuity is implied. Instrumentation grants permission through ordinary ADB dev tooling, tests zero rows and revokes it; normal consent UX/store eligibility are separate.

The bounded implementation is build preparation, reproducibility/guard scripts, instrumentation tests, formatting and prepared CI. Full call-domain logic and custom-code-to-library takeover stay with calendar/parity and later dependency tracks; this decision does not promote their shortlists prematurely.
