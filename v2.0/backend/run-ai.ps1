# run-ai.ps1 —— 带上 AI Key 启动后端
#
# 用法（在 v2.0\backend 目录下）：
#   1) 复制 ai.env.example 为 ai.env，把 AI_LLM_KEY 填上（ai.env 不入库）
#   2) .\run-ai.ps1            # 有 jar 就跑 jar，没有就 gradlew bootRun
#      .\run-ai.ps1 -Boot      # 强制走 gradlew bootRun（开发用）
#      .\run-ai.ps1 -Jar       # 强制跑 build\libs 里的 bootJar
#
# 它只做两件事：把 ai.env 里的 KEY=VALUE 注入当前进程环境变量；启动后端。
# 不会把密钥写进任何文件或日志（打印时只显示前 6 位 + 长度）。

param(
    [switch]$Boot,
    [switch]$Jar
)

$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $MyInvocation.MyCommand.Path
$envFile = Join-Path $root 'ai.env'

if (-not (Test-Path $envFile)) {
    Write-Host "[AI] 没找到 ai.env，将按当前环境变量启动（可能处于模板模式）。" -ForegroundColor Yellow
    Write-Host "     先执行：  Copy-Item ai.env.example ai.env   然后填 AI_LLM_KEY" -ForegroundColor Yellow
} else {
    $loaded = @()
    Get-Content $envFile -Encoding UTF8 | ForEach-Object {
        if ($_ -match '^\s*#' -or $_ -notmatch '^\s*([A-Za-z_][A-Za-z0-9_]*)\s*=\s*(.*)$') { return }
        $name = $Matches[1]
        $val = $Matches[2].Trim().Trim('"').Trim("'")
        if ($val -eq '') { return }
        [Environment]::SetEnvironmentVariable($name, $val, 'Process')
        $loaded += $name
    }
    Write-Host "[AI] 已从 ai.env 注入 $($loaded.Count) 个变量：" -ForegroundColor Green
    foreach ($n in $loaded) {
        $v = [Environment]::GetEnvironmentVariable($n, 'Process')
        $shown = if ($v.Length -le 6) { '***' } else { $v.Substring(0, 6) + '…(' + $v.Length + ')' }
        Write-Host ("     {0,-24} {1}" -f $n, $shown)
    }
}

if ($env:AI_LLM_KEY -or $env:AI_DASHSCOPE_KEY) {
    Write-Host "[AI] 直连通道：READY（模型 $($env:AI_LLM_MODEL)$($env:AI_DASHSCOPE_MODEL)）" -ForegroundColor Green
} else {
    Write-Host "[AI] 直连通道：未配置 → AI 会回退到规则模板（不是故障，是降级）" -ForegroundColor Yellow
}

$jar = $null
if (-not $Boot) {
    $jar = Get-ChildItem (Join-Path $root 'build\libs\*.jar') -ErrorAction SilentlyContinue |
        Where-Object { $_.Name -notmatch 'plain' } | Select-Object -First 1
}

if ($jar -and -not $Boot) {
    Write-Host "[启动] java -jar $($jar.Name)" -ForegroundColor Cyan
    & java -jar $jar.FullName
} else {
    Write-Host "[启动] .\gradlew.bat bootRun" -ForegroundColor Cyan
    & (Join-Path $root 'gradlew.bat') bootRun
}
