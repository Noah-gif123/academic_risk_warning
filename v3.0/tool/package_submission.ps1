<#
.SYNOPSIS
  生成传智杯源码提交包（干净目录 + zip）。
.DESCRIPTION
  把真实源码 v2.0/backend、v2.0/frontend 复制成提交包中的 backend/、frontend/；
  排除 node_modules、build、dist、ai.env、日志和私人资料。
.EXAMPLE
  powershell -NoProfile -ExecutionPolicy Bypass -File v3.0/tool/package_submission.ps1
  powershell -NoProfile -ExecutionPolicy Bypass -File v3.0/tool/package_submission.ps1 -Force
#>
param(
    [string]$OutDir = '',
    [switch]$NoZip,
    [switch]$Force,
    [switch]$IncludeLocalEnv
)

$ErrorActionPreference = 'Stop'
$v3Dir = Split-Path -Parent $PSScriptRoot
$root = Split-Path -Parent $v3Dir
if (-not $OutDir) { $OutDir = Join-Path $v3Dir 'release' }
$pkg = Join-Path $OutDir 'academic-risk-warning-v3.0'
$zip = Join-Path $OutDir $(if ($IncludeLocalEnv) { 'academic-risk-warning-v3.0-local-ai.zip' } else { 'academic-risk-warning-v3.0.zip' })

function Copy-Tree([string]$src, [string]$dst, [string[]]$excludeDirs, [string[]]$excludeFiles) {
    if (-not (Test-Path $src)) { throw "源目录不存在：$src" }
    New-Item -ItemType Directory -Force -Path $dst | Out-Null
    $roboArgs = @($src, $dst, '/E', '/NFL', '/NDL', '/NJH', '/NJS', '/NP')
    if ($excludeDirs.Count -gt 0) { $roboArgs += '/XD'; $roboArgs += $excludeDirs }
    if ($excludeFiles.Count -gt 0) { $roboArgs += '/XF'; $roboArgs += $excludeFiles }
    & robocopy @roboArgs | Out-Null
    if ($LASTEXITCODE -gt 7) { throw "复制失败：$src（robocopy exit=$LASTEXITCODE）" }
}
function Copy-One([string]$src, [string]$dst) {
    if (Test-Path $src) { Copy-Item $src $dst -Force }
}

if (Test-Path $pkg) {
    if (-not $Force) { throw "提交包已存在：$pkg。使用 -Force 覆盖。" }
    Remove-Item $pkg -Recurse -Force
}
New-Item -ItemType Directory -Force -Path $pkg | Out-Null

Write-Host '== 复制后端源码'
Copy-Tree (Join-Path $root 'v2.0\backend') (Join-Path $pkg 'backend') `
    @('build','build-verify','build-verify-lib','build-test','.gradle','.idea','out') `
    @('ai.env','*.log','hs_err_pid*','replay_pid*','javac*.args')

Write-Host '== 复制前端源码'
Copy-Tree (Join-Path $root 'v2.0\frontend') (Join-Path $pkg 'frontend') `
    @('node_modules','dist','.vite','.playwright-cli') @('*.local')

Write-Host '== 复制 SQL 与文档'
Copy-Tree (Join-Path $root 'v2.0\sql') (Join-Path $pkg 'sql') @() @()
Copy-Item (Join-Path $root 'v3.0\sql\snapshot_demo.sql') (Join-Path $pkg 'sql\snapshot_demo.sql') -Force
Copy-Tree (Join-Path $root 'v3.0\sql\seeds') (Join-Path $pkg 'sql\seeds') @() @()
New-Item -ItemType Directory -Force -Path (Join-Path $pkg 'sql\v3') | Out-Null
Get-ChildItem (Join-Path $root 'v3.0\sql') -File -Filter '*.sql' | Where-Object { $_.Name -notin @('snapshot_v3.sql','snapshot_demo.sql') } | ForEach-Object {
    Copy-Item $_.FullName (Join-Path $pkg 'sql\v3') -Force
}
New-Item -ItemType Directory -Force -Path (Join-Path $pkg 'docs') | Out-Null
Copy-One (Join-Path $root 'v3.0\docs\openapi.yaml') (Join-Path $pkg 'docs\openapi.yaml')
Copy-One (Join-Path $root 'v3.0\docs\prompt-history.md') (Join-Path $pkg 'docs\prompt-history.md')
Copy-One (Join-Path $root 'v3.0\docs\ablation.md') (Join-Path $pkg 'docs\ablation.md')
Copy-One (Join-Path $root 'v3.0\SUBMISSION_README.md') (Join-Path $pkg 'docs\README.md')
Copy-One (Join-Path $root 'v3.0\SUBMISSION_README.md') (Join-Path $pkg 'README.md')
Copy-One (Join-Path $root 'v3.0\.env.v3.example') (Join-Path $pkg '.env.v3.example')

# 过程可复现证据：评测、证据卡、Prompt/Dify 配置
Copy-Tree (Join-Path $root 'v3.0\eval') (Join-Path $pkg 'eval') @() @()
Copy-Tree (Join-Path $root 'v3.0\evidence') (Join-Path $pkg 'evidence') @() @()
Copy-Tree (Join-Path $root 'v3.0\p3') (Join-Path $pkg 'p3') @() @()

New-Item -ItemType Directory -Force -Path (Join-Path $pkg 'ai') | Out-Null
foreach ($f in @('rag-eval.md','report-workflow.md','README.md')) {
    Copy-One (Join-Path $root "v2.0\ai\$f") (Join-Path $pkg "ai\$f")
}

# 提交包的 compose 使用包内相对路径
$compose = Get-Content (Join-Path $root 'v3.0\docker-compose.demo.yml') -Raw -Encoding UTF8
$compose = $compose.Replace('../v2.0/backend', './backend').Replace('../v2.0/frontend', './frontend')
Set-Content -Path (Join-Path $pkg 'docker-compose.yml') -Value $compose -Encoding UTF8

if ($IncludeLocalEnv) {
    # 仅用于个人录制：把本机 ai.env 中的直连 Key 写入包内 .env；不要上传到公开仓库/公开网盘。
    $secretSource = Join-Path $root 'v2.0\backend\ai.env'
    if (-not (Test-Path $secretSource)) { throw '缺少 v2.0/backend/ai.env' }
    $secret = Get-Content $secretSource -Encoding UTF8 | Where-Object { $_ -match '^AI_LLM_(BASE_URL|KEY|MODEL)=' }
    if (-not ($secret -match '^AI_LLM_KEY=.')) { throw 'ai.env 缺少 AI_LLM_KEY' }
    $envLines = @('DB_ROOT_PASSWORD=root', 'AI_ENABLED=true') + $secret + @('CAPTCHA_DEBUG=false')
    Set-Content -Path (Join-Path $pkg '.env') -Value $envLines -Encoding UTF8
    Write-Host '== 已写入本地 AI Key（该 zip 只能本地/私有使用，禁止公开上传）' -ForegroundColor Yellow
}

$commit = (git -C $root rev-parse HEAD 2>$null)
if (-not $commit) { $commit = 'unknown' }
@(
    'academic-risk-warning v3.0 submission package',
    "generatedAt=$(Get-Date -Format o)",
    "gitCommit=$commit",
    'backend=v2.0/backend -> backend',
    'frontend=v2.0/frontend -> frontend',
    'database=study_warning_system_demo',
    'frontendPort=5174',
    'backendPort=18101',
    ('aiKeyIncluded=' + $(if ($IncludeLocalEnv) { 'true (LOCAL ONLY)' } else { 'false' }))
) | Set-Content -Path (Join-Path $pkg 'source-manifest.txt') -Encoding UTF8

Write-Host '== 检查包内容'
$files = Get-ChildItem $pkg -Recurse -File | Measure-Object
$size = (Get-ChildItem $pkg -Recurse -File | Measure-Object Length -Sum).Sum / 1MB
Write-Host ("   文件数={0} 大小={1:N1} MB" -f $files.Count, $size)
foreach ($must in @('backend\build.gradle','frontend\package.json','sql\snapshot_demo.sql','docker-compose.yml','README.md')) {
    if (-not (Test-Path (Join-Path $pkg $must))) { throw "提交包缺少：$must" }
}

if (-not $NoZip) {
    if (Test-Path $zip) { Remove-Item $zip -Force }
    Compress-Archive -Path $pkg -DestinationPath $zip -CompressionLevel Optimal
    Write-Host ("== 已生成：" + $zip)
} else {
    Write-Host ("== 已生成目录：" + $pkg)
}
Write-Host '完成'
