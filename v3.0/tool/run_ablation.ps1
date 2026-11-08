<#
.SYNOPSIS
  C1/C2 判定引擎消融：修前（v2.0 演示库）vs 修后（v3.0 独立库），输出 JSON 证据。

.DESCRIPTION
  P0-7：编译并运行 v3.0/tool/ablation/AblationToolV3.java。
  - C1：逐个关闭 C 通道因子（课程类型 / 历史挂科率 / 教师尺度 / 个人先验），统计等级变化率；
  - C2：阈值单变量扫描（±3 / ±5 分）。
  修前 = study_warning_system_v2（cfr 全 NULL、λ 全 0.620）；
  修后 = study_warning_system_v3（cfr 36/36 初始化、λ=n/(n+k)、2 例熔断）。
  产物写到 v3.0/evidence/ablation_v2.json 与 ablation_v3.json。

.EXAMPLE
  powershell -NoProfile -ExecutionPolicy Bypass -File v3.0/tool/run_ablation.ps1
#>
param(
    [int]$Term = 5,
    [string]$DbBefore = 'study_warning_system_v2',
    [string]$DbAfter = 'study_warning_system_v3'
)

$ErrorActionPreference = 'Stop'
$v3Dir = Split-Path -Parent $PSScriptRoot
$root = Split-Path -Parent $v3Dir
$backend = Join-Path $root 'v2.0\backend'
$src = Join-Path $v3Dir 'tool\ablation\AblationToolV3.java'
$outDir = Join-Path $v3Dir 'tool\ablation\classes'
$evidence = Join-Path $v3Dir 'evidence'
if (-not (Test-Path -LiteralPath $outDir)) { New-Item -ItemType Directory -Path $outDir -Force | Out-Null }
if (-not (Test-Path -LiteralPath $evidence)) { New-Item -ItemType Directory -Path $evidence -Force | Out-Null }

Write-Host "== 编译 AblationToolV3"
& javac '-encoding' 'UTF-8' '-cp' (Join-Path $backend 'build-verify') '-d' $outDir $src
if ($LASTEXITCODE -ne 0) { throw 'AblationToolV3 编译失败' }

$mysqlJar = Join-Path $backend 'build-verify-lib\mysql-connector-j-9.2.0.jar'
$cp = (Join-Path $backend 'build-verify') + ';' + $outDir + ';' + $mysqlJar

function Run-Ablation([string]$db, [string]$label, [string]$out) {
    Write-Host ("== 消融：" + $label + "（" + $db + "）")
    & java -cp $cp AblationToolV3 ("--db=" + $db) ("--term=" + $Term) ("--label=" + $label) ("--out=" + $out)
    if ($LASTEXITCODE -ne 0) { throw ("消融失败：" + $label) }
}

Run-Ablation $DbBefore '修前 v2.0' (Join-Path $evidence 'ablation_v2.json')
Run-Ablation $DbAfter '修后 v3.0' (Join-Path $evidence 'ablation_v3.json')

Write-Host ""
Write-Host "== 完成：修前/修后 JSON 已写入 v3.0/evidence/" -ForegroundColor Green
Write-Host "   目标（PLAN v3.0 一、总体验收）：教师松紧 1.5–3%、课程类型 2–4%、历史挂科率 1–2%"
