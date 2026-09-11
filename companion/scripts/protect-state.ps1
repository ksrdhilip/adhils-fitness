$ErrorActionPreference='Stop'
$stateDirectory=Join-Path (Split-Path -Parent $PSScriptRoot) '.state'
New-Item -ItemType Directory -Force -Path $stateDirectory | Out-Null
$currentSid=[Security.Principal.WindowsIdentity]::GetCurrent().User.Value
# Restrict only the companion's own state directory; keep owner, SYSTEM and Administrators access.
& icacls.exe $stateDirectory '/inheritance:r' '/grant:r' ('*'+$currentSid+':(OI)(CI)F') '*S-1-5-18:(OI)(CI)F' '*S-1-5-32-544:(OI)(CI)F' | Out-Null
if($LASTEXITCODE -ne 0) {throw 'Could not protect companion state directory'}
