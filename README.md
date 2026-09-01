# Precision Farming MVP

Protótipo executável de gestão agrícola de precisão: **microserviços Kotlin**, **web React** e **mobile KMP/Compose**. Clientes falam apenas com o gateway em `http://localhost:8080`.

## Stack

- Backend: Spring Boot 3.5, Java 21 (virtual threads), Spring Cloud Gateway
- Dados: PostGIS `:5432`, TimescaleDB `:5433`, RabbitMQ, Redis, MinIO (Compose; portas só em `127.0.0.1` — prod não deve publicar)
- Web: React + TypeScript + Vite + Tailwind
- Mobile: Kotlin Multiplatform / Compose (Android neste Windows; iOS exige macOS)
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

## Subir local (Docker Compose por serviço)

Imagens **multi-stage Alpine** (JDK/Node no builder; JRE/nginx no runtime). Mobile não entra no Compose.

```powershell
docker compose up -d
.\scripts\compose-up.ps1 -Profile core -Build
# ou stack completo:
# .\scripts\compose-up.ps1 -Profile all -Build
```

Linux / macOS:

```bash
chmod +x scripts/compose-up.sh
./scripts/compose-up.sh --build          # todos os containers (profile all)
# ./scripts/compose-up.sh core --build   # só core
```

Detalhes: [deploy/compose/README.md](deploy/compose/README.md). Gateway `http://localhost:8080`, web `http://localhost:5173`.

Opcional: `WEB_GOOGLE_MAPS_API_KEY` / `VITE_GOOGLE_MAPS_API_KEY` (nunca commitar). Sem chave, o mapa mostra status e retry — não um PNG estático.

## ADRs

- [001 Microserviços](docs/adr/001-microservices.md)
- [002 KMP](docs/adr/002-kmp-mobile.md)
- [003 Spring Boot 3.5](docs/adr/003-spring-boot-35.md)

## Seed

Com `APP_SEED=true` (padrão) cada serviço popula dados demo na subida. Reset: `POST /api/v1/dev/seed/reset` (hoje roteado ao auth; os demais expõem o mesmo path localmente).

## Limitações

- NDVI é camada **Demo NDVI**, não produto Google.
- IA é modelo estatístico demo (`demo-gradient-baseline`), não modelo agronômico validado.
- Telemetria demo não é telemetria de fabricante.
- iOS: código/host previstos; `xcodebuild` só em macOS.
