# Precision Farming MVP

[Português (Brasil)](README.pt-BR.md)

Executable precision-agriculture prototype: **Kotlin microservices**, a **React web** app, and a **KMP/Compose mobile** app. Clients talk only to the gateway at `http://localhost:8080`.

**Author:** Filipe de Brito Thomé

## Stack

- Backend: Spring Boot 4.1, Java 26 (virtual threads), Kotlin 2.4, Spring Cloud Gateway
- Data: PostGIS `:5432`, TimescaleDB `:5433`, RabbitMQ, Redis, MinIO (Compose; ports bound to `127.0.0.1` — production must not publish them)
- Web: React + TypeScript + Vite + Tailwind
- Mobile: Kotlin Multiplatform / Compose (Android on this Windows machine; iOS requires macOS). Gradle/JDK 26, Android jvmTarget 26, compileSdk 37
- Auth: JWT + refresh. Demo password for every persona: `Precision@123`
- Demo secrets: `.env.example` sets `ALLOW_DEMO_SECRETS=true`; without that (and without the `local` profile) boot refuses the demo JWT/DB secrets
- Web and mobile UI: pt-BR and en-US, with an in-app language toggle. Default locale is pt-BR. Seeded alert copy and API error messages are single-language.

Prerequisites: JDK 26, Docker Compose v2, and Node 22 for the web app. On Windows use `gradlew.bat`. Android: [mobile/README.md](mobile/README.md). Terraform under `infra/` is a skeleton and must not be applied.

Architecture approvals: [docs/architecture-approvals.md](docs/architecture-approvals.md).

## Demo personas

| Email | Role |
| --- | --- |
| `admin@precisionfarming.demo` | Admin |
| `manager@precisionfarming.demo` | Manager (approves prescriptions) |
| `operator@precisionfarming.demo` | Operator (runs the order, including offline) |
| `maintenance@precisionfarming.demo` | Maintenance |

## Run locally (Gradle / hybrid)

```bash
cp .env.example .env
docker compose up -d
./gradlew test
# bootRun defaults: APP_SEED=true and Spring profile `local` (demo seed + demo secrets).
# separate terminals:
./gradlew :backend:services:auth:bootRun
./gradlew :backend:services:farm:bootRun
./gradlew :backend:services:asset:bootRun
./gradlew :backend:services:telemetry:bootRun
./gradlew :backend:services:weather:bootRun
./gradlew :backend:services:operation:bootRun
./gradlew :backend:services:inventory:bootRun
./gradlew :backend:services:alert:bootRun
./gradlew :backend:services:ai:bootRun
./gradlew :backend:services:notification:bootRun
./gradlew :backend:services:file:bootRun
./gradlew :backend:services:reporting:bootRun
./gradlew :backend:services:sync:bootRun
./gradlew :backend:services:integration:bootRun
./gradlew :backend:services:agronomy:bootRun
./gradlew :backend:services:irrigation:bootRun
./gradlew :backend:services:harvest:bootRun
./gradlew :backend:services:finance:bootRun
./gradlew :backend:services:compliance:bootRun
./gradlew :backend:gateway:bootRun
cd web && npm install && npm run dev
```

## Run locally (Docker Compose)

**Multi-stage Alpine** images (JDK/Node in the builder; JRE/nginx at runtime). Mobile is not part of Compose.

Supported path: the scripts stage the Gradle zip (SHA-256), copy `deploy/compose/*.env.example` when missing, **always build**, and start with `pull_policy: never`. Default profile: **all**.

**all** starts every service and seeds its demo data. For profiles **all** and **domains**, compose-up applies `scripts/ensure-new-dbs.sql` so an older PostGIS volume gains `agronomy_db`, `irrigation_db`, `harvest_db`, `finance_db`, and `compliance_db`. **core** is the smaller stack: auth, farm, asset, telemetry, operation, inventory, alert, reporting, finance, gateway, and web. It leaves weather, agronomy, irrigation, harvest, compliance, AI, notification, file, sync, and integration down.

```powershell
docker compose up -d
.\scripts\compose-up.ps1
# smaller stack (weather, agronomy, irrigation, harvest, compliance, AI, notification, file, sync, and integration stay down):
# .\scripts\compose-up.ps1 core
```

Linux / macOS:

```bash
chmod +x scripts/compose-up.sh
./scripts/compose-up.sh            # profile all (default; seeds every service)
# ./scripts/compose-up.sh core     # smaller stack (weather, agronomy, irrigation, harvest, compliance, AI, notification, file, sync, and integration stay down)
```

Details and the profile table: [deploy/compose/README.md](deploy/compose/README.md). Gateway `http://localhost:8080`, web `http://localhost:5173`. Local infra: PostGIS `127.0.0.1:5432`, Timescale `127.0.0.1:5433`, RabbitMQ `5672`, Redis `6379`, MinIO.

The web app uses Leaflet with Esri satellite imagery and does not need a Google Maps key. Without `ANDROID_GOOGLE_MAPS_API_KEY`, mobile shows the key status instead of the map.

The web UI groups tower, farms, map, operations, decisions, data, harvest, ESG, and alerts.

## ADRs

- [001 Microservices](docs/adr/001-microservices.md)
- [002 KMP](docs/adr/002-kmp-mobile.md)
- [003 Spring Boot 4 / Java 26](docs/adr/003-spring-boot-4.md)

## Seed

With `APP_SEED=true` (the default) each service loads demo data on startup. Reset: `POST /api/v1/dev/seed/reset` (auth) or `POST /api/v1/dev/seed/reset/{service}` (farm, machines, telemetry, weather, operation, inventory, alerts, ai, notifications, files, reports, sync, integrations, agronomy, irrigation, harvest, finance, compliance).

## Investor demo

One closed loop, with deterministic data, on farm 1 (São Gabriel do Oeste, MS):

| Piece | Where to look | Demo data |
| --- | --- | --- |
| Approved `SPOT` prescription | Decisions / Agronomy | `rx-spot-001`, fraction 0.35, agronomic prescription `REC-DEMO-001` |
| Draft prescription (start refused) | Operations | `op-rx-draft` linked to `rx-draft-001` |
| Spray savings and MoA alert | Decisions | `GET /api/v1/prescriptions/{id}/spray-savings`, `GET /api/v1/prescriptions/moa-rotation?fieldId=` |
| Planting window | Seasons | ZARC 1 Oct–20 Dec; sanitary void 15 Jun–15 Sep. `GET /api/v1/weather/planting-gate` |
| Evidence pack | Compliance, lot | `LOT-BV-001` on `GET /api/v1/compliance/lots/LOT-BV-001` |
| Credit dossier | Compliance | farm 1 regular; farm 3 embargoed |
| Complete with liters | Mobile, in-progress order | `POST /api/v1/operations/{id}/complete` with `actualLiters` up to the reserved quantity; the remainder returns to stock |

Starting an order linked to a prescription proceeds only when the status is `APPROVED`. Savings, evidence, ZARC, and dossier figures are marked as simulation.

## Weather

The default series is demo data (`WEATHER_PROVIDER=demo`), so the stack starts without internet.

To call Open-Meteo (no API key; agricultural Weather.com requires a contract), on the weather `bootRun`:

```powershell
$env:WEATHER_PROVIDER = "open-meteo"
```

```bash
WEATHER_PROVIDER=open-meteo ./gradlew :backend:services:weather:bootRun
```

In Compose, the variable has to be in the `weather` service environment (`demo.env` does not turn this on by itself). `GET /api/v1/weather/forecast` and `/weather/current` then use that series (`vintage=open-meteo`). If the call fails, the series already stored stays in place. The ZARC gate stays on the local table.

## Tests

```bash
./gradlew test
cd web && npm test && npm run build
./gradlew -p mobile :composeApp:testDebugUnitTest
```

The mobile test needs the Android SDK. Without `ANDROID_HOME`, that task does not compile.

## Limitations

- NDVI is a **Demo NDVI** layer, not a Google product.
- AI is a demo statistical model (`demo-gradient-baseline`), not a validated agronomic model.
- Demo telemetry is not manufacturer telemetry.
- Live weather is daily Open-Meteo. There is no Weather.com evapotranspiration. MAPA ZARC replaces the seeded window only with `MAPA_LIVE=true` (open-data CKAN, short timeout; failure keeps the seed).
- The evidence pack and credit dossier are seed snapshots, not live CAR, PRODES, or SEFAZ lookups. `GET /api/v1/compliance/lots/{lotCode}/nfe` returns homologation XML (`tpAmb=2`) with the prescription and the technical manager CPF; it does not sign or submit to SEFAZ.
- The local seed has 8 farms and 22 fields, not the 180-field catalog from the specification.
- Demo telemetry: 7 days, 15-minute interval. The report CSV does not join live operation and inventory.
- RabbitMQ is up, but AMQP listeners stay off. The inventory saga calls inventory over HTTP. Kafka (`127.0.0.1:9092`) publishes `precision.operation.started` only with `KAFKA_ENABLED=true`, after a successful start; a broker failure does not roll back the saga.
- `GET /api/v1/prescriptions/{id}/isoxml` exports a minimal TASKDATA. There is no manufacturer file parser.
- The mobile map is Leaflet plus an Esri image in a WebView when the field has geometry. The Google Maps SDK is still deferred.
- Deferred: copilot/agents, GeoTIFF, OIDC, and the chaos lab screen.
- iOS: the Windows slice is `mobile/composeApp`. `xcodebuild` runs only on macOS.

Short companion list: [docs/known-limitations.md](docs/known-limitations.md).

---

Copyright © 2026 Filipe de Brito Thomé. All rights reserved.
