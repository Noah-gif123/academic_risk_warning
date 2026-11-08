<#
.SYNOPSIS
  P2-13 评测：30 题工具选择 + SQL 期望数字一致性。
#>
param(
    [string]$Db = 'study_warning_system_v3',
    [int]$Port = 18092,
    [string]$Out = '',
    [switch]$KeepBackend
)
$ErrorActionPreference = 'Stop'
. (Join-Path $PSScriptRoot 'lib\db_common.ps1')
$v3Dir = Split-Path -Parent $PSScriptRoot
$root = Split-Path -Parent $v3Dir
$backend = Join-Path $root 'v2.0\backend'
$tmpDir = Join-Path $root '.tmp-doc'
if (-not $Out) { $Out = Join-Path $v3Dir 'evidence\P2-4.md' }
$ctx = New-DbContext -Db $Db
function J($u, $t) { $h = @{}; if ($t) { $h['Authorization'] = "Bearer $t" }; $r = Invoke-WebRequest -Uri $u -Headers $h -UseBasicParsing; return ([Text.Encoding]::UTF8.GetString($r.RawContentStream.ToArray()) | ConvertFrom-Json) }
function P($u, $o, $t) { $b = [Text.Encoding]::UTF8.GetBytes(($o | ConvertTo-Json -Depth 12 -Compress)); $h = @{}; if ($t) { $h['Authorization'] = "Bearer $t" }; $r = Invoke-WebRequest -Uri $u -Method Post -Body $b -ContentType 'application/json; charset=utf-8' -Headers $h -UseBasicParsing; return ([Text.Encoding]::UTF8.GetString($r.RawContentStream.ToArray()) | ConvertFrom-Json) }
function Login($base, $no) { $cap = (J "$base/api/auth/captcha" $null).data; $code = (J "$base/api/auth/captcha/debug?captchaId=$($cap.captchaId)" $null).data; $r = P "$base/api/auth/login" @{ loginNo = $no; password = '000000'; captchaId = $cap.captchaId; captchaCode = $code } $null; if ($r.code -ne 0) { throw ("登录失败 " + $no) }; return $r.data.token }
function Sql1($sql) { $arr = @((Invoke-DbQuery -Ctx $ctx -Sql $sql -Database $Db | Select-Object -First 1)); return $arr[0] }

$java = $null
$sw = [Diagnostics.Stopwatch]::StartNew()
$expected = Get-Content -LiteralPath (Join-Path $v3Dir 'eval\agent_cases_expected.json') -Raw -Encoding UTF8 | ConvertFrom-Json
$caseResults = @()
try {
    $libs = (Get-ChildItem (Join-Path $backend 'build-verify-lib') -Filter '*.jar' | ForEach-Object { $_.FullName }) -join ';'
    $cp = (Join-Path $backend 'build-verify') + ';' + (Join-Path $backend 'src\main\resources') + ';' + $libs
    $url = 'jdbc:mysql://127.0.0.1:3306/' + $Db + '?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true&useSSL=false'
    $env:AI_ENABLED = 'false'
    $java = Start-Process -FilePath 'java' -ArgumentList @('-Xmx512m', '-Dfile.encoding=UTF-8', '-cp', $cp, 'com.example.academic_risk_warning.AcademicRiskWarningApplication', "--server.port=$Port", '--app.auth.captcha-debug=true', "--spring.datasource.url=$url") -WorkingDirectory $backend -RedirectStandardOutput (Join-Path $tmpDir 'verify-p2-13.log') -RedirectStandardError (Join-Path $tmpDir 'verify-p2-13.log.err') -PassThru -WindowStyle Hidden
    $base = "http://127.0.0.1:$Port"
    $ready = $false
    for ($i = 0; $i -lt 60; $i++) { Start-Sleep -Milliseconds 1500; if ($java.HasExited) { break }; try { $r = Invoke-WebRequest -Uri "$base/api/auth/captcha" -UseBasicParsing -TimeoutSec 2; if ($r.StatusCode -eq 200) { $ready = $true; break } } catch { } }
    if (-not $ready) { throw '后端未就绪' }
    $admin = Login $base 'admin001'
    foreach ($c in $expected) {
        $sw2 = [Diagnostics.Stopwatch]::StartNew()
        $r = (P "$base/api/agent/diagnose" @{ question = $c.question } $admin).data
        $sw2.Stop()
        $tools = @($r.tools) -join ','
        $toolOk = ($tools -eq (@($c.expectedTools) -join ','))
        $text = [string]$r.answer + ' ' + ($r.evidence | ConvertTo-Json -Depth 8 -Compress)
        $miss = @()
        foreach ($n in @($c.expectedNumbers)) {
            $needle = [string]$n.value
            if (-not $text.Contains($needle)) { $miss += $needle }
        }
        $numOk = ($miss.Count -eq 0)
        $caseResults += [pscustomobject]@{ id = $c.id; intent = $c.intent; toolOk = $toolOk; numOk = $numOk; miss = ($miss -join ','); latency = [math]::Round($sw2.Elapsed.TotalMilliseconds) }
    }
    $toolAcc = @($caseResults | Where-Object { $_.toolOk }).Count / $caseResults.Count
    $numOkCount = @($caseResults | Where-Object { $_.numOk }).Count
    $lat = $caseResults | ForEach-Object { $_.latency } | Sort-Object
    $p95 = $lat[[Math]::Min($lat.Count - 1, [Math]::Ceiling(0.95 * $lat.Count) - 1)]
    $toolPass = $toolAcc -ge 0.9
    $numPass = @($caseResults | Where-Object { -not $_.numOk }).Count -eq 0
    $report = [ordered]@{
        generatedAt = (Get-Date).ToString('yyyy-MM-ddTHH:mm:sszzz')
        cases = @($caseResults).Count
        toolAccuracy = [math]::Round($toolAcc * 100, 2)
        numberConsistency = if ($numPass) { 100 } else { [math]::Round((@($caseResults | Where-Object { $_.numOk }).Count / $caseResults.Count) * 100, 2) }
        p95LatencyMs = $p95
        failed = @($caseResults | Where-Object { -not $_.toolOk -or -not $_.numOk })
        details = $caseResults
    }
    [IO.File]::WriteAllText((Join-Path $v3Dir 'eval\agent_eval_report.json'), ($report | ConvertTo-Json -Depth 8), (New-Object System.Text.UTF8Encoding($false)))
    $lines = @('# v3.0 P2-13 评测报告', '', ('- 时间：' + $report.generatedAt), ('- 用例：' + $report.cases), ('- 工具选择准确率：**' + $report.toolAccuracy + '%**'), ('- 期望数字一致率：**' + $report.numberConsistency + '%**'), ('- P95 时延：**' + $p95 + 'ms**'), '', '| # | 意图 | 工具链 | 数字 | 时延 |', '|---|---|---|---|---|')
    foreach ($x in $caseResults) { $lines += ('| ' + $x.id + ' | ' + $x.intent + ' | ' + $(if ($x.toolOk) { 'PASS' } else { 'FAIL' }) + ' | ' + $(if ($x.numOk) { 'PASS' } else { 'FAIL(' + $x.miss + ')' }) + ' | ' + $x.latency + 'ms |') }
    $lines += ''; $lines += '> 复现：`powershell -File v3.0/tool/gen_agent_eval.ps1` 后用 `run_agent_eval.ps1`'
    [IO.File]::WriteAllText($Out, ($lines -join "`n"), (New-Object System.Text.UTF8Encoding($false)))
    Write-Host ("P2-13：cases=" + $report.cases + " toolAcc=" + $report.toolAccuracy + "% num=" + $report.numberConsistency + "% p95=" + $p95 + "ms")
    if (-not ($toolPass -and $numPass)) { throw 'P2-13 阈值未达标' }
} finally {
    if ($null -ne $java -and -not $KeepBackend) { Stop-Process -Id $java.Id -Force -ErrorAction SilentlyContinue; Wait-Process -Id $java.Id -Timeout 10 -ErrorAction SilentlyContinue }
}
$sw.Stop()
Write-Host ("报告 -> " + $Out + "；总耗时 " + [math]::Round($sw.Elapsed.TotalSeconds, 1) + "s")
