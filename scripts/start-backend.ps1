$ErrorActionPreference = 'Stop'
$root = Split-Path $PSScriptRoot -Parent

$envFile = Join-Path $root '.env'
if (-not (Test-Path -LiteralPath $envFile)) {
    throw "Missing $envFile. Copy .env.example to .env and fill in the settings."
}

Get-Content -LiteralPath $envFile | ForEach-Object {
    $line = $_.Trim()
    if ($line -and -not $line.StartsWith('#')) {
        $pair = $line -split '=', 2
        if ($pair.Count -eq 2) {
            $name = $pair[0].Trim()
            $value = $pair[1].Trim().Trim([char]34).Trim([char]39)
            [Environment]::SetEnvironmentVariable($name, $value, 'Process')
        }
    }
}

if (-not (Test-NetConnection -ComputerName localhost -Port 5432 -InformationLevel Quiet)) {
    $postgresService = Get-Service -Name 'postgresql-x64-18' -ErrorAction SilentlyContinue
    if (-not $postgresService) {
        throw 'PostgreSQL service was not found. Install PostgreSQL first.'
    }

    try {
        Start-Service -Name $postgresService.Name
    } catch {
        throw "Could not start the PostgreSQL service. Run PowerShell as Administrator and try again. Details: $($_.Exception.Message)"
    }

    $deadline = (Get-Date).AddSeconds(20)
    while ((Get-Date) -lt $deadline) {
        if (Test-NetConnection -ComputerName localhost -Port 5432 -InformationLevel Quiet) {
            break
        }
        Start-Sleep -Milliseconds 500
    }

    if (-not (Test-NetConnection -ComputerName localhost -Port 5432 -InformationLevel Quiet)) {
        throw 'PostgreSQL started, but port 5432 was not ready within 20 seconds.'
    }
}
Set-Location "$root\backend"
& .\mvnw.cmd spring-boot:run
