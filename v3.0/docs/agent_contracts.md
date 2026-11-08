# 四个业务智能体契约（P2-7 ~ P2-10）

> 统一入口：`/api/agent/*`；全部走 P2-1 工具白名单/P2-3 执行器/P2-6 台账。
> 当前实现为**确定性基线**（规则选路 + 模板/启发式生成），LLM 生成在 P2-13 评测阶段可替换内部实现，接口契约不变。

## 1. Diagnostician（P2-7）

- `POST /api/agent/diagnose`
- 权限：ADMIN / COUNSELOR
- 请求：`{question, termId?}`
- 响应：`{agent:"diagnostician", question, intent, intentLabel, routeMethod, tools[], evidence[], answer, refused, blockedReason, ledger, quality, latencyMs}`
- 验收：30 题工具选择 ≥90%、P95 <8s；本轮 30/30、P95 86ms。

## 2. Reporter（P2-8）

- `POST /api/agent/report`
- 权限：TEACHER（class/offering）、COUNSELOR（adminClass）
- 请求：`{type:"class|offering|adminClass", targetId}`
- 响应：`{agent:"reporter", reportType, targetId, id, title, summary, sections[], actions[], checksPassed, checksTotal, checks[], stale, source, latencyMs}`
- 验收：结构 100%、快照逐字段对账 100%、3–15s；本轮 6/6、10/10、6/6，0.06–0.11s。

## 3. Grader（P2-9）

- `POST /api/agent/grade`：`{submissionId}` → 逐题 `{humanScore, aiScore, diff, matchedPoints[], missingPoints[], comment}` + 总分/MAE
- `POST /api/agent/grade/eval`：全量 60 份提交/300 条作答 → `{samples, mae, maxAbsError, within2Rate, perAnswer[]}`
- `POST /api/agent/grade/apply`：`{answerId, score, comment}` → 替换旧分、重算提交总分、comment 留痕
- 权限：TEACHER / ADMIN
- 验收：MAE ≤2、要点逐条有对应、改后不留旧分；本轮 MAE **0.5**、max 1.0、within2 100%。

## 4. Tutor（P2-10）

- `POST /api/agent/tutor`
- 权限：STUDENT / TEACHER / ADMIN
- 请求：`{courseId, question}`
- 响应：`{agent:"tutor", courseId, courseName, question, retrievalMode, refused, gateReason, answer, citations[{chunkId,docName,sectionNo,score}], citationCount}`
- 验收：引用 100% 可定位到 chunkId；拒答准确/误拒；本轮核心集 4/4 可答（12 条引用全部可在 `ai_kb_chunk` 定位）、2/2 正确拒答、误拒 0；60 题盲评在 P2-13 扩展。

## 5. 已知边界

- Diagnostician 的选路目前由 IntentRouter 规则 + 编排器完成；LLM 输出受限 JSON 的选路可在不改契约的前提下接入。
- Grader 评分是“评分要点 + 作答质量”的确定性基线，用于证明契约与 MAE；真实评分需 LLM/教师校准。
- Reporter 复用 v2.0 报告服务，生成速度快于 3s 下限（<0.2s），验收按“≤15s 且对账 100%”执行。
- Tutor 答案当前由检索片段模板拼接；LLM 生成与 60 题盲评在 P2-13 接入。
