# Kotlin source conventions

Follow [official Kotlin conventions](https://kotlinlang.org/docs/coding-conventions.html)
and ktfmt0.59 `--kotlinlang-style` for formatting. Use explicit nullability and small
platform boundaries. Preserve existing Java contracts during incremental conversion;
JVM annotations and legacy nullable fields are intentional where verified by tests.
Do not replace stable persisted identities with data-class/hashCode defaults.

Formatter is a build tool only, not an app dependency. Its pinned Maven URL/SHA256
and non-mutating check are in the CI workflow. Kotlin compile errors, existing Java
interop contracts and Android lint complement formatting; no Compose/KMP/coroutine
architecture is implied by Kotlin syntax. Reassess the formatter if new syntax is
not supported; do not silently skip files or suppress formatter errors.
