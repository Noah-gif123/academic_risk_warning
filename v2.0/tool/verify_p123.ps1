# P1 / P2 / P3 验收：三类群体报告实际生成一次，结果写进 UTF-8 文件
# 注意：PS 5.1 的 Invoke-RestMethod 会把无 charset 的 JSON 按 Latin-1 解码（中文乱码），
# 所以这里统一用 Invoke-WebRequest + 手动 UTF-8 解码。
$ErrorActionPreference = 'Stop'
$b = 'http://127.0.0.1:8080'
$out = Join-Path $PSScriptRoot 'verify_p123.out.txt'

function Api($method, $path, $token, $body) {
  $h = @{}
  if ($token) { $h['Authorization'] = "Bearer $token" }
  $p = @{ Uri = "$b$path"; Method = $method; UseBasicParsing = $true }
  if ($h.Count) { $p['Headers'] = $h }
  if ($method -ne 'GET') {
    $p['ContentType'] = 'application/json'
    $p['Body'] = if ($body) { $body } else { '{}' }
  }
  $resp = Invoke-WebRequest @p
  $txt = [Text.Encoding]::UTF8.GetString($resp.RawContentStream.ToArray())
  return $txt | ConvertFrom-Json
}

function Login($no) {
  $body = (@{ loginNo = $no; password = '000000' } | ConvertTo-Json)
  $resp = Invoke-WebRequest -Uri "$b/api/auth/login" -Method POST -ContentType 'application/json' `
    -Body ([Text.Encoding]::UTF8.GetBytes($body)) -UseBasicParsing
  $txt = [Text.Encoding]::UTF8.GetString($resp.RawContentStream.ToArray())
  return ($txt | ConvertFrom-Json).data.token
}

$lines = New-Object System.Collections.Generic.List[string]
function W($s) { $lines.Add([string]$s) }

function HasSection($rep, $title) {
  return (@($rep.sections | Where-Object { $_.title -eq $title }).Count -gt 0)
}

# 段落名用码点拼，避免脚本被按 ANSI 读时中文字面量读坏（-eq 就永远不成立）
$T_EVID = -join ([char]0x53C2, [char]0x8003, [char]0x4F9D, [char]0x636E)                    # 参考依据
$T_CMP = -join ([char]0x6559, [char]0x5B66, [char]0x73ED, [char]0x5BF9, [char]0x6BD4)         # 教学班对比

function DumpReport($tag, $rep) {
  W "===== $tag ====="
  if ($null -eq $rep) { W "  (null)"; return }
  W "  title      : $($rep.title)"
  W "  scope      : $($rep.scope)  targetId=$($rep.targetId)"
  W "  source     : $($rep.source)   stale=$($rep.stale)"
  W "  sections   : $($rep.sections.Count)"
  foreach ($s in $rep.sections) {
    $t = [string]$s.text
    if ($t.Length -gt 240) { $t = $t.Substring(0, 240) + '...' }
    W "   - [$($s.title)] $t"
  }
}

# ---------- P3：教师课程维度（1004 名下 数据结构（Java）3 个教学班）----------
$t4 = Login '1004'
$before = Api 'GET' '/api/teacher/reports/offering/104/latest' $t4
W "P3 latest-before: $(if ($null -eq $before.data) { 'null' } else { $before.data.title })"
$sw = [Diagnostics.Stopwatch]::StartNew()
$p3 = Api 'POST' '/api/teacher/reports/offering/104' $t4
$sw.Stop()
W "P3 generate: $([math]::Round($sw.Elapsed.TotalSeconds,1))s"
DumpReport 'P3 课程报告 /teacher/reports/offering/104' $p3.data
$ev3 = HasSection $p3.data $T_EVID
$cmp3 = HasSection $p3.data $T_CMP
W "  P3 checks  : code=$($p3.code) term-in-title=$($p3.data.title -match '2026-2027') evidence=$ev3 class-compare=$cmp3"

# ---------- 权限：1001 不该能出 1004 名下课程的课程报告 ----------
$t1 = Login '1001'
$bad = Api 'POST' '/api/teacher/reports/offering/104' $t1
W "permission: code=$($bad.code) message=$($bad.message)"

# ---------- P2：教学班报告标题带学期 ----------
$p2 = Api 'POST' '/api/teacher/reports/class/201' $t1
DumpReport 'P2 教学班报告 /teacher/reports/class/201' $p2.data
$ev2 = HasSection $p2.data $T_EVID
W "  P2 checks  : term-in-title=$($p2.data.title -match '2026-2027') evidence=$ev2"

# ---------- P1 + P2：辅导员行政班报告带参考依据 ----------
$tc = Login 'C001'
$p1 = Api 'POST' '/api/counselor/reports/admin-class/1' $tc
DumpReport 'P1 行政班报告 /counselor/reports/admin-class/1' $p1.data
$ev1 = HasSection $p1.data $T_EVID
W "  P1 checks  : term-in-title=$($p1.data.title -match '2026-2027') evidence=$ev1"

$lines | Out-File -FilePath $out -Encoding utf8
Write-Output "written: $out ($($lines.Count) lines)"
