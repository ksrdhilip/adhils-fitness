param([ValidateSet('Snapshot','TapText','ScrollDown','ScrollUp','Screenshot')][string]$Action='Snapshot',[string]$Value='', [string]$Device='emulator-5554')
$ErrorActionPreference='Stop'
$taskRoot=Split-Path -Parent $PSScriptRoot
$taskAdb=Join-Path $env:LOCALAPPDATA 'Android\Sdk\platform-tools\adb.exe'
$taskLayout=Join-Path $taskRoot '.tools\adhils-window.xml'
if($Device -notmatch '^emulator-\d+$') {throw 'This helper only controls test emulators'}
function Read-Layout {
    $taskDump=& $taskAdb -s $Device shell uiautomator dump /sdcard/adhils-window.xml 2>&1
    if($LASTEXITCODE -ne 0 -or ($taskDump -join ' ') -notmatch 'dumped to:') {throw 'Could not inspect current emulator UI; refusing to reuse an older layout'}
    & $taskAdb -s $Device pull /sdcard/adhils-window.xml $taskLayout 2>$null | Out-Null
    if($LASTEXITCODE -ne 0) {throw 'Could not read emulator UI layout'}
    [xml](Get-Content -LiteralPath $taskLayout -Raw)
}
if($Action -eq 'Screenshot') {
    Start-Sleep -Milliseconds 750
    if($Value -notmatch '^[a-z0-9-]+$') {throw 'Use a simple screenshot name'}
    $taskShots=Join-Path $taskRoot 'docs\screenshots'
    New-Item -ItemType Directory -Force -Path $taskShots | Out-Null
    & $taskAdb -s $Device shell screencap -p /sdcard/adhils-screen.png
    if($LASTEXITCODE -ne 0) {throw 'Could not capture the current emulator screen'}
    & $taskAdb -s $Device pull /sdcard/adhils-screen.png (Join-Path $taskShots ($Value+'.png'))
    if($LASTEXITCODE -ne 0) {throw 'Could not save the emulator screenshot'}
} elseif($Action -in @('ScrollDown','ScrollUp')) {
    $layout=Read-Layout
    $scroll=$layout.SelectNodes('//node') | Where-Object {$_.scrollable -eq 'true'} | Select-Object -First 1
    if(!$scroll) {throw 'No scrollable area on this screen'}
    $numbers=[regex]::Matches($scroll.bounds,'\d+') | ForEach-Object {[int]$_.Value}
    # Use the content margin so a slider does not consume the scroll gesture.
    $x=$numbers[0]+20;$top=$numbers[1]+150;$bottom=$numbers[3]-150
    if($Action -eq 'ScrollDown') {& $taskAdb -s $Device shell input swipe $x $bottom $x $top 500}
    else {& $taskAdb -s $Device shell input swipe $x $top $x $bottom 500}
} else {
    $layout=Read-Layout
    $nodes=$layout.SelectNodes('//node')
    if($Action -eq 'Snapshot') {
        $nodes | Where-Object {$_.text -ne '' -or $_.'content-desc' -ne ''} | ForEach-Object {
            [pscustomobject]@{Text=$_.text;Description=$_.'content-desc';Bounds=$_.bounds}
        } | ConvertTo-Json -Compress
    } else {
        $node=$nodes | Where-Object {$_.text -eq $Value -or $_.'content-desc' -eq $Value} | Select-Object -First 1
        if(!$node) {throw "Text not present in current UI: $Value"}
        $numbers=[regex]::Matches($node.bounds,'\d+') | ForEach-Object {[int]$_.Value}
        & $taskAdb -s $Device shell input tap ([int](($numbers[0]+$numbers[2])/2)) ([int](($numbers[1]+$numbers[3])/2))
    }
}
