# Error patterns

- Do not share a database between services.
- Do not put Google API keys in source control.
- Do not return JPA entities from controllers.
- Do not use 2PC; compensate in sagas.
- Do not block coroutines with JDBC on the request thread without a proper pool (JPA is blocking; keep transactions short).
- Saga compensation must not return the operation to a retryable status if the compensating call failed. Inventory movements need a step key; a second RESERVE for the same step must be a no-op. Undo steps (`UNDO-RESERVE`, `UNDO-CONSUME`, `RESTORE-RESERVE`) no-op until the forward step is stored (fixed 2026-10-06).
- Do not hold a database transaction open across HTTP (farm membership, file bytes).
- Farm scope is the JWT `farmIds`. An in-process grant map on one JVM does not apply to the other services.
- Boot seed uses the same gate as `POST /api/v1/dev/seed/reset`.
- Web idle logout clears the TanStack Query cache together with the session.
- Mobile must not drop the refresh token inside the 401 validator before a refresh attempt. `Idempotency-Key` is unused by the backend; replay safety is the 409 path plus single-flight enqueue.
