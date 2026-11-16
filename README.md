# 基于 RAG 与多智能体的学情预警与个性化学习管理系统

面向高校课程过程性学情的预警、研判与干预系统。

系统按「**数据来源 → 风险判定 → 处置闭环**」组织业务，覆盖学生、教师、辅导员、教务四端；大模型负责在结构化事实之上生成可追溯的学情报告与解释，RAG 负责课程资料依据，规则引擎负责数字与等级判定。

当前主版本为 **v3.0**，面向 **传智杯 AI 创新应用挑战赛** 的演示与复现。

---

## 1. 当前状态

```text
版本：v3.0
前端：Vue 3 + Vite，默认 5174
后端：Spring Boot 4 + Java 17，默认 18101
数据库：MySQL，study_warning_system_v3
AI：Dify / OpenAI 兼容直连 / 规则模板三层降级
```

已完成：

- 四端业务闭环
- 教学班 / 课程 / 行政班 / 学生学期四类报告
- 报告落库、历史、PDF
- 真实 AI 报告链路（`source=LLM`）
- 事实包与数字对账
- 权限矩阵与越权拦截
- P1 异常收口与验收脚本
- P2 视觉、错误边界、可访问性与图表拆包
- v3 一键启动与 Docker 编排

> 详细启动见 [v3.0/START_HERE.md](v3.0/START_HERE.md)。  
> 历史 v2.0 说明保留在 [v2.0/README.md](v2.0/README.md)。

---

## 2. 系统解决什么问题

传统学情预警常见三个问题：

1. **只看分数**：缺少考勤、作业、项目、课堂表现等过程性数据。
2. **只给名单**：教师和辅导员知道“谁有风险”，但不知道“为什么”和“下一步做什么”。
3. **报告不可信**：大模型容易编数字，报告无法回溯到事实与依据。

本系统的处理方式是：

```text
规则引擎算事实
→ Agent 组织研判与写作
→ RAG 提供课程资料依据
→ 数字对账校验报告
→ 人工干预与复评闭环
```

核心原则：

> 数字由规则算，模型负责表达；结论必须能回溯到事实。

---

## 3. 四端功能

### 学生端

- 我的课程、考勤、成绩、考试
- 学情预警与预警解释
- 学习评价报告
- 个性化学习计划
- 课程答疑（RAG + 引用 + 无依据拒答）
- 问卷与个人中心

### 教师端

- 教学驾驶舱
- 课程、教学班、学生名单
- 考勤、成绩、测验、作业、项目
- 成绩构成与批改
- 预警监控与处置
- 教学班报告、课程总体报告、学生报告
- 课程内容、资料库、AI 出题辅助

### 辅导员端

- 首页看板、预警总览
- 行政班管理
- 学生学期汇总报告（一个学生 × 当前学期全部课程）
- 干预记录与行内处理
- 统计看板
- 问卷发布、系统通知

### 教务端

- 教务工作台
- 统计看板
- 基础数据、排课与教学班
- 成绩构成审批与锁定
- 考勤归档、覆盖率督办
- 规则版本配置
- 流程与问卷、审计日志
- AI 运行状态与 RAG 评测

教务端**不提供学情报告入口**，学情报告只服务于学生、教师、辅导员三端。

---

## 4. 报告体系

| 报告 | 视角 | 生成位置 | 主要用途 |
|---|---|---|---|
| 教学班报告 | 一门课 × 一个教学班 | 教师端 | 班级风险、成绩、重点学生、教学动作 |
| 课程报告 | 一门课 × 多个教学班 | 教师端 | 教学班对比、课程整体风险 |
| 行政班报告 | 一个行政班 × 多门课 | 辅导员端 | 集中风险课程、多门预警学生、干预动作 |
| 学生学期汇总 | 一个学生 × 当前学期全部课程 | 辅导员端 | 课程总览、考勤、风险、干预、动作清单 |

四类报告统一具备：

- 事实包快照 `fact_json`
- 章节内容 `content_json`
- 来源 `source`（DIFY / LLM / TEMPLATE）
- 模型与 Prompt 版本
- token 用量
- 数字对账 `checks`
- 历史与 PDF

学生学期报告使用 `STUDENT_TERM` scope，**不复用学生单课程报告**。

---

## 5. AI 设计

### 5.1 三层降级

```text
Dify 工作流
→ OpenAI 兼容直连模型
→ 规则模板
```

任一层失败自动下沉，页面始终可用。

健康检查：

```text
GET /api/ai/health
```

返回关键字段：

```json
{
  "enabled": true,
  "directReady": true,
  "scenes": {
    "report": "LLM",
    "groupReport": "LLM",
    "qa": "TEMPLATE"
  }
}
```

### 5.2 事实包与数字对账

报告生成前先构建事实包，记录：

- 学生数、红黄绿人数
- 课程数、选课记录
- 平均分、最低分、最高分
- 缺课、迟到、请假
- 课程级风险率
- 干预记录

报告生成后由 `NumberAuditor` 抽取正文关键数字，与事实包逐项比对。

### 5.3 可验证的多角色编排

系统使用可验证的多角色 LLM 编排，而不是宣称完全自主协商：

```text
数据体检 → 事实汇总 → 学情研判 → RAG 取证
→ 报告写作 → 数字对账 → 人工处置
```

设计约束：

- 工具白名单
- 步骤预算
- 证据台账
- 引用必须可定位
- 数字必须命中事实
- 失败可降级、可追踪

> 当前实际落地的真实 AI 主链路是报告写作；部分 Agent（如 Diagnostician / Grader / Tutor）仍保留确定性基线实现。参赛与答辩时应表述为“可验证的多角色编排”，不夸大为所有 Agent 完全自主决策。

### 5.4 运行轨迹

- `ai_run` / `ai_run_step`
- 报告 `source` / `model` / `prompt_version` / `tokens`
- 失败原因与降级链路
- 管理端「AI 运行状态」卡

---

## 6. RAG 与评测

RAG 采用全文 + 向量双路召回 + RRF 融合，引用带 `chunkId`，可回查课程资料。

评测资产：

- RAG 评测：`v2.0/ai/rag-eval.md`
- Agent 用例：`v3.0/eval/agent_cases_30.json`
- Agent 评测报告：`v3.0/eval/agent_eval_report.json`
- P3 parity：`v3.0/eval/p3_parity.json`
- 证据卡：`v3.0/evidence/`

评测数字请结合对应证据卡理解，不要把规则基线评测当成真实 LLM 全链路效果。

---

## 7. 快速启动

### 方式 A：Docker 一键栈（推荐给评委）

前置：Docker Desktop。

```bash
# 仓库根目录
copy v3.0\.env.v3.example v3.0\.env

# 可选：在 v3.0\.env 填真实 AI Key
docker compose -f v3.0/docker-compose.v3.yml up -d --build
```

访问：

```text
前端 http://localhost:5174
后端 http://localhost:18101
```

停止：

```bash
docker compose -f v3.0/docker-compose.v3.yml down
```

说明：

- MySQL 初始化使用 `v3.0/sql/snapshot_v3.sql`
- 不填 AI Key 也能启动，报告自动降级为模板
- Docker 首次构建需要拉取镜像，时间较长

### 方式 B：本机启动

前置：

```text
JDK 17
Node 18+
MySQL 8
```

数据库初始化（只做一次）：

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File v3.0/tool/run_v3_pipeline.ps1
```

编译后端：

```powershell
cd v2.0/backend
.\gradlew.bat compileJava
```

启动：

```powershell
# 仓库根目录
powershell -NoProfile -ExecutionPolicy Bypass -File v3.0/tool/start_v3.ps1 -EnableAi -DebugCaptcha
```

停止：

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File v3.0/tool/stop_v3.ps1
```

---

## 8. 演示账号

口令均为 `000000`。

| 角色 | 登录号 | 说明 |
|---|---|---|
| 学生 | `20250001` | 课程、预警、学习计划、答疑 |
| 教师 | `1001` | 课程、预警、报告、成绩、考勤 |
| 辅导员 | `C001` | 行政班、预警、学生学期报告、干预 |
| 教务 | `admin001` | 基础数据、排课、规则、归档、审计 |

登录带图形验证码。本地联调可加 `--app.auth.captcha-debug=true`，或使用 `start_v3.ps1 -DebugCaptcha`。

---

## 9. 验收命令

### P1 集成验收

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File v3.0/tool/verify_p1.ps1
```

覆盖：

- 四端登录
- 越权拦截
- 教师 / 辅导员本人报告
- 学生学期报告生成
- 学生学期报告 PDF
- 问卷方案 B
- 系统通知

### P1 + 真实 AI

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File v3.0/tool/verify_p1.ps1 -CheckLiveAi
```

### 单元测试

```powershell
cd v2.0/backend
.\gradlew.bat test
```

当前包含 `NumberAuditorTest` 等数字对账测试。

### 前端构建

```powershell
cd v2.0/frontend
npm run build
```

### CI

GitHub Actions 配置：

```text
.github/workflows/ci.yml
```

后端编译 + 单测，前端安装 + 构建。

---

## 10. 接口契约

核心业务接口的 OpenAPI 3.0 契约：

```text
v3.0/docs/openapi.yaml
```

包括：

- 登录 / 验证码
- AI 健康检查
- 教学班报告
- 行政班报告
- 学生学期报告（生成 / 最新 / 历史 / PDF）

---

## 11. 目录结构

```text
academic_risk_warning/
├─ v2.0/
│  ├─ backend/             Spring Boot 后端
│  ├─ frontend/            Vue 3 前端
│  ├─ ai/                  Dify / RAG 配置与评测
│  ├─ sql/                 建库与增量脚本
│  └─ tool/                构建与验收脚本
├─ v3.0/
│  ├─ tool/                v3 启动、流水线、P1 验收、CI
│  ├─ sql/                 v3 快照与增量 SQL
│  ├─ eval/                Agent / RAG 评测资产
│  ├─ evidence/            P0–P3 证据卡
│  ├─ docs/                OpenAPI 与口径文档
│  ├─ START_HERE.md        赛前最短启动路径
│  └─ docker-compose.v3.yml
├─ .github/workflows/ci.yml
├─ kb-source/              课程资料原件（不入库）
└─ README.md
```

---

## 12. 已知边界

这些边界是系统当前的真实状态，不应在比赛材料中过度承诺。

1. **部分 Agent 仍是确定性基线**
   - 真实 AI 主链路是报告写作与部分解释/答疑。
   - Diagnostician / Grader / Tutor 仍有规则或模板实现。

2. **问卷和通知按方案 B 收口**
   - 已有发布、列表、已答人数、已读。
   - 没有完整答卷分析、通知群发。

3. **数据为拟真种子数据**
   - 不是真实学校试点数据。
   - 干预效果属于准实验，不是 RCT。

4. **自动化测试覆盖仍有限**
   - 有单元测试和 P1 集成脚本。
   - 尚无全页面 E2E 和并发压测。

5. **Docker 已在本机完成构建与启动验证**
   - 已验证 MySQL、后端、前端三个容器 healthy，四端登录、教学班报告、学生学期报告、PDF 可用。
   - 仍建议在目标比赛机器上首次重新构建一次，确认网络与 Docker 环境差异。

6. **生产级安全与运维未完成**
   - 比赛级已有角色权限、令牌、验证码。
   - 生产还需 HTTPS、密钥管理、备份恢复、分场景限流和监控告警。

---

## 13. 比赛演示建议

建议 3–5 分钟按以下顺序演示：

```text
教师查看预警
→ 生成真实 AI 教学班报告
→ 展示来源 / 模型 / Prompt / 数字对账
→ 辅导员查看学生学期汇总报告
→ 记录一次干预
→ 教务查看统计与规则版本
```

不建议从登录页开始逐页浏览。

赛前重点准备：

- 一次真实 LLM 报告录屏
- Agent trace / 数字对账截图
- 四端首页截图
- 一份 PDF
- 一键启动证明
- 创新点对比页

---

## 14. 相关文档

| 文档 | 内容 |
|---|---|
| [v3.0/START_HERE.md](v3.0/START_HERE.md) | 赛前最短启动与验收路径 |
| [v3.0/docs/openapi.yaml](v3.0/docs/openapi.yaml) | 核心接口契约 |
| [v3.0/tool/verify_p1.ps1](v3.0/tool/verify_p1.ps1) | P1 集成验收 |
| [v2.0/ai/rag-eval.md](v2.0/ai/rag-eval.md) | RAG 架构与评测 |
| [v3.0/evidence/P3.md](v3.0/evidence/P3.md) | Dify Diagnostician 证据 |
| [v2.0/README.md](v2.0/README.md) | 历史 v2.0 说明 |

---

## 15. 说明

- 演示数据中的姓名、学号、手机号等均为虚构。
- 课程资料仅用于教学演示。
- 未配置 AI Key 时系统仍可启动，报告自动降级为规则模板。
- 本仓库不再维护旧的 v2 端口 / 数据库口径；当前以 v3.0 为准。
