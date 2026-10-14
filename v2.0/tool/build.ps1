param(
  [string]$Only = "*",
  [switch]$Png,
  [int]$Scale = 2
)
$ErrorActionPreference = 'Continue'
$gen = Split-Path -Parent $MyInvocation.MyCommand.Path
$specDir = Join-Path $gen 'spec'
$outDir = Join-Path $gen 'out'
New-Item -ItemType Directory -Force -Path $outDir | Out-Null
$utf8 = New-Object System.Text.UTF8Encoding($false)
$chrome = 'C:\Program Files\Google\Chrome\Application\chrome.exe'
$ud = Join-Path $env:TEMP 'dsh-chrome-diagrams'

$specs = Get-ChildItem (Join-Path $specDir ($Only + '.json')) | Sort-Object Name
if ($specs.Count -eq 0) { throw "no specs matched $Only" }

foreach ($sp in $specs) {
  $name = [System.IO.Path]::GetFileNameWithoutExtension($sp.Name)
  $svgFile = Join-Path $outDir ($name + '.svg')
  $drwFile = Join-Path $outDir ($name + '.drawio')
  $htmlFile = Join-Path $outDir ($name + '.html')
  $pngFile = Join-Path $outDir ($name + '.png')

  & node (Join-Path $gen 'render.js') $sp.FullName $svgFile $drwFile | Out-Null
  if ($LASTEXITCODE -ne 0) { throw "render.js failed for $name" }
  if (-not (Test-Path $svgFile)) { throw "svg missing: $svgFile" }

  $j = [System.IO.File]::ReadAllText($sp.FullName, [System.Text.Encoding]::UTF8) | ConvertFrom-Json
  $w = [int]$j.w; $h = [int]$j.h
  $svgText = [System.IO.File]::ReadAllText($svgFile, [System.Text.Encoding]::UTF8)
  $page = '<!DOCTYPE html><html><head><meta charset="utf-8"><style>html,body{margin:0;padding:0;overflow:hidden;background:#fff}</style></head><body>' + $svgText + '</body></html>'
  [System.IO.File]::WriteAllText($htmlFile, $page, $utf8)

  if ($Png) {
    if (Test-Path $pngFile) { Remove-Item $pngFile -Force }
    $uri = 'file:///' + ($htmlFile -replace '\\', '/')
    & $chrome --headless=new --no-sandbox --disable-gpu --disable-breakpad --disable-crash-reporter `
      --no-first-run --disable-extensions --hide-scrollbars --user-data-dir="$ud" `
      --force-device-scale-factor=$Scale --window-size=$w,$h --screenshot="$pngFile" $uri 2>$null | Out-Null
    if (Test-Path $pngFile) {
      $sz = (Get-Item $pngFile).Length
      Write-Output ("{0,-26} {1,4}x{2,-4} png={3}" -f $name, $w, $h, $sz)
    } else { Write-Output ("{0,-26} PNG FAILED" -f $name) }
  } else {
    Write-Output ("{0,-26} svg ok" -f $name)
  }
}
