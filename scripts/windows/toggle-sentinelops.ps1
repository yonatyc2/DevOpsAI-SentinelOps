$ErrorActionPreference = "Stop"

$projectRoot = "D:\Vibe Coding Project\DEVOPS Agent"
$scriptsDir = Join-Path $projectRoot "scripts\windows"
$startScript = Join-Path $scriptsDir "start-sentinelops.ps1"
$stopScript = Join-Path $scriptsDir "stop-sentinelops.ps1"

$isBackendRunning = $null -ne (Get-NetTCPConnection -State Listen -LocalPort 8080 -ErrorAction SilentlyContinue)
$isFrontendRunning = $null -ne (Get-NetTCPConnection -State Listen -LocalPort 5557 -ErrorAction SilentlyContinue)

if ($isBackendRunning -or $isFrontendRunning) {
    & powershell.exe -NoProfile -ExecutionPolicy Bypass -File $stopScript
} else {
    & powershell.exe -NoProfile -ExecutionPolicy Bypass -File $startScript
}
