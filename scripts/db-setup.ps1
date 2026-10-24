# Runs a SQL file against the Neon database (default: db/schema.sql, which creates the tables).
# Uses the same environment variables as the app: DB_URL, DB_USERNAME, DB_PASSWORD.
#
#   .\scripts\db-setup.ps1
#   .\scripts\db-setup.ps1 -SqlFile db\some-other-script.sql

param(
    [string]$SqlFile = (Join-Path $PSScriptRoot "..\db\schema.sql")
)

$ErrorActionPreference = "Stop"

foreach ($name in "DB_URL", "DB_USERNAME", "DB_PASSWORD") {
    if (-not [Environment]::GetEnvironmentVariable($name)) {
        throw "$name is not set. See the README for how to set it."
    }
}

# DB_URL looks like jdbc:postgresql://host[:port]/database?sslmode=require
if ($env:DB_URL -notmatch '^jdbc:postgresql://([^/:?]+)(?::(\d+))?/([^?]+)') {
    throw "DB_URL should look like jdbc:postgresql://<host>/<database>?sslmode=require"
}
$dbHost = $Matches[1]
$dbPort = if ($Matches[2]) { $Matches[2] } else { "5432" }
$dbName = $Matches[3]

# Use psql from PATH, or else the newest PostgreSQL install in Program Files.
$psql = (Get-Command psql -ErrorAction SilentlyContinue).Source
if (-not $psql) {
    $psql = Get-ChildItem "$env:ProgramFiles\PostgreSQL\*\bin\psql.exe" -ErrorAction SilentlyContinue |
        Sort-Object { [int]$_.Directory.Parent.Name } -Descending |
        Select-Object -First 1 -ExpandProperty FullName
}
if (-not $psql) {
    throw "psql not found. Install PostgreSQL client tools, or paste the SQL file into the Neon SQL Editor instead."
}

# psql reads the connection from these variables, so the password never appears on the command line.
$env:PGHOST = $dbHost
$env:PGPORT = $dbPort
$env:PGDATABASE = $dbName
$env:PGUSER = $env:DB_USERNAME
$env:PGPASSWORD = $env:DB_PASSWORD
$env:PGSSLMODE = "require"

Write-Host "Running $SqlFile on $dbName at $dbHost ..."
& $psql -v ON_ERROR_STOP=1 -f $SqlFile
if ($LASTEXITCODE -ne 0) {
    throw "psql failed with exit code $LASTEXITCODE"
}
Write-Host "Done."
