# Start Precision Farming local demo stack on Windows (infra + app containers).
# Mobile is excluded. Requires Docker Compose v2.
#
# Usage:
#   .\scripts\compose-up.ps1              # profile=core, no rebuild
#   .\scripts\compose-up.ps1 -Build       # rebuild images
#   .\scripts\compose-up.ps1 core -Build
#   .\scripts\compose-up.ps1 all
#
# Profiles: core | all | fleet | ops | domains

param(
  [Parameter(Position = 0)]
  [ValidateSet("core", "all", "fleet", "ops", "domains")]
  [string]$Profile = "core",
  [Alias("b")]
  [switch]$Build,
  [Alias("h")]
  [switch]$Help
)

$ErrorActionPreference = "Stop"

if ($Help) {
  Get-Content $PSCommandPath | Select-Object -Skip 1 -First 13
  exit 0
}

$Root = (Resolve-Path "$PSScriptRoot\..").Path
Set-Location $Root

Write-Host "==> Starting infra (postgres, timescaledb, rabbitmq, redis, minio)..."
docker compose --project-directory $Root -f docker-compose.yml up -d
if ($LASTEXITCODE -ne 0) { throw "infra compose failed" }

$buildFlag = @()
if ($Build) { $buildFlag = @("--build") }

Write-Host "==> Starting apps profile=$Profile..."
docker compose `
  --project-directory $Root `
  -f docker-compose.yml `
  -f deploy/compose/stack.yml `
  --env-file deploy/compose/demo.env `
  --profile $Profile `
  up -d @buildFlag

if ($LASTEXITCODE -ne 0) { throw "app compose failed" }

Write-Host ""
Write-Host "Gateway  http://localhost:8080"
if ($Profile -eq "core" -or $Profile -eq "all") {
  Write-Host "Web      http://localhost:5173"
}
Write-Host "Demo     manager@precisionfarming.demo / Precision@123"
Write-Host "Done."
