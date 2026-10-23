# Review Report: Kotlin migration and dependency reassessment

## Summary

The three-component Kotlin pilot preserves tested Java/data contracts with review fixes applied, while full migration and M4 prerequisite acceptance remain incomplete.

## Verification Checks

- [ ] **Plan Compliance**: Partial - discovery, bounded conversion and qualification delivered;21 Java files and full K06/K07 remain open.
- [x] **Style Compliance**: Pass - general/Kotlin guides, AOSP Java formatter and ktfmt checks pass.
- [x] **New Tests**: Yes - five Java migration contracts, one native receipt/upgrade scenario and one host stale-Kotlin-receipt guard.
- [x] **Test Coverage**: Partial - fingerprints/ABI/null/trim/overflow, existing delivery/filter tests and native SQLite fixtures; no production package upgrade or real calls/cloud scope.
- [x] **Test Results**: Passed -36 JVM,20 host and five native methods; debug/test and unsigned release APK packaging; lint zero errors,30 warnings; source and cleanup readback match.

## Findings

### Medium — Stored receipt compatibility needed native evidence (fixed)

- **File**: `app/src/androidTest/java/com/markobl/calllogsync/slice/ParityStoreTest.java` (Lines L140–L210)
- **Context**: JVM hashes and generic schema migration alone did not demonstrate the current Kotlin mapper recognizing a preserved Java-era receipt through native SQLite. A bad conversion could prompt duplicate insert/review.
- **Suggestion**: Applied in7cdd8d4, then verified on API36:
```diff
-Only generic receipt migration and JVM golden mapping evidence.
+Seed schema1 with the Java golden fingerprint; upgrade/reopen the native store.
+Assert ALREADY_LOGGED without insertion; assert human edit gives REVIEW_REQUIRED.
```

### Medium — CI exercised only the debug variant (fixed)

- **File**: `.github/workflows/qualification.yml` (Lines L78–L79)
- **Context**: Introducing a compiler affects both variants. Local release compilation needs a continuing CI gate; unsigned packaging must not be mistaken for production signing.
- **Suggestion**: Applied and matching local Gradle task passed:
```diff
+      - name: Compile and package unsigned release variant
+        run: sh gradlew --no-daemon --console=plain :app:assembleRelease
```

### Low — A single Kotlin directory glob would miss later source roots (fixed)

- **File**: `.github/workflows/qualification.yml` (Line L75)
- **Context**: Test or new-package Kotlin source could bypass the style check during later incremental conversion.
- **Suggestion**: Applied in7cdd8d4; tested locally with pinned formatter:
```diff
-java -jar ktfmt.jar ... app/src/main/kotlin/com/markobl/calllogsync/slice/*.kt
+rg --files app/src -g '*.kt' -0 | xargs -0 java -jar ktfmt.jar ...
```

No remaining correctness defect was found in the bounded conversion after checking
all three Kotlin implementations against their Java predecessors, Java callers,
JVM targets, dependency graphs, nullable constructor fields, UTF16/UTF8 identity,
ASCII trim, overflow and exception categories. Review used per-file iteration for
the385-insertion/162-deletion implementation diff outside Conductor. No concurrent
or persistence responsibilities were rewritten. Full track completion and archive
remain disallowed by the unmet original acceptance, rather than a waived warning.
