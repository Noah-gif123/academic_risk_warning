# 完整试卷演示：学生 20250001 交卷（客观题自动判分）→ 教师 1001 用 AI 批改拿建议分 → 采纳/定稿 → 出最终分
# 依赖：demo 试卷 20130 已由 sql/42_seed_grading_demo_paper.sql 建好；后端在 8080 运行
$ErrorActionPreference = 'Stop'
$port = if ($args.Count -gt 0) { $args[0] } else { '8080' }
$b = "http://127.0.0.1:$port"
$root = 'E:\JavaDEVIDEA\workspace\JavaProject\academic_risk_warning\v2.0'
$log = New-Object System.Collections.Generic.List[string]
function L($s) { $log.Add([string]$s) }

function Api($method, $path, $token, $bodyObj, $rawBody) {
    $h = @{}
    if ($token) { $h['Authorization'] = "Bearer $token" }
    $p = @{ Uri = "$b$path"; Method = $method; UseBasicParsing = $true }
    if ($h.Count) { $p['Headers'] = $h }
    if ($method -ne 'GET') {
        $p['ContentType'] = 'application/json'
        if ($rawBody) { $p['Body'] = [Text.Encoding]::UTF8.GetBytes($rawBody) }
        elseif ($bodyObj) { $p['Body'] = [Text.Encoding]::UTF8.GetBytes(($bodyObj | ConvertTo-Json -Depth 8 -Compress)) }
        else { $p['Body'] = [Text.Encoding]::UTF8.GetBytes('{}') }
    }
    $resp = Invoke-WebRequest @p
    return ([Text.Encoding]::UTF8.GetString($resp.RawContentStream.ToArray()) | ConvertFrom-Json)
}
function Login($no) {
    $bd = (@{ loginNo = $no; password = '000000' } | ConvertTo-Json)
    $r = Invoke-WebRequest -Uri "$b/api/auth/login" -Method POST -ContentType 'application/json' `
        -Body ([Text.Encoding]::UTF8.GetBytes($bd)) -UseBasicParsing
    return ([Text.Encoding]::UTF8.GetString($r.RawContentStream.ToArray()) | ConvertFrom-Json).data.token
}

# ---------- 0) 重置演示卷与提交（可反复运行） ----------
$cp = "$root\backend\build-verify-lib\mysql-connector-j-9.2.0.jar"
Push-Location "$root\tool\db"
& java -cp $cp Q.java "@$root\sql\42_seed_grading_demo_paper.sql" 2>&1 | Select-Object -Last 3
Pop-Location
L('演示卷已重置（6 题 100 分，客观 30 + 主观 70）')

$paperId = 20130
# ---------- 1) 学生交卷 ----------
$ts = Login '20250001'
$paper = Api 'GET' "/api/student/papers/$paperId" $ts
L('学生卷面: code=' + $paper.code + ' name=' + $paper.data.paperName + ' 题数=' + @($paper.data.questions).Count)

$answers = @(
    @{ qNo = 1; json = '{"keys":["B"]}' },                    # 正确
    @{ qNo = 2; json = '{"keys":["A"]}' },                    # 错误
    @{ qNo = 3; json = '{"keys":["A","B"]}' },                # 少选
    @{ qNo = 4; json = '{"text":"二进制只有 0 和 1 两个状态，正好对应电路的通断，物理上最容易实现、抗干扰强，而且加减乘除规则极简单，可以用逻辑门直接实现；它还能天然对应布尔逻辑的真假。适合的例子：开关状态、磁盘磁化方向都能表示两种状态；不适合直接表示的例子：十进制小数（0.1 在二进制里是无限循环，只能近似）和汉字（需要额外编码，如 UTF-8 用多个字节）。所以二进制是工程折中，实际要配合编码方案与浮点精度设计。"}' },
    @{ qNo = 5; json = '{"text":"冯·诺依曼结构包括运算器、控制器、存储器、输入设备和输出设备五部分，程序和数据都放在存储器里，CPU 按顺序取指令执行。"}' },
    @{ qNo = 6; json = '{"text":"我觉得作业容易被忽视，以后会认真对待。"}' }
)

# 拿题目 id（按 q_no 匹配）
$qmap = @{}
foreach ($q in @($paper.data.questions)) { $qmap[[int]$q.qNo] = $q.id }
$submitAnswers = @()
foreach ($a in $answers) {
    $submitAnswers += @{ questionId = $qmap[$a.qNo]; answerJson = $a.json }
}
$payload = @{ answers = $submitAnswers; startedAt = (Get-Date).AddMinutes(-25).ToString('yyyy-MM-ddTHH:mm:ss') } | ConvertTo-Json -Depth 6 -Compress
$sub = Api 'POST' "/api/student/papers/$paperId/submit" $ts $null $payload
L('交卷: code=' + $sub.code + ' submissionId=' + $sub.data.id + ' 客观分=' + $sub.data.objectiveScore + ' 状态=' + $sub.data.submitStatus)
$sid = $sub.data.id
if (-not $sid) { L('交卷失败: ' + $sub.message); $log | Out-File "$root\tool\_demo_grading.txt" -Encoding utf8; throw 'submit failed' }

# ---------- 2) 教师看卷面（拿到每题答案 id） ----------
$t1 = Login '1001'
$detail = Api 'GET' "/api/teacher/submissions/$sid" $t1
L('教师查看: code=' + $detail.code + ' 学生=' + $detail.data.studentName + ' 客观分=' + $detail.data.objectiveScore)
$ansIds = @{}
foreach ($a in @($detail.data.answers)) { $ansIds[[int]$a.qNo] = $a.id }

# ---------- 3) AI 批改建议 ----------
$sw = [Diagnostics.Stopwatch]::StartNew()
$sug = Api 'POST' "/api/teacher/ai/grading/submissions/$sid/suggest" $t1
$sw.Stop()
L(('AI 批改: code={0} 建议 {1} 条, tokens={2}, 耗时 {3:N1}s' -f $sug.code, @($sug.data.suggestions).Count, $sug.data.tokens, $sw.Elapsed.TotalSeconds))
$aiScores = @{}
foreach ($s in @($sug.data.suggestions)) {
    $gates = (@($s.checks) | ForEach-Object { $_.code + '=' + $(if ($_.pass) { 'PASS' } else { 'WARN' }) }) -join ' '
    $cmt = [string]$s.comment
    if ($cmt.Length -gt 90) { $cmt = $cmt.Substring(0, 90) + '…' }
    L(('  q#{0} 满分 {1} → AI 建议 {2} | 闸门 {3}' -f $s.questionId, $s.fullScore, $s.suggestedScore, $gates))
    L('     评语: ' + $cmt)
    if ($s.modelRubric) { L('     依据: ' + $s.modelRubric) }
    $aiScores[[string]$s.questionId] = @{ id = $s.id; score = [double]$s.suggestedScore }
}

# ---------- 4) 采纳第 1 条建议（演示"填入评分框"） ----------
$first = @($sug.data.suggestions)[0]
if ($first) {
    $ap = Api 'POST' "/api/teacher/ai/grading/suggestions/$($first.id)/apply" $t1 @{ score = $first.suggestedScore }
    L('采纳建议: code=' + $ap.code + ' status=' + $ap.data.status + ' 定稿分=' + $ap.data.finalScore)
}

# ---------- 5) 教师定稿（AI 建议 → 人工微调） ----------
$finalHuman = @{ 4 = 22.0; 5 = 12.0; 6 = 5.0 }   # 教师按评分维度给的分
$items = @()
foreach ($qno in 4, 5, 6) {
    $qid = $qmap[$qno]
    $items += @{ answerId = $ansIds[$qno]; score = $finalHuman[$qno]; comment = '按评分维度给分（教师定稿）' }
}
$grade = Api 'POST' "/api/teacher/submissions/$sid/grade" $t1 @{ items = $items; remark = 'AI 建议 + 教师定稿演示' }
L('提交批改: code=' + $grade.code + ' 状态=' + $grade.data.submitStatus + ' 客观=' + $grade.data.objectiveScore + ' 主观=' + $grade.data.subjectiveScore + ' 总分=' + $grade.data.totalScore)

# ---------- 6) 汇总对比 ----------
L('')
L('=== 主观题：AI 建议 vs 教师定稿 ===')
foreach ($qno in 4, 5, 6) {
    $qid = [string]$qmap[$qno]
    $ai = if ($aiScores.ContainsKey($qid)) { $aiScores[$qid].score } else { '—' }
    L(('  q#{0}  满分 {1}  AI {2}  教师 {3}' -f $qno, $(switch ($qno) { 4 { 25 } 5 { 25 } 6 { 20 } }), $ai, $finalHuman[$qno]))
}
L('')
L('=== 卷面结构 ===')
L('  客观题 3 道（30 分）：1 单选对、2 单选错、3 多选少选 → 自动判分')
L('  主观题 3 道（70 分）：4 完整作答、5 部分作答、6 敷衍作答 → AI 建议 + 教师定稿')

[IO.File]::WriteAllLines("$root\tool\_demo_grading.txt", $log, (New-Object Text.UTF8Encoding($false)))
Write-Output ('done ' + $log.Count)
