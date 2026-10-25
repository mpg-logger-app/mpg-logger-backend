# Deploys MPG Logger to the Google Cloud VM: builds both images (backend from this repo, frontend from
# ..\mpg-logger-frontend), pushes them to GitHub's container registry, and restarts the app on the VM.
# Deploys whatever is checked out in both repos, so pull develop in both first. Setup: HOSTING.md.
#
#   .\scripts\deploy.ps1

param(
    [string]$Vm = "mpg-logger",
    [string]$Zone = "us-east1-b"
)

$ErrorActionPreference = "Stop"

$compose = Join-Path $PSScriptRoot "..\deploy\docker-compose.yml"

# Native commands don't stop the script on failure by themselves, so check each exit code.
function Invoke-Step([string]$Description, [scriptblock]$Command) {
    Write-Host $Description
    & $Command
    if ($LASTEXITCODE -ne 0) {
        throw "$Description failed with exit code $LASTEXITCODE"
    }
}

Invoke-Step "Building the images ..." { docker compose -f $compose build }
Invoke-Step "Pushing them to ghcr.io ..." { docker compose -f $compose push }
# Copy the compose file too, so changes to it reach the VM.
Invoke-Step "Copying docker-compose.yml to $Vm ..." {
    gcloud compute scp $compose "${Vm}:mpg-logger/docker-compose.yml" --zone $Zone
}
Invoke-Step "Restarting the app on $Vm ..." {
    gcloud compute ssh $Vm --zone $Zone --command "cd mpg-logger && docker compose pull && docker compose up -d && docker image prune -f"
}
Write-Host "Done. The backend takes about a minute to start; until then the app says it can't connect."
