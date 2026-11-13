param([int]$Port = 18095)
$ErrorActionPreference = 'Stop'
$base = "http://127.0.0.1:$Port"
function Login($u) {
  $cap = Invoke-RestMethod "$base/api/auth/captcha"
  $code = Invoke-RestMethod "$base/api/auth/captcha/debug?captchaId=$($cap.data.captchaId)"
  $b = @{ loginNo=$u; password='000000'; captchaId=$cap.data.captchaId; captchaCode=$code.data } | ConvertTo-Json
  return (Invoke-RestMethod "$base/api/auth/login" -Method Post -ContentType 'application/json' -Body $b).data
}
$s = Login '20250001'
$h = @{ Authorization = 'Bearer ' + $s.token }
$created = @()
try {
  for ($i=1; $i -le 3; $i++) {
    $r = Invoke-RestMethod "$base/api/student/ai/sessions" -Method Post -Headers $h -ContentType 'application/json' `
      -Body (@{ courseId=1; teachingClassId=201; title="E2E-$i" } | ConvertTo-Json)
    $created += $r.data.id
  }
  $p = Invoke-RestMethod "$base/api/student/ai/sessions?courseId=1&page=1&size=1" -Headers $h
  if ($p.data.items.Count -ne 1 -or $p.data.total -lt 3) { throw "分页失败 total=$($p.data.total)" }
  $ren = Invoke-RestMethod "$base/api/student/ai/sessions/$($created[0])" -Method Patch -Headers $h -ContentType 'application/json' `
    -Body (@{ title='E2E-renamed' } | ConvertTo-Json)
  if ($ren.data.updated -ne 1) { throw '重命名失败' }
  $p2 = Invoke-RestMethod "$base/api/student/ai/sessions?courseId=1&page=1&size=10" -Headers $h
  $hit = $p2.data.items | Where-Object { $_.id -eq $created[0] -and $_.title -eq 'E2E-renamed' }
  if (-not $hit) { throw '重命名未生效' }
  "PASS sessions=$($p.data.total) pageSize=1 renamed=$($created[0])"
} finally {
  foreach ($id in $created) {
    try { Invoke-RestMethod "$base/api/student/ai/sessions/$id" -Method Delete -Headers $h | Out-Null } catch {}
  }
}
