# Error patterns

- Do not share a database between services.
- Do not put Google API keys in source control.
- Do not return JPA entities from controllers.
- Do not use 2PC; compensate in sagas.
- Do not block coroutines with JDBC on the request thread without a proper pool (JPA is blocking; keep transactions short).
