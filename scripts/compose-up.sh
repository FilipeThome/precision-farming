#!/usr/bin/env bash
# Start Precision Farming local demo stack on Linux (infra + app containers).
# Mobile is excluded. Requires Docker Compose v2.
# Infra includes Kafka (PLAINTEXT, 127.0.0.1:9092). App publish stays off unless KAFKA_ENABLED=true.
#
# Usage:
#   ./scripts/compose-up.sh              # profile=all (every service seeds demo data)
#   ./scripts/compose-up.sh --build      # same (cache is used; flag kept for compatibility)
#   ./scripts/compose-up.sh core --build # smaller stack; weather, agronomy, irrigation, harvest, compliance, AI, notification, file, sync, and integration stay down
#   ./scripts/compose-up.sh all          # same as the default
#
# Profiles: core | all | fleet | ops | domains
# Optional flags in deploy/compose/demo.env (copied from *.example on first run):
#   KAFKA_ENABLED=true   MAPA_LIVE=true   WEATHER_PROVIDER=open-meteo

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
      sed -n '2,17p' "$0"
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

gradle_wrapper_version() {
  local line version
  line="$(grep -E '^distributionUrl=' "$ROOT/gradle/wrapper/gradle-wrapper.properties" | head -n 1)"
  version="$(printf '%s\n' "$line" | sed -n 's/.*gradle-\([0-9.]*\)-bin\.zip.*/\1/p')"
  if [[ -z "$version" ]]; then
    echo "Could not parse Gradle version from gradle-wrapper.properties" >&2
    exit 1
  fi
  printf '%s\n' "$version"
}

gradle_wrapper_sha256() {
  local line
  line="$(grep -E '^distributionSha256Sum=' "$ROOT/gradle/wrapper/gradle-wrapper.properties" | head -n 1)"
  if [[ -z "$line" ]]; then
    echo "distributionSha256Sum missing from gradle-wrapper.properties" >&2
    exit 1
  fi
  printf '%s\n' "${line#distributionSha256Sum=}" | tr 'A-F' 'a-f'
}

gradle_zip_sha_ok() {
  local path="$1" expected="$2" actual
  [[ -f "$path" ]] || return 1
  actual="$(sha256sum "$path" | awk '{print $1}')"
  [[ "$actual" == "$expected" ]]
}

download_gradle_zip() {
  local version="$1" dest="$2"
  local url="https://services.gradle.org/distributions/gradle-${version}-bin.zip"
  echo "==> Downloading ${url}"
  rm -f "$dest"
  curl -fL --retry 3 --retry-delay 2 --connect-timeout 30 --max-time 300 -o "$dest" "$url"
}

stage_gradle_distribution() {
  local version dest expected gradle_home dist_dir cached
  version="$(gradle_wrapper_version)"
  expected="$(gradle_wrapper_sha256)"
  dest="$ROOT/gradle/wrapper/gradle-${version}-bin.zip"

  if [[ -f "$dest" ]] && gradle_zip_sha_ok "$dest" "$expected"; then
    echo "==> Gradle ${version} zip already staged (SHA-256 ok)"
    return 0
  fi
  if [[ -f "$dest" ]]; then
    echo "==> Staged Gradle zip failed SHA-256; re-downloading"
    rm -f "$dest"
  fi

  echo "==> Staging Gradle ${version} distribution for Docker image builds..."
  for gradle_home in ${GRADLE_USER_HOME:-} "$HOME/.gradle" "$ROOT/.gradle"; do
    [[ -n "$gradle_home" ]] || continue
    dist_dir="$gradle_home/wrapper/dists/gradle-${version}-bin"
    [[ -d "$dist_dir" ]] || continue
    cached="$(find "$dist_dir" -name "gradle-${version}-bin.zip" -size +1M 2>/dev/null | head -n 1 || true)"
    if [[ -n "$cached" ]]; then
      cp "$cached" "$dest"
      if gradle_zip_sha_ok "$dest" "$expected"; then
        echo "==> Copied ${cached}"
        return 0
      fi
      echo "==> Cached zip failed SHA-256; ignoring ${cached}"
      rm -f "$dest"
    fi
  done

  download_gradle_zip "$version" "$dest"
  if ! gradle_zip_sha_ok "$dest" "$expected"; then
    rm -f "$dest"
    echo "Gradle ${version} zip SHA-256 mismatch after download" >&2
    exit 1
  fi
}

ensure_demo_env() {
  local example dest key existing line
  for name in demo.env demo-auth.env demo-farm.env demo-operation.env; do
    example="$ROOT/deploy/compose/${name}.example"
    dest="$ROOT/deploy/compose/${name}"
    if [[ ! -f "$example" ]]; then
      echo "Missing deploy/compose/${name}.example" >&2
      exit 1
    fi
    if [[ ! -f "$dest" ]]; then
      cp "$example" "$dest"
      echo "==> Copied deploy/compose/${name}.example -> deploy/compose/${name}"
      continue
    fi
    # Append keys present in the example but missing from the runtime env (do not overwrite).
    existing="$(
      grep -E '^[A-Za-z_][A-Za-z0-9_]*=' "$dest" 2>/dev/null | cut -d= -f1 | tr 'a-z' 'A-Z' || true
    )"
    appended=0
    while IFS= read -r line || [[ -n "$line" ]]; do
      if [[ "$line" =~ ^([A-Za-z_][A-Za-z0-9_]*)= ]]; then
        key="$(printf '%s\n' "${BASH_REMATCH[1]}" | tr 'a-z' 'A-Z')"
        if ! printf '%s\n' "$existing" | grep -qx "$key"; then
          if [[ $appended -eq 0 ]]; then
            printf '\n# Added by compose-up from %s\n' "deploy/compose/${name}.example" >> "$dest"
          fi
          printf '%s\n' "$line" >> "$dest"
          existing="${existing}"$'\n'"${key}"
          appended=$((appended + 1))
        fi
      fi
    done < "$example"
    if [[ $appended -gt 0 ]]; then
      echo "==> Appended ${appended} missing key(s) to deploy/compose/${name}"
    fi
  done
}

buildable_services() {
  # Infra images have no build: — exclude so we only batch app images.
  local -a infra=(postgres timescaledb rabbitmq redis minio kafka)
  local -a all_services=()
  local -a out=()
  local name skip
  mapfile -t all_services < <(docker compose "$@" config --services)
  for name in "${all_services[@]}"; do
    skip=0
    for i in "${infra[@]}"; do
      [[ "$name" == "$i" ]] && { skip=1; break; }
    done
    [[ $skip -eq 1 ]] || out+=("$name")
  done
  if [[ ${#out[@]} -eq 0 ]]; then
    echo "No buildable services for profile" >&2
    exit 1
  fi
  printf '%s\n' "${out[@]}"
}

build_app_images() {
  # Compose v5 / Bake often ignores --parallel and builds every target at once.
  # Concurrent Gradle JVM image builds then OOM Docker Desktop BuildKit (RPC EOF).
  local batch_size=2
  local -a services=()
  local -a batch=()
  local i n total attempt

  mapfile -t services < <(buildable_services "$@")
  total="${#services[@]}"
  echo "==> Building app images profile=${PROFILE} (${total} services, batches of ${batch_size})..."

  for ((i = 0; i < total; i += batch_size)); do
    batch=("${services[@]:i:batch_size}")
    n=$((i + ${#batch[@]}))
    echo "==> Building images $((i + 1))-${n}/${total}: ${batch[*]}"
    attempt=0
    while true; do
      attempt=$((attempt + 1))
      if docker compose "$@" build "${batch[@]}"; then
        break
      fi
      if [[ $attempt -ge 2 ]]; then
        echo "app image build failed (${batch[*]})" >&2
        exit 1
      fi
      echo "==> BuildKit failed; retrying batch once after brief pause..."
      sleep 5
    done
  done
}

ensure_domain_databases() {
  if [[ "$PROFILE" != "all" && "$PROFILE" != "domains" && "$PROFILE" != "core" ]]; then
    return 0
  fi
  echo "==> Ensuring domain databases (profile=${PROFILE})..."
  local attempt=0
  until docker compose --project-directory "$ROOT" -f docker-compose.yml exec -T postgres \
    sh -c 'pg_isready -U "$POSTGRES_USER"'; do
    attempt=$((attempt + 1))
    if [[ $attempt -ge 60 ]]; then
      echo "postgres did not become ready" >&2
      exit 1
    fi
    sleep 2
  done
  docker compose --project-directory "$ROOT" -f docker-compose.yml cp \
    scripts/ensure-new-dbs.sql postgres:/tmp/ensure-new-dbs.sql
  docker compose --project-directory "$ROOT" -f docker-compose.yml exec -T postgres \
    sh -c 'PGPASSWORD="$POSTGRES_PASSWORD" psql -U "$POSTGRES_USER" -d postgres -v ON_ERROR_STOP=1 -f /tmp/ensure-new-dbs.sql'
}

stage_gradle_distribution
ensure_demo_env

echo "==> Starting infra (postgres, timescaledb, rabbitmq, redis, minio, kafka)..."
docker compose --project-directory "$ROOT" -f docker-compose.yml up -d
ensure_domain_databases

# Prefer classic compose build path; Bake ignores COMPOSE_PARALLEL_LIMIT on Compose v5.
export COMPOSE_BAKE=false
export COMPOSE_PARALLEL_LIMIT=2

COMPOSE_ARGS=(
  --project-directory "$ROOT"
  -f docker-compose.yml
  -f deploy/compose/stack.yml
  --env-file deploy/compose/demo.env
  --profile "$PROFILE"
)

build_app_images "${COMPOSE_ARGS[@]}"

echo "==> Starting apps profile=${PROFILE}..."
docker compose "${COMPOSE_ARGS[@]}" --parallel 4 up -d --pull never --no-build

echo
echo "Profile  ${PROFILE}"
echo "Gateway  http://localhost:8080"
if [[ "$PROFILE" == "core" || "$PROFILE" == "all" ]]; then
  echo "Web      http://localhost:5173"
fi
echo
echo "App users (password Precision@123):"
echo "  admin@precisionfarming.demo        Admin"
echo "  manager@precisionfarming.demo      Gerente"
echo "  operator@precisionfarming.demo     Operador"
echo "  maintenance@precisionfarming.demo  Manutencao"
echo
echo "Infra (local only, 127.0.0.1):"
echo "  Postgres   precision / precision       :5432"
echo "  Timescale  precision / precision       :5433"
echo "  RabbitMQ   precision / precision       :5672  UI :15672"
echo "  Redis      no password                 :6379"
echo "  MinIO      precision / precisionminio  :9000  console :9001"
echo "  Kafka      PLAINTEXT, no user          :9092"
echo
echo "Optional (deploy/compose/demo.env):"
echo "  KAFKA_ENABLED=false          publish precision.operation.started (operation uses kafka:9094)"
echo "  MAPA_LIVE=false              live ZARC from MAPA CKAN (weather; seed fallback)"
echo "  WEATHER_PROVIDER=demo        set open-meteo for live forecast"
if [[ "$PROFILE" == "core" ]]; then
  echo
  echo "Tip: core leaves weather, agronomy, irrigation, harvest, compliance, AI, notification, file, sync, and integration down. Profile 'all' starts them. Profiles core, domains, and all apply scripts/ensure-new-dbs.sql."
fi
echo "Done."
