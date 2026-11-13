$ErrorActionPreference = 'Stop'
$root = Resolve-Path (Join-Path $PSScriptRoot '..')
docker compose --env-file (Join-Path $root '.env') -f (Join-Path $PSScriptRoot 'docker-compose.yml') up -d --build --wait
if ($LASTEXITCODE -ne 0) { throw 'Не удалось запустить backend через Docker Compose.' }
Write-Host 'Backend: http://localhost:8080 | Swagger: http://localhost:8080/swagger-ui.html | Health: http://localhost:8080/actuator/health'
