# build-docker.ps1 - 打包 JAR + 建立 Docker Image + 匯出 tar
param(
    [string]$Version = ""
)
. "$PSScriptRoot\config.ps1"

$IMAGE_NAME  = "sef-cli-backend"
$DOCKER_DIR  = "$PROJECT_ROOT\docker\prd"

# 版號：有傳入就用傳入的，否則用今日日期
if ($Version -ne "") {
    $TAG = $Version
} else {
    $TAG = (Get-Date).ToString("yyMMdd") + "-PRD"
}

$TAR_PATH = "$DOCKER_DIR\$IMAGE_NAME-$TAG.tar"

Write-Host "=== sef-cli Build & Docker ===" -ForegroundColor Cyan
Write-Host "Tag: $TAG" -ForegroundColor Gray

# ── 1. Maven 打包 ──────────────────────────────────────────────
Write-Host "`n[1/4] 執行 mvn clean package -DskipTests ..." -ForegroundColor Yellow
Set-Location $PROJECT_ROOT
& $MVNW clean package -DskipTests -e

if ($LASTEXITCODE -ne 0) {
    Write-Host "=== 打包失敗，中止 ===" -ForegroundColor Red
    exit $LASTEXITCODE
}

# ── 2. 複製 JAR 到 docker/prd ──────────────────────────────────
Write-Host "`n[2/4] 複製 JAR 到 docker/prd ..." -ForegroundColor Yellow
Copy-Item "$JAR_PATH" "$DOCKER_DIR\$JAR_NAME" -Force

# ── 3. Docker build ────────────────────────────────────────────
Write-Host "`n[3/4] Docker build ..." -ForegroundColor Yellow
Set-Location $DOCKER_DIR
docker build -t "${IMAGE_NAME}:${TAG}" -t "${IMAGE_NAME}:latest" . --no-cache

if ($LASTEXITCODE -ne 0) {
    Write-Host "=== Docker build 失敗，中止 ===" -ForegroundColor Red
    exit $LASTEXITCODE
}

# ── 4. Docker save ─────────────────────────────────────────────
Write-Host "`n[4/4] 匯出 tar：$TAR_PATH ..." -ForegroundColor Yellow
docker save -o $TAR_PATH "${IMAGE_NAME}:${TAG}"

if ($LASTEXITCODE -eq 0) {
    Write-Host "`n=== 完成 ===" -ForegroundColor Green
    Write-Host "Image : ${IMAGE_NAME}:${TAG}" -ForegroundColor Green
    Write-Host "Tar   : $TAR_PATH" -ForegroundColor Green
} else {
    Write-Host "=== docker save 失敗 ===" -ForegroundColor Red
    exit $LASTEXITCODE
}
