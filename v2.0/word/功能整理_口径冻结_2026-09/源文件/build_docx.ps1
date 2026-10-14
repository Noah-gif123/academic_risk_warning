# Build a .docx directly from a JSON content description (no Word needed).
# ASCII-only script; all Chinese text comes from the JSON file read as UTF-8.
$ErrorActionPreference = "Stop"

$root     = "E:\JavaDEVIDEA\workspace\JavaProject\academic_risk_warning"
$jsonPath = Join-Path $root ".tmp-doc\algo_doc.json"
$cfg      = [System.IO.File]::ReadAllText($jsonPath, [System.Text.Encoding]::UTF8) | ConvertFrom-Json
$outPath  = Join-Path $root $cfg.output

$FONT = "Microsoft YaHei"
$EAST = [char]0x5FAE + [char]0x8F6F + [char]0x96C5 + [char]0x9ED1   # wei ruan ya hei

function Esc([string]$s) {
  if ($null -eq $s) { return "" }
  return $s.Replace("&", "&amp;").Replace("<", "&lt;").Replace(">", "&gt;")
}

function Runs([string]$text, [int]$sz, [bool]$bold, [int]$color) {
  $rpr = '<w:rPr><w:rFonts w:ascii="' + $FONT + '" w:hAnsi="' + $FONT + '" w:eastAsia="' + $EAST + '"/>'
  if ($bold) { $rpr += '<w:b/>' }
  if ($color -ne 0) { $rpr += '<w:color w:val="' + ('{0:X6}' -f $color) + '"/>' }
  $rpr += '<w:sz w:val="' + $sz + '"/><w:szCs w:val="' + $sz + '"/></w:rPr>'
  return '<w:r>' + $rpr + '<w:t xml:space="preserve">' + (Esc $text) + '</w:t></w:r>'
}

function Para([string]$inner, [string]$ppr) {
  return '<w:p>' + $ppr + $inner + '</w:p>'
}

function PPr([int]$before, [int]$after, [int]$left, [int]$hang, [int]$line, [bool]$keepNext) {
  $s = '<w:pPr>'
  if ($keepNext) { $s += '<w:keepNext/>' }
  $s += '<w:spacing w:before="' + $before + '" w:after="' + $after + '"'
  if ($line -gt 0) { $s += ' w:line="' + $line + '" w:lineRule="auto"' }
  $s += '/>'
  if ($left -ne 0 -or $hang -ne 0) {
    $s += '<w:ind w:left="' + $left + '"'
    if ($hang -ne 0) { $s += ' w:hanging="' + $hang + '"' }
    $s += '/>'
  }
  $s += '</w:pPr>'
  return $s
}

$sb = New-Object System.Text.StringBuilder

foreach ($b in $cfg.blocks) {
  switch ($b.t) {
    "title" {
      [void]$sb.Append((Para (Runs $b.x 36 $true 0) (PPr 0 120 0 0 240 $false)))
    }
    "sub" {
      [void]$sb.Append((Para (Runs $b.x 18 $false 8421504) (PPr 0 240 0 0 240 $false)))
    }
    "h1" {
      [void]$sb.Append((Para (Runs $b.x 28 $true 0) (PPr 320 140 0 0 240 $true)))
    }
    "h2" {
      [void]$sb.Append((Para (Runs $b.x 24 $true 0) (PPr 240 100 0 0 240 $true)))
    }
    "h3" {
      [void]$sb.Append((Para (Runs $b.x 21 $true 0) (PPr 180 80 0 0 240 $true)))
    }
    "p" {
      [void]$sb.Append((Para (Runs $b.x 19 $false 0) (PPr 0 100 0 0 264 $false)))
    }
    "note" {
      [void]$sb.Append((Para (Runs $b.x 18 $false 3355443) (PPr 60 120 170 0 264 $false)))
    }
    "ul" {
      foreach ($it in $b.items) {
        $txt = [char]0x00B7 + " " + $it
        [void]$sb.Append((Para (Runs $txt 19 $false 0) (PPr 0 80 227 227 264 $false)))
      }
    }
    "ol" {
      $i = 0
      foreach ($it in $b.items) {
        $i++
        $txt = "$i. " + $it
        [void]$sb.Append((Para (Runs $txt 19 $false 0) (PPr 0 80 227 227 264 $false)))
      }
    }
    "table" {
      $cols = $b.head.Count
      $w = [int](10092 / $cols)
      $tbl = '<w:tbl><w:tblPr><w:tblW w:w="5000" w:type="pct"/><w:tblLayout w:type="autofit"/>'
      $tbl += '<w:tblBorders>'
      foreach ($side in @("top","left","bottom","right","insideH","insideV")) {
        $tbl += '<w:' + $side + ' w:val="single" w:sz="4" w:space="0" w:color="BFBFBF"/>'
      }
      $tbl += '</w:tblBorders><w:tblCellMar><w:top w:w="40" w:type="dxa"/><w:left w:w="80" w:type="dxa"/><w:bottom w:w="40" w:type="dxa"/><w:right w:w="80" w:type="dxa"/></w:tblCellMar></w:tblPr>'
      $grid = '<w:tblGrid>'
      for ($c = 0; $c -lt $cols; $c++) { $grid += '<w:gridCol w:w="' + $w + '"/>' }
      $grid += '</w:tblGrid>'
      $tbl += $grid

      # header row
      $tbl += '<w:tr><w:trPr><w:tblHeader/></w:trPr>'
      foreach ($h in $b.head) {
        $tc = '<w:tc><w:tcPr><w:shd w:val="clear" w:color="auto" w:fill="F2F2F2"/><w:vAlign w:val="center"/></w:tcPr>'
        $tc += (Para (Runs $h 17 $true 0) (PPr 20 20 0 0 240 $false))
        $tc += '</w:tc>'
        $tbl += $tc
      }
      $tbl += '</w:tr>'

      foreach ($row in $b.rows) {
        $tbl += '<w:tr>'
        foreach ($cell in $row) {
          $tc = '<w:tc><w:tcPr><w:vAlign w:val="center"/></w:tcPr>'
          $tc += (Para (Runs ([string]$cell) 17 $false 0) (PPr 20 20 0 0 240 $false))
          $tc += '</w:tc>'
          $tbl += $tc
        }
        $tbl += '</w:tr>'
      }
      $tbl += '</w:tbl>'
      # empty paragraph after table (Word needs a paragraph between adjacent tables)
      $tbl += (Para "" (PPr 0 60 0 0 240 $false))
      [void]$sb.Append($tbl)
    }
    default { }
  }
}

$sect = '<w:sectPr><w:pgSz w:w="11906" w:h="16838"/><w:pgMar w:top="907" w:right="907" w:bottom="907" w:left="907" w:header="0" w:footer="0" w:gutter="0"/></w:sectPr>'

$docXml = '<?xml version="1.0" encoding="UTF-8" standalone="yes"?>' +
          '<w:document xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main"><w:body>' +
          $sb.ToString() + $sect + '</w:body></w:document>'

$ctXml = '<?xml version="1.0" encoding="UTF-8" standalone="yes"?>' +
         '<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">' +
         '<Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>' +
         '<Default Extension="xml" ContentType="application/xml"/>' +
         '<Override PartName="/word/document.xml" ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.document.main+xml"/>' +
         '</Types>'

$relsXml = '<?xml version="1.0" encoding="UTF-8" standalone="yes"?>' +
           '<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">' +
           '<Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="word/document.xml"/>' +
           '</Relationships>'

# validate XML well-formedness before packaging
$null = [xml]$docXml
$null = [xml]$ctXml
$null = [xml]$relsXml

Add-Type -AssemblyName System.IO.Compression | Out-Null
Add-Type -AssemblyName System.IO.Compression.FileSystem | Out-Null

if (Test-Path -LiteralPath $outPath) { Remove-Item -LiteralPath $outPath -Force }
$zip = [System.IO.Compression.ZipFile]::Open($outPath, [System.IO.Compression.ZipArchiveMode]::Create)
$enc = New-Object System.Text.UTF8Encoding($false)
function AddEntry($zip, $name, $content, $enc) {
  $e = $zip.CreateEntry($name, [System.IO.Compression.CompressionLevel]::Optimal)
  $s = $e.Open()
  $bytes = $enc.GetBytes($content)
  $s.Write($bytes, 0, $bytes.Length)
  $s.Close()
}
AddEntry $zip "[Content_Types].xml" $ctXml $enc
AddEntry $zip "_rels/.rels" $relsXml $enc
AddEntry $zip "word/document.xml" $docXml $enc
$zip.Dispose()

$info = Get-Item -LiteralPath $outPath
$tables = ([regex]::Matches($docXml, '<w:tbl>')).Count
$paras  = ([regex]::Matches($docXml, '<w:p>')).Count
Write-Host ("SAVED : " + $outPath)
Write-Host ("SIZE  : {0} bytes" -f $info.Length)
Write-Host ("BLOCKS: {0}   TABLES: {1}   PARAGRAPHS: {2}" -f $cfg.blocks.Count, $tables, $paras)
