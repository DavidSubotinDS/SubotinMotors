Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

$script:RepositoryRoot = (Resolve-Path (Join-Path $PSScriptRoot '..\..')).Path
$script:ComposeFile = Join-Path $script:RepositoryRoot 'compose.yaml'
$script:CdComposeFile = Join-Path $PSScriptRoot 'compose.cd.yaml'
$script:ProjectName = if ($env:AUTOSTRADA_DEPLOY_PROJECT) { $env:AUTOSTRADA_DEPLOY_PROJECT } else { 'autostrada-local' }
if ($script:ProjectName -ne 'autostrada-local' -and $script:ProjectName -notmatch '^autostrada-local-test-[0-9a-f]{12}$') {
    throw 'AUTOSTRADA_DEPLOY_PROJECT may only select autostrada-local or a disposable autostrada-local-test-<12 hex> project.'
}

function Test-WindowsAbsolutePath([string]$Path) {
    return -not [string]::IsNullOrWhiteSpace($Path) -and
        ($Path -match '^[A-Za-z]:[\\/]' -or $Path -match '^[\\/]{2}[^\\/]+[\\/][^\\/]+')
}

function Assert-AbsoluteExternalPath([string]$Path, [string]$Name) {
    if (-not (Test-WindowsAbsolutePath $Path)) { throw "$Name must be an absolute path." }
    $full = [IO.Path]::GetFullPath($Path)
    if ($full.StartsWith($script:RepositoryRoot + [IO.Path]::DirectorySeparatorChar, [StringComparison]::OrdinalIgnoreCase)) {
        throw "$Name must be outside the repository so secrets and deployment state cannot be committed."
    }
    return $full
}

function Assert-DeployInputs([string]$Sha, [string]$EnvFile, [string]$StateRoot) {
    if ($Sha -notmatch '^[0-9a-f]{40}$') { throw 'Sha must be a full lowercase 40-character Git commit SHA.' }
    $resolvedEnv = Assert-AbsoluteExternalPath $EnvFile 'EnvFile'
    $resolvedState = Assert-AbsoluteExternalPath $StateRoot 'StateRoot'
    if (-not (Test-Path -LiteralPath $resolvedEnv -PathType Leaf)) { throw "Deployment env file not found: $resolvedEnv" }
    New-Item -ItemType Directory -Force -Path $resolvedState | Out-Null
    docker version --format '{{.Server.Version}}' | Out-Null
    if ($LASTEXITCODE -ne 0) { throw 'Docker Engine is unavailable.' }
    docker compose version | Out-Null
    if ($LASTEXITCODE -ne 0) { throw 'Docker Compose is unavailable.' }
    return @{ EnvFile = $resolvedEnv; StateRoot = $resolvedState }
}

function Invoke-Compose([string]$EnvFile, [string[]]$Arguments, [string[]]$ExtraFiles = @()) {
    $files = @('-f', $script:ComposeFile, '-f', $script:CdComposeFile)
    if (Test-ObservabilityEnabled $EnvFile) { $files += @('-f', (Join-Path $script:RepositoryRoot 'compose.observability.yaml')) }
    foreach ($file in $ExtraFiles) { $files += @('-f', $file) }
    & docker compose --project-name $script:ProjectName --env-file $EnvFile @files @Arguments
    if ($LASTEXITCODE -ne 0) { throw "docker compose failed: $($Arguments -join ' ')" }
}

function Test-ObservabilityEnabled([string]$EnvFile) {
    return @(Get-Content -LiteralPath $EnvFile | Where-Object { $_ -match '^OBSERVABILITY_ENABLED=true\s*$' }).Count -gt 0
}

function Build-ReleaseImages([string]$Sha) {
    $builds = @(
        [pscustomobject]@{ Image='autostrada/backend:' + $Sha; Context='back'; Target='production' }
        [pscustomobject]@{ Image='autostrada/identity:' + $Sha; Context='services/identity-service'; Target='production' }
        [pscustomobject]@{ Image='autostrada/notification:' + $Sha; Context='services/notification-service'; Target='production' }
        [pscustomobject]@{ Image='autostrada/payment:' + $Sha; Context='services/payment-service'; Target='production' }
        [pscustomobject]@{ Image='autostrada/gateway:' + $Sha; Context='gateway'; Target='' }
        [pscustomobject]@{ Image='autostrada/frontend:' + $Sha; Context='front'; Target='' }
        [pscustomobject]@{ Image='autostrada/rabbitmq:' + $Sha; Context='infra/rabbitmq'; Target='' }
    )
    foreach ($build in $builds) {
        $args = @('build', '--pull', '--label', "org.opencontainers.image.revision=$Sha", '--tag', $build.Image)
        if ($build.Target) { $args += @('--target', $build.Target) }
        $args += (Join-Path $script:RepositoryRoot $build.Context)
        & docker @args
        if ($LASTEXITCODE -ne 0) { throw "Image build failed: $($build.Image)" }
    }
}

function Test-PublicOrigin([string]$EnvFile) {
    $publicUrl = 'http://localhost:8081'
    foreach ($line in Get-Content -LiteralPath $EnvFile) {
        if ($line -match '^PUBLIC_URL=(.+)$') { $publicUrl = $Matches[1].Trim(); break }
    }
    $readiness = if (Test-ObservabilityEnabled $EnvFile) { '/readyz' } else { '/actuator/health/readiness' }
    foreach ($path in @($readiness, '/api/session', '/')) {
        $response = Invoke-WebRequest -UseBasicParsing -Uri ($publicUrl.TrimEnd('/') + $path) -TimeoutSec 15
        if ($response.StatusCode -ne 200) { throw "Public smoke failed for $path with status $($response.StatusCode)." }
    }
    if (Test-ObservabilityEnabled $EnvFile) {
        $settings = @{}
        foreach ($line in Get-Content -LiteralPath $EnvFile) {
            if ($line -match '^(OBSERVABILITY_[A-Z_]+)=(.*)$') { $settings[$Matches[1]] = $Matches[2] }
        }
        $port = if ($settings.ContainsKey('OBSERVABILITY_GRAFANA_PORT')) { $settings.OBSERVABILITY_GRAFANA_PORT } else { '3001' }
        $credential = [Convert]::ToBase64String([Text.Encoding]::UTF8.GetBytes('observer:' + $settings.OBSERVABILITY_ADMIN_PASSWORD))
        $headers = @{ Authorization = 'Basic ' + $credential }
        $origin = 'http://127.0.0.1:' + $port
        $ready = $false
        for ($attempt = 0; $attempt -lt 20; $attempt++) {
            try {
                $result = Invoke-RestMethod -Uri ($origin + '/api/datasources/proxy/uid/autostrada-prometheus/api/v1/query?query=up%7Bjob%3D%22autostrada%22%7D') -Headers $headers -TimeoutSec 5
                $samples = @($result.data.result)
                $ready = $samples.Count -eq 5 -and @($samples | Where-Object { $_.value[1] -ne '1' }).Count -eq 0
                if ($ready) { break }
            } catch { $ready = $false }
            Start-Sleep -Seconds 3
        }
        if (-not $ready) { throw 'Monitoring post-deploy check failed: expected five successful service scrapes.' }
        foreach ($uid in @('autostrada-tempo', 'autostrada-loki')) {
            $health = Invoke-RestMethod -Uri ($origin + '/api/datasources/uid/' + $uid + '/health') -Headers $headers -TimeoutSec 10
            if ($health.status -ne 'OK') { throw "Monitoring datasource is unhealthy: $uid" }
        }
    }
}

function Write-SanitizedEvidence([string]$StateRoot, [string]$EnvFile, [string]$Sha, [string]$Status) {
    $latest = Join-Path $StateRoot 'evidence\latest'
    New-Item -ItemType Directory -Force -Path $latest | Out-Null
    @{ sha = $Sha; status = $Status; completedAtUtc = [DateTime]::UtcNow.ToString('o'); project = $script:ProjectName } |
        ConvertTo-Json | Set-Content -LiteralPath (Join-Path $latest 'deployment.json') -Encoding utf8
    Invoke-Compose $EnvFile @('ps', '--format', 'json') |
        Set-Content -LiteralPath (Join-Path $latest 'compose-ps.jsonl') -Encoding utf8
}
