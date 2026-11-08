<#
.SYNOPSIS
  P2-13 期望值生成：用 SQL 生成 30 题的期望数字（不手写）。
#>
param(
    [string]$Db = 'study_warning_system_v3',
    [string]$Cases = '',
    [string]$Out = ''
)
$ErrorActionPreference = 'Stop'
. (Join-Path $PSScriptRoot 'lib\db_common.ps1')
$v3Dir = Split-Path -Parent $PSScriptRoot
if (-not $Cases) { $Cases = Join-Path $v3Dir 'eval\agent_cases_30.json' }
if (-not $Out) { $Out = Join-Path $v3Dir 'eval\agent_cases_expected.json' }
$ctx = New-DbContext -Db $Db
function One([string]$sql) { return @(Invoke-DbQuery -Ctx $ctx -Sql $sql -Database $Db | Select-Object -First 1) }
function Cols([string]$sql) { $arr = @((One $sql) -split "`t"); return ,$arr }

$class = Cols "select count(*), sum(risk_level='RED'), sum(risk_level='YELLOW'), round(sum(risk_level in ('RED','ORANGE','YELLOW'))*100.0/count(*),2) from risk_state where term_id=5 and is_void=0"
$trend = Cols "select count(distinct stat_date), round(sum(red+orange+yellow)*100.0/sum(student_count),2) from stat_daily_class where term_id=5 and stat_date=(select max(stat_date) from stat_daily_class where term_id=5)"
$student = Cols "select risk_level, risk_score from risk_state where student_id=1001 and term_id=5 and is_void=0 limit 1"
$inter = Cols "select count(*) from intervention_record where student_id=1001 and term_id=5 and is_void=0"
$compareRows = 10   # Diagnostician 计划 topN=10

$rawJson = Get-Content -LiteralPath $Cases -Raw -Encoding UTF8
$caseList = $rawJson | ConvertFrom-Json
$resultRows = @()
foreach ($c in $caseList) {
    $numbers = @()
    switch ($c.intent) {
        'CLASS_STATS' {
            $numbers = @(
                @{ metric = 'total'; value = [int]$class[0] },
                @{ metric = 'red'; value = [int]$class[1] },
                @{ metric = 'yellow'; value = [int]$class[2] },
                @{ metric = 'riskRatio'; value = [double]$class[3] }
            )
        }
        'TREND' {
            $numbers = @(
                @{ metric = 'points'; value = [int]$trend[0] },
                @{ metric = 'lastRiskRatio'; value = [double]$trend[1] }
            )
        }
        'COURSE_COMPARE' {
            $numbers = @(@{ metric = 'rows'; value = [int]$compareRows })
        }
        'STUDENT_RISK' {
            $numbers = @(
                @{ metric = 'riskLevel'; value = [string]$student[0] },
                @{ metric = 'riskScore'; value = [double]$student[1] }
            )
        }
        'INTERVENTION' {
            $numbers = @(@{ metric = 'interventions'; value = [int]$inter[0] })
        }
        default { $numbers = @() }
    }
    $resultRows += [pscustomobject]@{
        id = $c.id
        question = $c.question
        intent = $c.intent
        expectedTools = $c.tools
        expectedNumbers = $numbers
    }
}
[IO.File]::WriteAllText($Out, ($resultRows | ConvertTo-Json -Depth 8), (New-Object System.Text.UTF8Encoding($false)))
Write-Host ("期望值已生成：" + @($resultRows).Count + " 题 -> " + $Out)
Write-Host ("class=" + ($class -join '/') + " trend=" + ($trend -join '/') + " student=" + ($student -join '/') + " interventions=" + ($inter -join '/'))
