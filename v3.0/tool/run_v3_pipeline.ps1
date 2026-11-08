<#
.SYNOPSIS
  v3.0 重算流水线：seed → 判定 → 报告 → 快照 → 基线（一条命令）。

.DESCRIPTION
  对应 P0-5；只操作 v3.0 独立库（默认 study_warning_system_v3），v2.0 演示库不受影响。
  步骤可单独跳过（-SkipSeed / -SkipEngine / -SkipLifecycle / -SkipReports / -SkipSnapshot / -SkipBaseline），
  用于中断后续跑；全部重跑时结果幂等（同 seed 同数据）。

.EXAMPLE
  powershell -NoProfile -ExecutionPolicy Bypass -File v3.0/tool/run_v3_pipeline.ps1
  powershell -NoProfile -ExecutionPolicy Bypass -File v3.0/tool/run_v3_pipeline.ps1 -SkipSeed -SkipSnapshot

.NOTES
  PS 5.1 下本文件必须保存为 UTF-8 带 BOM。
#>
param(
    [string]$Db = 'study_warning_system_v3',
    [int]$Port = 18082,
    [long]$TermId = 5,
    [long]$Seed = 20261108,
    [switch]$SkipSeed,
    [switch]$SkipEngine,
    [switch]$SkipLifecycle,
    [switch]$SkipReports,
    [switch]$SkipSnapshot,
    [switch]$SkipBaseline,
    [switch]$KeepBackend,
    [string]$BaselinePath = '',
    [string]$SnapshotOut = '',
    [string]$SnapshotManifest = ''
)

$ErrorActionPreference = 'Stop'
. (Join-Path $PSScriptRoot 'lib\db_common.ps1')

$v3Dir = Split-Path -Parent $PSScriptRoot
$root = Split-Path -Parent $v3Dir
$backend = Join-Path $root 'v2.0\backend'
$tmpDir = Join-Path $root '.tmp-doc'
if (-not (Test-Path -LiteralPath $tmpDir)) { New-Item -ItemType Directory -Path $tmpDir -Force | Out-Null }
if (-not $BaselinePath) { $BaselinePath = Join-Path $v3Dir 'eval\baseline.json' }
if (-not $SnapshotOut) { $SnapshotOut = Join-Path $v3Dir 'sql\snapshot_v3.sql' }
if (-not $SnapshotManifest) { $SnapshotManifest = Join-Path $v3Dir 'sql\snapshot_v3.manifest.json' }
$backendLog = Join-Path $tmpDir 'pipeline-backend.log'
$script:reportRows = @()
$total = [Diagnostics.Stopwatch]::StartNew()
$stepTimes = @{}

function Write-Step([string]$text) {
    Write-Host ""
    Write-Host ("=== " + $text + " ===") -ForegroundColor Cyan
}
function Time-Step([string]$name, [scriptblock]$body) {
    $sw = [Diagnostics.Stopwatch]::StartNew()
    & $body
    $sw.Stop()
    $stepTimes[$name] = [math]::Round($sw.Elapsed.TotalSeconds, 1)
    Write-Host ("   [" + $name + " " + $stepTimes[$name] + "s]") -ForegroundColor DarkGray
}

# ============================================================ HTTP helpers（与 verify_*.ps1 同口径 UTF-8 解码）
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
function Login([string]$base, [string]$loginNo, [string]$password = '000000') {
    $cap = (Get-Json "$base/api/auth/captcha" $null).data
    $code = (Get-Json "$base/api/auth/captcha/debug?captchaId=$($cap.captchaId)" $null).data
    $login = Post-Json "$base/api/auth/login" @{ loginNo = $loginNo; password = $password; captchaId = $cap.captchaId; captchaCode = $code } $null
    if ($login.code -ne 0) { throw ("登录失败 {0}: {1}" -f $loginNo, $login.message) }
    return $login.data.token
}

# ============================================================ 1) seed
if (-not $SkipSeed) {
    Write-Step "1/5 seed：重建 v3 库（快照克隆 → 生成 → 导入 → P0-2/P0-3）"
    Time-Step 'seed' {
        & powershell -NoProfile -ExecutionPolicy Bypass -File (Join-Path $PSScriptRoot 'bootstrap_v3.ps1') -Force -Db $Db
        if ($LASTEXITCODE -ne 0) { throw "bootstrap_v3.ps1 失败（exit=$LASTEXITCODE）" }
    }
} else {
    Write-Host "== 跳过 seed"
}

# ============================================================ 2) 后端 + 3) 判定 + 4) 报告
if (-not ($SkipEngine -and $SkipReports)) {
    Write-Step "启动后端（端口 $Port / 库 $Db）"
    $libs = (Get-ChildItem (Join-Path $backend 'build-verify-lib') -Filter *.jar | ForEach-Object { $_.FullName }) -join ';'
    $cp = (Join-Path $backend 'build-verify') + ';' + (Join-Path $backend 'src\main\resources') + ';' + $libs
    $url = 'jdbc:mysql://127.0.0.1:3306/' + $Db +
        '?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true&useSSL=false'
    $javaArgs = @(
        '-Xmx512m', '-Dfile.encoding=UTF-8', '-cp', $cp,
        'com.example.academic_risk_warning.AcademicRiskWarningApplication',
        "--server.port=$Port", '--app.auth.captcha-debug=true', "--spring.datasource.url=$url"
    )
    $env:AI_ENABLED = 'false'
    if (Test-Path -LiteralPath $backendLog) { Remove-Item -LiteralPath $backendLog -Force -ErrorAction SilentlyContinue }
    $java = Start-Process -FilePath 'java' -ArgumentList $javaArgs -WorkingDirectory $backend `
        -RedirectStandardOutput $backendLog -RedirectStandardError ($backendLog + '.err') -PassThru -WindowStyle Hidden
    $base = "http://127.0.0.1:$Port"
    $ready = $false
    for ($i = 0; $i -lt 60; $i++) {
        Start-Sleep -Milliseconds 1500
        if ($java.HasExited) { break }
        try {
            $r = Invoke-WebRequest -Uri "$base/api/auth/captcha" -UseBasicParsing -TimeoutSec 2
            if ($r.StatusCode -eq 200) { $ready = $true; break }
        } catch { }
    }
    if (-not $ready) {
        $tail = (Get-Content -LiteralPath $backendLog -Tail 30 -ErrorAction SilentlyContinue) -join "`n"
        if (-not $java.HasExited) { Stop-Process -Id $java.Id -Force -ErrorAction SilentlyContinue }
        throw ("后端未就绪（端口 $Port）。日志尾部：`n" + $tail)
    }
    Write-Host ("   后端就绪 pid=" + $java.Id)
    try {
        if (-not $SkipEngine) {
            Write-Step "2/5 判定：POST /api/engine/evaluate?termId=$TermId"
            Time-Step 'engine' {
                $token = Login $base 'admin001'
                $res = Post-Json "$base/api/engine/evaluate?termId=$TermId" @{} $token
                if ($res.code -ne 0) { throw ("判定失败：" + $res.message) }
                $script:engineResult = $res.data
                Write-Host ("   evaluated=" + $res.data.evaluatedCount + " inserted=" + $res.data.insertedCount +
                    " updated=" + $res.data.updatedCount + " levelChanged=" + $res.data.levelChangedCount +
                    " costMs=" + $res.data.costMs)
            }
        } else { Write-Host "== 跳过判定" }

        if (-not $SkipLifecycle) {
            Write-Step "2.5/5 P1-0 生命周期：告警关闭 / 干预 / T+7 复评 / 对照组 / 8 周快照"
            Time-Step 'lifecycle' {
                & powershell -NoProfile -ExecutionPolicy Bypass -File (Join-Path $PSScriptRoot 'gen_p1_lifecycle.ps1') -Db $Db -Term $TermId -Seed $Seed
                if ($LASTEXITCODE -ne 0) { throw "P1-0 生命周期生成失败（exit=$LASTEXITCODE）" }
            }
        } else { Write-Host "== 跳过 P1-0 生命周期" }

        if (-not $SkipReports) {
            Write-Step "3/5 报告：学生/班级/课程/行政班三类演示报告（模板通道）"
            Time-Step 'reports' {
                $t1 = Login $base '1001'
                $tcReport = Post-Json "$base/api/teacher/reports/class/201" @{} $t1
                if ($tcReport.code -ne 0) { throw ("教学班报告失败：" + $tcReport.message) }
                $script:reportRows += [pscustomobject]@{ type = 'class'; target = 201; title = $tcReport.data.title; sections = @($tcReport.data.sections).Count; checks = "$($tcReport.data.checksPassed)/$($tcReport.data.checksTotal)" }
                $ofReport = Post-Json "$base/api/teacher/reports/offering/101" @{} $t1
                if ($ofReport.code -ne 0) { throw ("课程报告失败：" + $ofReport.message) }
                $script:reportRows += [pscustomobject]@{ type = 'offering'; target = 101; title = $ofReport.data.title; sections = @($ofReport.data.sections).Count; checks = "$($ofReport.data.checksPassed)/$($ofReport.data.checksTotal)" }
                $c1 = Login $base 'C001'
                $acReport = Post-Json "$base/api/counselor/reports/admin-class/1" @{} $c1
                if ($acReport.code -ne 0) { throw ("行政班报告失败：" + $acReport.message) }
                $script:reportRows += [pscustomobject]@{ type = 'adminClass'; target = 1; title = $acReport.data.title; sections = @($acReport.data.sections).Count; checks = "$($acReport.data.checksPassed)/$($acReport.data.checksTotal)" }
                $script:reportRows | ForEach-Object { Write-Host ("   [" + $_.type + "] " + $_.title + " sections=" + $_.sections + " checks=" + $_.checks) }
            }
        } else { Write-Host "== 跳过报告" }
    } finally {
        if (-not $KeepBackend) {
            Stop-Process -Id $java.Id -Force -ErrorAction SilentlyContinue
            Wait-Process -Id $java.Id -Timeout 10 -ErrorAction SilentlyContinue
            Write-Host "== 后端已停止"
        } else {
            Write-Host ("== 后端保留：pid=" + $java.Id + "（端口 $Port）")
        }
    }
} else {
    Write-Host "== 跳过后端/判定/报告"
}

# ============================================================ 4) 快照
if (-not $SkipSnapshot) {
    Write-Step "4/5 快照：v3.0 库 → snapshot_v3.sql + 清单"
    Time-Step 'snapshot' {
        & powershell -NoProfile -ExecutionPolicy Bypass -File (Join-Path $PSScriptRoot 'snapshot_v2.0.ps1') `
            -Db $Db -Out $SnapshotOut -Manifest $SnapshotManifest
        if ($LASTEXITCODE -ne 0) { throw "快照失败（exit=$LASTEXITCODE）" }
    }
} else { Write-Host "== 跳过快照" }

# ============================================================ 5) 基线
if (-not $SkipBaseline) {
    Write-Step "5/5 基线：写 v3.0/eval/baseline.json"
    Time-Step 'baseline' {
        $ctx = New-DbContext -Db $Db
        function Scalar([string]$sql) {
            return [string](@(Invoke-DbQuery -Ctx $ctx -Sql $sql -Database $Db) | Select-Object -First 1)
        }
        $levels = [ordered]@{}
        foreach ($line in (Invoke-DbQuery -Ctx $ctx -Sql 'select risk_level, count(*) from risk_state group by risk_level order by risk_level' -Database $Db)) {
            $c = $line -split "`t"; if ($c.Count -ge 2) { $levels[$c[0]] = [long]$c[1] }
        }
        $lambdaRows = @(Invoke-DbQuery -Ctx $ctx -Sql 'select distinct shrink_lambda from teacher_style where fused = 0 order by shrink_lambda' -Database $Db)
        $baseline = [ordered]@{
            format        = 'v3-baseline/1'
            db            = $Db
            termId        = $TermId
            generatedAt   = (Get-Date).ToString('yyyy-MM-ddTHH:mm:sszzz')
            engineVersion = (Scalar 'select max(engine_version) from risk_state')
            risk          = [ordered]@{
                total  = [long](Scalar 'select count(*) from risk_state')
                levels = $levels
            }
            alerts        = [ordered]@{ total = [long](Scalar 'select count(*) from alert_record') }
            scores        = [ordered]@{
                enrollments   = [long](Scalar 'select count(*) from enrollment where term_id=5')
                scoreResults  = [long](Scalar 'select count(*) from score_result where term_id=5')
                attendance    = [long](Scalar 'select count(*) from attendance_record where term_id=5')
            }
            courseFactor  = [ordered]@{
                rows          = [long](Scalar 'select count(*) from course_fail_rate_history')
                initialized   = [long](Scalar 'select count(*) from course_fail_rate_history where is_initialized=1 and course_factor is not null')
            }
            teacherStyle  = [ordered]@{
                rows          = [long](Scalar 'select count(*) from teacher_style')
                fused         = [long](Scalar 'select count(*) from teacher_style where fused=1')
                lambdaValues  = @($lambdaRows | ForEach-Object { [double]$_ })
                deltaNullRows = [long](Scalar 'select count(*) from risk_state where delta_used is null')
            }
            reports       = @($script:reportRows)
        }
        Write-JsonFile -Path $BaselinePath -Object $baseline
        Write-Host ("   baseline -> " + $BaselinePath)
        Write-Host ("   risk=" + $baseline.risk.total + " levels=" + (($levels.GetEnumerator() | ForEach-Object { $_.Key + ':' + $_.Value }) -join ',') +
            " alerts=" + $baseline.alerts.total + " reports=" + $baseline.reports.Count)
    }
} else { Write-Host "== 跳过基线" }

# ============================================================ 汇总
$total.Stop()
$totalSec = [math]::Round($total.Elapsed.TotalSeconds, 1)
Write-Host ""
Write-Host ("== 流水线完成：{0}s（{1}）" -f $totalSec, (($stepTimes.GetEnumerator() | Sort-Object Name | ForEach-Object { $_.Key + '=' + $_.Value + 's' }) -join ' ')) -ForegroundColor Green
if ($totalSec -gt 180) {
    Write-Host "!! 超过 3 分钟目标，需要优化" -ForegroundColor Yellow
}
