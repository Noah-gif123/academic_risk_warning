<#
.SYNOPSIS
  生成 P1-0 演示数据生命周期（v3.0）：告警关闭流程、干预/T+7 复评、对照组、8 周快照。

.DESCRIPTION
  1) 编译 v3.0/tool/demo/GenLifecycle.java（JDBC，仅需 mysql 驱动）；
  2) 对 study_warning_system_v3 执行：清空生命周期派生表 → 重写 662 条告警的
     create/handle/close 时间与状态 → 生成 TREAT/CONTROL 干预与 level_before/level_after
     → 回填 stat_daily_class 最近 8 周；
  3) 同一 seed 可重复执行（逻辑幂等）。

.EXAMPLE
  powershell -NoProfile -ExecutionPolicy Bypass -File v3.0/tool/gen_p1_lifecycle.ps1
#>
param(
    [string]$Db = 'study_warning_system_v3',
    [long]$Term = 5,
    [long]$Seed = 20261108,
    [string]$MySqlJar = ''
)

$ErrorActionPreference = 'Stop'
$v3Dir = Split-Path -Parent $PSScriptRoot
$root = Split-Path -Parent $v3Dir
$src = Join-Path $v3Dir 'tool\demo\GenLifecycle.java'
$classes = Join-Path $v3Dir 'tool\demo\classes'
if (-not $MySqlJar) { $MySqlJar = Join-Path $root 'v2.0\backend\build-verify-lib\mysql-connector-j-9.2.0.jar' }
if (-not (Test-Path -LiteralPath $MySqlJar)) { throw "找不到 MySQL 驱动：$MySqlJar" }
if (-not (Test-Path -LiteralPath $classes)) { New-Item -ItemType Directory -Path $classes -Force | Out-Null }

Write-Host "== 编译 GenLifecycle"
& javac '-encoding' 'UTF-8' '-cp' $MySqlJar '-d' $classes $src
if ($LASTEXITCODE -ne 0) { throw 'GenLifecycle 编译失败' }

Write-Host ("== 生成生命周期：db={0} term={1} seed={2}" -f $Db, $Term, $Seed)
& java '-Dfile.encoding=UTF-8' -cp ($classes + ';' + $MySqlJar) GenLifecycle ("--db=" + $Db) ("--term=" + $Term) ("--seed=" + $Seed)
if ($LASTEXITCODE -ne 0) { throw "GenLifecycle 执行失败（exit=$LASTEXITCODE）" }
Write-Host "== P1-0 生命周期生成完成" -ForegroundColor Green
