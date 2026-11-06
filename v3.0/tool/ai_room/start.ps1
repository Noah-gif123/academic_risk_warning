# AI 群聊工作台一键启动（Windows PowerShell）
# 用法：右键“使用 PowerShell 运行”，或在终端里  .\start.ps1
$ErrorActionPreference = 'Stop'
Set-Location -Path $PSScriptRoot

if (-not (Get-Command node -ErrorAction SilentlyContinue)) {
    Write-Host '未找到 node，请先安装 Node.js 18+' -ForegroundColor Red
    exit 1
}

if (-not (Test-Path 'keys.json')) {
    Copy-Item 'keys.json.example' 'keys.json'
    Write-Host '已生成 keys.json —— 现在可以填 key，也可以先看 Mock 演示。' -ForegroundColor Yellow
}

$port = if ($env:AI_ROOM_PORT) { $env:AI_ROOM_PORT } else { '8099' }
Write-Host "启动中…  http://127.0.0.1:$port" -ForegroundColor Cyan
Start-Process "http://127.0.0.1:$port"
node server.mjs
