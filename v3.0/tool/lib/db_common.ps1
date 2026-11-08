# v3.0 公共库：MySQL 命令行封装 + 快照清单读写
# 用途：snapshot_v2.0.ps1（生成快照）/ rollback_v2.0.ps1（回滚与恢复演练）共用。
# 约定：
#   1. 密码通过子进程环境变量 MYSQL_PWD 传递，不写进命令行；
#   2. 所有函数只操作指定库与文件，不改全局配置；
#   3. 本文件含中文，必须保存为 UTF-8 带 BOM（PS 5.1 否则按 ANSI 读会乱码）。

$script:MySqlBinDefault = 'C:\Program Files\MySQL\MySQL Server 8.0\bin'

function Assert-PathExists {
    param([string]$Path, [string]$What)
    if (-not (Test-Path -LiteralPath $Path)) { throw ("找不到{0}：{1}" -f $What, $Path) }
}

# 创建连接上下文；同时把密码写进 MYSQL_PWD
function New-DbContext {
    param(
        [string]$Server = '127.0.0.1',
        [int]$Port = 3306,
        [string]$Db = 'study_warning_system_v2',
        [string]$User = 'root',
        [string]$Password = 'root',
        [string]$MySql = '',
        [string]$MySqlDump = ''
    )
    if (-not $MySql) { $MySql = Join-Path $script:MySqlBinDefault 'mysql.exe' }
    if (-not $MySqlDump) { $MySqlDump = Join-Path $script:MySqlBinDefault 'mysqldump.exe' }
    Assert-PathExists $MySql 'mysql 客户端'
    Assert-PathExists $MySqlDump 'mysqldump'
    $env:MYSQL_PWD = $Password
    return [pscustomobject]@{
        Server    = $Server
        Port      = $Port
        Db        = $Db
        User      = $User
        MySql     = $MySql
        MySqlDump = $MySqlDump
    }
}

# 执行 SQL 并返回文本行（mysql -N -B，制表符分隔）；不指定 Database 时不带库名
function Invoke-DbQuery {
    param(
        [Parameter(Mandatory = $true)] $Ctx,
        [Parameter(Mandatory = $true)] [string]$Sql,
        [string]$Database = ''
    )
    $callArgs = @(
        "--host=$($Ctx.Server)", "--port=$($Ctx.Port)", "--user=$($Ctx.User)",
        '--default-character-set=utf8mb4', '-N', '-B', "--execute=$Sql"
    )
    if ($Database) { $callArgs += $Database }
    $output = & $Ctx.MySql @callArgs 2>&1
    if ($LASTEXITCODE -ne 0) {
        $text = (($output | ForEach-Object { [string]$_ }) -join "`n")
        throw ("SQL 执行失败（exit=$LASTEXITCODE）：$text")
    }
    # 过滤 mysql 客户端写进 stderr 的提示行（如 "mysql: [Warning] ..."）
    return @($output | ForEach-Object { [string]$_ } | Where-Object { $_ -notmatch '^mysql:' })
}

function Get-TableNames {
    param($Ctx, [string]$Database = '')
    if (-not $Database) { $Database = $Ctx.Db }
    $sql = "select table_name from information_schema.tables where table_schema='$Database' and table_type='BASE TABLE' order by table_name"
    $rows = Invoke-DbQuery -Ctx $Ctx -Sql $sql
    return @($rows | Where-Object { $_.Trim() -ne '' } | ForEach-Object { $_.Trim() })
}

# 逐表精确行数（count(*)，不用 information_schema 的近似值）
function Get-TableRowCounts {
    param($Ctx, [string]$Database, [string[]]$Tables)
    $parts = foreach ($t in $Tables) {
        "select '{0}' as tbl, count(*) as n from ``{0}``" -f $t
    }
    $sql = $parts -join ' union all '
    $map = [ordered]@{}
    foreach ($line in (Invoke-DbQuery -Ctx $Ctx -Sql $sql -Database $Database)) {
        if ($line.Trim() -eq '') { continue }
        $cols = $line -split "`t"
        if ($cols.Count -lt 2) { continue }
        $map[$cols[0]] = [long]$cols[1]
    }
    return $map
}

# 快照校验口径：风险等级/通道分布 + 关键表行数与关键参数
# 全部转成字符串存清单，避免浮点格式差异导致误判
function Get-KeyChecks {
    param($Ctx, [string]$Database = '')
    if (-not $Database) { $Database = $Ctx.Db }
    $checks = [ordered]@{}

    $groupSpecs = @(
        @{ Prefix = 'risk.level.';   Sql = 'select risk_level, count(*) from risk_state group by risk_level order by risk_level' },
        @{ Prefix = 'risk.channel.'; Sql = 'select active_channel, count(*) from risk_state group by active_channel order by active_channel' }
    )
    foreach ($spec in $groupSpecs) {
        foreach ($line in (Invoke-DbQuery -Ctx $Ctx -Sql $spec.Sql -Database $Database)) {
            if ($line.Trim() -eq '') { continue }
            $cols = $line -split "`t"
            if ($cols.Count -lt 2) { continue }
            $checks[($spec.Prefix + $cols[0])] = [string]$cols[1]
        }
    }

    $single = [ordered]@{
        'students'          = 'select count(*) from student'
        'teachers'          = 'select count(*) from teacher'
        'courses'           = 'select count(*) from course'
        'teachingClasses'   = 'select count(*) from teaching_class'
        'enrollments'       = 'select count(*) from enrollment'
        'riskStates'        = 'select count(*) from risk_state'
        'alerts'            = 'select count(*) from alert_record'
        'cfr.rows'          = 'select count(*) from course_fail_rate_history'
        'cfr.factorNull'    = 'select count(*) from course_fail_rate_history where course_factor is null'
        'cfr.uninitialized' = 'select count(*) from course_fail_rate_history where is_initialized = 0'
        'ts.rows'           = 'select count(*) from teacher_style'
        'ts.lambdaMin'      = 'select cast(round(min(shrink_lambda), 3) as char) from teacher_style'
        'ts.lambdaMax'      = 'select cast(round(max(shrink_lambda), 3) as char) from teacher_style'
        'ts.sampleMin'      = 'select cast(min(sample_size) as char) from teacher_style'
        'ts.sampleMax'      = 'select cast(max(sample_size) as char) from teacher_style'
    }
    foreach ($k in $single.Keys) {
        $v = @(Invoke-DbQuery -Ctx $Ctx -Sql $single[$k] -Database $Database) | Select-Object -First 1
        $checks[$k] = [string]$v
    }

    # ai 表可能按需关闭，存在才统计
    $hasAi = @(Invoke-DbQuery -Ctx $Ctx -Database $Database -Sql "select count(*) from information_schema.tables where table_schema='$Database' and table_name='ai_run'") | Select-Object -First 1
    if ([string]$hasAi -eq '1') {
        $checks['aiRuns'] = [string](@(Invoke-DbQuery -Ctx $Ctx -Sql 'select count(*) from ai_run' -Database $Database) | Select-Object -First 1)
    }

    return $checks
}

# 生成快照文件（mysqldump；不含 CREATE DATABASE/USE，便于导入到任意库名）
function Invoke-DbDump {
    param($Ctx, [string]$Database, [string]$ResultFile)
    $dumpArgs = @(
        "--host=$($Ctx.Server)", "--port=$($Ctx.Port)", "--user=$($Ctx.User)",
        '--single-transaction', '--skip-lock-tables', '--no-tablespaces',
        '--set-gtid-purged=OFF', '--default-character-set=utf8mb4', '--hex-blob',
        '--triggers', "--result-file=$ResultFile", $Database
    )
    $output = & $Ctx.MySqlDump @dumpArgs 2>&1
    if ($LASTEXITCODE -ne 0) {
        $text = (($output | ForEach-Object { [string]$_ }) -join "`n")
        throw ("mysqldump 失败（exit=$LASTEXITCODE）：$text")
    }
}

# 导入 SQL 文件（用 cmd 重定向，避免 PS 5.1 管道对中文/二进制的编码破坏）
function Import-SqlFile {
    param($Ctx, [string]$Database, [string]$File)
    Assert-PathExists $File 'SQL 文件'
    $cmdLine = '"' + $Ctx.MySql + '" --host=' + $Ctx.Server + ' --port=' + $Ctx.Port +
               ' --user=' + $Ctx.User + ' --default-character-set=utf8mb4 --max-allowed-packet=256M ' +
               $Database + ' < "' + $File + '"'
    $output = cmd.exe /c $cmdLine 2>&1
    if ($LASTEXITCODE -ne 0) {
        $text = (($output | ForEach-Object { [string]$_ }) -join "`n")
        throw ("导入失败（exit=$LASTEXITCODE）：$text")
    }
}

function New-Database {
    param($Ctx, [string]$Database)
    Invoke-DbQuery -Ctx $Ctx -Sql ("create database if not exists ``$Database`` default character set utf8mb4 collate utf8mb4_0900_ai_ci") | Out-Null
}

function Remove-Database {
    param($Ctx, [string]$Database)
    Invoke-DbQuery -Ctx $Ctx -Sql ("drop database if exists ``$Database``") | Out-Null
}

function Get-Sha256 {
    param([string]$Path)
    return (Get-FileHash -LiteralPath $Path -Algorithm SHA256).Hash.ToLowerInvariant()
}

function Read-Manifest {
    param([string]$Path)
    Assert-PathExists $Path '快照清单'
    return (Get-Content -LiteralPath $Path -Raw -Encoding UTF8 | ConvertFrom-Json)
}

function Write-JsonFile {
    param([string]$Path, $Object)
    $json = $Object | ConvertTo-Json -Depth 8
    [IO.File]::WriteAllText($Path, $json, (New-Object System.Text.UTF8Encoding($false)))
}

# 比对「清单期望」与「实际恢复库」；返回差异列表（空 = 全绿）
function Compare-SnapshotState {
    param($Manifest, $ActualCounts, $ActualChecks)
    $diffs = New-Object System.Collections.Generic.List[string]

    $expTables = @{}
    foreach ($p in $Manifest.tables.PSObject.Properties) { $expTables[$p.Name] = [long]$p.Value }
    if ($expTables.Count -ne $ActualCounts.Count) {
        $diffs.Add("表数量不一致：清单=$($expTables.Count) 实际=$($ActualCounts.Count)")
    }
    foreach ($name in $expTables.Keys) {
        if (-not $ActualCounts.Contains($name)) { $diffs.Add("缺少表：$name"); continue }
        if ([long]$ActualCounts[$name] -ne [long]$expTables[$name]) {
            $diffs.Add("表 $name 行数不一致：清单=$($expTables[$name]) 实际=$($ActualCounts[$name])")
        }
    }
    foreach ($name in $ActualCounts.Keys) {
        if (-not $expTables.ContainsKey($name)) { $diffs.Add("多出表：$name") }
    }

    $expChecks = @{}
    foreach ($p in $Manifest.keyChecks.PSObject.Properties) { $expChecks[$p.Name] = [string]$p.Value }
    foreach ($k in $expChecks.Keys) {
        $actual = if ($ActualChecks.Contains($k)) { [string]$ActualChecks[$k] } else { '<缺失>' }
        if ($actual -ne $expChecks[$k]) {
            $diffs.Add("口径 $k 不一致：清单=$($expChecks[$k]) 实际=$actual")
        }
    }
    return , $diffs
}
