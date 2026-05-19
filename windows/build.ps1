# build.ps1 - 打包 sef-chill-lounge-interface-server 專案
. "$PSScriptRoot\config.ps1"

Write-Host "=== sef-cli Build ===" -ForegroundColor Cyan
Write-Host "JAVA_HOME: $JAVA_HOME" -ForegroundColor Gray
Write-Host "執行 mvn clean package -DskipTests -e ..." -ForegroundColor Yellow

Set-Location $PROJECT_ROOT
& $MVNW clean package -DskipTests -e

if ($LASTEXITCODE -eq 0) {
    Write-Host "=== 打包成功 ===" -ForegroundColor Green
    Write-Host "JAR 位置: $JAR_PATH" -ForegroundColor Green
} else {
    Write-Host "=== 打包失敗，錯誤碼: $LASTEXITCODE ===" -ForegroundColor Red
    exit $LASTEXITCODE
}
