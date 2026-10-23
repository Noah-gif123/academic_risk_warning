# 第 5 步验收：教务端规则配置（版本 + 生效切换 + 留痕）
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
"教务登录：admin001"

# 1. 当前规则
$r0 = Get-Json "$base/admin/rules" $adm
$act = $r0.data.active
"当前生效：$($act.engineVersion)  阈值 $($act.thresholdRed)/$($act.thresholdOrange)/$($act.thresholdYellow)  扣分 $($act.deductAbsent)/$($act.deductLate)/$($act.deductLeave)  请假免扣 $($act.leaveFreeTimes)  期末满分 $($act.finalFullScore)  版本数=$($r0.data.versions.Count)"

# 2. 判定一次（基线）
$ev0 = Post-Json "$base/engine/evaluate?teachingClassId=201&trigger=MANUAL" @{} $adm
"基线判定：$($ev0.data.evaluatedCount) 人，等级变化 $($ev0.data.levelChangedCount)，引擎 $($ev0.data.engineVersion)"

# 3. 非法阈值应被拦
$bad = Post-Json "$base/admin/rules" @{ thresholdRed = 50; thresholdOrange = 60; thresholdYellow = 40 } $adm
"非法阈值 code=$($bad.code) msg=$($bad.message)"

# 4. 新增版本（收紧阈值）并生效；版本号固定为 v9.9.9，验收后可用 sql/24 里的清理语句删掉
$new = Post-Json "$base/admin/rules" @{
  engineVersion = 'v9.9.9'
  thresholdRed = 70
  thresholdOrange = 55
  thresholdYellow = 35
  remark = '系统验收：期末阶段收紧三档阈值'
} $adm
"新增版本 code=$($new.code) msg=$($new.message)"
$act2 = $new.data.active
"生效版本：$($act2.engineVersion)  阈值 $($act2.thresholdRed)/$($act2.thresholdOrange)/$($act2.thresholdYellow)  id=$($act2.id)"
"历史版本数=$($new.data.versions.Count)"

# 4.1 版本号重复应被拦
$dup = Post-Json "$base/admin/rules" @{ engineVersion = 'v9.9.9'; remark = '重复版本号' } $adm
"重复版本号 code=$($dup.code) msg=$($dup.message)"

# 5. 按新规则重新判定（教学班 201）
$ev1 = Post-Json "$base/engine/evaluate?teachingClassId=201&trigger=MANUAL" @{} $adm
"新规则判定：$($ev1.data.evaluatedCount) 人，等级变化 $($ev1.data.levelChangedCount)，引擎 $($ev1.data.engineVersion)"

# 6. 教师端看等级变化后的分布
$tea = Login '1001'
$st = (Get-Json "$base/teacher/alerts/stats?teachingClassId=201" $tea).data
"教师端总览：total=$($st.total) red=$($st.red) orange=$($st.orange) yellow=$($st.yellow) engine=$($st.engineVersion)"

# 7. 回切老版本
$oldId = ($new.data.versions | Where-Object { $_.engineVersion -eq $act.engineVersion }).id
$back = Post-Json "$base/admin/rules/$oldId/activate" @{} $adm
"回切 code=$($back.code) msg=$($back.message) 生效=$($back.data.active.engineVersion)"
$ev2 = Post-Json "$base/engine/evaluate?teachingClassId=201&trigger=MANUAL" @{} $adm
"回切后判定：等级变化 $($ev2.data.levelChangedCount)，引擎 $($ev2.data.engineVersion)"
$st2 = (Get-Json "$base/teacher/alerts/stats?teachingClassId=201" $tea).data
"教师端总览：red=$($st2.red) orange=$($st2.orange) yellow=$($st2.yellow) engine=$($st2.engineVersion)"

# 8. 审计留痕
$au = (Get-Json "$base/admin/audit?bizType=RISK_ENGINE_CONFIG&page=1&size=5" $adm).data
"审计条数=$($au.total)"
$au.items | ForEach-Object { "  - $($_.createTime) $($_.action) $($_.detail)" }

# 9. 越权：教师调规则接口
try {
  Get-Json "$base/admin/rules" $tea | Out-Null
  "教师调规则接口：未拦截"
} catch {
  "教师调规则接口：已拦截（403）"
}

