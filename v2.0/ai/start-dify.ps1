<#
    start-dify.ps1  —— 重启电脑后一键唤起 Dify（8090）
    用法（在任意目录）：
        powershell -ExecutionPolicy Bypass -File E:\JavaDEVIDEA\workspace\JavaProject\academic_risk_warning\v2.0\ai\start-dify.ps1
    做的事：
        1) Docker 引擎没起 → 启动 Docker Desktop 并等它就绪
        2) docker compose up -d（幂等，已存在的容器直接拉起）
        3) 停掉两个可选容器（agent_backend / local_sandbox），省内存、避免 restart 循环
        4) 等 http://localhost:8090 返回 200，并打印各容器状态
#>
[CmdletBinding()]
param(
    [int]$EngineWaitSeconds = 240,
    [int]$ProbeWaitSeconds = 180
)

$ErrorActionPreference = 'Continue'
$dockerDir = Join-Path $PSScriptRoot 'dify\docker'
$dockerExe = 'C:\Program Files\Docker\Docker\Docker Desktop.exe'

function Test-Engine {
    $v = docker info --format '{{.ServerVersion}}' 2>$null
    return [bool]$v
}

Write-Host '== 1/4 检查 Docker 引擎 ==' -ForegroundColor Cyan
if (-not (Test-Engine)) {
    Write-Host '   引擎未运行，尝试启动 Docker Desktop…'
    if (-not (Test-Path $dockerExe)) {
        Write-Host "   找不到 $dockerExe，请手动启动 Docker Desktop 后重跑本脚本。" -ForegroundColor Red
        exit 1
    }
    Start-Process $dockerExe
    $deadline = (Get-Date).AddSeconds($EngineWaitSeconds)
    while ((Get-Date) -lt $deadline) {
        Start-Sleep -Seconds 5
        if (Test-Engine) { break }
        Write-Host '   等待引擎…'
    }
    if (-not (Test-Engine)) {
        Write-Host '   引擎仍未就绪：请打开 Docker Desktop 看看是否卡在"接受协议/更新"上。' -ForegroundColor Red
        exit 1
    }
}
Write-Host ('   引擎版本：' + (docker info --format '{{.ServerVersion}}')) -ForegroundColor Green

Write-Host '== 2/4 拉起 Dify 容器 ==' -ForegroundColor Cyan
if (-not (Test-Path $dockerDir)) {
    Write-Host "   找不到 $dockerDir，请确认 v2.0/ai/dify 是否还在（见 v2.0/ai/README.md 重新拉取）。" -ForegroundColor Red
    exit 1
}
Push-Location $dockerDir
docker compose up -d 2>&1 | Out-Null
Write-Host '   compose up -d 已执行' -ForegroundColor Green

Write-Host '== 3/4 停掉可选容器（省内存） ==' -ForegroundColor Cyan
docker compose stop agent_backend local_sandbox 2>&1 | Out-Null
Write-Host '   agent_backend / local_sandbox 已停' -ForegroundColor Green

Write-Host '== 4/4 等待 8090 可访问 ==' -ForegroundColor Cyan
$deadline = (Get-Date).AddSeconds($ProbeWaitSeconds)
$ok = $false
while ((Get-Date) -lt $deadline) {
    try {
        $r = Invoke-WebRequest -Uri 'http://localhost:8090/console/api/setup' -TimeoutSec 8 -UseBasicParsing
        if ($r.StatusCode -eq 200) { $ok = $true; break }
    } catch { }
    Start-Sleep -Seconds 5
    Write-Host '   等待 8090…'
}

docker compose ps --format '{{.Name}} | {{.Status}}'
Pop-Location

if ($ok) {
    Write-Host ''
    Write-Host 'Dify 已就绪：http://localhost:8090' -ForegroundColor Green
    Write-Host '  账号：admin@campus.local / Dify@2026demo'
    Write-Host '  后端体检：http://localhost:8080/api/ai/health'
} else {
    Write-Host ''
    Write-Host '8090 还没起来：先 docker compose logs api 看报错；内存不足时把 Docker Desktop 内存调大。' -ForegroundColor Yellow
}