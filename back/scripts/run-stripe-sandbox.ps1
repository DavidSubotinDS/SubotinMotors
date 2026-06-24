param(
  [ValidateRange(1, 65535)]
  [int] $Port = 8080
)

$ErrorActionPreference = "Stop"

$stripeCommand = Get-Command stripe -ErrorAction SilentlyContinue
if ($null -eq $stripeCommand) {
  $stripePath = Join-Path $env:USERPROFILE ".local\bin\stripe.exe"
  if (-not (Test-Path -LiteralPath $stripePath)) {
    throw "Stripe CLI was not found. Expected it at $stripePath."
  }
} else {
  $stripePath = $stripeCommand.Source
}

if ([string]::IsNullOrWhiteSpace($env:STRIPE_SECRET_KEY)) {
  $stripeConfig = Join-Path $env:USERPROFILE ".config\stripe\config.toml"
  if (Test-Path -LiteralPath $stripeConfig) {
    $configText = Get-Content -LiteralPath $stripeConfig -Raw
    $keyMatch = [regex]::Match(
        $configText,
        '(?m)^\s*test_mode_api_key\s*=\s*(.+?)\s*$')
    if ($keyMatch.Success) {
      $env:STRIPE_SECRET_KEY =
          $keyMatch.Groups[1].Value.Trim().Trim('"').Trim("'")
    }
  }
}

if ([string]::IsNullOrWhiteSpace($env:STRIPE_SECRET_KEY)) {
  $secureKey =
      Read-Host "Paste a replacement Stripe sandbox secret key" -AsSecureString
  $env:STRIPE_SECRET_KEY = [Net.NetworkCredential]::new("", $secureKey).Password
}

$isSandboxKey =
    $env:STRIPE_SECRET_KEY.StartsWith("sk_test_") -or
    $env:STRIPE_SECRET_KEY.StartsWith("rk_test_") -or
    $env:STRIPE_SECRET_KEY.StartsWith("rkcs_test_")
if (-not $isSandboxKey) {
  throw "This school project accepts only Stripe sandbox credentials."
}

$env:STRIPE_API_KEY = $env:STRIPE_SECRET_KEY
$env:STRIPE_ENABLED = "true"
$env:STRIPE_CURRENCY = "eur"
$env:STRIPE_PLATFORM_FEE_BPS = "250"
$env:APP_BASE_URL = "http://localhost:$Port"

Write-Host "Requesting a temporary webhook signing secret..."
$secretOutput =
    (& $stripePath listen --print-secret --skip-update 2>&1 | Out-String)
if ($LASTEXITCODE -ne 0) {
  throw "Stripe could not create a local webhook listener. $secretOutput"
}

$secretMatch = [regex]::Match($secretOutput, "whsec_[A-Za-z0-9]+")
if (-not $secretMatch.Success) {
  throw "Stripe did not return a webhook signing secret."
}
$env:STRIPE_WEBHOOK_SECRET = $secretMatch.Value

$eventNames = @(
  "checkout.session.completed",
  "checkout.session.async_payment_succeeded",
  "checkout.session.async_payment_failed",
  "checkout.session.expired"
) -join ","

$logPrefix = Join-Path $env:TEMP "autostrada-stripe-$PID"
$listenerOutput = "$logPrefix-output.log"
$listenerError = "$logPrefix-error.log"
$listenerArguments = @(
  "listen",
  "--skip-update",
  "--events", $eventNames,
  "--forward-to", "http://localhost:$Port/webhooks/stripe"
)

$listener = Start-Process `
    -FilePath $stripePath `
    -ArgumentList $listenerArguments `
    -WindowStyle Hidden `
    -PassThru `
    -RedirectStandardOutput $listenerOutput `
    -RedirectStandardError $listenerError

Start-Sleep -Seconds 2
if ($listener.HasExited) {
  $errorText = Get-Content -LiteralPath $listenerError -Raw -ErrorAction SilentlyContinue
  throw "The Stripe listener stopped unexpectedly. $errorText"
}

$projectRoot = Split-Path -Parent $PSScriptRoot
$previousLocation = Get-Location

Write-Host ""
Write-Host "Stripe sandbox is ready."
Write-Host "Webhook forwarding: http://localhost:$Port/webhooks/stripe"
Write-Host "Listener logs: $listenerOutput and $listenerError"
Write-Host "Starting Autostrada Auctions..."
Write-Host ""

try {
  Set-Location -LiteralPath $projectRoot
  & .\mvnw.cmd spring-boot:run "-Dspring-boot.run.arguments=--server.port=$Port"
} finally {
  Set-Location -LiteralPath $previousLocation
  if (-not $listener.HasExited) {
    Stop-Process -Id $listener.Id -Force
  }
  $env:STRIPE_WEBHOOK_SECRET = $null
  $env:STRIPE_API_KEY = $null
  $env:STRIPE_SECRET_KEY = $null
}
