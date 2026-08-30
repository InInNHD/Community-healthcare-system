$ErrorActionPreference = 'Stop'

$apiBaseUrl = if ($env:API_BASE_URL) { $env:API_BASE_URL.TrimEnd('/') } else { 'http://localhost:8080' }
$adminPassword = if ($env:ADMIN_PASSWORD) { $env:ADMIN_PASSWORD } else { 'Admin@123456' }
$loginBody = @{
    username = 'admin'
    password = $adminPassword
    portal = 'admin'
} | ConvertTo-Json

$session = Invoke-RestMethod -Method Post -Uri "$apiBaseUrl/api/auth/login" -ContentType 'application/json' -Body $loginBody
$headers = @{ Authorization = "Bearer $($session.accessToken)" }
$contractPath = Join-Path $PSScriptRoot '..\openapi.json'
$outputPath = Join-Path $PSScriptRoot '..\src\api\generated'

$contract = Invoke-RestMethod -Method Get -Uri "$apiBaseUrl/v3/api-docs" -Headers $headers
# Keep generated clients environment-neutral even when the contract is downloaded
# from a temporary demo port. Runtime configuration supplies the deployment origin.
$contract.servers = @(@{ url = '' })
$contract | ConvertTo-Json -Depth 100 | Set-Content -LiteralPath $contractPath -Encoding utf8

& npx.cmd openapi --input $contractPath --output $outputPath --client axios --useOptions
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }

Write-Host "Generated API client at $outputPath"
