param(
    [int]$Port = 8082,
    [string]$Base = ''
)
$ErrorActionPreference = 'Stop'
if (-not $Base) { $Base = "http://127.0.0.1:$Port" }
function Login($loginNo) {
    $cap = Invoke-RestMethod "$Base/api/auth/captcha"
    $code = Invoke-RestMethod "$Base/api/auth/captcha/debug?captchaId=$($cap.data.captchaId)"
    $body = @{ loginNo = $loginNo; password = '000000'; captchaId = $cap.data.captchaId; captchaCode = $code.data } | ConvertTo-Json
    $r = Invoke-RestMethod "$Base/api/auth/login" -Method Post -ContentType 'application/json' -Body $body
    if ($r.code -ne 0) { throw "登录失败：$($r.message)" }
    return $r.data
}
function Headers($u) { return @{ Authorization = 'Bearer ' + $u.token } }

$teacher = Login '1001'
$h = Headers $teacher
$list = Invoke-RestMethod "$Base/api/reports/student/1" -Headers $h
if ($list.code -ne 0) { throw "报告列表失败：$($list.message)" }
$report = $list.data | Where-Object { $_.teachingClassId -eq 201 } | Select-Object -First 1
if (-not $report) {
    $gen = Invoke-RestMethod "$Base/api/reports/student/1/generate?teachingClassId=201" -Method Post -Headers $h -ContentType 'application/json'
    if ($gen.code -ne 0) { throw "报告生成失败：$($gen.message)" }
    $report = $gen.data
}
if (-not $report.id) { throw '没有拿到独立报告 ID' }
$pdf = Invoke-WebRequest "$Base/api/reports/student/1/$($report.id)/pdf" -Headers $h -UseBasicParsing
$bytes = [byte[]]$pdf.Content
$head = [Text.Encoding]::ASCII.GetString($bytes[0..4])
if ($pdf.StatusCode -ne 200 -or $head -ne '%PDF-') { throw "PDF 校验失败：status=$($pdf.StatusCode), head=$head" }
"PASS reportId=$($report.id) runId=$($report.runId) metrics=$($report.metrics.dimensions.Count) pdfBytes=$($bytes.Length)"
