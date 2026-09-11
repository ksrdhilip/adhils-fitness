param([ValidateSet('Set','Read','Delete')][string]$Action,[ValidateSet('openai','claude')][string]$Name)
$ErrorActionPreference='Stop'
$credentialDirectory=Join-Path (Split-Path -Parent $PSScriptRoot) '.state'
New-Item -ItemType Directory -Force -Path $credentialDirectory | Out-Null
$credentialFile=Join-Path $credentialDirectory ($Name+'.credential')
if($Action -eq 'Set') {
    $secretValue=Read-Host "Enter $Name API key (stored with Windows user encryption)" -AsSecureString
    try { ConvertFrom-SecureString -SecureString $secretValue | Set-Content -LiteralPath $credentialFile -NoNewline }
    finally { $secretValue.Dispose() }
    Write-Host 'Credential saved for this Windows user. Restart the companion if it is running.'
} elseif($Action -eq 'Read') {
    $secretValue=Get-Content -LiteralPath $credentialFile -Raw | ConvertTo-SecureString
    $secretPointer=[Runtime.InteropServices.Marshal]::SecureStringToBSTR($secretValue)
    try { [Console]::Out.Write([Runtime.InteropServices.Marshal]::PtrToStringBSTR($secretPointer)) }
    finally { [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($secretPointer);$secretValue.Dispose() }
} else {
    if(Test-Path -LiteralPath $credentialFile) {Remove-Item -LiteralPath $credentialFile}
}
