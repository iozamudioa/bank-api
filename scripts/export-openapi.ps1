$ErrorActionPreference = 'Stop'
Push-Location (Join-Path $PSScriptRoot '..')
try {
    mvn '-Dtest=OpenApiContractTest' '-Dbank.openapi.export=true' test
    if ($LASTEXITCODE -ne 0) { throw 'No se pudo generar o validar OpenAPI.' }
    Write-Output 'Contrato generado y verificado: docs/openapi/bank-api-v1.yaml'
} finally {
    Pop-Location
}
