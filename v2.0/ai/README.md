# 第三阶段 · AI 服务（Dify 自托管）

本目录放"AI 侧"的部署与配置，**不放业务代码**。业务代码在 `v2.0/backend`，通过
`/api/ai/*` 代理到 Dify，密钥只在后端。

## 1. 目录约定

```
v2.0/ai/
  dify/                 # Dify 1.18.0 源码（不入库，见 .gitignore）
    docker/             # 官方 compose + 我们的 .env / override
  dify-workflows/       # 三条工作流的 DSL 导出（入库，用于重建与版本化）
    report-student.yml  # 学生端学习评价报告（M3）
    question-gen.yml    # 课件出题（M2）
    course-qa.yml       # 课程问答（M4）
```

## 2. 端口规划（与现有栈零冲突）

| 宿主端口 | 服务 | 说明 |
| --- | --- | --- |
| 3306 | 宿主 MySQL | 业务库，不动 |
| 8080 | Spring Boot | 业务后端，不动 |
| 5173 | 前端 dev | 不动 |
| **8090** | dify-nginx | `EXPOSE_NGINX_PORT=8090` |
| **8443** | dify-nginx HTTPS | 不用 HTTPS 可删 |
| 5003 | plugin_daemon（调试） | 仅调试插件用 |

Dify 的 Postgres / Redis / Weaviate / Sandbox **均不发布宿主端口**（实测官方 compose 只有
nginx 与 plugin_daemon 有 `ports`），所以不会和 8080 / 3306 抢。

## 3. 首次部署

```powershell
# 0) Docker Desktop 内存建议 8 GiB（WSL2 后端）
# 1) 取源码（任一方式）
#    a. git clone --depth 1 --branch 1.18.0 https://github.com/langgenius/dify.git
#    b. 下 tarball: https://codeload.github.com/langgenius/dify/tar.gz/refs/tags/1.18.0
# 2) 配置
cd v2.0/ai/dify/docker
copy .env.example .env
# 必改：EXPOSE_NGINX_PORT=8090、EXPOSE_NGINX_SSL_PORT=8443、FILES_URL=http://localhost:8090、
#       SECRET_KEY=<随机 48 位>、PLUGIN_DAEMON_KEY=<随机 40 位>
# 3) 起容器（首次会拉 8 个镜像，约 6~8 GB）
docker compose up -d
docker compose ps      # 期望 nginx / api / worker / web / db_postgres / redis / weaviate / sandbox 全 Up
```

验证：浏览器打开 `http://localhost:8090` 进入初始化页。

## 4. 初始化（命令行即可完成，也可用浏览器）

```powershell
# 建管理员（仅首次）
$body = @{ email='admin@campus.local'; name='admin'; password='Dify@2026demo' } | ConvertTo-Json
Invoke-RestMethod -Uri 'http://localhost:8090/console/api/setup' -Method Post -ContentType 'application/json' -Body $body

# 登录（注意：password 必须传 base64）
$pw = [Convert]::ToBase64String([Text.Encoding]::UTF8.GetBytes('Dify@2026demo'))
$null = Invoke-WebRequest -Uri 'http://localhost:8090/console/api/login' -Method Post -ContentType 'application/json' `
  -Body (@{ email='admin@campus.local'; password=$pw; language='zh-Hans'; remember_me=$true } | ConvertTo-Json) -SessionVariable sess
# 之后所有 console API 都要：-WebSession $sess -Headers @{ 'X-CSRF-Token' = <csrf_token cookie 值> }
```

要点（踩过的坑）：

1. **登录密码必须 base64**，直接传明文返回 `Invalid encrypted data`；
2. console API 鉴权 = 会话 Cookie + `X-CSRF-Token` 请求头，两者都要，单独用 Bearer 会报 CSRF 缺失；
3. 登录成功体里没有 `access_token` 字段，token 在 Set-Cookie 的 `access_token` 里。

## 5. 模型接入

两条路都行，**任选其一**填 Key 即可；没配 Key 的供应商放着不动，工作流只用它节点里指定的模型。

### 路线一：DeepSeek（国内直连，便宜）

| 配置 | 值 |
| --- | --- |
| 供应商 | DeepSeek（插件 `langgenius/deepseek`，Dify 设置里安装） |
| API Key | platform.deepseek.com → API keys 生成的 `sk-...` |
| Base URL | 不用填（插件内置 `https://api.deepseek.com`） |
| 模型 | 插件提供 4 个：`deepseek-flash`、`deepseek-v4-flash`、`deepseek-v4-pro`、`deepseek-v4-flash-vision-exp`（本项目用 **`deepseek-v4-flash`**，上下文 1M） |

> 实测说明：DeepSeek 的 v4 是**推理模型**，会把思考过程写在 `<think>…</think>` 里一起返回。后端 `ReportComposer.stripReasoning()` 会先剥掉推理块再解析，所以正文里不会出现思考内容；Dify 的「运行」面板里仍能看到（属于平台侧展示，不影响业务）。

> 本项目已经把报告工作流的 LLM 节点改成 `DeepSeek / deepseek-v4-flash` 并发布（版本 `v3-prompt`），后端 `AI_REPORT_APP_KEY` 一配即走 Dify。

### 路线二：DashScope（阿里云百炼）

Dify 里"设置 → 模型供应商 → 添加 OpenAI-API-compatible"：

| 配置 | 值 |
| --- | --- |
| Base URL | `https://dashscope.aliyuncs.com/compatible-mode/v1` |
| API Key | DashScope Key（v1.0 那把仍有效，已实测） |
| 模型 | 出题/批改用 qwen-max，报告用 qwen-plus，去重用 text-embedding 系列 |


> 若模型供应商插件需要从 marketplace 安装而网络不稳，后端已内置 **DashScope 直连通道**：
> 把 `AI_DASHSCOPE_KEY` 配好即可先跑通报告，Dify 工作流配好后再把 `AI_REPORT_APP_KEY` 填上，
> 后端会优先走 Dify（`ai.source=DIFY`），失败自动回退直连或模板。

## 6. 后端开关（环境变量）

| 变量 | 默认 | 说明 |
| --- | --- | --- |
| `AI_ENABLED` | true | false 时全站 AI 入口回退模板，不调用任何外部服务 |
| `AI_DIFY_BASE_URL` | http://localhost:8090 | Dify 地址 |
| `AI_REPORT_APP_KEY` | 空 | 报告工作流 App API Key（配了优先走 Dify） |
| `AI_DASHSCOPE_KEY` | 空 | 备用直连通道 Key |
| `AI_QUESTION_APP_KEY` / `AI_CHAT_APP_KEY` | 空 | 出题 / 问答（M2、M4 用） |

体检：`curl http://localhost:8080/api/ai/health`（免登录）→ 看到 `reachable` / `reportWorkflowReady` / `usage`。

## 7. 备份与升级

- 备份三件套：`docker/volumes/` 整目录、`pg_dump`、三条工作流 DSL（本目录 `dify-workflows/`）；
- 升级：固定 tag（如 1.18.0，**不要 latest/main**）→ 先导出 DSL 与数据库备份 → 拉新镜像 → `up -d` → 冒烟；
- 离线：有网机器 `docker save` 上述镜像，内网 `docker load` 后 `up -d`。

## 8. 重启电脑后怎么恢复（最常见的问题）

**结论：Dify 是 Docker 里的服务，电脑重启后 Docker Desktop 不会自己起来（除非你开了开机自启），所以 8090 一定打不开。**
按顺序把三样东西拉起来即可：

| 顺序 | 依赖 | 怎么起 | 验证 |
| --- | --- | --- | --- |
| 1 | Docker Desktop | 开始菜单点「Docker Desktop」，等托盘图标变绿 | `docker info` 有版本号 |
| 2 | Dify 全家桶 | `v2.0/ai/start-dify.ps1`（推荐）或 `cd v2.0/ai/dify/docker && docker compose up -d` | 浏览器打开 http://localhost:8090 |
| 3 | 业务后端 + 前端 | IDEA 起 Spring Boot（8080）+ Vite（5173） | `curl http://localhost:8080/api/ai/health` |

### 8.1 一键脚本

```powershell
powershell -ExecutionPolicy Bypass -File E:\JavaDEVIDEA\workspace\JavaProject\academic_risk_warning\v2.0\ai\start-dify.ps1
```

脚本做四件事：引擎没起就帮你起 Docker Desktop 并等待 → `docker compose up -d` → 停掉两个可选容器（省内存）→ 等 8090 返回 200 并打印所有容器状态。

桌面上已放了一个快捷方式「启动 Dify(8090)」，双击即可（会弹一个 PowerShell 窗口显示进度）。

### 8.2 让 Docker Desktop 开机自启（可选，省掉第 1 步）

Docker Desktop → 右上齿轮「Settings」→「General」→ 勾选 **Start Docker Desktop when you sign in to your computer** → Apply。

容器本身已经是 `restart: always`，所以只要 Docker 引擎起来，Dify 会自动跟着起来，脚本里的 `compose up -d` 只是兜底。

### 8.3 现象对照

| 现象 | 原因 | 处理 |
| --- | --- | --- |
| 8090 拒绝连接 | Docker 没起 / Dify 没起 | 跑 8.1 的脚本 |
| `docker ps` 报 `dockerDesktopLinuxEngine` 管道不存在 | Docker Desktop 没启动 | 同上 |
| 容器 `Restarting` 一直循环 | 多半是可选容器（agent_backend / local_sandbox） | 脚本已自动停掉；手动：`docker compose stop agent_backend local_sandbox` |
| 8090 转圈很久 | 首次启动要等 postgres 初始化 | 等 1–2 分钟；`docker compose logs api` 看进度 |
| 浏览器能开 8090，但后端 `/api/ai/health` 显示 `reachable=false` | 后端比 Dify 先起，或 `AI_DIFY_BASE_URL` 指错 | 重启后端；确认变量是 `http://localhost:8090` |
| 8090 能开但登录提示密码错 | 用了明文密码（API 才要 base64，UI 不用） | UI 登录直接输 `Dify@2026demo` |
| `model-providers` 一直是 0（已装插件） | 插件安装任务没走到"激活" | UI 里卸载重装一次，见 v2.0/word/学情预警系统_AI功能测试与Dify配置指南.docx 第 2 章 |
> **用过 DeepSeek 后要在工作流里改一行**：工作室 → 打开报告工作流 → 点 LLM 节点 → 模型改成
> `DeepSeek / deepseek-chat` → 右上「发布」。后端与接口都不用动。

> **后端直连通道也支持 DeepSeek**（Dify 挂了时的兜底）：设
> `AI_LLM_BASE_URL=https://api.deepseek.com/v1`、`AI_LLM_KEY=sk-...`、`AI_LLM_MODEL=deepseek-chat`
> 即可；三项留空则回退旧的 `AI_DASHSCOPE_*` 配置。

## 9. 应用容器化（与 Dify 分开编排）

我们自己的后端与前端有自己的镜像与 compose，放在 `v2.0/` 下，**不与 Dify 的 compose 混在一起**：

| 文件 | 作用 |
| --- | --- |
| `v2.0/backend/Dockerfile` | 后端多阶段构建（gradle 构建 → JRE 运行），非 root、HEALTHCHECK 指向 `/api/ai/health`、`MaxRAMPercentage=75` |
| `v2.0/backend/.dockerignore` | 剔除 build/.gradle/.git/file-store 等 |
| `v2.0/frontend/Dockerfile` + `nginx.conf` | 前端 Vite 构建 → nginx 托管 dist，并把 `/api` 反代到 `app:8080` |
| `v2.0/docker-compose.app.yml` | app + web 两个服务：mem_limit、日志轮转、健康检查、命名卷、自定义网络 |

```bash
cd v2.0
docker compose -f docker-compose.app.yml build
docker compose -f docker-compose.app.yml up -d
docker compose -f docker-compose.app.yml ps      # 期望两个都 healthy
```

宿主的 MySQL(3306) 与 Dify(8090) 通过 `host.docker.internal` 访问；密钥用环境变量注入（`AI_LLM_KEY`、`AI_REPORT_APP_KEY`）。

> **端口冲突提示**：容器占 8080，用 IDEA 直跑后端前先 `docker compose -f docker-compose.app.yml down`。

### 9.1 Dify 的 override 副本（可版本化）

`v2.0/ai/dify/` 整个目录被 `.gitignore` 忽略（体积大、按需拉取），但我们在里面改过 override
（固定 nginx / squid 镜像标签、给 api/worker 加 host.docker.internal）。
所以在这里存一份**可追踪的副本**：`v2.0/ai/dify-overrides/docker-compose.override.yaml`，
重新拉取 Dify 源码后把它复制到 `v2.0/ai/dify/docker/docker-compose.override.yaml` 即可。
## 10. 课程知识库与课件入库（M11）

RAG 的"R"这一步没有引入第二套向量库，直接用 MySQL 的 ngram 全文索引（`ai_kb_chunk` 的 `FULLTEXT ... WITH PARSER ngram`），
引用契约（文档名 + 第几段 + 课件第几页）与向量方案一致，将来换向量检索只改 `KbService.retrieve()`。

| 环节 | 位置 | 说明 |
| --- | --- | --- |
| 抽文本 | `service/ai/KbExtractor` | 支持 `txt/md/csv/docx/pptx`；txt 先按 UTF-8 严格解码，失败再按 GBK；PPTX 一页一块、块首 `【第N页】`，顺带收讲稿备注；DOCX 按原文顺序拍平段落与表格（行用 `|` 分隔） |
| 入库 | `KbService.syncCourse(courseId)` | 导学 + 资料；内容 hash 未变不重写；**资料作废/取消发布后，下次入库自动下线对应分块**（返回值 `purged`） |
| 上传即入库 | `ResourceFileService.store` | 教师上传课件后立刻单份入库，失败只告警、不影响上传 |
| 检索 | `KbService.retrieve` | ngram 打分排序，返回 `docName / sectionNo / pageNo / score`；超短查询零召回时回退 LIKE |
| 生成与拒答 | `StudentQaService` | 只依据命中片段作答，答案末尾必须给「依据：文档名 第N段」，后端逐条校验；检索为空 / 相关度低于 0.10 / **词面支撑度低于 0.10** 都直接拒答 |

课件演示数据（可选）：`v2.0/tool/courseware` 下的生成器造一份 9 页 PPTX 与一份 6 章 DOCX，
再用 `sql/32_seed_courseware.sql` 挂到教学班 201，最后触发一次 `POST /api/teacher/ai/kb/sync?courseId=1`。

实测（课程 1）：知识库 7 份文档 / 20 个分块；「VLOOKUP 返回 #N/A 是什么原因」→ 命中课件第 4 页（相关度 23.08、词面 47%），
答案 3 条要点 + 引用两条；「实验二的评分标准是什么」→ 命中评分标准表（词面 50%）；「帮我写一份入党申请书」→ 零命中直接拒答。