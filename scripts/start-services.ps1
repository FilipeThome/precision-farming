param(
  [string]$Gradle = "$PSScriptRoot\..\gradlew.bat"
)
$root = Resolve-Path "$PSScriptRoot\.."
Set-Location $root
# Local demo: allow in-repo JWT/DB defaults (fail-closed without this outside profile `local`).
if (-not $env:ALLOW_DEMO_SECRETS) { $env:ALLOW_DEMO_SECRETS = "true" }
if (-not $env:APP_SEED) { $env:APP_SEED = "true" }
$modules = @(
  "backend:services:auth",
  "backend:services:farm",
  "backend:services:asset",
  "backend:services:telemetry",
  "backend:services:weather",
  "backend:services:operation",
  "backend:services:inventory",
  "backend:services:alert",
  "backend:services:ai",
  "backend:services:notification",
  "backend:services:file",
  "backend:services:reporting",
  "backend:services:sync",
  "backend:services:integration",
  "backend:services:agronomy",
  "backend:services:irrigation",
  "backend:services:harvest",
  "backend:services:finance",
  "backend:services:compliance",
  "backend:gateway"
)
foreach ($m in $modules) {
  Write-Host "Starting $m"
  Start-Process -FilePath $Gradle -ArgumentList ":$m:bootRun" -WorkingDirectory $root
}
Write-Host "Services launching. Gateway http://localhost:8080"
