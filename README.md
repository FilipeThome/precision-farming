# Precision Farming MVP

Protótipo executável de gestão agrícola de precisão: **microserviços Kotlin**, **web React** e **mobile KMP/Compose**. Clientes falam apenas com o gateway em `http://localhost:8080`.

## Stack

- Backend: Spring Boot 3.5, Java 21 (virtual threads), Spring Cloud Gateway
- Dados: PostGIS `:5432`, TimescaleDB `:5433`, RabbitMQ, Redis, MinIO (Compose completo, pronto mesmo se um serviço ainda não usa)
- Web: React + TypeScript + Vite + Tailwind
- Mobile: Kotlin Multiplatform / Compose (Android neste Windows; iOS exige macOS)
- Auth: JWT + refresh. Demo: `manager@precisionfarming.demo` / `Precision@123`

Aprovações de arquitetura: [docs/architecture-approvals.md](docs/architecture-approvals.md).

## Subir local

```bash
cp .env.example .env
docker compose up -d
./gradlew test
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
./gradlew :backend:gateway:bootRun
cd web && npm install && npm run dev
```

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
