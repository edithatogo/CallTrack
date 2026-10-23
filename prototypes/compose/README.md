# Isolated Compose experiment

Synthetic UI only. Not included in the root app build, not a replacement launcher,
and never connected to calls, calendars, accounts, permissions or the ledger.
Open only through instrumentation. Production adoption requires the recorded
behaviour/accessibility gates and a separate integration change.

Use the repository Gradle8.13 wrapper/JDK17/SDK36. Build and source-bound emulator
qualification are documented by `.venv/bin/python scripts/qualify_compose_prototype.py --help`
from the repository root. No public release or signing channel is provided.

Dependencies are prototype-only and pinned in this project's build.gradle. They
must not be copied wholesale into the root app. Revisit after stabilized M3/M5
state/controller contracts; remove this standalone directory and optional test
harness/CI job if the experiment is rejected or replaced.
