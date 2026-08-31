# Known limitations

- Docker Desktop was not running during the first implementation pass; `docker compose up` is required before bootRun.
- Telemetry seed uses 7 days at 15-minute intervals (not 30 days / 5 minutes) to keep startup fast.
- Reporting CSV is a demo export; it does not yet join live operation/inventory services.
- RabbitMQ, Redis and MinIO are in Compose and on the classpath (AMQP listeners off until outbox consumers exist). Sagas still call inventory over HTTP with a cached service JWT.
- Mobile Windows slice is `composeApp`; the approved KMP split is `shared` + `androidApp` + `iosApp`.
- Google Maps keys are env-only. Without them the map page shows status, not a fake satellite image.
- Virtual threads do not make JDBC non-blocking; Hikari stays small so the database is not flooded.
