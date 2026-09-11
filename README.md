# 📊 学情预警系统（Academic Risk Warning System）

一个面向高校的**智能学情预警系统**，基于多维度学习数据（成绩、作业、出勤、知识掌握、历史风险），结合 **MAS 多智能体（六智能体）+ 大模型（LLM）+ RAG 知识库**，实现学业风险的自动监测、深度分析、学生画像、个性化资源推荐、干预策略动态调整，以及预警的全流程闭环管理。

---

## ✨ 功能特性

### 🎯 多维学情预警
- 融合**成绩、作业、出勤、知识掌握、历史风险**五个维度进行风险评分
- 支持**红 / 橙 / 黄**三级预警等级划分
- 预警规则按**大一 / 高年级**不同培养体系可配置（权重、阈值）
- 预警全流程闭环管理：生成 → 处理 → 干预 → 效果回测 → 闭环

### 🤖 MAS 多智能体智能分析
- **六智能体协同**：监测、分析、画像、推荐、策略、反馈
- **四套编排流水线**：完整评估、反馈闭环、端到端闭环、效果驱动调整
- **基于 LLM 的深度归因分析**与个性化干预策略建议
- **RAG 知识库**实现课程级知识隔离检索与智能答疑

### 👤 三角色权限体系
- **管理员**：系统总览、教师管理、预警规则配置、题库审核、知识库构建
- **教师**：学生学情查看、预警处理、干预记录、练习出题/批改、数据导入、AI 分析
- **学生**：个人学情、预警确认/回应、推荐练习、学习目标、AI 助手

### 📚 练习推荐系统
- 课程知识点树形管理
- 题库管理（支持 PDF/Word/图片附件、子题目、标准答案）
- 基于薄弱点自动生成个性化练习推荐
- 学生作答 + 教师子题评分

### 💬 AI 助手
- 教师端 / 学生端独立 AI 智能对话
- 基于个人学情上下文的个性化问答
- 动态推荐问题、个人风险诊断

---

## 🛠️ 技术栈

| 层次 | 技术 |
|------|------|
| **后端** | Java 17 · Spring Boot 4.0 · MyBatis-Plus 3.5 · Maven/Gradle |
| **前端** | Vue 3 · Vue Router · Tailwind CSS · Vite · GSAP · pdfjs-dist |
| **数据库** | MySQL 8 · MyBatis-Plus ORM |
| **AI 能力** | 阿里云百炼（DashScope）大模型 · RAG 知识库 · MAS 多智能体编排 |
| **其他** | Lombok · Apache POI（Excel 导入）· Spring Mail（邮件通知） |

---

## 🗂️ 项目结构

```
academic_risk_warning/
├── backend/                       # Spring Boot 后端
│   ├── src/main/java/com/example/academic_risk_warning/
│   │   ├── agent/                 # MAS 多智能体（六智能体 + 编排器）
│   │   ├── controller/            # REST 控制器（10 个）
│   │   ├── service/               # 业务服务层（21 个）
│   │   ├── config/                # 配置类
│   │   ├── common/                # 通用类（统一返回、异常处理）
│   │   └── context/               # 上下文（教师/学生会话）
│   ├── src/main/resources/        # 配置文件（application.properties）
│   ├── sql/                       # 数据库脚本
│   └── build.gradle               # Gradle 构建配置
│
├── frontend/                      # Vue 3 前端
│   └── src/
│       ├── api/                   # 接口封装
│       ├── components/            # 公共组件 + 教师端组件
│       ├── pages/                 # 页面（路由）
│       ├── composables/           # 组合式函数
│       └── router/                # 路由配置
│
├── 学情预警系统_使用说明.docx      # 使用说明文档
├── 学情预警系统_项目报告.docx      # 项目报告
└── README.md
```

---

## 🚀 快速开始

### 环境要求
- **JDK** 17+
- **Node.js** 16+
- **MySQL** 8.x
- **Maven** / **Gradle**（后端构建）

### 1. 初始化数据库

使用 **Navicat** 或命令行连接 MySQL，执行 SQL 脚本：

```bash
# 在 MySQL 中执行（数据库名：study_warning_system）
mysql -u root -p < backend/sql/study_warning_system.sql
mysql -u root -p < backend/sql/exercise_draft.sql
# 学情画像与智能体产物相关表（student_profile、student_goal、monitor_report 等 7 张）
mysql -u root -p < backend/sql/missing_tables.sql
# （可选）历史重复预警快照去重：从"按预警条数写快照"升级为"一人一课一天一条"后执行一次
mysql -u root -p < backend/sql/dedupe_alert_snapshots.sql
```

> 数据库名、账号密码可在 `backend/src/main/resources/application.properties` 中修改。
>
> 若库中已有旧版 `student_profile`（学生级、无 `course_id`），请再执行一次
> `backend/sql/migrate_student_profile_course.sql`，把画像升级为 `(学生, 课程)` 粒度。

> **预警快照口径（方案A）**：快照是"每日全量档案"——每个选了课的学生、每门课每天一条，
> **未触发预警的 GREEN 学生同样记录**（用 `is_generated_alert` 区分当天是否真的触发了预警），
> 因此风险趋势曲线对每个学生都是连续的。由每天 02:30 的定时任务生成，教师端也可手动触发
> （`POST /api/alert/teacher/generate-snapshots`，含"全体"版本）；一人一课一天只保留一条，
> 重复执行会先清理当天旧记录。
>
> **风险画像取数优先级**：风险雷达（`/api/alert/student/{id}/radar`）与风险诊断
> （`/api/agent/student/risk-profile`）都优先使用**仍有效的预警**（已撤销/已闭环不算），
> 没有预警时**自动回退到最新每日快照**，并用 `source` / `riskSource`（`ALERT` / `SNAPSHOT` / `NONE`）
> 标明来源；两者都没有时返回"无数据"而不是 0 分画像。班级均值与学生本人**同源**
> （个人走快照则班级也用快照），另用 `classAvgSource` 标明。
>
> **运行约定**：未登录 / Token 失效的请求返回**真实 HTTP 401**（响应体仍为 `{code:401,...}`），
> 前端据此自动续期并重试一次；其余业务错误保持 HTTP 200 + `body.code` 的既有约定。
> 定时任务的超时催办/预警升级按"预警 ID + 通知类型 + 标题"幂等，同一条预警的同一步只发一次通知。
>
> **智能体运行记录（W1）**：每次流水线运行会落库到 `agent_run` / `agent_run_step`
> （流水线、触发方式、状态、每步智能体、耗时、输出摘要、失败原因），
> `GET /api/agent/runs?studentId=` 查运行历史、`GET /api/agent/runs/{runId}` 查明细、
> `GET /api/agent/stats` 返回**真实统计**（成功率、各智能体失败率与平均耗时、P95）；
> 教师端「学生详情 → 智能体运行历史」卡片可直接查看。初始化数据库时执行
> `backend/sql/agent_run_tables.sql`。
>
> **智能体开关与超时**：`agent.enabled=false` 会关闭全部智能体调用（含答疑），API 返回明确提示；
> `agent.llm-timeout`（单次 LLM 调用）与 `agent.agent-timeout`（单智能体预算）同时生效，
> 实际取两者较小值作为单次调用上限。
>
> **知识库检索**：学生端 AI 助手与教师端答疑均走百炼应用 API，按 `courseId` 路由到对应课程知识库
> （`agent.bailian-app-ids.*`），学生助手的提问会带上个人学情上下文。
>
> **输出校验与反思（W2）**：每个智能体的结构化输出都会过三层校验——
> 结构（必需字段）、取值（等级/可行性枚举、百分比 0~100）、**真值一致性**
> （输出里的 `riskScore`/`predictedScore` 必须与库中真值一致，防幻觉数值）；
> 校验不通过时按 `agent.reflection-enabled` 触发 **Self-Refine 反思重写**
> （默认 1 轮：带着批评重写一版，只有"改后更合规"才采用），全过程写入
> `agent_run_step`（`validation_status` / `validation_detail` / `reflection` / `attempts` / `output_fields`）。
> `GET /api/agent/eval?live=false` 跑 **golden set 评测**（用例见
> `backend/src/main/resources/golden/agent_golden_cases.json`；`live=true` 会真实跑评估，用于论文出表），
> 教师端运行历史卡片里也有「🧪 golden set 评测」按钮。

### 2. 启动后端

```bash
cd backend
# 方式一：使用 Gradle 直接运行
./gradlew bootRun

# 方式二：打包后运行
./gradlew build
java -jar build/libs/academic_risk_warning-0.0.1-SNAPSHOT.jar
```

后端默认端口：**8080**

### 3. 启动前端

```bash
cd frontend
npm install
npm run dev
```

前端默认地址：`http://localhost:5173`

### 4. 打包一体（前端 → 后端静态资源）

```bash
cd backend
./gradlew buildAll
# 会自动构建前端并复制到后端 static 目录，之后通过后端端口访问
```

---

## 🔧 配置说明

主要配置位于 `backend/src/main/resources/application.properties`：

| 配置项 | 说明 |
|--------|------|
| `spring.datasource.*` | MySQL 连接信息（库名 `study_warning_system`） |
| `auth.token-expire-hours` | 登录 Token 有效期（默认 24 小时） |
| `warning.alert-expire-days` | 预警默认过期天数 |
| `warning.upload-dir` | 文件上传目录 |
| `warning.study-duration-thresholds` | 学习时长风险分段阈值 |
| `agent.bailian-api-key` | 阿里云百炼 API Key（**请自行配置，勿泄露**） |
| `agent.bailian-app-id` | 百炼知识库应用 ID |
| `agent.bailian-chat-model` | 百炼对话模型（qwen-plus 等） |
| `spring.mail.*` | 邮件通知配置（可选） |

> ⚠️ **安全提示**：`agent.bailian-api-key` 需替换为你自己的阿里云百炼密钥。建议通过环境变量注入，避免敏感信息入库。

---

## 🔐 默认账号

> 初始密码默认为 `123456`，请登录后及时修改。

| 角色 | 说明 |
|------|------|
| 管理员 | 账号在 `admin` 表中，初始密码 `123456` |
| 教师 | 账号在 `teacher` 表中，初始密码 `123456` |
| 学生 | 账号在 `student` 表中，初始密码 `123456` |

---

## 🧠 MAS 多智能体架构

```
                ┌─────────────────────────────┐
                │     AgentOrchestrator       │
                │      （智能体协调器）          │
                └──────┬──────────┬───────────┘
                       │          │
      ┌────────────────┴──┐   ┌───┴────────────────┐
      │  FULL 完整评估     │   │  FEEDBACK 反馈闭环  │
      │  CLOSE_LOOP 闭环   │   │  EFFECT 效果驱动    │
      └───────────────────┘   └───────────────────┘
                 │
    ┌────────────┼────────────┬────────────┬───────────┬─────────────┐
    ▼            ▼            ▼            ▼           ▼             ▼
 Monitor     Analysis     Profile     Recommend    Strategy    Feedback
监测智能体   分析智能体    画像智能体    推荐智能体    策略智能体    反馈智能体
```

| 智能体 | 职责 |
|--------|------|
| **MonitorAgent**（监测） | 并行采集 5 维度数据 + LLM 异常识别 + 生成监测报告 |
| **AnalysisAgent**（分析） | 问题根源分析、薄弱点归类、补强优先级排序 |
| **ProfileAgent**（画像） | 知识掌握 / 学习习惯 / 学习目标三维画像 |
| **RecommendAgent**（推荐） | 分阶段学习计划 + 薄弱知识点练习推荐 |
| **StrategyAgent**（策略） | 基于效果动态调整干预策略（保持/调整/升级/降级） |
| **FeedbackAgent**（反馈） | 学生反馈意图识别、RAG 答疑、关键词/情感提取 |

---

## 📊 数据库表（27 张）

- **用户/角色**：`admin`、`teacher`、`student`、`teacher_class`
- **课程学情**：`course`、`score_info`、`homework_info`、`class_performance`、`history_risk`、`knowledge_mastery`、`study_duration`、`student_course`
- **预警干预**：`alert_record`、`alert_rule_config`、`alert_snapshot`、`alert_operation_log`、`intervention_record`、`notification`、`student_weak_point`
- **练习题库**：`course_knowledge_point`、`exercise`、`exercise_knowledge_point`、`exercise_sub_question`、`exercise_sub_kp`、`exercise_recommendation`、`sub_question_answer`
- **智能体产物**：`monitor_report`、`analysis_report`、`student_profile`、`strategy_record`、`study_plan`、`student_goal`、`teacher_management_log`

---

## 📄 文档

- `学情预警系统_使用说明.docx` — 系统安装部署与使用说明
- `学情预警系统_项目报告.docx` — 项目设计实现报告

---

## 📜 License

本项目仅供学习研究使用。
