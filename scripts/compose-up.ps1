# Start Precision Farming local demo stack on Windows (infra + app containers).
# Mobile is excluded. Requires Docker Compose v2.
#
# Usage:
#   .\scripts\compose-up.ps1              # profile=core
#   .\scripts\compose-up.ps1 -Build       # same (cache is used; flag kept for compatibility)
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

function Get-GradleWrapperVersion {
  $line = Get-Content "$Root\gradle\wrapper\gradle-wrapper.properties" |
    Where-Object { $_ -match '^distributionUrl=' } |
    Select-Object -First 1
  if ($line -notmatch 'gradle-([0-9.]+)-bin\.zip') {
    throw "Could not parse Gradle version from gradle-wrapper.properties"
  }
  return $Matches[1]
}

function Get-GradleWrapperSha256 {
  $line = Get-Content "$Root\gradle\wrapper\gradle-wrapper.properties" |
    Where-Object { $_ -match '^distributionSha256Sum=' } |
    Select-Object -First 1
  if (-not $line) {
    throw "distributionSha256Sum missing from gradle-wrapper.properties"
  }
  return ($line -replace '^distributionSha256Sum=', '').Trim().ToLowerInvariant()
}

function Test-GradleZipSha([string]$Path, [string]$Expected) {
  if (-not (Test-Path $Path)) { return $false }
  $actual = (Get-FileHash -Algorithm SHA256 -Path $Path).Hash.ToLowerInvariant()
  return $actual -eq $Expected
}

function Download-GradleZip([string]$Version, [string]$Dest) {
  $url = "https://services.gradle.org/distributions/gradle-$Version-bin.zip"
  Write-Host "==> Downloading $url"
  if (Test-Path $Dest) { Remove-Item $Dest -Force }
  $ProgressPreference = "SilentlyContinue"
  try {
    if (Get-Command curl.exe -ErrorAction SilentlyContinue) {
      & curl.exe -fL --retry 3 --retry-delay 2 --connect-timeout 30 --max-time 300 -o $Dest $url
      if ($LASTEXITCODE -ne 0) { throw "curl exited $LASTEXITCODE" }
    } else {
      Invoke-WebRequest -Uri $url -OutFile $Dest -UseBasicParsing -TimeoutSec 300
    }
  } catch {
    if (Test-Path $Dest) { Remove-Item $Dest -Force }
    throw "Failed to download Gradle $Version zip (needed for Docker image builds): $_"
  }
}

function Stage-GradleDistribution {
  $version = Get-GradleWrapperVersion
  $expected = Get-GradleWrapperSha256
  $dest = Join-Path $Root "gradle\wrapper\gradle-$version-bin.zip"

  if ((Test-Path $dest) -and (Test-GradleZipSha $dest $expected)) {
    Write-Host "==> Gradle $version zip already staged (SHA-256 ok)"
    return
  }
  if (Test-Path $dest) {
    Write-Host "==> Staged Gradle zip failed SHA-256; re-downloading"
    Remove-Item $dest -Force
  }

  Write-Host "==> Staging Gradle $version distribution for Docker image builds..."
  $homes = @()
  if ($env:GRADLE_USER_HOME) { $homes += $env:GRADLE_USER_HOME }
  $homes += (Join-Path $env:USERPROFILE ".gradle")
  $homes += (Join-Path $Root ".gradle")

  foreach ($home in $homes) {
    $distDir = Join-Path $home "wrapper\dists\gradle-$version-bin"
    if (-not (Test-Path $distDir)) { continue }
    $cached = Get-ChildItem -Path $distDir -Recurse -Filter "gradle-$version-bin.zip" -ErrorAction SilentlyContinue |
      Where-Object { $_.Length -gt 1MB } |
      Select-Object -First 1
    if ($cached) {
      Copy-Item $cached.FullName $dest
      if (Test-GradleZipSha $dest $expected) {
        Write-Host "==> Copied $($cached.FullName)"
        return
      }
      Write-Host "==> Cached zip failed SHA-256; ignoring $($cached.FullName)"
      Remove-Item $dest -Force
    }
  }

  Download-GradleZip $version $dest
  if (-not (Test-GradleZipSha $dest $expected)) {
    if (Test-Path $dest) { Remove-Item $dest -Force }
    throw "Gradle $version zip SHA-256 mismatch after download"
  }
}

function Ensure-DemoEnv {
  $pairs = @(
    @{ Example = "deploy\compose\demo.env.example"; Dest = "deploy\compose\demo.env" },
    @{ Example = "deploy\compose\demo-auth.env.example"; Dest = "deploy\compose\demo-auth.env" },
    @{ Example = "deploy\compose\demo-farm.env.example"; Dest = "deploy\compose\demo-farm.env" },
    @{ Example = "deploy\compose\demo-operation.env.example"; Dest = "deploy\compose\demo-operation.env" }
  )
  foreach ($pair in $pairs) {
    $example = Join-Path $Root $pair.Example
    $dest = Join-Path $Root $pair.Dest
    if (-not (Test-Path $example)) {
      throw "Missing $($pair.Example)"
    }
    if (-not (Test-Path $dest)) {
      Copy-Item $example $dest
      Write-Host "==> Copied $($pair.Example) -> $($pair.Dest)"
    }
  }
}

Stage-GradleDistribution
Ensure-DemoEnv

Write-Host "==> Starting infra (postgres, timescaledb, rabbitmq, redis, minio)..."
docker compose --project-directory $Root -f docker-compose.yml up -d
if ($LASTEXITCODE -ne 0) { throw "infra compose failed" }

$env:COMPOSE_BAKE = "false"

$composeArgs = @(
  "--parallel", "4",
  "--project-directory", $Root,
  "-f", "docker-compose.yml",
  "-f", "deploy/compose/stack.yml",
  "--env-file", "deploy/compose/demo.env",
  "--profile", $Profile
)

Write-Host "==> Building app images profile=$Profile (max 4 in parallel)..."
docker compose @composeArgs build
if ($LASTEXITCODE -ne 0) { throw "app image build failed" }

Write-Host "==> Starting apps profile=$Profile..."
docker compose @composeArgs up -d --pull never --no-build
if ($LASTEXITCODE -ne 0) { throw "app compose failed" }

Write-Host ""
Write-Host "Gateway  http://localhost:8080"
if ($Profile -eq "core" -or $Profile -eq "all") {
  Write-Host "Web      http://localhost:5173"
}
Write-Host "Demo     manager@precisionfarming.demo / Precision@123"
Write-Host "Done."
