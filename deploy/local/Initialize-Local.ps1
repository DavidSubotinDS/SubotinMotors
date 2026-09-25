[CmdletBinding(SupportsShouldProcess, ConfirmImpact = 'High')]
param(
    [Parameter(Mandatory)][string]$Sha,
    [Parameter(Mandatory)][string]$EnvFile,
    [Parameter(Mandatory)][string]$StateRoot
)
. (Join-Path $PSScriptRoot 'Common.ps1')
$paths = Assert-DeployInputs $Sha $EnvFile $StateRoot
$env:DEPLOY_SHA = $Sha
$volume = docker volume ls --quiet --filter "label=com.docker.compose.project=$script:ProjectName"
if ($volume) { throw "Project $script:ProjectName already owns volumes. Initialization is fresh-only; use Deploy-Local.ps1 for upgrades." }
if (-not $PSCmdlet.ShouldProcess($script:ProjectName, 'create dedicated deployment volumes and perform the S5/S6/S8 copy cutovers')) { return }

Build-ReleaseImages $Sha
$bootstrap = Join-Path $PSScriptRoot 'compose.bootstrap.yaml'
try {
    $env:BACKEND_FLYWAY_TARGET = '18'
    Invoke-Compose $paths.EnvFile @('config', '--quiet') @($bootstrap)
    Invoke-Compose $paths.EnvFile @('up', '--detach', '--wait', '--wait-timeout', '240', 'mysql') @($bootstrap)
    Invoke-Compose $paths.EnvFile @('up', '--detach', '--wait', '--wait-timeout', '240', 'identity') @($bootstrap)
    Invoke-Compose $paths.EnvFile @('up', '--detach', '--wait', '--wait-timeout', '240', 'backend') @($bootstrap)
    Invoke-Compose $paths.EnvFile @('stop', 'backend', 'identity') @($bootstrap)
    Invoke-Compose $paths.EnvFile @('run', '--rm', '--no-deps', 'identity-copy') @($bootstrap)

    $env:BACKEND_FLYWAY_TARGET = '20'
    Invoke-Compose $paths.EnvFile @('up', '--detach', '--wait', '--wait-timeout', '240', 'identity', 'backend', 'rabbitmq', 'notification') @($bootstrap)
    Invoke-Compose $paths.EnvFile @('stop', 'backend', 'identity', 'notification') @($bootstrap)
    Invoke-Compose $paths.EnvFile @('run', '--rm', '--no-deps', 'notification-copy') @($bootstrap)

    Remove-Item Env:BACKEND_FLYWAY_TARGET
    Invoke-Compose $paths.EnvFile @('up', '--detach', '--wait', '--wait-timeout', '240', 'backend', 'payment')
    Invoke-Compose $paths.EnvFile @('stop', 'backend', 'payment')
    $env:BACKEND_FLYWAY_TARGET = '26'
    Invoke-Compose $paths.EnvFile @('run', '--rm', '--no-deps', 'payment-copy') @($bootstrap)
    Remove-Item Env:BACKEND_FLYWAY_TARGET
    Invoke-Compose $paths.EnvFile @('up', '--detach', '--wait', '--wait-timeout', '300', '--remove-orphans')
    Test-PublicOrigin $paths.EnvFile
    Set-Content -LiteralPath (Join-Path $paths.StateRoot 'current-sha.txt') -Value $Sha -Encoding ascii
    Write-SanitizedEvidence $paths.StateRoot $paths.EnvFile $Sha 'initialized'
} catch {
    try {
        $privateFailure = Join-Path $paths.StateRoot ('private-failures\initialization-' + [DateTime]::UtcNow.ToString('yyyyMMdd-HHmmss'))
        New-Item -ItemType Directory -Force -Path $privateFailure | Out-Null
        Invoke-Compose $paths.EnvFile @('logs', '--no-color', '--tail', '300') | Set-Content -LiteralPath (Join-Path $privateFailure 'compose.log')
    } catch {}
    throw
} finally {
    Remove-Item Env:BACKEND_FLYWAY_TARGET -ErrorAction SilentlyContinue
}
