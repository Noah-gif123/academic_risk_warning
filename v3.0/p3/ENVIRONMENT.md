# P3 环境与启动说明

> 本文只写启动方式，不提交任何密钥；Dify 应用与工具配置已保存在本机 Dify 数据卷中。

## 1. 启动 Dify

```powershell
cd v2.0/ai/dify/docker
$env:SSRF_PROXY_ALLOW_PRIVATE_IPS = '192.168.0.0/16'
$env:SSRF_PROXY_ALLOW_PRIVATE_DOMAINS = 'host.docker.internal'
docker compose up -d
```

确认：

```powershell
Invoke-WebRequest http://127.0.0.1:8090/console/api/setup -UseBasicParsing
```

Dify 1.18 控制台：`http://127.0.0.1:8090`。应用 `Diagnostician P3` 已配置：

- 8 个只读工具：`campus-agent-tools`；
- 共享 prompt：`v3.0/p3/diagnostician-system-prompt.md`；
- 模型：`deepseek-v4-flash`；
- 工具鉴权：Header `X-Dify-Token`，值与环境变量 `AI_DIFY_TOOL_TOKEN` 一致；
- `draftIntervention` 不在工具列表。

## 2. 配置后端环境（不提交）

在 `v2.0/backend/ai.env`（本地、gitignore）中追加：

```ini
AI_DIAGNOSTICIAN_APP_KEY=app-你的Dify应用Key
AI_DIFY_TOOL_TOKEN=p3-dify-token-2026
```

如果 Dify 工具里改过 token，两边必须一致。

## 3. 启动后端 8080

推荐使用项目原有启动脚本（会读取 `ai.env`）：

```powershell
powershell -File v2.0/backend/run-ai.ps1
```

确认：

```powershell
Invoke-WebRequest http://127.0.0.1:8080/api/auth/captcha -UseBasicParsing
```

> 若用 `javac` 启动，需给 JVM `-Xmx384m`；CI 环境另需 `JAVA_TOOL_OPTIONS=-Xmx256m` 规避本机内存不足。

## 4. 启动前端（轨迹页）

```powershell
cd v2.0/frontend
npm run dev -- --port 5173 --strictPort
```

打开 `http://localhost:5173/#/admin/trace`。

## 5. 跑 P3 验收

Dify 已启动且后端在 8080：

```powershell
$env:AI_DIAGNOSTICIAN_APP_KEY='app-...'
$env:AI_DIFY_TOOL_TOKEN='p3-dify-token-2026'
powershell -File v3.0/tool/verify_p3.ps1 -Port 8080 -UseExisting
```

- Dify 未配置/不可达：按 `skip-if-offline` 输出 SKIPPED，退出码 0。
- 已配置：30 题 parity + 故障注入；结果写 `v3.0/eval/p3_parity.json`、`v3.0/evidence/P3.md`。

## 6. CI

```powershell
$env:JAVA_TOOL_OPTIONS='-Xmx256m'
powershell -File v3.0/tool/ci.ps1
```

> 需要先停止 Dify 容器释放内存；CI 只跑编译/单测/数据/报告/基线/统计，不依赖 Dify。
