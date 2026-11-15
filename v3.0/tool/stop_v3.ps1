param()
$ErrorActionPreference = 'Stop'
$v3Dir = Split-Path -Parent $PSScriptRoot
$pidFile = Join-Path $v3Dir '.tmp-start\pids.json'
if (-not (Test-Path $pidFile)) { Write-Host '没有 start_v3.ps1 生成的 PID 记录'; exit 0 }
$items = @(Get-Content $pidFile -Raw | ConvertFrom-Json)
if ($items -isnot [array]) { $items = @($items) }
foreach ($it in $items) {
    if (-not $it.pid) { continue }
    $p = Get-Process -Id $it.pid -ErrorAction SilentlyContinue
    if ($p) {
        Write-Host "停止 $($it.name) pid=$($it.pid)"
        & taskkill /PID $it.pid /T /F 2>$null | Out-Null
    }
}
Remove-Item $pidFile -Force -ErrorAction SilentlyContinue
Write-Host 'v3 服务已停止'
