# Kotlin source conventions

Follow [official Kotlin conventions](https://kotlinlang.org/docs/coding-conventions.html)
and the version-catalogued ktfmt `--kotlinlang-style` for formatting. Use explicit nullability and small
platform boundaries. Preserve existing Java contracts during incremental conversion;
JVM annotations and legacy nullable fields are intentional where verified by tests.
Do not replace stable persisted identities with data-class/hashCode defaults.

The build resolves the formatter through Gradle dependency verification.
The required Compose CI job runs its non-mutating check. Kotlin compile errors, existing Java
interop contracts and Android lint complement formatting; no Compose/KMP/coroutine
architecture is implied by Kotlin syntax. Reassess the formatter if new syntax is
not supported; do not silently skip files or suppress formatter errors.
