$ErrorActionPreference = 'Stop'
$root = Split-Path $PSScriptRoot -Parent
$credentials = Get-Content 'C:\Users\necro\AppData\Local\SchedulingDev\credentials.json' -Raw | ConvertFrom-Json
$env:DB_PASSWORD = $credentials.appPassword
$env:JAVA_HOME = 'C:\Users\necro\AppData\Local\DevTools\jdk-21.0.12.1+1'
$env:Path = 'C:\Users\necro\AppData\Local\DevTools\apache-maven-3.9.16\bin;' + $env:JAVA_HOME + '\bin;' + $env:Path
if (-not (Get-NetTCPConnection -State Listen -LocalPort 5432 -ErrorAction SilentlyContinue)) {
    & 'C:\Users\necro\AppData\Local\SchedulingDev\start-postgresql.cmd'
}
Set-Location "$root\backend"
& mvn.cmd spring-boot:run
