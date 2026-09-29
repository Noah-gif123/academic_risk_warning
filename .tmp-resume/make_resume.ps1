# Build a one-page resume .docx with Word COM and verify page count with Word itself.
# ASCII-only script: all Chinese content comes from resume_items.json (read as UTF-8).
$ErrorActionPreference = "Stop"

$root     = "E:\JavaDEVIDEA\workspace\JavaProject\academic_risk_warning"
$jsonPath = Join-Path $root ".tmp-resume\resume_items.json"
$cfg      = [System.IO.File]::ReadAllText($jsonPath, [System.Text.Encoding]::UTF8) | ConvertFrom-Json
$outPath  = Join-Path $root $cfg.output
$allItems = @($cfg.items)

$FONT = "Microsoft YaHei"

function FS([double]$v, [double]$scale) { return [double]($v * $scale) }

function Build-Doc($word, $items, [double]$scale) {
  $doc = $word.Documents.Add()
  $ps = $doc.PageSetup
  $ps.TopMargin    = $word.CentimetersToPoints(1.2)
  $ps.BottomMargin = $word.CentimetersToPoints(1.0)
  $ps.LeftMargin   = $word.CentimetersToPoints(1.5)
  $ps.RightMargin  = $word.CentimetersToPoints(1.5)

  $sel = $word.Selection
  $sel.ParagraphFormat.LineSpacingRule = 0
  $sel.ParagraphFormat.SpaceBefore = 0
  $sel.ParagraphFormat.SpaceAfter = 0
  $first = $true

  foreach ($it in $items) {
    if (-not $first) { $sel.TypeParagraph() }
    $first = $false
    $sel.ParagraphFormat.LeftIndent = 0
    $sel.ParagraphFormat.FirstLineIndent = 0
    $sel.ParagraphFormat.SpaceBefore = 0
    $sel.ParagraphFormat.SpaceAfter = 0
    $sel.Font.Name = $FONT
    $sel.Font.NameFarEast = $FONT
    $sel.Font.Color = 0
    $sel.Font.Bold = 0
    $sel.Font.Italic = 0
    $sel.Font.Size = (FS 9 $scale)

    switch ($it.k) {
      "name"  { $sel.Font.Size = (FS 16 $scale);   $sel.Font.Bold = 1; $sel.ParagraphFormat.SpaceAfter = 1; $sel.TypeText($it.t) }
      "sub"   { $sel.Font.Size = (FS 10.5 $scale); $sel.Font.Bold = 1; $sel.ParagraphFormat.SpaceAfter = 2; $sel.TypeText($it.t) }
      "meta"  { $sel.Font.Size = (FS 8 $scale);    $sel.Font.Color = 8421504; $sel.TypeText($it.t) }
      "h"     { $sel.Font.Size = (FS 11 $scale);   $sel.Font.Bold = 1; $sel.ParagraphFormat.SpaceBefore = 5; $sel.ParagraphFormat.SpaceAfter = 1; $sel.TypeText($it.t) }
      "pt"    { $sel.Font.Size = (FS 10 $scale);   $sel.Font.Bold = 1; $sel.ParagraphFormat.SpaceBefore = 3; $sel.TypeText($it.t) }
      "plain" { $sel.TypeText($it.t) }
      "lab"   { $sel.Font.Bold = 1; $sel.TypeText($it.l); $sel.Font.Bold = 0; if ($it.t -ne "") { $sel.TypeText($it.t) } }
      "li"    { $sel.ParagraphFormat.LeftIndent = $word.CentimetersToPoints(0.42); $sel.ParagraphFormat.FirstLineIndent = $word.CentimetersToPoints(-0.42); $sel.TypeText(([char]0x00B7) + " " + $it.t) }
      "sk"    { $sel.TypeText($it.t) }
    }
  }
  return $doc
}

$word = New-Object -ComObject Word.Application
$word.Visible = $false
$word.DisplayAlerts = 0
try { $word.Options.CheckSpellingAsYouType = $false; $word.Options.CheckGrammarAsYouType = $false } catch {}

$saved = $false
try {
  $kept = New-Object System.Collections.ArrayList
  foreach ($i in $allItems) { [void]$kept.Add($i) }
  $dropped = @()

  # Stage 1: try full content, then drop by ascending priority group; Stage 2: shrink font.
  $plans = New-Object System.Collections.ArrayList
  [void]$plans.Add(@{ items = @($kept); scale = 1.0; label = "full" })
  foreach ($pLevel in @(1, 2, 3)) {
    $filtered = @($allItems | Where-Object { $_.p -ne $pLevel })
    $gone = @($allItems | Where-Object { $_.p -eq $pLevel } | ForEach-Object { $_.t.Substring(0, [Math]::Min(14, $_.t.Length)) })
    [void]$plans.Add(@{ items = $filtered; scale = 1.0; label = "drop P=$pLevel ($($gone -join ' / '))" })
  }
  foreach ($sc in @(0.95, 0.9, 0.85)) {
    $filtered = @($allItems | Where-Object { $_.p -ge 3 })
    [void]$plans.Add(@{ items = $filtered; scale = $sc; label = "drop P<3 + scale $sc" })
  }

  $chosen = $null
  foreach ($plan in $plans) {
    $doc = Build-Doc $word $plan.items $plan.scale
    $pages = $doc.ComputeStatistics(2)
    Write-Host ("plan [{0}] : items={1} pages={2}" -f $plan.label, $plan.items.Count, $pages)
    if ($pages -le 1) { $chosen = @{ doc = $doc; plan = $plan; pages = $pages }; break }
    $doc.Close(0)
  }
  if ($null -eq $chosen) { throw "cannot fit into one page even after trimming" }

  $doc = $chosen.doc
  $doc.SaveAs2($outPath, 16)
  $pagesFinal = $doc.ComputeStatistics(2)
  $chars = $doc.ComputeStatistics(3)
  $doc.Close(0)
  $saved = $true
  Write-Host ""
  Write-Host ("SAVED  : {0}" -f $outPath)
  Write-Host ("PLAN   : {0}" -f $chosen.plan.label)
  Write-Host ("PAGES  : {0}   CHARS: {1}" -f $pagesFinal, $chars)
}
finally {
  $word.Quit()
  [System.Runtime.InteropServices.Marshal]::ReleaseComObject($word) | Out-Null
  [GC]::Collect()
}
if (-not $saved) { throw "save failed" }
