# Security assessment — Precision Farming (2026-09-01)

Authorized assessment of the local monorepo. Updated after full cyber-team remediations.

Skill: `.cursor/skills/cybersecurity-team/SKILL.md`

## Status: remediations applied (audit pass 2)

| Finding | Status |
| --- | --- |
| Cross-farm IDOR on lists (`findAll`) | **Fixed** — JWT `farmIds` + `AccessScope` |
| Unauthenticated `/dev/seed/reset` | **Fixed** — ADMIN + authn |
| CORS `*` + credentials | **Fixed** — localhost patterns |
| Demo JWT/DB secrets outside local | **Fixed** — `allow-demo-secrets` default **false**; `DemoSecretsGuard` + gateway guard; `.env.example` sets `ALLOW_DEMO_SECRETS=true` for local |
| Cross-farm inventory via operation saga | **Fixed** — `DemoItemFarms` on create; saga `type=service` + `farmIds=[op.farmId]` only |
| Compose infra LAN exposure | **Fixed** — ports bound to `127.0.0.1` |
| OPERATOR PATCH/DELETE farm/field | **Fixed** — `@PreAuthorize ADMIN|FARM_MANAGER` |
| Sync unbound deviceId | **Fixed** — deviceId must equal `sub` or `sub:…` |
| Field/machine write integrity | **Fixed** — `requireBelongsToFarm` on agronomy/harvest/irrigation/asset/operation |
| Service tokens indistinguishable from ADMIN | **Fixed** — `type=service`, `role=SERVICE`, short TTL |
| Operation start/complete race | **Fixed** — `STARTING`/`COMPLETING` + `@Version` |
| AI feedback unscoped | **Fixed** — load prediction + `requireEntityFarm` |
| Mobile token memory-only | **Fixed** — Android `TokenStore` (EncryptedSharedPreferences) |

## Hardening slice (post AgOS V4 assessment)

| Finding | Status |
| --- | --- |
| No login/refresh rate limit | **Fixed** — `AuthRateLimiter` (IP+email), `AUTH_RATE_LIMIT` (default 20/min) |
| Swagger/`v3/api-docs` permitAll | **Fixed** — `SPRINGDOC_ENABLED` default **false**; permitAll only when enabled; demo compose sets true |
| Approve/dispatch UI for all roles | **Fixed** — UI gated to ADMIN/FARM_MANAGER; approve `@PreAuthorize` on agronomy |
| No idle session timeout (web) | **Fixed** — 30 min idle logout in `AppShell` |
| Weak AuthZ-deny audit | **Fixed** — `RestExceptionHandler` warns on 401/403 / AccessDenied |
| Market quotes unscoped stub | **Accepted** — authenticated global stub; documented on controller |

## Remaining / accepted for MVP

1. Persist user↔farm membership in auth DB (still role→demo map at login).
2. SCA (OWASP Dependency-Check / OSV) in CI.
3. iOS Keychain when KMP ios target lands.
4. Demo credentials (`Precision@123`) local-only (`ALLOW_DEMO_SECRETS` / profile `local`).
5. Known JWT/DB defaults in `demo.env` — local compose only; never expose.

## Validation

`./gradlew test` after remediations.
