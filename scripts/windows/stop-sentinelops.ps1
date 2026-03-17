$ErrorActionPreference = "Stop"

function Stop-PortProcess {
    param([int]$Port)

    $connections = Get-NetTCPConnection -State Listen -LocalPort $Port -ErrorAction SilentlyContinue
    if ($null -eq $connections) {
        return $false
    }

    $processIds = $connections | Select-Object -ExpandProperty OwningProcess -Unique
    foreach ($processId in $processIds) {
        Stop-Process -Id $processId -Force -ErrorAction SilentlyContinue
    }

    return $true
}

$backendStopped = Stop-PortProcess -Port 8080
$frontendStopped = Stop-PortProcess -Port 5557

if (-not $backendStopped -and -not $frontendStopped) {
    Write-Host "SentinelOps is already stopped."
} else {
    Write-Host "SentinelOps services stopped."
}
