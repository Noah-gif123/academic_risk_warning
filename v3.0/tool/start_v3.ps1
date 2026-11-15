<#
.SYNOPSIS
  v3.0 一键启动：后端（18101）+ 前端（5174），可选真实 AI。
.DESCRIPTION
  只启动已编译的代码，不重建数据库；数据库请先用 v3.0/tool/run_v3_pipeline.ps1 生成。
  启动日志写入 v3.0/.tmp-start/，PID 写入 v3.0/.tmp-start/pids.json。
.EXAMPLE
  powershell -NoProfile -ExecutionPolicy Bypass -File v3.0/tool/start_v3.ps1
  powershell -NoProfile -ExecutionPolicy Bypass -File v3.0/tool/start_v3.ps1 -EnableAi
  powershell -NoProfile -ExecutionPolicy Bypass -File v3.0/tool/start_v3.ps1 -BackendPort 18102 -FrontendPort 5175
#>
param(
    [int]$BackendPort = 18101,
    [int]$FrontendPort = 5174,
    [string]$Db = 'study_warning_system_v3',
    [switch]$EnableAi,
    [switch]$SkipFrontend,
    [switch]$DebugCaptcha
)

$ErrorActionPreference = 'Stop'
$v3Dir = Split-Path -Parent $PSScriptRoot
$root = Split-Path -Parent $v3Dir
$backend = Join-Path $root 'v2.0\backend'
$frontend = Join-Path $root 'v2.0\frontend'
$runDir = Join-Path $v3Dir '.tmp-start'
New-Item -ItemType Directory -Force -Path $runDir | Out-Null
$pidFile = Join-Path $runDir 'pids.json'
$backendLog = Join-Path $runDir 'backend.log'
$frontendLog = Join-Path $runDir 'frontend.log'

function Wait-Http([string]$url, [int]$seconds = 45) {
    for ($i = 0; $i -lt $seconds; $i++) {
        Start-Sleep -Seconds 1
        try {
            $r = Invoke-WebRequest -Uri $url -UseBasicParsing -TimeoutSec 2
            if ($r.StatusCode -eq 200) { return $true }
        } catch { }
    }
    return $false
}
function Port-Busy([int]$port) {
    try { return [bool](Get-NetTCPConnection -LocalPort $port -State Listen -ErrorAction Stop) } catch { return $false }
}

if (-not (Test-Path (Join-Path $backend 'build\classes\java\main'))) {
    throw "后端未编译：先执行 cd v2.0/backend && .\gradlew.bat compileJava。"
}
if (-not (Test-Path (Join-Path $backend 'build-verify-lib'))) {
    throw "缺少 build-verify-lib，无法组装运行 classpath。"
}
# 保证 classpath 里的资源是最新的（application.yml、ai/tools.yaml 等）
$resSrc = Join-Path $backend 'src\main\resources'
$resDst = Join-Path $backend 'build\resources\main'
New-Item -ItemType Directory -Force -Path $resDst | Out-Null
Copy-Item (Join-Path $resSrc '*') $resDst -Recurse -Force

$extraJar = Get-ChildItem (Join-Path $env:USERPROFILE '.gradle\caches\modules-2\files-2.1\com.github.librepdf\openpdf') -Recurse -Filter 'openpdf-*.jar' -ErrorAction SilentlyContinue | Select-Object -First 1
$extra = if ($extraJar) { $extraJar.FullName } else { '' }
$libs = (Get-ChildItem (Join-Path $backend 'build-verify-lib') -Filter *.jar | ForEach-Object { $_.FullName }) -join ';'
$cp = (Join-Path $backend 'build\classes\java\main') + ';' + (Join-Path $backend 'build\resources\main') + ';' + $libs
if ($extra) { $cp += ';' + $extra }
$url = 'jdbc:mysql://127.0.0.1:3306/' + $Db + '?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true&useSSL=false'

$running = @()
if (Port-Busy $BackendPort) {
    Write-Host "后端端口 $BackendPort 已占用，复用现有服务" -ForegroundColor Yellow
} else {
    if (Test-Path $backendLog) { Remove-Item $backendLog -Force -ErrorAction SilentlyContinue }
    $env:AI_ENABLED = if ($EnableAi) { 'true' } else { 'false' }
    $args = @('-Xmx512m', '-Dfile.encoding=UTF-8', '-cp', $cp,
        'com.example.academic_risk_warning.AcademicRiskWarningApplication',
        "--server.port=$BackendPort", "--spring.datasource.url=$url")
    if ($DebugCaptcha) { $args += '--app.auth.captcha-debug=true' }
    $proc = Start-Process -FilePath 'java' -ArgumentList $args -WorkingDirectory $backend -RedirectStandardOutput $backendLog -RedirectStandardError ($backendLog + '.err') -PassThru -WindowStyle Hidden
    $running += [pscustomobject]@{ name = 'backend'; pid = $proc.Id; port = $BackendPort; log = $backendLog }
    Write-Host "后端启动中 pid=$($proc.Id)，等待 /api/ai/health ..."
    if (-not (Wait-Http "http://127.0.0.1:$BackendPort/api/ai/health" 50)) {
        throw "后端未就绪，查看日志：$backendLog"
    }
    Write-Host "后端就绪：http://127.0.0.1:$BackendPort" -ForegroundColor Green
}

if (-not $SkipFrontend) {
    if (Port-Busy $FrontendPort) {
        Write-Host "前端端口 $FrontendPort 已占用，复用现有服务" -ForegroundColor Yellow
    } else {
        if (Test-Path $frontendLog) { Remove-Item $frontendLog -Force -ErrorAction SilentlyContinue }
        $env:VITE_API_TARGET = "http://127.0.0.1:$BackendPort"
        $env:ESBUILD_WORKER_THREADS = '1'
        $proc = Start-Process -FilePath 'npm.cmd' -ArgumentList @('run', 'dev', '--', '--port', "$FrontendPort", '--strictPort') -WorkingDirectory $frontend -RedirectStandardOutput $frontendLog -RedirectStandardError ($frontendLog + '.err') -PassThru -WindowStyle Hidden
        $running += [pscustomobject]@{ name = 'frontend'; pid = $proc.Id; port = $FrontendPort; log = $frontendLog }
        Write-Host "前端启动中 pid=$($proc.Id)，等待页面 ..."
        if (-not (Wait-Http "http://localhost:$FrontendPort/index.html" 60)) {
            throw "前端未就绪，查看日志：$frontendLog"
        }
        Write-Host "前端就绪：http://localhost:$FrontendPort/index.html#/login" -ForegroundColor Green
    }
}

$running | ConvertTo-Json | Set-Content -Path $pidFile -Encoding UTF8
Write-Host ""
Write-Host "演示账号（口令均为 000000）" -ForegroundColor Cyan
Write-Host "  学生 20250001 / 教师 1001 / 辅导员 C001 / 教务 admin001"
Write-Host "停止服务： powershell -File v3.0/tool/stop_v3.ps1"
Write-Host "日志目录： $runDir"
