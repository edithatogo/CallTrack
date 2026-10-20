# Review Report: Android Compose discovery and migration

## Summary

The isolated experiment supports a measured retain-Views/defer-adoption decision, with verification fixes applied and production UI acceptance explicitly separate.

## Verification Checks

- [x] **Plan Compliance**: Yes for conditional research/decision; production implementation is not applicable under the justified deferral, not reported as migrated.
- [x] **Style Compliance**: Pass - general/Kotlin guides, ktfmt, source/link checks; no working-app behaviour change.
- [x] **New Tests**: Yes -3 policy contracts,5 native UI cases and5 host safety regressions; documentation fixture fixed.
- [x] **Test Coverage**: Partial by design - synthetic controls/state, recreation, large font/RTL, automated accessibility and foreground-window guard; no real provider/permission or manual accessibility acceptance.
- [ ] **Test Results**: Failed native gate -3 JVM/25 host and packaging/lint pass; final5/5 native cases fail on the blocking Android system window. Earlier semantics success is rejected as visual proof. Production adoption is deferred; see verification/evidence.

## Findings

### High — Green semantics checks did not prove unobstructed UI (fixed)

- **File**: `prototypes/compose/src/androidTest/kotlin/org/calltrack/composeprototype/WindowAssertions.kt` (Lines L6–L15)
- **Context**: Screenshot inspection found a system ANR dialog over the prototype after all five semantics tests had passed. Accessibility automation alone missed the obstruction.
- **Suggestion**: Applied; the guarded rerun correctly failed while the Android system dialog remained:
```diff
-Use semantics success alone as screen acceptance.
+Require the prototype package to own the active accessible window.
+Inspect the collected image and retain the environment failure without suppressing it.
```

### Medium — Screenshot used the wrong private-data owner (fixed)

- **File**: `prototypes/compose/src/androidTest/kotlin/org/calltrack/composeprototype/PrototypeScreenTest.kt` (Lines L35–L43)
- **Context**: Instrumentation executes in the target process; the test package's private directory was unavailable there. First native run failed its capture rather than verifying an image.
- **Suggestion**: Applied together with matching run-as collection in the harness:
```diff
-File(instrumentation.context.filesDir, "compose-prototype.png")
+File(instrumentation.targetContext.filesDir, "compose-prototype.png")
```

### Medium — Prototype permission isolation needed a regression gate (fixed)

- **File**: `scripts/qualify_compose_prototype.py` (Lines L84–L98)
- **Context**: Source manifest intent alone would not catch a later dependency/manifest adding call, calendar, contacts, microphone or network permissions to this no-data experiment.
- **Suggestion**: Applied with an early-denial host regression:
```diff
-Check package identity only.
+Reject declared forbidden permissions before installing the target APK.
```

### Low — Documentation fixture omitted the new source tree (fixed)

- **File**: `tests/test_verify_conductor.py` (Lines L29–L31)
- **Context**: The good-link test failed after adding a genuine link to prototype instructions; its isolated copy omitted that directory.
- **Suggestion**: Applied without weakening link validation:
```diff
+Copy prototype sources into the fixture, excluding build/.gradle/.kotlin outputs.
```

Review iterated over the727-line implementation outside Conductor, including both
screen/state implementations, native tests, manifests/Gradle, host safety fixtures,
source/target/cleanup harness and CI. Real OS permission state is never delegated
to the prototype's simulated access flag. The root app retains all74 qualified
input hashes; no outbox/provider/worker behaviour or runtime dependency was changed.

The environment gate remains unresolved after the bounded recovery attempt. The
recommendation is to retain Views and defer production adoption, not ignore the
failing suite. Research/decision closure is explicitly permitted by the original
spec/workflow; native acceptance and production M6 remain open on reopening.
