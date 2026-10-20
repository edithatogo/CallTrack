# Refined UI requirements

R15/R26,2026-10-21. C01–C08 distinguish research closure from adopted UI.

| ID | Current need / proposed behaviour | Acceptance and evidence boundary |
|---|---|---|
| C01 | No call effect on launch, permission or destination selection | Synthetic request counter stays zero through those actions; actual source/provider flow required before adoption |
| C02 | Explicit stable destination and no fallback | Reorder preserves ID; loss clears selection; placeholder disables action |
| C03 | Busy/refresh/permission gate | Pure policy and rendered enabled state agree; real controller owns revocation/generation |
| C04 | State restoration without replay | Selection/counter survive Activity recreation without another request; process-death/real operation reconstruction remains adoption gate |
| C05 | Accessible selection/status/actions | Radio semantics, heading/live region, min48dp rows, scroll at2x font/RTL and automated accessibility; manual service acceptance separately required |
| C06 | No working-app regression | Standalone Gradle app with separate ID/no sensitive permissions, no root inclusion/dependencies; original app input manifest remains unchanged |
| C07 | Dependency lifecycle / alternatives | Pinned matrix, native/current/library/deferral comparison, resolved graph, prototype expiry/promotion/removal and rollback |
| C08 | Honest final decision | Adopt only after real behavioural/accessibility parity; otherwise retain Views with measured reasons and reopening triggers |

Scope is not automatic logging, historical import, permission architecture rewrite,
Compose Multiplatform or a new generic state framework. Production implementation
may be marked not applicable with a justified deferral per the existing workflow;
a successful experiment alone cannot mark real screen migration complete.
