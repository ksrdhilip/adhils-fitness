param([string[]]$Tasks = @(':core:test', ':app:assembleDebug', ':app:lintDebug'))
$ErrorActionPreference = 'Stop'
$taskRoot = Split-Path -Parent $PSScriptRoot
$taskJava = 'C:\Program Files\Android\Android Studio\jbr'
if (Test-Path -LiteralPath $taskJava) { $env:JAVA_HOME = $taskJava }
if (!$env:ANDROID_HOME) { $env:ANDROID_HOME = Join-Path $env:LOCALAPPDATA 'Android\Sdk' }
$env:GRADLE_USER_HOME = Join-Path $taskRoot '.tools\gradle-user-home'
$taskSocketDir = Join-Path $taskRoot '.tmp'
New-Item -ItemType Directory -Force -Path $taskSocketDir | Out-Null
# An explicit filesystem location avoids Windows packaged-app TEMP redirection.
$env:JAVA_TOOL_OPTIONS = '-Djdk.net.unixdomain.tmpdir="' + $taskSocketDir + '"'
$taskGradle = Join-Path $taskRoot '.tools\gradle-8.11.1\bin\gradle.bat'
if (!(Test-Path -LiteralPath $taskGradle)) { throw 'Gradle is not installed. Review and run the separate bootstrap step first; this build never downloads or executes an installer automatically.' }
Push-Location $taskRoot
try { & $taskGradle @Tasks --console=plain; if ($LASTEXITCODE -ne 0) { throw "Gradle failed ($LASTEXITCODE)" } } finally { Pop-Location }
