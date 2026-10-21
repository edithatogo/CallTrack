# Kotlin migration decisions

Date/evidence refresh: 2026-10-20. R14/R26. Owning track kotlin_20261020.

## K01 — Bounded mixed Java/Kotlin migration

- Problem: introduce Kotlin without changing stored identity, event rendering or
  Java callers while M3 synchronization still evolves and M4 release is open.
- Existing owned code: CallSelection, CallIdentity and CallEvent are pure Java;
  CallRecord, orchestration, SQLite and Android adapters remain coupled to current
  contracts. No dependency can replace our product mapping/identity policy safely.
- Options: retain all Java; mechanically migrate pure components; simultaneously
  replace persistence with Room/coroutines; adopt Kotlin2.4 with new R8/toolchain;
  defer everything. Select a bounded mixed-language migration; defer wider changes.
- Compatibility: Kotlin/KGP2.3.21 (Apache2.0) with JDK17, Gradle8.13, AGP8.13.2,
  compile/target36 and min26. Both bytecode targets remain1.8. Kotlin2.4.21 is newer,
  but its R8 requirement differs from the qualified bundled R8. No AGP upgrade.
- Primary evidence: [KGP matrix](https://kotlinlang.org/docs/gradle-configure-project.html),
  [Android Kotlin/R8 matrix](https://developer.android.com/build/kotlin-support),
  [Kotlin releases](https://kotlinlang.org/docs/releases.html).
- Role: development candidate, promoted only after Java contract tests, debug/test
  packaging, lint, native store regression and dependency readback. No public release.
- Replacement: three pure Java implementations only. No persistence, scheduling,
  network, UI, account consent, schema or version migration. Preserve Java static
  methods/public fields with JVM annotations and legacy nullable constructor fields.
- Costs/transitives: adds pinned compiler/plugin and upgrades the Kotlin runtime
  already transitively present. Existing coroutines remain inherited, not adopted
  for app orchestration. Recheck graph/duplicate classes; report actual selections.
- Prototype hypothesis: exact golden hashes/rendering, filters, Java field/static
  calls and legacy null/whitespace/overflow behaviour remain identical. Establish
  golden contracts on Java before translation. Do not confuse green compile with
  stored-data compatibility.
- Alternatives/takeover: Room could replace owned SQL, DataStore preferences,
  coroutines executor/state delivery, serialization Gson. Their migrations change
  semantics beyond this slice; Track4 owns store/scheduler decisions, Kotlin and
  destinations tracks own later serialization. No KSP/kapt/coroutines/UI plugin.
- Error/security/performance: retain exact arithmetic, UTF16 length prefixes and
  UTF8 hashing; no new telemetry or personal fixtures. No speed improvement claimed.
- Decision: adopt only if bounded qualification passes; otherwise restore Java from
  f163e4c and remove plugin/runtime changes before commit. Preserve tests/evidence.
- Expiry: reassess before M3/M4 promotion or a further Kotlin conversion. Kotlin2.4's
  documented support window is a reason to revisit toolchain compatibility, not to
  assume the older line has the same support guarantee. No permanent version freeze.
- Rollback: code/build reversal before distribution; unchanged schema2 and source
  signature version. Do not downgrade an installed app or erase data as rollback.
- Outstanding: M4 prerequisite and full migration acceptance remain open. Pure-code
  pilot cannot archive the whole migration track or waive remaining Java work.

## K02 — Kotlin style tool

Pin ktfmt0.59 (Apache2.0) with `--kotlinlang-style`, downloaded only under excluded
`.local/toolchain`. Compare manual formatting, IDE-only formatting and adding a
Gradle formatter plugin: choose the standalone tool consistent with the existing
Java formatter. It owns formatting only; compiler/lint/tests own semantic checks.
Maven SHA256 a493271c137074436a133d72e9e323f91604c97e0c40555629ec39598efead54
was checked against the published checksum before use. The initial guessed
`jar-with-dependencies` URL returned404; the published `with-dependencies` artifact
was located and used. No app/ABI/persistence effect. Promote to contributor/CI check
after local formatting and dry-run pass; reevaluate if later Kotlin syntax exceeds
parser support, remove the tool/check if replaced, preserve source-style parity.
The bundled tool transitives stay outside APK/runtime dependency graphs.

## Qualification outcome (2026-10-21)

K01/K02 promotion criteria passed for the bounded development slice:36 JVM/20 host
and five native methods, source-bound packaging, unsigned release and dependency
readback. Retain Kotlin2.3.21 and ktfmt0.59 for these three components. This adopts
the bounded development implementation, not all suggested libraries or full M5.
See [verification](verification.md); compiler and app/runtime graphs remain distinct.

## K03 — Expand stabilized model/policy migration (2026-10-21)

The user approved the iOS-last implementation sequence and all active-code Kotlin
migration. M3 selected-emulator contracts are stable; M4 signing/operational gates
remain. Convert CallRecord, DeliveryCounts, ImportRange, PayloadRetention, WorkStatus,
AutomaticWorkPolicy and ActivationCommitGuard without changing persistence or
controllers. Seven components join the existing three Kotlin models.

Retain the qualified compiler/formatter and native APIs. No runtime/build dependency
is added or updated. Libraries cannot replace these product-specific validation,
state and latch policies without a semantic change. Room/DataStore/coroutines
remain deferred; combining takeover with language conversion is unjustified.

Java fields/static entry points/overloads and nullable inputs remain explicit.
CallRecord validates null IDs before asserting nonnull stored fields; raw nullable
presentation is distinct from legacy absent presentation. WorkStatus preserves
stale-generation early return and saturating failure counts. ActivationCommitGuard
retains JVM synchronized methods and fail-closed latching before a throwing write.
Its Kotlin internal class is public in bytecode (Kotlin has no Java package-private
type visibility); methods retain unmangled names for existing package Java callers.
This intentional visibility widening is not an exported Android component or a
new external SDK contract, and does not bypass any operation/permission authority.

The retained-class javac fallback is retired: once models move, compiling only
remaining Java against old implementation classes cannot qualify Kotlin changes.
Use Gradle mixed-source tests; preserve historical receipts without relabelling.

Acceptance: freeze Java golden/interop contracts first, then the same JVM suite,
full debug/test/release packaging/lint, native store regressions and actual
same-version development package replacement of private synthetic state. This
last fixture is separately named/bounded and cannot satisfy production-version,
provider/cloud or operational gates. Roll back code only before promotion if a
contract fails; preserve data/receipts and never clear app data or downgrade a
user installation. Further26 active Java/14 inherited files remain work, not
permanent justified exceptions. See current evidence when recorded.

## Active-code continuation — 2026-10-21

The approved stage1 converted33 active Java classes in preserving batches,
retaining existing Views/executors/SQLite/WorkManager and golden identities.
There are36 production Kotlin files and one deliberate Java exception:
NullableCalendarLabel preserves the old nullable Choice.toString ABI, which
Kotlin Any.toString cannot represent. Remove only with an explicit consumer
contract change and parity evidence. Package-private helpers widen to public JVM
visibility for unmangled Java interop; no component export or external SDK is added.

Reachability audit retired14 inherited Java files,12 resources and seven unused
direct dependencies. Material/AppCompat still consume ConstraintLayout, RecyclerView
and Transition; review found their removal accidentally lowered selected versions.
Gradle constraints retain qualified2.1.3/1.2.1/1.4.1 respectively without restoring
unused API declarations. Reassess/remove constraints when those consumers retire.
This is dependency preservation, not an ecosystem upgrade or licence clearance.

The retained-javac fallback is retired; source-bound Kotlin APKs are mandatory.
The private-state same-version debug replacement proves only selected private data
preservation. Production identity/signing, increasing-version upgrades, cloud and
physical/operational acceptance retain separate release gates.
