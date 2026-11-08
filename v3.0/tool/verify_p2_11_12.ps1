<#
.SYNOPSIS
  P2-11/12 对账：记忆表/写入读取删除 + ai_run/step 轨迹。
#>
param(
    [string]$Db = 'study_warning_system_v3',
    [int]$Port = 18094,
    [string]$Out = '',
    [switch]$KeepBackend
)
$ErrorActionPreference = 'Stop'
. (Join-Path $PSScriptRoot 'lib\db_common.ps1')
$v3Dir = Split-Path -Parent $PSScriptRoot
$root = Split-Path -Parent $v3Dir
$backend = Join-Path $root 'v2.0\backend'
$tmpDir = Join-Path $root '.tmp-doc'
if (-not $Out) { $Out = Join-Path $v3Dir 'evidence\P2-6.md' }
$ctx = New-DbContext -Db $Db
$results = @()
function Check([string]$name, [bool]$ok, [string]$detail) { $script:results += [pscustomobject]@{ name = $name; ok = $ok; detail = $detail }; Write-Host ("  [" + $(if ($ok) { 'PASS' } else { 'FAIL' }) + "] " + $name + "：" + $detail) -ForegroundColor $(if ($ok) { 'Green' } else { 'Red' }) }
function J($u, $t) { $h = @{}; if ($t) { $h['Authorization'] = "Bearer $t" }; $r = Invoke-WebRequest -Uri $u -Headers $h -UseBasicParsing; return ([Text.Encoding]::UTF8.GetString($r.RawContentStream.ToArray()) | ConvertFrom-Json) }
function P($u, $o, $t) { $b = [Text.Encoding]::UTF8.GetBytes(($o | ConvertTo-Json -Depth 12 -Compress)); $h = @{}; if ($t) { $h['Authorization'] = "Bearer $t" }; $r = Invoke-WebRequest -Uri $u -Method Post -Body $b -ContentType 'application/json; charset=utf-8' -Headers $h -UseBasicParsing; return ([Text.Encoding]::UTF8.GetString($r.RawContentStream.ToArray()) | ConvertFrom-Json) }
function D($u, $t) { $h = @{}; if ($t) { $h['Authorization'] = "Bearer $t" }; $r = Invoke-WebRequest -Uri $u -Method Delete -Headers $h -UseBasicParsing; return ([Text.Encoding]::UTF8.GetString($r.RawContentStream.ToArray()) | ConvertFrom-Json) }
function Login($base, $no) { $cap = (J "$base/api/auth/captcha" $null).data; $code = (J "$base/api/auth/captcha/debug?captchaId=$($cap.captchaId)" $null).data; $r = P "$base/api/auth/login" @{ loginNo = $no; password = '000000'; captchaId = $cap.captchaId; captchaCode = $code } $null; if ($r.code -ne 0) { throw ("登录失败 " + $no) }; return $r.data.token }
function Sql1($sql) { $arr = @((Invoke-DbQuery -Ctx $ctx -Sql $sql -Database $Db | Select-Object -First 1)); return $arr[0] }

$java = $null
try {
    $libs = (Get-ChildItem (Join-Path $backend 'build-verify-lib') -Filter '*.jar' | ForEach-Object { $_.FullName }) -join ';'
    $cp = (Join-Path $backend 'build-verify') + ';' + (Join-Path $backend 'src\main\resources') + ';' + $libs
    $url = 'jdbc:mysql://127.0.0.1:3306/' + $Db + '?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true&useSSL=false'
    $env:AI_ENABLED = 'false'
    $java = Start-Process -FilePath 'java' -ArgumentList @('-Xmx512m', '-Dfile.encoding=UTF-8', '-cp', $cp, 'com.example.academic_risk_warning.AcademicRiskWarningApplication', "--server.port=$Port", '--app.auth.captcha-debug=true', "--spring.datasource.url=$url") -WorkingDirectory $backend -RedirectStandardOutput (Join-Path $tmpDir 'verify-p2-11-12.log') -RedirectStandardError (Join-Path $tmpDir 'verify-p2-11-12.log.err') -PassThru -WindowStyle Hidden
    $base = "http://127.0.0.1:$Port"
    $ready = $false
    for ($i = 0; $i -lt 60; $i++) { Start-Sleep -Milliseconds 1500; if ($java.HasExited) { break }; try { $r = Invoke-WebRequest -Uri "$base/api/auth/captcha" -UseBasicParsing -TimeoutSec 2; if ($r.StatusCode -eq 200) { $ready = $true; break } } catch { } }
    if (-not $ready) { throw '后端未就绪' }
    $admin = Login $base 'admin001'
    $student = Login $base '20250001'
    $studentId = [int](Sql1 "select s.id from student s join sys_account a on a.id=s.account_id where a.login_no='20250001' limit 1")

    # ---------- P2-11 记忆 ----------
    $rebuild = (P "$base/api/admin/memory/$studentId/rebuild" @{} $admin).data
    Check 'P2-11 写入/摘要生成' ($rebuild.exists -and $rebuild.memory.summary.Length -le 300) ("summaryLen=" + $rebuild.memory.summary.Length)
    $adminGet = (J "$base/api/admin/memory/$studentId" $admin).data
    Check 'P2-11 事件流' (@($adminGet.events).Count -gt 0) ("events=" + @($adminGet.events).Count)
    $mine = (J "$base/api/student/memory" $student).data
    Check 'P2-11 学生可查' $mine.exists ("exists=" + $mine.exists)
    $tool = (P "$base/api/agent/tool-call" @{ tool = 'getStudentMemory'; args = @{ studentId = $studentId } } $admin).data
    Check 'P2-11 工具读取记忆' ($tool.ok -and @($tool.data.items).Count -ge 1) ("items=" + @($tool.data.items).Count)
    $del = (D "$base/api/student/memory" $student).data
    $after = (J "$base/api/student/memory" $student).data
    $tool2 = (P "$base/api/agent/tool-call" @{ tool = 'getStudentMemory'; args = @{ studentId = $studentId } } $admin).data
    Check 'P2-11 删除后不再返回' ((-not $after.exists) -and (@($tool2.data.items).Count -eq 0)) ("deletedMemory=" + $del.deletedMemory + " exists=" + $after.exists + " items=" + @($tool2.data.items).Count)
    $null = P "$base/api/admin/memory/1001/rebuild" @{} $admin   # 恢复演示数据

    # ---------- P2-12 轨迹 ----------
    $diag = (P "$base/api/agent/diagnose" @{ question = '学生 1001 的干预记录和成效' } $admin).data
    $runId = $diag.traceRunId
    Check 'P2-12 运行落库' ($null -ne $runId -and $runId -gt 0) ("traceRunId=" + $runId)
    $runs = (J "$base/api/agent/runs?limit=10" $admin).data
    Check 'P2-12 轨迹列表' (@($runs | Where-Object { $_.id -eq $runId }).Count -eq 1) ("runs=" + @($runs).Count)
    $detail = (J "$base/api/agent/runs/$runId" $admin).data
    $stepOk = @($detail.steps | Where-Object { $_.stepRole -and $_.gateStatus -and $_.durationMs -ne $null }).Count -ge 1
    Check 'P2-12 步骤角色/门控/耗时' $stepOk ("steps=" + @($detail.steps).Count)
    $toolStep = @($detail.steps | Where-Object { $_.toolName })[0]
    Check 'P2-12 工具入参/返回摘要' ($null -ne $toolStep.argsDigest -and $null -ne $toolStep.outputDigest) ("tool=" + $toolStep.toolName + " args=" + $toolStep.argsDigest)
    $ledgerOk = (@($detail.ledger).Count -gt 0) -and (@($detail.ledger | Where-Object { $_.stepNo -gt 0 -and $_.metric -and $_.valueText -ne $null }).Count -gt 0)
    Check 'P2-12 数字回溯到来源步' $ledgerOk ("ledger=" + @($detail.ledger).Count)
    $dbRun = (Sql1 "select concat(provider,'/',role_count,'/',ifnull(run_key,'')) from ai_run where id=$runId") -split '/'
    Check 'P2-12 ai_run 扩展字段' ($dbRun[0] -eq 'LOCAL' -and [int]$dbRun[1] -ge 2 -and $dbRun[2].Length -gt 0) ("provider=" + $dbRun[0] + " roles=" + $dbRun[1])
    $stepCols = Sql1 "select count(*) from information_schema.columns where table_schema=database() and table_name='ai_run_step' and column_name in ('step_role','tool_name','args_digest','result_digest','gate_status')"
    Check 'P2-12 ai_run_step 迁移列' ([int]$stepCols -eq 5) ("columns=" + $stepCols)
} catch { Check '脚本执行' $false $_.Exception.Message } finally {
    if ($null -ne $java -and -not $KeepBackend) { Stop-Process -Id $java.Id -Force -ErrorAction SilentlyContinue; Wait-Process -Id $java.Id -Timeout 10 -ErrorAction SilentlyContinue }
}

$okCount = @($results | Where-Object { $_.ok }).Count
$all = ($okCount -eq $results.Count) -and ($results.Count -gt 0)
$lines = @('# v3.0 P2-11/12 证据（记忆 + 轨迹）', '', ('- 时间：' + (Get-Date -Format 'yyyy-MM-ddTHH:mm:sszzz')), ('- 结果：**' + $(if ($all) { 'PASS' } else { 'FAIL' }) + '**（' + $okCount + '/' + $results.Count + '）'), '', '| # | 检查 | 结果 | 证据 |', '|---|---|---|---|')
$i = 0; foreach ($r in $results) { $i++; $lines += ('| ' + $i + ' | ' + $r.name + ' | ' + $(if ($r.ok) { 'PASS' } else { 'FAIL' }) + ' | ' + ($r.detail -replace '\|', '/') + ' |') }
$lines += ''; $lines += '> 复现：`powershell -File v3.0/tool/verify_p2_11_12.ps1`'
[IO.File]::WriteAllText($Out, ($lines -join "`n"), (New-Object System.Text.UTF8Encoding($false)))
Write-Host ("== P2-11/12：" + $(if ($all) { 'PASS' } else { 'FAIL' }) + "（$okCount/$($results.Count)）-> $Out") -ForegroundColor $(if ($all) { 'Green' } else { 'Red' })
if (-not $all) { exit 1 }
