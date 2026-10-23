# 一键重建演示库并补齐第二阶段演示痕迹
#   1) SQL：19 清理 → 20 重建（tool/20_rebuild_demo.sql，由 tool/seed_demo.js 生成）→ 21 收尾
#   2) 起后端后：跑一次判定（admin，当前学期）→ 补演示痕迹（认领/处理/申诉/回复）→ 发布 5 门课导学
#
# 用法（先启动后端：java -jar ... --server.port=18080 --app.auth.captcha-debug=true）：
#   powershell -NoProfile -File v2.0\tool\rebuild_demo.ps1
#   powershell -NoProfile -File v2.0\tool\rebuild_demo.ps1 -Base http://127.0.0.1:18080
#   powershell -NoProfile -File v2.0\tool\rebuild_demo.ps1 -SkipSql      # 只补接口侧数据
param(
  [string]$Base = 'http://127.0.0.1:18080',
  [string]$MySql = 'C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe',
  [string]$Db = 'study_warning_system_v2',
  [string]$DbUser = 'root',
  [string]$DbPassword = 'root',
  [switch]$SkipSql,
  [switch]$SkipApi
)

$ErrorActionPreference = 'Stop'
$toolDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$root = Split-Path -Parent $toolDir           # v2.0
$sqlDir = Join-Path $root 'sql'

function Step($text) { Write-Host ""; Write-Host ("=== " + $text + " ===") -ForegroundColor Cyan }

if (-not $SkipSql) {
  if (-not (Test-Path $MySql)) { throw "找不到 mysql 客户端：$MySql（可用 -MySql 指定）" }
  $env:MYSQL_PWD = $DbPassword
  $files = @(
    (Join-Path $sqlDir '19_cleanup_demo.sql'),
    (Join-Path $toolDir '20_rebuild_demo.sql'),
    (Join-Path $sqlDir '21_tidy_after_rebuild.sql'),
    (Join-Path $sqlDir '25_alert_reply.sql')
  )
  foreach ($f in $files) {
    if (-not (Test-Path $f)) { throw "缺少脚本：$f" }
    Step ("SQL: " + (Split-Path -Leaf $f))
    & $MySql -u $DbUser --default-character-set=utf8mb4 --execute="source $f"
    if ($LASTEXITCODE -ne 0) { throw "执行失败：$f" }
  }
  Write-Host "  SQL 完成（19 → 20 → 21 → 25）" -ForegroundColor Green
}

if (-not $SkipApi) {
  Step '接口：判定 + 演示痕迹 + 导学'
  & powershell -NoProfile -File (Join-Path $toolDir 'run_engine_after_seed.ps1') -Base $Base
  if ($LASTEXITCODE -ne 0) { throw '判定脚本失败（后端是否已启动？）' }
  & powershell -NoProfile -File (Join-Path $toolDir 'stage2_demo_data.ps1')
  if ($LASTEXITCODE -ne 0) { throw '演示痕迹脚本失败' }
  & powershell -NoProfile -File (Join-Path $toolDir 'publish_guides.ps1')
  if ($LASTEXITCODE -ne 0) { throw '导学发布脚本失败' }
}

Step '完成'
Write-Host '演示库已就绪：' -ForegroundColor Green
Write-Host '  · 5 门课 / 12 教学班 / 540 选课；试卷 124 张 + 2 张进行中，项目 12 个'
Write-Host '  · 预警由引擎生成；计基 01 班有 1 条「处理中」+ 1 条「学生申诉已回复」'
Write-Host '  · 5 门课的导学已发布（成绩构成 + 各小项 + 项目要求）'
Write-Host '  打开前端 http://localhost:5173 ，账号 20250001 / 1001 / 1003 / admin001（口令 000000）'
