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
