# P3：Dify Diagnostician 第二实现

> **最终状态（2026-11-10）**：PASS。Dify 应用已配置 8 工具/共享 prompt/API Key；30 题 parity 30/30，数字 96.7%、方向 93.3%；故障注入降级通过。详见 `v3.0/evidence/P3.md`。

> 范围：只做 Diagnostician 的 Dify 版本；8 个只读工具，`draftIntervention` 不进入 Dify。
> 当前状态（2026-11-09）：Dify 1.18 本机可达（8090），但未配置 `AI_DIAGNOSTICIAN_APP_KEY`；
> `verify_p3.ps1` 按 **skip-if-offline** 输出 SKIPPED，未伪造 parity。

## 文件

| 文件 | 用途 |
|---|---|
| `tools.openapi.json` | 8 个只读工具的 OpenAPI 3.0.3；导入 Dify「自定义工具」 |
| `diagnostician_openapi.yaml` | Diagnostician `/v1/run` 契约（请求/响应/证据结构） |
| `diagnostician-system-prompt.md` | 本地/Dify 共享 system prompt（单一来源） |
| `dify-app-manifest.json` | Dify Agent 应用配置清单（非 DSL 导出） |
| `../tool/verify_p3.ps1` | 30 题 parity；离线/未配置自动 SKIPPED |
| `../eval/p3_parity.json` | parity 结果（passed/failed/skipped） |
| `../evidence/P3.md` | P3 证据卡 |

## 启用步骤（需要 Dify 应用已创建）

1. 启动 Dify：`powershell -File v2.0/ai/start-dify.ps1`（或 `cd v2.0/ai/dify/docker && docker compose up -d`）。
2. Dify 控制台 → 工具 → 自定义工具 → 导入 `v3.0/p3/tools.openapi.json`。
   - 服务 URL 使用 `http://host.docker.internal:8080`（Dify 容器访问宿主机后端）。
   - 工具调用需走自研鉴权；当前 OpenAPI 未附带服务令牌，导入后需在 Dify 工具凭据中补齐。
3. 创建 Agent 应用（`agent-chat`），输入 `question`（可选 `termId`），粘贴 `diagnostician-system-prompt.md`，只选 8 个只读工具。
4. 发布应用并创建 API Key，设置：
   ```powershell
   $env:AI_DIAGNOSTICIAN_APP_KEY = 'app-...'
   $env:AI_DIFY_BASE_URL = 'http://127.0.0.1:8090'
   ```
5. 重启后端，运行：
   ```powershell
   powershell -File v3.0/tool/verify_p3.ps1
   ```
   - Dify 未配置/不可达：输出 SKIPPED，退出码 0。
   - 已配置：30 题双跑，数字一致 ≥95% 或方向一致 ≥90% 才 PASS；同时做故障注入（错误 Dify 地址应降级本地并记 `fallback_reason`）。

## 代码入口

- `POST /api/agent/diagnose`：body 增加 `provider=LOCAL|DIFY`、`strictDify`；DIFY 失败默认降级本地并返回 `fallbackReason`。
- `GET /api/agent/dify/health`：配置/可达/允许工具/排除写工具。
- `DifyAgentProvider`：调用 Dify `/v1/chat-messages`，强制 JSON 契约，复用台账/拒答/工具白名单校验。
- `GET /api/agent/tools?format=openapi&dify=true`：动态生成 8 工具 OpenAPI（排除 `draftIntervention`）。
- `POST /api/agent/v1/run`：按 `diagnostician_openapi.yaml` 的 `/v1/run` 契约运行（本地 Diagnostician）。

## 已知阻塞

- 当前 Dify 控制台没有 Diagnostician Agent 应用，环境无 `AI_DIAGNOSTICIAN_APP_KEY`；因此 **parity 0 passed / 0 failed / 30 skipped**，不能声称已通过。
- Dify 自定义工具调用自研后端需要服务令牌；未配置前不要用“放宽校验”绕过，按 P3 规则如实记失败/跳过。


## 本次收口补充

- 已提供 `POST /api/agent/dify/tool/{tool}`，由 `X-Dify-Token`（环境变量 `AI_DIFY_TOOL_TOKEN`）保护，供 Dify 自定义工具回调；不带 token 返回 401，`draftIntervention` 返回 403。
- Dify 1.18 本机可达；最小 `/ping` 工具可导入，但 8 工具 OpenAPI 的导入请求在 Dify 端挂起（provider 未创建），故 Diagnostician Agent 仍无法发布拿 Key。这是当前 parity 的唯一外部阻塞，未伪造结果。
