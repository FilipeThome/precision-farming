# Known limitations

- Docker Desktop may need a manual start before `docker compose up`.
- Telemetry seed uses 7 days at 15-minute intervals (not 30 days / 5 minutes) to keep startup fast.
- Reporting CSV is a demo export; it does not yet join live operation/inventory services.
- RabbitMQ, Redis and MinIO are in Compose and on the classpath (AMQP listeners off until outbox consumers exist). Sagas still call inventory over HTTP with a cached service JWT.
- Mobile Windows slice is `composeApp`; the approved KMP split is `shared` + `androidApp` + `iosApp`.
- Google Maps keys stay env-only and the Maps SDK is not in this slice. The mobile map is an interim Leaflet WebView with the same Esri imagery as the web map, and only when a field geometry yields a centroid.
- Virtual threads do not make JDBC non-blocking; Hikari stays small so the database is not flooded.
- Full-spec LOCAL seed is 8 farms / 22 fields, not the 180-field catalog in §54.
- Existing PostGIS volumes need `scripts/ensure-new-dbs.sql` (or volume recreate) before agronomy/irrigation/harvest/finance/compliance boot.
- Android compile needs local SDK/`ANDROID_HOME`; CI/dev machines without it skip `:composeApp:compileDebugKotlin`.
- Kafka is publish-only (`KAFKA_ENABLED=true`) on `precision.operation.started`. It is not the inventory saga bus.
- ISOXML export is a minimal TASKDATA document, not an OEM file parser. NF-e is homologation XML (`tpAmb=2`), not a SEFAZ submission. Live MAPA ZARC is off unless `MAPA_LIVE=true`.
- Deferred: workflow engine, Copilot/Agents, GeoTIFF, OIDC, Chaos lab UI.
