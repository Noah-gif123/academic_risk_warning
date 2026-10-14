param([Parameter(Mandatory = $true)][string]$Doc)
$ErrorActionPreference = 'Continue'
Add-Type -AssemblyName System.IO.Compression.FileSystem
$zip = [System.IO.Compression.ZipFile]::OpenRead($Doc)
$e = $zip.Entries | Where-Object { $_.FullName -eq 'word/document.xml' }
$sr = New-Object System.IO.StreamReader($e.Open(), [System.Text.Encoding]::UTF8)
$xml = $sr.ReadToEnd(); $sr.Close(); $zip.Dispose()
$t = $xml -replace '</w:p>', "`n" -replace '<[^>]+>', ''
$t = $t -replace '&amp;', '&' -replace '&lt;', '<' -replace '&gt;', '>' -replace '&quot;', '"' -replace '&apos;', "'"
$out = "E:\JavaDEVIDEA\workspace\JavaProject\academic_risk_warning\.tmp-doc\feature_text.txt"
[System.IO.File]::WriteAllText($out, $t, (New-Object System.Text.UTF8Encoding($false)))
# ASCII-only checks (\uXXXX escapes keep this file encoding-safe)
$c = { param($p) ([regex]::Matches($t, $p)).Count }
"tables=" + $c.Invoke('<w:tbl>') + "  chars=" + $t.Length
"one_third=" + $c.Invoke('\u4e09\u5206\u4e4b\u4e00') + "  one_half=" + $c.Invoke('\u4e8c\u5206\u4e4b\u4e00') + "  slash12=" + $c.Invoke('1/2')
"course_close=" + $c.Invoke('\u7ed3\u8bfe') + "  basic_setting=" + $c.Invoke('\u57fa\u7840\u8bbe\u7f6e')
"chapters:"
($t -split "`n") | Select-String -Pattern '^.{1,3}\u7ae0' | ForEach-Object { "  " + $_.Line.Trim() }
"sub3:"
($t -split "`n") | Select-String -Pattern '^3\.\d' | ForEach-Object { "  " + $_.Line.Trim() }
