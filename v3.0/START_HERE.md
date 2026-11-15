# v3.0 启动与复现入口

> 给评委/新机器用的最短路径。数据库已包含在 `v3.0/sql/snapshot_v3.sql`。

## A. Docker 一键（推荐）

前置：Docker Desktop。

```bash
# 仓库根目录
copy v3.0\.env.v3.example v3.0\.env
# 在 v3.0\.env 里填 AI_LLM_KEY（不填也能启动，报告走规则模板）

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

## B. 本机一键（已有 JDK 17 / Node / MySQL）

```powershell
# 数据库只初始化一次（或使用 snapshot_v3.sql 导入）
powershell -NoProfile -ExecutionPolicy Bypass -File v3.0/tool/run_v3_pipeline.ps1

# 启动（真实 AI 用 -EnableAi；没有 Key 会自动回退模板）
powershell -NoProfile -ExecutionPolicy Bypass -File v3.0/tool/start_v3.ps1 -EnableAi -DebugCaptcha

# 停止
powershell -NoProfile -ExecutionPolicy Bypass -File v3.0/tool/stop_v3.ps1
```

## 演示账号

```text
学生   20250001 / 000000
教师   1001     / 000000
辅导员 C001     / 000000
教务   admin001 / 000000
```

## 健康检查

```text
GET http://127.0.0.1:18101/api/ai/health
GET http://localhost:5174/index.html
```

`/api/ai/health` 返回 `directReady=true` 时，真实 AI 报告可用；否则系统仍可用，报告自动降级为规则模板。

## 关键复现点

- 报告事实包：`ai_group_report.fact_json`
- 报告章节：`ai_group_report.content_json`
- Agent 执行：`ai_run` / `ai_run_step`
- Prompt/模型：`ai_group_report.prompt_version` / `model`
- 数字对账：报告接口 `checks` 字段
- 学生学期报告：`STUDENT_TERM` scope，支持 latest / history / PDF
