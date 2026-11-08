<#
.SYNOPSIS
  P2-4/5/6 对账：四角色/自适应、Critic、EvidenceLedger。

.DESCRIPTION
  起 v3 后端 → 登录 → 校验：
  - mode=4 固定 4 角色；mode=2 固定 2；auto 在 6 题上比 4 角色调用少 ≥25% 且质量不降；
  - 单轮工具步数 ≤4；
  - 台账落库/命中/一次重写/降级；
  - Critic 10 例信息不足 ≥8 例要求取证 + 空转检测。
  输出 evidence/P2-2.md；失败非零退出。
#>
param(
    [string]$Db = 'study_warning_system_v3',
    [int]$Port = 18087,
    [string]$Out = '',
    [switch]$KeepBackend
)
$ErrorActionPreference = 'Stop'
. (Join-Path $PSScriptRoot 'lib\db_common.ps1')
$v3Dir = Split-Path -Parent $PSScriptRoot
$root = Split-Path -Parent $v3Dir
$backend = Join-Path $root 'v2.0\backend'
$tmpDir = Join-Path $root '.tmp-doc'
if (-not $Out) { $Out = Join-Path $v3Dir 'evidence\P2-2.md' }
if (-not (Test-Path -LiteralPath $tmpDir)) { New-Item -ItemType Directory -Path $tmpDir -Force | Out-Null }
$results = @()
function Check([string]$name, [bool]$ok, [string]$detail) {
    $script:results += [pscustomobject]@{ name = $name; ok = $ok; detail = $detail }
    Write-Host ("  [" + $(if ($ok) { 'PASS' } else { 'FAIL' }) + "] " + $name + "：" + $detail) -ForegroundColor $(if ($ok) { 'Green' } else { 'Red' })
}
function Get-Json($url, $token) {
    $h = @{}
    if ($token) { $h['Authorization'] = "Bearer $token" }
    $r = Invoke-WebRequest -Uri $url -Headers $h -UseBasicParsing
    return ([Text.Encoding]::UTF8.GetString($r.RawContentStream.ToArray()) | ConvertFrom-Json)
}
function Post-Json($url, $obj, $token) {
    $bytes = [Text.Encoding]::UTF8.GetBytes(($obj | ConvertTo-Json -Depth 12 -Compress))
    $h = @{}
    if ($token) { $h['Authorization'] = "Bearer $token" }
    $r = Invoke-WebRequest -Uri $url -Method Post -Body $bytes -ContentType 'application/json; charset=utf-8' -Headers $h -UseBasicParsing
    return ([Text.Encoding]::UTF8.GetString($r.RawContentStream.ToArray()) | ConvertFrom-Json)
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
    ) -WorkingDirectory $backend -RedirectStandardOutput (Join-Path $tmpDir 'verify-p2-4-6.log') `
        -RedirectStandardError (Join-Path $tmpDir 'verify-p2-4-6.log.err') -PassThru -WindowStyle Hidden
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
    if (-not $ready) { throw '后端未就绪' }
    $admin = Login $base 'admin001'

    $questions = @(
        '计算机1班当前红黄灯风险统计',
        '最近八周风险趋势',
        '高等数学和数据结构哪个班风险更高',
        '学生 1001 的风险情况',
        '学生 1001 的干预记录和成效',
        '高等数学 课程中极限的定义是什么'
    )

    # ---------- P2-4 固定 4 / 2 / auto ----------
    $fixedRuns = @()
    foreach ($q in $questions) {
        $fixedRuns += (Post-Json "$base/api/agent/run" @{ question = $q; mode = '4' } $admin).data
    }
    $fixedRoles = ($fixedRuns | Measure-Object -Property roleCalls -Sum).Sum
    $fixedOk = @($fixedRuns | Where-Object { $_.roleCalls -eq 4 -and -not $_.blocked -and $_.ledger.hitRate -eq 1 -and $_.quality.requiredCoverage -eq 1 }).Count
    Check 'P2-4 固定 4 角色' ($fixedOk -eq 6 -and $fixedRoles -eq 24) ("runs=$fixedOk/6 roleCalls=$fixedRoles")
    $maxStepOk = @($fixedRuns | Where-Object { $_.usedSteps -le 4 -and @($_.steps).Count -le 4 }).Count -eq 6
    Check 'P2-4 单轮 ≤4 步' $maxStepOk ("steps=" + (($fixedRuns | ForEach-Object { $_.usedSteps }) -join ','))

    $fixed2 = @()
    foreach ($q in $questions) { $fixed2 += (Post-Json "$base/api/agent/run" @{ question = $q; mode = '2' } $admin).data }
    $fixed2Ok = @($fixed2 | Where-Object { $_.roleCalls -eq 2 -and $_.ledger.hitRate -eq 1 }).Count
    Check 'P2-4 固定 2 角色' ($fixed2Ok -eq 6) ("runs=$fixed2Ok/6")

    $autoRuns = @()
    foreach ($q in $questions) { $autoRuns += (Post-Json "$base/api/agent/run" @{ question = $q; mode = 'auto' } $admin).data }
    $autoRoles = ($autoRuns | Measure-Object -Property roleCalls -Sum).Sum
    $autoChecks = @($autoRuns | Where-Object {
        -not $_.blocked -and $_.ledger.hitRate -eq 1 -and $_.quality.requiredCoverage -eq 1
    }).Count
    $reduction = 1 - ($autoRoles / $fixedRoles)
    Check 'P2-4 auto 调用少 ≥25%' (($autoRoles -lt $fixedRoles * 0.75) -and $reduction -ge 0.25) ("auto=$autoRoles fixed=$fixedRoles reduction=" + [math]::Round($reduction * 100, 1) + "%")
    Check 'P2-4 auto 质量不降' ($autoChecks -eq 6) ("qualityOk=$autoChecks/6（ledgerHit=1, coverage=1）")

    # ---------- P2-6 台账 ----------
    $sample = $fixedRuns[0]
    $entries = (Get-Json "$base/api/agent/ledger?runKey=$($sample.ledger.runKey)" $admin).data
    Check 'P2-6 台账落库' (@($entries).Count -gt 0) ("entries=" + @($entries).Count)
    $valid = (Post-Json "$base/api/agent/ledger/validate" @{ runKey = $sample.ledger.runKey; answer = $sample.answerDraft } $admin).data
    Check 'P2-6 正常答案命中' ($valid.misses.Count -eq 0) ("numbers=" + $valid.numbers.Count + " hitRate=" + $valid.hitRate)
    $fake = (Post-Json "$base/api/agent/ledger/validate" @{ runKey = $sample.ledger.runKey; answer = '红=999999' } $admin).data
    Check 'P2-6 未命中可检出' ($fake.misses.Count -gt 0 -and $fake.hitRate -lt 1) ("misses=" + ($fake.misses -join ','))
    $repair = (Post-Json "$base/api/agent/ledger/repair" @{ runKey = $sample.ledger.runKey; answer = '红=999999' } $admin).data
    Check 'P2-6 一次重写' ($repair.rewritten -and $repair.second.misses.Count -eq 0 -and -not $repair.degraded) ("rewritten=" + $repair.rewritten + " secondMisses=" + $repair.second.misses.Count)
    $degrade = (Post-Json "$base/api/agent/ledger/repair" @{ runKey = 'run-not-exist'; answer = '12345' } $admin).data
    Check 'P2-6 空台账降级' ($degrade.degraded -and -not $degrade.rewritten) ("degraded=" + $degrade.degraded)

    # ---------- P2-5 Critic ----------
    $criticCases = @(
        @{ name = '空步骤'; intent = 'CLASS_STATS'; steps = @() },
        @{ name = '班级缺红黄'; intent = 'CLASS_STATS'; steps = @(@{ tool = 'getClassRiskStats'; args = @{}; evidence = @{ total = 1927 } }) },
        @{ name = '趋势缺points'; intent = 'TREND'; steps = @(@{ tool = 'getRiskTrend'; args = @{}; evidence = @{ lastWeek = '2026-11-02' } }) },
        @{ name = '对比缺rows'; intent = 'COURSE_COMPARE'; steps = @(@{ tool = 'getCourseCompare'; args = @{}; evidence = @{ termId = 5 } }) },
        @{ name = '学生缺等级'; intent = 'STUDENT_RISK'; steps = @(@{ tool = 'getStudentRisk'; args = @{}; evidence = @{ studentId = 1001 } }) },
        @{ name = '干预缺interventions'; intent = 'INTERVENTION'; steps = @(@{ tool = 'getInterventions'; args = @{}; evidence = @{ studentId = 1001 } }) },
        @{ name = 'RAG无命中'; intent = 'COURSE_QA'; steps = @(@{ tool = 'retrieveCourseDocs'; args = @{}; evidence = @{ hits = 0; refused = $true } }) },
        @{ name = '证据为空'; intent = 'TREND'; steps = @(@{ tool = 'getRiskTrend'; args = @{}; evidence = @{} }) },
        @{ name = '空转检测'; intent = 'CLASS_STATS'; steps = @(
            @{ tool = 'getClassRiskStats'; args = @{ termId = 5 }; evidence = @{ total = 1; red = 0; yellow = 0; riskRatio = 0 } },
            @{ tool = 'getClassRiskStats'; args = @{ termId = 5 }; evidence = @{ total = 1; red = 0; yellow = 0; riskRatio = 0 } }
        ) },
        @{ name = '缺对比行且空转'; intent = 'COURSE_COMPARE'; steps = @(
            @{ tool = 'getCourseCompare'; args = @{ topN = 10 }; evidence = @{} },
            @{ tool = 'getCourseCompare'; args = @{ topN = 10 }; evidence = @{} }
        ) }
    )
    $asks = 0
    $criticDetail = @()
    foreach ($c in $criticCases) {
        $r = (Post-Json "$base/api/agent/critic" @{ intent = $c.intent; steps = $c.steps } $admin).data
        if ($r.asksEvidence) { $asks++ }
        $criticDetail += ($c.name + '=' + $r.asksEvidence)
    }
    Check 'P2-5 10 例信息不足 ≥8 取证' ($asks -ge 8) ("asks=$asks/10：" + ($criticDetail -join '; '))
    $idle = (Post-Json "$base/api/agent/critic" @{ intent = 'CLASS_STATS'; steps = $criticCases[8].steps } $admin).data
    Check 'P2-5 空转检测' ($idle.idleLoop -and $idle.asksEvidence) ("idle=" + $idle.idleLoop)
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
$all = ($okCount -eq $results.Count) -and ($results.Count -gt 0)
$lines = @(
    '# v3.0 P2-4/5/6 证据（四角色 / Critic / EvidenceLedger）',
    '',
    ('- 时间：' + (Get-Date -Format 'yyyy-MM-ddTHH:mm:sszzz')),
    ('- 数据库：`' + $Db + '`；耗时：' + [math]::Round($sw.Elapsed.TotalSeconds, 1) + 's'),
    ('- 结果：**' + $(if ($all) { 'PASS' } else { 'FAIL' }) + '**（' + $okCount + '/' + $results.Count + '）'),
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
$lines += '> 复现：`powershell -NoProfile -ExecutionPolicy Bypass -File v3.0/tool/verify_p2_4_6.ps1`'
[IO.File]::WriteAllText($Out, ($lines -join "`n"), (New-Object System.Text.UTF8Encoding($false)))
Write-Host ""
Write-Host ("== P2-4/5/6：" + $(if ($all) { 'PASS' } else { 'FAIL' }) + "（$okCount/$($results.Count)），报告 -> $Out") -ForegroundColor $(if ($all) { 'Green' } else { 'Red' })
if (-not $all) { exit 1 }
