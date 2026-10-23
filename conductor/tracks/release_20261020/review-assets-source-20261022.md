# Review Report: Track5 asset provenance and source/notices preparation

## Summary

The uncertain artwork is replaced and the exact-source/licence/notices candidate
passes local packaging/build checks; Track5 retains its external release gates.

## Verification Checks

- [x] **Plan Compliance**: Partial for full Track5; the authorised local provenance and bundle work is complete within the explicit published-source scope.
- [x] **Style Compliance**: Pass; original legal texts retain their audited bytes, with authored code/document checks separate.
- [x] **New Tests**: Yes; five packaging boundary regressions and four actual CLI rejection cases.
- [x] **Test Coverage**: Bounded; source/APK/runtime/notice correspondence, archive readback, source-only build, cache/path/private-input guards. No final signing, native acceptance or upstream dependency rebuild claim.
- [x] **Test Results**: Passed;126 host tests, two clean115-test JVM builds, zero lint errors/fatal findings and identical unsigned APKs. Final source-only outcome is retained in the linked evidence.

## Review corrections applied

- Retain project-authored active German translations; remove only44 unused
  inherited endpoint keys per locale and replace the inherited launcher label.
- Derive packaged legal documents from the committed Git snapshot, avoiding
  arbitrary working-tree glob inputs or ignored/private files.
- Resolve/check source-cache paths before creating directories; reject external
  links, unsafe/private archive names, changed checksums and incomplete coverage.
- Require exact candidate revision and actual unsigned APK hashes; compare every
  committed legal payload with both APKs. Real missing-notice rejection proves
  that a successful build alone cannot qualify a notices bundle.
- Preserve original licence/copyright text including whitespace and all source
  headers. Keep wrapper/build-tool terms separate from app/runtime attribution.
- Distinguish published source material and a successful app reconstruction from
  independent-host or full upstream dependency source reproduction. Preserve final
  channel/source access, signed-artifact and permanent trademark gates.

No unresolved implementation defect within the authorised local preparation.
Source-candidate evidence remains bound to5a3a3e4; later evidence-only commits do
not relabel its APK or the previous native matrix. No track archive is warranted.
