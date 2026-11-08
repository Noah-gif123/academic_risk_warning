param(
    [string]$EnvFile = '',
    [string]$Out = '',
    [int]$MaxTokens = 256,
    [int]$Retries = 2
)
$ErrorActionPreference = 'Stop'
[Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12
$v3Dir = Split-Path -Parent $PSScriptRoot
$root = Split-Path -Parent $v3Dir
if (-not $EnvFile) { $EnvFile = Join-Path $root 'v2.0\backend\ai.env' }
if (-not $Out) { $Out = Join-Path $v3Dir 'eval\model_check.json' }
if (-not (Test-Path -LiteralPath $EnvFile)) { throw "找不到 ai.env：$EnvFile" }

# 只读取键值，不打印密钥
$conf = @{}
foreach ($line in Get-Content -LiteralPath $EnvFile -Encoding UTF8) {
    if ($line -match '^\s*#' -or $line -notmatch '=') { continue }
    $k, $v = $line -split '=', 2
    $conf[$k.Trim()] = $v.Trim()
}
$base = $conf['AI_LLM_BASE_URL']
$key = $conf['AI_LLM_KEY']
$model = $conf['AI_LLM_MODEL']
if (-not $base -or -not $key -or -not $model) { throw 'ai.env 缺少 AI_LLM_BASE_URL / AI_LLM_KEY / AI_LLM_MODEL' }
$base = $base.TrimEnd('/')
$uri = "$base/chat/completions"
$prompt = '用一句话回答：学情预警系统的作用是什么？'
$bodyObj = @{
    model = $model
    temperature = 0
    max_tokens = $MaxTokens
    messages = @(
        @{ role = 'system'; content = '你是简洁的助教。' },
        @{ role = 'user'; content = $prompt }
    )
}
$body = $bodyObj | ConvertTo-Json -Depth 6 -Compress

function Invoke-Chat {
    $sw = [Diagnostics.Stopwatch]::StartNew()
    $raw = Invoke-WebRequest -Uri $uri -Method Post -Headers @{ Authorization = "Bearer $key" } `
        -ContentType 'application/json; charset=utf-8' -Body ([Text.Encoding]::UTF8.GetBytes($body)) -TimeoutSec 60 -UseBasicParsing
    $sw.Stop()
    $resp = ([Text.Encoding]::UTF8.GetString($raw.RawContentStream.ToArray()) | ConvertFrom-Json)
    return [pscustomobject]@{ ms = [math]::Round($sw.Elapsed.TotalMilliseconds); resp = $resp }
}

$ok = $false
$errorText = ''
$first = $null
$second = $null
for ($i = 0; $i -le $Retries; $i++) {
    try {
        if ($null -eq $first) { $first = Invoke-Chat }
        elseif ($null -eq $second) { $second = Invoke-Chat }
        if ($null -ne $first -and $null -ne $second) { $ok = $true; break }
    } catch {
        $errorText = $_.Exception.Message
        Start-Sleep -Seconds 2
    }
}
$text1 = ''
$text2 = ''
$tokens = $null
if ($null -ne $first) {
    $msg = $first.resp.choices[0].message
    $text1 = [string]$msg.content
    if (-not $text1 -and $msg.reasoning_content) { $text1 = [string]$msg.reasoning_content }
    if ($first.resp.usage) { $tokens = $first.resp.usage.total_tokens }
    $rawPath = Join-Path (Split-Path -Parent $Out) 'model_check_raw.json'
    [IO.File]::WriteAllText($rawPath, ($first.resp | ConvertTo-Json -Depth 8), (New-Object System.Text.UTF8Encoding($false)))
}
if ($null -ne $second) {
    $msg2 = $second.resp.choices[0].message
    $text2 = [string]$msg2.content
    if (-not $text2 -and $msg2.reasoning_content) { $text2 = [string]$msg2.reasoning_content }
}
$reproducible = ($text1 -ne '' -and $text1 -eq $text2)
$json = [ordered]@{
    checkedAt = (Get-Date).ToString('yyyy-MM-ddTHH:mm:sszzz')
    endpoint = ($base -replace '//[^/]+//', '//***//')   # 只保留形式，避免泄露内网
    model = $model
    reachable = $ok
    firstLatencyMs = $(if ($first) { $first.ms } else { $null })
    secondLatencyMs = $(if ($second) { $second.ms } else { $null })
    totalTokens = $tokens
    sampleAnswer = $text1
    temperature0Reproducible = $reproducible
    error = $(if (-not $ok) { $errorText } else { $null })
    note = '费用未按单价折算；token 用量见 totalTokens，按所用模型官网单价另算'
}
$dir = Split-Path -Parent $Out
if (-not (Test-Path -LiteralPath $dir)) { New-Item -ItemType Directory -Path $dir -Force | Out-Null }
[IO.File]::WriteAllText($Out, ($json | ConvertTo-Json -Depth 6), (New-Object System.Text.UTF8Encoding($false)))
Write-Host ("模型自检：" + $(if ($ok) { 'PASS' } else { 'FAIL' }) + "；model=$model；延迟=" +
    $(if ($first) { $first.ms } else { '-' }) + "ms；tokens=$tokens；结果 -> $Out") -ForegroundColor $(if ($ok) { 'Green' } else { 'Red' })
if (-not $ok) { exit 1 }
