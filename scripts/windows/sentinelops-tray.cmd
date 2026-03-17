@echo off
set SCRIPT_PATH=D:\Vibe Coding Project\DEVOPS Agent\scripts\windows\sentinelops-tray.ps1
start "" powershell.exe -NoProfile -WindowStyle Hidden -ExecutionPolicy Bypass -File "%SCRIPT_PATH%"
