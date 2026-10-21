# Plan: Community distribution and release readiness

Original release acceptance is retained. Preparation can progress independently of
Track4; a source-matched release candidate and recorded operational acceptance
cannot. Track remains active and cannot yet be archived as complete.

## Phase 1: Discovery and mapping

- [x] Task: Research distribution eligibility, licensing and reproducible release/signing options — findings.md
- [x] Task: Map contributor needs, dependency inventory and privacy/data lifecycle — CONTRIBUTING.md, PRIVACY.md, dependency-report.json
- [x] Task: Specify alpha observation, update/migration/rollback and supported-device criteria — RELEASING.md
- [x] Task: Write findings and refine track requirements/acceptance with source provenance — requirements.md D01–D09; original acceptance retained
- [x] Task: Phase Verification & Checkpoint (refer to ../../workflow.md) — verification.md and preparation-evidence.json

## Phase 2: Options and dependency decision

- [x] Task: Compare retained/native/custom implementation with dependencies and deferral — decisions.md RD01–RD03
- [x] Task: Run a bounded prototype only where it resolves uncertainty; identify temporary dependencies and expiry criteria — native dependency report only; extra plugin/signing prototype unjustified
- [x] Task: Record decision, update spec and implementation tasks; propagate any changed prerequisite assumptions — independent preparation permitted; release still depends on M3
- [x] Task: Phase Verification & Checkpoint (refer to ../../workflow.md) — no new dependencies adopted

## Phase 3: Conditional implementation and verification

- [x] Task: Prepare contributor, security, lifecycle and release documents plus issue/PR templates; no app behaviour or dependency changes
- [x] Task: If adopted, write meaningful failing contract/regression tests before changing behaviour — no app change; existing documentation fixture test exposed new root-document dependency and was corrected
- [ ] Task: Qualify a source-matched release candidate after M3, stable identity/key custody and dependency/artifact provenance audit
- [ ] Task: Verify candidate tests plus device/provider readback, preserving upgrade and recovery; record operational acceptance separately
- [ ] Task: Phase Verification & Checkpoint (refer to ../../workflow.md)

## Phase 4: Handoff

- [x] Task: Update consolidated requirements and roadmap with actual preparation outcomes; no production stack adoption
- [x] Task: Document residual gaps, next-track inputs and checkpoint evidence — D01–D09, verification.md
- [ ] Task: Complete release/operational gates, then final checkpoint and archive

## Phase: Review Fixes

- [x] Task: Apply review suggestions — 7a7d180; README schema2 correction, transitive inventory, test fixture completeness and retained release gate

## Execution checkpoint

Track start: 4b31e39. User explicitly authorised implementation, review, fixes and
archive once completed. Track4 remains active, physical-call acceptance pending;
that prerequisite cannot be manufactured by release documentation. No publishing,
key generation, account consent or external messaging was performed. Resume at
D01–D09 after the M3 evidence exists; do not repeat discovery without changed inputs.

Preparation implementation: 4754d72. Review corrections: 7a7d180. Final repository
checks passed; no candidate release was created and archive remains conditional on
the open Phase3/4 acceptance tasks.

## Productivity research handoff

[Track11 requirements](../../archive/productivity_20261020/requirements.md) distinguish future
annotations/index/export copies from existing payload/receipt retention. No new
storage/backup policy is adopted. Reopen lifecycle and recovery acceptance before
shipping authored journal data; research closure is not release readiness.

## M3 acceptance handoff — 2026-10-21

[Track4 handoff](../../archive/parity_20261020/handoff.md) now supplies selected API36 emulator
acceptance: schema6/import2,110 JVM/73 host tests and89 successful native invocations
plus one expected process kill. Resume the original remaining gates above; do not
repeat completed discovery or infer physical/cloud/release acceptance. This handoff
does not mark this track complete or waive its original prerequisites.

## Resumed execution — 2026-10-21

- [x] Task: Qualify unsigned release build from two isolated clean committed-source checkouts — 6b353f4; unsigned-evidence.json: identical APKs,110 JVM tests each,zero lint errors. Same-host comparison is not independent-environment reproduction.
- [x] Task: Review candidate tooling and results — review-20261021.md; fixes66593c3,78 host tests. Original identity/signing, upgrade and operational gates retained.

Track4 M3 selected-emulator handoff is available. No production signing, key
generation, publication or physical-call testing is implied by this resumption.

User selected retaining the development identity and unsigned qualification only;
production signing remains pending. Native release/upgrade acceptance was not run.
Runtime graph inventory now includes89 artifact hashes and cached POM declarations,
with one missing POM licence declaration; this is not licence clearance or trusted
verification. Both clean builds used candidate6b353f4; later changes are tooling
tests/docs only and app/build inputs remain identical. Track remains active; no
archive or complete-M4 claim. See unsigned-evidence.json and review-20261021.md.

## Kotlin continuation candidate — 2026-10-21

- [x] Task: Requalify two isolated clean unsigned release builds after active Kotlin migration and transitive-version correction — candidate787ae35, unsigned-kotlin-evidence-20261021.json;115 JVM tests each,zero lint errors,identical APKs.
- [ ] Task: Close the original production identity/signing, licence/trust, independent reproduction, supported upgrade and operational gates before full M4.

The new candidate supersedes the earlier6b353f4 artifact claim for current app
inputs; old evidence remains historical. All68 retained runtime artifacts match
the prior inventory. Seven unused direct declarations are retired while three
still-consumed transitive versions are preserved by compatibility constraints.
Cached artifact/POM continuity is not licence clearance or independent trust.
The user's development-identity/unsigned scope remains in force. No publication,
key generation, cloud consent, physical testing or elapsed observation is claimed.

## Local release continuation — 2026-10-21

- [x] Task: Audit canonical repository bytes/POMs/embedded notices for all current runtime artifacts — b3774ac,77f1533;63 unique artifacts from68 resolved rows; all match. Build tools and redistribution clearance remain separate.
- [x] Task: Compare retained assets to the audited upstream and preserve unresolved rights —77f1533;23 resource/artwork files,18 unchanged; source hash continuity is not rights clearance.
- [x] Task: Verify exact unsigned artifacts and corresponding-source archive — local-artifact-checks-20261021.json; original787ae35 candidate retained without relabelling later tool commits.
- [x] Task: Qualify development-key increasing-version replacement, controlled downgrade/wrong-key rejection and private-state preservation —547c869,b3774ac,77f1533; development-upgrade-evidence-20261021.json passes four native invocations and cleanup.
- [x] Task: Review local continuation, apply fixes, run host/document checks and record bounded native evidence — review-local-qualification-20261021.md;105 host tests and current Conductor verification pass. Review-fix commit recorded below.
- [ ] Task: Complete original D01–D09 release gates; no archive or full-M4 completion from this local continuation.

- [x] Task: Apply local qualification review fixes and commit current evidence — 94f8893; production/physical/cloud/operational gates remain pending.

## Local licence/tool-trust and schema/provider continuation — 2026-10-21

- [x] Task: Inventory and audit selected build-plugin/compile/test/lint dependencies, wrapper and SDK tool origins; preserve licences/notices and unresolved rights without production conveyance —350ce3d,3daa8ef,b1ed08b;239 binaries,736 enforced metadata/binary checks; JDK resource sealing explicitly unqualified.
- [x] Task: Qualify exact source-pinned schema1–5 fixtures across10→11 replacement with human-edited/deleted events, ambiguous insert recovery, paused/permission-loss/resume and migration rollback/newer-schema refusal —350ce3d,caf8d8c;15 native invocations including import1→2; cleanup/readback pass.
- [x] Task: Review continuation, apply fixes and record source-bound local evidence; retain production signing, physical, cloud and operational gates — review-licence-tool-upgrade-20261021.md;121 host tests,154-document verification and fresh5578bf5 clean-build comparison pass.

- [x] Task: Promote independently audited strict Gradle verification after positive/fault tests; bind policy in build receipts —5578bf5 (policy),caf8d8c (receipt binding).
- [x] Task: Qualify two clean unsigned release builds under the promoted policy —5578bf5;trusted-unsigned-evidence-20261021.json;115 JVM tests each,zero lint errors,identical APKs.
- [ ] Task: Complete the retained production/independent/platform/licence-conveyance/operational gates before full M4 and archive.

- [x] Task: Apply licence/tool-trust and schema/provider review fixes — fee1f27; local acceptance only, original full-M4 gates retained.

## Isolated JDK continuation — 2026-10-22

- [x] Task: Qualify an isolated publisher-signed JDK17 archive without restart, global install changes or ad-hoc re-signing — isolated-jdk-evidence-20261022.json;459 archive files, strict resource/publisher verification and real tamper rejection pass.
- [x] Task: Rebuild frozen5578bf5 twice under the isolated JDK; verify unchanged installed tools and review evidence —115 JVM tests per build, zero lint errors, exact prior APK match;121 host tests. Review: review-isolated-jdk-20261022.md; implementation/review evidence95d3851.
- [ ] Task: Complete independent-host/platform, production identity/signing, rights/conveyance and live/operational gates before full M4 and archive.


## Asset/source/notices continuation — 2026-10-22

- [x] Task: Finish current asset/translation provenance; replace uncertain launcher/store artwork and preserve active translations —5a3a3e4; ASSET_PROVENANCE.md, MODIFICATIONS.md and qualification/release/assets-provenance.json.
- [x] Task: Prepare exact-source/licence/notices candidate with all63 published dependency source/POM archives and embedded APK notices —5a3a3e4; assets-source-evidence-20261022.json,643-member archive, four real CLI rejection cases and full readback/repeatability pass.
- [x] Task: Qualify two clean unsigned builds and a source-only archive reconstruction outside Git; review and apply corrections —115 JVM tests per build, zero lint errors,126 host tests; review-assets-source-20261022.md; evidence/review1384c58. Original production/native/external gates retained.
- [ ] Task: Complete signed/channel corresponding-source delivery, permanent identity/signing, independent/platform/native and live/operational acceptance before full M4/archive. Published source JARs are not proof of complete upstream dependency rebuilding.


## Independent Linux build continuation — 2026-10-22

- [x] Task: Discover safe separate environments and prepare a cold Linux runner/CI recipe —9464408; exact Git source, digest-pinned JDK image, fresh SDK/Gradle directories, no host cache/source mounts.
- [x] Task: Diagnose initial cold verification failure; canonically audit three metadata files and preserve all736 old entries —9464408;739 entries, no runtime version change or trust exception.
- [x] Task: Requalify two Mac builds and a separate native Linux physical host, including tests/lint/source/runtime/artifact readback — linux-reproduction-evidence-20261022.json; identical unsigned APK,115 JVM tests per build,131 Python tests on both systems; source/notices bundle refreshed.
- [x] Task: Review/apply fixes and prepare exact-candidate manual GitHub workflow — review-linux-reproduction-20261022.md; evidence/review 484a8dd; actionlint and Linux execution of build/tooling commands pass. Final reporting fixture corrected;131 host tests and160 Conductor documents pass.
- [ ] Task: Execute and read back hosted CI after the repository choice is authorised; source workflow preparation is not a provider run.
- [ ] Task: Finish original signing/identity, signed source conveyance, Android/platform/native, cloud and operational gates before M4/archive.


## Extended emulator acceptance — 2026-10-22

- [x] Task: Qualify exact dedicated API26 minimum and API33 intermediate images and source-bound builds; preserve existing guests — production6f3381c, emulator-matrix-evidence-20261022.json; one exact development APK across standard suites.
- [x] Task: Run native store/provider/worker, rendered settings/import/recovery, accessibility at normal/large text, lifecycle and simulated telephony plus development-key schema/provider upgrades —109 successful native invocations each on API26/API33,11 affected API36 checks, two expected terminations,134 screenshot/tree pairs;115 JVM/141 host tests.
- [x] Task: Review, fix and rerun affected checks — runtime fixes6f3381c, final fixture hardeningbd8c2e9, evidence/review fa7b944; protected-cache round-trip/worker regression and cleanup pass; API26 ordinary ADB privilege restored and both new guests stopped. Original external/manual gates retained.

- [x] Task: Refreeze the current runtime fixes into new unsigned artifacts/source-notices bundle and independently reproduce that exact candidate —15998ba; see final local candidate evidence below.9464408 retains its prior qualified release/source scope.
- [ ] Task: Complete authorised hosted execution and original identity/signing, physical/OEM/manual, remote and operational gates before M4/archive; repository authority remains pending.


## First-release baseline and final local candidate —2026-10-22

- [x] Task: Define fresh-install-first support, empty issued-predecessor set and immutable future upgrade baseline; preserve development identity and production custody gates —29c6211,15998ba.
- [x] Task: Freeze current minimum-platform/accessibility fixes into matching unsigned builds and refreshed source/notices; reproduce exact candidate on the separate Linux host —15998ba; first-release-candidate-evidence-20261022.json.
- [x] Task: Qualify the exact release payload using an existing-development-key copy on dedicated emulators; prove initial denied/unconfigured launch and historical schema/provider forward upgrade preservation —15998ba;60 successful invocations across26/33/36,6 expected rejections,3 PNG/tree pairs; cleanup passes.
- [x] Task: Review/fix tools and evidence, run host/Conductor checks and record remaining production/physical/cloud/operational gates without archiving M4 —a8c7f9f,15998ba; review-first-release-candidate-20261022.md;144 host tests on Mac/Linux; final document checkpoint recorded below.

The final local candidate supersedes the pending runtime-refreeze action recorded
under the extended emulator checkpoint:15998ba includes the fixes and is independently
reproduced. Older artifacts/probes retain their original scope; full M4 is still
open. First-release installation has no issued predecessor; production identity/
signing and original physical/cloud/operational/conveyance gates are not waived.

- [x] Task: Commit reviewed candidate evidence and final document checkpoint —76c5f64bd02365c3b14c03d823364d06943b6341;
  Mac/Linux144 host tests pass; current Conductor check165 documents/20 tracks/25
  scenarios passes. Artifact freeze remains15998ba; evidence commits do not relabel
  that APK/source bundle. Original M4 release gates remain pending.


## Physical calls and account-calendar continuation —2026-10-22

- [x] Task: Inspect current device visibility, retained candidate and native calendar contract; refresh official provider/Google/Outlook prerequisites — physical-calendar-qualification.md; no physical phone visible, candidate15998ba artifact/signature/payload rechecked.
- [x] Task: Prepare staged controlled-call, local/provider/remote readback, preservation and cleanup criteria; review preparation without treating it as executed acceptance — physical-calendar-qualification.md.
- [ ] Task: Select connected physical phone, participating controlled second phone and dedicated account calendars; preserve existing installations/accounts before testing.
- [ ] Task: Execute actual physical-call cases and independently read back each claimed Google/Outlook route; record failures/unsupported configurations without substituting destinations.
- [ ] Task: Verify cleanup/settings restoration, review actual runtime outcomes and apply/requalify any fixes before promoting route claims.

Physical preparation review: review-physical-preparation-20261022.md.144 host tests
and167-document/20-track/25-scenario Conductor checks pass. Repeated discovery
still exposes only emulators. Required device, participation and account-calendar
answers remain pending; no physical/remote acceptance or observation time is claimed.


## Seven-day operational observation —2026-10-22

- [x] Task: Recheck physical prerequisites and prepare private cohort/day journal with unknown outcome counts and no start date; seven-day-observation.md. Only emulators visible; no elapsed observation claimed.
- [x] Task: Configure daily thread follow-up with prerequisite checks and evidence-only accounting; never equate runs/time with accepted use —calltrack-seven-day-observation, active daily09:00 local; native create/view and saved configuration verified.
- [ ] Task: Complete selected physical/remote baselines, confirm observation scope and participant readiness, verify actual installation and start an explicit cohort/window.
- [ ] Task: Observe at least168 real elapsed hours with seven accounted daily intervals; preserve denominators, delays, conditions, gaps and findings without fabricated zeros.
- [ ] Task: Perform final outcome review, apply findings and repeat affected acceptance/new observation where required; retain independent release gates.

Observation preparation review: review-observation-preparation-20261022.md;144
host tests and169-document/20-track/25-scenario Conductor checks pass. Readback
confirms one active daily09:00 AEST heartbeat, null observation dates, seven pending
intervals and zero accepted days/hours. Final runtime review is not yet performed.
