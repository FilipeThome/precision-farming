#!/usr/bin/env bash
# Start Precision Farming local demo stack on Linux (infra + app containers).
# Mobile is excluded. Requires Docker Compose v2.
#
# Usage:
#   ./scripts/compose-up.sh              # profile=all, no rebuild
#   ./scripts/compose-up.sh --build      # rebuild images
#   ./scripts/compose-up.sh core --build
#   ./scripts/compose-up.sh all
#
# Profiles: core | all | fleet | ops | domains

set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT"

PROFILE="all"
BUILD=0

for arg in "$@"; do
  case "$arg" in
    --build|-b) BUILD=1 ;;
    core|all|fleet|ops|domains) PROFILE="$arg" ;;
    -h|--help)
      sed -n '2,14p' "$0"
      exit 0
      ;;
    *)
      echo "Unknown argument: $arg" >&2
      echo "Usage: $0 [core|all|fleet|ops|domains] [--build]" >&2
      exit 1
      ;;
  esac
done

case "$PROFILE" in
  core|all|fleet|ops|domains) ;;
  *)
    echo "Invalid profile: $PROFILE" >&2
    exit 1
    ;;
esac

echo "==> Starting infra (postgres, timescaledb, rabbitmq, redis, minio)..."
docker compose -f docker-compose.yml up -d

BUILD_ARGS=()
if [[ "$BUILD" -eq 1 ]]; then
  BUILD_ARGS=(--build)
fi

echo "==> Starting apps profile=${PROFILE}..."
docker compose \
  -f docker-compose.yml \
  -f deploy/compose/stack.yml \
  --env-file deploy/compose/demo.env \
  --profile "$PROFILE" \
  up -d "${BUILD_ARGS[@]}"

echo
echo "Gateway  http://localhost:8080"
if [[ "$PROFILE" == "core" || "$PROFILE" == "all" ]]; then
  echo "Web      http://localhost:5173"
fi
echo "Demo     manager@precisionfarming.demo / Precision@123"
echo "Done."
