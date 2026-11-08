<#
.SYNOPSIS
  P2-7 ~ P2-10 对账：Diagnostician / Reporter / Grader / Tutor。

.DESCRIPTION
  起 v3 后端 → 登录 → 校验：
  - Diagnostician：30 题工具选择 ≥90%，P95 <8s，输出契约字段；
  - Reporter：三类报告结构完整、checksPassed==checksTotal、≤15s；
  - Grader：逐题评分/要点对应；全量 MAE≤2；人工改分替换旧分且可恢复；
  - Tutor：引用 chunkId 可定位；核心集拒答正确、误拒 0。
  输出 evidence/P2-3.md；失败非零退出。
#>
param(
    [string]$Db = 'study_warning_system_v3',
    [int]$Port = 18090,
    [string]$Out = '',
    [switch]$KeepBackend
)
$ErrorActionPreference = 'Stop'
. (Join-Path $PSScriptRoot 'lib\db_common.ps1')
$v3Dir = Split-Path -Parent $PSScriptRoot
$root = Split-Path -Parent $v3Dir
$backend = Join-Path $root 'v2.0\backend'
$tmpDir = Join-Path $root '.tmp-doc'
if (-not $Out) { $Out = Join-Path $v3Dir 'evidence\P2-3.md' }
if (-not (Test-Path -LiteralPath $tmpDir)) { New-Item -ItemType Directory -Path $tmpDir -Force | Out-Null }
$ctx = New-DbContext -Db $Db
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
function Sql1([string]$sql) { return @(Invoke-DbQuery -Ctx $ctx -Sql $sql -Database $Db | Select-Object -First 1) }

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
    ) -WorkingDirectory $backend -RedirectStandardOutput (Join-Path $tmpDir 'verify-p2-agents.log') `
        -RedirectStandardError (Join-Path $tmpDir 'verify-p2-agents.log.err') -PassThru -WindowStyle Hidden
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
    $teacher = Login $base '1001'
    $counselor = Login $base 'C001'
    $student = Login $base '20250001'

    # ---------- P2-7 Diagnostician 30 题 ----------
    $cases = Get-Content -LiteralPath (Join-Path $v3Dir 'eval\agent_cases_30.json') -Raw -Encoding UTF8 | ConvertFrom-Json
    $okSelect = 0
    $lat = @()
    $detail = @()
    foreach ($c in $cases) {
        $sw2 = [Diagnostics.Stopwatch]::StartNew()
        $r = (Post-Json "$base/api/agent/diagnose" @{ question = $c.question } $admin).data
        $sw2.Stop()
        $lat += $sw2.Elapsed.TotalMilliseconds
        $tools = @($r.tools) -join ','
        $match = ($r.intent -eq $c.intent) -and ($tools -eq (@($c.tools) -join ','))
        if ($match) { $okSelect++ }
        $detail += ($c.id.ToString() + ':' + $(if ($match) { 'ok' } else { 'FAIL(' + $r.intent + '/' + $tools + ')' }))
    }
    $acc = $okSelect / $cases.Count
    $sorted = $lat | Sort-Object
    $p95 = $sorted[[Math]::Min($sorted.Count - 1, [Math]::Ceiling(0.95 * $sorted.Count) - 1)]
    Check 'P2-7 30 题工具选择 ≥90%' ($acc -ge 0.9) ("accuracy=" + [math]::Round($acc * 100, 1) + "% (" + $okSelect + "/" + $cases.Count + ")")
    Check 'P2-7 P95 <8s' ($p95 -lt 8000) ("p95=" + [math]::Round($p95) + "ms")
    $contractOk = @($cases | Where-Object { $true }).Count -gt 0
    $first = (Post-Json "$base/api/agent/diagnose" @{ question = '计算机1班当前红黄灯风险统计' } $admin).data
    $contractOk = ($first.agent -eq 'diagnostician') -and ($null -ne $first.answer) -and ($null -ne $first.tools) -and ($null -ne $first.ledger)
    Check 'P2-7 输出契约字段' $contractOk ("agent=" + $first.agent + " tools=" + (@($first.tools) -join ','))

    # ---------- P2-8 Reporter ----------
    $reports = @(
        @{ name = 'class/201'; body = @{ type = 'class'; targetId = 201 }; token = $teacher },
        @{ name = 'offering/101'; body = @{ type = 'offering'; targetId = 101 }; token = $teacher },
        @{ name = 'adminClass/1'; body = @{ type = 'adminClass'; targetId = 1 }; token = $counselor }
    )
    $repOk = $true
    $repDetail = @()
    foreach ($rp in $reports) {
        $sw3 = [Diagnostics.Stopwatch]::StartNew()
        $d = (Post-Json "$base/api/agent/report" $rp.body $rp.token).data
        $sw3.Stop()
        $pass = ($d.checksPassed -eq $d.checksTotal) -and (@($d.sections).Count -gt 0) -and ($sw3.Elapsed.TotalSeconds -le 15)
        if (-not $pass) { $repOk = $false }
        $repDetail += ($rp.name + ' checks=' + $d.checksPassed + '/' + $d.checksTotal + ' sections=' + @($d.sections).Count +
            ' ' + [math]::Round($sw3.Elapsed.TotalSeconds, 2) + 's')
    }
    Check 'P2-8 三类报告/对账/≤15s' $repOk ($repDetail -join '；')

    # ---------- P2-9 Grader ----------
    $g = (Post-Json "$base/api/agent/grade" @{ submissionId = 900001 } $admin).data
    $structOk = ($g.agent -eq 'grader') -and (@($g.answers).Count -eq 5) -and ($null -ne $g.answers[0].matchedPoints) -and ($null -ne $g.answers[0].missingPoints)
    Check 'P2-9 逐题契约' $structOk ("answers=" + @($g.answers).Count)
    $eval = (Post-Json "$base/api/agent/grade/eval" @{} $admin).data
    Check 'P2-9 MAE ≤2' ([double]$eval.mae -le 2) ("samples=" + $eval.samples + " mae=" + $eval.mae + " max=" + $eval.maxAbsError + " within2=" + $eval.within2Rate + "%")
    $a0 = $g.answers[0]
    $old = [double]$a0.humanScore
    $new = if ($old -ge 10) { 9 } else { $old + 1 }
    $apply = (Post-Json "$base/api/agent/grade/apply" @{ answerId = $a0.answerId; score = $new; comment = 'P2-9 人工改分验证' } $admin).data
    $restore = (Post-Json "$base/api/agent/grade/apply" @{ answerId = $a0.answerId; score = $old; comment = 'P2-9 验证后恢复' } $admin).data
    $dbScore = [double](Sql1 "select score from submission_answer where id=$($a0.answerId)")
    $editOk = ([double]$apply.newScore -eq $new) -and ([double]$apply.oldScore -eq $old) -and ([math]::Abs($dbScore - $old) -lt 0.001) -and ($apply.comment -like '*人工改分*')
    Check 'P2-9 人工改分留痕可恢复' $editOk ("old=" + $old + " -> " + $new + " -> restored=" + $dbScore)

    # ---------- P2-10 Tutor ----------
    $tutorCases = @(
        @{ courseId = 4; q = '极限的定义是什么'; expectRefuse = $false },
        @{ courseId = 7; q = '栈和队列的区别'; expectRefuse = $false },
        @{ courseId = 9; q = '事务的 ACID 是什么'; expectRefuse = $false },
        @{ courseId = 6; q = '指针和内存的关系'; expectRefuse = $false },
        @{ courseId = 4; q = '量子纠缠与超导'; expectRefuse = $true },
        @{ courseId = 999; q = '任意问题'; expectRefuse = $true }
    )
    $tutorOk = 0
    $refuseOk = 0
    $falseRefuse = 0
    $citedIds = @()
    $tutorDetail = @()
    foreach ($tc in $tutorCases) {
        $d = (Post-Json "$base/api/agent/tutor" @{ courseId = $tc.courseId; question = $tc.q } $student).data
        if ($tc.expectRefuse) {
            if ($d.refused) { $refuseOk++ }
        } else {
            if (-not $d.refused -and @($d.citations).Count -gt 0) { $tutorOk++ }
            elseif ($d.refused) { $falseRefuse++ }
            foreach ($c in @($d.citations)) { $citedIds += [long]$c.chunkId }
        }
        $tutorDetail += ("$($tc.courseId)/" + $(if ($d.refused) { 'refused' } else { 'cites=' + @($d.citations).Count }))
    }
    $answerable = @($tutorCases | Where-Object { -not $_.expectRefuse }).Count
    $refusable = @($tutorCases | Where-Object { $_.expectRefuse }).Count
    $chunkOk = $true
    if ($citedIds.Count -gt 0) {
        $in = ($citedIds | Select-Object -Unique) -join ','
        $found = [int](Sql1 "select count(*) from ai_kb_chunk where id in ($in)")
        $chunkOk = ($found -eq @($citedIds | Select-Object -Unique).Count)
    }
    Check 'P2-10 引用带 chunkId 可定位' ($chunkOk -and $citedIds.Count -gt 0) ("citations=" + $citedIds.Count + " chunks_ok=" + $chunkOk)
    Check 'P2-10 核心集拒答/误拒' (($refuseOk -eq $refusable) -and ($falseRefuse -eq 0) -and ($tutorOk -eq $answerable)) ("answers=" + $tutorOk + "/" + $answerable + " refuse=" + $refuseOk + "/" + $refusable + " falseRefuse=" + $falseRefuse)
    Check 'P2-10 核心集明细' $true ($tutorDetail -join '；')
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
    '# v3.0 P2-7~P2-10 证据（四个业务智能体）',
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
$lines += '> 复现：`powershell -NoProfile -ExecutionPolicy Bypass -File v3.0/tool/verify_p2_7_10.ps1`'
[IO.File]::WriteAllText($Out, ($lines -join "`n"), (New-Object System.Text.UTF8Encoding($false)))
Write-Host ""
Write-Host ("== P2-7~10：" + $(if ($all) { 'PASS' } else { 'FAIL' }) + "（$okCount/$($results.Count)），报告 -> $Out") -ForegroundColor $(if ($all) { 'Green' } else { 'Red' })
if (-not $all) { exit 1 }
