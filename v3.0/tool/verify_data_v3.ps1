<#
.SYNOPSIS
  v3.0 数据校验（P0-6）：分类合计=样本数、无孤儿、三通道非空率 100%、v3 数据口径。

.DESCRIPTION
  只读校验 study_warning_system_v3（默认），输出逐项 PASS/FAIL 到控制台与 markdown 报告；
  有失败项时以非零退出码结束（供 ci.ps1 门禁）。
  校验项：
    A 分类合计 = 样本数（risk_state 等级合计、与选课/成绩单对账）
    B 无孤儿（选课/教学班/开课/风险/考勤/分项/成绩/教师尺度/挂科率 的外键关系）
    C 三通道非空率 100%（A/B/C 等级 + 生效通道 + 风险等级 + 课程因子 + 置信度）
    D v3 数据口径（course_factor 已初始化、λ=n/(n+k)、熔断原因、对照组 60:40、规模区间）

.EXAMPLE
  powershell -NoProfile -ExecutionPolicy Bypass -File v3.0/tool/verify_data_v3.ps1
#>
param(
    [string]$Db = 'study_warning_system_v3',
    [string]$Out = '',
    [string]$Params = ''
)

$ErrorActionPreference = 'Stop'
. (Join-Path $PSScriptRoot 'lib\db_common.ps1')

$v3Dir = Split-Path -Parent $PSScriptRoot
if (-not $Out) { $Out = Join-Path $v3Dir 'evidence\verify_data_v3.md' }
if (-not $Params) { $Params = Join-Path $v3Dir 'sql\seeds\params.json' }
$p = Get-Content -LiteralPath $Params -Raw -Encoding UTF8 | ConvertFrom-Json
$cohortFile = Join-Path $v3Dir 'sql\seeds\v3_cohort.json'

$ctx = New-DbContext -Db $Db
$rows2 = New-Object System.Collections.Generic.List[object]
$fail = 0
$idx = 0

function Scalar([string]$sql) {
    $v = @(Invoke-DbQuery -Ctx $ctx -Sql $sql -Database $Db) | Select-Object -First 1
    if ($null -eq $v -or [string]$v -eq '') { return 'NULL' }
    return [string]$v
}
function Add-Row([string]$name, [string]$expect, [string]$actual, [bool]$ok) {
    $script:idx++
    $script:rows2.Add([pscustomobject]@{ id = $script:idx; name = $name; expect = $expect; actual = $actual; ok = $ok })
    if ($ok) {
        Write-Host ("  PASS [" + $script:idx + "] " + $name) -ForegroundColor Green
    } else {
        $script:fail++
        Write-Host ("  FAIL [" + $script:idx + "] " + $name + "：期望 " + $expect + "，实际 " + $actual) -ForegroundColor Red
    }
}
function Check-Equal([string]$name, [string]$sql, [string]$expect) {
    $actual = Scalar $sql
    Add-Row $name $expect $actual ([string]$actual -eq [string]$expect)
}
function Check-Zero([string]$name, [string]$sql) {
    Check-Equal $name $sql '0'
}
function Check-Range([string]$name, [string]$sql, [double]$min, [double]$max) {
    $actual = Scalar $sql
    $num = 0.0
    $ok = [double]::TryParse($actual, [ref]$num) -and $num -ge $min -and $num -le $max
    Add-Row $name ("[" + $min + ", " + $max + "]") $actual $ok
}

Write-Host ("== v3.0 数据校验：" + $Db + "（seed=" + $p.seed + "）") -ForegroundColor Cyan

# ---------- A 分类合计 = 样本数
Write-Host "-- A 分类合计 = 样本数" -ForegroundColor Yellow
Check-Zero 'A1 risk_state 等级合计与总数一致' @'
select (select count(*) from risk_state)
     - (select sum(c) from (select count(*) c from risk_state group by risk_level) x)
'@
Check-Zero 'A2 risk_state 行数 = 当前学期选课数' @'
select abs((select count(*) from risk_state where term_id=5)
         - (select count(*) from enrollment where term_id=5))
'@
Check-Zero 'A3 成绩单行数 = 当前学期选课数' @'
select abs((select count(*) from score_result where term_id=5)
         - (select count(*) from enrollment where term_id=5))
'@
Check-Zero 'A4 每条选课都有分项成绩' @'
select count(*) from enrollment e
 where e.term_id = 5
   and not exists (select 1 from score_item si where si.enrollment_id = e.id)
'@
Check-Zero 'A5 考勤无重复（选课×课次）' @'
select count(*) from (
  select enrollment_id, session_id, count(*) c
    from attendance_record group by enrollment_id, session_id having c > 1
) x
'@

# ---------- B 无孤儿
Write-Host "-- B 无孤儿" -ForegroundColor Yellow
Check-Zero 'B1 选课→学生' 'select count(*) from enrollment e left join student s on s.id=e.student_id where s.id is null'
Check-Zero 'B2 选课→教学班' 'select count(*) from enrollment e left join teaching_class t on t.id=e.teaching_class_id where t.id is null'
Check-Zero 'B3 选课→开课' 'select count(*) from enrollment e left join course_offering o on o.id=e.offering_id where o.id is null'
Check-Zero 'B4 学生→行政班' 'select count(*) from student s left join administrative_class a on a.id=s.admin_class_id where a.id is null'
Check-Zero 'B5 教学班→开课' 'select count(*) from teaching_class t left join course_offering o on o.id=t.offering_id where o.id is null'
Check-Zero 'B6 教学班→教师' 'select count(*) from teaching_class t left join teacher x on x.id=t.teacher_id where x.id is null'
Check-Zero 'B7 教学班↔行政班→教学班' 'select count(*) from teaching_class_admin_class m left join teaching_class t on t.id=m.teaching_class_id where t.id is null'
Check-Zero 'B8 教学班↔行政班→行政班' 'select count(*) from teaching_class_admin_class m left join administrative_class a on a.id=m.admin_class_id where a.id is null'
Check-Zero 'B9 开课→课程' 'select count(*) from course_offering o left join course c on c.id=o.course_id where c.id is null'
Check-Zero 'B10 开课→学期' 'select count(*) from course_offering o left join term t on t.id=o.term_id where t.id is null'
Check-Zero 'B11 风险状态→选课' 'select count(*) from risk_state r left join enrollment e on e.id=r.enrollment_id where e.id is null'
Check-Zero 'B12 考勤→选课' 'select count(*) from attendance_record a left join enrollment e on e.id=a.enrollment_id where e.id is null'
Check-Zero 'B13 考勤→课次' 'select count(*) from attendance_record a left join course_session s on s.id=a.session_id where s.id is null'
Check-Zero 'B14 分项→选课' 'select count(*) from score_item i left join enrollment e on e.id=i.enrollment_id where e.id is null'
Check-Zero 'B15 分项→评分项' 'select count(*) from score_item i left join score_scheme_item s on s.id=i.scheme_item_id where s.id is null'
Check-Zero 'B16 成绩单→选课' 'select count(*) from score_result r left join enrollment e on e.id=r.enrollment_id where e.id is null'
Check-Zero 'B17 教师尺度→教师' 'select count(*) from teacher_style t left join teacher x on x.id=t.teacher_id where x.id is null'
Check-Zero 'B18 教师尺度→开课' 'select count(*) from teacher_style t left join course_offering o on o.id=t.offering_id where o.id is null'
Check-Zero 'B19 课程挂科率→课程' 'select count(*) from course_fail_rate_history h left join course c on c.id=h.course_id where c.id is null'
Check-Zero 'B20 历史挂科→学生' 'select count(*) from student_fail_history h left join student s on s.id=h.student_id where s.id is null'

# ---------- C 三通道非空率 100%
Write-Host "-- C 三通道非空率 100%" -ForegroundColor Yellow
Check-Zero 'C1 三通道 + 等级/通道/因子/置信度 无空值' @'
select count(*) from risk_state
 where channel_a_level is null or channel_b_level is null or channel_c_level is null
    or active_channel is null or risk_level is null or course_factor_used is null
    or confidence is null
'@
Check-Zero 'C2 风险等级合法' "select count(*) from risk_state where risk_level not in ('RED','ORANGE','YELLOW','GREEN')"
Check-Zero 'C3 生效通道合法' "select count(*) from risk_state where active_channel not in ('A','B','C')"

# ---------- D v3 数据口径
Write-Host "-- D v3 数据口径" -ForegroundColor Yellow
$cfrExpect = ([int]$p.courses.Count) * ([int]$p.terms.history.Count + 1)
Check-Equal 'D1 课程挂科率行数 = 课程数×(历史+当前)' 'select count(*) from course_fail_rate_history' ([string]$cfrExpect)
Check-Zero 'D2 课程因子全部初始化' 'select count(*) from course_fail_rate_history where is_initialized <> 1 or course_factor is null'
Check-Zero 'D3 课程因子在 [0.90,1.15]' 'select count(*) from course_fail_rate_history where course_factor < 0.90 or course_factor > 1.15'
Check-Zero 'D4 非熔断行 λ = n/(n+k)（容差 0.002）' @'
select count(*) from teacher_style ts
  join risk_engine_config c on c.is_active = 1
 where ts.fused = 0
   and abs(ts.shrink_lambda - ts.sample_size / (ts.sample_size + c.delta_shrink_k)) > 0.002
'@
Check-Zero 'D5 熔断行都有原因' "select count(*) from teacher_style where fused = 1 and (fuse_reason is null or fuse_reason = '')"
Check-Range 'D6 熔断案例数' 'select count(*) from teacher_style where fused = 1' 1 3
Check-Equal 'D7 当前学期唯一' 'select count(*) from term where is_current = 1' '1'
Check-Equal 'D8 学期数 = 当前+历史' 'select count(*) from term' ([string]([int]$p.terms.history.Count + 1))
Check-Range 'D9 教师数（12–15）' 'select count(*) from teacher' 12 15
Check-Range 'D10 课程数（8–10）' 'select count(*) from course' 8 10
Check-Range 'D11 行政班数（6–8）' 'select count(*) from administrative_class' 6 8
Check-Zero 'D12 行政班规模 30–45 人' 'select count(*) from (select admin_class_id, count(*) c from student group by admin_class_id) x where c < 30 or c > 45'
Check-Zero 'D13 教学班规模不超过容量' 'select count(*) from (select teaching_class_id, count(*) c from enrollment group by teaching_class_id) x join teaching_class t on t.id=x.teaching_class_id where x.c > t.capacity'
Check-Zero 'D14 重修班标记与选课一致' 'select count(*) from enrollment e join teaching_class t on t.id=e.teaching_class_id where t.is_retake_class <> e.retake_times'

# 对照组（JSON）
if (Test-Path -LiteralPath $cohortFile) {
    $cohort = (Get-Content -LiteralPath $cohortFile -Raw -Encoding UTF8 | ConvertFrom-Json)
    $vals = @($cohort.cohort.PSObject.Properties | ForEach-Object { $_.Value })
    $treat = @($vals | Where-Object { $_ -eq 'TREAT' }).Count
    $control = @($vals | Where-Object { $_ -eq 'CONTROL' }).Count
    $total = $treat + $control
    $ratio = if ($total -gt 0) { [math]::Round($treat * 100.0 / $total, 1) } else { 0 }
    Add-Row 'D15 对照组人数 60:40（干预占比 55–65%）' '[55, 65]' ([string]$ratio + '%（TREAT=' + $treat + '/CONTROL=' + $control + '）') ($ratio -ge 55 -and $ratio -le 65)
} else {
    Add-Row 'D15 对照组文件存在' 'v3_cohort.json' '缺少' $false
}

# ---------- 汇总 + 报告
$totalChecks = $idx
$pass = $totalChecks - $fail
$summary = if ($fail -eq 0) { 'PASS' } else { 'FAIL' }
Write-Host ""
Write-Host ("== 校验完成：" + $pass + "/" + $totalChecks + " 通过（" + $summary + "）") -ForegroundColor $(if ($fail -eq 0) { 'Green' } else { 'Red' })

$md = New-Object System.Collections.Generic.List[string]
$md.Add('# v3.0 数据校验报告（P0-6）')
$md.Add('')
$md.Add('- 库：`' + $Db + '`；seed：`' + $p.seed + '`；生成时间：' + (Get-Date).ToString('yyyy-MM-ddTHH:mm:sszzz'))
$md.Add('- 结果：**' + $pass + ' / ' + $totalChecks + ' 通过（' + $summary + '）**')
$md.Add('')
$md.Add('| # | 检查项 | 期望 | 实际 | 结果 |')
$md.Add('|---|---|---|---|---|')
foreach ($r in $rows2) {
    $md.Add('| ' + $r.id + ' | ' + $r.name + ' | ' + $r.expect + ' | ' + $r.actual + ' | ' + $(if ($r.ok) { '✅' } else { '❌' }) + ' |')
}
$md.Add('')
$md.Add('> 复现：`powershell -NoProfile -ExecutionPolicy Bypass -File v3.0/tool/verify_data_v3.ps1`')
[IO.File]::WriteAllText([IO.Path]::GetFullPath($Out), ($md -join "`n"), (New-Object System.Text.UTF8Encoding($false)))
Write-Host ("   报告 -> " + $Out)

if ($fail -gt 0) { exit 1 }
exit 0
