# 第 3 步验收：教师端预警监控相关接口
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

# 1. 登录取 token
$cap = (Get-Json "$base/auth/captcha").data
$code = (Get-Json "$base/auth/captcha/debug?captchaId=$($cap.captchaId)").data
$login = Post-Json "$base/auth/login" @{ loginNo = '1001'; password = '000000'; captchaId = $cap.captchaId; captchaCode = $code } $null
if ($login.code -ne 0) { throw "登录失败: $($login.message)" }
$token = $login.data.token
"登录成功：$($login.data.realName) / $($login.data.role)"

# 2. 总览
$stats = Get-Json "$base/teacher/alerts/stats?teachingClassId=201" $token
"stats.code=$($stats.code)"
$s = $stats.data
"总览: total=$($s.total) red=$($s.red) orange=$($s.orange) yellow=$($s.yellow) active=$($s.active) handling=$($s.handling) closed=$($s.closed) todayNew=$($s.todayNew) pending=$($s.pending) evaluated=$($s.evaluated) lastEval=$($s.lastEvalTime) engine=$($s.engineVersion)"

# 3. 名单
$list = Get-Json "$base/teacher/alerts?teachingClassId=201" $token
"名单 code=$($list.code) 条数=$($list.data.Count)"
$list.data | Select-Object -First 6 | ForEach-Object {
  "  #$($_.id) $($_.studentNo) $($_.studentName) $($_.alertLevel) 分=$($_.riskScore) 缺=$($_.absentCount)/$($_.allowedAbsent) 余=$($_.remainingAbsent) 期末=$($_.finalExamNeeded) 规则=$($_.triggerRule) 状态=$($_.status)"
}

# 4. 详情（取第一条）
$first = $list.data[0]
$detail = Get-Json "$base/teacher/alerts/$($first.id)" $token
"详情 code=$($detail.code) 时间线=$($detail.data.timeline.Count) 处置=$($detail.data.interventions.Count)"
"  依据: $($detail.data.alert.alertReason)"
$detail.data.timeline | ForEach-Object { "  - $($_.eventTime) $($_.eventType) $($_.fromStatus)->$($_.toStatus) $($_.eventDesc)" }

# 5. 导出 xlsx
$headers = @{ Authorization = "Bearer $token" }
$tmp = Join-Path $env:TEMP 'alerts_export_test.xlsx'
Invoke-WebRequest -Uri "$base/teacher/alerts/export?teachingClassId=201" -Headers $headers -OutFile $tmp -UseBasicParsing
$fi = Get-Item $tmp
$bytes = [IO.File]::ReadAllBytes($tmp)
"导出: $($fi.Length) 字节  zip魔数: $($bytes[0]) $($bytes[1])  （50 4B = xlsx）"
Remove-Item $tmp

# 6. 已关闭的不能再操作（负向）
$closed = $list.data | Where-Object { $_.status -eq 'CLOSED' } | Select-Object -First 1
if ($closed) {
  $badClaim = Post-Json "$base/teacher/alerts/$($closed.id)/claim" @{} $token
  "已关闭 #$($closed.id) 认领 code=$($badClaim.code) msg=$($badClaim.message)"
}

# 7. 取一条待处理的做处置链路：提醒 → 认领 → 记录处理
$act = $list.data | Where-Object { $_.status -eq 'ACTIVE' } | Select-Object -First 1
"处置对象 #$($act.id) $($act.studentNo) $($act.studentName) $($act.alertLevel)"
$remind = Post-Json "$base/teacher/alerts/$($act.id)/remind" @{ note = '系统验收：请尽快与老师联系' } $token
"提醒 code=$($remind.code) 发出=$($remind.data) msg=$($remind.message)"
$claim = Post-Json "$base/teacher/alerts/$($act.id)/claim" @{} $token
"认领 code=$($claim.code) 状态=$($claim.data.status) 处置角色=$($claim.data.handleRole)"
$handle = Post-Json "$base/teacher/alerts/$($act.id)/handle" @{ actionType = 'TALK'; content = '系统验收：已约谈学生，确认缺课原因为兼职'; result = '承诺下周起正常出勤' } $token
"处理 code=$($handle.code) 状态=$($handle.data.status) 备注=$($handle.data.handleNote)"
$after = Get-Json "$base/teacher/alerts/$($act.id)" $token
"处置后时间线条数=$($after.data.timeline.Count) 处置记录=$($after.data.interventions.Count) 当前状态=$($after.data.alert.status)"

# 8. 越权：查别人教学班
$bad = Get-Json "$base/teacher/alerts?teachingClassId=203" $token
"越权校验 code=$($bad.code) msg=$($bad.message)"

# 9. 导出口径：等级过滤 RED
$red = Get-Json "$base/teacher/alerts?teachingClassId=201&level=RED" $token
"RED 过滤条数=$($red.data.Count)"
