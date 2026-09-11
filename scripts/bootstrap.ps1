param([switch]$SkipModel)
$ErrorActionPreference = 'Stop'
$ProgressPreference = 'SilentlyContinue'
$taskRoot = Split-Path -Parent $PSScriptRoot
$taskTools = Join-Path $taskRoot '.tools'
New-Item -ItemType Directory -Force -Path $taskTools | Out-Null
$taskGradleZip = Join-Path $taskTools 'gradle-8.11.1-bin.zip'
$taskGradleHome = Join-Path $taskTools 'gradle-8.11.1'
if (!(Test-Path -LiteralPath $taskGradleHome)) {
    if (!(Test-Path -LiteralPath $taskGradleZip)) { Invoke-WebRequest 'https://services.gradle.org/distributions/gradle-8.11.1-bin.zip' -OutFile $taskGradleZip }
    $taskChecksumResponse = (Invoke-WebRequest 'https://services.gradle.org/distributions/gradle-8.11.1-bin.zip.sha256').Content
    $taskChecksum = if ($taskChecksumResponse -is [byte[]]) { [Text.Encoding]::UTF8.GetString($taskChecksumResponse).Trim() } else { ([string]$taskChecksumResponse).Trim() }
    if ((Get-FileHash -LiteralPath $taskGradleZip -Algorithm SHA256).Hash -ne $taskChecksum) { throw 'Gradle checksum mismatch.' }
    Expand-Archive -LiteralPath $taskGradleZip -DestinationPath $taskTools -Force
}
if (!$SkipModel) {
    $taskAssets = Join-Path $taskRoot 'app\src\main\assets'
    New-Item -ItemType Directory -Force -Path $taskAssets | Out-Null
    $taskModel = Join-Path $taskAssets 'pose_landmarker_lite.task'
    if (!(Test-Path -LiteralPath $taskModel)) {
        Invoke-WebRequest 'https://storage.googleapis.com/mediapipe-models/pose_landmarker/pose_landmarker_lite/float16/1/pose_landmarker_lite.task' -OutFile $taskModel
    }
    Get-FileHash -LiteralPath $taskModel -Algorithm SHA256 | Select-Object Hash,Path
}
Write-Output 'Build tools ready. Run scripts/build.ps1.'
