<#
.SYNOPSIS
  P2-1/2/3 对账：ToolSpec、ToolRegistry、受限 JSON 执行器、IntentRouter、Orchestrator。

.DESCRIPTION
  起 v3 后端 → 登录 → 校验：
  - 9 工具 + OpenAPI 9 paths + schema 字段；
  - 6 类问题路由与工具链；
  - 非法 JSON/未知工具/非法参数/超预算 100% 拦截；
  - 同题 3 次工具链与摘要一致；
  - 辅导员范围仍生效。
  输出 evidence/P2-1.md；失败非零退出。
#>
param(
    [string]$Db = 'study_warning_system_v3',
    [int]$Port = 18086,
    [string]$Out = '',
    [switch]$KeepBackend
)
$ErrorActionPreference = 'Stop'
. (Join-Path $PSScriptRoot 'lib\db_common.ps1')
$v3Dir = Split-Path -Parent $PSScriptRoot
$root = Split-Path -Parent $v3Dir
$backend = Join-Path $root 'v2.0\backend'
$tmpDir = Join-Path $root '.tmp-doc'
if (-not $Out) { $Out = Join-Path $v3Dir 'evidence\P2-1.md' }
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
    $bytes = [Text.Encoding]::UTF8.GetBytes(($obj | ConvertTo-Json -Depth 10 -Compress))
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
    ) -WorkingDirectory $backend -RedirectStandardOutput (Join-Path $tmpDir 'verify-p2-backend.log') `
        -RedirectStandardError (Join-Path $tmpDir 'verify-p2-backend.log.err') -PassThru -WindowStyle Hidden
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
    $coun = Login $base 'C001'

    # ---------- 1) ToolSpec / OpenAPI ----------
    $tools = (Get-Json "$base/api/agent/tools" $admin).data
    $names = @($tools | ForEach-Object { $_.name })
    Check '工具数量=9' ($tools.Count -eq 9) ("n=" + $tools.Count)
    $expected = @('getClassRiskStats','getRiskTrend','getCourseCompare','getStudentRisk','getInterventions','retrieveCourseDocs','getAlertExplain','getStudentMemory','draftIntervention')
    $missing = @($expected | Where-Object { $names -notcontains $_ })
    Check '工具名齐全' ($missing.Count -eq 0) ("missing=" + ($missing -join ','))
    $schemaOk = @($tools | Where-Object { -not $_.evidence_fields -or $_.timeout_ms -le 0 -or -not $_.description }).Count -eq 0
    Check 'schema 字段完整' $schemaOk 'name/description/timeout/evidence_fields 均存在'
    $oa = (Get-Json "$base/api/agent/tools?format=openapi" $admin).data
    $paths = @($oa.paths.PSObject.Properties)
    Check 'OpenAPI 可生成' ($oa.openapi -eq '3.0.3' -and $paths.Count -eq 9) ("openapi=" + $oa.openapi + " paths=" + $paths.Count)

    # ---------- 2) 6 类路由 ----------
    $cases = @(
        @{ q = '计算机1班当前红黄灯风险统计'; intent = 'CLASS_STATS'; tools = @('getClassRiskStats') },
        @{ q = '最近八周风险趋势'; intent = 'TREND'; tools = @('getRiskTrend') },
        @{ q = '高等数学和数据结构哪个班风险更高'; intent = 'COURSE_COMPARE'; tools = @('getCourseCompare') },
        @{ q = '学生 1001 的风险情况'; intent = 'STUDENT_RISK'; tools = @('getStudentRisk') },
        @{ q = '学生 1001 的干预记录和成效'; intent = 'INTERVENTION'; tools = @('getInterventions','getClassRiskStats') },
        @{ q = '高等数学 课程中极限的定义是什么'; intent = 'COURSE_QA'; tools = @('retrieveCourseDocs') }
    )
    $routeOk = $true
    $routeDetail = @()
    foreach ($c in $cases) {
        $run = (Post-Json "$base/api/agent/run" @{ question = $c.q } $admin).data
        $toolNames = @($run.steps | ForEach-Object { $_.tool })
        $match = ($run.intent -eq $c.intent) -and (-not $run.blocked) -and (($toolNames -join ',') -eq ($c.tools -join ','))
        if (-not $match) { $routeOk = $false }
        $routeDetail += ($c.intent + '→' + $run.intent + '[' + ($toolNames -join ',') + ']' + $(if ($run.blocked) { ' blocked' } else { '' }))
    }
    Check '6 类问题路由+工具链' $routeOk ($routeDetail -join '；')

    # ---------- 3) 同题 3 次一致性 ----------
    $q = '学生 1001 的干预记录和成效'
    $runs = 1..3 | ForEach-Object { (Post-Json "$base/api/agent/run" @{ question = $q } $admin).data }
    $chains = $runs | ForEach-Object { (@($_.steps | ForEach-Object { $_.tool + ':' + ($_.digest) }) -join '|') }
    $same = ($chains | Select-Object -Unique).Count -eq 1
    Check '同题 3 次工具链一致' $same ("chain=" + $chains[0])

    # ---------- 4) ToolRegistry 非法输入 ----------
    $unknown = (Post-Json "$base/api/agent/tool-call" @{ tool = 'rm_rf'; args = @{} } $admin).data
    Check '未知工具拦截' ((-not $unknown.ok) -and $unknown.errorCode -eq 'UNKNOWN_TOOL') ("error=" + $unknown.errorCode)
    $badType = (Post-Json "$base/api/agent/tool-call" @{ tool = 'getStudentRisk'; args = @{ studentId = 'abc' } } $admin).data
    Check '参数类型拦截' ((-not $badType.ok) -and $badType.errorCode -eq 'INVALID_ARGS') ("error=" + $badType.errorCode)
    $missingArg = (Post-Json "$base/api/agent/tool-call" @{ tool = 'getStudentRisk'; args = @{} } $admin).data
    Check '必填参数拦截' ((-not $missingArg.ok) -and $missingArg.errorCode -eq 'INVALID_ARGS') ("error=" + $missingArg.errorCode)
    $extra = (Post-Json "$base/api/agent/tool-call" @{ tool = 'getRiskTrend'; args = @{ foo = 1 } } $admin).data
    Check '未知参数拦截' ((-not $extra.ok) -and $extra.errorCode -eq 'INVALID_ARGS') ("error=" + $extra.errorCode)
    $valid = (Post-Json "$base/api/agent/tool-call" @{ tool = 'getClassRiskStats'; args = @{ termId = 5 } } $admin).data
    Check '合法工具可调用' ($valid.ok -and $valid.digest) ("digest=" + $valid.digest + " bytes=" + $valid.bytes)

    $rag = (Post-Json "$base/api/agent/tool-call" @{ tool = 'retrieveCourseDocs'; args = @{ courseId = 4; query = '极限'; topK = 3 } } $admin).data
    Check '工具 retrieveCourseDocs' ($rag.ok -and $rag.data.hits.Count -ge 1) ("hits=" + $rag.data.hits.Count + " refused=" + $rag.data.refused)
    $alert = (Post-Json "$base/api/agent/tool-call" @{ tool = 'getAlertExplain'; args = @{ alertId = 1 } } $admin).data
    Check '工具 getAlertExplain' ($alert.ok -and $null -ne $alert.data.found) ("found=" + $alert.data.found)
    $mem = (Post-Json "$base/api/agent/tool-call" @{ tool = 'getStudentMemory'; args = @{ studentId = 1001 } } $admin).data
    Check '工具 getStudentMemory' ($mem.ok -and $null -ne $mem.data.items) ("items=" + $mem.data.items.Count)
    $draft = (Post-Json "$base/api/agent/tool-call" @{ tool = 'draftIntervention'; args = @{ studentId = 1001; reason = '连续缺勤需约谈'; actionType = 'TALK' } } $admin).data
    Check '工具 draftIntervention' ($draft.ok -and $draft.data.draft.requiresHumanApproval) ("status=" + $draft.data.draft.status)

    # ---------- 5) 四道检查（execute-json） ----------
    $parseBad = (Post-Json "$base/api/agent/execute-json" @{ json = '{bad'; maxSteps = 1 } $admin).data
    $parseCheck = @($parseBad.checks | Where-Object { $_.name -eq 'parse' -and -not $_.ok }).Count
    Check '检查1 可解析拦截' ((-not $parseBad.ok) -and $parseBad.result.errorCode -eq 'PARSE_ERROR' -and $parseCheck -eq 1) ("error=" + $parseBad.result.errorCode)
    $unknown2 = (Post-Json "$base/api/agent/execute-json" @{ json = '{"tool":"rm_rf","args":{}}'; maxSteps = 1 } $admin).data
    $whiteCheck = @($unknown2.checks | Where-Object { $_.name -eq 'whitelist' -and -not $_.ok }).Count
    Check '检查2 白名单拦截' ((-not $unknown2.ok) -and $unknown2.result.errorCode -eq 'UNKNOWN_TOOL' -and $whiteCheck -eq 1) ("error=" + $unknown2.result.errorCode)
    $schemaBad = (Post-Json "$base/api/agent/execute-json" @{ json = '{"tool":"getStudentRisk","args":{"studentId":"x"}}'; maxSteps = 1 } $admin).data
    $schemaCheck = @($schemaBad.checks | Where-Object { $_.name -eq 'schema' -and -not $_.ok }).Count
    Check '检查3 强校验拦截' ((-not $schemaBad.ok) -and $schemaBad.result.errorCode -eq 'INVALID_ARGS' -and $schemaCheck -eq 1) ("error=" + $schemaBad.result.errorCode)
    $budget = (Post-Json "$base/api/agent/execute-json" @{ json = '{"tool":"getClassRiskStats","args":{}}'; maxSteps = 0; usedSteps = 0 } $admin).data
    $budgetCheck = @($budget.checks | Where-Object { $_.name -eq 'budget' -and -not $_.ok }).Count
    Check '检查4 步数预算拦截' ((-not $budget.ok) -and $budget.result.errorCode -eq 'STEP_BUDGET_EXCEEDED' -and $budgetCheck -eq 1) ("error=" + $budget.result.errorCode)
    $okJson = (Post-Json "$base/api/agent/execute-json" @{ json = '{"tool":"getClassRiskStats","args":{"termId":5}}'; maxSteps = 1 } $admin).data
    $allChecks = (@($okJson.checks | Where-Object { $_.ok }).Count -eq 4)
    Check '四道检查全通过' ($okJson.ok -and $allChecks) ("used=" + $okJson.usedStepsAfter)

    # ---------- 6) 辅导员范围 ----------
    $crun = (Post-Json "$base/api/agent/run" @{ question = '班级风险统计' } $coun).data
    Check '辅导员可运行' ((-not $crun.blocked) -and $crun.steps.Count -ge 1) ("intent=" + $crun.intent + " steps=" + $crun.steps.Count)
    $outOfScope = 1301
    $scopeRun = (Post-Json "$base/api/agent/run" @{ question = ('学生 ' + $outOfScope + ' 的风险情况') } $coun).data
    Check '辅导员越权学生被阻断' ($scopeRun.blocked -and ($scopeRun.blockedReason -like 'FORBIDDEN*')) ("blocked=" + $scopeRun.blockedReason)
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
    '# v3.0 P2-1/2/3 证据（ToolSpec / Registry / 执行器与路由）',
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
$lines += '> 复现：`powershell -NoProfile -ExecutionPolicy Bypass -File v3.0/tool/verify_p2_1_3.ps1`'
[IO.File]::WriteAllText($Out, ($lines -join "`n"), (New-Object System.Text.UTF8Encoding($false)))
Write-Host ""
Write-Host ("== P2-1/2/3：" + $(if ($all) { 'PASS' } else { 'FAIL' }) + "（$okCount/$($results.Count)），报告 -> $Out") -ForegroundColor $(if ($all) { 'Green' } else { 'Red' })
if (-not $all) { exit 1 }
