<#
.SYNOPSIS
  引导 v3.0 独立库：从 v2.0 回滚快照克隆结构/参考数据，再生成 v3.0 拟真数据。

.DESCRIPTION
  P0-4 的基础设施（v2.0 演示库完全不动）：
  1) 用 snapshot_v2.0.sql 新建 study_warning_system_v3（结构 + 参考数据 + v2 旧主线数据）；
  2) 运行 v3.0 独立生成器（v3.0/tool/gen_v3_seed.js）产出 v3.0/sql/seeds/v3_seed.sql，
     该 SQL 会清空旧主线并按 params.json 重新生成；
  3) 应用 P0-2 / P0-3 数据脚本（course_factor 初始化、λ+熔断）；
  4) 校验规模（教师/课程/行政班/教学班/选课）并打印后续启动方式。

.PARAMETER SkipSeed
  只克隆结构，不生成数据（用于调试生成器）。

.PARAMETER SkipP0Sql
  生成数据后不应用 P0-2/P0-3 脚本。

.EXAMPLE
  powershell -NoProfile -ExecutionPolicy Bypass -File v3.0/tool/bootstrap_v3.ps1 -Force

.NOTES
  PS 5.1 下本文件必须保存为 UTF-8 带 BOM。
#>
param(
    [string]$Db = 'study_warning_system_v3',
    [string]$Snapshot = '',
    [string]$Manifest = '',
    [string]$Server = '127.0.0.1',
    [int]$Port = 3306,
    [string]$DbUser = 'root',
    [string]$DbPassword = 'root',
    [string]$MySql = '',
    [string]$MySqlDump = '',
    [switch]$SkipSeed,
    [switch]$SkipP0Sql,
    [switch]$Force
)

$ErrorActionPreference = 'Stop'
. (Join-Path $PSScriptRoot 'lib\db_common.ps1')

$v3Dir = Split-Path -Parent $PSScriptRoot
if (-not $Snapshot) { $Snapshot = Join-Path $v3Dir 'sql\snapshot_v2.0.sql' }
if (-not $Manifest) { $Manifest = Join-Path $v3Dir 'sql\snapshot_v2.0.manifest.json' }
$Snapshot = [IO.Path]::GetFullPath($Snapshot)
$Manifest = [IO.Path]::GetFullPath($Manifest)
$seedSql = [IO.Path]::GetFullPath((Join-Path $v3Dir 'sql\seeds\v3_seed.sql'))
$genJs = [IO.Path]::GetFullPath((Join-Path $v3Dir 'tool\gen_v3_seed.js'))

Assert-PathExists $Snapshot 'v2.0 回滚快照（先跑 snapshot_v2.0.ps1）'
$manifestObj = Read-Manifest -Path $Manifest
$sha = Get-Sha256 -Path $Snapshot
if ($sha -ne [string]$manifestObj.snapshot.sha256) {
    throw ("快照文件与清单不一致：`n  清单 sha256={0}`n  实际 sha256={1}" -f $manifestObj.snapshot.sha256, $sha)
}

$ctx = New-DbContext -Server $Server -Port $Port -Db $Db -User $DbUser -Password $DbPassword -MySql $MySql -MySqlDump $MySqlDump

if (-not $Force) {
    $ans = Read-Host ("确认重建 v3.0 库 {0}（v2.0 库不受影响）？输入 yes 继续" -f $Db)
    if ($ans -ne 'yes') { throw '已取消' }
}

Write-Host ("== 克隆 v2.0 快照 -> {0}" -f $Db)
Remove-Database -Ctx $ctx -Database $Db
New-Database -Ctx $ctx -Database $Db
Import-SqlFile -Ctx $ctx -Database $Db -File $Snapshot

if (-not $SkipSeed) {
    if (-not (Test-Path -LiteralPath $genJs)) { throw "缺少 v3.0 生成器：$genJs" }
    Write-Host "== 运行 v3.0 生成器（node gen_v3_seed.js）"
    & node $genJs
    if ($LASTEXITCODE -ne 0) { throw "v3 生成器失败（exit=$LASTEXITCODE）" }
    Assert-PathExists $seedSql 'v3 种子 SQL'
    Write-Host ("== 导入 v3 种子：{0}" -f $seedSql)
    Import-SqlFile -Ctx $ctx -Database $Db -File $seedSql

    if (-not $SkipP0Sql) {
        foreach ($name in @('40_p0_2_course_factor_init.sql', '41_p0_3_lambda_and_fuse.sql',
                            '50_stats_ddl.sql', '51_p1_intervention_review.sql',
                            '52_stats_term_id.sql', '53_stats_source_indexes.sql')) {
            $f = [IO.Path]::GetFullPath((Join-Path $v3Dir ("sql\" + $name)))
            Assert-PathExists $f ("P0 数据脚本 " + $name)
            Write-Host ("== 应用 {0}" -f $name)
            Import-SqlFile -Ctx $ctx -Database $Db -File $f
        }
    }
}

Write-Host "== 校验 v3.0 库规模"
function Get-Scalar([string]$sql) {
    return [string](@(Invoke-DbQuery -Ctx $ctx -Sql $sql -Database $Db) | Select-Object -First 1)
}
$scale = [ordered]@{
    tables           = Get-Scalar "select count(*) from information_schema.tables where table_schema='$Db'"
    teachers         = Get-Scalar 'select count(*) from teacher'
    courses          = Get-Scalar 'select count(*) from course'
    adminClasses     = Get-Scalar 'select count(*) from administrative_class'
    teachingClasses  = Get-Scalar 'select count(*) from teaching_class'
    students         = Get-Scalar 'select count(*) from student'
    enrollments      = Get-Scalar 'select count(*) from enrollment where term_id=5'
    scoreResults     = Get-Scalar 'select count(*) from score_result where term_id=5'
    attendance       = Get-Scalar 'select count(*) from attendance_record where term_id=5'
    historyTerms     = Get-Scalar "select count(*) from term where is_current=0 and term_code like '20%'"
}
$scale.GetEnumerator() | ForEach-Object { Write-Host ("   {0,-16} = {1}" -f $_.Key, $_.Value) }

Write-Host ""
Write-Host "v3.0 库已就绪。启动 v3.0 后端（连 v3 库）：" -ForegroundColor Green
Write-Host ("  powershell -NoProfile -ExecutionPolicy Bypass -File v3.0\tool\run_v3_backend.ps1 -Port 8080  # 库=" + $Db)
