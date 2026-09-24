# Start Precision Farming local demo stack on Windows (infra + app containers).
# Mobile is excluded. Requires Docker Compose v2.
# Infra includes Kafka (PLAINTEXT, 127.0.0.1:9092). App publish stays off unless KAFKA_ENABLED=true.
#
# Usage:
#   .\scripts\compose-up.ps1              # profile=core
#   .\scripts\compose-up.ps1 -Build       # same (cache is used; flag kept for compatibility)
#   .\scripts\compose-up.ps1 core -Build
#   .\scripts\compose-up.ps1 all          # investor loop: weather + agronomy + compliance
#
# Profiles: core | all | fleet | ops | domains
# Optional flags in deploy/compose/demo.env (copied from *.example on first run):
#   KAFKA_ENABLED=true   MAPA_LIVE=true   WEATHER_PROVIDER=open-meteo

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
  Get-Content $PSCommandPath | Select-Object -Skip 1 -First 16
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
  $gradleHomes = @()
  if ($env:GRADLE_USER_HOME) { $gradleHomes += $env:GRADLE_USER_HOME }
  $gradleHomes += (Join-Path $env:USERPROFILE ".gradle")
  $gradleHomes += (Join-Path $Root ".gradle")

  # Do not use $home — PowerShell aliases it to read-only automatic $HOME.
  foreach ($gradleHome in $gradleHomes) {
    $distDir = Join-Path $gradleHome "wrapper\dists\gradle-$version-bin"
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
      continue
    }
    # Append keys present in the example but missing from the runtime env (do not overwrite).
    $existing = @(
      Get-Content $dest | ForEach-Object {
        if ($_ -match '^\s*([A-Za-z_][A-Za-z0-9_]*)=') { $Matches[1] }
      } | Where-Object { $_ } | ForEach-Object { $_.ToUpperInvariant() }
    )
    $existingSet = [System.Collections.Generic.HashSet[string]]::new()
    foreach ($key in $existing) { [void]$existingSet.Add($key) }
    $appended = @()
    foreach ($line in Get-Content $example) {
      if ($line -match '^\s*([A-Za-z_][A-Za-z0-9_]*)=') {
        $key = $Matches[1]
        if (-not $existingSet.Contains($key.ToUpperInvariant())) {
          $appended += $line
          [void]$existingSet.Add($key.ToUpperInvariant())
        }
      }
    }
    if ($appended.Count -gt 0) {
      Add-Content -Path $dest -Value ""
      Add-Content -Path $dest -Value "# Added by compose-up from $($pair.Example)"
      Add-Content -Path $dest -Value $appended
      Write-Host "==> Appended $($appended.Count) missing key(s) to $($pair.Dest)"
    }
  }
}

function Get-BuildableServices([string[]]$ComposeArgs) {
  # Infra images have no build: — exclude so we only batch app images.
  $infra = [System.Collections.Generic.HashSet[string]]::new(
    [string[]]@("postgres", "timescaledb", "rabbitmq", "redis", "minio", "kafka")
  )
  $names = docker compose @ComposeArgs config --services
  if ($LASTEXITCODE -ne 0) { throw "compose config --services failed" }
  $services = @($names | Where-Object { $_ -and -not $infra.Contains($_) })
  if ($services.Count -eq 0) { throw "No buildable services for profile" }
  return $services
}

function Build-AppImages([string[]]$ComposeArgs, [string[]]$Services, [int]$BatchSize = 2) {
  # Compose v5 / Bake often ignores --parallel and builds every target at once.
  # Concurrent Gradle JVM image builds then OOM Docker Desktop BuildKit (RPC EOF).
  $total = $Services.Count
  for ($i = 0; $i -lt $total; $i += $BatchSize) {
    $end = [Math]::Min($i + $BatchSize - 1, $total - 1)
    $batch = @($Services[$i..$end])
    Write-Host "==> Building images $($i + 1)-$($end + 1)/${total}: $($batch -join ', ')"
    $attempt = 0
    while ($true) {
      $attempt++
      docker compose @ComposeArgs build @batch
      if ($LASTEXITCODE -eq 0) { break }
      if ($attempt -ge 2) {
        throw "app image build failed ($($batch -join ', '))"
      }
      Write-Host "==> BuildKit failed; retrying batch once after brief pause..."
      Start-Sleep -Seconds 5
    }
  }
}

Stage-GradleDistribution
Ensure-DemoEnv

Write-Host "==> Starting infra (postgres, timescaledb, rabbitmq, redis, minio, kafka)..."
docker compose --project-directory $Root -f docker-compose.yml up -d
if ($LASTEXITCODE -ne 0) { throw "infra compose failed" }

# Prefer classic compose build path; Bake ignores COMPOSE_PARALLEL_LIMIT on Compose v5.
$env:COMPOSE_BAKE = "false"
$env:COMPOSE_PARALLEL_LIMIT = "2"

$composeArgs = @(
  "--project-directory", $Root,
  "-f", "docker-compose.yml",
  "-f", "deploy/compose/stack.yml",
  "--env-file", "deploy/compose/demo.env",
  "--profile", $Profile
)

$services = Get-BuildableServices -ComposeArgs $composeArgs
Write-Host "==> Building app images profile=$Profile ($($services.Count) services, batches of 2)..."
Build-AppImages -ComposeArgs $composeArgs -Services $services -BatchSize 2

Write-Host "==> Starting apps profile=$Profile..."
docker compose @composeArgs --parallel 4 up -d --pull never --no-build
if ($LASTEXITCODE -ne 0) { throw "app compose failed" }

Write-Host ""
Write-Host "Profile  $Profile"
Write-Host "Gateway  http://localhost:8080"
if ($Profile -eq "core" -or $Profile -eq "all") {
  Write-Host "Web      http://localhost:5173"
}
Write-Host ""
Write-Host "App users (password Precision@123):"
Write-Host "  admin@precisionfarming.demo        Admin"
Write-Host "  manager@precisionfarming.demo      Gerente"
Write-Host "  operator@precisionfarming.demo     Operador"
Write-Host "  maintenance@precisionfarming.demo  Manutencao"
Write-Host ""
Write-Host "Infra (local only, 127.0.0.1):"
Write-Host "  Postgres   precision / precision       :5432"
Write-Host "  Timescale  precision / precision       :5433"
Write-Host "  RabbitMQ   precision / precision       :5672  UI :15672"
Write-Host "  Redis      no password                 :6379"
Write-Host "  MinIO      precision / precisionminio  :9000  console :9001"
Write-Host "  Kafka      PLAINTEXT, no user          :9092"
Write-Host ""
Write-Host "Optional (deploy/compose/demo.env):"
Write-Host "  KAFKA_ENABLED=false          publish precision.operation.started (operation uses kafka:9094)"
Write-Host "  MAPA_LIVE=false              live ZARC from MAPA CKAN (weather; seed fallback)"
Write-Host "  WEATHER_PROVIDER=demo        set open-meteo for live forecast"
if ($Profile -eq "core") {
  Write-Host ""
  Write-Host "Tip: investor demo needs profile 'all' (weather, agronomy, compliance)."
}
Write-Host "Done."
