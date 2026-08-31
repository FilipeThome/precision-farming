# Architecture approvals (MVP)

Recorded after parallel architect review. Implementation may proceed.

| Area | Agent | Decision |
| --- | --- | --- |
| Backend microservices | [kotlin-architect](9d7336f8-d1c4-49d2-85cb-60c95e9cef61) | APPROVED — gateway-only clients, database-per-service, sagas in operation-service, no 2PC |
| Web | [react-architect](10b0af38-63fb-48ed-9253-323379f20e3b) | APPROVED — Query for server state, Zustand for session/UI, SATELLITE default |
| Web scaffold | [react-engineer](51c6a8e2-9a5c-425f-a44b-dc58d5958edf) | Delivered `web/` (build + Vitest green) |
| Local infra | [infra-architect](08fd4415-19a9-496a-ad62-354020106a30) | APPROVED — Compose on network `precision-farming`, host 8080 reserved for gateway |
| Mobile | [kmp-architect](91d4d619-25da-4b0f-8cdc-1ba7d5e9e33e) | APPROVED — `shared` + thin `androidApp`/`iosApp`, SQLDelight queue, iOS gated on macOS |

## Adopted now

- Compose runs the full local stack: PostGIS Alpine `:5432` (transactional DBs), Timescale `:5433` (`telemetry_db`), RabbitMQ management-alpine, Redis, MinIO. Network `precision-farming`. Host 8080 stays reserved for the gateway.
- PostGIS is enabled on `farm_db`.
- Servlet services use Java 21 virtual threads (`spring.threads.virtual.enabled=true`) with a bounded Hikari pool (10). Gateway stays on Netty and does not enable servlet VTs.
- AMQP is on the classpath with listener `auto-startup: false` so the broker is ready without blocking boot when unused.
- Web JWT stays in memory. Maps SATELLITE; missing key shows status, not a fake image.

## Deferred (next slices)

- Full transactional outbox + Rabbit command bus as the primary saga transport (HTTP reserve/consume remains the working path).
- Spring Boot 4.1.1 (ADR-003 keeps 3.5.x until Cloud/Springdoc 4.x is wired).
- KMP module split `shared` / `androidApp` / `iosApp` + SQLDelight (current Android `composeApp` is the Windows slice).
- Terraform apply — `infra/` stays a skeleton.
