# Security assessment — Precision Farming (2026-09-01)

Authorized assessment of the local monorepo. Updated after remediation pass.

Skill: `.cursor/skills/cybersecurity-team/SKILL.md`  
Architect: [kotlin-architect](d7301184-6575-4c84-8b0f-7b32449c2a99)  
Engineer: [kotlin-backend-engineer](04dc0eeb-0cb2-4215-afc7-897dda48b7ce)

## Status: remediations applied

| Former finding | Status |
| --- | --- |
| Cross-farm IDOR on lists (`findAll`) | **Fixed** — JWT `farmIds` + `AccessScope.resolveFarms` + `findByFarmIdIn` |
| Cross-farm writes via client `farmId` | **Fixed** — `requireFarm` / `requireEntityFarm` |
| ID-only approve/complete/dispatch | **Fixed** — farm scope + `@PreAuthorize` role matrix |
| Compliance get by id without farm | **Fixed** — entity farm check |
| Unauthenticated `/dev/seed/reset` | **Fixed** — removed from `permitAll`; requires `ADMIN` |
| Demo JWT/DB secrets outside local | **Mitigated** — `DemoSecretsGuard` + `app.security.allow-demo-secrets` (default `true` for local demo; set `false` or use profile `local` only in staging/prod) |
| CORS `*` + credentials | **Accepted for local demo** — restrict per env in staging/prod |

## Controls now in place

- JWT claims: `tenantId`, `farmIds[]`, plus `sub` / `email` / `role`
- Demo role → farms: ADMIN 001–005; FARM_MANAGER/MAINTENANCE 001–002; OPERATOR 001
- Shared `FarmAccess` / `AccessScope` in `backend/libs/security`
- Role gates: prescription approve; work-order complete; logistics dispatch; operation start/pause/complete; seed reset ADMIN-only
- Gateway no longer permits seed anonymously

## Remaining recommendations (not blockers for local MVP)

1. Persist user↔farm membership in auth DB instead of role→demo map.
2. Restrict CORS origins outside local.
3. Add SCA (OWASP Dependency-Check / OSV) in CI.
4. Mobile: secure token storage before production.

## Validation

`./gradlew test` — BUILD SUCCESSFUL after remediation.
