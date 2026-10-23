# 重建演示库后的第二阶段演示数据（走真实接口，不手改表）
#   1) 教师 1001：认领 + 记录处理一条红色预警 → 处理中 + 干预记录 + 时间线
#   2) 学生 20250001：对一条红色预警提交申诉 → 教师端出现「申诉」
$ErrorActionPreference = 'Stop'
$base = 'http://127.0.0.1:18080/api'
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
function Login($no) {
  $cap = (Get-Json "$base/auth/captcha").data
  $code = (Get-Json "$base/auth/captcha/debug?captchaId=$($cap.captchaId)").data
  $r = Post-Json "$base/auth/login" @{ loginNo = $no; password = '000000'; captchaId = $cap.captchaId; captchaCode = $code } $null
  if ($r.code -ne 0) { throw "登录失败($no)：$($r.message)" }
  return $r.data.token
}

# 1) 教师端：认领 + 记录处理
$tea = Login '1001'
$list = (Get-Json "$base/teacher/alerts?teachingClassId=201" $tea).data
$red = $list | Where-Object { $_.alertLevel -eq 'RED' -and $_.status -eq 'ACTIVE' } | Select-Object -First 1
if ($red) {
  $c = Post-Json "$base/teacher/alerts/$($red.id)/claim" @{} $tea
  "认领 #{0} {1} {2} → {3}" -f $red.id, $red.studentNo, $red.studentName, $c.data.status
  $h = Post-Json "$base/teacher/alerts/$($red.id)/handle" @{
    actionType = 'TALK'
    content = '课后约谈：缺课主要因校外兼职，已与家长沟通调整排班'
    result = '学生承诺剩余课次全勤，并补交前两次作业'
  } $tea
  "记录处理 → 状态 {0}，处置备注 {1}" -f $h.data.status, $h.data.handleNote
  $d = (Get-Json "$base/teacher/alerts/$($red.id)" $tea).data
  "该条时间线：" + (($d.timeline | ForEach-Object { $_.eventType }) -join ' → ')
  "干预记录数：$($d.interventions.Count)"
}

# 另取一条黄色预警，提醒学生（学生端会收到消息）
$yellow = $list | Where-Object { $_.alertLevel -eq 'YELLOW' -and $_.status -eq 'ACTIVE' -and $_.studentNo -eq '20250002' } | Select-Object -First 1
if ($yellow) {
  $r = Post-Json "$base/teacher/alerts/$($yellow.id)/remind" @{ note = '你的缺课已到 3 次，再缺一次就会触及硬线，请保证出勤' } $tea
  "提醒 {0} {1} → code={2} 发出={3}" -f $yellow.studentNo, $yellow.studentName, $r.code, $r.data
}

# 2) 学生端：对红色预警提交申诉
$stu = Login '20250001'
$ov = (Get-Json "$base/student/warnings" $stu).data
"学生 20250001 状态：红 $($ov.summary.red) / 橙 $($ov.summary.orange) / 黄 $($ov.summary.yellow) / 正常 $($ov.summary.green)"
$ov.items | ForEach-Object { "  [{0}] {1}｜缺课 {2}/{3}｜期末需考 {4}｜{5}" -f $_.level, $_.courseName, $_.absentCount, $_.allowedAbsent, $_.finalExamNeeded, $_.conclusion }
$target = $ov.items | Where-Object { $_.level -eq 'RED' -and $_.alertId } | Select-Object -First 1
if ($target) {
  $ap = Post-Json "$base/student/warnings/$($target.alertId)/appeal" @{
    reason = '第六次缺课当天在校外参加学科竞赛，带队老师可证明，申请按公假处理'
  } $stu
  "申诉 #{0} {1} → code={2} 申诉时间={3}" -f $target.alertId, $target.courseName, $ap.code, $ap.data.studentResponseTime
}
