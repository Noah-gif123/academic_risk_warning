# 基于 RAG 与多智能体的学情预警与个性化学习管理系统的设计与实现（v2.0）

面向高校课程的过程性学情预警系统。教师录入考勤与成绩、学生在线作答、辅导员与教务分级处置，系统按
「**数据来源 → 风险判定 → 处置闭环**」三条链给出可解释、可复算的预警结论；大模型负责把结构化事实写成
人话，并**通过检索增强（RAG）引用课程资料作为依据**。

- **判定口径**：引擎 v2.1.0（M16），过程分与终结分分离、分段映射、四道护栏、C 通道四因子；阈值 75/60/40
- **AI 三层降级**：Dify 工作流 → 通用直连模型 → 规则模板，任一层失败自动下沉，页面永远可用
- **RAG 真实现**：全文 + 向量双路召回 + RRF 融合 + 拒答护栏，四类产出（问答/报告/解释卡/群体报告）都带「依据」
- **可观测**：`ai_run` / `ai_run_step` / `ai_usage` 记录每次调用的通道、校验结果、token 与成本

## 效果指标（评测方法见 `v2.0/ai/rag-eval.md`）

语料 213 块 / 57,506 字 / 29 份文档 / 5 门课；评测集 64 条（54 可答 + 10 不可答）。

| 检索模式 | recall@4 | MRR | nDCG@4 | 拒答准确率 | 误拒率 | 平均耗时 |
| --- | --- | --- | --- | --- | --- | --- |
| 全文（MySQL ngram） | 0.8889 | 0.8164 | 0.8349 | 0.90 | 9.3% | 1–2 ms |
| 向量（bge-m3） | 1.0000 | 0.9414 | 0.9560 | 0.10 | 0% | 42 ms |
| **混合（RRF 融合）** | **1.0000** | **0.9491** | **0.9621** | **0.90** | 9.3% | 41 ms |

> 三条结论：① 向量路补上了全文路漏掉的口语化提问（recall 0.8889 → 1.0000）；
> ② 纯向量路拒答只有 0.10，说明语义相似度不能当"有依据"的判据，必须带词面护栏；
> ③ 混合路同时拿到 1.0 召回与 0.90 拒答，是最终采用的形态。

## 目录结构

```
v2.0/                    现版本（交付内容）
  backend/               Spring Boot 4.0.6 后端（Gradle 构建）
  frontend/              Vue 3 + Vite + Tailwind 前端（四端门户）
  sql/                   建库与增量脚本 01~39（含演示数据重建与口径冻结）
  ai/                    Dify 部署、报告工作流配置、RAG 图层与评测（rag-eval.md）、语料投放目录
  tool/                  文档/图表生成脚本、验收脚本、语料入库工具（kb/KbIngest.java）
  word/                  项目文档、问题汇总、AI 优化指南、合规记录等
  HANDOFF_M16.md         判定口径改造的完整交接（公式、数据源、验收、回滚）
  剩余步骤与优化方向.md     后续工作清单（含比赛视角的短板与优先级）
  部署与运维速查.md         起服务、环境变量、建库顺序、故障排查
kb-source/               课程资料原件（按课程分目录，不入 git；由 KbIngest 解析入库）
v1.0/                    第一版归档，仅作历史留档
```

## 技术栈

| 层 | 技术 | 版本 |
| --- | --- | --- |
| 前端 | Vue 3 + Vue Router + Vite + Tailwind CSS | 3.5 / 4.6 / 4.5 / 3.4 |
| 后端 | Spring Boot（webmvc）+ MyBatis-Plus | 4.0.6 / 3.5.15 |
| 语言与构建 | Java 17 + Gradle wrapper | 17.0.8 / 9.4.1 |
| 数据库 | MySQL（驱动 mysql-connector-j） | 8.0.30 / 9.2.0 |
| 导入导出 | Apache POI（+ PDFBox 3 用于 PDF 讲义入库） | 5.2.5 |
| 认证 | 自研：PBKDF2WithHmacSHA256 + 数据库令牌 + 图形验证码 | — |
| 大模型平台 | Dify 自托管（工作流 + 知识库） | 1.18.0 |
| 向量库 / Embedding | Weaviate（Dify 内置）/ `BAAI/bge-m3`（SiliconFlow） | 1.39 / — |
| 直连模型 | DeepSeek（OpenAI 兼容） | `deepseek-flash` |

## 快速开始

1）数据库（MySQL 8.0）

```bash
mysql -uroot -p -e "create database study_warning_system_v2 default charset utf8mb4;"
# 建表：v2.0/sql 下按序执行 01 → 15
# 演示数据：19_cleanup_demo.sql → 20_rebuild_demo.sql → 21_tidy_after_rebuild.sql
#          → 25 → 26…36（AI 各表与配置）→ 跑一次判定 → 37/38/39（检索日志、评测集、旧报告归档）
```

一键重建：`powershell -NoProfile -File v2.0/tool/rebuild_demo.ps1`

2）后端（JDK 17）

```bash
cd v2.0/backend
./gradlew bootRun          # 或 gradlew build -x test 后 java -jar build/libs/*.jar
# 端口 8080；数据源见 src/main/resources/application.yml（DB_USERNAME / DB_PASSWORD 可覆盖）
```

AI 相关密钥走环境变量，不写进仓库：把 `v2.0/backend/ai.env.example` 复制成 `v2.0/backend/ai.env` 填好即可。
`application.yml` 已配置 `spring.config.import: optional:file:./ai.env[.properties]`，
所以 **IDEA 直接 Run、`gradlew bootRun`、`java -jar` 都能自动读到密钥，不必手动配环境变量**；
也可以用 `powershell -NoProfile -File v2.0/backend/run-ai.ps1`（会注入并打印脱敏日志）。
配好与否看启动日志的 `[AI] 直连通道就绪：…` 或 `/api/ai/health` 的 `directReady`。

3）前端（Node 18+）

```bash
cd v2.0/frontend && npm install && npm run dev     # http://localhost:5173，/api 代理到 127.0.0.1:8080
```

4）Dify（可选，报告要走工作流才需要）

```bash
powershell -File v2.0/ai/start-dify.ps1            # 或双击桌面「启动 Dify(8090)」
# 打开 http://localhost:8090 建应用；报告工作流的必需配置见 v2.0/ai/report-workflow.md
```

## 演示账号（口令均为 `000000`）

| 角色 | 登录号 | 说明 |
| --- | --- | --- |
| 学生 | `20250001` | 我的课程、考勤、成绩、考试、**学情预警（含申诉）**、学习评价报告、课程答疑 |
| 教师 | `1001` | 大学计算机基础（一）01 班（45 人）：考勤归档、成绩构成与批改、项目与贡献、**预警监控**、班级报告、导学与资料库 |
| 教师 | `1006` | 同课程组 02 班，演示资料库按开课实例共享 |
| 辅导员 | `C001` | 行政班重点关注、班级管理、干预记录、干预度量 |
| 教务 | `admin001` | 基础数据、排课、**规则配置**、流程与问卷、审计、AI 用量 |

> 登录带图形验证码；调试可 `--app.auth.captcha-debug=true`，再调 `GET /api/auth/captcha/debug?captchaId=`。

## 数据库概览

- 库名 `study_warning_system_v2`，**75 张表**（基础业务表 + 第三阶段 AI 表）
- 演示数据「一条主线 + 一段历史 + 一个合成样本学期」：

| 学期 | 定位 | 规模 |
| --- | --- | --- |
| 2025-2026-2（term 4） | 历史学期 | 90 条成绩（24 条不及格 → 学生挂科历史） |
| **2026-2027-1（term 5）** | **主线** | 135 学生 / 5 门课 / 12 教学班 / 540 选课 / 5535 考勤 / 540 风险状态（引擎 v2.1.0）/ 144 预警 |
| 2027-2028-1（term 8 · SYN） | 合成样本（勿混用） | 810 选课 / 9720 考勤 / 810 成绩单 |

- 风险分布：红 16 / 橙 3 / 黄 100 / 绿 421
- 成绩单：term 5 中 405 条 `PARTIAL`（期末未录，报告以**过程分**为主口径）+ 135 条 `FINAL`

## AI 部分怎么用

| 场景 | 通道 | 入口 | 依据 |
| --- | --- | --- | --- |
| 学习评价报告 | Dify 工作流 | 学生端 → 学习评价 | 第 5 段「参考依据」 |
| 预警解释卡 | 直连 | 教师端/学生端 → 预警详情 | 第 5 段「依据」 |
| 群体报告 | 直连 | 教师端 → 班级报告 | 末段「参考依据」 |
| 课程答疑 | 直连 + RAG | 学生端 → 课程答疑 | 正文末「依据：文档名 第N段」+ 逐条引用校验 |
| AI 出题 / 批改辅助 | 直连 | 教师端 | 五道闸门 / 三道校验 |

质量控制：数字白名单回验（越界定点修复）、引用逐条校验、无依据拒答、触发类型一致性闸门、卡片/报告过期标记。

## 语料入库（RAG 数据侧）

```bash
# 资料原件放仓库根 kb-source/<课程>/，支持 docx / pptx / pdf / txt / md
cd v2.0/tool/kb
java -cp ".;../../backend/build-verify-lib/*" KbIngest ../../../kb-source   # 解析 → 切块（300 字 + 60 字重叠）→ ai_kb_chunk
```

口径修正（学分/学时/考核构成）登记在 `KbIngest.CORRECTIONS`，不改用户原件，任何一次重建都得到一致语料。
语料变更后需同步到 Dify 知识库（`v2.0/ai/rag-eval.md` §6）。

## 验收与演示

| 脚本 / 接口 | 用途 |
| --- | --- |
| `v2.0/tool/verify_m3..m5.ps1` | 教师端 / 学生端 / 规则配置三步验收 |
| `v2.0/tool/demo_stage2.ps1` | 端到端演示（规则 → 判定 → 教师端 → 学生端） |
| `v2.0/tool/rebuild_demo.ps1` | 一键重建演示库（含判定与演示痕迹） |
| `POST /api/admin/ai/rag-eval?mode=HYBRID&k=4` | 检索评测（只检索不生成，不消耗 token） |
| `v2.0/tool/rag-blind-eval.ps1` | 盲测集 vs 自编集对比（回应「自编自测」质疑，见 rag-eval.md §11） |
| `v2.0/tool/kb/AblationTool.java` | 判定引擎四因子消融与阈值敏感性（离线复算，自检 540/540） |
| `GET /api/ai/health` | AI 通道自检：逐场景 DIFY/DIRECT/TEMPLATE、缺哪个 Key |
| `java -cp "build-verify;build-test;<libs>" RunTest` | 单测（`TriggerGateTest` 5 + `RiskMathTest` 20） |

## 文档索引

| 文档 | 内容 |
| --- | --- |
| `v2.0/HANDOFF_M16.md` | 判定口径 v2.1.0 的完整交接：公式、数据源、验收结果、回滚 SQL |
| `v2.0/ai/rag-eval.md` | RAG 架构、配置、评测方法、三行消融、扫参、闸门、A5 口径、语料工具 |
| `v2.0/ai/report-workflow.md` | Dify 报告工作流的最小可用配置（`max_tokens≥4096` 等踩坑记录） |
| `v2.0/剩余步骤与优化方向.md` | **后续工作清单**：证据缺口、任务优先级、加分方向、执行顺序 |
| `v2.0/部署与运维速查.md` | 起服务、环境变量、建库顺序、重启恢复、容器化、故障排查 |
| `v2.0/backend/README.md` | 后端分层、判定口径摘要、AI 通道说明 |
| `v2.0/sql/README.md` | 脚本清单与口径（含 37/38/39 的用途） |
| `v2.0/word/*.docx` | 项目文档、问题汇总、AI 后续优化指南、能力范围确认书、Docker 合规记录 |

## 部署（容器化）

```bash
cd v2.0 && docker compose -f docker-compose.app.yml build && docker compose -f docker-compose.app.yml up -d
# 后端 8080 / 前端 8081；两容器均带健康检查、资源限制与日志轮转；上传资料挂命名卷 arw-app-file-store
```

AI 平台（Dify）自托管在 8090，应用容器通过 `host.docker.internal` 访问。
合规对照与整改记录见 `v2.0/word/学情预警系统_Docker部署合规差异记录.docx`。

## 已知短板

1. **判定有效性未做纵向验证**：缺少"预测 vs 实际挂科"的混淆矩阵与 AUC（现有数据无项级历史样本），详见路线文档任务 A
2. **评测集由实现者自编**：需要盲测集交叉验证（任务 B）
3. **无真实使用痕迹**：数据为种子生成，未做教师/学生试点（任务 D）
4. **前端偏薄**：过期提示与「依据」段落尚未渲染；无仪表盘（任务 E）
5. **工程配套**：无 CI/CD；`ai_usage.prompt_tokens` 未拆分；解释卡/群体报告/问答未设日限额（任务 F）

## 说明

- 未配置 CI/CD，构建与验证靠本地脚本与人工回归清单
- 未配任何 AI Key 时：报告走规则模板、答疑拒答，页面照常可用；`AI_ENABLED=false` 时全站不调用外部服务
- 演示数据中的姓名、手机号等均为虚构，不对应真实个人信息；课程资料为教学用示例材料
