# Full Functional Spec — Implementation Slice (2026-09-01)

Source: `docs/spec-full-functional-extract.txt` (from Precision_Farming_MVP_Full_Functional_Specification.docx).

## Constraints (must keep)

- ADR-001 microservices, gateway-only clients (`/api/v1`).
- Database per service, JWT at gateway + each service.
- Domain without Spring/JPA; DTOs at HTTP edges.
- Fake adapters + deterministic seed; no real OEM/satellite/weather vendors.
- Architect approval before large features.

## User asks this turn

1. Implement missing capabilities from the full functional spec across **web, backend, mobile**.
2. Demo seed data.
3. **i18n PT-BR + EN-US** with Brazil / USA flag toggle (international-ready UX).
4. Keep existing architecture and design patterns.
5. Parallel sub-agents.

## Pragmatic breadth (not production-depth)

Deliver **executable vertical slices** for every major navigation branch with list/detail APIs, seed, and UI — not full OEM integrations. Scale seed for LOCAL demo (not 180 fields): keep deterministic DemoIds, expand to ~8 farms / ~12–20 fields / richer ops when cheap.

### Backend (new or extended services)

| Domain | Approach |
| --- | --- |
| Seasons | Extend **farm-service** (`/seasons`) |
| Map layers catalog | Extend **file-service** (`/map/layers`) |
| Scouting / soil / prescriptions / recommendations | New **agronomy-service** (8095) |
| Irrigation / water | New **irrigation-service** (8096) |
| Harvest / logistics / storage | New **harvest-service** (8097) |
| Finance / market / scenarios | New **finance-service** (8098) |
| Maintenance work orders | Extend **asset-service** (`/maintenance/work-orders`) |
| Weather windows | Extend **weather-service** (`/weather/windows`) |
| Traceability / ESG (read models) | New **compliance-service** (8099) or fold into reporting |
| Seeded alerts / AI / ops / inventory | Enrich existing seeds to match §§57–82 |

Gateway routes + Flyway + `APP_SEED` runners for each.

### Web

- i18n: `pt-BR` / `en-US`, Zustand locale, BR/US flag buttons in Header (persist `localStorage`).
- Expand Sidebar to match §5 navigation tree (grouped or flat with new routes).
- New feature pages calling gateway; loading/empty/error states; no hardcoded UI-only fake results.
- Existing screens switch to `t()` keys.

### Mobile

- Same locales; flag toggle on login + home shell.
- Extra tabs/screens for ops-critical: scouting, weather windows, irrigation recommendations, maintenance WOs (read + simple actions where APIs exist).
- Strings via resource maps (no hardcode).

### Explicitly deferred (still Fake Adapter contracts later)

- Kafka, full Workflow engine, Copilot chat, Agent runners, GeoTIFF tiles, ISOXML, real OIDC, 50k telemetry samples, Chaos lab UI.
- KMP `shared`/`iosApp` split (Windows slice stays `composeApp`).

## Acceptance for this slice

- `./gradlew test` green.
- `cd web && npm test && npm run build` green.
- Mobile compiles (Android) when SDK present.
- Flag toggle switches all chrome + feature strings PT↔EN.
- New nav modules show seeded demo data via gateway.
