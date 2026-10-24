# Review: current unsigned candidate and KMP release tooling

## Scope

Reviewed the updated external-runtime inventory, canonical artifact/POM audit,
published-source manifest, metadata-only source handling, Linux test baseline,
local same-host candidate builds, source/notices archive readback, hosted Linux
reproduction, and Track5 gate statements.

## Findings and fixes

- The release runtime inventory failed on the KMP `shared-core` project's
  multiple Android variants. The Gradle inventory now selects external Maven
  components only; a local release-classpath resolution passed and yielded the
  92-artifact inventory used by the audit.
- Five published source archives are metadata-only. The bundle validator now
  accepts them only when the manifest records no Java/Kotlin source, verifies
  the archive contents and entry count, and still rejects unsafe paths, corrupt
  archives and missing source where the manifest expects source code.
- The cold-Linux runner's fixed 115-test expectation was stale. The current
  candidate reports 119; the exact count is required with zero failures,
  errors and skips. A focused JUnit aggregation test was added.
- Two clean same-host builds and the hosted cold-Linux run match the candidate
  APK. The source/notices archive is repeatable and its complete manifest was
  read back.

No remaining defect was found in this bounded qualification slice. The APK is
unsigned and `release_ready=false`. Repository byte agreement and POM licence
declarations do not establish legal clearance. Hosted CI metadata does not
establish separate native physical-host acceptance. Production signing,
physical calls, selected calendar routes and operational observation remain
independent gates.

## Verification

- Python 3.15.1: `test_release_bundle.py` (6 tests),
  `test_release_toolchain.py` (8 tests), `test_linux_reproduction.py` (6 tests).
- `scripts/verify_conductor.py`: passed after the evidence update.
- Two local builds at candidate `ae39ef9`: each 119 JVM tests, zero failures,
  errors or skips; zero lint Error/Fatal findings; identical unsigned APKs.
- Hosted cold-Linux run 40081001647: 119 JVM tests, zero failures/errors/skips,
  zero lint Error/Fatal findings, exact unsigned APK hash match.
- Runtime artifact audit: 92/92 canonical artifact byte matches and 92/92
  effective POM licence declarations recorded.
- Source/notices archive: 877 files; checksum/member/readback and repeatability
  checks passed.
- Final PR checks and merge are tracked separately from local qualification.
