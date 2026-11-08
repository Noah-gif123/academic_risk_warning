<#
.SYNOPSIS
  v3.0 P1-8 统计对账（7 接口 ↔ SQL）：图表/接口数字必须与数据库口径一致。

.DESCRIPTION
  1) 起 v3 后端（默认 18085，captcha-debug，只读统计接口）；
  2) 登录教务/辅导员/学生，逐接口取数；
  3) 用 mysql 直查 SQL 对照 7 项检查（overview/distribution/compare/funnel/effect/trend/export）；
  4) 额外验证 401/403、xlsx 导出可下载；
  5) 输出 `v3.0/evidence/verify_stats.md`；有失败项以非零退出（供 ci.ps1 门禁）。

.EXAMPLE
  powershell -NoProfile -ExecutionPolicy Bypass -File v3.0/tool/verify_stats.ps1
#>
param(
    [string]$Db = 'study_warning_system_v3',
    [int]$Port = 18085,
    [long]$TermId = 5,
    [string]$Out = '',
    [switch]$KeepBackend
)

$ErrorActionPreference = 'Stop'
. (Join-Path $PSScriptRoot 'lib\db_common.ps1')

$v3Dir = Split-Path -Parent $PSScriptRoot
$root = Split-Path -Parent $v3Dir
$backend = Join-Path $root 'v2.0\backend'
$tmpDir = Join-Path $root '.tmp-doc'
if (-not $Out) { $Out = Join-Path $v3Dir 'evidence\verify_stats.md' }
if (-not (Test-Path -LiteralPath $tmpDir)) { New-Item -ItemType Directory -Path $tmpDir -Force | Out-Null }
$ctx = New-DbContext -Db $Db
$results = @()
function Check([string]$name, [bool]$ok, [string]$detail) {
    $script:results += [pscustomobject]@{ name = $name; ok = $ok; detail = $detail }
    $color = if ($ok) { 'Green' } else { 'Red' }
    Write-Host ("  [" + $(if ($ok) { 'PASS' } else { 'FAIL' }) + "] " + $name + "：" + $detail) -ForegroundColor $color
}
function Sql1([string]$sql) {
    $lines = Invoke-DbQuery -Ctx $ctx -Sql $sql -Database $Db
    $arr = @((($lines | Select-Object -First 1) -split "`t"))
    return ,$arr
}

# ---------------- HTTP helpers ----------------
function Get-Json($url, $token) {
    $headers = @{}
    if ($token) { $headers['Authorization'] = "Bearer $token" }
    $resp = Invoke-WebRequest -Uri $url -Headers $headers -UseBasicParsing
    return ([Text.Encoding]::UTF8.GetString($resp.RawContentStream.ToArray()) | ConvertFrom-Json)
}
function Post-Json($url, $obj, $token) {
    $bytes = [Text.Encoding]::UTF8.GetBytes(($obj | ConvertTo-Json -Depth 6 -Compress))
    $headers = @{}
    if ($token) { $headers['Authorization'] = "Bearer $token" }
    $resp = Invoke-WebRequest -Uri $url -Method Post -Body $bytes -ContentType 'application/json; charset=utf-8' -Headers $headers -UseBasicParsing
    return ([Text.Encoding]::UTF8.GetString($resp.RawContentStream.ToArray()) | ConvertFrom-Json)
}
function Login([string]$base, [string]$loginNo) {
    $cap = (Get-Json "$base/api/auth/captcha" $null).data
    $code = (Get-Json "$base/api/auth/captcha/debug?captchaId=$($cap.captchaId)" $null).data
    $login = Post-Json "$base/api/auth/login" @{ loginNo = $loginNo; password = '000000'; captchaId = $cap.captchaId; captchaCode = $code } $null
    if ($login.code -ne 0) { throw ("登录失败 " + $loginNo) }
    return $login.data.token
}

$java = $null
$sw = [Diagnostics.Stopwatch]::StartNew()
try {
    $libs = (Get-ChildItem (Join-Path $backend 'build-verify-lib') -Filter '*.jar' | ForEach-Object { $_.FullName }) -join ';'
    $cp = (Join-Path $backend 'build-verify') + ';' + (Join-Path $backend 'src\main\resources') + ';' + $libs
    $url = 'jdbc:mysql://127.0.0.1:3306/' + $Db +
        '?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true&useSSL=false'
    $env:AI_ENABLED = 'false'
    $java = Start-Process -FilePath 'java' -ArgumentList @(
        '-Xmx512m', '-Dfile.encoding=UTF-8', '-cp', $cp,
        'com.example.academic_risk_warning.AcademicRiskWarningApplication',
        "--server.port=$Port", '--app.auth.captcha-debug=true', "--spring.datasource.url=$url"
    ) -WorkingDirectory $backend -RedirectStandardOutput (Join-Path $tmpDir 'verify-stats-backend.log') `
        -RedirectStandardError (Join-Path $tmpDir 'verify-stats-backend.log.err') -PassThru -WindowStyle Hidden
    $base = "http://127.0.0.1:$Port"
    $ready = $false
    for ($i = 0; $i -lt 60; $i++) {
        Start-Sleep -Milliseconds 1500
        if ($java.HasExited) { break }
        try {
            $r = Invoke-WebRequest -Uri "$base/api/auth/captcha" -UseBasicParsing -TimeoutSec 2
            if ($r.StatusCode -eq 200) { $ready = $true; break }
        } catch { }
    }
    if (-not $ready) { throw '后端未就绪（' + $Port + '）' }

    $admin = Login $base 'admin001'
    $coun = Login $base 'C001'
    $stu = Login $base '20250001'
    $q = "termId=$TermId&period=term"

    # ---------- 1) overview ----------
    $ov = (Get-Json "$base/api/stats/overview?$q" $admin).data
    $sqlRisk = Sql1 "select count(*), sum(risk_level='RED'), sum(risk_level='ORANGE'), sum(risk_level='YELLOW'), sum(risk_level='GREEN'), round((sum(risk_level in ('RED','ORANGE','YELLOW'))*100.0/count(*)),2) from risk_state where term_id=$TermId and is_void=0"
    $ovOk = ($ov.current.total -eq [int]$sqlRisk[0]) -and ($ov.current.red -eq [int]$sqlRisk[1]) -and
            ($ov.current.orange -eq [int]$sqlRisk[2]) -and ($ov.current.yellow -eq [int]$sqlRisk[3]) -and
            ($ov.current.green -eq [int]$sqlRisk[4]) -and ([math]::Abs([double]$ov.current.riskRatio - [double]$sqlRisk[5]) -le 0.01)
    Check 'overview 风险面' $ovOk ("API {0}/{1}/{2}/{3}/{4} ratio={5}；SQL 一致" -f $ov.current.total, $ov.current.red, $ov.current.orange, $ov.current.yellow, $ov.current.green, $ov.current.riskRatio)

    # ---------- 2) closeRate / handle ----------
    $sqlClose = Sql1 "select sum(close_time is not null), count(*), round(sum(close_time is not null)*100.0/count(*),2) from alert_record where term_id=$TermId and is_void=0 and create_time < now()"
    $closeOk = ([math]::Abs([double]$ov.current.closeRate - [double]$sqlClose[2]) -le 0.01)
    Check 'overview 关闭率' $closeOk ("API {0}%；SQL {1}%（{2}/{3}）" -f $ov.current.closeRate, $sqlClose[2], $sqlClose[0], $sqlClose[1])

    # ---------- 3) distribution ----------
    $dist = (Get-Json "$base/api/stats/distribution?$q" $admin).data
    $sqlCourse = Sql1 "select count(distinct co.course_id) from risk_state rs join course_offering co on co.id=rs.offering_id where rs.term_id=$TermId and rs.is_void=0"
    $distOk = ($dist.current.levels.RED -eq [int]$sqlRisk[1]) -and ($dist.current.byCourse.Count -eq [int]$sqlCourse[0]) -and ($dist.current.calibration.Count -gt 0)
    Check 'distribution 分布/校准' $distOk ("等级 RED={0}；课程 {1} 门；校准 {2} 门" -f $dist.current.levels.RED, $dist.current.byCourse.Count, $dist.current.calibration.Count)

    # ---------- 4) compare ----------
    $cmp = (Get-Json "$base/api/stats/compare?$q" $admin).data
    $sqlClass = Sql1 "select count(distinct teaching_class_id) from risk_state where term_id=$TermId and is_void=0"
    Check 'compare 班级行数' ($cmp.current.rows.Count -eq [int]$sqlClass[0]) ("API {0} 行；SQL {1}" -f $cmp.current.rows.Count, $sqlClass[0])

    # ---------- 5) funnel ----------
    $fun = (Get-Json "$base/api/stats/funnel?$q" $admin).data
    $sqlAlert = Sql1 "select count(*) from alert_record where term_id=$TermId and is_void=0"
    $created = ($fun.current.stages | Where-Object { $_.key -eq 'created' }).value
    Check 'funnel 新增=SQL' ($created -eq [int]$sqlAlert[0]) ("API created={0}；SQL alerts={1}" -f $created, $sqlAlert[0])

    # ---------- 6) effect / 对照组 ----------
    $eff = (Get-Json "$base/api/stats/effect?$q" $admin).data
    $sqlEff = Sql1 "select sum(level_after is not null and effect_check_time between create_time + interval 7 day and create_time + interval 14 day), sum(level_after is not null and effect_check_time between create_time + interval 7 day and create_time + interval 14 day and (case level_after when 'RED' then 4 when 'ORANGE' then 3 when 'YELLOW' then 2 else 1 end) < (case level_before when 'RED' then 4 when 'ORANGE' then 3 when 'YELLOW' then 2 else 1 end)) from intervention_record where term_id=$TermId and is_void=0"
    $effOk = ($eff.current.due -eq [int]$sqlEff[0]) -and ($eff.current.improved -eq [int]$sqlEff[1]) -and ([double]$eff.current.diffPp -ge 20)
    Check 'effect T+7/对照组' $effOk ("API due={0} improved={1} TREAT={2}% CONTROL={3}% diff={4}pp；SQL due={5} improved={6}" -f $eff.current.due, $eff.current.improved, $eff.current.treatRate, $eff.current.controlRate, $eff.current.diffPp, $sqlEff[0], $sqlEff[1])

    # ---------- 7) trend ----------
    $tr = (Get-Json "$base/api/stats/trend?$q" $admin).data
    $sqlTrend = Sql1 "select round(sum(red+orange+yellow)*100.0/sum(student_count),2) from stat_daily_class where stat_date=(select max(stat_date) from stat_daily_class)"
    $last = $tr.current.points[$tr.current.points.Count - 1]
    $trOk = ($tr.dataStatus -eq 'OK') -and ([math]::Abs([double]$last.riskRatio - [double]$sqlTrend[0]) -le 0.01)
    Check 'trend 最新周风险面' $trOk ("points={0} last={1} ratio={2}；SQL={3}" -f $tr.current.points.Count, $last.weekStart, $last.riskRatio, $sqlTrend[0])

    # ---------- 8) export CSV / xlsx ----------
    $csvPath = Join-Path $tmpDir 'verify_stats_export.csv'
    $h = @{ Authorization = "Bearer $admin" }
    Invoke-WebRequest -Uri "$base/api/stats/export?type=overview&$q" -Headers $h -OutFile $csvPath -UseBasicParsing
    $csv = [IO.File]::ReadAllText($csvPath, [Text.Encoding]::UTF8)
    $csvOk = $csv -match ('风险总人数,' + $sqlRisk[0]) -and $csv -match ('红,' + $sqlRisk[1]) -and $csv -match ('橙,' + $sqlRisk[2])
    Check 'export CSV 与接口一致' $csvOk ("CSV 首行=" + (($csv -split "`n")[0]).Trim())

    $xlsxPath = Join-Path $tmpDir 'verify_stats_export.xlsx'
    Invoke-WebRequest -Uri "$base/api/stats/export?type=overview&format=xlsx&$q" -Headers $h -OutFile $xlsxPath -UseBasicParsing
    $magic = [IO.File]::ReadAllBytes($xlsxPath)
    $xlsxOk = ($magic.Length -gt 1000) -and ($magic[0] -eq 0x50) -and ($magic[1] -eq 0x4B)
    Check 'export XLSX 可下载' $xlsxOk ("bytes=" + $magic.Length + "（PK 头）")

    # ---------- 9) 权限 ----------
    $noToken = 0
    try { Get-Json "$base/api/stats/overview" $null | Out-Null } catch { $noToken = [int]$_.Exception.Response.StatusCode.value__ }
    $stuCode = 0
    try { Get-Json "$base/api/stats/overview" $stu | Out-Null } catch { $stuCode = [int]$_.Exception.Response.StatusCode.value__ }
    Check '权限 401/403' (($noToken -eq 401) -and ($stuCode -eq 403)) ("无 token={0}；学生={1}" -f $noToken, $stuCode)

    # ---------- 10) 辅导员 scope ----------
    $cOv = (Get-Json "$base/api/stats/overview?$q" $coun).data
    $sqlScope = Sql1 "select count(*) from risk_state rs join student s on s.id=rs.student_id where rs.term_id=$TermId and rs.is_void=0 and s.admin_class_id in (select admin_class_id from counselor_class where counselor_id=1 and term_id=$TermId)"
    Check '辅导员范围裁剪' ($cOv.current.total -eq [int]$sqlScope[0]) ("C001={0}；SQL={1}" -f $cOv.current.total, $sqlScope[0])

    # ---------- 11) 硬化校验：term_id / 日快照 / scope / stale / 全类型导出 / 性能 / 索引 ----------
    $nullTerm = [int](Sql1 "select count(*) from stat_daily_class where term_id is null")[0]
    Check 'stat.term_id 已回填' ($nullTerm -eq 0) ("null term rows=" + $nullTerm)
    $dailyDays = [int](Sql1 "select count(distinct stat_date) from stat_daily_class where term_id=$TermId")[0]
    Check '日快照为逐日累积' ($dailyDays -ge 30) ("distinct dates=" + $dailyDays)
    $scopes = [int](Sql1 "select count(distinct scope_type) from stat_weekly_kpi where term_id=$TermId")[0]
    Check '周 KPI 分 scope' ($scopes -ge 3) ("scope types=" + $scopes)
    $sourceKinds = [int](Sql1 "select count(distinct source) from stat_daily_class where term_id=$TermId")[0]
    Check '回填来源可区分' ($sourceKinds -ge 2) ("source kinds=" + $sourceKinds)
    $staleFields = ($ov.PSObject.Properties.Name -contains 'refreshed') -and ($ov.PSObject.Properties.Name -contains 'staleBefore')
    Check 'stale 语义字段' $staleFields ("refreshed=" + $ov.refreshed + " staleBefore=" + $ov.staleBefore)
    $medianOk = $null -ne $ov.current.medianHandleHours
    Check '辅助口径 中位处置时长' $medianOk ("median=" + $ov.current.medianHandleHours)
    $overdueOk = ($eff.current.PSObject.Properties.Name -contains 'overdue') -and ($eff.current.PSObject.Properties.Name -contains 'notReviewed')
    Check '辅助口径 逾期/未复评' $overdueOk ("overdue=" + $eff.current.overdue + " notReviewed=" + $eff.current.notReviewed)

    # 全类型导出：CSV 头 + XLSX 内容
    $exportOk = $true
    $exportDetail = @()
    foreach ($t in @('overview','distribution','compare','funnel','effect')) {
        $pth = Join-Path $tmpDir ("verify_export_" + $t + ".csv")
        Invoke-WebRequest -Uri "$base/api/stats/export?type=$t&$q" -Headers $h -OutFile $pth -UseBasicParsing
        $txt = [IO.File]::ReadAllText($pth, [Text.Encoding]::UTF8)
        $expect = switch ($t) {
            'overview' { '风险总人数' }
            'distribution' { '等级,数量' }
            'compare' { '教学班' }
            'funnel' { '阶段,数量' }
            'effect' { '组别' }
        }
        $ok = $txt.Contains($expect)
        if (-not $ok) { $exportOk = $false; $exportDetail += ($t + ' 缺 ' + $expect) }
    }
    Check 'export 全类型 CSV' $exportOk (($exportDetail -join '；') + $(if ($exportOk) { '5 类均含表头' } else { '' }))

    function Get-XlsxText([string]$path) {
        Add-Type -AssemblyName System.IO.Compression.FileSystem
        $zip = [IO.Compression.ZipFile]::OpenRead($path)
        try {
            $entry = $zip.Entries | Where-Object { $_.FullName -eq 'xl/sharedStrings.xml' }
            if (-not $entry) { return '' }
            $reader = New-Object IO.StreamReader($entry.Open())
            return $reader.ReadToEnd()
        } finally { $zip.Dispose() }
    }
    $fx = Join-Path $tmpDir 'verify_export_effect.xlsx'
    Invoke-WebRequest -Uri "$base/api/stats/export?type=effect&format=xlsx&$q" -Headers $h -OutFile $fx -UseBasicParsing
    $xtext = Get-XlsxText $fx
    $xlsxOk = $xtext.Contains('TREAT') -and $xtext.Contains('CONTROL')
    Check 'export XLSX 内容' $xlsxOk ("bytes=" + (Get-Item $fx).Length + " contains TREAT/CONTROL=" + $xlsxOk)

    # 性能：warm 请求单接口 <2s
    $perfOk = $true
    $perfDetail = @()
    foreach ($ep in @('overview','trend','distribution','compare','funnel','effect')) {
        $sw2 = [Diagnostics.Stopwatch]::StartNew()
        Get-Json "$base/api/stats/$ep`?$q" $admin | Out-Null
        $sw2.Stop()
        $perfDetail += ($ep + '=' + [math]::Round($sw2.Elapsed.TotalMilliseconds) + 'ms')
        if ($sw2.Elapsed.TotalMilliseconds -gt 2000) { $perfOk = $false }
    }
    Check '接口时延 <2s' $perfOk ($perfDetail -join ' ')

    $idx = [int](Sql1 "select count(*) from information_schema.statistics where table_schema=database() and table_name='alert_record' and index_name in ('idx_term_create','idx_term_class_create')")[0]
    Check '性能索引存在' ($idx -ge 2) ("alert_record indexes=" + $idx)
} catch {
    Check '脚本执行' $false $_.Exception.Message
} finally {
    if ($null -ne $java -and -not $KeepBackend) {
        Stop-Process -Id $java.Id -Force -ErrorAction SilentlyContinue
        Wait-Process -Id $java.Id -Timeout 10 -ErrorAction SilentlyContinue
    }
}

$sw.Stop()
$okCount = @($results | Where-Object { $_.ok }).Count
$allOk = ($okCount -eq $results.Count) -and ($results.Count -gt 0)
$lines = @(
    '# v3.0 P1-8 统计对账报告',
    '',
    ('- 时间：' + (Get-Date -Format 'yyyy-MM-ddTHH:mm:sszzz')),
    ('- 数据库：`' + $Db + '`；termId=' + $TermId),
    ('- 结果：**' + $(if ($allOk) { 'PASS' } else { 'FAIL' }) + '**（' + $okCount + '/' + $results.Count + '）'),
    ('- 耗时：' + [math]::Round($sw.Elapsed.TotalSeconds, 1) + 's'),
    '',
    '| # | 检查 | 结果 | 证据 |',
    '|---|---|---|---|'
)
$i = 0
foreach ($r in $results) {
    $i++
    $lines += ('| ' + $i + ' | ' + $r.name + ' | ' + $(if ($r.ok) { 'PASS' } else { 'FAIL' }) + ' | ' + ($r.detail -replace '\|', '/') + ' |')
}
$lines += ''
$lines += '> 复现：`powershell -NoProfile -ExecutionPolicy Bypass -File v3.0/tool/verify_stats.ps1`'
[IO.File]::WriteAllText($Out, ($lines -join "`n"), (New-Object System.Text.UTF8Encoding($false)))
Write-Host ""
Write-Host ("== 统计对账：" + $(if ($allOk) { 'PASS' } else { 'FAIL' }) + "（$okCount/$($results.Count)），报告 -> $Out") -ForegroundColor $(if ($allOk) { 'Green' } else { 'Red' })
if (-not $allOk) { exit 1 }
