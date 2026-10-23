#!/usr/bin/env -S uv run python
"""Retired Java-only retained-class fallback; use qualified mixed Kotlin/Java Gradle tests."""
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]


def run():
    raise RuntimeError(
        'Retired retained-class JVM fallback: expanded Kotlin migration cannot be verified '
        'by compiling only remaining Java sources. Use scripts/run_slice_build.py with '
        'the qualified JDK17/Android SDK; reconnect the qualified toolchain if unavailable.'
    )


if __name__ == '__main__':
    run()
