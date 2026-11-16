# 学情预警与个性化学习管理系统 · v3.0 比赛提交包

本包是传智杯 AI 创新应用挑战赛的源码提交包。

## 目录

```text
backend/             Spring Boot 4 后端源码
frontend/            Vue 3 + Vite 前端源码
sql/                 建库、增量脚本与 v3 演示快照
docs/                OpenAPI、Prompt 演进、消融表、启动说明
eval/                Agent / RAG 评测用例与结果
evidence/            P0–P3 证据卡与消融原始数据
p3/                  Diagnostician Prompt、Dify 配置与 parity 材料
ai/                  RAG / Dify 说明（不含模型密钥）
docker-compose.yml   一键应用栈
.env.v3.example      环境变量模板
source-manifest.txt  生成时的 Git 提交信息
README.md            本文件
```

## 一键启动

前置：Docker Desktop。

```bash
copy .env.v3.example .env
# 可选：在 .env 里填 AI_LLM_KEY；不填则报告自动降级为规则模板

docker compose up -d --build
```

访问：

```text
前端 http://localhost:5174
后端 http://localhost:18101
```

停止：

```bash
docker compose down
```

> 首次构建需要拉取 MySQL / Gradle / Node 镜像，时间较长。

## 本机启动

前置：JDK 17、Node 18+、MySQL 8。

```bash
# 1. 导入 SQL
#    sql/ 下按 README 顺序执行，或直接导入 sql/snapshot_v3.sql

# 2. 后端
cd backend
./gradlew bootRun

# 3. 前端
cd ../frontend
npm install
npm run dev -- --port 5174
```

后端默认端口以 `backend/src/main/resources/application.yml` 为准；提交包 Docker 固定使用 `18101`。

## 演示账号

口令均为 `000000`。

| 角色 | 登录号 |
|---|---|
| 学生 | `20250001` |
| 教师 | `1001` |
| 辅导员 | `C001` |
| 教务 | `admin001` |

## 验证

```text
GET http://127.0.0.1:18101/api/ai/health
GET http://localhost:5174/index.html
```

AI Key 未配置时：

```text
enabled=true / directReady=false
报告自动使用规则模板，页面仍可用
```

配置真实 Key 后：

```text
source=LLM
model / promptVersion / tokens 会写入报告
```

## 过程证据

| 目录 | 内容 |
|---|---|
| `eval/` | Agent 30 题、RAG 回放、P3 parity、基线 |
| `evidence/` | P0–P3 证据卡、消融原始 JSON |
| `p3/` | Diagnostician Prompt、Dify 工具契约、parity 截图 |
| `docs/prompt-history.md` | Prompt 版本与变更记录 |
| `docs/ablation.md` | 因子消融与阈值敏感性表 |

## 已知边界

- 部分 Agent 保留确定性基线，真实 AI 主链路为报告写作与部分解释/答疑；
- 问卷与通知为方案 B（发布 / 列表 / 已读），不含完整答卷分析；
- 演示数据为拟真种子数据，不是真实学校试点；
- 自动化测试以单元测试 + P1 验收脚本为主，未做全页面 E2E 和并发压测；
- 生产部署还需 HTTPS、密钥管理、备份、监控和分场景限流。

## 安全

- `ai.env` 和真实 API Key 不进入提交包；
- 演示数据中的姓名、学号、手机号均为虚构；
- 课程资料仅用于教学演示。
