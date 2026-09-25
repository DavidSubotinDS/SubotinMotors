[CmdletBinding(SupportsShouldProcess)]
param([Parameter(Mandatory)][string]$Path)
. (Join-Path $PSScriptRoot 'Common.ps1')
$full = Assert-AbsoluteExternalPath $Path 'Path'
if (-not (Test-Path -LiteralPath $full -PathType Leaf)) { throw 'Initialize the private environment file first.' }
$lines = @(Get-Content -LiteralPath $full)
$passwordLines = @($lines | Where-Object { $_ -match '^OBSERVABILITY_ADMIN_PASSWORD=' })
if ($passwordLines.Count -gt 1) { throw 'Duplicate Grafana password settings; resolve them manually.' }
if ($passwordLines.Count -eq 1 -and $passwordLines[0].Substring('OBSERVABILITY_ADMIN_PASSWORD='.Length).Length -lt 16) {
    throw 'Existing Grafana password is too short; set at least 16 characters privately.'
}
$updated = @($lines | Where-Object { $_ -notmatch '^OBSERVABILITY_ENABLED=' })
if ($passwordLines.Count -eq 0) {
    $bytes = New-Object byte[] 32
    $rng = [Security.Cryptography.RandomNumberGenerator]::Create()
    try { $rng.GetBytes($bytes) } finally { $rng.Dispose() }
    $secret = -join ($bytes | ForEach-Object { $_.ToString('x2') })
    $updated += "OBSERVABILITY_ADMIN_PASSWORD=$secret"
}
if (-not @($lines | Where-Object { $_ -match '^OBSERVABILITY_GRAFANA_PORT=' }).Count) { $updated += 'OBSERVABILITY_GRAFANA_PORT=3001' }
$updated += 'OBSERVABILITY_ENABLED=true'
if (($lines -join "`n") -eq ($updated -join "`n")) { Write-Output 'Local observability is already enabled.'; return }
if ($PSCmdlet.ShouldProcess($full, 'enable monitoring and add missing private Grafana credentials')) {
    Set-Content -LiteralPath $full -Value $updated -Encoding utf8
    Write-Output 'Monitoring enabled for the next deployment. Existing application credentials preserved; no stack was started.'
}
