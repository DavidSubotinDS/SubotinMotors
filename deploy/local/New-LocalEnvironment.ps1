[CmdletBinding(SupportsShouldProcess, ConfirmImpact = 'Medium')]
param([Parameter(Mandatory)][string]$Path)
Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'
if (-not [IO.Path]::IsPathFullyQualified($Path)) { throw 'Path must be absolute.' }
$full = [IO.Path]::GetFullPath($Path)
if (Test-Path -LiteralPath $full) { throw "Refusing to overwrite existing configuration: $full" }

function ConvertTo-Base64Url([byte[]]$Bytes) {
    return [Convert]::ToBase64String($Bytes).TrimEnd('=').Replace('+', '-').Replace('/', '_')
}
function New-HexSecret([int]$Bytes = 32) {
    $buffer = [byte[]]::new($Bytes); [Security.Cryptography.RandomNumberGenerator]::Fill($buffer)
    return [Convert]::ToHexString($buffer).ToLowerInvariant()
}

$rsa = [Security.Cryptography.RSA]::Create(2048)
$key = $rsa.ExportParameters($true)
$kid = 'local-' + (New-HexSecret 8)
$private = [ordered]@{ kty='RSA'; kid=$kid; alg='RS256'; use='sig'; n=(ConvertTo-Base64Url $key.Modulus); e=(ConvertTo-Base64Url $key.Exponent); d=(ConvertTo-Base64Url $key.D); p=(ConvertTo-Base64Url $key.P); q=(ConvertTo-Base64Url $key.Q); dp=(ConvertTo-Base64Url $key.DP); dq=(ConvertTo-Base64Url $key.DQ); qi=(ConvertTo-Base64Url $key.InverseQ) }
$public = [ordered]@{ kty='RSA'; kid=$kid; alg='RS256'; use='sig'; n=$private.n; e=$private.e }
$delivery = [byte[]]::new(32); [Security.Cryptography.RandomNumberGenerator]::Fill($delivery)

$values = [ordered]@{
    MYSQL_PASSWORD = New-HexSecret; MYSQL_ROOT_PASSWORD = New-HexSecret
    MYSQL_DATABASE = 'autostrada'; MYSQL_USER = 'autostrada'
    DB_RUNTIME_USERNAME = 'autostrada_runtime'; DB_RUNTIME_PASSWORD = New-HexSecret
    DB_MIGRATION_USERNAME = 'autostrada_migration'; DB_MIGRATION_PASSWORD = New-HexSecret
    IDENTITY_DB_NAME = 'autostrada_identity'; IDENTITY_DB_USERNAME = 'identity_runtime'; IDENTITY_DB_PASSWORD = New-HexSecret
    IDENTITY_SIGNING_JWK = ($private | ConvertTo-Json -Compress)
    IDENTITY_VERIFICATION_JWKS = (@{ keys=@($public) } | ConvertTo-Json -Compress -Depth 4)
    IDENTITY_GATEWAY_SECRET = New-HexSecret; IDENTITY_BACKEND_SECRET = New-HexSecret
    APP_DEMO_DATA_ACK = 'I_ACCEPT_EXISTING_DEMO_DATA'
    PUBLIC_URL = 'http://localhost:8081'; GATEWAY_BIND_ADDRESS = '127.0.0.1'; GATEWAY_PORT = '8081'; SESSION_COOKIE_SECURE = 'false'
    STRIPE_ENABLED = 'false'; STRIPE_SECRET_KEY = ''; STRIPE_WEBHOOK_SECRET = ''
    APP_MAIL_MODE = 'log'; APP_MAIL_FROM = 'no-reply@autostradaauctions.local'; SMTP_HOST = ''; SMTP_PORT = '587'; SMTP_USERNAME = ''; SMTP_PASSWORD = ''; SMTP_AUTH = 'true'; SMTP_STARTTLS = 'true'
    NOTIFICATION_DB_NAME = 'autostrada_notification'; NOTIFICATION_DB_USERNAME = 'notification_runtime'; NOTIFICATION_DB_PASSWORD = New-HexSecret
    PAYMENT_DB_NAME = 'autostrada_payment'; PAYMENT_DB_USERNAME = 'payment_runtime'; PAYMENT_DB_PASSWORD = New-HexSecret
    RABBITMQ_BACKEND_PASSWORD = New-HexSecret; RABBITMQ_IDENTITY_PASSWORD = New-HexSecret; RABBITMQ_NOTIFICATION_PASSWORD = New-HexSecret; RABBITMQ_PAYMENT_PASSWORD = New-HexSecret; RABBITMQ_OPERATOR_PASSWORD = New-HexSecret
    NOTIFICATION_DELIVERY_KEY = [Convert]::ToBase64String($delivery)
}
if (-not $PSCmdlet.ShouldProcess($full, 'write new local deployment credentials')) { return }
New-Item -ItemType Directory -Force -Path ([IO.Path]::GetDirectoryName($full)) | Out-Null
$values.GetEnumerator() | ForEach-Object { "$($_.Key)=$($_.Value)" } | Set-Content -LiteralPath $full -Encoding utf8
Write-Output "Created host-only deployment configuration at $full. Keep it private and back it up securely."

