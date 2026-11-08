<#
.SYNOPSIS
  P2-14 护栏 10 例：越界/超步/工具报错/证据不足/空转/拒答。
#>
param(
    [string]$Db = 'study_warning_system_v3',
    [int]$Port = 18093,
    [string]$Out = '',
    [switch]$KeepBackend
)
$ErrorActionPreference = 'Stop'
$v3Dir = Split-Path -Parent $PSScriptRoot
$root = Split-Path -Parent $v3Dir
$backend = Join-Path $root 'v2.0\backend'
$tmpDir = Join-Path $root '.tmp-doc'
if (-not $Out) { $Out = Join-Path $v3Dir 'evidence\P2-5.md' }
function J($u, $t) { $h = @{}; if ($t) { $h['Authorization'] = "Bearer $t" }; $r = Invoke-WebRequest -Uri $u -Headers $h -UseBasicParsing; return ([Text.Encoding]::UTF8.GetString($r.RawContentStream.ToArray()) | ConvertFrom-Json) }
function P($u, $o, $t) { $b = [Text.Encoding]::UTF8.GetBytes(($o | ConvertTo-Json -Depth 12 -Compress)); $h = @{}; if ($t) { $h['Authorization'] = "Bearer $t" }; $r = Invoke-WebRequest -Uri $u -Method Post -Body $b -ContentType 'application/json; charset=utf-8' -Headers $h -UseBasicParsing; return ([Text.Encoding]::UTF8.GetString($r.RawContentStream.ToArray()) | ConvertFrom-Json) }
function Login($base, $no) { $cap = (J "$base/api/auth/captcha" $null).data; $code = (J "$base/api/auth/captcha/debug?captchaId=$($cap.captchaId)" $null).data; $r = P "$base/api/auth/login" @{ loginNo = $no; password = '000000'; captchaId = $cap.captchaId; captchaCode = $code } $null; if ($r.code -ne 0) { throw ("登录失败 " + $no) }; return $r.data.token }

$results = @()
function Check([string]$name, [bool]$ok, [string]$detail) { $script:results += [pscustomobject]@{ name = $name; ok = $ok; detail = $detail }; Write-Host ("  [" + $(if ($ok) { 'PASS' } else { 'FAIL' }) + "] " + $name + "：" + $detail) -ForegroundColor $(if ($ok) { 'Green' } else { 'Red' }) }

$java = $null
try {
    $libs = (Get-ChildItem (Join-Path $backend 'build-verify-lib') -Filter '*.jar' | ForEach-Object { $_.FullName }) -join ';'
    $cp = (Join-Path $backend 'build-verify') + ';' + (Join-Path $backend 'src\main\resources') + ';' + $libs
    $url = 'jdbc:mysql://127.0.0.1:3306/' + $Db + '?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true&useSSL=false'
    $env:AI_ENABLED = 'false'
    $java = Start-Process -FilePath 'java' -ArgumentList @('-Xmx512m', '-Dfile.encoding=UTF-8', '-cp', $cp, 'com.example.academic_risk_warning.AcademicRiskWarningApplication', "--server.port=$Port", '--app.auth.captcha-debug=true', "--spring.datasource.url=$url") -WorkingDirectory $backend -RedirectStandardOutput (Join-Path $tmpDir 'verify-p2-14.log') -RedirectStandardError (Join-Path $tmpDir 'verify-p2-14.log.err') -PassThru -WindowStyle Hidden
    $base = "http://127.0.0.1:$Port"
    $ready = $false
    for ($i = 0; $i -lt 60; $i++) { Start-Sleep -Milliseconds 1500; if ($java.HasExited) { break }; try { $r = Invoke-WebRequest -Uri "$base/api/auth/captcha" -UseBasicParsing -TimeoutSec 2; if ($r.StatusCode -eq 200) { $ready = $true; break } } catch { } }
    if (-not $ready) { throw '后端未就绪' }
    $admin = Login $base 'admin001'
    $counselor = Login $base 'C001'
    $student = Login $base '20250001'

    $c1 = (P "$base/api/agent/execute-json" @{ json = '{bad'; maxSteps = 1 } $admin).data
    Check '1 JSON 解析失败降级' ((-not $c1.ok) -and $c1.result.errorCode -eq 'PARSE_ERROR') ("code=" + $c1.result.errorCode)
    $c2 = (P "$base/api/agent/execute-json" @{ json = '{"tool":"rm_rf","args":{}}'; maxSteps = 1 } $admin).data
    Check '2 未知工具白名单拦截' ((-not $c2.ok) -and $c2.result.errorCode -eq 'UNKNOWN_TOOL') ("code=" + $c2.result.errorCode)
    $c3 = (P "$base/api/agent/execute-json" @{ json = '{"tool":"getStudentRisk","args":{"studentId":"x"}}'; maxSteps = 1 } $admin).data
    Check '3 参数类型拦截' ((-not $c3.ok) -and $c3.result.errorCode -eq 'INVALID_ARGS') ("code=" + $c3.result.errorCode)
    $c4 = (P "$base/api/agent/execute-json" @{ json = '{"tool":"getStudentRisk","args":{}}'; maxSteps = 1 } $admin).data
    Check '4 必填参数拦截' ((-not $c4.ok) -and $c4.result.errorCode -eq 'INVALID_ARGS') ("code=" + $c4.result.errorCode)
    $c5 = (P "$base/api/agent/execute-json" @{ json = '{"tool":"getRiskTrend","args":{"foo":1}}'; maxSteps = 1 } $admin).data
    Check '5 未知参数拦截' ((-not $c5.ok) -and $c5.result.errorCode -eq 'INVALID_ARGS') ("code=" + $c5.result.errorCode)
    $c6 = (P "$base/api/agent/execute-json" @{ json = '{"tool":"getClassRiskStats","args":{}}'; maxSteps = 0 } $admin).data
    Check '6 步数预算拦截' ((-not $c6.ok) -and $c6.result.errorCode -eq 'STEP_BUDGET_EXCEEDED') ("code=" + $c6.result.errorCode)
    $c7 = (P "$base/api/agent/critic" @{ intent = 'CLASS_STATS'; steps = @() } $admin).data
    Check '7 证据不足要求取证' $c7.asksEvidence ("message=" + $c7.message)
    $steps = @(
        @{ tool = 'getClassRiskStats'; args = @{ termId = 5 }; evidence = @{ total = 1927; red = 141; yellow = 383; riskRatio = 34.35 } },
        @{ tool = 'getClassRiskStats'; args = @{ termId = 5 }; evidence = @{ total = 1927; red = 141; yellow = 383; riskRatio = 34.35 } }
    )
    $c8 = (P "$base/api/agent/critic" @{ intent = 'CLASS_STATS'; steps = $steps } $admin).data
    Check '8 空转检测' ($c8.idleLoop -and $c8.asksEvidence) ("idle=" + $c8.idleLoop)
    $c9 = (P "$base/api/agent/run" @{ question = '学生 1301 的风险情况' } $counselor).data
    Check '9 越权学生拦截' ($c9.blocked -and ($c9.blockedReason -like 'FORBIDDEN*')) ("blocked=" + $c9.blockedReason)
    $c10 = (P "$base/api/agent/tutor" @{ courseId = 4; question = '量子纠缠与超导' } $student).data
    Check '10 无证据拒答' $c10.refused ("reason=" + $c10.gateReason)
} catch { Check '脚本执行' $false $_.Exception.Message } finally {
    if ($null -ne $java -and -not $KeepBackend) { Stop-Process -Id $java.Id -Force -ErrorAction SilentlyContinue; Wait-Process -Id $java.Id -Timeout 10 -ErrorAction SilentlyContinue }
}

$okCount = @($results | Where-Object { $_.ok }).Count
$all = ($okCount -eq $results.Count) -and ($results.Count -gt 0)
$lines = @('# v3.0 P2-14 护栏回归（10 例）', '', ('- 时间：' + (Get-Date -Format 'yyyy-MM-ddTHH:mm:sszzz')), ('- 结果：**' + $(if ($all) { 'PASS' } else { 'FAIL' }) + '**（' + $okCount + '/' + $results.Count + '）'), '', '| # | 检查 | 结果 | 证据 |', '|---|---|---|---|')
$i = 0; foreach ($r in $results) { $i++; $lines += ('| ' + $i + ' | ' + $r.name + ' | ' + $(if ($r.ok) { 'PASS' } else { 'FAIL' }) + ' | ' + ($r.detail -replace '\|', '/') + ' |') }
$lines += ''; $lines += '> 复现：`powershell -File v3.0/tool/verify_p2_14.ps1`'
[IO.File]::WriteAllText($Out, ($lines -join "`n"), (New-Object System.Text.UTF8Encoding($false)))
Write-Host ("== P2-14：" + $(if ($all) { 'PASS' } else { 'FAIL' }) + "（$okCount/$($results.Count)）-> $Out") -ForegroundColor $(if ($all) { 'Green' } else { 'Red' })
if (-not $all) { exit 1 }
