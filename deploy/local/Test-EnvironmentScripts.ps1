# Disposable regression for host credential scripts. No Docker or normal env access.
[CmdletBinding()]
param()
Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'
$fixture = Join-Path ([IO.Path]::GetTempPath()) ('autostrada-env-test-' + [Guid]::NewGuid().ToString('N') + '.env')
try {
    & (Join-Path $PSScriptRoot 'New-LocalEnvironment.ps1') -Path $fixture -Confirm:$false | Out-Null
    $values = @{}
    foreach ($line in Get-Content -LiteralPath $fixture) {
        if ($line -match '^([A-Z0-9_]+)=(.*)$') { $values[$Matches[1]] = $Matches[2] }
    }
    foreach ($name in @('PAYMENT_DB_PASSWORD', 'RABBITMQ_PAYMENT_PASSWORD', 'IDENTITY_GATEWAY_SECRET')) {
        if ($values[$name] -notmatch '^[0-9a-f]{64}$') { throw "Invalid generated secret format for $name." }
    }
    $jwk = $values.IDENTITY_SIGNING_JWK | ConvertFrom-Json
    $modulus = $jwk.n.Replace('-', '+').Replace('_', '/')
    $modulus = $modulus.PadRight($modulus.Length + (4 - $modulus.Length % 4) % 4, '=')
    if ([Convert]::FromBase64String($modulus).Length -ne 256) { throw 'Expected a 2048-bit RSA key.' }
    if ([Convert]::FromBase64String($values.NOTIFICATION_DELIVERY_KEY).Length -ne 32) { throw 'Expected a 256-bit delivery key.' }
    # Existing host settings must not change when the S8 updater adds missing keys.
    'EXISTING_KEEP=unchanged', 'PAYMENT_DB_PASSWORD=preserve-existing-value' | Set-Content -LiteralPath $fixture -Encoding utf8
    & (Join-Path $PSScriptRoot 'Update-LocalEnvironmentS8.ps1') -Path $fixture -WhatIf | Out-Null
    if (@(Get-Content -LiteralPath $fixture).Count -ne 2) { throw 'WhatIf modified the file.' }
    & (Join-Path $PSScriptRoot 'Update-LocalEnvironmentS8.ps1') -Path $fixture -Confirm:$false | Out-Null
    $firstHash = (Get-FileHash -LiteralPath $fixture).Hash
    & (Join-Path $PSScriptRoot 'Update-LocalEnvironmentS8.ps1') -Path $fixture -Confirm:$false | Out-Null
    if ((Get-FileHash -LiteralPath $fixture).Hash -ne $firstHash) { throw 'Updater is not idempotent.' }
    $lines = @(Get-Content -LiteralPath $fixture)
    foreach ($line in @('EXISTING_KEEP=unchanged', 'PAYMENT_DB_PASSWORD=preserve-existing-value')) {
        if ($lines -notcontains $line) { throw 'Updater changed an existing setting.' }
    }
    foreach ($name in @('PAYMENT_DB_NAME', 'PAYMENT_DB_USERNAME', 'PAYMENT_DB_PASSWORD', 'RABBITMQ_PAYMENT_PASSWORD')) {
        if (@($lines | Where-Object { $_ -match "^$name=" }).Count -ne 1) { throw "Expected one $name setting." }
    }
    . (Join-Path $PSScriptRoot 'Common.ps1')
    foreach ($path in @('C:\deployment\state', '\\server\share\state')) {
        if (-not (Test-WindowsAbsolutePath $path)) { throw 'Absolute path rejected.' }
    }
    foreach ($path in @('state', 'C:state', '\state', '')) {
        if (Test-WindowsAbsolutePath $path) { throw 'Relative path accepted.' }
    }
    Write-Output "Environment script regression passed on PowerShell $($PSVersionTable.PSVersion)."
} finally {
    if (Test-Path -LiteralPath $fixture) { Remove-Item -LiteralPath $fixture -Force }
}
