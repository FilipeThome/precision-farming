param(
  [string]$Gradle = "$PSScriptRoot\..\gradlew.bat"
)
$root = Resolve-Path "$PSScriptRoot\.."
Set-Location $root
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
  "backend:gateway"
)
foreach ($m in $modules) {
  Write-Host "Starting $m"
  Start-Process -FilePath $Gradle -ArgumentList ":$m:bootRun" -WorkingDirectory $root
}
Write-Host "Services launching. Gateway http://localhost:8080"
