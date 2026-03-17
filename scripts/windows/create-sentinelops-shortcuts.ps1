$ErrorActionPreference = "Stop"

$projectRoot = "D:\Vibe Coding Project\DEVOPS Agent"
$scriptsDir = Join-Path $projectRoot "scripts\windows"
$desktopPath = [Environment]::GetFolderPath("Desktop")
$wsh = New-Object -ComObject WScript.Shell

function New-Shortcut {
    param(
        [string]$ShortcutName,
        [string]$TargetPath
    )

    $shortcutPath = Join-Path $desktopPath "$ShortcutName.lnk"
    $shortcut = $wsh.CreateShortcut($shortcutPath)
    $shortcut.TargetPath = $TargetPath
    $shortcut.WorkingDirectory = $projectRoot
    $shortcut.WindowStyle = 7
    $shortcut.Save()
}

New-Shortcut -ShortcutName "SentinelOps Start" -TargetPath (Join-Path $scriptsDir "start-sentinelops.cmd")
New-Shortcut -ShortcutName "SentinelOps Stop" -TargetPath (Join-Path $scriptsDir "stop-sentinelops.cmd")
New-Shortcut -ShortcutName "SentinelOps Toggle" -TargetPath (Join-Path $scriptsDir "toggle-sentinelops.cmd")
New-Shortcut -ShortcutName "SentinelOps Tray" -TargetPath (Join-Path $scriptsDir "sentinelops-tray.cmd")

Write-Host "Desktop shortcuts created: SentinelOps Start / Stop / Toggle / Tray"
