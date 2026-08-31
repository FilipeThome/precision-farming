# Precision Farming — Agent orchestration

Large features require architect approval before implementation.

| Stack | Architect | Engineer | Validate |
| --- | --- | --- | --- |
| Backend Kotlin / Spring Cloud | kotlin-architect | kotlin-backend-engineer | `./gradlew test` |
| Web React | react-architect | react-engineer | `npm test` + `npm run build` |
| Mobile KMP | kmp-architect | kmp-engineer | Android compile + commonTest |
| Infra Compose/Terraform | infra-architect | terraform-engineer | `docker compose config` |

Then **test-engineer** and **reviewer**.

Clients call only `http://localhost:8080/api/v1`. Database per service. Sagas via outbox + RabbitMQ. No secrets in git.

See `docs/adr/` and `CLAUDE.md`.
