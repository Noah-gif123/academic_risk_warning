# 评测 AI 批改：对 3 份不同质量的作答跑 suggest，并与人工分对比
$ErrorActionPreference = 'Stop'
$b = 'http://127.0.0.1:8080'
$root = 'E:\JavaDEVIDEA\workspace\JavaProject\academic_risk_warning\v2.0'
$log = New-Object System.Collections.Generic.List[string]
function L($s) { $log.Add([string]$s) }

function Api($method, $path, $token, $body) {
    $h = @{}
    if ($token) { $h['Authorization'] = "Bearer $token" }
    $p = @{ Uri = "$b$path"; Method = $method; UseBasicParsing = $true }
    if ($h.Count) { $p['Headers'] = $h }
    if ($method -ne 'GET') {
        $p['ContentType'] = 'application/json'
        $p['Body'] = if ($body) { $body } else { '{}' }
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

$t1 = Login '1001'
$ids = @(800001, 800002, 800003)
foreach ($sid in $ids) {
    $sw = [Diagnostics.Stopwatch]::StartNew()
    $r = Api 'POST' "/api/teacher/ai/grading/submissions/$sid/suggest" $t1
    $sw.Stop()
    if ($r.code -ne 0) { L("submission $sid 失败: " + $r.message); continue }
    L(('submission {0}: {1} 条建议, tokens={2}, 耗时 {3:N1}s, skipped={4}' -f $sid, @($r.data.suggestions).Count, $r.data.tokens, $sw.Elapsed.TotalSeconds, @($r.data.skipped).Count))
    foreach ($s in @($r.data.suggestions)) {
        $gates = @($s.checks)
        $gateTxt = (($gates | ForEach-Object { $_.code + '=' + $(if ($_.pass) { 'PASS' } else { 'WARN' }) }) -join ' ')
        L(('   q#{0} 满分 {1} → AI {2} | 闸门: {3} | 依据: {4}' -f $s.questionId, $s.fullScore, $s.suggestedScore, $gateTxt, $s.modelRubric))
        $cmt = [string]$s.comment
        if ($cmt.Length -gt 100) { $cmt = $cmt.Substring(0, 100) + '…' }
        L('      评语: ' + $cmt)
    }
}

# 与人工分对比（直接查库，避免前端字段猜测）
$cp = "$root\backend\build-verify-lib\mysql-connector-j-9.2.0.jar"
$sql = "select g.submission_id, q.q_no, g.full_score, g.suggested_score, a.score as human_score, abs(g.suggested_score - a.score) as diff from ai_grading_suggest g join submission_answer a on a.id = g.answer_id join question q on q.id = g.question_id where g.submission_id in (800001,800002,800003) and g.status = 'SUGGESTED' order by g.submission_id, q.q_no"
Push-Location "$root\tool\db"
$rows = & java -cp $cp Q.java $sql 2>&1
Pop-Location
L('')
L('=== AI 建议 vs 人工分 ===')
foreach ($line in $rows) { L($line) }

[IO.File]::WriteAllLines("$root\tool\_grading_eval.txt", $log, (New-Object Text.UTF8Encoding($false)))
Write-Output ('done ' + $log.Count)
