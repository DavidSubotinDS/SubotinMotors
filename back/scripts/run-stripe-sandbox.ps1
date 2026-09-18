param(
  [ValidateRange(1, 65535)]
  [int] $Port = 8080,
  [string] $PublicBaseUrl,
  [ValidateNotNullOrEmpty()]
  [string] $StripeProfile = "default",
  [switch] $UseCliLogin
)

$ErrorActionPreference = "Stop"

function Get-StripeProfileTestKey {
  param([string] $ConfigText, [string] $Profile)

  # Stripe can store several accounts in one file. Never use a key from a
  # different section just because it appears first in the file.
  $escapedProfile = [regex]::Escape($Profile)
  $sectionPattern = '(?ms)^[ \t]*\[[ \t]*(?:' + $escapedProfile +
      '|"' + $escapedProfile + '"|''' + $escapedProfile +
      ''')[ \t]*\][ \t]*(?:#[^\r\n]*)?\r?\n(?<body>.*?)(?=^[ \t]*\[|\z)'
  $section = [regex]::Match($ConfigText, $sectionPattern)
  if (-not $section.Success) {
    return $null
  }

  $keyMatch = [regex]::Match(
      $section.Groups["body"].Value,
      '(?m)^[ \t]*test_mode_api_key[ \t]*=[ \t]*(?:"(?<key>[^"\r\n]+)"|''(?<key>[^''\r\n]+)'')[ \t]*(?:#[^\r\n]*)?\r?$')
  if ($keyMatch.Success) {
    return $keyMatch.Groups["key"].Value
  }
  return $null
}

$listener = $null
$previousLocation = Get-Location

# Cover authentication and listener setup too, so an early failure cannot
# leave an expired key overriding the next successful `stripe login`.
try {
  $stripeCommand = Get-Command stripe -ErrorAction SilentlyContinue
  if ($null -eq $stripeCommand) {
    $stripePath = Join-Path $env:USERPROFILE ".local\bin\stripe.exe"
    if (-not (Test-Path -LiteralPath $stripePath)) {
      throw "Stripe CLI was not found. Expected it at $stripePath."
    }
  } else {
    $stripePath = $stripeCommand.Source
  }

  if ($UseCliLogin) {
    $env:STRIPE_SECRET_KEY = $null
  }

  $keySource = "STRIPE_SECRET_KEY from this PowerShell session"
  if ([string]::IsNullOrWhiteSpace($env:STRIPE_SECRET_KEY)) {
    $stripeConfig = Join-Path $env:USERPROFILE ".config\stripe\config.toml"
    if (Test-Path -LiteralPath $stripeConfig) {
      $configText = Get-Content -LiteralPath $stripeConfig -Raw
      $env:STRIPE_SECRET_KEY = Get-StripeProfileTestKey -ConfigText $configText -Profile $StripeProfile
      $keySource = "Stripe CLI profile '$StripeProfile'"
    }
  }

  if ([string]::IsNullOrWhiteSpace($env:STRIPE_SECRET_KEY)) {
    if ($UseCliLogin) {
      throw "No sandbox key found in Stripe CLI profile '$StripeProfile'. Run stripe login --project-name `"$StripeProfile`" and retry."
    }
    $secureKey =
        Read-Host "Paste a replacement Stripe sandbox secret key" -AsSecureString
    $env:STRIPE_SECRET_KEY = [Net.NetworkCredential]::new("", $secureKey).Password
    $keySource = "the sandbox key entered at the prompt"
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
  if ([string]::IsNullOrWhiteSpace($PublicBaseUrl)) { $PublicBaseUrl = "http://localhost:$Port" }
  $publicOrigin = [uri] $PublicBaseUrl
  if (-not $publicOrigin.IsAbsoluteUri -or $publicOrigin.Scheme -notin @('http', 'https') -or
      $publicOrigin.UserInfo -or $publicOrigin.Query -or $publicOrigin.Fragment -or $publicOrigin.AbsolutePath -ne '/') {
    throw 'PublicBaseUrl must be an HTTP(S) origin without credentials, path, query or fragment.'
  }
  $env:APP_BASE_URL = $PublicBaseUrl.TrimEnd('/')

  Write-Host "Using $keySource (key not displayed)."
  Write-Host "Requesting a temporary webhook signing secret..."
  $nativeErrorPreference = $ErrorActionPreference
  try {
    # Windows PowerShell treats native stderr as ErrorRecord objects. Capture
    # it before inspecting the exit code, instead of aborting mid-command.
    $ErrorActionPreference = "Continue"
    $secretOutput = (& $stripePath listen --print-secret --skip-update 2>&1 | Out-String)
    $stripeExitCode = $LASTEXITCODE
  } finally {
    $ErrorActionPreference = $nativeErrorPreference
  }
  if ($stripeExitCode -ne 0) {
    if ($secretOutput -match '401|api_key_expired|Invalid API Key') {
      throw "Stripe rejected the sandbox key from $keySource. Run stripe login --project-name `"$StripeProfile`", then rerun this script with -UseCliLogin."
    }
    # Do not echo provider responses, which can contain credential details.
    throw "Stripe could not create a local webhook listener (exit code $stripeExitCode). Check Stripe CLI authentication and network access."
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
    "--forward-to", "$($env:APP_BASE_URL)/webhooks/stripe"
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

  Write-Host ""
  Write-Host "Stripe sandbox is ready."
  Write-Host "Webhook forwarding: $($env:APP_BASE_URL)/webhooks/stripe"
  Write-Host "Listener logs: $listenerOutput and $listenerError"
  Write-Host "Starting Autostrada Auctions..."
  Write-Host ""

  Set-Location -LiteralPath $projectRoot
  & .\mvnw.cmd spring-boot:run "-Dspring-boot.run.arguments=--server.port=$Port"
} finally {
  Set-Location -LiteralPath $previousLocation
  if ($null -ne $listener -and -not $listener.HasExited) {
    Stop-Process -Id $listener.Id -Force -ErrorAction SilentlyContinue
  }
  $env:STRIPE_WEBHOOK_SECRET = $null
  $env:STRIPE_API_KEY = $null
  $env:STRIPE_SECRET_KEY = $null
}
