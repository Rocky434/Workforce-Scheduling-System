$ErrorActionPreference = 'Stop'
$root = Split-Path $PSScriptRoot -Parent
$env:Path = 'C:\Users\necro\AppData\Local\DevTools\node-v24.21.0-win-x64;' + $env:Path
Set-Location "$root\frontend"
& npm.cmd run dev
