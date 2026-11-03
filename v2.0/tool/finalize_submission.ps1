# 收尾：把演示手册放进提交文件夹、更新 README 结构说明、把提交文件夹加入 .gitignore
$ErrorActionPreference = 'Stop'
$root = 'E:\JavaDEVIDEA\workspace\JavaProject\academic_risk_warning'
$base = Join-Path $root '狄珂23001021010 基于RAG与多智能体的学情预警与个性化学习管理系统的设计与实现'
$log = New-Object System.Collections.Generic.List[string]
function L($s) { $log.Add([string]$s) }

# 1) 手册入库到提交文件夹
$manual = Join-Path $root 'v2.0\word\答辩演示讲解手册.docx'
if (-not (Test-Path $manual)) { throw "手册不存在：$manual" }
Copy-Item $manual (Join-Path $base '答辩演示讲解手册.docx') -Force
L('手册已放入提交文件夹')

# 2) 重写 README 的目录结构段，反映真实布局
$readmePath = Join-Path $base 'README.md'
if (Test-Path $readmePath) {
    $readme = [IO.File]::ReadAllText($readmePath, [Text.Encoding]::UTF8)
    $oldStart = $readme.IndexOf('## 目录结构')
    $oldEnd = $readme.IndexOf('## 运行环境')
    if ($oldStart -ge 0 -and $oldEnd -gt $oldStart) {
        $newBlock = @'
## 目录结构

```
狄珂23001021010 基于RAG与多智能体的学情预警与个性化学习管理系统的设计与实现/
  基于RAG与多智能体的学情预警与个性化学习管理系统的设计与实现_项目文档.docx   项目文档（10 章）
  基于RAG与多智能体的学情预警与个性化学习管理系统_答辩PPT.pptx                答辩 PPT（20 页）
  答辩演示讲解手册.docx                                                       演示怎么点、怎么讲（9 页）
  01 项目源码/
    后端 backend/        Spring Boot 4.0.6 后端（Java 17 + MyBatis-Plus + MySQL 8）
      src/main/java      业务代码（controller / service / mapper / entity / vo / config / security）
      src/main/resources application.yml 等配置（不含 ai.env，密钥请自行配置）
      src/test/java      JUnit 5 单元测试（RiskMathTest / TriggerGateTest 等）
      javac-full.args    离线编译用的源文件清单
    前端 frontend/       Vue 3 + Vite + Tailwind 前端
      src/               页面、组件、接口封装、路由
      package.json / vite.config.js / tailwind.config.js / index.html
  02 数据库脚本 sql/     39 个 SQL 脚本（按文件名编号顺序执行，01 → 40）
  README.md              本文件
```

'@
        $readme = $readme.Substring(0, $oldStart) + $newBlock + $readme.Substring($oldEnd)
        [IO.File]::WriteAllText($readmePath, $readme, (New-Object Text.UTF8Encoding($false)))
        L('README 目录结构已更新')
    }
}

# 3) .gitignore：提交文件夹是交付产物，不进版本库（源码已在 v2.0 里）
$gi = Join-Path $root '.gitignore'
$c = [IO.File]::ReadAllText($gi, [Text.Encoding]::UTF8)
$folderName = Split-Path $base -Leaf
if ($c -notmatch [regex]::Escape($folderName)) {
    $c = $c.TrimEnd() + "`n# 课程提交文件夹（交付产物，源码在 v2.0 内）`n/$folderName/`n"
    [IO.File]::WriteAllText($gi, $c, (New-Object Text.UTF8Encoding($false)))
    L('.gitignore 已加入提交文件夹')
}

# 4) 统计
$files = [IO.Directory]::GetFiles($base, '*', 'AllDirectories')
$bytes = 0; foreach ($f in $files) { $bytes += ([IO.FileInfo]$f).Length }
L(('提交文件夹：{0} 个文件，{1:N1} MB' -f $files.Count, ($bytes / 1MB)))
L('--- 顶层内容 ---')
Get-ChildItem $base | ForEach-Object {
    if ($_.PSIsContainer) { L('  [DIR ] ' + $_.Name) }
    else { L(('  [FILE] {0}  ({1} KB)' -f $_.Name, [int]($_.Length / 1024))) }
}
[IO.File]::WriteAllLines((Join-Path $root 'v2.0\tool\_final_log.txt'), $log, (New-Object Text.UTF8Encoding($false)))
Write-Output ('done ' + $log.Count)
