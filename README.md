# Precision Farming MVP

Protótipo executável de gestão agrícola de precisão: **microserviços Kotlin**, **web React** e **mobile KMP/Compose**. Clientes falam apenas com o gateway em `http://localhost:8080`.

## Stack

- Backend: Spring Boot 4.1, Java 26 (virtual threads), Kotlin 2.4, Spring Cloud Gateway
- Dados: PostGIS `:5432`, TimescaleDB `:5433`, RabbitMQ, Redis, MinIO (Compose; portas só em `127.0.0.1` — prod não deve publicar)
- Web: React + TypeScript + Vite + Tailwind
- Mobile: Kotlin Multiplatform / Compose (Android neste Windows; iOS exige macOS). Gradle/JDK 26, Android jvmTarget 26, compileSdk 37
- Auth: JWT + refresh. Demo: `manager@precisionfarming.demo` / `Precision@123`
- Segredos demo: `.env.example` define `ALLOW_DEMO_SECRETS=true`; sem isso (e sem profile `local`) o boot recusa JWT/DB demo

Aprovações de arquitetura: [docs/architecture-approvals.md](docs/architecture-approvals.md).

## Subir local (Gradle / hybrid)

```bash
cp .env.example .env
docker compose up -d
./gradlew test
# ALLOW_DEMO_SECRETS=true (via .env) ou Spring profile `local`
# terminais separados:
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

## Subir local (Docker Compose)

Imagens **multi-stage Alpine** (JDK/Node no builder; JRE/nginx no runtime). Mobile não entra no Compose.

Caminho suportado: os scripts fazem stage do zip Gradle (SHA-256), copiam `deploy/compose/*.env.example` se faltar, **sempre buildam**, e sobem com `pull_policy: never`. Perfil padrão: **core** (não `all`).

```powershell
docker compose up -d
.\scripts\compose-up.ps1
# stack completo:
# .\scripts\compose-up.ps1 all
```

Linux / macOS:

```bash
chmod +x scripts/compose-up.sh
./scripts/compose-up.sh            # profile core
# ./scripts/compose-up.sh all      # todos os containers
```

Detalhes: [deploy/compose/README.md](deploy/compose/README.md). Gateway `http://localhost:8080`, web `http://localhost:5173`.

O mapa web usa Leaflet com imagens de satélite Esri (sem chave do Google Maps).

## ADRs

- [001 Microserviços](docs/adr/001-microservices.md)
- [002 KMP](docs/adr/002-kmp-mobile.md)
- [003 Spring Boot 4 / Java 26](docs/adr/003-spring-boot-4.md)

## Seed

Com `APP_SEED=true` (padrão) cada serviço popula dados demo na subida. Reset: `POST /api/v1/dev/seed/reset` (auth) ou `POST /api/v1/dev/seed/reset/{service}` (farm, machines, telemetry, weather, operation, inventory, alerts, ai, notifications, files, reports, sync, integrations, agronomy, irrigation, harvest, finance, compliance).

## Limitações

- NDVI é camada **Demo NDVI**, não produto Google.
- IA é modelo estatístico demo (`demo-gradient-baseline`), não modelo agronômico validado.
- Telemetria demo não é telemetria de fabricante.
- iOS: código/host previstos; `xcodebuild` só em macOS.
