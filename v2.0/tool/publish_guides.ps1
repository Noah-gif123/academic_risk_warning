# 为 5 门课（12 个教学班）发布课程导学：成绩构成 + 各小项说明 + 纪律与提示
# 走真实接口 POST /api/teacher/guide（由各任课教师本人发布，publish=true）
param([string]$Base = 'http://127.0.0.1:18080')
$ErrorActionPreference = 'Stop'
$base = $Base.TrimEnd('/') + '/api'

function Post-Json($url, $obj, $token) {
  $bytes = [Text.Encoding]::UTF8.GetBytes(($obj | ConvertTo-Json -Depth 6 -Compress))
  $headers = @{}
  if ($token) { $headers['Authorization'] = "Bearer $token" }
  $resp = Invoke-WebRequest -Uri $url -Method Post -Body $bytes -ContentType 'application/json; charset=utf-8' -Headers $headers -UseBasicParsing
  return ([Text.Encoding]::UTF8.GetString($resp.RawContentStream.ToArray()) | ConvertFrom-Json)
}
function Get-Json($url, $token) {
  $headers = @{}
  if ($token) { $headers['Authorization'] = "Bearer $token" }
  $resp = Invoke-WebRequest -Uri $url -Headers $headers -UseBasicParsing
  return ([Text.Encoding]::UTF8.GetString($resp.RawContentStream.ToArray()) | ConvertFrom-Json)
}
function Login($no) {
  $cap = (Get-Json "$base/auth/captcha").data
  $code = (Get-Json "$base/auth/captcha/debug?captchaId=$($cap.captchaId)").data
  $r = Post-Json "$base/auth/login" @{ loginNo = $no; password = '000000'; captchaId = $cap.captchaId; captchaCode = $code } $null
  if ($r.code -ne 0) { throw "登录失败($no)：$($r.message)" }
  return $r.data.token
}

# ---------------- 各课程导学内容（HTML） ----------------
$GUIDE_A = @'
<h3>大学计算机基础（一）· 课程导学</h3>
<p>学分 <b>2.0</b>　周次 <b>12 周（12 次课）</b>　考核方式 <b>考试课</b>　及格线 60 分</p>

<h4>一、成绩构成（总评 100 分）</h4>
<p><b>总评 = 平时成绩 × 40% + 终结性考核（期末考试）× 60%</b></p>
<table>
  <tr><th>一级项</th><th>二级项</th><th>权重</th><th>说明</th></tr>
  <tr><td rowspan="4">平时成绩<br>40%</td><td>课堂测试</td><td>35</td><td>共 7 次、每次 5 分，随堂完成；缺考当次计 0 分</td></tr>
  <tr><td>三级项目</td><td>20</td><td>个人项目（Office 基础）：Excel 数据处理、函数、透视表等；提交以「学号 + 姓名」命名的 Excel 文件，题目不可重复</td></tr>
  <tr><td>课堂表现</td><td>5</td><td>出勤、提问与课堂参与</td></tr>
  <tr><td>课后作业</td><td>40</td><td>共 4 次、每次 10 分；逾期不补交</td></tr>
  <tr><td>终结性考核<br>60%</td><td>期末考试</td><td>60</td><td>卷面 100 分，按 60% 折算计入总评</td></tr>
</table>

<h4>二、考核与纪律</h4>
<ul>
  <li>及格线 60 分；<b>缺课超过允许上限（12 次课 → 允许缺 4 次）直接认定硬性不合格</b>，与卷面分无关。</li>
  <li>扣分（只影响平时分与预警结论）：每次缺课 −2 分、迟到 −1 分、请假超出 3 次后每次 −1 分。</li>
  <li>课堂测试、课后作业缺考或缺交按 0 分计；请假请提前在学习平台提交。</li>
</ul>

<h4>三、平时分怎么拿满</h4>
<ul>
  <li>课堂测试 7 次共 35 分，缺一次直接损失 5 分，尽量不缺席。</li>
  <li>三级项目占 20 分，建议第 3 周前定题；文件命名或格式不符会扣分。</li>
  <li>课后作业 4 次共 40 分，按次提交，逾期不补。</li>
</ul>

<h4>四、你会收到什么提示</h4>
<p>系统按考勤与成绩自动判定：缺课接近上限为黄色提醒、超过上限为红色；期末前会给出「期末需考分」，即按当前平时分测算的及格所需卷面分。</p>
'@

$GUIDE_B = @'
<h3>高等数学（一）· 课程导学</h3>
<p>学分 <b>4.0</b>　周次 <b>16 周（16 次课）</b>　考核方式 <b>考试课</b>　及格线 60 分</p>

<h4>一、成绩构成（总评 100 分）</h4>
<p><b>总评 = 平时成绩 × 40% + 终结性考核（期末考试）× 60%</b></p>
<table>
  <tr><th>一级项</th><th>二级项</th><th>权重</th><th>说明</th></tr>
  <tr><td rowspan="3">平时成绩<br>40%</td><td>课堂测试</td><td>30</td><td>共 6 次、每次 5 分，随堂小测；缺考当次计 0 分</td></tr>
  <tr><td>期中考试</td><td>20</td><td>第 8 周左右统一安排，卷面 100 分按 20 分折算</td></tr>
  <tr><td>课后作业</td><td>50</td><td>共 5 次、每次 10 分；逾期不补交</td></tr>
  <tr><td>终结性考核<br>60%</td><td>期末考试</td><td>60</td><td>卷面 100 分，按 60% 折算计入总评</td></tr>
</table>

<h4>二、考核与纪律</h4>
<ul>
  <li>及格线 60 分；<b>缺课超过允许上限（16 次课 → 允许缺 5 次）直接认定硬性不合格</b>。</li>
  <li>扣分：每次缺课 −2 分、迟到 −1 分、请假超出 3 次后每次 −1 分。</li>
  <li>期中考试缺考不安排补考，当次计 0 分；平时作业缺交同样计 0 分。</li>
</ul>

<h4>三、平时分怎么拿满</h4>
<ul>
  <li>课堂测试 6 次共 30 分，随堂进行，缺一次损失 5 分。</li>
  <li>课后作业 5 次共 50 分，是平时分的主体，建议当次课当天完成。</li>
  <li>期中考试占 20 分，范围覆盖前半学期内容，提前一周复习。</li>
</ul>

<h4>四、你会收到什么提示</h4>
<p>系统按考勤与成绩自动判定：缺课接近上限为黄色提醒、超过上限为红色；期末前会给出「期末需考分」，即按当前平时分测算的及格所需卷面分。</p>
'@

$GUIDE_C = @'
<h3>四史概述 · 课程导学</h3>
<p>学分 <b>1.0</b>　周次 <b>12 周（12 次课）</b>　考核方式 <b>考试课</b>　及格线 60 分</p>

<h4>一、成绩构成（总评 100 分）</h4>
<p><b>总评 = 平时成绩 × 60% + 终结性考核（期末考试）× 40%</b></p>
<table>
  <tr><th>一级项</th><th>二级项</th><th>权重</th><th>说明</th></tr>
  <tr><td rowspan="4">平时成绩<br>60%</td><td>课堂测试</td><td>35</td><td>共 7 次、每次 5 分，随堂完成；缺考当次计 0 分</td></tr>
  <tr><td>三级项目</td><td>20</td><td><b>团队项目</b>（4–6 人）：参观或走访历史遗址 / 相关场馆，收集口述、影像与文献材料，完成选题与资料整理，并做阶段汇报</td></tr>
  <tr><td>四级项目</td><td>40</td><td><b>团队项目</b>（同一小组）：在三级项目材料基础上完成 PPT 制作与课堂答辩，按内容、逻辑与表达打分</td></tr>
  <tr><td>课堂表现</td><td>5</td><td>出勤、发言与课堂参与</td></tr>
  <tr><td>终结性考核<br>40%</td><td>期末考试</td><td>40</td><td>卷面 100 分，按 40% 折算计入总评</td></tr>
</table>

<h4>二、考核与纪律</h4>
<ul>
  <li>及格线 60 分；<b>缺课超过允许上限（12 次课 → 允许缺 4 次）直接认定硬性不合格</b>。</li>
  <li>扣分：每次缺课 −2 分、迟到 −1 分、请假超出 3 次后每次 −1 分。</li>
  <li>团队项目按小组统一评分，组内贡献差异由组长在贡献表中说明（合计 100%）。</li>
</ul>

<h4>三、项目怎么做</h4>
<ul>
  <li>第 4 周前完成分组（4–6 人）并提交选题，避免与其它小组重复。</li>
  <li>三级项目重材料：参观/走访记录、照片、录音、文献摘录都要留档。</li>
  <li>四级项目重表达：PPT 控制在 10–12 页，答辩 8 分钟以内，组员分工陈述。</li>
</ul>

<h4>四、你会收到什么提示</h4>
<p>系统按考勤与成绩自动判定：缺课接近上限为黄色提醒、超过上限为红色；期末前会给出「期末需考分」。</p>
'@

$GUIDE_D = @'
<h3>数据结构（Java）· 课程导学</h3>
<p>学分 <b>2.0</b>　周次 <b>16 周（16 次课）</b>　考核方式 <b>考查课</b>　及格线 60 分</p>

<h4>一、成绩构成（总评 100 分）</h4>
<p><b>总评 = 平时成绩 × 40% + 终结性考核（大作业）× 60%</b></p>
<table>
  <tr><th>一级项</th><th>二级项</th><th>权重</th><th>说明</th></tr>
  <tr><td rowspan="3">平时成绩<br>40%</td><td>课堂测试</td><td>35</td><td>共 7 次、每次 5 分，随堂完成；缺考当次计 0 分</td></tr>
  <tr><td>三级项目</td><td>25</td><td><b>个人项目</b>：课上用一节课完成，按教师给定的数据库表结构与任务要求，编写对应需求的代码结构</td></tr>
  <tr><td>课后作业</td><td>40</td><td>共 4 次、每次 10 分；逾期不补交</td></tr>
  <tr><td>终结性考核<br>60%</td><td>大作业</td><td>60</td><td><b>团队项目</b>（2–3 人）：从教师给定题目中选题自主开发，按完成度打分</td></tr>
</table>

<h4>二、考核与纪律</h4>
<ul>
  <li>及格线 60 分；<b>缺课超过允许上限（16 次课 → 允许缺 5 次）直接认定硬性不合格</b>。</li>
  <li>扣分：每次缺课 −2 分、迟到 −1 分、请假超出 3 次后每次 −1 分。</li>
  <li>三级项目当次课未完成按缺交处理；大作业逾期不接收。</li>
</ul>

<h4>三、项目怎么做</h4>
<ul>
  <li>三级项目（个人）：课上一节完成，提前熟悉建表语句与基本增删改查结构。</li>
  <li>大作业（团队 2–3 人）：第 10 周前定题，明确分工（建模 / 编码 / 文档与演示）。</li>
  <li>代码需可编译运行，README 说明运行方式与功能清单，按完成度给分。</li>
</ul>

<h4>四、你会收到什么提示</h4>
<p>系统按考勤与成绩自动判定：缺课接近上限为黄色提醒、超过上限为红色；终结性考核前会给出「大作业需得分」测算。</p>
'@

$GUIDE_E = @'
<h3>计算机网络 · 课程导学</h3>
<p>学分 <b>2.0</b>　周次 <b>12 周（12 次课）</b>　考核方式 <b>考试课</b>　及格线 60 分</p>

<h4>一、成绩构成（总评 100 分）</h4>
<p><b>总评 = 平时成绩 × 50% + 终结性考核（期末考试）× 50%</b></p>
<table>
  <tr><th>一级项</th><th>二级项</th><th>权重</th><th>说明</th></tr>
  <tr><td rowspan="3">平时成绩<br>50%</td><td>课堂测试</td><td>30</td><td>共 6 次、每次 5 分，随堂完成；缺考当次计 0 分</td></tr>
  <tr><td>期中考试</td><td>20</td><td>第 6 周左右统一安排，卷面 100 分按 20 分折算</td></tr>
  <tr><td>课后作业</td><td>50</td><td>共 5 次、每次 10 分；逾期不补交</td></tr>
  <tr><td>终结性考核<br>50%</td><td>期末考试</td><td>50</td><td>卷面 100 分，按 50% 折算计入总评</td></tr>
</table>

<h4>二、考核与纪律</h4>
<ul>
  <li>及格线 60 分；<b>缺课超过允许上限（12 次课 → 允许缺 4 次）直接认定硬性不合格</b>。</li>
  <li>扣分：每次缺课 −2 分、迟到 −1 分、请假超出 3 次后每次 −1 分。</li>
  <li>期中考试缺考不安排补考；课后作业缺交当次计 0 分。</li>
</ul>

<h4>三、平时分怎么拿满</h4>
<ul>
  <li>课堂测试 6 次共 30 分；课后作业 5 次共 50 分，是平时分主体。</li>
  <li>平时分占总评一半，比其它课程更能靠出勤与作业把总分拉过线。</li>
  <li>期中考试 20 分，重点覆盖网络分层、IP 编址与传输层基础。</li>
</ul>

<h4>四、你会收到什么提示</h4>
<p>系统按考勤与成绩自动判定：缺课接近上限为黄色提醒、超过上限为红色；期末前会给出「期末需考分」，即按当前平时分测算的及格所需卷面分。</p>
'@

$GUIDES = @{ A = $GUIDE_A; B = $GUIDE_B; C = $GUIDE_C; D = $GUIDE_D; E = $GUIDE_E }

# ---------------- 教学班 → 课程 / 任课教师 ----------------
$TARGETS = @(
  @{ tc = 201; key = 'A'; teacher = '1001' }, @{ tc = 202; key = 'A'; teacher = '1006' },
  @{ tc = 203; key = 'B'; teacher = '1002' }, @{ tc = 204; key = 'B'; teacher = '1002' },
  @{ tc = 205; key = 'C'; teacher = '1003' }, @{ tc = 206; key = 'C'; teacher = '1003' },
  @{ tc = 207; key = 'D'; teacher = '1004' }, @{ tc = 208; key = 'D'; teacher = '1004' }, @{ tc = 209; key = 'D'; teacher = '1004' },
  @{ tc = 210; key = 'E'; teacher = '1005' }, @{ tc = 211; key = 'E'; teacher = '1005' }, @{ tc = 212; key = 'E'; teacher = '1005' }
)

$tokens = @{}
foreach ($t in $TARGETS) {
  if (-not $tokens.ContainsKey($t.teacher)) { $tokens[$t.teacher] = Login $t.teacher }
  $r = Post-Json "$base/teacher/guide" @{
    teachingClassId = $t.tc
    content = $GUIDES[$t.key]
    contentFormat = 'HTML'
    publish = $true
  } $tokens[$t.teacher]
  if ($r.code -ne 0) { "教学班 $($t.tc) 发布失败：$($r.message)"; continue }
  "教学班 {0}（{1}）→ 导学 v{2}，已发布={3}" -f $t.tc, $t.teacher, $r.data.version, $r.data.isPublished
}
