<#
.SYNOPSIS
  P2-0 前置对账：模型自检 + Grader 数据 + KB 语料覆盖。

.DESCRIPTION
  - P2-0a：读取 eval/model_check.json（reachable / 延迟 / tokens / 复现性）；
  - P2-0b：paper/question/submission/submission_answer 计数与结构；
  - P2-0c：ai_kb_doc/chunk 覆盖 9 门课、60+ 篇、全文检索可用。
  输出 `v3.0/evidence/P2-0.md`；有失败项非零退出。
#>
param(
    [string]$Db = 'study_warning_system_v3',
    [string]$Out = ''
)
$ErrorActionPreference = 'Stop'
. (Join-Path $PSScriptRoot 'lib\db_common.ps1')
$v3Dir = Split-Path -Parent $PSScriptRoot
if (-not $Out) { $Out = Join-Path $v3Dir 'evidence\P2-0.md' }
$ctx = New-DbContext -Db $Db
$results = @()
function Check([string]$name, [bool]$ok, [string]$detail) {
    $script:results += [pscustomobject]@{ name = $name; ok = $ok; detail = $detail }
    Write-Host ("  [" + $(if ($ok) { 'PASS' } else { 'FAIL' }) + "] " + $name + "：" + $detail) -ForegroundColor $(if ($ok) { 'Green' } else { 'Red' })
}
function Q([string]$sql) {
    return @(Invoke-DbQuery -Ctx $ctx -Sql $sql -Database $Db | Select-Object -First 1) -split "`t"
}

# ---------- P2-0a 模型自检 ----------
$mcPath = Join-Path $v3Dir 'eval\model_check.json'
$mc = $null
if (Test-Path -LiteralPath $mcPath) { $mc = Get-Content -LiteralPath $mcPath -Raw -Encoding UTF8 | ConvertFrom-Json }
Check 'P2-0a 模型可达' ($null -ne $mc -and $mc.reachable) ("model=" + $(if ($mc) { $mc.model } else { '-' }) +
    " lat=" + $(if ($mc) { $mc.firstLatencyMs } else { '-' }) + "ms tokens=" + $(if ($mc) { $mc.totalTokens } else { '-' }))
if ($mc) {
    Check 'P2-0a 返回内容非空' ([string]$mc.sampleAnswer).Length -gt 0 ("sample=" + ([string]$mc.sampleAnswer).Substring(0, [Math]::Min(24, ([string]$mc.sampleAnswer).Length)))
    Check 'P2-0a 复现性已记录' ($null -ne $mc.PSObject.Properties['temperature0Reproducible']) ("reproducible=" + $mc.temperature0Reproducible + "（false 非阻塞：P2 评测需多次运行/缓存）")
}

# ---------- P2-0b Grader 数据 ----------
$paper = [int](Q "select count(*) from paper")
$question = [int](Q "select count(*) from question")
$submission = [int](Q "select count(*) from submission")
$answer = [int](Q "select count(*) from submission_answer")
Check 'P2-0b 套卷数=2' ($paper -eq 2) ("paper=" + $paper)
Check 'P2-0b 主观题=10' ($question -eq 10) ("question=" + $question)
Check 'P2-0b 提交=60' ($submission -eq 60) ("submission=" + $submission)
Check 'P2-0b 作答=300' ($answer -eq 300) ("answer=" + $answer)
$badPaper = [int](Q "select count(*) from (select paper_id,count(*) c from question group by paper_id having c<>5) x")
Check 'P2-0b 每卷 5 题' ($badPaper -eq 0) ("异常卷数=" + $badPaper)
$badSub = [int](Q "select count(*) from (select submission_id,count(*) c from submission_answer group by submission_id having c<>5) x")
Check 'P2-0b 每份提交 5 题' ($badSub -eq 0) ("异常提交数=" + $badSub)
$scoreOk = [int](Q "select count(*) from submission_answer where score is null or score < 0 or score > 10")
Check 'P2-0b 人工分完整' ($scoreOk -eq 0) ("异常分=" + $scoreOk)
$courses = [int](Q "select count(distinct co.course_id) from paper p join course_offering co on co.id=p.offering_id")
Check 'P2-0b 覆盖 2 门课' ($courses -eq 2) ("courses=" + $courses)

# ---------- P2-0c KB 覆盖 ----------
$docs = [int](Q "select count(*) from ai_kb_doc")
$docCourses = [int](Q "select count(distinct course_id) from ai_kb_doc")
$chunks = [int](Q "select count(*) from ai_kb_chunk")
$chunkCourses = [int](Q "select count(distinct course_id) from ai_kb_chunk")
Check 'P2-0c 文档数≥60' ($docs -ge 60) ("docs=" + $docs)
Check 'P2-0c 覆盖 9 门课' ($docCourses -eq 9 -and $chunkCourses -eq 9) ("docCourses=" + $docCourses + " chunkCourses=" + $chunkCourses)
Check 'P2-0c chunk≥300' ($chunks -ge 300) ("chunks=" + $chunks)
$missing = [int](Q "select count(*) from (select course_id from ai_kb_doc where course_id in (5,6,9,11) group by course_id having count(*)>=8) x")
Check 'P2-0c 缺失 4 门各≥8 篇' ($missing -eq 4) ("达标课程数=" + $missing)
$empty = [int](Q "select count(*) from ai_kb_chunk where content is null or char_length(content)=0")
Check 'P2-0c chunk 内容非空' ($empty -eq 0) ("empty=" + $empty)
$ft5 = [int](Q "select count(*) from ai_kb_chunk where course_id=5 and match(content) against ('静电场' in natural language mode)")
$ft9 = [int](Q "select count(*) from ai_kb_chunk where course_id=9 and match(content) against ('索引' in natural language mode)")
$ft11 = [int](Q "select count(*) from ai_kb_chunk where course_id=11 and match(content) against ('特征值' in natural language mode)")
Check 'P2-0c 全文检索可用' (($ft5 + $ft9 + $ft11) -gt 0) ("静电场=" + $ft5 + " 索引=" + $ft9 + " 特征值=" + $ft11)

$okCount = @($results | Where-Object { $_.ok }).Count
$all = ($okCount -eq $results.Count)
$lines = @(
    '# v3.0 P2-0 前置证据（模型 / Grader 数据 / KB 语料）',
    '',
    ('- 时间：' + (Get-Date -Format 'yyyy-MM-ddTHH:mm:sszzz')),
    ('- 数据库：`' + $Db + '`'),
    ('- 结果：**' + $(if ($all) { 'PASS' } else { 'FAIL' }) + '**（' + $okCount + '/' + $results.Count + '）'),
    '',
    '## 1. P2-0a 模型自检',
    '',
    '- 模型：`' + $(if ($mc) { $mc.model } else { '-' }) + '`；可达：' + $(if ($mc) { $mc.reachable } else { '-' }) +
        '；延迟：' + $(if ($mc) { $mc.firstLatencyMs }) + ' ms / ' + $(if ($mc) { $mc.secondLatencyMs }) + ' ms；tokens=' + $(if ($mc) { $mc.totalTokens }),
    '- temperature=0 可复现：**' + $(if ($mc) { $mc.temperature0Reproducible } else { '-' }) + '**（false → P2 评测需多次运行/缓存，不能假设严格确定性）',
    '- 样例回答：' + $(if ($mc) { $mc.sampleAnswer } else { '-' }),
    '- 费用：未按单价折算；按 tokens 与模型官网单价另算',
    '',
    '## 2. P2-0b Grader 数据',
    '',
    '| 表 | 行数 | 说明 |',
    '|---|---|---|',
    ('| paper | ' + $paper + ' | 2 门课各 1 套 5 题主观卷，满分 50 |'),
    ('| question | ' + $question + ' | q_type=SHORT，含 ref/rubric，单题 10 分 |'),
    ('| submission | ' + $submission + ' | 每套 30 份，含人工总分 |'),
    ('| submission_answer | ' + $answer + ' | 每份 5 条，含人工分与评语 |'),
    '',
    '## 3. P2-0c KB 语料',
    '',
    '- 文档：**' + $docs + ' 篇**，覆盖 **' + $docCourses + '/9** 门课；chunk：**' + $chunks + '** 个',
    '- 缺失课程 5/6/9/11 各补 8 篇（`gen_kb_seed.js` 生成，source_type=RESOURCE）',
    '- 全文检索冒烟：静电场=' + $ft5 + '，索引=' + $ft9 + '，特征值=' + $ft11,
    '',
    '## 4. 复现',
    '',
    '```powershell',
    'node v3.0/tool/gen_v3_seed.js    # 含 P2-0b Grader 数据',
    'node v3.0/tool/gen_kb_seed.js    # P2-0c 32 篇语料',
    'powershell -File v3.0/tool/bootstrap_v3.ps1 -Force',
    'powershell -File v3.0/tool/run_v3_pipeline.ps1 -SkipSeed',
    'powershell -File v3.0/tool/verify_p2_0.ps1',
    '```',
    '',
    '> 边界：Grader 的人工分与 KB 语料均为演示合成数据；模型自检只证明可达与延迟，不代表输出质量。'
)
[IO.File]::WriteAllText($Out, ($lines -join "`n"), (New-Object System.Text.UTF8Encoding($false)))
Write-Host ""
Write-Host ("== P2-0：" + $(if ($all) { 'PASS' } else { 'FAIL' }) + "（$okCount/$($results.Count)），报告 -> $Out") -ForegroundColor $(if ($all) { 'Green' } else { 'Red' })
if (-not $all) { exit 1 }
