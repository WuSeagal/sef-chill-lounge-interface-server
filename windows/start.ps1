# start.ps1 - 啟動 sef-chill-lounge-interface-server
. "$PSScriptRoot\config.ps1"

if (!(Test-Path $JAR_PATH)) {
    Write-Host "找不到 JAR 檔，請先執行 build.ps1 打包。" -ForegroundColor Red
    exit 1
}

if (Test-Path $PID_FILE) {
    $existingPid = Get-Content $PID_FILE
    $proc = Get-Process -Id $existingPid -ErrorAction SilentlyContinue
    if ($proc) {
        Write-Host "sef-cli 已在執行中 (PID: $existingPid)，請先執行 stop.ps1 關閉。" -ForegroundColor Yellow
        exit 0
    } else {
        Remove-Item $PID_FILE -Force
    }
}

Write-Host "=== 啟動 sef-chill-lounge-interface-server ===" -ForegroundColor Cyan
Write-Host "JAVA_HOME: $JAVA_HOME" -ForegroundColor Gray

$proc = Start-Process -FilePath "$JAVA_HOME\bin\java.exe" `
    -ArgumentList "-jar", $JAR_PATH `
    -PassThru -NoNewWindow

$proc.Id | Set-Content $PID_FILE
Write-Host "已啟動，PID: $($proc.Id)" -ForegroundColor Green
Write-Host "執行 stop.ps1 可關閉應用程式。" -ForegroundColor Gray
