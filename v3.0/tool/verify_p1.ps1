<#
.SYNOPSIS
  P1 验收：健康检查、四端登录、权限矩阵、学生学期报告、PDF、方案 B 问卷/通知。
.DESCRIPTION
  依赖后端以 --app.auth.captcha-debug=true 启动（v3.0/tool/start_v3.ps1 -DebugCaptcha）。
  默认不消耗真实 AI token；加 -CheckLiveAi 才生成一次真实教学班报告。
.EXAMPLE
  powershell -NoProfile -ExecutionPolicy Bypass -File v3.0/tool/verify_p1.ps1
  powershell -NoProfile -ExecutionPolicy Bypass -File v3.0/tool/verify_p1.ps1 -CheckLiveAi
#>
param(
    [string]$Base = 'http://127.0.0.1:18101',
    [switch]$CheckLiveAi
)

$ErrorActionPreference = 'Stop'
$script:pass = 0
$script:fail = 0
function Ok([string]$name, [string]$detail = '') {
    $script:pass++
    Write-Host ("[PASS] " + $name + $(if ($detail) { "：$detail" } else { '' })) -ForegroundColor Green
}
function Bad([string]$name, [string]$detail = '') {
    $script:fail++
    Write-Host ("[FAIL] " + $name + $(if ($detail) { "：$detail" } else { '' })) -ForegroundColor Red
}
function Json([string]$method, [string]$path, $body = $null, $token = '') {
    $headers = @{}
    if ($token) { $headers['Authorization'] = "Bearer $token" }
    $req = @{ Uri = "$Base$path"; Method = $method; Headers = $headers; UseBasicParsing = $true; TimeoutSec = 180 }
    if ($null -ne $body) {
        $req['Body'] = [Text.Encoding]::UTF8.GetBytes(($body | ConvertTo-Json -Depth 8 -Compress))
        $req['ContentType'] = 'application/json; charset=utf-8'
    }
    try {
        $r = Invoke-WebRequest @req
        return [pscustomobject]@{ ok = $true; status = [int]$r.StatusCode; data = ([Text.Encoding]::UTF8.GetString($r.RawContentStream.ToArray()) | ConvertFrom-Json) }
    } catch {
        $status = 0
        try { $status = [int]$_.Exception.Response.StatusCode } catch { }
        $text = ''
        try { $text = $_.ErrorDetails.Message } catch { }
        return [pscustomobject]@{ ok = $false; status = $status; data = $null; error = $text }
    }
}
function Login([string]$no) {
    $cap = Json 'GET' '/api/auth/captcha'
    if (-not $cap.ok) { throw "获取验证码失败：$($cap.status)" }
    $cid = $cap.data.data.captchaId
    $code = Json 'GET' "/api/auth/captcha/debug?captchaId=$cid"
    if (-not $code.ok) { throw "验证码调试接口不可用（启动时加 -DebugCaptcha）" }
    $r = Json 'POST' '/api/auth/login' @{ loginNo = $no; password = '000000'; captchaId = $cid; captchaCode = $code.data.data }
    if (-not $r.ok -or $r.data.code -ne 0) { throw "登录失败 $no" }
    return $r.data.data.token
}

Write-Host "== P1 验收（$Base）" -ForegroundColor Cyan
$health = Json 'GET' '/api/ai/health'
if ($health.ok -and $health.data.code -eq 0) { Ok '后端健康检查' "enabled=$($health.data.data.enabled) directReady=$($health.data.data.directReady)" } else { Bad '后端健康检查' }

try {
    $student = Login '20250001'
    $teacher = Login '1001'
    $counselor = Login 'C001'
    $admin = Login 'admin001'
    Ok '四端登录' 'student / teacher / counselor / admin'
} catch {
    Bad '四端登录' $_.Exception.Message
    Write-Host "验收中止" -ForegroundColor Red
    exit 1
}

$cases = @(
    @{ name = '学生禁止访问辅导员接口'; token = $student; method = 'GET'; path = '/api/counselor/overview' },
    @{ name = '教师禁止访问行政班报告'; token = $teacher; method = 'GET'; path = '/api/counselor/reports/admin-class/1/latest' },
    @{ name = '辅导员禁止访问教学班报告'; token = $counselor; method = 'GET'; path = '/api/teacher/reports/class/201/latest' },
    @{ name = '辅导员禁止访问单课程学生报告'; token = $counselor; method = 'GET'; path = '/api/reports/student/1' }
)
foreach ($c in $cases) {
    $r = Json $c.method $c.path $null $c.token
    $denied = (-not $r.ok) -and ($r.status -eq 403 -or ($r.data -and $r.data.code -eq 403))
    if ($denied) { Ok $c.name "HTTP $($r.status)" } else { Bad $c.name "HTTP $($r.status)" }
}

$own = Json 'GET' '/api/counselor/reports/admin-class/1/latest' $null $counselor
if ($own.ok -and $own.data.code -eq 0) { Ok '辅导员读取本人行政班报告' } else { Bad '辅导员读取本人行政班报告' }
$tc = Json 'GET' '/api/teacher/reports/class/201/latest' $null $teacher
if ($tc.ok -and $tc.data.code -eq 0) { Ok '教师读取本人教学班报告' } else { Bad '教师读取本人教学班报告' }

$term = Json 'POST' '/api/counselor/reports/students/1/term-report' @{} $counselor
if ($term.ok -and $term.data.code -eq 0 -and $term.data.data.sections.Count -ge 4) {
    Ok '学生学期报告生成' "sections=$($term.data.data.sections.Count)"
} else { Bad '学生学期报告生成' }
try {
    $pdfPath = Join-Path $env:TEMP 'p1-student-term.pdf'
    $resp = Invoke-WebRequest -Uri "$Base/api/counselor/reports/students/1/term-report/pdf" -Headers @{ Authorization = "Bearer $counselor" } -OutFile $pdfPath -UseBasicParsing -TimeoutSec 60
    $bytes = [IO.File]::ReadAllBytes($pdfPath)
    $header = [Text.Encoding]::ASCII.GetString($bytes[0..3])
    if ($header -eq '%PDF') { Ok '学生学期报告 PDF' "bytes=$($bytes.Length)" } else { Bad '学生学期报告 PDF' "header=$header" }
} catch { Bad '学生学期报告 PDF' $_.Exception.Message }

$q = Json 'GET' '/api/counselor/questionnaires' $null $counselor
$n = Json 'GET' '/api/counselor/notices' $null $counselor
if ($q.ok -and $q.data.code -eq 0) { Ok '问卷方案 B（发布/列表）' } else { Bad '问卷方案 B（发布/列表）' }
if ($n.ok -and $n.data.code -eq 0) { Ok '系统通知（列表/已读）' } else { Bad '系统通知（列表/已读）' }

if ($CheckLiveAi) {
    $h = $health.data.data
    if (-not $h.directReady) { Bad '真实 AI 报告' 'directReady=false' }
    else {
        $live = Json 'POST' '/api/teacher/reports/class/201' @{} $teacher
        if ($live.ok -and $live.data.code -eq 0 -and $live.data.data.source -eq 'LLM') {
            Ok '真实 AI 报告' "model=$($live.data.data.model) tokens=$($live.data.data.tokens) checks=$($live.data.data.checksPassed)/$($live.data.data.checksTotal)"
        } else { Bad '真实 AI 报告' "source=$($live.data.data.source)" }
    }
}

Write-Host ""
Write-Host ("结果：PASS=" + $script:pass + " FAIL=" + $script:fail) -ForegroundColor $(if ($script:fail -eq 0) { 'Green' } else { 'Red' })
if ($script:fail -gt 0) { exit 1 }
