# 重建演示库后：按新成绩构成跑一次判定（admin 全学期）
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

$adm = Login 'admin001'
$ev = Post-Json "$base/engine/evaluate?termId=5&trigger=MANUAL" @{} $adm
"判定 code=$($ev.code) 人数=$($ev.data.evaluatedCount) 新增=$($ev.data.insertedCount) 更新=$($ev.data.updatedCount) 等级变化=$($ev.data.levelChangedCount) 引擎=$($ev.data.engineVersion) 耗时=$($ev.data.costMs)ms"

$cap = (Get-Json "$base/auth/captcha").data
$code = (Get-Json "$base/auth/captcha/debug?captchaId=$($cap.captchaId)").data
$tea = Login '1001'
foreach ($tc in @(201, 202)) {
  $st = (Get-Json "$base/teacher/alerts/stats?teachingClassId=$tc" $tea).data
  "教学班 $tc 总览：红 $($st.red) 橙 $($st.orange) 黄 $($st.yellow) 待处理 $($st.active) 处理中 $($st.handling) 已判定 $($st.evaluated)"
}
$list = (Get-Json "$base/teacher/alerts?teachingClassId=201" $tea).data
"名单 $($list.Count) 条："
$list | Select-Object -First 8 | ForEach-Object {
  "  #{0} {1} {2} {3} 分={4} 缺课{5}/{6} 期末需考={7} 状态={8}" -f $_.id, $_.studentNo, $_.studentName, $_.alertLevel, $_.riskScore, $_.absentCount, $_.allowedAbsent, $_.finalExamNeeded, $_.status
}
