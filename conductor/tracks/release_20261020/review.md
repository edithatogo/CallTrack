# Review Report: Community distribution and release readiness

## Summary

Community preparation is reviewed with fixes; candidate/operational acceptance is
still incomplete, so Track5 remains active and unarchived.

## Verification Checks

- [ ] **Plan Compliance**: Partial - discovery/options and preparation delivered; original candidate/operational gates remain open.
- [x] **Style Compliance**: Pass - existing plain Markdown/workflow; no product branding or app behaviour change.
- [ ] **New Tests**: No - existing documentation fault-injection fixture corrected; documentation uses source/link checks.
- [x] **Test Coverage**: Partial - tooling/domain coverage only; signing/upgrade runtime evidence absent.
- [x] **Test Results**: Passed - 19 host tests, 31 JVM tests, app/test APK builds and lint; zero lint errors, 30 warnings. Conductor/private-input and root/community Markdown links pass.

## Findings

### Medium — README described the pre-migration ledger (fixed)

- **File**: `README.md` (Lines L66–L68)
- **Context**: Schema1 wording misdescribed the preserving schema2 upgrade and unwired outbox.
- **Suggestion**: Applied:
```diff
-Ledger schema1 is preserving: unsupported upgrades fail instead of dropping data.
+Ledger schema2 preserves schema1 receipts and adds a durable outbox foundation;
+unsupported upgrades fail instead of dropping data. The manual launcher still uses
+the original receipt path; automatic delivery/import integration is unfinished.
```

### Medium — Direct pins omitted transitive Kotlin runtime (fixed)

- **File**: `conductor/tracks/release_20261020/findings.md` (Lines L81–L87)
- **Context**: Java source does not imply Kotlin-free binaries. Direct inventory alone cannot support release provenance.
- **Suggestion**: Applied native graph extraction and explicit limits:
```diff
-Direct dependency inventory alone.
+Record requested/selected releaseRuntimeClasspath modules and Kotlin/coroutines transitives.
+Retain per-artifact licence/SBOM gates; graph resolution is not licence clearance.
```

### Medium — Documentation test omitted new root files (fixed)

- **File**: `tests/test_verify_conductor.py` (Lines L26–L28)
- **Context**: Existing good-document test failed because its isolated fixture lacked newly linked files. Checks must not be weakened to hide missing fixture inputs.
- **Suggestion**: Applied; final 19-test suite passes:
```diff
-for name in ('README.md', 'NOTICE.md', 'LICENSE'):
-    source = MODULE.parent.parent / name
+for source in [*MODULE.parent.parent.glob('*.md'), MODULE.parent.parent / 'LICENSE']:
     if source.exists():
-        shutil.copy2(source, self.root / name)
+        shutil.copy2(source, self.root / source.name)
```

### High — Preparation cannot satisfy candidate acceptance (gate retained)

- **File**: `conductor/tracks/release_20261020/plan.md` (Phase3 and handoff)
- **Context**: Track4 and production/operational evidence are missing. Archiving as completed would misstate milestone acceptance. This is a release gate, not a new app-code regression.
- **Suggestion**: Applied without weakening original acceptance:
```diff
-Close/archive Track5 on preparation documents alone.
+Keep candidate/upgrade/operational tasks open and Track5 active.
+Resume D01–D09 after M3; physical-call acceptance remains pending.
```
