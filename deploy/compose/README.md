# Local Docker Compose (demo)

Optimized multi-stage images live under `deploy/docker/`. Per-service overlays are in this folder. **Mobile is excluded.**

## Image strategy (small final images)

| Target | Builder | Runtime |
| --- | --- | --- |
| JVM services / gateway | `eclipse-temurin:21-jdk-alpine` + Gradle `bootJar` | `eclipse-temurin:21-jre-alpine` (jar only, non-root) |
| Web | `node:22-alpine` (`npm ci` + `npm run build`) | `nginx:1.27-alpine` (static `dist/` only) |

Repo-root `.dockerignore` keeps context lean (no `mobile/`, `node_modules`, `build/`, `.git`).

## Prerequisites

1. Docker Desktop with Compose v2
2. Start infra once: `docker compose up -d` (Postgres, Timescale, Rabbit, Redis, MinIO on `127.0.0.1`)

## Quick start — core demo

```powershell
# Windows
.\scripts\compose-up.ps1 -Profile core -Build
```

```bash
# Linux / macOS — all containers (default)
chmod +x scripts/compose-up.sh
./scripts/compose-up.sh --build

# core only (auth + farm + gateway + web)
./scripts/compose-up.sh core --build
```

Equivalent:

```bash
docker compose up -d
docker compose -f docker-compose.yml -f deploy/compose/stack.yml \
  --env-file deploy/compose/demo.env --profile all up -d --build
```

- API gateway: http://localhost:8080  
- Web: http://localhost:5173  
- Login: `manager@precisionfarming.demo` / `Precision@123`  
- Seeds: `APP_SEED=true` in `demo.env`

## Profiles (`stack.yml`)

| Profile | Services |
| --- | --- |
| `core` | auth, farm, gateway, web |
| `fleet` | asset, telemetry |
| `ops` | operation, inventory, alert |
| `domains` | agronomy, irrigation, harvest, finance, compliance |
| `all` | every backend service + gateway + web |
| per-name | `auth`, `farm`, `gateway`, … |

## Single service overlay

```powershell
docker compose -f docker-compose.yml -f deploy/compose/auth.yml --env-file deploy/compose/demo.env up -d --build
```

## Notes

- App containers talk to DBs via Docker DNS (`postgres`, `timescaledb:5432`), not `localhost`.
- Only **gateway** (`8080`) and **web** (`5173`) are published to the host.
- First JVM image build compiles with Gradle inside Docker (slower); rebuilds reuse layers when sources unchanged.
- Do not use these demo secrets outside local machines.
