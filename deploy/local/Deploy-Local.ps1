[CmdletBinding()]
param(
    [Parameter(Mandatory)][string]$Sha,
    [Parameter(Mandatory)][string]$EnvFile,
    [Parameter(Mandatory)][string]$StateRoot
)
. (Join-Path $PSScriptRoot 'Common.ps1')
$paths = Assert-DeployInputs $Sha $EnvFile $StateRoot
$env:DEPLOY_SHA = $Sha
$currentFile = Join-Path $paths.StateRoot 'current-sha.txt'
if (-not (Test-Path -LiteralPath $currentFile)) {
    throw "Deployment is not initialized. Run Initialize-Local.ps1 once from the reviewed master revision."
}
$previous = (Get-Content -LiteralPath $currentFile -Raw).Trim()
if ($previous -notmatch '^[0-9a-f]{40}$') { throw 'The deployment state contains an invalid previous SHA.' }

$mysqlId = (Invoke-Compose $paths.EnvFile @('ps', '--quiet', 'mysql') | Out-String).Trim()
if (-not $mysqlId) { throw 'The initialized MySQL deployment is not running; refusing an upgrade without a pre-deploy backup.' }
$backupDir = Join-Path $paths.StateRoot ('backups\' + [DateTime]::UtcNow.ToString('yyyyMMdd-HHmmss') + '-' + $previous.Substring(0, 12))
New-Item -ItemType Directory -Force -Path $backupDir | Out-Null

try {
    Invoke-Compose $paths.EnvFile @('exec', '-T', 'mysql', 'sh', '-c', 'MYSQL_PWD="$MYSQL_ROOT_PASSWORD" mysqldump -uroot --single-transaction --routines --events --databases "$MYSQL_DATABASE" "$IDENTITY_DB_NAME" "$NOTIFICATION_DB_NAME" > /tmp/autostrada-predeploy.sql')
    & docker cp "${mysqlId}:/tmp/autostrada-predeploy.sql" (Join-Path $backupDir 'databases.sql')
    if ($LASTEXITCODE -ne 0) { throw 'Unable to copy the pre-deploy database backup.' }
    Invoke-Compose $paths.EnvFile @('exec', '-T', 'mysql', 'rm', '-f', '/tmp/autostrada-predeploy.sql')
    @{ previousSha = $previous; targetSha = $Sha; createdAtUtc = [DateTime]::UtcNow.ToString('o') } |
        ConvertTo-Json | Set-Content -LiteralPath (Join-Path $backupDir 'manifest.json') -Encoding utf8

    Build-ReleaseImages $Sha
    Invoke-Compose $paths.EnvFile @('config', '--quiet')
    Invoke-Compose $paths.EnvFile @('up', '--detach', '--wait', '--wait-timeout', '300', '--remove-orphans')
    Test-PublicOrigin $paths.EnvFile
    Set-Content -LiteralPath $currentFile -Value $Sha -Encoding ascii
    Write-SanitizedEvidence $paths.StateRoot $paths.EnvFile $Sha 'healthy'
} catch {
    try {
        Write-SanitizedEvidence $paths.StateRoot $paths.EnvFile $Sha 'failed'
        $privateFailure = Join-Path $paths.StateRoot ('private-failures\deploy-' + [DateTime]::UtcNow.ToString('yyyyMMdd-HHmmss') + '-' + $Sha.Substring(0, 12))
        New-Item -ItemType Directory -Force -Path $privateFailure | Out-Null
        Invoke-Compose $paths.EnvFile @('logs', '--no-color', '--tail', '500') | Set-Content -LiteralPath (Join-Path $privateFailure 'compose.log')
    } catch {}
    throw "Deployment failed. The database backup is at $backupDir. No automatic image or database rollback was attempted because post-cutover identity, role, reset, inbox, outbox, and broker state must not be made stale. $($_.Exception.Message)"
}
