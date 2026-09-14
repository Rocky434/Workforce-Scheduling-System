@echo off
start "Scheduling Backend" powershell.exe -NoExit -ExecutionPolicy Bypass -File "%~dp0scripts\start-backend.ps1"
start "Scheduling Frontend" powershell.exe -NoExit -ExecutionPolicy Bypass -File "%~dp0scripts\start-frontend.ps1"
echo Development servers are starting.
echo Frontend: http://localhost:5173
echo Backend:  http://localhost:8080
