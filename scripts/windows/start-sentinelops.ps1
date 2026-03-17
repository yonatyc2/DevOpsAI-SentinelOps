$ErrorActionPreference = "Stop"

$projectRoot = "D:\Vibe Coding Project\DEVOPS Agent"
$backendDir = Join-Path $projectRoot "backend"
$frontendDir = Join-Path $projectRoot "frontend"
$logDir = Join-Path $projectRoot "logs"

if (-not (Test-Path $logDir)) {
    New-Item -Path $logDir -ItemType Directory | Out-Null
}

function Test-PortListening {
    param([int]$Port)
    $conn = Get-NetTCPConnection -State Listen -LocalPort $Port -ErrorAction SilentlyContinue
    return $null -ne $conn
}

function Start-Backend {
    if (Test-PortListening -Port 8080) {
        return
    }

    Start-Process -FilePath "powershell.exe" -ArgumentList @(
        "-NoExit",
        "-ExecutionPolicy", "Bypass",
        "-Command",
        "Set-Location -Path '$backendDir'; mvn spring-boot:run *>> '$logDir\backend-startup.log'"
    ) -WindowStyle Minimized
}

function Start-Frontend {
    if (Test-PortListening -Port 5557) {
        return
    }

    Start-Process -FilePath "powershell.exe" -ArgumentList @(
        "-NoExit",
        "-ExecutionPolicy", "Bypass",
        "-Command",
        "Set-Location -Path '$frontendDir'; npm run dev *>> '$logDir\frontend-startup.log'"
    ) -WindowStyle Minimized
}

Start-Backend
Start-Frontend
