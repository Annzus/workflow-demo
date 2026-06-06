param(
    [string]$ComposeFile = (Join-Path $PSScriptRoot "..\docker-compose.yml"),
    [string]$SqlFile = (Join-Path $PSScriptRoot "reset-demo-applications.sql"),
    [string]$MinioBucket = "workflow-attachments"
)

$ErrorActionPreference = "Stop"

$composePath = (Resolve-Path -LiteralPath $ComposeFile).Path
$sqlPath = (Resolve-Path -LiteralPath $SqlFile).Path

Get-Content -Raw -LiteralPath $sqlPath |
    docker compose -f $composePath exec -T postgres psql -U workflow -d workflow_demo

if ($LASTEXITCODE -ne 0) {
    throw "Failed to reset PostgreSQL demo application data."
}

$minioResetCommand = 'mc alias set local http://127.0.0.1:9000 workflow workflow-secret >/dev/null && (mc rm --recursive --force "local/$WORKFLOW_MINIO_BUCKET" >/dev/null 2>&1 || true) && mc mb --ignore-existing "local/$WORKFLOW_MINIO_BUCKET" >/dev/null'

docker run --rm `
    --network container:workflow-demo-minio `
    -e "WORKFLOW_MINIO_BUCKET=$MinioBucket" `
    minio/mc:latest `
    sh -c $minioResetCommand

if ($LASTEXITCODE -ne 0) {
    throw "Failed to reset MinIO demo attachment objects."
}

Write-Host "Demo application rows and attachment objects were reset."
