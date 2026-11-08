param(
    [string]$Db = 'study_warning_system_v3',
    [long]$Term = 5,
    [long]$Seed = 20261108,
    [int]$Permutations = 5000,
    [string]$Out = ''
)
$ErrorActionPreference = 'Stop'
$v3Dir = Split-Path -Parent $PSScriptRoot
$root = Split-Path -Parent $v3Dir
$src = Join-Path $PSScriptRoot 'placebo\PlaceboTest.java'
$classes = Join-Path $PSScriptRoot 'classes'
$jar = Join-Path $root 'v2.0\backend\build-verify-lib\mysql-connector-j-9.2.0.jar'
$outFile = if ($Out) { $Out } else { Join-Path $v3Dir 'eval\placebo.json' }
if (-not (Test-Path -LiteralPath $classes)) { New-Item -ItemType Directory -Path $classes -Force | Out-Null }
& javac '-encoding' 'UTF-8' '-cp' $jar '-d' $classes $src
if ($LASTEXITCODE -ne 0) { throw 'PlaceboTest 编译失败' }
& java '-Dfile.encoding=UTF-8' -cp ($classes + ';' + $jar) PlaceboTest ("--db=" + $Db) ("--term=" + $Term) ("--seed=" + $Seed) ("--perm=" + $Permutations) ("--out=" + $outFile)
if ($LASTEXITCODE -ne 0) { throw "PlaceboTest 执行失败（exit=$LASTEXITCODE）" }
