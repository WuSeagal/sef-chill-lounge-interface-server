# stop.ps1 - 關閉 sef-chill-lounge-interface-server
. "$PSScriptRoot\config.ps1"

if (!(Test-Path $PID_FILE)) {
    Write-Host "找不到 PID 檔，應用程式可能未透過 start.ps1 啟動。" -ForegroundColor Yellow
    exit 0
}

$targetPid = Get-Content $PID_FILE
$proc = Get-Process -Id $targetPid -ErrorAction SilentlyContinue

if ($proc) {
    Write-Host "=== 關閉 sef-cli (PID: $targetPid) ===" -ForegroundColor Cyan
    Stop-Process -Id $targetPid -Force
    Write-Host "已關閉。" -ForegroundColor Green
} else {
    Write-Host "PID $targetPid 的程序不存在，可能已經關閉。" -ForegroundColor Yellow
}

Remove-Item $PID_FILE -Force
