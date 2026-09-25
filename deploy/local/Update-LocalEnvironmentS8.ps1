[CmdletBinding(SupportsShouldProcess,ConfirmImpact='Medium')]
param([Parameter(Mandatory)][string]$Path)
Set-StrictMode -Version Latest;$ErrorActionPreference='Stop'
if(-not [IO.Path]::IsPathFullyQualified($Path)){throw 'Path must be absolute.'}
$full=[IO.Path]::GetFullPath($Path);if(-not(Test-Path -LiteralPath $full -PathType Leaf)){throw "Deployment env file not found: $full"}
function New-HexSecret(){ $bytes=[byte[]]::new(32);[Security.Cryptography.RandomNumberGenerator]::Fill($bytes);return [Convert]::ToHexString($bytes).ToLowerInvariant() }
$existing=@{};foreach($line in Get-Content -LiteralPath $full){if($line -match '^([A-Z0-9_]+)='){$existing[$Matches[1]]=$true}}
$add=[ordered]@{}
if(-not $existing.ContainsKey('PAYMENT_DB_NAME')){$add.PAYMENT_DB_NAME='autostrada_payment'}
if(-not $existing.ContainsKey('PAYMENT_DB_USERNAME')){$add.PAYMENT_DB_USERNAME='payment_runtime'}
if(-not $existing.ContainsKey('PAYMENT_DB_PASSWORD')){$add.PAYMENT_DB_PASSWORD=New-HexSecret}
if(-not $existing.ContainsKey('RABBITMQ_PAYMENT_PASSWORD')){$add.RABBITMQ_PAYMENT_PASSWORD=New-HexSecret}
if($add.Count -eq 0){Write-Output 'The deployment environment already contains the S8 payment settings.';return}
if(-not $PSCmdlet.ShouldProcess($full,'append missing S8 payment credentials without changing existing values')){return}
Add-Content -LiteralPath $full -Value '' -Encoding utf8
$add.GetEnumerator()|ForEach-Object{"$($_.Key)=$($_.Value)"}|Add-Content -LiteralPath $full -Encoding utf8
Write-Output 'Added the missing S8 payment settings. Existing credentials were preserved.'
