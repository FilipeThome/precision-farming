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

## Remaining / accepted for MVP

1. Persist user↔farm membership in auth DB (still role→demo map at login).
2. SCA (OWASP Dependency-Check / OSV) in CI.
3. iOS Keychain when KMP ios target lands.
4. Demo credentials (`Precision@123`) local-only.

## Validation

`./gradlew test` after remediations.
