# Precision Farming MVP

[English](README.md)

Protótipo executável de gestão agrícola de precisão: **microserviços Kotlin**, **web React** e **mobile KMP/Compose**. Clientes falam apenas com o gateway em `http://localhost:8080`.

**Autor:** Filipe de Brito Thomé

## Stack

- Backend: Spring Boot 4.1, Java 26 (virtual threads), Kotlin 2.4, Spring Cloud Gateway
- Dados: PostGIS `:5432`, TimescaleDB `:5433`, RabbitMQ, Redis, MinIO (Compose; portas só em `127.0.0.1` — prod não deve publicar)
- Web: React + TypeScript + Vite + Tailwind
- Mobile: Kotlin Multiplatform / Compose (Android neste Windows; iOS exige macOS). Gradle/JDK 26, Android jvmTarget 26, compileSdk 37
- Auth: JWT + refresh. Senha demo de todas as personas: `Precision@123`
- Segredos demo: `.env.example` define `ALLOW_DEMO_SECRETS=true`; sem isso (e sem profile `local`) o boot recusa JWT/DB demo
- Interface web e mobile: pt-BR e en-US, com troca de idioma. O padrão é pt-BR. Textos de alerta do seed e mensagens de erro da API ficam em um único idioma.

Pré-requisitos: JDK 26, Docker Compose v2 e, para a web, Node 22. No Windows use `gradlew.bat`. Android: [mobile/README.md](mobile/README.md). Terraform em `infra/` é esqueleto e não deve ser aplicado.

Aprovações de arquitetura: [docs/architecture-approvals.md](docs/architecture-approvals.md).

## Personas demo

| E-mail | Papel |
| --- | --- |
| `admin@precisionfarming.demo` | Admin |
| `manager@precisionfarming.demo` | Gerente (aprova prescrição) |
| `operator@precisionfarming.demo` | Operador (executa a ordem, inclusive offline) |
| `maintenance@precisionfarming.demo` | Manutenção |

## Subir local (Gradle / hybrid)

```bash
cp .env.example .env
docker compose up -d
./gradlew test
# bootRun padrão: APP_SEED=true e profile Spring `local` (seed demo + segredos demo).
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

Caminho suportado: os scripts fazem stage do zip Gradle (SHA-256), copiam `deploy/compose/*.env.example` se faltar, **sempre buildam**, e sobem com `pull_policy: never`. Perfil padrão: **all**.

**all** sobe cada serviço e popula o seed demo. Nos perfis **all** e **domains**, o compose-up aplica `scripts/ensure-new-dbs.sql` para um volume PostGIS antigo ganhar `agronomy_db`, `irrigation_db`, `harvest_db`, `finance_db` e `compliance_db`. **core** é o stack menor: auth, farm, asset, telemetry, operation, inventory, alert, reporting, finance, gateway e web. Ficam de fora clima, agronomia, irrigação, colheita, compliance, IA, notificação, arquivos, sync e integração.

```powershell
docker compose up -d
.\scripts\compose-up.ps1
# stack menor (ficam de fora clima, agronomia, irrigação, colheita, compliance, IA, notificação, arquivos, sync e integração):
# .\scripts\compose-up.ps1 core
```

Linux / macOS:

```bash
chmod +x scripts/compose-up.sh
./scripts/compose-up.sh            # profile all (padrão; seed de cada serviço)
# ./scripts/compose-up.sh core     # stack menor (ficam de fora clima, agronomia, irrigação, colheita, compliance, IA, notificação, arquivos, sync e integração)
```

Detalhes e a tabela de perfis: [deploy/compose/README.md](deploy/compose/README.md). Gateway `http://localhost:8080`, web `http://localhost:5173`. Infra local: PostGIS `127.0.0.1:5432`, Timescale `127.0.0.1:5433`, RabbitMQ `5672`, Redis `6379`, MinIO.

A web usa Leaflet com satélite Esri, sem chave do Google Maps. O mobile, sem `ANDROID_GOOGLE_MAPS_API_KEY`, mostra o status da chave em vez do mapa.

A interface web agrupa torre, fazendas, mapa, operações, decisões, dados, colheita, ESG e alertas.

## ADRs

- [001 Microserviços](docs/adr/001-microservices.md)
- [002 KMP](docs/adr/002-kmp-mobile.md)
- [003 Spring Boot 4 / Java 26](docs/adr/003-spring-boot-4.md)

## Seed

Com `APP_SEED=true` (padrão) cada serviço popula dados demo na subida. Reset: `POST /api/v1/dev/seed/reset` (auth) ou `POST /api/v1/dev/seed/reset/{service}` (farm, machines, telemetry, weather, operation, inventory, alerts, ai, notifications, files, reports, sync, integrations, agronomy, irrigation, harvest, finance, compliance).

## Demo do investidor

Um ciclo fechado, com dados determinísticos, na fazenda 1 (São Gabriel do Oeste, MS):

| Peça | Onde ver | Dado demo |
| --- | --- | --- |
| Prescrição `SPOT` aprovada | Decisões / Agronomia | `rx-spot-001`, fração 0,35, receituário `REC-DEMO-001` |
| Prescrição rascunho (start recusa) | Operações | `op-rx-draft` ligada a `rx-draft-001` |
| Economia de pulverização e alerta de MoA | Decisões | `GET /api/v1/prescriptions/{id}/spray-savings`, `GET /api/v1/prescriptions/moa-rotation?fieldId=` |
| Janela de plantio | Safras | ZARC 1 out–20 dez; vazio sanitário 15 jun–15 set. `GET /api/v1/weather/planting-gate` |
| Pacote de evidência | Compliance, lote | `LOT-BV-001` em `GET /api/v1/compliance/lots/LOT-BV-001` |
| Dossiê de crédito | Compliance | fazenda 1 regular; fazenda 3 embargada |
| Complete com litros | Mobile, ordem em andamento | `POST /api/v1/operations/{id}/complete` com `actualLiters` até a quantidade reservada; o restante volta ao estoque |

O start de uma ordem ligada a prescrição só segue se o status for `APPROVED`. Números de economia, evidência, ZARC e dossiê vêm marcados como simulação.

## Clima

O padrão é a série demo (`WEATHER_PROVIDER=demo`), para o stack subir sem internet.

Para consumir a Open-Meteo (sem chave; a Weather.com agrícola exige contrato), no `bootRun` do clima:

```powershell
$env:WEATHER_PROVIDER = "open-meteo"
```

```bash
WEATHER_PROVIDER=open-meteo ./gradlew :backend:services:weather:bootRun
```

No Compose, a variável tem de estar no ambiente do serviço `weather` (o `demo.env` não liga isso sozinho). `GET /api/v1/weather/forecast` e `/weather/current` passam a usar essa série (`vintage=open-meteo`). Se a chamada falhar, permanece a série já gravada. A trava de ZARC continua na tabela local.

## Testes

```bash
./gradlew test
cd web && npm test && npm run build
./gradlew -p mobile :composeApp:testDebugUnitTest
```

O teste do mobile exige Android SDK. Sem `ANDROID_HOME`, essa tarefa não compila.

## Limitações

- NDVI é camada **Demo NDVI**, não produto Google.
- IA é modelo estatístico demo (`demo-gradient-baseline`), não modelo agronômico validado.
- Telemetria demo não é telemetria de fabricante.
- Clima ao vivo é Open-Meteo diária. Não há evapotranspiração da Weather.com. O ZARC do MAPA só substitui a janela semeada com `MAPA_LIVE=true` (CKAN de dados abertos, timeout curto; falha mantém o seed).
- Pacote de evidência e dossiê de crédito são snapshots de seed, não consulta a CAR, PRODES ou SEFAZ. `GET /api/v1/compliance/lots/{lotCode}/nfe` devolve XML de homologação (`tpAmb=2`) com receituário e CPF do RT; não assina e não transmite à SEFAZ.
- O seed local tem 8 fazendas e 22 talhões, não o catálogo de 180 talhões da especificação.
- Telemetria demo: 7 dias, intervalo de 15 minutos. O CSV de relatório não junta operação e estoque ao vivo.
- RabbitMQ está no ar, mas os listeners AMQP seguem desligados. A saga de estoque chama o inventory por HTTP. Kafka (`127.0.0.1:9092`) publica `precision.operation.started` só com `KAFKA_ENABLED=true`, depois do start bem-sucedido; falha do broker não desfaz a saga.
- `GET /api/v1/prescriptions/{id}/isoxml` exporta um TASKDATA mínimo. Não há parser de arquivo de fabricante.
- O mapa do mobile é Leaflet + imagem Esri num WebView quando o talhão tem geometria. O SDK do Google Maps continua adiado.
- Adiado: copilot/agentes, GeoTIFF, OIDC e a tela de chaos lab.
- iOS: o slice deste Windows é `mobile/composeApp`. `xcodebuild` só em macOS.

Lista curta complementar: [docs/known-limitations.md](docs/known-limitations.md).

---

Copyright © 2026 Filipe de Brito Thomé. Todos os direitos reservados.
