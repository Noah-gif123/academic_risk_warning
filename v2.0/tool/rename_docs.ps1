# 把三份辅助文档统一改名为新项目名（文件名 + 正文标题 + 生成源 JSON/脚本）
# 说明：脚本只用码点拼中文，避免 Windows PowerShell 5.1 按 ANSI 读脚本导致中文乱码
$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.IO.Compression.FileSystem

$OLD = -join ([char]0x5B66, [char]0x60C5, [char]0x9884, [char]0x8B66, [char]0x7CFB, [char]0x7EDF)   # 学情预警系统
$PRE = (-join ([char]0x57FA, [char]0x4E8E)) + 'RAG' + (-join ([char]0x4E0E, [char]0x591A, [char]0x667A, [char]0x80FD, [char]0x4F53, [char]0x7684))  # 基于RAG与多智能体的
$NEW = $PRE + $OLD                                                                                  # 基于RAG与多智能体的学情预警系统

$word = 'E:\JavaDEVIDEA\workspace\JavaProject\academic_risk_warning\v2.0\word'
$tool = 'E:\JavaDEVIDEA\workspace\JavaProject\academic_risk_warning\v2.0\tool'

# 1) 改 docx 内部文本（document.xml 以及页眉页脚）
function Rename-DocxText($path) {
  $zip = [IO.Compression.ZipFile]::Open($path, 'Update')
  try {
    $parts = @($zip.Entries | Where-Object { $_.FullName -match '^word/(document|header\d*|footer\d*)\.xml$' })
    $changed = 0
    foreach ($entry in $parts) {
      $sr = New-Object IO.StreamReader($entry.Open(), [Text.Encoding]::UTF8)
      $xml = $sr.ReadToEnd(); $sr.Close()
      if ($xml.IndexOf($NEW) -ge 0) { continue }
      if ($xml.IndexOf($OLD) -lt 0) { continue }
      $xml2 = $xml.Replace($OLD, $NEW)
      $name = $entry.FullName
      $entry.Delete()
      $newEntry = $zip.CreateEntry($name)
      $sw = New-Object IO.StreamWriter($newEntry.Open(), (New-Object Text.UTF8Encoding($false)))
      $sw.Write($xml2); $sw.Close()
      $changed++
    }
    return $changed
  } finally { $zip.Dispose() }
}

$map = @(
  @{ from = ${OLD} + '_AI功能点击验收手册.docx'; to = ${NEW} + '_AI功能点击验收手册.docx' },
  @{ from = ${OLD} + '_问题汇总.docx'; to = ${NEW} + '_问题汇总.docx' },
  @{ from = ${OLD} + '_Docker部署合规差异记录.docx'; to = ${NEW} + '_Docker部署合规差异记录.docx' }
)

Set-Location 'E:\JavaDEVIDEA\workspace\JavaProject\academic_risk_warning'
foreach ($m in $map) {
  $oldPath = Join-Path $word $m.from
  if (-not (Test-Path $oldPath)) { Write-Output "SKIP missing: $($m.from)"; continue }
  $n = Rename-DocxText $oldPath
  & git mv -- $oldPath (Join-Path $word $m.to)
  Write-Output ("RENAMED {0} -> {1} (text parts changed: {2})" -f $m.from, $m.to, $n)
}

# 2) 同步生成源（JSON / JS），保证以后重建也是新名
foreach ($f in @('ai_click_guide.json', 'docker_diff.json', 'ai_test_guide.json', 'build_issue_summary.js')) {
  $p = Join-Path $tool $f
  if (-not (Test-Path $p)) { continue }
  $c = [IO.File]::ReadAllText($p, [Text.Encoding]::UTF8)
  if ($c.IndexOf($OLD) -lt 0) { Write-Output "SRC skip (no old name): $f"; continue }
  if ($c.IndexOf($NEW) -ge 0) { Write-Output "SRC skip (already new): $f"; continue }
  $c2 = $c.Replace($OLD, $NEW)
  [IO.File]::WriteAllText($p, $c2, (New-Object Text.UTF8Encoding($false)))
  Write-Output "SRC updated: $f"
}
Write-Output 'DONE'
