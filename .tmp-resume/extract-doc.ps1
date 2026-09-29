param([Parameter(Mandatory=$true)][string]$Path, [Parameter(Mandatory=$true)][string]$Out)

$ErrorActionPreference = 'Stop'
$b = [System.IO.File]::ReadAllBytes((Resolve-Path $Path).Path)

function Cvt([uint32]$v) { if ($v -eq 0xFFFFFFFF) { return -1 } else { return ($v -as [int]) } }
function U16([int]$o) { [BitConverter]::ToUInt16($b, $o) }
function S32([int]$o) { return (Cvt ([BitConverter]::ToUInt32($b, $o))) }
function S32A([byte[]]$a, [int]$o) { return (Cvt ([BitConverter]::ToUInt32($a, $o))) }
function U32A([byte[]]$a, [int]$o) { return [BitConverter]::ToUInt32($a, $o) }

# --- OLE2 header ---
$ssz  = [int](1 -shl (U16 30))
$mssz = [int](1 -shl (U16 32))
$dirStart = S32 48
$miniCutoff = (Cvt ([BitConverter]::ToUInt32($b, 56)))
$miniFatStart = S32 60
$difatStart = S32 68

function Off([int]$sector) { return 512 + $sector * $ssz }

# --- DIFAT -> FAT sector list ---
$difat = New-Object System.Collections.Generic.List[int]
for ($i = 0; $i -lt 109; $i++) {
  $s = S32 (76 + 4*$i)
  if ($s -ge 0) { $difat.Add($s) }
}
$next = $difatStart
$guard = 0
while ($next -ge 0 -and $guard -lt 5000) {
  $base = Off $next
  for ($i = 0; $i -lt ($ssz/4 - 1); $i++) {
    $s = S32 ($base + 4*$i)
    if ($s -ge 0) { $difat.Add($s) }
  }
  $next = S32 ($base + $ssz - 4)
  $guard++
}

# --- FAT ---
$fat = New-Object System.Collections.Generic.List[int]
foreach ($fs in $difat) {
  if ($fs -lt 0) { continue }
  $base = Off $fs
  for ($i = 0; $i -lt ($ssz/4); $i++) { $fat.Add((S32 ($base + 4*$i))) }
}

function Chain([int]$start) {
  $c = New-Object System.Collections.Generic.List[int]
  $cur = $start
  $g = 0
  while ($cur -ge 0 -and $cur -lt $fat.Count -and $g -lt 500000) { $c.Add($cur); $cur = $fat[$cur]; $g++ }
  return ,$c
}
function ReadChain([int]$start, [int]$size) {
  $ms = New-Object System.IO.MemoryStream
  foreach ($s in (Chain $start)) { $ms.Write($b, (Off $s), $ssz) }
  $arr = $ms.ToArray()
  if ($size -gt 0 -and $size -le $arr.Length) { return ,$arr[0..($size-1)] }
  return ,$arr
}

# --- Directory ---
$dirBytes = ReadChain $dirStart 0
$entries = @{}
$order = New-Object System.Collections.Generic.List[object]
for ($o = 0; $o -lt $dirBytes.Length; $o += 128) {
  $nameLen = [BitConverter]::ToUInt16($dirBytes, $o + 64)
  if ($nameLen -le 2 -or $nameLen -gt 64) { continue }
  $name = [System.Text.Encoding]::Unicode.GetString($dirBytes, $o, $nameLen - 2)
  $type = $dirBytes[$o + 66]
  $e = [pscustomobject]@{ Name=$name; Type=$type; Start=(S32A $dirBytes ($o+116)); Size=(S32A $dirBytes ($o+120)) }
  $order.Add($e)
  if (-not $entries.ContainsKey($name)) { $entries[$name] = $e }
}
Write-Host ("streams: " + (($order | ForEach-Object { "$($_.Name)[t$($_.Type),$($_.Size)]" }) -join ' '))

if (-not $entries.ContainsKey('WordDocument')) { throw "no WordDocument stream" }

# --- mini stream ---
$root = $order | Where-Object { $_.Type -eq 5 } | Select-Object -First 1
$miniStream = ReadChain $root.Start 0
$miniFatBytes = ReadChain $miniFatStart 0
$miniFat = New-Object System.Collections.Generic.List[int]
for ($i = 0; $i -lt [int]($miniFatBytes.Length/4); $i++) { $miniFat.Add((S32A $miniFatBytes (4*$i))) }

function ReadStream([string]$nm) {
  $e = $entries[$nm]
  if ($null -eq $e) { return $null }
  if ($e.Size -lt $miniCutoff) {
    $ms = New-Object System.IO.MemoryStream
    $cur = $e.Start
    $g = 0
    while ($cur -ge 0 -and $cur -lt $miniFat.Count -and $g -lt 500000) { $ms.Write($miniStream, $cur*$mssz, $mssz); $cur = $miniFat[$cur]; $g++ }
    $a = $ms.ToArray()
    if ($e.Size -le $a.Length) { return ,$a[0..($e.Size-1)] }
    return ,$a
  }
  return ReadChain $e.Start $e.Size
}

$wd = ReadStream 'WordDocument'
$fcMin = S32A $wd 24
$ccpText = S32A $wd 76
$fComplex = (([BitConverter]::ToUInt16($wd, 10)) -band 0x0004) -ne 0
$fcClx = S32A $wd 418
$lcbClx = S32A $wd 422
Write-Host "fComplex=$fComplex ccpText=$ccpText fcClx=$fcClx lcbClx=$lcbClx wdLen=$($wd.Length)"

$sb = New-Object System.Text.StringBuilder

if (-not $fComplex) {
  $len = $ccpText * 2
  if ($len -gt ($wd.Length - $fcMin)) { $len = $wd.Length - $fcMin }
  [void]$sb.Append([System.Text.Encoding]::Unicode.GetString($wd, $fcMin, $len))
} else {
  $tbl = ReadStream '1Table'
  if ($null -eq $tbl) { $tbl = ReadStream '0Table' }
  if ($null -eq $tbl) { throw 'complex file but no table stream' }
  Write-Host "tableLen=$($tbl.Length)"
  $p = $fcClx
  $end = [Math]::Min($fcClx + $lcbClx, $tbl.Length)
  while ($p -lt $end) {
    $t = $tbl[$p]
    if ($t -eq 1) {
      $cb = S32A $tbl ($p + 1)
      $p += 5 + $cb
    } elseif ($t -eq 2) {
      $lcb = S32A $tbl ($p + 1)
      $p += 5
      $n = [int](($lcb - 4) / 12)
      Write-Host "pieceCount=$n"
      $pcdBase = $p + 4*($n+1)
      for ($i = 0; $i -lt $n; $i++) {
        $cpStart = S32A $tbl ($p + 4*$i)
        $cpEnd   = S32A $tbl ($p + 4*($i+1))
        $fcRaw   = U32A $tbl ($pcdBase + 8*$i + 2)
        $compressed = ($fcRaw -band 0x40000000) -ne 0
        $fcReal = [int]($fcRaw -band 0x3FFFFFFF)
        $cch = $cpEnd - $cpStart
        if ($compressed) {
          $fcReal = [int]($fcReal / 2)
          if ($fcReal -ge 0 -and ($fcReal + $cch) -le $wd.Length) {
            [void]$sb.Append([System.Text.Encoding]::GetEncoding(1252).GetString($wd, $fcReal, $cch))
          } else { Write-Host "skip cp-compressed $fcReal $cch" }
        } else {
          if ($fcReal -ge 0 -and ($fcReal + $cch*2) -le $wd.Length) {
            [void]$sb.Append([System.Text.Encoding]::Unicode.GetString($wd, $fcReal, $cch*2))
          } else { Write-Host "skip cp-utf16 $fcReal $cch" }
        }
      }
      break
    } else { break }
  }
}

$text = $sb.ToString()
$text = $text -replace "`r", "`n" -replace "`a", '' -replace "`v", "`n" -replace "`f", "`n"
Set-Content -Path $Out -Value $text -Encoding UTF8
"chars=$($text.Length)"
