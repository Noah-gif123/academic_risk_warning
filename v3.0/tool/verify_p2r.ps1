<#
.SYNOPSIS
  P2-R 对账：RAG 工具化 / 引用绑定 / 独立重放 + 五类混淆。
#>
param(
    [string]$Db = 'study_warning_system_v3',
    [int]$Port = 18095,
    [string]$Out = '',
    [switch]$KeepBackend
)
$ErrorActionPreference = 'Stop'
. (Join-Path $PSScriptRoot 'lib\db_common.ps1')
$v3Dir = Split-Path -Parent $PSScriptRoot
$root = Split-Path -Parent $v3Dir
$backend = Join-Path $root 'v2.0\backend'
$tmpDir = Join-Path $root '.tmp-doc'
if (-not $Out) { $Out = Join-Path $v3Dir 'evidence\P2R.md' }
$ctx = New-DbContext -Db $Db
$results = @()
function Check([string]$name, [bool]$ok, [string]$detail) { $script:results += [pscustomobject]@{ name = $name; ok = $ok; detail = $detail }; Write-Host ("  [" + $(if ($ok) { 'PASS' } else { 'FAIL' }) + "] " + $name + "：" + $detail) -ForegroundColor $(if ($ok) { 'Green' } else { 'Red' }) }
function J($u, $t) { $h = @{}; if ($t) { $h['Authorization'] = "Bearer $t" }; $r = Invoke-WebRequest -Uri $u -Headers $h -UseBasicParsing; return ([Text.Encoding]::UTF8.GetString($r.RawContentStream.ToArray()) | ConvertFrom-Json) }
function P($u, $o, $t) { $b = [Text.Encoding]::UTF8.GetBytes(($o | ConvertTo-Json -Depth 12 -Compress)); $h = @{}; if ($t) { $h['Authorization'] = "Bearer $t" }; $r = Invoke-WebRequest -Uri $u -Method Post -Body $b -ContentType 'application/json; charset=utf-8' -Headers $h -UseBasicParsing; return ([Text.Encoding]::UTF8.GetString($r.RawContentStream.ToArray()) | ConvertFrom-Json) }
function Login($base, $no) { $cap = (J "$base/api/auth/captcha" $null).data; $code = (J "$base/api/auth/captcha/debug?captchaId=$($cap.captchaId)" $null).data; $r = P "$base/api/auth/login" @{ loginNo = $no; password = '000000'; captchaId = $cap.captchaId; captchaCode = $code } $null; if ($r.code -ne 0) { throw ("登录失败 " + $no) }; return $r.data.token }

$java = $null
try {
    $libs = (Get-ChildItem (Join-Path $backend 'build-verify-lib') -Filter '*.jar' | ForEach-Object { $_.FullName }) -join ';'
    $cp = (Join-Path $backend 'build-verify') + ';' + (Join-Path $backend 'src\main\resources') + ';' + $libs
    $url = 'jdbc:mysql://127.0.0.1:3306/' + $Db + '?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true&useSSL=false'
    $env:AI_ENABLED = 'false'
    $java = Start-Process -FilePath 'java' -ArgumentList @('-Xmx512m', '-Dfile.encoding=UTF-8', '-cp', $cp, 'com.example.academic_risk_warning.AcademicRiskWarningApplication', "--server.port=$Port", '--app.auth.captcha-debug=true', "--spring.datasource.url=$url") -WorkingDirectory $backend -RedirectStandardOutput (Join-Path $tmpDir 'verify-p2r.log') -RedirectStandardError (Join-Path $tmpDir 'verify-p2r.log.err') -PassThru -WindowStyle Hidden
    $base = "http://127.0.0.1:$Port"
    $ready = $false
    for ($i = 0; $i -lt 60; $i++) { Start-Sleep -Milliseconds 1500; if ($java.HasExited) { break }; try { $r = Invoke-WebRequest -Uri "$base/api/auth/captcha" -UseBasicParsing -TimeoutSec 2; if ($r.StatusCode -eq 200) { $ready = $true; break } } catch { } }
    if (-not $ready) { throw '后端未就绪' }
    $admin = Login $base 'admin001'
    $student = Login $base '20250001'

    # ---------- P2R-1 工具结构一致 ----------
    $tool = (P "$base/api/agent/tool-call" @{ tool = 'retrieveCourseDocs'; args = @{ courseId = 4; query = '极限'; topK = 4 } } $admin).data
    $hit = @($tool.data.hits)[0]
    $toolKeys = @('chunkId','docId','docName','sectionNo','score','rrfRank')
    $toolOk = ($null -ne $hit) -and (@($toolKeys | Where-Object { -not $hit.PSObject.Properties.Name.Contains($_) }).Count -eq 0)
    Check 'P2R-1 工具返回结构化命中' $toolOk ("keys=" + ($toolKeys -join ','))
    $diag = (P "$base/api/agent/diagnose" @{ question = '高等数学 课程中极限的定义是什么' } $admin).data
    $dc = @($diag.citations)[0]
    $diagOk = ($null -ne $dc) -and (@($toolKeys | Where-Object { -not $dc.PSObject.Properties.Name.Contains($_) }).Count -eq 0)
    Check 'P2R-1 Diagnostician 用同一结构' $diagOk ("chunkId=" + $dc.chunkId)
    $tutor = (P "$base/api/agent/tutor" @{ courseId = 4; question = '极限的定义是什么' } $student).data
    $tc = @($tutor.citations)[0]
    $tutorOk = ($null -ne $tc) -and (@($toolKeys | Where-Object { -not $tc.PSObject.Properties.Name.Contains($_) }).Count -eq 0)
    Check 'P2R-1 Tutor 用同一结构' ($tutorOk -and $tutor.citationsValid) ("chunkId=" + $tc.chunkId + " valid=" + $tutor.citationsValid)
    $sameKeys = (($toolKeys | Where-Object { $hit.PSObject.Properties.Name.Contains($_) }) -join ',') -eq (($toolKeys | Where-Object { $tc.PSObject.Properties.Name.Contains($_) }) -join ',')
    Check 'P2R-1 三个入口结构一致' ($sameKeys -and $toolOk -and $diagOk -and $tutorOk) ("tool/diagnoser/tutor keys 一致")

    # ---------- P2R-2 引用绑定/原文 ----------
    $ids = @($tutor.citations | ForEach-Object { [long]$_.chunkId })
    $in = ($ids | Select-Object -Unique) -join ','
    $found = [int](@(Invoke-DbQuery -Ctx $ctx -Sql "select count(*) from ai_kb_chunk where id in ($in)" -Database $Db)[0])
    Check 'P2R-2 引用可定位 chunk' ($found -eq @($ids | Select-Object -Unique).Count) ("citations=" + $ids.Count + " found=" + $found)
    $chunk = (J "$base/api/agent/kb/chunk/$($ids[0])" $admin).data
    Check 'P2R-2 点开原文片段' ($null -ne $chunk -and $chunk.content.Length -gt 0) ("chunk=" + $chunk.chunkId + " len=" + $chunk.content.Length)
    $trace = (J "$base/api/agent/runs/$($diag.traceRunId)" $admin).data
    $chunkLedger = @($trace.ledger | Where-Object { $_.metric -like '*ChunkId*' })
    Check 'P2R-2 轨迹含 chunkId 回溯' ($chunkLedger.Count -gt 0) ("entries=" + $chunkLedger.Count)

    # ---------- P2R-3 独立重放 + 五类混淆 ----------
    $docCount = [int](@(Invoke-DbQuery -Ctx $ctx -Sql "select count(*) from ai_kb_doc" -Database $Db)[0])
    $chunkCount = [int](@(Invoke-DbQuery -Ctx $ctx -Sql "select count(*) from ai_kb_chunk" -Database $Db)[0])
    $courseCount = [int](@(Invoke-DbQuery -Ctx $ctx -Sql "select count(distinct course_id) from ai_kb_doc" -Database $Db)[0])
    Check 'P2R-3 语料 100-150 篇/9 门课' (($docCount -ge 100) -and ($docCount -le 150) -and ($courseCount -eq 9)) ("docs=" + $docCount + " chunks=" + $chunkCount + " courses=" + $courseCount)

    $eval = (P "$base/api/admin/ai/rag-eval?mode=FULLTEXT&k=4" @{} $admin).data
    $cases = @(Invoke-DbQuery -Ctx $ctx -Sql "select id,course_id,question,answerable,gold_doc from ai_rag_eval_case order by id" -Database $Db)
    $hitCount = 0; $missCount = 0; $correctRefuse = 0; $falseRefuse = 0; $emptyCandidate = 0
    $answerable = 0; $unanswerable = 0
    $caseDetail = @()
    foreach ($line in $cases) {
        $cols = $line -split "`t"
        $courseId = [long]$cols[1]; $question = $cols[2]; $ans = ($cols[3] -eq '1')
        $gold = @($cols[4] -split '\|')
        $r = (P "$base/api/agent/tutor" @{ courseId = $courseId; question = $question } $admin).data
        $cites = @($r.citations)
        $matched = @($cites | Where-Object {
            $d = [string]$_.docName
            @($gold | Where-Object { $d -ne '' -and ($d.Contains($_) -or $_.Contains($d)) }).Count -gt 0
        }).Count -gt 0
        if ($ans) { $answerable++ } else { $unanswerable++ }
        $caseDetail += [pscustomobject]@{ id = [long]$cols[0]; answerable = $ans; matched = $matched; refused = $r.refused; docs = @($cites | ForEach-Object { $_.docName }) }
        if ($ans) {
            if ($r.refused) { $falseRefuse++ }
            elseif ($matched) { $hitCount++ }
            elseif ($cites.Count -gt 0) { $missCount++ }
            else { $emptyCandidate++ }
        } else {
            if ($r.refused) { $correctRefuse++ } else { $emptyCandidate++ }
        }
    }
    $matchedAnswerable = @($caseDetail | Where-Object { $_.answerable -and $_.matched }).Count
    $recall = if ($answerable -eq 0) { 0 } else { $matchedAnswerable / $answerable }
    $refuseAcc = if ($unanswerable -eq 0) { 0 } else { $correctRefuse / $unanswerable }
    $falseRefuseRate = if ($answerable -eq 0) { 0 } else { $falseRefuse / $answerable }
    $diffRecall = [math]::Abs($recall - [double]$eval.recallAtK)
    $diffRefuse = [math]::Abs($refuseAcc - [double]$eval.refuseAccuracy)
    $diffFalse = [math]::Abs($falseRefuseRate - [double]$eval.falseRefuse)
    $replayOk = ($diffRecall -lt 0.005) -and ($diffRefuse -lt 0.005) -and ($diffFalse -lt 0.005)
    $classesOk = (($hitCount + $missCount + $correctRefuse + $falseRefuse + $emptyCandidate) -eq $cases.Count)
    $five = [ordered]@{ hit = $hitCount; miss = $missCount; correctRefuse = $correctRefuse; falseRefuse = $falseRefuse; emptyCandidate = $emptyCandidate; total = $cases.Count }
    Check 'P2R-3 五类混淆齐全' $classesOk (($five | ConvertTo-Json -Compress))
    Check 'P2R-3 独立重算差异 <0.5%' $replayOk ("recall=" + [math]::Round($recall,4) + " vs " + $eval.recallAtK + "；refuse=" + [math]::Round($refuseAcc,4) + " vs " + $eval.refuseAccuracy + "；false=" + [math]::Round($falseRefuseRate,4) + " vs " + $eval.falseRefuse)
    $replay = [ordered]@{ generatedAt = (Get-Date).ToString('yyyy-MM-ddTHH:mm:sszzz'); fiveClass = $five; detail = $caseDetail; replay = @{ recall = [math]::Round($recall,4); refuseAccuracy = [math]::Round($refuseAcc,4); falseRefuse = [math]::Round($falseRefuseRate,4) }; main = @{ recallAtK = $eval.recallAtK; refuseAccuracy = $eval.refuseAccuracy; falseRefuse = $eval.falseRefuse }; diffs = @{ recall = [math]::Round($diffRecall,6); refuse = [math]::Round($diffRefuse,6); falseRefuse = [math]::Round($diffFalse,6) } }
    [IO.File]::WriteAllText((Join-Path $v3Dir 'eval\rag_replay.json'), ($replay | ConvertTo-Json -Depth 8), (New-Object System.Text.UTF8Encoding($false)))
} catch { Check '脚本执行' $false $_.Exception.Message } finally {
    if ($null -ne $java -and -not $KeepBackend) { Stop-Process -Id $java.Id -Force -ErrorAction SilentlyContinue; Wait-Process -Id $java.Id -Timeout 10 -ErrorAction SilentlyContinue }
}

$okCount = @($results | Where-Object { $_.ok }).Count
$all = ($okCount -eq $results.Count) -and ($results.Count -gt 0)
$lines = @('# v3.0 P2-R 证据（RAG 工具化 / 引用绑定 / 独立重放）', '', ('- 时间：' + (Get-Date -Format 'yyyy-MM-ddTHH:mm:sszzz')), ('- 结果：**' + $(if ($all) { 'PASS' } else { 'FAIL' }) + '**（' + $okCount + '/' + $results.Count + '）'), '', '| # | 检查 | 结果 | 证据 |', '|---|---|---|---|')
$i = 0; foreach ($r in $results) { $i++; $lines += ('| ' + $i + ' | ' + $r.name + ' | ' + $(if ($r.ok) { 'PASS' } else { 'FAIL' }) + ' | ' + ($r.detail -replace '\|', '/') + ' |') }
$lines += ''; $lines += '> 复现：`powershell -File v3.0/tool/verify_p2r.ps1`'
[IO.File]::WriteAllText($Out, ($lines -join "`n"), (New-Object System.Text.UTF8Encoding($false)))
Write-Host ("== P2-R：" + $(if ($all) { 'PASS' } else { 'FAIL' }) + "（$okCount/$($results.Count)）-> $Out") -ForegroundColor $(if ($all) { 'Green' } else { 'Red' })
if (-not $all) { exit 1 }
