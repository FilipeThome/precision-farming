param(
  [ValidateSet("core", "all", "fleet", "ops", "domains")]
  [string]$Profile = "core",
  [switch]$Build
)
$ErrorActionPreference = "Stop"
$Root = (Resolve-Path "$PSScriptRoot\..").Path
Set-Location $Root

Write-Host "Starting infra..."
docker compose --project-directory $Root -f docker-compose.yml up -d
if ($LASTEXITCODE -ne 0) { throw "infra compose failed" }

$buildFlag = @()
if ($Build) { $buildFlag = @("--build") }

Write-Host "Starting apps profile=$Profile ..."
docker compose `
  --project-directory $Root `
  -f docker-compose.yml `
  -f deploy/compose/stack.yml `
  --env-file deploy/compose/demo.env `
  --profile $Profile `
  up -d @buildFlag

if ($LASTEXITCODE -ne 0) { throw "app compose failed" }
Write-Host "Gateway http://localhost:8080  Web http://localhost:5173 (if profile includes web)"
Write-Host "Demo login: manager@precisionfarming.demo / Precision@123"
