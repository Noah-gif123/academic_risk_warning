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
