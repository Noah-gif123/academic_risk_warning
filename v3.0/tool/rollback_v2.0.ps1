<#
.SYNOPSIS
  用 snapshot_v2.0.sql 回滚数据库，或做一次"恢复到临时库"的演练。

.DESCRIPTION
  P0-1 的一部分。两种模式：
  - 演练（-Drill）：把快照导入临时库 study_warning_system_v2_verify，逐表行数 + 关键口径与清单比对，然后删除临时库（-KeepDrillDb 可保留）。不动正式库。
  - 回滚（默认）：先给正式库拍一张安全快照（v3.0/sql/_backup/），再删库重建并导入快照，最后按清单校验。
  校验不通过时以非零退出码结束。

.PARAMETER Drill
  演练模式：只操作临时库。

.PARAMETER KeepDrillDb
  演练后保留临时库（便于接着用后端跑 v2.0 验收脚本）。

.PARAMETER Force
  回滚正式库时跳过交互确认（脚本化调用用）。

.PARAMETER SkipSafetyBackup
  回滚正式库前不拍安全快照（不推荐）。

.EXAMPLE
  # 恢复演练（推荐每次改数据前跑一次）
  pwsh -NoProfile -File v3.0/tool/rollback_v2.0.ps1 -Drill

.EXAMPLE
  # 正式回滚（交互确认）
  pwsh -NoProfile -File v3.0/tool/rollback_v2.0.ps1
#>
param(
    [switch]$Drill,
    [string]$DrillDb = 'study_warning_system_v2_verify',
    [switch]$KeepDrillDb,
    [switch]$Force,
    [switch]$SkipSafetyBackup,
    [string]$Snapshot = '',
    [string]$Manifest = '',
    [string]$Server = '127.0.0.1',
    [int]$Port = 3306,
    [string]$Db = 'study_warning_system_v2',
    [string]$DbUser = 'root',
    [string]$DbPassword = 'root',
    [string]$MySql = '',
    [string]$MySqlDump = '',
    [string]$BackupDir = ''
)

$ErrorActionPreference = 'Stop'
. (Join-Path $PSScriptRoot 'lib\db_common.ps1')

$v3Dir = Split-Path -Parent $PSScriptRoot
if (-not $Snapshot) { $Snapshot = Join-Path $v3Dir 'sql\snapshot_v2.0.sql' }
if (-not $Manifest) { $Manifest = Join-Path $v3Dir 'sql\snapshot_v2.0.manifest.json' }
if (-not $BackupDir) { $BackupDir = Join-Path $v3Dir 'sql\_backup' }
$Snapshot = [IO.Path]::GetFullPath($Snapshot)
$Manifest = [IO.Path]::GetFullPath($Manifest)
$BackupDir = [IO.Path]::GetFullPath($BackupDir)

Assert-PathExists $Snapshot '快照文件（先跑 snapshot_v2.0.ps1 生成）'
$manifestObj = Read-Manifest -Path $Manifest

$sha = Get-Sha256 -Path $Snapshot
if ($sha -ne [string]$manifestObj.snapshot.sha256) {
    throw ("快照文件与清单不一致：`n  清单 sha256={0}`n  实际 sha256={1}" -f $manifestObj.snapshot.sha256, $sha)
}

$ctx = New-DbContext -Server $Server -Port $Port -Db $Db -User $DbUser -Password $DbPassword -MySql $MySql -MySqlDump $MySqlDump
$target = if ($Drill) { $DrillDb } else { $Db }

Write-Host ("== 目标库：{0}（{1}）" -f $target, $(if ($Drill) { '恢复演练' } else { '正式回滚' }))
Write-Host ("   快照：{0}" -f $Snapshot)
Write-Host ("   清单：{0} 表 / {1} 行" -f $manifestObj.tableCount, $manifestObj.totalRows)

if (-not $Drill) {
    if (-not $Force) {
        $ans = Read-Host ("确认把库 {0} 覆盖为快照？输入 yes 继续" -f $Db)
        if ($ans -ne 'yes') { throw '已取消' }
    }
    if (-not $SkipSafetyBackup) {
        if (-not (Test-Path -LiteralPath $BackupDir)) { New-Item -ItemType Directory -Path $BackupDir -Force | Out-Null }
        $stamp = (Get-Date).ToString('yyyyMMdd_HHmmss')
        $safety = Join-Path $BackupDir ("pre_rollback_{0}.sql" -f $stamp)
        Write-Host ("== 回滚前安全快照 -> {0}" -f $safety)
        Invoke-DbDump -Ctx $ctx -Database $Db -ResultFile $safety
    }
}

Write-Host ("== 重建库：{0}" -f $target)
Remove-Database -Ctx $ctx -Database $target
New-Database -Ctx $ctx -Database $target

Write-Host "== 导入快照"
Import-SqlFile -Ctx $ctx -Database $target -File $Snapshot

Write-Host "== 校验：逐表行数 + 关键口径"
$tables = Get-TableNames -Ctx $ctx -Database $target
$counts = Get-TableRowCounts -Ctx $ctx -Database $target -Tables $tables
$checks = Get-KeyChecks -Ctx $ctx -Database $target
$diffs = Compare-SnapshotState -Manifest $manifestObj -ActualCounts $counts -ActualChecks $checks

if ($diffs.Count -gt 0) {
    Write-Host ("!! 校验不通过，差异 {0} 条：" -f $diffs.Count) -ForegroundColor Red
    $diffs | Select-Object -First 50 | ForEach-Object { Write-Host ("   - " + $_) }
    exit 1
}

Write-Host ("== 校验通过：{0} 表 / {1} 行与清单一致" -f $tables.Count, ($counts.Values | Measure-Object -Sum).Sum) -ForegroundColor Green
foreach ($k in $checks.Keys) { Write-Host ("   {0} = {1}" -f $k, $checks[$k]) }

if ($Drill) {
    if ($KeepDrillDb) {
        Write-Host ("== 临时库已保留：{0}（用完请手工删除）" -f $DrillDb)
    } else {
        Remove-Database -Ctx $ctx -Database $DrillDb
        Write-Host ("== 临时库已删除：{0}" -f $DrillDb)
    }
} else {
    Write-Host ("== 回滚完成：{0}" -f $Db) -ForegroundColor Green
}
