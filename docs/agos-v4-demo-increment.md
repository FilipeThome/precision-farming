# AgOS V4 Blueprint — Demo MVP Increment (2026-09-01)

Source: `Precision_Farming_AgOS_Master_Blueprint_V4_Market_Complete.docx` → `docs/blueprint-v4-extract.txt`.

## Goal

Maximize **demo-ready** AgOS surface with deterministic fake data — not Kafka/OIDC/real OEM.

## In scope (this increment)

1. **Story seed pack** — ~8 farms, ~20–24 fields, richer seasons/machines/ops/inventory/alerts/AI.
2. **Wire unused APIs to web**: prescriptions tab, weather windows, map layers, integrations console, storage lots, ops board statuses.
3. **Dashboard KPIs** from seeded data (ops on-time proxy, critical alerts, fleet availability, margin risk stub).
4. **Harvest dispatch** on web if API exists; enrich harvest seeds.
5. **Traceability detail** chain UI.
6. **Mobile**: show weather windows already; add prescriptions list if cheap; sync conflict stub if API.
7. i18n keys for new UI (pt-BR/en-US).

## Out of scope

Kafka, Temporal, OIDC, GeoTIFF, CV, AI agents/copilot, 180 fields, real vendors, Chaos lab.
**No new microservices.** Prefer enriching existing services + fake adapters.

## Acceptance

- `./gradlew test` green
- `cd web && npm test && npm run build` green
- Demo login still works; seeds via `APP_SEED=true`

---

## Backend architecture (kotlin-architect)

**Decision: APPROVED** — seed densification + optional thin reads only as specified below. Gateway-only (`/api/v1`), DB-per-service, fake adapters, no Kafka/OIDC.

### Non-negotiable constraints

1. Keep deterministic IDs via `DemoIds.uuid("…")`; never random IDs in seeds.
2. Expand shared scope maps when farms/fields/machines/items grow — JWT farm claims come from `DemoFarmDirectory`.
3. Seed must be **idempotent and incremental**: insert missing keys only (farm pattern). Do **not** early-return on first id and skip densification on existing DBs.
4. Domain stays free of Spring/JPA; DTOs at HTTP edges; `@Transactional` on application seed methods only.
5. Clients call gateway only; no cross-service SQL; no new BFF microservice for KPIs.
6. Fake adapters only (integrations stay static DEMO connectors).

### Target seed counts (DemoIds keys)

| Domain | Keys | Count | Notes |
| --- | --- | --- | --- |
| Farms | `farm-001`…`farm-008` | **8** | +3 vs current 5 |
| Fields | `field-001`…`field-022` | **22** | in 20–24 band; distribute across all 8 farms |
| Seasons | `season-001`…`season-010` | **10** | ≥1 ACTIVE per farm; mix PLANNED |
| Machines | `machine-001`…`machine-012` | **12** | statuses: OPERATING / IDLE / MAINTENANCE |
| Work orders | `wo-001`…`wo-024` | **24** | keep ~25% COMPLETED |
| Operations | `op-001`…`op-020` | **20** | status mix: PLANNED, IN_PROGRESS, PAUSED, COMPLETED |
| Inventory items | `item-001`…`item-016` | **16** | categories DEFENSIVO/FERTILIZANTE/SEMENTE/COMBUSTIVEL/PECA across farms |
| Alerts | `alert-001`…`alert-012` | **12** | ≥3 CRITICAL, mix OPEN/ACKED, entity refs to machines/ops/fields |
| AI predictions | `prediction-001`…`prediction-010` | **10** | MACHINE_FAILURE_RISK, YIELD_FORECAST, OPERATIONAL_DELAY_RISK (+ optional WEATHER/PEST stub types) |
| Harvest plans | `hplan-001`…`hplan-012` | **12** | PLANNED / IN_PROGRESS / COMPLETED |
| Yields | `hyield-001`…`hyield-008` | **8** | |
| Logistics loads | `load-001`…`load-010` | **10** | QUEUED / DISPATCHED / DELIVERED |
| Storage units | `sunit-001`…`sunit-008` | **8** | one per farm ok |
| Storage lots | `slot-001`…`slot-010` | **10** | wire to units |
| Trace events | `trace-001`…`trace-012` | **12** | ≥2 lots with multi-event chains (same `lotCode`) |
| ESG metrics | `esg-001`…`esg-008` | **8** | cover new farms |
| Prescriptions | `rx-001`…`rx-016` | **16** | DRAFT/APPROVED; fields up to `field-022` |
| Weather windows | `wwin-001`…`wwin-016` | **16** | all 8 farms; SPRAYING/PLANTING/HARVEST |
| Weather forecasts | per farm | **8 farms** | extend keys list to farm-008 |
| Map layers | `layer-*` | **8** | NDVI/SOIL/YIELD stubs across farms |
| Irrigation assets | keep 8 + optional +2 | **8–10** | only if field refs stay valid |
| Scouting / soil | keep 40 / 30 | remap field/farm refs to new fields where cheap | |
| Finance costs/budgets | extend farm refs | costs ≥24, budgets ≥6, exposure ≥5 | margin stub for dashboard |

### Services / files to touch

**Shared (mandatory with farm/field growth)**

- `backend/libs/security/.../DemoFarmDirectory.kt` — `ALL = farm-001…farm-008`; role scopes may keep manager on farm-001/002 only.
- `backend/libs/security/.../DemoMachineFarms.kt` — map all 12 machines.
- `backend/libs/security/.../DemoFieldFarms.kt` — map all 22 fields.
- `backend/libs/security/.../DemoItemFarms.kt` — map all 16 items.
- Tests under `backend/libs/security/src/test/...` as needed.

**Seed densification (application `seed()` only; no schema migrations unless unavoidable)**

- `backend/services/farm/.../FarmService.kt` — farms 8, fields 22, seasons 10.
- `backend/services/asset/.../AssetService.kt` — machines 12, work orders 24.
- `backend/services/operation/.../OperationService.kt` — ops 20 (rich status mix for ops board).
- `backend/services/inventory/.../InventoryService.kt` — items 16.
- `backend/services/alert/.../AlertService.kt` — alerts 12.
- `backend/services/ai/.../AiService.kt` — predictions 10.
- `backend/services/harvest/.../HarvestService.kt` — plans/yields/loads/units/lots per table.
- `backend/services/compliance/.../ComplianceService.kt` — traces 12 + ESG 8; chain-friendly `lotCode`s.
- `backend/services/agronomy/.../AgronomyService.kt` — prescriptions 16; scout/soil field index → use full field set.
- `backend/services/weather/.../WeatherService.kt` — forecasts + windows for 8 farms.
- `backend/services/file/.../FileService.kt` — map layers ≥8.
- `backend/services/finance/.../FinanceService.kt` — costs/budgets/exposure covering new farms (margin for KPI stub).
- `backend/services/irrigation/.../IrrigationService.kt` — only if needed for new field refs.
- `backend/services/telemetry/.../TelemetryService.kt` — optional: series for 2–3 machines max (keep volume bounded).
- `backend/services/integration/.../IntegrationController.kt` — **no seed change** (static DEMO list OK; may add 1–2 connector rows if web needs richer console).

**Do not touch for this increment:** auth persona set (unless JWT farm claim tests break), gateway routes (unless new path added below), Kafka/outbox, new services.

### Thin new read endpoints — ONLY these

| Endpoint | Service | Why | Else use |
| --- | --- | --- | --- |
| **None required for dispatch board** | operation | Existing `GET /api/v1/operations` + denser status seeds | Web status columns / filters |
| **None required for dashboard KPI aggregate** | — | Cross-service BFF forbidden; lists stay small | Web fan-out: ops + alerts + machines + `GET /api/v1/finance/pnl` (margin stub) |
| `GET /api/v1/traceability?lotCode=` **or** filter on existing list | compliance | Chain UI needs all events for one lot | Prefer query param on existing list; avoid new resource if list already returns enough |
| Integrations | integration | Already static | No change |

**Optional (approve only if web cannot derive cheaply):**

- `GET /api/v1/operations/summary?farmId=` → counts by status + `onTimePct` proxy (`COMPLETED` with `actualEnd <= plannedEnd` / completed). Single-service, no joins.
- `GET /api/v1/assets/summary?farmId=` → counts by machine status (fleet availability %).

If added: wire gateway path only if not already covered by existing `operations/**` / `assets` or `machines` routes; keep DTOs thin; no reporting-service KPI endpoint.

**Harvest dispatch:** already exists (`GET /api/v1/logistics/loads`, `POST /api/v1/logistics/dispatch`). Enrich seeds only; no new harvest microservice APIs.

### Dashboard KPI derivation (no cross-DB)

| KPI | Source |
| --- | --- |
| Ops on-time proxy | operation list/summary |
| Critical alerts | alert list where `severity=CRITICAL` & `status=OPEN` |
| Fleet availability | machines `OPERATING+IDLE` / total (exclude MAINTENANCE as unavailable) |
| Margin risk stub | finance PnL / exposure (existing) |

### Risks

1. **Early-return seeds** leave old DBs sparse → mitigate with insert-missing-by-id.
2. **Scope maps drift** → update Demo* maps + security tests in same PR as farm/field growth.
3. **Telemetry volume** → do not multiply 7-day×15min series across all 12 machines; cap at ≤3 machines.

### Engineer checklist

- [x] Shared Demo* maps + FarmDirectory for 8 farms / 22 fields / 12 machines / 16 items
- [x] Seed counts match table above; `./gradlew test` green
- [x] No new microservice; no Kafka/OIDC
- [x] No cross-service dashboard aggregate endpoint
- [x] Harvest/logistics/storage/prescriptions/weather windows/integrations remain gateway-routed as today

### Delivery notes (2026-09-01)

- **Backend:** densified seeds + `DemoFarmDirectory` / `DemoFieldFarms` / `DemoMachineFarms` / `DemoItemFarms`; insert-missing pattern.
- **Web:** prescriptions tab, weather windows, map layer toggles, ops board filters, dashboard KPIs (fan-out), harvest lots/dispatch, `/compliance/lots/:lotCode`, `/integrations`, i18n.
- **Mobile:** Mais → prescriptions + sync status stub (`POST /sync/pull`).
- **Validated:** `./gradlew test`, `cd web && npm test && npm run build`. Mobile unit tests need local Android SDK (`ANDROID_HOME` / `local.properties`).
