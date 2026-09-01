# Known limitations

# Known limitations

- Docker Desktop may need a manual start before `docker compose up`.
- Telemetry seed uses 7 days at 15-minute intervals (not 30 days / 5 minutes) to keep startup fast.
- Reporting CSV is a demo export; it does not yet join live operation/inventory services.
- RabbitMQ, Redis and MinIO are in Compose and on the classpath (AMQP listeners off until outbox consumers exist). Sagas still call inventory over HTTP with a cached service JWT.
- Mobile Windows slice is `composeApp`; the approved KMP split is `shared` + `androidApp` + `iosApp`.
- Google Maps keys are env-only. Without them the map page shows status, not a fake satellite image.
- Virtual threads do not make JDBC non-blocking; Hikari stays small so the database is not flooded.
- Full-spec LOCAL seed is lean (~5 farms / ~16 fields), not the 180-field catalog in §54.
- Existing PostGIS volumes need `scripts/ensure-new-dbs.sql` (or volume recreate) before agronomy/irrigation/harvest/finance/compliance boot.
- Android compile needs local SDK/`ANDROID_HOME`; CI/dev machines without it skip `:composeApp:compileDebugKotlin`.
- Deferred: Kafka, workflow engine, Copilot/Agents, GeoTIFF/ISOXML, OIDC, Chaos lab UI.
