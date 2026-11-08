<#
.SYNOPSIS
  生成 v2.0 演示库回滚快照（结构 + 数据）及其校验清单。

.DESCRIPTION
  P0-1 的一部分：动任何数据之前，先保证"任何时刻能退回 v2.0 演示快照"。
  - 快照：mysqldump 全库（不含 CREATE DATABASE/USE），默认写到 v3.0/sql/snapshot_v2.0.sql；
  - 清单：同目录 snapshot_v2.0.manifest.json，含 sha256、逐表精确行数、关键口径；
  - 回滚/演练：用 rollback_v2.0.ps1 -Drill。

.PARAMETER Out
  快照输出文件。默认 v3.0/sql/snapshot_v2.0.sql。

.PARAMETER Manifest
  清单输出文件。默认 v3.0/sql/snapshot_v2.0.manifest.json。

.EXAMPLE
  pwsh -NoProfile -File v3.0/tool/snapshot_v2.0.ps1

.NOTES
  PS 5.1 下本文件必须保存为 UTF-8 带 BOM。
#>
param(
    [string]$Server = '127.0.0.1',
    [int]$Port = 3306,
    [string]$Db = 'study_warning_system_v2',
    [string]$DbUser = 'root',
    [string]$DbPassword = 'root',
    [string]$MySql = '',
    [string]$MySqlDump = '',
    [string]$Out = '',
    [string]$Manifest = ''
)

$ErrorActionPreference = 'Stop'
. (Join-Path $PSScriptRoot 'lib\db_common.ps1')

$v3Dir = Split-Path -Parent $PSScriptRoot
if (-not $Out) { $Out = Join-Path $v3Dir 'sql\snapshot_v2.0.sql' }
if (-not $Manifest) { $Manifest = Join-Path $v3Dir 'sql\snapshot_v2.0.manifest.json' }
$Out = [IO.Path]::GetFullPath($Out)
$Manifest = [IO.Path]::GetFullPath($Manifest)
$outDir = Split-Path -Parent $Out
if (-not (Test-Path -LiteralPath $outDir)) { New-Item -ItemType Directory -Path $outDir -Force | Out-Null }

$ctx = New-DbContext -Server $Server -Port $Port -Db $Db -User $DbUser -Password $DbPassword -MySql $MySql -MySqlDump $MySqlDump

Write-Host ("== 生成快照：{0} @ {1}:{2}" -f $Db, $Server, $Port)
$tables = Get-TableNames -Ctx $ctx -Database $Db
if ($tables.Count -eq 0) { throw "库里没有表：$Db" }
Write-Host ("   表数：{0}" -f $tables.Count)

$counts = Get-TableRowCounts -Ctx $ctx -Database $Db -Tables $tables
$totalRows = [long]0
foreach ($k in $counts.Keys) { $totalRows += [long]$counts[$k] }

$checks = Get-KeyChecks -Ctx $ctx -Database $Db

Write-Host ("== mysqldump -> {0}" -f $Out)
Invoke-DbDump -Ctx $ctx -Database $Db -ResultFile $Out

if (-not (Test-Path -LiteralPath $Out)) { throw '快照文件未生成' }
$size = (Get-Item -LiteralPath $Out).Length
if ($size -lt 1024) { throw ("快照文件过小（{0} 字节），疑似导出失败" -f $size) }
if (-not (Select-String -LiteralPath $Out -Pattern 'CREATE TABLE `risk_state`' -Quiet)) {
    throw '快照内容校验失败：未找到 risk_state 建表语句'
}

$sha = Get-Sha256 -Path $Out
$serverVersion = [string](@(Invoke-DbQuery -Ctx $ctx -Sql 'select version()') | Select-Object -First 1)

$manifestObj = [ordered]@{
    project       = 'academic_risk_warning'
    format        = 'v2.0-rollback-snapshot/1'
    db            = $Db
    generatedAt   = (Get-Date).ToString('yyyy-MM-ddTHH:mm:sszzz')
    serverVersion = $serverVersion
    snapshot      = [ordered]@{
        file      = (Split-Path -Leaf $Out)
        sizeBytes = [long]$size
        sha256    = $sha
    }
    tableCount    = $tables.Count
    totalRows     = [long]$totalRows
    tables        = $counts
    keyChecks     = $checks
}
Write-JsonFile -Path $Manifest -Object $manifestObj

Write-Host ("== 完成：{0:N1} MB / {1} 表 / {2} 行" -f ($size / 1MB), $tables.Count, $totalRows)
Write-Host ("   sha256 = {0}" -f $sha)
Write-Host ("   快照   = {0}" -f $Out)
Write-Host ("   清单   = {0}" -f $Manifest)
