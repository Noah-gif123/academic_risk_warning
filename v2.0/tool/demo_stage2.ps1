# 学情预警系统 v2 · 第二阶段端到端演示脚本（只读为主，可反复运行）
#
# 用法：
#   1) 后端跑在 18080（演示用）并开启验证码调试：
#      java -jar academic-risk-warning-2.0.0-SNAPSHOT.jar --server.port=18080 --app.auth.captcha-debug=true
#   2) powershell -NoProfile -File v2.0\tool\demo_stage2.ps1
#   3) 想换端口/账号：powershell -NoProfile -File v2.0\tool\demo_stage2.ps1 -Base http://127.0.0.1:8080
#
# 覆盖链路：规则版本 → 重新判定 → 学生端结论 → 教师端名单与处置 → 学生申诉 → 教师核实
param(
  [string]$Base = 'http://127.0.0.1:18080',
  [string]$StudentNo = '20250001',
  [string]$TeacherNo = '1001',
  [string]$AdminNo = 'admin001',
  [long]$TeachingClassId = 201
)

$ErrorActionPreference = 'Stop'
$script:token = $null

function Post-Json($url, $obj) {
  $bytes = [Text.Encoding]::UTF8.GetBytes(($obj | ConvertTo-Json -Depth 6 -Compress))
  $headers = @{}
  if ($script:token) { $headers['Authorization'] = "Bearer $script:token" }
  $resp = Invoke-WebRequest -Uri $url -Method Post -Body $bytes -ContentType 'application/json; charset=utf-8' -Headers $headers -UseBasicParsing
  return ([Text.Encoding]::UTF8.GetString($resp.RawContentStream.ToArray()) | ConvertFrom-Json)
}
function Get-Json($url) {
  $headers = @{}
  if ($script:token) { $headers['Authorization'] = "Bearer $script:token" }
  $resp = Invoke-WebRequest -Uri $url -Headers $headers -UseBasicParsing
  return ([Text.Encoding]::UTF8.GetString($resp.RawContentStream.ToArray()) | ConvertFrom-Json)
}
function Login($no) {
  $cap = (Get-Json "$Base/api/auth/captcha").data
  $code = (Get-Json "$Base/api/auth/captcha/debug?captchaId=$($cap.captchaId)").data
  $r = Post-Json "$Base/api/auth/login" @{ loginNo = $no; password = '000000'; captchaId = $cap.captchaId; captchaCode = $code }
  if ($r.code -ne 0) { throw "登录失败($no)：$($r.message)" }
  $script:token = $r.data.token
  Write-Host ("  [登录] {0} {1}（{2}）" -f $r.data.loginNo, $r.data.realName, $r.data.role) -ForegroundColor DarkGray
}
function Section($text) {
  Write-Host ""
  Write-Host ("=== " + $text + " ===") -ForegroundColor Cyan
}

Section "1. 教务端：判定口径的版本管理"
Login $AdminNo
$rules = (Get-Json "$Base/api/admin/rules").data
Write-Host ("  生效版本：{0}（阈值 红{1}/橙{2}/黄{3}，扣分 缺{4}/迟{5}/假{6}，请假免扣 {7} 次）" -f `
  $rules.active.engineVersion, $rules.active.thresholdRed, $rules.active.thresholdOrange, $rules.active.thresholdYellow, `
  $rules.active.deductAbsent, $rules.active.deductLate, $rules.active.deductLeave, $rules.active.leaveFreeTimes)
Write-Host ("  版本数：{0}  当前生效：{1}" -f $rules.versions.Count, ($rules.versions | Where-Object { $_.isActive }).engineVersion)

Section "2. 触发一次判定（教学班 $TeachingClassId）"
$ev = (Post-Json "$Base/api/engine/evaluate?teachingClassId=$TeachingClassId&trigger=MANUAL" @{}).data
Write-Host ("  判定 {0} 人｜新增 {1}｜更新 {2}｜等级变化 {3}｜引擎 {4}｜耗时 {5} ms" -f `
  $ev.evaluatedCount, $ev.insertedCount, $ev.updatedCount, $ev.levelChangedCount, $ev.engineVersion, $ev.costMs)

Section "3. 教师端：预警监控（名单 + 总览）"
Login $TeacherNo
$st = (Get-Json "$Base/api/teacher/alerts/stats?teachingClassId=$TeachingClassId").data
Write-Host ("  总览：红 {0}｜橙 {1}｜黄 {2}｜待处理 {3}｜处理中 {4}｜已判定 {5} 人" -f `
  $st.red, $st.orange, $st.yellow, $st.active, $st.handling, $st.evaluated)
$list = (Get-Json "$Base/api/teacher/alerts?teachingClassId=$TeachingClassId").data
Write-Host "  名单（按风险分倒序，前 6 条）："
$list | Select-Object -First 6 | ForEach-Object {
  Write-Host ("    #{0} {1} {2}  {3} 分={4}  缺课{5}/{6}  期末需考={7}  {8}" -f `
    $_.id, $_.studentNo, $_.studentName, $_.alertLevel, $_.riskScore, $_.absentCount, $_.allowedAbsent, $_.finalExamNeeded, $_.status)
}

Section "4. 教师端：一条预警的详情与处置入口"
$one = $list | Select-Object -First 1
$d = (Get-Json "$Base/api/teacher/alerts/$($one.id)").data
Write-Host ("  预警 #{0}：{1} {2}｜{3}｜{4}" -f $d.alert.id, $d.alert.studentNo, $d.alert.studentName, $d.alert.alertLevel, $d.alert.status)
Write-Host ("  判定依据：{0}" -f $d.alert.alertReason)
Write-Host ("  三通道：A={0}｜B={1}（班内 {2}/{3}）｜C 未启用" -f $d.alert.channelALevel, $d.alert.channelBScore, $d.alert.channelBRank, $d.alert.channelBClassSize)
Write-Host ("  时间线：{0}" -f (($d.timeline | ForEach-Object { $_.eventType }) -join ' → '))
Write-Host ("  学生申诉：{0}" -f ($(if ($d.alert.studentResponse) { $d.alert.studentResponse } else { '（无）' })))
Write-Host ("  教师回复：{0}" -f ($(if ($d.alert.teacherReply) { $d.alert.teacherReply } else { '（待回复）' })))
$pendingAppeal = (Get-Json "$Base/api/teacher/alerts?teachingClassId=$TeachingClassId&appealPending=true").data
Write-Host ("  待核实申诉：{0} 条" -f $pendingAppeal.Count)

Section "5. 学生端：我的学情预警（结论 + 建议 + 申诉）"
Login $StudentNo
$ov = (Get-Json "$Base/api/student/warnings").data
Write-Host ("  {0} 本学期 {1} 门课：红 {2}｜橙 {3}｜黄 {4}｜正常 {5}｜已申诉 {6}" -f `
  $ov.studentName, $ov.summary.total, $ov.summary.red, $ov.summary.orange, $ov.summary.yellow, $ov.summary.green, $ov.summary.appealed)
$ov.items | Select-Object -First 3 | ForEach-Object {
  Write-Host ("    [{0}] {1}｜{2}" -f $_.level, $_.courseName, $_.conclusion)
}
Write-Host "  总体建议："
$ov.advice | ForEach-Object { Write-Host ("    - " + $_) }

$withAlert = $ov.items | Where-Object { $_.alertId } | Select-Object -First 1
if ($withAlert) {
  $sd = (Get-Json "$Base/api/student/warnings/$($withAlert.enrollmentId)").data
  Write-Host ("  详情（{0}）：时间线 {1} 个节点，建议 {2} 条" -f $sd.item.courseName, $sd.timeline.Count, $sd.advice.Count)
  $sd.advice | ForEach-Object { Write-Host ("    - " + $_) }
  Write-Host ("  申诉状态：{0}" -f ($(if ($sd.item.studentResponse) { "已提交（$($sd.item.studentResponse)）" } else { '未申诉（可提交一次）' })))
  if ($sd.item.teacherReply) {
    Write-Host ("  老师回复：{0}" -f $sd.item.teacherReply)
  }
}

Section "6. 课程导学（成绩构成 + 各小项说明）"
$cards = (Get-Json "$Base/api/student/course-cards").data.items
foreach ($c in $cards) {
  $cd = (Get-Json "$Base/api/student/course/$($c.teachingClassId)").data
  $len = if ($cd.guideContent) { $cd.guideContent.Length } else { 0 }
  $plain = if ($len -gt 0) { ($cd.guideContent -replace '<[^>]+>', ' ' -replace '\s+', ' ').Trim() } else { '' }
  if ($len -gt 0) { $plain = $plain.Substring(0, [Math]::Min(40, $plain.Length)) }
  Write-Host ("  {0}（{1}）：导学 {2} 字｜{3}" -f $c.courseName, $c.className, $len, $plain)
}

Section "7. 口径说明（学生端可见）"
$ov.guidelines | ForEach-Object { Write-Host ("  · " + $_) }

Write-Host ""
Write-Host "演示结束。以上数字均来自真实接口，可直接截图用于论文/答辩。" -ForegroundColor Green
