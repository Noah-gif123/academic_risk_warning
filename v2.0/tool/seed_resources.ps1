# 资料库演示数据：同一课程组两位老师各上传一份，互相可见（课程组共享）
param([string]$Base = 'http://127.0.0.1:18080')
$ErrorActionPreference='Continue'
$base = $Base.TrimEnd('/') + '/api'
function Post-Json($url,$obj,$token){$bytes=[Text.Encoding]::UTF8.GetBytes(($obj|ConvertTo-Json -Depth 6 -Compress));$h=@{};if($token){$h['Authorization']="Bearer $token"};$r=Invoke-WebRequest -Uri $url -Method Post -Body $bytes -ContentType 'application/json; charset=utf-8' -Headers $h -UseBasicParsing;return ([Text.Encoding]::UTF8.GetString($r.RawContentStream.ToArray())|ConvertFrom-Json)}
function Get-Json($url,$token){$h=@{};if($token){$h['Authorization']="Bearer $token"};$r=Invoke-WebRequest -Uri $url -Headers $h -UseBasicParsing;return ([Text.Encoding]::UTF8.GetString($r.RawContentStream.ToArray())|ConvertFrom-Json)}
function Login($no){$cap=(Get-Json "$base/auth/captcha").data;$code=(Get-Json "$base/auth/captcha/debug?captchaId=$($cap.captchaId)").data;$r=Post-Json "$base/auth/login" @{loginNo=$no;password='000000';captchaId=$cap.captchaId;captchaCode=$code} $null;return $r.data.token}
$t1=Login '1001'; $t6=Login '1006'
$tmp = Join-Path $env:TEMP 'demo_res'
New-Item -ItemType Directory -Force -Path $tmp | Out-Null
[IO.File]::WriteAllText((Join-Path $tmp '第1讲_课程说明与考核方式.txt'), "大学计算机基础（一）· 第1讲 课程说明`r`n1. 学分 2.0，12 周（12 次课）`r`n2. 总评 = 平时 40% + 期末考试 60%`r`n3. 平时 = 课堂测试 7×5 + 三级项目 20 + 课堂表现 5 + 课后作业 4×10`r`n4. 缺课超过 4 次按硬性不合格处理", (New-Object Text.UTF8Encoding($false)))
[IO.File]::WriteAllText((Join-Path $tmp 'Excel三级项目_选题与提交要求.txt'), "三级项目（个人）：Excel 数据处理、函数、透视表`r`n提交：以「学号+姓名」命名的 xlsx 文件`r`n选题不可与他人重复；第 10 周前提交", (New-Object Text.UTF8Encoding($false)))
[IO.File]::WriteAllText((Join-Path $tmp '期末复习提纲.txt'), "期末复习提纲（赵明整理）`r`n考试范围：第 1~12 讲；题型：选择 40% + 简答 30% + 综合 30%", (New-Object Text.UTF8Encoding($false)))
$f1 = Join-Path $tmp '第1讲_课程说明与考核方式.txt'
$f2 = Join-Path $tmp 'Excel三级项目_选题与提交要求.txt'
$f3 = Join-Path $tmp '期末复习提纲.txt'
& curl.exe -s -X POST "http://127.0.0.1:18080/api/teacher/resources/upload?teachingClassId=201" -H "Authorization: Bearer $t1" -F "file=@$f1" | Out-Null
& curl.exe -s -X POST "http://127.0.0.1:18080/api/teacher/resources/upload?teachingClassId=201" -H "Authorization: Bearer $t1" -F "file=@$f2" | Out-Null
& curl.exe -s -X POST "http://127.0.0.1:18080/api/teacher/resources/upload?teachingClassId=202" -H "Authorization: Bearer $t6" -F "file=@$f3" | Out-Null
"1001 视角（课程组 101）："
((Get-Json "$base/teacher/resources?offeringId=101" $t1).data) | ForEach-Object { "   {0}｜{1}｜{2}｜我的={3}" -f $_.fileName,$_.uploaderName,$_.classCode,$_.isMine }
"1006 视角（课程组 101）："
((Get-Json "$base/teacher/resources?offeringId=101" $t6).data) | ForEach-Object { "   {0}｜{1}｜{2}｜我的={3}" -f $_.fileName,$_.uploaderName,$_.classCode,$_.isMine }
Remove-Item $tmp -Recurse -Force -ErrorAction SilentlyContinue
