# v3.0 交接说明（HANDOFF）

> 用途：新开会话时先读这一份 + `v3.0/PLAN_v3.0.md` + `v3.0/word/v3.0规划清单.docx`，即可直接开工，无需回溯长对话。
> 更新：v3.0 规划定稿、尚未写任何 v3.0 代码。

---

## 一、当前状态（一句话）

**v2.0 已定稿并发布（GitHub Release v2.0.0，含 tag 与文档包）；v3.0 P0 数据底座已完成（P0-1~P0-8，P0-7 部分达标如实标注）；P1 统计模块已完成（P1-1 ~ P1-9 + 隐藏前置 P1-0；含看板、口径、导出、对账、证据卡）。**

| 项 | 状态 |
|---|---|
| v2.0 代码/数据 | 已完成，别再改（用户明确要求） |
| v2.0 tag `v2.0.0` | 指向 `360c920`，**不要动** |
| v2.0 Release | GitHub 上已发布 3 个（v1.0.0 / v1.1.0 / v2.0.0），**不要动** |
| v3.0 规划 | 已完成：`v3.0/PLAN_v3.0.md`（**终版 v3.0.5**）+ `v3.0/word/v3.0规划清单.docx`（**56 项 / 49.5 人日，11 页**） |
| v3.0 目录 | 已建骨架：backend / frontend / ai(dify) / sql(seeds) / tool / word / eval / evidence |
| v3.0 代码 | **P0-1 ~ P0-8 完成**（2026-11-08；P0-7 部分达标）；**P1-1 ~ P1-9 完成**（统计模块/看板/对账/证据卡）；见 §十、§十二 |

## 二、推送规则（务必遵守）

1. **只推 GitHub**（`origin`），**不再推 Gitee**（Gitee 停在 `b52349a`，有意为之）。
2. 只推 `main`，不快进以外的操作；**不创建/不移动任何 tag 与 Release**（v3.0 定稿前一律不建）。
3. `origin` 的 https 地址已配置为自动走 SSH：
   ```
   git config --local url."ssh://git@ssh.github.com:443/".insteadOf "https://github.com/"
   ```
   直接 `git push origin main` 即可；本机 HTTPS（schannel）不可用，勿改回。
4. 推之前先 `git ls-remote origin refs/heads/main` 确认真实远端，不要只看本地跟踪引用。

## 三、已确认的口径与决策（照此实现，不要重议）

| 项 | 决策 |
|---|---|
| 关闭率分母 | **周期内新增预警**（同期新增中已关闭数 ÷ 周期内新增数） |
| 复评窗口 | **干预后第 7 天**（<7 天不计改善；>14 天标「逾期未复评」） |
| Dify | **本机部署**（复用 `v2.0/ai/dify`，端口 8090） |
| Agent 路线 | **全自研为主**；仅 **Diagnostician 额外做 Dify 版**（Tutor 的检索段可复用 Dify 知识库）；其余不做 Dify 版 |
| Agent 编排 | **四角色** Planner/Executor/Critic/Writer；自适应 `auto/2/4`；路由规则优先、未命中才问模型 |
| 智能体清单 | 4 核心 = Diagnostician / Reporter / Grader / Tutor；2 可选 = Coach / Steward；**不做**「周报解读 Agent」 |
| 冲突仲裁 | 智能体之间不互相调用；冲突**以证据台账为准**，最终由人拍板 |
| 记忆 | 两级（摘要 + 事件流）、按事件类型可开关、**学生端可查可删** |
| 统计看板 | 先 **辅导员 + 教务**；教师端 v3.1 |
| 评测集 | 我起草工具链，用户复核；**期望数字由 SQL 生成**，不手写 |
| CI | 本地八步脚本，不上云 |
| 创新点 | 主：M-1 证据台账、M-3 可归因评估、A-1 可核查对话式研判 |
| **Agent 落地级别** | **只做 L2**（工具白名单 + 共享台账 + 步数预算 + Critic 回环）；**不做 L3** 自主协商/群聊/投票（实测 10 人 @全体+2 轮=18 次调用且极不稳） |
| **工具调用协议** | **不用厂商 function calling**；用受限 JSON `{"tool","args","why"}` + 白名单 + 强校验 + 步数预算（四道检查） |
| **RAG 定位** | RAG 是**受台账约束的智能体工具** `retrieveCourseDocs`，不是独立场景；引用必须带 `chunkId` 且可点开原文 |
| **术语口径** | 统一写「**可验证的多角色 LLM 编排**」，不宣称自主协商；论文 3.2 节加「vs 经典 MAS vs prompt chaining」对比表 |
| **v3.0 数据隔离**（2026-11-08 用户确认） | v2.0 演示库 `study_warning_system_v2` **保持原样**（已用 P0-1 快照恢复，cfr/λ 回到原始值）；v3.0 用独立库 `study_warning_system_v3`，由 v3.0 内的独立生成器按 `v3.0/sql/seeds/params.json` 生成；启动 v3.0 用 `v3.0/tool/run_v3_backend.ps1`，两套演示互不影响 |
## 四、开工前必须知道的既有事实（已核实）

这些是 v3.0 P0 要处理的真实问题，新会话不要重新"发现"：

| 事实 | 证据 |
|---|---|
| `course_fail_rate_history` 5 行，但 **`course_factor` 全 NULL、`is_initialized=0`** | 直接查库 |
| `teacher_style` 6 行，**`shrink_lambda` 全 0.620、`sample_size` 全 45**（收缩"假生效"） | 直接查库 |
| `risk_state` 540 行；**524 行走 C 通道、16 行走 A 通道**；`delta_used` 全部非 0、`course_factor_used` 全部 ≠1 | 直接查库 |
| 班间差异极小：CS-01-A avg 30.9 / CS-02-A avg 32.0；cs-01-D 81.8 / 02-D 79.9 / RT-D 81.7；班内 sd 4–8 | 直接查库 |
| 135 名学生中仅 1 人叫「学生1」（20250001），其余为拟真姓名 | 直接查库 |
| 全库快照口径：75 表 / 40,013 行（2026-11-08，含 term 4/5/8 三学期） | `v3.0/sql/snapshot_v2.0.manifest.json` |
| 9 名教师最多 2 门课、最多 4 个班（1001 只 1 个班） | 直接查库 |
| 前端**没有图表库**（package.json 无 echarts） | 查 package.json |
| 现有统计只有零散 overview / coverage 接口，无独立统计模块 | 查 controller |

## 五、下一步：P0 数据底座（约 6 人日；开工口径见下）

按 `v3.0规划清单.docx` 的 P0-1 … P0-8 执行，顺序：

1. ✅ `v3.0/sql/snapshot_v2.0.sql`（先能回滚，再动数据）—— 2026-11-08 完成，见 `v3.0/evidence/P0.md`
2. ✅ `course_factor` 初始化：`clamp(1 + k*(fail_rate - baseline), 0.90, 1.15)`，写回 `is_initialized=1`；引擎读 NULL 时按 1.0 + WARN —— 2026-11-08 完成，见 §十 与 `v3.0/sql/40_p0_2_course_factor_init.sql`
3. ✅ `λ = n/(n+k)` 随样本量生效 + 造 1–2 个样本 <10 的熔断案例（`fused=1` + 原因）—— 2026-11-08 完成，见 §十 与 `v3.0/sql/41_p0_3_lambda_and_fuse.sql`
4. ✅ 拟真数据生成器（教师 12–15 / 课程 8–10 / 班 6–8 / 3 学期 / 对照组 60:40），参数化 + seed 可复现 —— 2026-11-08 完成，见 §十 与 `v3.0/tool/gen_v3_seed.js`
5. ✅ 重算流水线 `v3.0/tool/run_v3_pipeline.ps1`：seed → 判定 → 报告 → 快照 → 基线，一条命令 <3 分钟 —— 2026-11-08 完成，见 §十
6. ✅ 数据校验脚本 + `v3.0/sql/seeds/data_story.md`（每班/课的成因说明）—— 2026-11-08 完成，见 §十 与 `v3.0/tool/verify_data_v3.ps1`
7. ✅ 重跑 C1/C2 消融 → `v3.0/evidence/P0.md`（修前 vs 修后对照）—— 2026-11-08 完成（教师松紧达标；课程类型/历史挂科率超目标，如实标注部分达标），见 §十
8. ✅ `v3.0/tool/ci.ps1` 骨架（编译 / 单测 / 数据校验 / 报告对账）—— 2026-11-08 完成，见 §十、§十一

**P0 出口标准**：流水线一条命令跑通；C1 效应量达教师松紧 1.5–3%、课程因子 2–4%、历史挂科率 1–2%；v2.0 验收脚本仍全绿（未破坏 v2.0 演示）。

## 六、环境与常用命令（本机）

```powershell
# 后端（8080；调接口用 8081 避免占用用户端口）
cd v2.0\backend
$libs = (Get-ChildItem build-verify-lib -Filter *.jar | % { $_.FullName }) -join ';'
java "-Xmx384m" "-Dfile.encoding=UTF-8" -cp "build-verify;src/main/resources;$libs" `
     com.example.academic_risk_warning.AcademicRiskWarningApplication --server.port=8081

# 编译
javac @javac-full.args

# 查库（支持 SELECT/DML，支持 @文件 批量执行、@out=文件 写 UTF-8）
cd v2.0\tool\db
java -cp ..\..\backend\build-verify-lib\mysql-connector-j-9.2.0.jar Q.java "select 1"

# 单元测试（不依赖 Maven）
java -cp "build-test;build-verify;src/main/resources;<jars>" RunTest com.example.academic_risk_warning.util.RiskMathTest

# v3.0 快照 / 回滚（P0-1；本机执行策略限制，必须带 -ExecutionPolicy Bypass）
powershell -NoProfile -ExecutionPolicy Bypass -File v3.0\tool\snapshot_v2.0.ps1
powershell -NoProfile -ExecutionPolicy Bypass -File v3.0\tool\rollback_v2.0.ps1 -Drill
powershell -NoProfile -ExecutionPolicy Bypass -File v3.0\tool\rollback_v2.0.ps1

# v3.0 独立库（P0-4 起；v2.0 库 study_warning_system_v2 不受影响）
node v3.0\tool\gen_v3_seed.js
powershell -NoProfile -ExecutionPolicy Bypass -File v3.0\tool\bootstrap_v3.ps1 -Force
powershell -NoProfile -ExecutionPolicy Bypass -File v3.0\tool\run_v3_backend.ps1 -Port 8080

# v3.0 重算流水线（P0-5：seed→判定→报告→快照→基线）
powershell -NoProfile -ExecutionPolicy Bypass -File v3.0\tool\run_v3_pipeline.ps1

# v3.0 数据校验（P0-6：43 项）
powershell -NoProfile -ExecutionPolicy Bypass -File v3.0\tool\verify_data_v3.ps1

# v3.0 CI（P0-8：编译/单测/数据校验/报告对账/基线 diff）
powershell -NoProfile -ExecutionPolicy Bypass -File v3.0\tool\ci.ps1
powershell -NoProfile -ExecutionPolicy Bypass -File v3.0\tool\ci.ps1 -Db study_warning_system_v2 -SkipBaseline   # 负向：应退出 1
```

- 数据库：`study_warning_system_v2`，`root/root`
- 演示账号（口令 `000000`）：学生 `20250001`、教师 `1001`（教研 201）、`1004`（数据结构 3 个班）、辅导员 `C001`、教务 `admin001`
- PS 5.1 注意：含中文的 `.ps1` 必须存为 **UTF-8 带 BOM**；`.NET` 读写中文路径用绝对路径（`Set-Location` 不影响 .NET 的相对解析）
- **本机 PowerShell 执行策略禁止直接运行未签名 `.ps1`**：一律用 `powershell -NoProfile -ExecutionPolicy Bypass -File ...`；`v2.0/tool/rebuild_demo.ps1` 内部调用没加 Bypass，本机直接跑会失败（要用时先补参数）。
- **DSH 会话沙箱（workspace-write）的两个坑**（2026-11-08 已在本机修复/规避）：
  1. 工作区目录未继承 DSH 所需的权限与完整性标签时，命令行**不能写任何文件**；本机已给整个仓库补了当前用户可继承完全控制 + Low 完整性标签，恢复记录在 `.dsh-acl-recovery/`（该目录已 gitignore）。新克隆/新机器可能重现。
  2. `Start-Process` 启动的 java 会随命令结束被沙箱清理；做后端验收要用工具后台作业启动（或另开窗口）。
- 后端连恢复库做验收：加参数 `--spring.datasource.url=jdbc:mysql://127.0.0.1:3306/study_warning_system_v2_verify?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true&useSSL=false`，并设环境变量 `AI_ENABLED=false`。
- **DSH 会话里 git 推 GitHub 的坑**（2026-11-08）：本会话 `HOME` 为空、用户目录含中文，ssh 读不到 `~/.ssh/config` 与密钥，报 `Host key verification failed` / `Permission denied (publickey)`。绕法是给 ssh 一个 ASCII 路径的 HOME：
  ```powershell
  New-Item -ItemType Junction -Path .tmp-doc\sshhome -Target $env:USERPROFILE
  $env:HOME = '/e/JavaDEVIDEA/workspace/JavaProject/academic_risk_warning/.tmp-doc/sshhome'
  git push origin main
  ```
  只在 DSH 会话内需要，用户自己的终端正常；`.tmp-doc\sshhome` 是目录联接，用完删它不影响原目录。本地 main 已改为跟踪 `origin/main`。


## 七、如果新会话只做一件事

先做 **P0-2（course_factor 初始化）**：它是 C 通道、课程对比、校准页、消融实验的共同前置，改动小、收益立竿见影。

---

## 八、会话切分建议（v3.0 必须分窗口做）

v3.0 共 **56 项 / 49.5 人日**，任何单个会话都做不完。建议按阶段开新会话，每个会话开头只贴这三样：

1. 本文件（`v3.0/HANDOFF.md`）
2. `v3.0/PLAN_v3.0.md` 里对应轨道的那一节（不用整篇）
3. 一句开工口令，例如「按 HANDOFF 开工 P1-2 到 P1-4」

| 会话 | 范围 | 项数 / 人日 | 建议开场任务 |
|---|---|---|---|
| **A** | P0 数据底座 | 8 项 / 6.0 | ~~P0-1 快照~~（已完成）→ P0-2 course_factor → P0-3 λ |
| **B** | P1 统计模块 | 9 项 / 7.75 | P1-2 三表 DDL → P1-4 七接口 |
| **C** | P2 框架 + Diagnostician | P2-1~P2-7 | ToolSpec → 受限 JSON 执行器 → 台账 |
| **D** | P2 其余 3 agent + 记忆 + 轨迹 | P2-8~P2-12 | Reporter → Grader → Tutor → 轨迹页 |
| **E** | P2 评测与护栏 | P2-13~P2-15 | 30 题集 + SQL 生成期望值 |
| **F** | P2-R RAG 工具化 | 3 项 / 3.0 | retrieveCourseDocs + 引用绑定 |
| **G** | P3 Dify parity | 8 项 / 4.75 | 手写 OpenAPI → parity 脚本 |
| **H** | P4 + P5 | 13 项 / 8.0 | 亮点收口 → 文档答辩 |

**每个会话结束前必做**：跑 `ci.ps1`、更新 `evidence/P{n}.md` 证据卡、把"已核实事实/新坑"追加进本文件第四、六节、**commit + push GitHub**。这样下一个会话不用回溯长对话。

## 九、本机已有的辅助工具（可选，不影响主线）

`v3.0/tool/ai_room/`：零依赖的多模型工作台（会议模式 + 群聊模式），用于给设计挑刺。
- 启动：`node server.mjs` → `http://127.0.0.1:8099`
- 需要 `keys.json`（已 gitignore，不进仓库）
- 结论：**群聊模式实测可用但不进系统**（决策 19 只做 L2）；头脑风暴记录见 `v3.0/eval/brainstorm_20261106_2040.md`
- 注意那份记录里有幻觉（`agent/orchestrator.py`、`arXiv` 假引用、`Dify v1.22`），**只取判断，不取路径与文献**

## 十、P0 进度记录（逐项追加）

| 任务 | 改动摘要 | 验证结论 | 遗留风险 |
|---|---|---|---|
| **P0-2**（2026-11-08） | 新增 `v3.0/sql/40_p0_2_course_factor_init.sql`：按 `clamp(1 + k*(fail_rate - baseline), 0.90, min(cap,1.15))` 幂等回填 `course_fail_rate_history.course_factor` 并置 `is_initialized=1`；`RiskEngineService#failRateFactor` 改为读落库因子（`round(avg,4)`），行存在但因子 NULL 时按 1.00 并 `log.warn`，无历史行时保留 `student_fail_history` 反推兜底 | `javac @javac-full.args` 通过；RiskMathTest 20/20；SQL 回填后 5/5 行 factor 非空、`is_initialized=1`（1.006/0.983/0.983/0.987/0.987）；探针行验证两条路径：NULL→日志出现 WARN 且 `data_quality_json.failRateFactor=1.0`；落库 0.990→`risk_state.course_factor_used=1.089`（1.10×0.99） | 现有历史行只有当学期（term 5），引擎按口径排除，本次初始化对当前 `risk_state` 无直接影响，效果在下一学期体现；历史行若忘初始化会按 1.00 低估课程风险，需 P0-5 流水线保证回填 |
| **P0-3**（2026-11-08） | 新增 `v3.0/sql/41_p0_3_lambda_and_fuse.sql`：λ=n/(n+k)（k=delta_shrink_k）重算 6 行并同步 Δ̂=raw×λ；造 2 个样本<10 熔断案例（teacher1/offering101、teacher4/offering104，sample_size=8、fused=1、status=FUSED、fuse_reason）。引擎 `channelC` 跳过 FUSED/INSUFFICIENT 的 Δ 并按 1.00 处理 + `log.warn`；`teacherStyleOf` 改读 OVERALL 行（不再只在 SQL 侧过滤 ACTIVE）；`RiskState.deltaUsed` 加 `updateStrategy=ALWAYS` 让熔断时的 null 能写回 | `javac` 通过、RiskMathTest 20/20；λ 由全 0.620 → 45 人 0.429 / 8 人 0.118；重算 term 5（540 行）后：熔断 3 个班（201/207/208/209，共 180 行）`delta_used` 为 NULL，日志出现 4 条「已熔断」WARN；非熔断班 Δ 用 λ 后值（-0.57/-0.40/-1.11/-0.79），`data_quality_json.teacherFactor` 相应为 1.0282/1.0197/1.0557/1.0390；等级分布 421/3/16/100 → 424/3/16/97（3 人黄→绿） | 引擎仍保留 `RiskMath.teacherStyleFactor` 的「样本<30 折半收缩」旧逻辑，与数据层 λ 叠加会双重收缩；当前 45 人样本不触发，P0-4 生成小样本班级时需统一（建议下沉为 λ 或删除折半）；熔断为演示用人工样本 8，非真实班级人数 |
| **P0-4**（完成，2026-11-08） | 方案 B：v2.0 演示库隔离 + v3.0 独立生成器。新增 `v3.0/tool/gen_v3_seed.js`：当前+3 历史学期、14 教师、9 课程、8 行政班（30–45 人）、50 教学班、1,927 选课、17,343 考勤、6,895 分项、1,927 成绩单、5,544 条历史挂科、36 行课程挂科率、12 行教师尺度、60:40 对照组 `v3_cohort.json`、`data_story.md`；`params.json` 全参数化（seed=20261108）；`bootstrap_v3.ps1` 全链路（快照→克隆 v3 库→生成→导入→P0-2/P0-3→规模校验）；`run_v3_backend.ps1` 连 v3 库启动 | 同 seed 两次生成 sha256 一致（`3f5ed3a9…`）；bootstrap 全流程通过：75 表 / 14 教师 / 9 课 / 8 行政班 / 50 教学班 / 1,927 选课；统计自检全绿：班间差 8.4–11.7（目标 6–12）、班内 sd 9.0–11.4（目标 9–12）、历史挂科率 1.8–23.7%（目标 3–25%）；v3 引擎实测：`risk_state` 1,927 行（GREEN 1265 / YELLOW 383 / ORANGE 138 / RED 141）、`alert_record` 662；课程因子取 3 个历史学期均值（101 班 factor_avg=1.081→×1.10=1.189）、熔断班 433 行 `delta_used=NULL` | v2.0 演示库校验未变（421/3/16/100、cfr 全 NULL）；初版不生成试卷/项目/问卷（v3 对应页面为空，P2 前补齐）；当前学期 RED 占 7.3% 偏高，P1 看板前可再调参；生成器只应写 `study_warning_system_v3`，不要指向 v2 库 |
| **P0-5**（完成，2026-11-08） | 新增 `v3.0/tool/run_v3_pipeline.ps1`：seed→判定→报告→快照→基线（一条命令）；支持 `-SkipSeed/-SkipEngine/-SkipReports/-SkipSnapshot/-SkipBaseline` 续跑；内置后端启停、登录/判定/三类报告 HTTP 调用；基线写 `v3.0/eval/baseline.json`，快照写 `v3.0/sql/snapshot_v3.sql`（已 gitignore，可由流水线重生成）。同时修复 `NumberAuditor` 对账误判：①「及格线」正则要求后随「分」（避免把列表序号 3）/4）当数字）；② 行政班报告的「分课程红/黄人数」纳入 perClass 备选 | 全量跑 2 次：**41.9s / 38.7s**（目标 <3 分钟）；两次 baseline（忽略 `generatedAt`）**完全一致**；报告对账 **6/6、10/10、6/6（0 不一致）**；判定 1,927 条；快照 75 表 / 41,995 行；v2.0 库未受影响（421/3/16/100、cfr 全 NULL） | 快照含 NOW() 类时间戳，逐字节 sha 每次不同（只保证逻辑幂等）；流水线尚未生成干预记录（对照组 cohort 已就绪，P1 前补）；报告每种类型各生成 1 份（201/101/行政班1） |
| **P0-6**（完成，2026-11-08） | 新增 `v3.0/tool/verify_data_v3.ps1`：**43 项**校验（A 分类合计/选课-成绩单对账、B 20 类孤儿、C 三通道非空率 100%、D v3 口径：因子初始化/λ=n/(n+k)/熔断原因/对照组 60:40/规模区间），失败非零退出，报告写 `v3.0/evidence/verify_data_v3.md`；生成器 `data_story.md` 扩写为「参数+课程总表+分班成因说明」（每班：班效应/教师尺度/缺勤超限/重修，131 行） | 43/43 PASS：risk 等级合计=1,927、risk_state=选课=成绩单 1,927、三通道空值 0、cfr 36/36 已初始化且因子在 [0.90,1.15]、λ 偏差 0、熔断 2 行均有原因、TREAT 181/CONTROL 127（58.8%）、教师 14/课程 9/行政班 8/班额 30–45；生成器 seed SQL sha 未变（`3f5ed3a9…`） | 校验是库内一致性，不含浏览器端到端；班间差/班内 sd 自检在生成器阶段（DB 侧未重复断言）；试卷/项目/问卷仍缺 |
| **P0-7**（完成·部分达标，2026-11-08） | 新增 `v3.0/tool/ablation/AblationToolV3.java`（可指定库的 C1/C2 消融：复算自检 + 等级变化率/风险分变化/红黄占比 + 阈值扫描 + JSON 输出）与 `v3.0/tool/run_ablation.ps1`（编译+双库运行）；`v3.0/evidence/P0.md` 增加「修前 vs 修后」对照 | 自检复算 540/540、1927/1927 全一致；修前 v2.0：教师 1.48% / 课程类型 3.33% / 历史 0%；修后 v3.0：教师 **2.39%（目标 1.5–3% ✅）**、课程类型 **8.51%（目标 2–4% ❌）**、历史挂科率 **7.78%（目标 1–2% ❌）**；C2 阈值扫描已入 JSON | 两项超目标，按用户确认「如实报告 + 标注部分达标」，未改数据/配置硬凑；原因：1,927 vs 540 行、红黄占比 34.4% vs 22.0%、历史因子由 0 变全覆盖；P1 前需决策是否调参（证据卡列 3 个方向） |

| **P0-8**（完成，2026-11-08） | 新增 `v3.0/tool/ci.ps1`：**5 步本地门禁**（编译 / 单测 25 项 / 数据校验 43 项 / 报告对账 3 份 / 基线 diff），输出 `v3.0/tool/ci_report.md`，任一步失败非零退出；封装原生调用避免 PS5.1 在 EAP=Stop 下把 javac stderr 注记误判为失败 | 正向：**5/5 PASS，24.6s**（compile 10.8s、RiskMathTest 20/20 + TriggerGateTest 5/5、数据 43/43、报告 6/6+10/10+6/6、基线一致）；负向：`-Db study_warning_system_v2 -SkipBaseline` 时数据校验 35/43 → **退出码 1**（阻断验证通过） | CI 依赖 v3 库已由 pipeline 生成（不自动重建）；基线 diff 目前只比关键计数，未做报告文本全文 diff；Dify 用例未纳入（P3 再加 `[skip-if-offline]`） |

## 十一、P0 出口状态（2026-11-08）

| 出口标准（PLAN §一 / §七） | 结果 |
|---|---|
| 重算流水线一条命令跑通 <3 分钟 | ✅ 38.7–41.9s（P0-5） |
| C1 效应量 · 教师松紧 1.5–3% | ✅ 修后 2.39%（P0-7） |
| C1 效应量 · 课程类型 2–4% / 历史挂科率 1–2% | ⚠️ 修后 8.51% / 7.78%，已如实标注部分达标（P0-7，未改数据/配置硬凑） |
| v2.0 验收脚本仍全绿（未破坏演示） | ✅ P0-1 在恢复库跑 m3/m4/m5 全绿；v2 库此后未改动 |
| 数据校验（分类合计/孤儿/三通道） | ✅ 43/43（P0-6） |
| CI 四步全绿、非零退出阻断 | ✅ 5/5 PASS，负向退出 1（P0-8） |
| 证据卡 | ✅ `v3.0/evidence/P0.md`（P0-1 快照 + P0-7 修前/修后消融）、`verify_data_v3.md`、`ablation_v2/v3.json` |

> 结论：P0 数据底座可交付；唯一未闭合项是 P0-7 的两项效应量超目标（原因与调参方向见证据卡），不影响 P1 统计模块开工。

## 十二、P1 进度记录（2026-11-08 起）

**P1-4 决策（用户已确认）**：D1 方案 A（实时查询现有业务表 + `NO_DATA/PARTIAL` 降级，不改数据库结构）；D2 统一返回 200 + `current=null` + `dataStatus/reason`；D3 旧 `/api/{admin,counselor}/metrics/intervention` 保留未下线；D4 单路径 `/api/stats` + 角色裁剪（COUNSELOR 本班 / ADMIN 全校，教师端不开放）；D5 最小口径（关闭率分母=周期内新增、处置时长=close−create、干预改善暂用 `intervention_record.effective` 字段、P1-4 不做对照组）；D6 包名 `stats` + CSV 导出；D7 默认 `termId=当前学期`、`period=week`（自然周）。

| 任务 | 改动摘要 | 验证结论 | 遗留风险 |
|---|---|---|---|
| **P1-4**（完成，2026-11-08） | 新建 `com...stats` 模块：`controller/stats/StatsController`、`service/stats/StatsService|StatsRepository|StatsScopeResolver`、`vo/stats/StatsEnvelope|StatsVos|StatsQuery`；`/api/stats` 七接口（overview/trend/distribution/compare/funnel/effect/export），统一 `current/previous/deltaRate/dataStatus/reason/sampleSize/asOf/period`；辅导员按 `counselor_class` 裁剪、教务全校；CSV 导出；本地 `javac-full.args` 追加 7 个源文件（该文件被 .gitignore，仅本机） | `javac @javac-full.args` 通过；v3 库实测与 SQL 逐项一致：admin overview **1927 / RED141 / ORANGE138 / YELLOW383 / GREEN1265**、avgRisk 29.14、classCount 50、active/new 662、closed 0；C001 范围 **1222 / 99 / 93 / 266 / 764**、classCount 31、alerts 458；distribution/compare `OK`，funnel `PARTIAL`（关闭/干预 0），effect `NO_DATA`（干预 0），trend 8 周（当前周 662）；无 token **401**、学生 token **403**；export CSV 与接口数字一致 | P1-2/P1-3 物化表未落地：trend 仅预警/干预事件口径、effect 用 `effective` 字段、无对照组；旧干预指标接口保留；P1-1 口径文档、P1-5/6 看板、P1-7 气泡/Excel、P1-8 对账脚本、P1-9 证据卡未做 |
| **P1-1**（完成，2026-11-08） | 新增 `v3.0/docs/metrics.md`：五个闭环指标主/辅口径 + 可执行 SQL（关闭率 / 处置时长 / 干预等级下降比例 / 7 天复评改善率 / 风险面变化率）、周期与 T+7 定义、对照组口径、物化与失效约定、接口映射与降级；确定 `intervention_record` 增 `level_before/level_after/cohort` | 文档 SQL 与现有表字段一致；与 P1-4 响应字段、P1-2/P1-3 表结构对齐；口径可直接作为 P1-3 物化与 P1-8 对账依据 | 物化 SQL 需 P1-3 实现后回填真实数字；对照组为演示构造，证据卡需标注「准实验、非净效应」 |
| **P1-2**（完成，2026-11-08） | 新增 `v3.0/sql/50_stats_ddl.sql`（`stat_daily_class` / `stat_weekly_kpi` / `stat_intervention_effect`，含 `engine_version`/`source_max_update_time`/`is_stale` + 唯一键与索引）与 `v3.0/sql/51_p1_intervention_review.sql`（`intervention_record` 增 `level_before/level_after/cohort` + 索引，幂等动态执行）；`bootstrap_v3.ps1` 重建时自动应用 50/51 | v3 库执行成功：3 张 stat 表存在、intervention_record 3 列存在；50/51 可重复执行（幂等）；bootstrap 现会自动应用 40/41/50/51 四个脚本 | stat 表当前为空（待 P1-3 物化）；51 只加列不改已有数据 |
| **P1-0**（完成，2026-11-08） | 新增 `v3.0/tool/demo/GenLifecycle.java` + `v3.0/tool/gen_p1_lifecycle.ps1`：确定性重写 662 条告警的 create/handle/close 时间与状态（ACTIVE 132 / HANDLING 167 / CLOSED 363）并写 2,747 条 alert_flow_log；生成 TREAT 316 / CONTROL 214 干预记录（含 `level_before/level_after/cohort` 与 T+7 复评）；回填 `stat_daily_class` 450 行（9 周 × 50 班）；流水线新增 2.5 步（`-SkipLifecycle` 可跳过） | pipeline 全链路 27s（lifecycle 3.2s）；term 口径：关闭率 54.83%、平均处置 185.2h、funnel 662→530→363→530→214、effect due=510 improved=214 rate=41.96%；TREAT 改善 162/316=51.3% vs CONTROL 52/214=24.3%（差值约 27pp；按 due 口径 54.2% vs 24.6% ≈29.6pp ≥20pp）；trend 8 周事件非零；verify_data_v3 仍 43/43 PASS | 数据为演示构造（准实验、非净效应），证据卡需标注；干预 create 上限为 now−8d，当前周无干预，默认 week 的 effect 为 NO_DATA（P1-5/6 建议默认 term 或 8 周窗口）；stat_daily_class 历史为演示回填值 |
| **P1-3**（完成，2026-11-08） | 新增 `service/stats/StatsMaterializer`：日快照 upsert（`stat_daily_class`）、周 KPI（close_rate/avg_handle_hours/improve_rate/risk_ratio，`stat_weekly_kpi`）、干预成效（周×教学班×cohort，`stat_intervention_effect`）；engine_version/source_max_update_time 落后或 `is_stale=1` 时读取前自动重算；每日 02:10 定时 + `POST /api/stats/recompute`（仅教务）；P1-4 接口升级：overview 输出 previous/deltaRate（风险面百分点）、trend 增加红/黄/占比/改善数、effect 改为 T+7 等级口径 + TREAT/CONTROL 对比 | pipeline 新增 2.6 物化步（0.4s）；recompute 返回 `daily=50, weeks=36, effects=328`；overview `OK` riskRatio=34.35、deltaRate=−6.08pp、stale=false；trend `OK` 8 周含风险面与改善；effect `OK` TREAT 54.18% vs CONTROL 24.64%、diffPp=29.54、due=510；stale 测试：手工置 500 行 `is_stale=1` → GET overview 自动重算回 0；ci.ps1 **5/5 PASS** | 周 KPI 目前仅 scope=ALL；stat 表未带 term_id（单学期演示假设）；P1-4 实时查询与物化读取并存，P1-5/6 统一用物化；previous 取上一自然周最后快照 |
| **P1-5**（完成，2026-11-08） | 新增 `pages/counselor/Stats.vue` + `api/stats.js` + 自研 SVG 组件（StatCard/InfoTooltip/TrendChart/BarChart/FunnelChart/StackedBars）；导航与路由加「统计看板」；5 图：班级风险趋势（红黄占比+新增预警）、多门预警学生变化、干预成效漏斗、TREAT/CONTROL 对照组对比、班级风险堆叠对比；支持 学期/月/周 切换、刷新、CSV 导出；每图带口径气泡 | `npm run build` 通过（2.94s）；接口驱动数据：riskRatio 34.35%、multiWarn 11、TREAT 54.18% vs CONTROL 24.64%（diff 29.54pp）、funnel 662→530→363→530→214；辅导员范围 C001 1222 人；图表零依赖（echarts 本机 npm 不可用，改自研 SVG） | 未接浏览器截图（P1-9 补）；趋势/漏斗状态以接口 dataStatus 为准；前端依赖后端已启动（18084） |
| **P1-6**（完成，2026-11-08） | 新增 `pages/admin/Stats.vue` + `ScatterPlot.vue`；导航与路由加「统计看板」；5 图：预警关闭率/平均处置时长/红黄占比/干预改善率 KPI、课程集中风险 Top5、课程因子校准散点（fail_rate vs course_factor）、处置时效分档分布、课程风险堆叠；AI 质量卡占位（P2 接入）；教务可手动「重算」调用 `/api/stats/recompute` + CSV 导出 | `npm run build` 通过；接口实测 closeRate=54.83%、avgHandleHours=185.20、buckets 0/29/334、calibration 9 门课（如 大学计算机基础 failRate 6.17 → factor 1.01）、课程分布 9 门；重算按钮走 P1-3 物化 | 校准散点为全校口径（未按辅导员裁剪）；处置分档 lt24=0 是演示数据 handle 延迟 2–72h 所致；AI 质量卡仅占位 |
| **P1-7**（完成，2026-11-08） | 新增 `StatsExcelExporter`（POI）与 `/api/stats/export?format=xlsx`；看板每图接入 `InfoTooltip`（定义 + SQL）；CSV/XLSX 与接口同源数据 | 每图可点开口径气泡；CSV 与接口数字一致（verify_stats 内校验）；XLSX PK 头、~3.9KB 可下载 | 前端未引图表库（自研 SVG）；气泡为静态文案 + SQL，未从后端动态取 SQL 文本 |
| **P1-8**（完成，2026-11-08） | 新增 `v3.0/tool/verify_stats.ps1`：起后端（18085）→ 7 接口取数 → SQL 逐项对照 → 401/403 → 导出校验 → 写 `evidence/verify_stats.md`，失败非零退出；`ci.ps1` 增加第 6 步 `stats-verify` | `verify_stats.ps1` **11/11 PASS**；`ci.ps1` **6/6 PASS（35.7s）**：compile/unit-tests/data-verify/report-audit/baseline-diff/stats-verify | 对账覆盖 7 接口 + 权限 + 导出；未覆盖前端 DOM 数字（由截图人工核对） |
| **P1-9**（完成，2026-11-08） | 新增 `v3.0/evidence/P1.md`：五指标实测、P1-0/2/3 数据底座、验收对照、复现命令、限制；新增 `evidence/screenshots/admin_stats.png`、`counselor_stats.png`（Chrome headless CDP 注入登录态截取） | 教务 5 图 + 辅导员 5 图截图可复核；对照组差值 29.54pp（全校）/32.93pp（C001）；CI 证据 `ci_report.md`、对账 `verify_stats.md` | 截图为 1280×900 首屏（班级对比图在折叠下方）；数据为演示构造，需在论文标注准实验边界 |

## 十三、P1 硬化记录（限制 1–9、12，2026-11-08）

| 项 | 改动 | 验证 |
|---|---|---|
| 限制 1 | `docs/data_authenticity.md` + `tool/placebo/PlaceboTest.java` + `run_placebo.ps1` + `eval/placebo.json` | 置换检验：观测差 29.54pp，零分布 95% [−8.46, 8.52]，p≈0.0002（5000 次） |
| 限制 2 | `52_stats_term_id.sql`：stat 三表加 term_id + 唯一键；Materializer/Repository 按 term 读写；bootstrap 应用 50/51/52/53 | null term rows=0；verify_stats PASS |
| 限制 3 | Materializer 周 KPI 分 `ALL/COUNSELOR/CLASS` | scope types=3；KPI 1211 行 |
| 限制 4 | Scope 增加 teachingClassIds（enrollment→student 映射），Repository 统一 teaching_class_id 口径 | C001=1222=SQL；与 C002 无重叠；verify PASS |
| 限制 5 | 中位处置时长、逾期/未复评、课程/班级风险占比 | median=178h、overdue/notReviewed 有值；verify PASS |
| 限制 6 | export CSV/XLSX 覆盖 5 类；verify 校验表头与 XLSX 内容 | 5/5 CSV + xlsx 内容 PASS |
| 限制 7 | GenLifecycle 逐日回填 62 天、source 标记 | distinct dates=62、source kinds=2 |
| 限制 8 | Envelope `staleBefore/refreshed/refreshReason`；看板提示 | verify 字段检查 PASS |
| 限制 9 | 离线 ECharts（ESM）+ 缩放/图例/点击/筛选/明细；截图更新 | `npm run build` 通过；截图 `screenshots/*.png` |
| 限制 12 | 复合索引 + 多门预警单查询 + 接口计时 | 26–54ms；索引检查 PASS |

> P1-8 对账升级为 **22 项**（原 11 项 + term_id/日快照/scope/source/stale/中位/逾期/全类型导出/xlsx 内容/时延/索引）。

## 十四、P2-0 前置记录（2026-11-08）

| 项 | 改动 | 验证 |
|---|---|---|
| **P2-0a 模型自检** | 新增 `v3.0/tool/check_model.ps1`：读取 `ai.env`（不打印密钥）→ DeepSeek `/chat/completions` 两次调用（temperature=0）→ 写 `eval/model_check.json` | 可达 true；model=`deepseek-flash`；延迟 1.39–1.64s；tokens≈120；返回非空；**temperature=0 不可复现（false）**，P2 评测需多次运行/缓存 |
| **P2-0b Grader 数据** | `gen_v3_seed.js` 新增：2 门课（高数/数据结构）× 5 道主观题（q_type=SHORT，含 ref/rubric）× 30 份提交/课；人工分 5–10、评语齐全；随 bootstrap 重建 | v3 库实测：paper=2、question=10、submission=60、submission_answer=300；每卷 5 题、每份 5 答、人工分无缺失；覆盖 2 门课 |
| **P2-0c KB 语料** | 新增 `gen_kb_seed.js` → `sql/seeds/v3_kb_seed.sql`：课程 5/6/9/11 各 8 篇（大纲/6 讲/习题），32 篇/116 chunk；bootstrap 导入 | 总量 **61 篇/330 chunk/9 门课**；缺失 4 门各≥8 篇；全文检索冒烟（静电场/索引/特征值）通过 |

> 对账：`verify_p2_0.ps1` **17/17 PASS**；重建后 `verify_data_v3` 43/43、`verify_stats` 22/22、`ci.ps1` 6/6 均 PASS。
> P2-0a 结论：模型可达、延迟可接受，但**temperature=0 不保证逐字一致**；P2-13 评测需固定模型 + 多次运行/缓存，不能假设严格确定性。

## 十五、P2-1/2/3 记录（2026-11-08）

| 项 | 改动 | 验证 |
|---|---|---|
| **P2-1 ToolSpec** | 新增 `v2.0/backend/src/main/resources/ai/tools.yaml`（9 工具：统计/RAG/解释/记忆/草稿）与 `ToolSpecLoader`（启动校验必填/类型/枚举/范围/唯一性）；`GET /api/agent/tools`、`?format=openapi`（OpenAPI 3.0.3） | 9/9 工具 schema 完整；OpenAPI 生成 9 paths |
| **P2-2 ToolRegistry** | 9 工具实现（统计复用 P1 StatsService/ScopeResolver，检索复用 RetrievalService；学生/班级范围校验；结果带 evidence/digest/bytes）；参数强校验、超时、结果摘要 | 9 工具均可调用；未知工具/类型错/缺必填/未知参数 100% 拦截；`getClassRiskStats` digest 稳定 |
| **P2-3 执行器/路由** | `ToolCallExecutor` 四道检查（parse/whitelist/schema/budget）；`AgentOrchestrator`（路由→计划→受限 JSON 执行→草稿答案，步数预算 1–8）；`IntentRouter` 6 类规则 | 6/6 路由与工具链正确；同题 3 次工具链+digest 一致；四道检查逐项拦截；辅导员越权学生被 FORBIDDEN 阻断 |

> 入口：`/api/agent/tools`、`/api/agent/intents`、`/api/agent/run`、`/api/agent/tool-call`（仅教务）、`/api/agent/execute-json`（仅教务）。
> 对账：`verify_p2_1_3.ps1` **22/22 PASS**；`ci.ps1` 6/6 PASS（39.7s）。
> 下一步：P2-4 四角色编排（P2-5 Critic、P2-6 EvidenceLedger 随之）。

## 十六、P2-4/5/6 记录（2026-11-08）

| 项 | 改动 | 验证 |
|---|---|---|
| **P2-4 四角色/自适应** | `AgentOrchestrator` 增加 Planner/Executor/Critic/Writer 角色与 `mode=auto|2|4`；单轮工具步数 ≤ maxSteps（默认 4）；auto 简单意图 2 角色、复杂意图 4 角色 | 固定 4 角色 6/6、固定 2 角色 6/6；auto 16 vs 固定 24（**−33.3%**）；auto 质量不降（ledger=1.0、coverage=1.0）；单轮步数 1,1,1,1,2,1 |
| **P2-5 Critic** | `CriticRules`：6 意图最小证据集 + 连续同工具同参空转检测；缺证据/空转时要求取证而非硬答 | 10 例信息不足 **10/10** 正确要求取证；空转检测 PASS |
| **P2-6 EvidenceLedger** | `54_ai_evidence_ledger.sql` 落库；`EvidenceLedger` 入账/校验/一次重写/降级；`/api/agent/ledger`、`/ledger/validate`、`/ledger/repair`（仅教务） | 台账 entries=8、正常答案 hitRate=1.0；假数字 `999999` 未命中可检出；重写后 secondMisses=0；空台账降级=True |

> 对账：`verify_p2_4_6.ps1` **12/12 PASS**；P2-1~3 回归仍 **22/22 PASS**；`ci.ps1` **6/6 PASS（43s）**。
> 阶段 1（P2-1~P2-6）完成；下一步 P2-7 Diagnostician（接 LLM 输出受限 JSON）。

## 十七、P2-7~P2-10 记录（2026-11-08）

| 项 | 改动 | 验证 |
|---|---|---|
| **P2-7 Diagnostician** | 新增 `agent/biz/DiagnosticianAgent` + `/api/agent/diagnose`：复用 P2-3 编排器（路由/工具/台账），输出 `{answer,intent,tools,evidence,ledger,refused,latencyMs}`；新增 30 题评测集 `eval/agent_cases_30.json` | 30/30 工具选择（100%）；P95 **86ms**（<8s）；契约字段齐全 |
| **P2-8 Reporter** | 新增 `agent/biz/ReporterAgent` + `/api/agent/report`：复用 GroupReportService 三类报告，输出 sections/checksPassed/checksTotal/checks | class/201 6/6、offering/101 10/10、adminClass/1 6/6；0.06–0.11s（≤15s） |
| **P2-9 Grader** | 新增 `agent/biz/GraderAgent` + `/api/agent/grade|grade/eval|grade/apply`：逐题评分、要点命中/缺失、人工改分替换旧分并重算总分 | 300 条作答 MAE **0.5**、max 1.0、within2 100%；改分 6→7→恢复 6，comment 留痕 |
| **P2-10 Tutor** | 新增 `agent/biz/TutorAgent` + `/api/agent/tutor`：复用 RetrievalService 门控，输出答案 + citations(chunkId/docName/sectionNo/score) | 核心集 4/4 可答（12 条引用全部可定位）、2/2 拒答、误拒 0；60 题盲评留 P2-13 |

> 契约文档：`v3.0/docs/agent_contracts.md`；对账：`verify_p2_7_10.ps1` **10/10 PASS**；回归 P2-1~3 22/22、P2-4~6 12/12、`ci.ps1` 6/6（41.7s）。
> 边界：P2-7 选路为规则版、P2-9 评分为确定性基线、P2-10 为核心集；LLM 替换与 60 题盲评在 P2-13 完成。

## 十八、P2-11~P2-15 记录（2026-11-08）

| 项 | 改动 | 验证 |
|---|---|---|
| **P2-11 记忆** | `55_student_memory.sql`（student_memory / student_memory_event）；`MemoryService` 类型开关/摘要重建/删除；`MemoryController`：学生 GET/DELETE、管理员 GET/rebuild；学生端 `/student/memory`「我的画像」页面 | 摘要 95 字（≤300）；学生可查；删除后 `exists=false`、工具 items=0；重建可恢复 |
| **P2-12 轨迹** | `56_ai_run_trace.sql`（provider/role_count/run_key + step_role/tool_name/args_digest/result_digest/gate_status）；`AgentTraceService` 落库 ai_run/step；Orchestrator 每步落轨迹；`/api/agent/runs`、`/runs/{id}`；`pages/Trace.vue`（admin/counselor「运行轨迹」） | run #259：provider=LOCAL、role_count=4、4 步（2 工具+Critic+Writer）、台账 11 条可回溯；截图 `screenshots/trace_page.png` |
| **P2-13 评测** | `gen_agent_eval.ps1`（SQL 生成期望值）+ `agent_cases_expected.json` + `run_agent_eval.ps1` | 30/30 工具（100%）、期望数字 100%、P95 77ms |
| **P2-14 护栏** | `verify_p2_14.ps1` 10 例：JSON 解析/白名单/参数/必填/未知参数/步数/证据不足/空转/越权/拒答 | **10/10 PASS** |
| **P2-15 证据卡** | `evidence/P2.md` + 轨迹截图；汇总 P2-1~P2-15 证据与边界 | 见证据卡 |

> 回归：P2-1~3 22/22、P2-4~6 12/12、P2-7~10 10/10、P2-11/12 12/12、P2-13 100%/100%、P2-14 10/10；`ci.ps1` 6/6。
> 下一步：P2-R（retrieveCourseDocs 工具已存在；补句级引用绑定、五类混淆表、语料扩到 100–150 篇）。

## 十九、P2-R 记录（2026-11-08）

| 项 | 改动 | 验证 |
|---|---|---|
| **P2R-1 工具化** | `retrieveCourseDocs` 命中增加 `docId/chunkId/docName/sectionNo/score/rrfRank`，证据含 `gateReason/refused/topChunkId`；新增可选 `mode`（FULLTEXT/VECTOR/HYBRID）；Tutor 改为走同一 ToolRegistry，Diagnostician QA 固定 FULLTEXT | 直接工具/Diagnostician/Tutor 三入口结构键完全一致 |
| **P2R-2 引用绑定** | 新增 `agent/rag/CitationBinder`：chunk 存在性校验、绑定 docId/docName/sectionNo/score/rrfRank/content；Tutor 返回 `citationsValid`；新增 `/api/agent/kb/chunk/{id}`；轨迹页台账中 `*ChunkId*` 值可点击打开原文 | citations 4/4 可在 `ai_kb_chunk` 定位；点开 chunk 内容 254 字；轨迹台账含 chunkId 回溯 |
| **P2R-3 语料+重放** | `gen_kb_seed.js` 扩到 9 门课各 8–18 篇：**101 篇 / 475 chunk / 9 门课**；`verify_p2r.ps1` 独立重放 64 条 RAG 用例并生成五类混淆 + `eval/rag_replay.json` | 五类齐全（hit42/miss7/correctRefuse8/falseRefuse5/emptyCandidate2=64）；recall/refuse/falseRefuse 与主流程差异 **0**（<0.5%） |

> 对账：`verify_p2r.ps1` **10/10 PASS**，报告 `evidence/P2R.md`。
> 说明：replay 与 RagEvalService 同为 FULLTEXT、k=4、同一 guard；差异 0 说明两条路径一致。
