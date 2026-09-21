#!/usr/bin/env bash
# Start Precision Farming local demo stack on Linux (infra + app containers).
# Mobile is excluded. Requires Docker Compose v2.
#
# Usage:
#   ./scripts/compose-up.sh              # profile=core
#   ./scripts/compose-up.sh --build      # same (cache is used; flag kept for compatibility)
#   ./scripts/compose-up.sh core --build
#   ./scripts/compose-up.sh all
#
# Profiles: core | all | fleet | ops | domains

set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT"

PROFILE="core"
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
  local example dest
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
    fi
  done
}

stage_gradle_distribution
ensure_demo_env

echo "==> Starting infra (postgres, timescaledb, rabbitmq, redis, minio)..."
docker compose --project-directory "$ROOT" -f docker-compose.yml up -d

export COMPOSE_BAKE=false

COMPOSE_ARGS=(
  --parallel 4
  --project-directory "$ROOT"
  -f docker-compose.yml
  -f deploy/compose/stack.yml
  --env-file deploy/compose/demo.env
  --profile "$PROFILE"
)

echo "==> Building app images profile=${PROFILE} (max 4 in parallel)..."
docker compose "${COMPOSE_ARGS[@]}" build

echo "==> Starting apps profile=${PROFILE}..."
docker compose "${COMPOSE_ARGS[@]}" up -d --pull never --no-build

echo
echo "Gateway  http://localhost:8080"
if [[ "$PROFILE" == "core" || "$PROFILE" == "all" ]]; then
  echo "Web      http://localhost:5173"
fi
echo "Demo     manager@precisionfarming.demo / Precision@123"
echo "Done."
