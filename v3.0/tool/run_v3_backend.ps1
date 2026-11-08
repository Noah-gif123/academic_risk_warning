<#
.SYNOPSIS
  启动 v3.0 后端（连 v3.0 独立库），用于演示 v3.0 生成后的数据。
.DESCRIPTION
  v2.0 与 v3.0 各用一个库：
    - 运行 v2.0：默认配置 → study_warning_system_v2（原演示数据）
    - 运行 v3.0：本脚本 → study_warning_system_v3（P0-4 生成的新数据）
  前端 v2.0/frontend 代理到 127.0.0.1:8080，所以两个后端不要同时占用同一端口。
.EXAMPLE
  powershell -NoProfile -ExecutionPolicy Bypass -File v3.0/tool/run_v3_backend.ps1
  powershell -NoProfile -ExecutionPolicy Bypass -File v3.0/tool/run_v3_backend.ps1 -Port 8081 -DebugCaptcha
#>
param(
    [int]$Port = 8080,
    [string]$Db = 'study_warning_system_v3',
    [switch]$DebugCaptcha,
    [string]$Xmx = '512m',
    [switch]$EnableAi
)

$ErrorActionPreference = 'Stop'
$v3Dir = Split-Path -Parent $PSScriptRoot
$root = Split-Path -Parent $v3Dir
$backend = Join-Path $root 'v2.0\backend'
if (-not (Test-Path -LiteralPath $backend)) { throw "找不到后端目录：$backend" }

$libs = (Get-ChildItem (Join-Path $backend 'build-verify-lib') -Filter *.jar | ForEach-Object { $_.FullName }) -join ';'
$cp = (Join-Path $backend 'build-verify') + ';' + (Join-Path $backend 'src\main\resources') + ';' + $libs
$url = 'jdbc:mysql://127.0.0.1:3306/' + $Db +
       '?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true&useSSL=false'

$env:AI_ENABLED = if ($EnableAi) { 'true' } else { 'false' }
$argList = @(
    "-Xmx$Xmx", '-Dfile.encoding=UTF-8', '-cp', $cp,
    'com.example.academic_risk_warning.AcademicRiskWarningApplication',
    "--server.port=$Port",
    "--spring.datasource.url=$url"
)
if ($DebugCaptcha) { $argList += '--app.auth.captcha-debug=true' }

Write-Host ("== 启动 v3.0 后端：端口 {0} / 库 {1}" -f $Port, $Db) -ForegroundColor Cyan
Write-Host ("   前端：cd v2.0\frontend && npm run dev  （代理到 127.0.0.1:8080）")
Write-Host ("   v2.0 演示：不传本脚本参数、直接按 v2.0 原方式启动即可（连 study_warning_system_v2）")
Push-Location $backend
try {
    & java @argList
} finally {
    Pop-Location
}
