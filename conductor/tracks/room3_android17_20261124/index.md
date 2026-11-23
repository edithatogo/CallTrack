# Track 22 — Room 3 preserving migration

[Active architecture](../../architecture-android17.md) · [Scope](../../active-scope.json) · [Plan](plan.md)

Room **3.0.3 stable** is the required native persistence target, not yet the deployed writer. The existing SQLite schema and source-bound contract suite remain authoritative until an independently verified migration replaces them. Use a short incremental PR and no destructive fallback.
