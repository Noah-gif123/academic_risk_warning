# v3.0 交接说明（HANDOFF）

> 用途：新开会话时先读这一份 + `v3.0/PLAN_v3.0.md` + `v3.0/word/v3.0规划清单.docx`，即可直接开工，无需回溯长对话。
> 更新：v3.0 规划定稿、尚未写任何 v3.0 代码。

---

## 一、当前状态（一句话）

**v2.0 已定稿并发布（GitHub Release v2.0.0，含 tag 与文档包）；v3.0 只完成了规划与目录骨架，代码零改动。**

| 项 | 状态 |
|---|---|
| v2.0 代码/数据 | 已完成，别再改（用户明确要求） |
| v2.0 tag `v2.0.0` | 指向 `360c920`，**不要动** |
| v2.0 Release | GitHub 上已发布 3 个（v1.0.0 / v1.1.0 / v2.0.0），**不要动** |
| v3.0 规划 | 已完成：`v3.0/PLAN_v3.0.md`（终版 v3.0.4）+ `v3.0/word/v3.0规划清单.docx`（53 项 / 46.5 人日） |
| v3.0 目录 | 已建骨架：backend / frontend / ai(dify) / sql(seeds) / tool / word / eval / evidence |
| v3.0 代码 | **未开始** |

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

## 四、开工前必须知道的既有事实（已核实）

这些是 v3.0 P0 要处理的真实问题，新会话不要重新"发现"：

| 事实 | 证据 |
|---|---|
| `course_fail_rate_history` 5 行，但 **`course_factor` 全 NULL、`is_initialized=0`** | 直接查库 |
| `teacher_style` 6 行，**`shrink_lambda` 全 0.620、`sample_size` 全 45**（收缩"假生效"） | 直接查库 |
| `risk_state` 540 行；**524 行走 C 通道、16 行走 A 通道**；`delta_used` 全部非 0、`course_factor_used` 全部 ≠1 | 直接查库 |
| 班间差异极小：CS-01-A avg 30.9 / CS-02-A avg 32.0；cs-01-D 81.8 / 02-D 79.9 / RT-D 81.7；班内 sd 4–8 | 直接查库 |
| 135 名学生中仅 1 人叫「学生1」（20250001），其余为拟真姓名 | 直接查库 |
| 9 名教师最多 2 门课、最多 4 个班（1001 只 1 个班） | 直接查库 |
| 前端**没有图表库**（package.json 无 echarts） | 查 package.json |
| 现有统计只有零散 overview / coverage 接口，无独立统计模块 | 查 controller |

## 五、下一步：P0 数据底座（约 6 人日）

按 `v3.0规划清单.docx` 的 P0-1 … P0-8 执行，顺序：

1. `v3.0/sql/snapshot_v2.0.sql`（先能回滚，再动数据）
2. `course_factor` 初始化：`clamp(1 + k*(fail_rate - baseline), 0.90, 1.15)`，写回 `is_initialized=1`；引擎读 NULL 时按 1.0 + WARN
3. `λ = n/(n+k)` 随样本量生效 + 造 1–2 个样本 <10 的熔断案例（`fused=1` + 原因）
4. 拟真数据生成器（教师 12–15 / 课程 8–10 / 班 6–8 / 3 学期 / 对照组 60:40），参数化 + seed 可复现
5. 重算流水线 `v3.0/tool/run_v3_pipeline.ps1`：seed → 判定 → 报告 → 快照 → 基线，一条命令 <3 分钟
6. 数据校验脚本 + `v3.0/sql/seeds/data_story.md`
7. 重跑 C1/C2 消融 → `v3.0/evidence/P0.md`（修前 vs 修后对照）
8. `v3.0/tool/ci.ps1` 骨架（编译 / 单测 / 数据校验 / 报告对账）

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
```

- 数据库：`study_warning_system_v2`，`root/root`
- 演示账号（口令 `000000`）：学生 `20250001`、教师 `1001`（教研 201）、`1004`（数据结构 3 个班）、辅导员 `C001`、教务 `admin001`
- PS 5.1 注意：含中文的 `.ps1` 必须存为 **UTF-8 带 BOM**；`.NET` 读写中文路径用绝对路径（`Set-Location` 不影响 .NET 的相对解析）

## 七、如果新会话只做一件事

先做 **P0-2（course_factor 初始化）**：它是 C 通道、课程对比、校准页、消融实验的共同前置，改动小、收益立竿见影。
