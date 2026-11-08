<#
.SYNOPSIS
  v3.0 本地一键 CI（P0-8 + P1-8）：编译 → 单测 → 数据校验 → 报告对账 → 基线 diff → 统计对账。

.DESCRIPTION
  本地门禁脚本（不上云）。任一步失败以非零退出码结束，输出 v3.0/tool/ci_report.md。
  - 第 1 步 编译：javac @javac-full.args
  - 第 2 步 单测：RunTest RiskMathTest（20）+ TriggerGateTest（5）
  - 第 3 步 数据校验：verify_data_v3.ps1（43 项）
  - 第 4 步 报告对账：起 v3 后端生成 3 类报告，要求 checksPassed == checksTotal
  - 第 5 步 基线 diff（有 baseline.json 才做）：DB 关键数字与 v3.0/eval/baseline.json 对比
  前置：v3.0 库已由 run_v3_pipeline.ps1 生成；本脚本不重建数据。

.EXAMPLE
  powershell -NoProfile -ExecutionPolicy Bypass -File v3.0/tool/ci.ps1
  powershell -NoProfile -ExecutionPolicy Bypass -File v3.0/tool/ci.ps1 -SkipBaseline
#>
param(
    [string]$Db = 'study_warning_system_v3',
    [int]$Port = 18083,
    [long]$TermId = 5,
    [switch]$SkipBaseline,
    [switch]$KeepBackend
)

$ErrorActionPreference = 'Stop'
. (Join-Path $PSScriptRoot 'lib\db_common.ps1')

$v3Dir = Split-Path -Parent $PSScriptRoot
$root = Split-Path -Parent $v3Dir
$backend = Join-Path $root 'v2.0\backend'
$tmpDir = Join-Path $root '.tmp-doc'
if (-not (Test-Path -LiteralPath $tmpDir)) { New-Item -ItemType Directory -Path $tmpDir -Force | Out-Null }
$reportPath = Join-Path $PSScriptRoot 'ci_report.md'
$baselinePath = Join-Path $v3Dir 'eval\baseline.json'
$total = [Diagnostics.Stopwatch]::StartNew()
$steps = New-Object System.Collections.Generic.List[object]

function Add-Step([string]$name, [bool]$ok, [string]$evidence, [double]$seconds) {
    $script:steps.Add([pscustomobject]@{ name = $name; ok = $ok; evidence = $evidence; seconds = $seconds })
    $color = if ($ok) { 'Green' } else { 'Red' }
    Write-Host ("   [" + $(if ($ok) { 'PASS' } else { 'FAIL' }) + "] " + $name + "（" + [math]::Round($seconds, 1) + "s）" + $(if ($evidence) { "：" + $evidence } else { "" })) -ForegroundColor $color
}
function Write-Step([string]$text) {
    Write-Host ""
    Write-Host ("=== " + $text + " ===") -ForegroundColor Cyan
}
# 原生命令包装：避免 PS 5.1 在 EAP=Stop 下把 stderr（如 javac 的注记）当终止错误
function Invoke-Native {
    param([string]$exe, [string[]]$argList)
    $prev = $ErrorActionPreference
    $ErrorActionPreference = 'Continue'
    try {
        $out = & $exe @argList 2>&1
        $code = $LASTEXITCODE
    } finally {
        $ErrorActionPreference = $prev
    }
    return [pscustomobject]@{ code = $code; out = @($out) }
}

Write-Host "== v3.0 CI（Db=$Db，Term=$TermId）" -ForegroundColor Cyan

# ============================================================ 1) 编译
Write-Step "1/5 编译"
$sw = [Diagnostics.Stopwatch]::StartNew()
Push-Location $backend
try {
    $r = Invoke-Native 'javac' @('@javac-full.args')
} finally { Pop-Location }
$sw.Stop()
Add-Step 'compile' ($r.code -eq 0) $(if ($r.code -eq 0) { 'javac OK' } else { (($r.out | Select-Object -Last 5) -join ' / ') }) $sw.Elapsed.TotalSeconds

# ============================================================ 2) 单测
Write-Step "2/5 单测"
$sw = [Diagnostics.Stopwatch]::StartNew()
$libs = (Get-ChildItem (Join-Path $backend 'build-verify-lib') -Filter *.jar | ForEach-Object { $_.FullName }) -join ';'
$testCp = (Join-Path $backend 'build-test') + ';' + (Join-Path $backend 'build-verify') + ';' +
          (Join-Path $backend 'src\main\resources') + ';' + $libs
$testDetail = @()
$testOk = $true
foreach ($cls in @('com.example.academic_risk_warning.util.RiskMathTest',
                   'com.example.academic_risk_warning.service.ai.TriggerGateTest')) {
    $r = Invoke-Native 'java' @('-cp', $testCp, 'RunTest', $cls)
    $passed = ($r.out | Select-String -Pattern 'PASSED\s+(\d+)\s*/\s*(\d+)' | Select-Object -Last 1)
    if ($r.code -ne 0 -or $null -eq $passed) {
        $testOk = $false
        $testDetail += ($cls.Split('.')[-1] + '：失败')
    } else {
        $testDetail += ($cls.Split('.')[-1] + ' ' + $passed.Matches[0].Groups[1].Value + '/' + $passed.Matches[0].Groups[2].Value)
    }
}
$sw.Stop()
Add-Step 'unit-tests' $testOk ($testDetail -join '；') $sw.Elapsed.TotalSeconds

# ============================================================ 3) 数据校验
Write-Step "3/5 数据校验（43 项）"
$sw = [Diagnostics.Stopwatch]::StartNew()
$r = Invoke-Native 'powershell' @('-NoProfile', '-ExecutionPolicy', 'Bypass', '-File', (Join-Path $PSScriptRoot 'verify_data_v3.ps1'), '-Db', $Db)
$verifyExit = $r.code
$verifyLine = ($r.out | Select-String -Pattern '校验完成：(.+)$' | Select-Object -Last 1)
$sw.Stop()
Add-Step 'data-verify' ($verifyExit -eq 0) $(if ($verifyLine) { $verifyLine.Matches[0].Groups[1].Value } else { '见 evidence/verify_data_v3.md' }) $sw.Elapsed.TotalSeconds

# ============================================================ 4) 报告对账
Write-Step "4/5 报告对账"
$sw = [Diagnostics.Stopwatch]::StartNew()
function Post-Json($url, $obj, $token) {
    $bytes = [Text.Encoding]::UTF8.GetBytes(($obj | ConvertTo-Json -Depth 6 -Compress))
    $headers = @{}
    if ($token) { $headers['Authorization'] = "Bearer $token" }
    $resp = Invoke-WebRequest -Uri $url -Method Post -Body $bytes -ContentType 'application/json; charset=utf-8' -Headers $headers -UseBasicParsing
    return ([Text.Encoding]::UTF8.GetString($resp.RawContentStream.ToArray()) | ConvertFrom-Json)
}
function Get-Json($url, $token) {
    $headers = @{}
    if ($token) { $headers['Authorization'] = "Bearer $token" }
    $resp = Invoke-WebRequest -Uri $url -Headers $headers -UseBasicParsing
    return ([Text.Encoding]::UTF8.GetString($resp.RawContentStream.ToArray()) | ConvertFrom-Json)
}
function Login([string]$base, [string]$loginNo) {
    $cap = (Get-Json "$base/api/auth/captcha" $null).data
    $code = (Get-Json "$base/api/auth/captcha/debug?captchaId=$($cap.captchaId)" $null).data
    $login = Post-Json "$base/api/auth/login" @{ loginNo = $loginNo; password = '000000'; captchaId = $cap.captchaId; captchaCode = $code } $null
    if ($login.code -ne 0) { throw ("登录失败 " + $loginNo) }
    return $login.data.token
}
$reportOk = $false
$reportDetail = ''
$java = $null
try {
    $cp = (Join-Path $backend 'build-verify') + ';' + (Join-Path $backend 'src\main\resources') + ';' + $libs
    $url = 'jdbc:mysql://127.0.0.1:3306/' + $Db +
        '?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true&useSSL=false'
    $env:AI_ENABLED = 'false'
    $java = Start-Process -FilePath 'java' -ArgumentList @(
        '-Xmx512m', '-Dfile.encoding=UTF-8', '-cp', $cp,
        'com.example.academic_risk_warning.AcademicRiskWarningApplication',
        "--server.port=$Port", '--app.auth.captcha-debug=true', "--spring.datasource.url=$url"
    ) -WorkingDirectory $backend -RedirectStandardOutput (Join-Path $tmpDir 'ci-backend.log') `
        -RedirectStandardError (Join-Path $tmpDir 'ci-backend.log.err') -PassThru -WindowStyle Hidden
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
    $t1 = Login $base '1001'
    $t2 = Login $base 'C001'
    $reports = @(
        @{ tag = 'class/201'; obj = (Post-Json "$base/api/teacher/reports/class/201" @{} $t1) },
        @{ tag = 'offering/101'; obj = (Post-Json "$base/api/teacher/reports/offering/101" @{} $t1) },
        @{ tag = 'adminClass/1'; obj = (Post-Json "$base/api/counselor/reports/admin-class/1" @{} $t2) }
    )
    $parts = @()
    $allOk = $true
    foreach ($rep in $reports) {
        $d = $rep.obj.data
        if ($rep.obj.code -ne 0) { $allOk = $false; $parts += ($rep.tag + ' 生成失败') ; continue }
        $parts += ($rep.tag + ' ' + $d.checksPassed + '/' + $d.checksTotal)
        if ($d.checksPassed -ne $d.checksTotal) { $allOk = $false }
    }
    $reportOk = $allOk
    $reportDetail = ($parts -join '；')
} catch {
    $reportOk = $false
    $reportDetail = $_.Exception.Message
} finally {
    if ($null -ne $java -and -not $KeepBackend) {
        Stop-Process -Id $java.Id -Force -ErrorAction SilentlyContinue
        Wait-Process -Id $java.Id -Timeout 10 -ErrorAction SilentlyContinue
    }
}
$sw.Stop()
Add-Step 'report-audit' $reportOk $reportDetail $sw.Elapsed.TotalSeconds

# ============================================================ 5) 基线 diff（可选）
if (-not $SkipBaseline -and (Test-Path -LiteralPath $baselinePath)) {
    Write-Step "5/5 基线 diff"
    $sw = [Diagnostics.Stopwatch]::StartNew()
    $diffOk = $true
    $diffDetail = @()
    try {
        $ctx = New-DbContext -Db $Db
        $baseline = Get-Content -LiteralPath $baselinePath -Raw -Encoding UTF8 | ConvertFrom-Json
        function Scalar([string]$sql) { return [string](@(Invoke-DbQuery -Ctx $ctx -Sql $sql -Database $Db) | Select-Object -First 1) }
        $checks = @(
            @{ name = 'risk.total'; sql = 'select count(*) from risk_state'; expected = [string]$baseline.risk.total },
            @{ name = 'alerts.total'; sql = 'select count(*) from alert_record'; expected = [string]$baseline.alerts.total },
            @{ name = 'cfr.initialized'; sql = 'select count(*) from course_fail_rate_history where is_initialized=1 and course_factor is not null'; expected = [string]$baseline.courseFactor.initialized },
            @{ name = 'teacherStyle.fused'; sql = 'select count(*) from teacher_style where fused=1'; expected = [string]$baseline.teacherStyle.fused },
            @{ name = 'deltaNull'; sql = 'select count(*) from risk_state where delta_used is null'; expected = [string]$baseline.teacherStyle.deltaNullRows }
        )
        foreach ($c in $checks) {
            $actual = Scalar $c.sql
            if ($actual -ne $c.expected) { $diffOk = $false; $diffDetail += ($c.name + ' 基线=' + $c.expected + ' 实际=' + $actual) }
        }
        foreach ($p in $baseline.risk.levels.PSObject.Properties) {
            $actual = Scalar ("select count(*) from risk_state where risk_level='" + $p.Name + "'")
            if ($actual -ne [string]$p.Value) { $diffOk = $false; $diffDetail += ('risk.' + $p.Name + ' 基线=' + $p.Value + ' 实际=' + $actual) }
        }
        if ($diffDetail.Count -eq 0) { $diffDetail = @('与 baseline 一致') }
    } catch {
        $diffOk = $false
        $diffDetail = @('基线 diff 异常：' + $_.Exception.Message)
    }
    $sw.Stop()
    Add-Step 'baseline-diff' $diffOk ($diffDetail -join '；') $sw.Elapsed.TotalSeconds
} else {
    Write-Host "== 跳过基线 diff（-SkipBaseline 或无 baseline.json）"
}

# ============================================================ 6) 统计对账（P1-8）
Write-Step "6/6 统计对账（P1-8）"
$sw = [Diagnostics.Stopwatch]::StartNew()
$statsOk = $false
$statsDetail = ''
try {
    $statsOut = & powershell -NoProfile -ExecutionPolicy Bypass -File (Join-Path $v3Dir 'tool\verify_stats.ps1') -Db $Db -TermId $TermId 2>&1
    $statsOk = ($LASTEXITCODE -eq 0)
    $statsDetail = (($statsOut | Select-Object -Last 1) -join ' ')
} catch {
    $statsOk = $false
    $statsDetail = $_.Exception.Message
}
$sw.Stop()
Add-Step 'stats-verify' $statsOk $statsDetail $sw.Elapsed.TotalSeconds

# ============================================================ 汇总 + 报告
$total.Stop()
$failed = @($steps | Where-Object { -not $_.ok })
$overall = if ($failed.Count -eq 0) { 'PASS' } else { 'FAIL' }
Write-Host ""
Write-Host ("== CI 结果：" + $overall + "（" + ($steps.Count - $failed.Count) + "/" + $steps.Count + " 步通过，" +
    [math]::Round($total.Elapsed.TotalSeconds, 1) + "s）") -ForegroundColor $(if ($overall -eq 'PASS') { 'Green' } else { 'Red' })

$md = New-Object System.Collections.Generic.List[string]
$md.Add('# v3.0 CI 报告')
$md.Add('')
$md.Add('- 时间：' + (Get-Date).ToString('yyyy-MM-ddTHH:mm:sszzz'))
$md.Add('- 数据库：`' + $Db + '`；总耗时：' + [math]::Round($total.Elapsed.TotalSeconds, 1) + 's')
$md.Add('- 结果：**' + $overall + '**（' + ($steps.Count - $failed.Count) + '/' + $steps.Count + ' 步通过）')
$md.Add('')
$md.Add('| # | 步骤 | 结果 | 耗时 | 证据 |')
$md.Add('|---|---|---|---|---|')
$i = 0
foreach ($s in $steps) {
    $i++
    $md.Add('| ' + $i + ' | ' + $s.name + ' | ' + $(if ($s.ok) { '✅ PASS' } else { '❌ FAIL' }) + ' | ' +
        [math]::Round($s.seconds, 1) + 's | ' + $s.evidence.Replace('|', '/') + ' |')
}
$md.Add('')
$md.Add('> 复现：`powershell -NoProfile -ExecutionPolicy Bypass -File v3.0/tool/ci.ps1`')
[IO.File]::WriteAllText([IO.Path]::GetFullPath($reportPath), ($md -join "`n"), (New-Object System.Text.UTF8Encoding($false)))
Write-Host ("   报告 -> " + $reportPath)

if ($failed.Count -gt 0) { exit 1 }
exit 0
