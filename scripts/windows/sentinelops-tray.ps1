$ErrorActionPreference = "Stop"

$projectRoot = "D:\Vibe Coding Project\DEVOPS Agent"
$scriptsDir = Join-Path $projectRoot "scripts\windows"
$startScript = Join-Path $scriptsDir "start-sentinelops.ps1"
$stopScript = Join-Path $scriptsDir "stop-sentinelops.ps1"
$toggleScript = Join-Path $scriptsDir "toggle-sentinelops.ps1"

Add-Type -AssemblyName System.Windows.Forms
Add-Type -AssemblyName System.Drawing

function Get-IsRunning {
    $backendRunning = $null -ne (Get-NetTCPConnection -State Listen -LocalPort 8080 -ErrorAction SilentlyContinue)
    $frontendRunning = $null -ne (Get-NetTCPConnection -State Listen -LocalPort 5557 -ErrorAction SilentlyContinue)
    return ($backendRunning -or $frontendRunning)
}

function Invoke-Action {
    param([string]$ScriptPath)

    Start-Process -FilePath "powershell.exe" -ArgumentList @(
        "-NoProfile",
        "-ExecutionPolicy", "Bypass",
        "-File", $ScriptPath
    ) -WindowStyle Minimized
}

$notifyIcon = New-Object System.Windows.Forms.NotifyIcon
$notifyIcon.Icon = [System.Drawing.SystemIcons]::Application
$notifyIcon.Visible = $true
$notifyIcon.Text = "SentinelOps Control"

$menu = New-Object System.Windows.Forms.ContextMenuStrip

$statusItem = New-Object System.Windows.Forms.ToolStripMenuItem
$statusItem.Enabled = $false
$null = $menu.Items.Add($statusItem)
$null = $menu.Items.Add("-")

$startItem = New-Object System.Windows.Forms.ToolStripMenuItem
$startItem.Text = "Start"
$startItem.Add_Click({ Invoke-Action -ScriptPath $startScript })
$null = $menu.Items.Add($startItem)

$stopItem = New-Object System.Windows.Forms.ToolStripMenuItem
$stopItem.Text = "Stop"
$stopItem.Add_Click({ Invoke-Action -ScriptPath $stopScript })
$null = $menu.Items.Add($stopItem)

$toggleItem = New-Object System.Windows.Forms.ToolStripMenuItem
$toggleItem.Text = "Toggle"
$toggleItem.Add_Click({ Invoke-Action -ScriptPath $toggleScript })
$null = $menu.Items.Add($toggleItem)

$null = $menu.Items.Add("-")

$exitItem = New-Object System.Windows.Forms.ToolStripMenuItem
$exitItem.Text = "Exit"
$exitItem.Add_Click({
    $notifyIcon.Visible = $false
    $notifyIcon.Dispose()
    [System.Windows.Forms.Application]::Exit()
})
$null = $menu.Items.Add($exitItem)

function Update-Status {
    $running = Get-IsRunning
    if ($running) {
        $statusItem.Text = "Status: Running"
        $notifyIcon.Text = "SentinelOps Control - Running"
    } else {
        $statusItem.Text = "Status: Stopped"
        $notifyIcon.Text = "SentinelOps Control - Stopped"
    }
}

$timer = New-Object System.Windows.Forms.Timer
$timer.Interval = 3000
$timer.Add_Tick({ Update-Status })
$timer.Start()

$notifyIcon.ContextMenuStrip = $menu
$notifyIcon.Add_MouseClick({
    param($sender, $args)
    if ($args.Button -eq [System.Windows.Forms.MouseButtons]::Left) {
        $menu.Show([System.Windows.Forms.Cursor]::Position)
    }
})

Update-Status
[System.Windows.Forms.Application]::Run()
