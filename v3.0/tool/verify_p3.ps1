<#
.SYNOPSIS
  P3 parity：Diagnostician 自研 vs Dify；离线/未配置自动 skip。
.DESCRIPTION
  - Dify 健康/App Key 不满足：输出 SKIPPED，退出码 0（skip-if-offline，不判失败）
  - 已配置：30 题双跑，记录数字/方向 parity 通过项与失败项；做一次故障注入
  - 结果写 eval/p3_parity.json 与 evidence/P3.md
#>
param(
    [string]$Db = 'study_warning_system_v3',
    [int]$Port = 18098,
    [string]$Out = '',
    [switch]$SkipIfOffline
)
$ErrorActionPreference = 'Stop'
. (Join-Path $PSScriptRoot 'lib\db_common.ps1')
$v3Dir = Split-Path -Parent $PSScriptRoot
$root = Split-Path -Parent $v3Dir
$backend = Join-Path $root 'v2.0\backend'
$tmpDir = Join-Path $root '.tmp-doc'
if (-not $Out) { $Out = Join-Path $v3Dir 'evidence\P3.md' }
$cases = Get-Content -LiteralPath (Join-Path $v3Dir 'eval\agent_cases_expected.json') -Raw -Encoding UTF8 | ConvertFrom-Json
$results = @()
function Check([string]$name, [bool]$ok, [string]$detail) {
    $script:results += [pscustomobject]@{ name = $name; ok = $ok; detail = $detail }
    Write-Host ("  [" + $(if ($ok) { 'PASS' } else { 'FAIL' }) + "] " + $name + "：" + $detail) -ForegroundColor $(if ($ok) { 'Green' } else { 'Red' })
}
function Write-P3Evidence($Parity, $Health, $Status) {
    $okCount = @($script:results | Where-Object { $_.ok }).Count
    $lines = @(
        '# v3.0 P3 证据（Dify 第二实现 / parity）',
        '',
        ('- 时间：' + (Get-Date -Format 'yyyy-MM-ddTHH:mm:sszzz')),
        ('- 状态：**' + $Status + '**'),
        ('- Dify health：`' + (($Health | ConvertTo-Json -Compress -Depth 6)) + '`'),
        ('- parity：passed=' + $Parity.passed + ' / failed=' + $Parity.failed + ' / skipped=' + $Parity.skipped + ' / total=' + $Parity.total),
        ('- 数字一致：' + $(if ($null -ne $Parity.numberParity) { $Parity.numberParity } else { 0 }) + '；方向一致：' + $(if ($null -ne $Parity.directionParity) { $Parity.directionParity } else { 0 })),
        '',
        '| # | 检查 | 结果 | 证据 |',
        '|---|---|---|---|'
    )
    $i = 0
    foreach ($r in $script:results) { $i++; $lines += ('| ' + $i + ' | ' + $r.name + ' | ' + $(if ($r.ok) { 'PASS' } else { 'FAIL' }) + ' | ' + ($r.detail -replace '\|', '/') + ' |') }
    if (@($Parity.failedCases).Count -gt 0) {
        $lines += ''
        $lines += '## 失败项'
        foreach ($f in @($Parity.failedCases)) { $lines += ('- #' + $f.id + ' ' + $f.question + '（数字=' + $f.num + '，方向=' + $f.dir + '）') }
    }
    $lines += ''
    $lines += '> 复现：`powershell -File v3.0/tool/verify_p3.ps1`；Dify 未配置/离线时按 skip-if-offline 输出 SKIPPED，不判失败。'
    [IO.File]::WriteAllText($Out, ($lines -join "`n"), (New-Object System.Text.UTF8Encoding($false)))
}

function J($u, $t) { $h = @{}; if ($t) { $h['Authorization'] = "Bearer $t" }; $r = Invoke-WebRequest -Uri $u -Headers $h -UseBasicParsing; return ([Text.Encoding]::UTF8.GetString($r.RawContentStream.ToArray()) | ConvertFrom-Json) }
function P($u, $o, $t) { $b = [Text.Encoding]::UTF8.GetBytes(($o | ConvertTo-Json -Depth 15 -Compress)); $h = @{}; if ($t) { $h['Authorization'] = "Bearer $t" }; $r = Invoke-WebRequest -Uri $u -Method Post -Body $b -ContentType 'application/json; charset=utf-8' -Headers $h -UseBasicParsing; return ([Text.Encoding]::UTF8.GetString($r.RawContentStream.ToArray()) | ConvertFrom-Json) }
function Login($base, $no) {
    $cap = (J "$base/api/auth/captcha" $null).data
    $code = (J "$base/api/auth/captcha/debug?captchaId=$($cap.captchaId)" $null).data
    $r = P "$base/api/auth/login" @{ loginNo = $no; password = '000000'; captchaId = $cap.captchaId; captchaCode = $code } $null
    if ($r.code -ne 0) { throw ("登录失败 " + $no) }
    return $r.data.token
}
function Start-App([int]$p, [string]$baseUrl) {
    $libs = (Get-ChildItem (Join-Path $backend 'build-verify-lib') -Filter '*.jar' | ForEach-Object { $_.FullName }) -join ';'
    $cp = (Join-Path $backend 'build-verify') + ';' + (Join-Path $backend 'src\main\resources') + ';' + $libs
    $url = 'jdbc:mysql://127.0.0.1:3306/' + $Db + '?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true&useSSL=false'
    $env:AI_ENABLED = 'true'
    $jargs = @('-Xmx512m', '-Dfile.encoding=UTF-8', '-cp', $cp,
        'com.example.academic_risk_warning.AcademicRiskWarningApplication',
        "--server.port=$p", '--app.auth.captcha-debug=true', "--spring.datasource.url=$url")
    if ($baseUrl) { $jargs += "--ai.base-url=$baseUrl" }
    return Start-Process -FilePath 'java' -ArgumentList $jargs -WorkingDirectory $backend `
        -RedirectStandardOutput (Join-Path $tmpDir ("p3-backend-" + $p + ".log")) `
        -RedirectStandardError (Join-Path $tmpDir ("p3-backend-" + $p + ".log.err")) -PassThru -WindowStyle Hidden
}
function Wait-App($p, $proc) {
    for ($i = 0; $i -lt 60; $i++) {
        Start-Sleep -Milliseconds 1500
        if ($proc.HasExited) { break }
        try { $r = Invoke-WebRequest -Uri "http://127.0.0.1:$p/api/auth/captcha" -UseBasicParsing -TimeoutSec 2; if ($r.StatusCode -eq 200) { return $true } } catch { }
    }
    return $false
}

$java = $null
$faultJava = $null
$sw = [Diagnostics.Stopwatch]::StartNew()
try {
    $java = Start-App $Port $null
    if (-not (Wait-App $Port $java)) { throw '后端未就绪' }
    $base = "http://127.0.0.1:$Port"
    $admin = Login $base 'admin001'
    $v1 = (P "$base/api/agent/v1/run" @{ question = '计算机1班当前红黄灯风险统计' } $admin).data
    Check 'P3 /v1/run 契约端点' ($v1.agent -eq 'diagnostician' -and -not [string]::IsNullOrWhiteSpace([string]$v1.answer)) ("provider=" + $v1.provider)
    $health = (J "$base/api/agent/dify/health" $admin).data
    $configured = [bool]$health.configured
    $reachable = [bool]$health.reachable

    if ((-not $configured) -or (-not $reachable)) {
        $reason = if (-not $reachable) { 'DIFY_UNREACHABLE' } else { 'DIFY_NOT_CONFIGURED: 缺少 AI_DIAGNOSTICIAN_APP_KEY' }
        # 离线时仍验证“DIFY 请求会安全降级本地”，但不把 parity 判为失败
        $fb = (P "$base/api/agent/diagnose" @{ question = '计算机1班当前红黄灯风险统计'; provider = 'DIFY' } $admin).data
        $fallbackOk = ($fb.provider -eq 'LOCAL') -and (-not [string]::IsNullOrWhiteSpace([string]$fb.fallbackReason))
        Check 'Dify 离线/未配置时降级本地' $fallbackOk ("provider=" + $fb.provider + " reason=" + $fb.fallbackReason)
        $parity = [ordered]@{
            status = 'SKIPPED'; reason = $reason; total = @($cases).Count; passed = 0; failed = 0; skipped = @($cases).Count
            numberParity = 0; directionParity = 0; failedCases = @(); fallbackOk = $fallbackOk
            health = $health; generatedAt = (Get-Date).ToString('yyyy-MM-ddTHH:mm:sszzz')
        }
        [IO.File]::WriteAllText((Join-Path $v3Dir 'eval\p3_parity.json'), ($parity | ConvertTo-Json -Depth 10), (New-Object System.Text.UTF8Encoding($false)))
        Check 'skip-if-offline（不计失败）' $true ("status=SKIPPED；" + $reason + "；30 例标记 skipped")
        Write-P3Evidence -Parity $parity -Health $health -Status 'SKIPPED'
        Write-Host ("== P3：SKIPPED（Dify 未配置/离线；已按 skip-if-offline 处理）") -ForegroundColor Yellow
        exit 0
    }

    # ---------- 已配置：30 题双跑 ----------
    $localRuns = @(); $difyRuns = @()
    foreach ($c in $cases) {
        $localRuns += (P "$base/api/agent/diagnose" @{ question = $c.question; provider = 'LOCAL' } $admin).data
        $difyRuns += (P "$base/api/agent/diagnose" @{ question = $c.question; provider = 'DIFY'; strictDify = $true } $admin).data
    }
    $numberParity = 0; $directionParity = 0; $passed = 0; $failed = 0
    $failedCases = @()
    for ($i = 0; $i -lt @($cases).Count; $i++) {
        $c = $cases[$i]; $l = $localRuns[$i]; $d = $difyRuns[$i]
        $dText = [string]$d.answer + ' ' + ($d.evidence | ConvertTo-Json -Depth 10 -Compress)
        $numOk = $true
        foreach ($n in @($c.expectedNumbers)) { if (-not $dText.Contains([string]$n.value)) { $numOk = $false } }
        $dirOk = ([bool]$l.refused -eq [bool]$d.refused)
        if ((-not $l.refused) -and (-not $d.refused)) {
            $lt = @($l.tools); $dt = @($d.tools)
            $dirOk = $dirOk -and (@($lt | Where-Object { $dt -contains $_ }).Count -gt 0)
        }
        if ($numOk) { $numberParity++ }
        if ($dirOk) { $directionParity++ }
        if ($numOk -or $dirOk) { $passed++ } else {
            $failed++; $failedCases += [pscustomobject]@{ id = $c.id; question = $c.question; num = $numOk; dir = $dirOk }
        }
    }
    $n = @($cases).Count
    $numRate = $numberParity / $n; $dirRate = $directionParity / $n
    $parityOk = ($numRate -ge 0.95) -or ($dirRate -ge 0.90)
    Check 'P3 数字一致 ≥95% 或方向一致 ≥90%' $parityOk ("number=" + [math]::Round($numRate * 100, 1) + "% direction=" + [math]::Round($dirRate * 100, 1) + "%")

    # ---------- 故障注入：错误 Dify 地址，非 strict 应降级 ----------
    $faultPort = $Port + 1
    $faultJava = Start-App $faultPort 'http://127.0.0.1:1'
    $faultOk = $false
    $faultReason = ''
    if (Wait-App $faultPort $faultJava) {
        $faultToken = Login "http://127.0.0.1:$faultPort" 'admin001'
        $fb = (P "http://127.0.0.1:$faultPort/api/agent/diagnose" @{ question = '计算机1班当前红黄灯风险统计'; provider = 'DIFY' } $faultToken).data
        $faultOk = ($fb.provider -eq 'LOCAL') -and (-not [string]::IsNullOrWhiteSpace([string]$fb.fallbackReason))
        $faultReason = [string]$fb.fallbackReason
    }
    Check 'P3 故障注入降级并记 fallback_reason' $faultOk ("reason=" + $faultReason)

    $parity = [ordered]@{
        status = $(if ($parityOk -and $faultOk) { 'PASS' } else { 'FAIL' })
        reason = ''
        total = $n; passed = $passed; failed = $failed; skipped = 0
        numberParity = $numberParity; directionParity = $directionParity
        numberRate = [math]::Round($numRate * 100, 2); directionRate = [math]::Round($dirRate * 100, 2)
        failedCases = $failedCases; fallbackOk = $faultOk; fallbackReason = $faultReason
        health = $health; generatedAt = (Get-Date).ToString('yyyy-MM-ddTHH:mm:sszzz')
    }
    [IO.File]::WriteAllText((Join-Path $v3Dir 'eval\p3_parity.json'), ($parity | ConvertTo-Json -Depth 10), (New-Object System.Text.UTF8Encoding($false)))
    Write-P3Evidence -Parity $parity -Health $health -Status $parity.status
    if (-not ($parityOk -and $faultOk)) { exit 1 }
} catch {
    Check '脚本执行' $false $_.Exception.Message
    $parity = [ordered]@{ status = 'ERROR'; reason = $_.Exception.Message; total = @($cases).Count; passed = 0; failed = 0; skipped = @($cases).Count; failedCases = @() }
    [IO.File]::WriteAllText((Join-Path $v3Dir 'eval\p3_parity.json'), ($parity | ConvertTo-Json -Depth 8), (New-Object System.Text.UTF8Encoding($false)))
    Write-P3Evidence -Parity $parity -Health @{} -Status 'ERROR'
    exit 1
} finally {
    foreach ($p in @($java, $faultJava)) {
        if ($null -ne $p) { Stop-Process -Id $p.Id -Force -ErrorAction SilentlyContinue; Wait-Process -Id $p.Id -Timeout 10 -ErrorAction SilentlyContinue }
    }
}
$sw.Stop()
