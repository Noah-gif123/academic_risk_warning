# 整理提交文件夹：前后台源码 + sql 脚本 + README
$ErrorActionPreference = 'Stop'
$root = 'E:\JavaDEVIDEA\workspace\JavaProject\academic_risk_warning'
$base = Join-Path $root '狄珂23001021010 基于RAG与多智能体的学情预警与个性化学习管理系统的设计与实现'
$log = New-Object System.Collections.Generic.List[string]
function L($s) { $log.Add([string]$s) }

if (-not (Test-Path $base)) { throw "提交文件夹不存在：$base" }
L('BASE: ' + $base)

$srcDir = Join-Path $base '01 项目源码'
$beDir = Join-Path $srcDir '后端 backend'
$feDir = Join-Path $srcDir '前端 frontend'
$sqlDir = Join-Path $base '02 数据库脚本 sql'
foreach ($d in @($srcDir, $beDir, $feDir, $sqlDir)) { New-Item -ItemType Directory -Force -Path $d | Out-Null }

# ---------- 后端：src + 构建/配置说明（排除编译产物与密钥） ----------
robocopy (Join-Path $root 'v2.0\backend\src') (Join-Path $beDir 'src') /E /XD 'build' 'build-verify' 'build-test' '__pycache__' /XF 'ai.env' '*.class' '*.log' /NFL /NDL /NJH /NJS /NP | Out-Null
# 后端根目录的构建/说明文件
$beFiles = @('pom.xml', 'build.gradle', 'gradlew', 'gradlew.bat', 'README.md', 'javac-full.args', 'javac.args')
foreach ($f in $beFiles) {
    $p = Join-Path $root "v2.0\backend\$f"
    if (Test-Path $p) { Copy-Item $p (Join-Path $beDir $f) -Force; L('  + backend/' + $f) }
}

# ---------- 前端：src + 构建配置（排除 node_modules / 产物） ----------
robocopy (Join-Path $root 'v2.0\frontend\src') (Join-Path $feDir 'src') /E /XD '__pycache__' /XF '*.log' /NFL /NDL /NJH /NJS /NP | Out-Null
$feFiles = @('package.json', 'package-lock.json', 'pnpm-lock.yaml', 'vite.config.js', 'tailwind.config.js', 'postcss.config.js', 'index.html', 'README.md', 'tool\check-sfc.mjs')
foreach ($f in $feFiles) {
    $p = Join-Path $root "v2.0\frontend\$f"
    if (Test-Path $p) {
        $target = Join-Path $feDir $f
        New-Item -ItemType Directory -Force -Path (Split-Path $target) | Out-Null
        Copy-Item $p $target -Force
        L('  + frontend/' + $f)
    }
}

# ---------- SQL 脚本 ----------
$sqls = @([IO.Directory]::GetFiles((Join-Path $root 'v2.0\sql'), '*.sql') | Sort-Object)
foreach ($s in $sqls) { Copy-Item $s (Join-Path $sqlDir ([IO.Path]::GetFileName($s))) -Force }
L('  SQL 文件数: ' + $sqls.Count)

# ---------- 目录说明 README ----------
$readme = @'
# 狄珂 23001021010 · 基于 RAG 与多智能体的学情预警与个性化学习管理系统的设计与实现

## 目录结构

```
01 项目源码/
  后端 backend/          Spring Boot 4.0.6 后端（Java 17 + MyBatis-Plus + MySQL 8）
    src/main/java        业务代码（controller / service / mapper / entity / vo / config / security）
    src/main/resources   application.yml 与静态资源（不含 ai.env，密钥请自行配置）
    src/test/java        JUnit 5 单元测试（RiskMathTest / TriggerGateTest 等）
    javac-full.args      离线编译用的源文件清单（配合 javac 使用）
  前端 frontend/         Vue 3 + Vite + Tailwind 前端
    src/                 页面、组件、接口封装、路由
    package.json / vite.config.js / tailwind.config.js / index.html
02 数据库脚本 sql/       39 个 SQL 脚本（按文件名编号顺序执行）
03 答辩材料/             答辩 PPT、项目文档、答辩演示讲解手册（Word）
```

## 运行环境

| 组件 | 版本 |
|---|---|
| JDK | 17 |
| Spring Boot | 4.0.6 |
| MyBatis-Plus | 3.5.15 |
| MySQL | 8.0.30（库名 `study_warning_system_v2`） |
| Node.js | 18+（前端 Vite 4.5） |

## 启动步骤（后端）

1. 按顺序执行 `02 数据库脚本 sql/` 下的脚本（文件名前缀为执行顺序），建库 `study_warning_system_v2` 并灌入演示数据。
2. 在 `后端 backend/` 下新建 `ai.env`（不随源码提供），内容示例：

   ```
   AI_LLM_BASE_URL=https://api.deepseek.com/v1
   AI_LLM_KEY=你的模型 Key
   AI_LLM_MODEL=deepseek-flash
   # 可选：自建 Dify 时配置以下项
   AI_DIFY_BASE_URL=http://localhost:8090
   AI_GROUP_REPORT_APP_KEY=
   ```

   配置由 `application.yml` 的 `spring.config.import: optional:file:./ai.env[.properties]` 自动加载；
   **不配也能启动**，此时 AI 能力自动降级为规则模板（报告仍可生成）。
3. 编译与运行（无 Maven 环境时按本项目的离线方式）：

   ```
   javac -encoding UTF-8 -d build-verify @javac-full.args
   java -cp "build-verify;src/main/resources;依赖目录/*" com.example.academic_risk_warning.AcademicRiskWarningApplication
   ```

4. 默认端口 8080，启动后可访问 `/api/ai/health` 查看 AI 通道就绪状态。

## 启动步骤（前端）

```
cd 前端 frontend
npm install
npm run dev          # 默认 5173
```

## 演示账号（初始口令均为 000000）

| 角色 | 账号 | 说明 |
|---|---|---|
| 教师 | 1001 | 任教「大学计算机基础（一）」01 班（教学班 201） |
| 教师 | 1004 | 任教「数据结构（Java）」3 个教学班（201/202 之外的 207~209），适合演示课程总报告 |
| 辅导员 | C001 | 带「计算机1班」「CS-2024-1」等行政班 |
| 学生 | 20250001 | 有红灯/黄灯记录，可演示学生视角与预警解释卡 |
| 教务 | admin001 | 基础数据、规则配置、审计 |

## 注意事项

- 源码不含任何密钥；`ai.env` 需自行创建（已在 `.gitignore` 中忽略）。
- 数据库脚本含演示种子数据，重建后账号与密码会恢复为初始状态。
- 前端 `node_modules` 未打包，首次运行请先 `npm install`。
'@
[IO.File]::WriteAllText((Join-Path $base 'README.md'), $readme, (New-Object Text.UTF8Encoding($false)))
L('README.md 写入完成')

# ---------- 统计 ----------
foreach ($d in @($beDir, $feDir, $sqlDir)) {
    $files = [IO.Directory]::GetFiles($d, '*', 'AllDirectories')
    $bytes = 0; foreach ($f in $files) { $bytes += ([IO.FileInfo]$f).Length }
    L(('{0}  文件 {1} 个  {2:N1} MB' -f (Split-Path $d -Leaf), $files.Count, ($bytes / 1MB)))
}
[IO.File]::WriteAllLines((Join-Path $root 'v2.0\tool\_pack_log.txt'), $log, (New-Object Text.UTF8Encoding($false)))
Write-Output ('done, log lines=' + $log.Count)
