$ErrorActionPreference = 'Stop'
Push-Location (Join-Path $PSScriptRoot '..')
try {
    if (-not (Get-Command docker -ErrorAction SilentlyContinue)) {
        throw 'Docker no está instalado o no está en PATH. No se puede validar atomicidad real.'
    }
    docker info --format '{{.ServerVersion}}'
    if ($LASTEXITCODE -ne 0) { throw 'Docker Engine no está disponible.' }
    mvn '-Dtest=MongoFinanceIntegrationTest' test
    if ($LASTEXITCODE -ne 0) { throw 'Falló la suite MongoDB.' }
    $reportPath = Join-Path (Get-Location) 'target/surefire-reports/TEST-dev.ioza.bank.integration.MongoFinanceIntegrationTest.xml'
    [xml]$report = Get-Content -LiteralPath $reportPath -Raw
    if ([int]$report.testsuite.skipped -gt 0 -or [int]$report.testsuite.tests -lt 9) {
        throw 'La suite MongoDB omitió pruebas: no se considera validada.'
    }
    Write-Output 'Pruebas de transacciones MongoDB ejecutadas sin omisiones.'
} finally {
    Pop-Location
}
