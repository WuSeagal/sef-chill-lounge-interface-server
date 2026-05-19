# config.ps1 - sef-chill-lounge-interface-server 共用設定
# ============================================================
# JDK 路徑設定 (修改這裡即可套用到所有腳本)
# ============================================================
$JAVA_HOME = "C:\Users\aa288\.jdks\corretto-21.0.5"

# ============================================================
# 專案設定 (通常不需要更動)
# ============================================================
$PROJECT_ROOT  = "$PSScriptRoot\.."
$JAR_NAME      = "sef-chill-lounge-interface-server-0.0.1-SNAPSHOT.jar"
$JAR_PATH      = "$PROJECT_ROOT\target\$JAR_NAME"
$PID_FILE      = "$PROJECT_ROOT\windows\sef-cli.pid"
$MVNW          = "$PROJECT_ROOT\mvnw.cmd"

# ============================================================
# 套用 JAVA_HOME 到目前 session
# ============================================================
$env:JAVA_HOME = $JAVA_HOME
$env:Path      = "$JAVA_HOME\bin;" + $env:Path
