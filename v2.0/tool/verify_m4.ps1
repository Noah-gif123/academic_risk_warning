# 第 4 步验收：学生端 我的学情预警 + 申诉
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
  if ($r.code -ne 0) { throw "登录失败($no): $($r.message)" }
  return $r.data.token
}

$stu = Login '20250001'
"学生登录：20250001"

# 1. 概览
$ov = Get-Json "$base/student/warnings" $stu
"概览 code=$($ov.code) 学期=$($ov.data.termName) 引擎=$($ov.data.engineVersion) 最近判定=$($ov.data.lastEvalTime)"
$s = $ov.data.summary
"汇总: 共 $($s.total) 门  红 $($s.red) / 橙 $($s.orange) / 黄 $($s.yellow) / 正常 $($s.green)  已申诉 $($s.appealed)"
"总体建议："
$ov.data.advice | ForEach-Object { "  - $_" }
"课程结论："
$ov.data.items | ForEach-Object {
  "  [$($_.level)] $($_.courseName)（$($_.classCode) / $($_.teacherName)） 缺课 $($_.absentCount)/$($_.allowedAbsent) 综合分=$($_.totalScore) 期末需考=$($_.finalExamNeeded) 状态=$($_.status) alert=$($_.alertId)"
  "      结论：$($_.conclusion)"
}
"口径条数=$($ov.data.guidelines.Count)"

# 2. 详情（取有预警的一条）
$target = $ov.data.items | Where-Object { $_.alertId } | Select-Object -First 1
$d = Get-Json "$base/student/warnings/$($target.enrollmentId)" $stu
"详情 code=$($d.code) 课程=$($d.data.item.courseName) 时间线=$($d.data.timeline.Count) 建议=$($d.data.advice.Count)"
$d.data.timeline | ForEach-Object { "  - $($_.eventTime) $($_.label)" }

# 3. 越权：看不存在/别人的课程
$bad = Get-Json "$base/student/warnings/999999" $stu
"越权详情 code=$($bad.code) msg=$($bad.message)"

# 4. 申诉
$ap = Post-Json "$base/student/warnings/$($target.alertId)/appeal" @{ reason = '系统验收：缺课因参加学校竞赛，指导老师可证明，请核实' } $stu
"申诉 code=$($ap.code) 状态=$($ap.data.status) 回复=$($ap.data.studentResponse)"
"申诉时间=$($ap.data.studentResponseTime)"

# 5. 重复申诉应被拦
$again = Post-Json "$base/student/warnings/$($target.alertId)/appeal" @{ reason = '再申诉一次' } $stu
"重复申诉 code=$($again.code) msg=$($again.message)"

# 6. 空原因应被拦（换一条预警）
$target2 = $ov.data.items | Where-Object { $_.alertId -and $_.alertId -ne $target.alertId } | Select-Object -First 1
if ($target2) {
  $empty = Post-Json "$base/student/warnings/$($target2.alertId)/appeal" @{ reason = '   ' } $stu
  "空原因 code=$($empty.code) msg=$($empty.message)"
}

# 7. 教师端能看到申诉并收到通知
$tea = Login '1001'
$tl = Get-Json "$base/teacher/alerts?teachingClassId=201" $tea
$hit = $tl.data | Where-Object { $_.id -eq $target.alertId }
"教师端该条：状态=$($hit.status) 申诉=$($hit.studentResponse)"
$nt = Get-Json "$base/notifications?page=1&size=20" $tea
"教师消息 code=$($nt.code) 总条数=$($nt.data.total)"
$nt.data.items | Select-Object -First 5 | ForEach-Object { "  - [$($_.bizType)] $($_.title)" }

# 8. 学生端复看：已申诉标记
$ov2 = Get-Json "$base/student/warnings" $stu
"复看概览：已申诉 $($ov2.data.summary.appealed) 门"
$ov2.data.items | Where-Object { $_.alertId -eq $target.alertId } | ForEach-Object {
  "  课程 $($_.courseName) 申诉内容=$($_.studentResponse)"
}
