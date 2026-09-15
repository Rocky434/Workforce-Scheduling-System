$ErrorActionPreference = 'Stop'
$root = Split-Path $PSScriptRoot -Parent
Set-Location "$root\frontend"

if (-not (Get-Command node -ErrorAction SilentlyContinue)) {
    throw 'Node.js was not found. Install Node.js LTS first.'
}

if (-not (Test-Path -LiteralPath '.\node_modules\.bin\vite.cmd')) {
    & npm.cmd ci
}
& npm.cmd run dev
