/* gen_ai_guide.js — 《Dify 补齐指南 & AI 新功能测试手册》
 * 用法: node gen_ai_guide.js   → ai_guide.json
 * 说明: 只出操作手册，不含代码改动；所有命令、app_id、端口均来自本机实测
 */
'use strict';
const fs = require('fs');
const path = require('path');

const B = [];
const h1 = (x) => B.push({ t: 'h1', x });
const h2 = (x) => B.push({ t: 'h2', x });
const h3 = (x) => B.push({ t: 'h3', x });
const p = (x) => B.push({ t: 'p', x });
const note = (x) => B.push({ t: 'note', x });
const ul = (x) => B.push({ t: 'ul', x });
const ol = (x) => B.push({ t: 'ol', x });
const table = (cap, head, rows, widths) => B.push({ t: 'table', cap, head, rows, widths });

/* ============================== 1 现状 ============================== */
h1('1 现在的状态：哪些已经好了、哪一步卡住');
h2('1.1 已经就绪的部分（我这边实测通过）');
table('已完成清单',
  ['项', '状态', '证据 / 位置'],
  [
    ['Dify 1.18.0 自托管', '**已跑起来**', '`docker compose ps` 13 个容器 Up；控制台 http://localhost:8090 可打开'],
    ['端口与冲突', '**不冲突**', '宿主只占 8090 / 8443 / 5003；db / redis / weaviate / sandbox 全在容器内网，没抢 8080、3306、5173'],
    ['管理员账号', '**已创建**', 'admin@campus.local / Dify@2026demo'],
    ['报告工作流', '**已导入并发布**', '应用「学习评价报告（学生端）」，app_id `0922a164-d2f9-4c82-b485-97eabcd60891`'],
    ['工作流 DSL 源文件', '**已入库**', '`v2.0/ai/dify-workflows/report-student.yml`（0.3.1 格式，导入时自动升到 0.7.0）'],
    ['应用 API Key', '**已创建**', '`app-QuSV6iZg2lpkuPj3JCiQspEw`（可在 Dify 应用「访问 API」页随时重建）'],
    ['后端 M1 底座', '**已完成**', '`ai_*` 7 张表、`/api/ai/health`、`ai_run/ai_run_step/ai_usage`、日限额'],
    ['学生端报告功能', '**已跑通**', '5 门课生成 5 份，其中 4 份 AI 撰写（source=LLM）、1 份按设计退模板'],
    ['三层降级链', '**已验证**', 'Dify 失败 → DashScope 直连 → 规则模板；实测 Dify 报错时自动落到直连，页面无感'],
    ['模型供应商插件', '**装了一半**', '`plugin/list` 里能看到记录（0.0.73，source=marketplace），但 `model-providers` 仍返回 0 → 见第 2 章']
  ], [22, 18, 60]);
h2('1.2 唯一卡住的一步');
p('**模型供应商没注册成功。** Dify 1.x 的模型供应商是插件，我通过控制台 API 走完了"取 checksum → 安装 marketplace 包 → 插件预加载"三步，插件记录也落库了，但 `GET /console/api/workspaces/current/model-providers` 仍然返回 0 个供应商，导致工作流跑到 LLM 节点时直接失败（`Workflow not published` 之外的下一层错误就是"模型未配置"）。');
p('这不影响你现在演示：后端的三层链会自动退到 DashScope 直连，报告照常生成、数字照样回验，只是 `source` 显示 `LLM` 而不是 `DIFY`。补齐第 2 章三小步后就会变成 `DIFY`。');
h2('1.3 记牢这几个值');
table('账号 / 地址 / 密钥',
  ['项', '值', '在哪改'],
  [
    ['Dify 控制台', 'http://localhost:8090', '—'],
    ['Dify 管理账号', 'admin@campus.local / Dify@2026demo', 'Dify → 个人设置'],
    ['报告应用 app_id', '`0922a164-d2f9-4c82-b485-97eabcd60891`', 'Dify → 工作室 → 学习评价报告（学生端）'],
    ['报告应用 API Key', '`app-QuSV6iZg2lpkuPj3JCiQspEw`', 'Dify → 该应用 → 访问 API → 重新生成'],
    ['后端 AI 地址', '`AI_DIFY_BASE_URL=http://localhost:8090`', '`v2.0/backend/src/main/resources/application.yml` 或环境变量'],
    ['报告工作流 Key', '`AI_REPORT_APP_KEY=<上表 Key>`', '同上'],
    ['直连兜底 Key', '`AI_DASHSCOPE_KEY=<DashScope Key>`', '同上（v1.0 那把仍有效）'],
    ['业务后端', 'http://localhost:8080', '你自己的 IDEA 实例；我验证用的是 18080']
  ], [18, 44, 38]);

/* ============================== 2 补齐 Dify ============================== */
h1('2 补齐 Dify：三步，约 40 分钟');
p('顺序不能换：**先装模型供应商 → 再配凭据 → 最后验证工作流**。任何一步没通，后端都会自动降级，所以可以放心一步一步来。');
h2('2.1 第一步：装模型供应商（OpenAI-API-compatible）');
h3('路线 A（推荐，浏览器点两下）');
ol([
  '打开 http://localhost:8090 ，用 admin@campus.local 登录。',
  '右上角头像 →「设置」→「模型供应商」→ 右上「安装模型供应商」。',
  '搜索 `OpenAI-API-compatible`（作者 langgenius），点「安装」。等 1–3 分钟（插件会装 Python 依赖）。',
  '刷新页面，左侧出现「OpenAI-API-compatible」即成功。'
]);
h3('路线 B（命令行，我已经走到最后一步）');
note('下面命令在 PowerShell 里执行；密码必须 base64，console API 必须带 Cookie + X-CSRF-Token。');
ul([
  '① 取插件最新版与 checksum：`GET https://marketplace.dify.ai/api/v1/plugins/langgenius/openai_api_compatible/0.0.73` → 记下 `checksum`；',
  '② 登录拿会话：`POST /console/api/login`，body 里 `password` 传 base64；',
  '③ 安装：`POST /console/api/workspaces/current/plugin/install/marketplace`，body `{"plugin_unique_identifiers":["langgenius/openai_api_compatible:0.0.73@<checksum>"]}`；',
  '④ 等插件预加载完成（`docker logs docker-plugin_daemon-1 | Select-String "pre-loaded"`）；',
  '⑤ 重启 api 与 worker：`docker compose restart api worker`；',
  '⑥ 验证：`GET /console/api/workspaces/current/model-providers` 应能看到 `langgenius/openai_api_compatible/openai_api_compatible`。'
]);
p('**如果第 ⑥ 步仍是 0**（我现在就是这种情况）：说明插件安装任务在"预加载成功"之后没有走到"激活"，最省事的办法是在 UI 里把该插件「卸载 → 重新安装」一次，UI 会补齐 API 没做的激活步骤。');
h2('2.2 第二步：给模型供应商填 Key（DashScope 或 DeepSeek 二选一）');
p('**两家都能用**。Dify 里已装好三个插件（openai / openai_api_compatible / deepseek），填哪家就用哪家；**没 Key 的供应商保持未配置即可**，工作流只会用它节点里指定的那个模型。');
table('路线一：DeepSeek（国内直连、便宜，推荐）',
  ['字段', '值', '说明'],
  [
    ['供应商', 'DeepSeek（插件已装：langgenius/deepseek:0.0.26）', '设置 → 模型供应商 → DeepSeek → 设置'],
    ['API Key', 'platform.deepseek.com → API keys 新建的 sk-...', '只存在 Dify 里，不进代码库'],
    ['Base URL', '不用填（插件内置 https://api.deepseek.com）', '只有走 OpenAI-API-compatible 路线才要手填'],
    ['模型', 'deepseek-v4-flash（本项目用的就是它，1M 上下文）；另有 deepseek-flash / deepseek-v4-pro 可选', '勾选即可，上下文长度用默认'],
    ['注意', 'DeepSeek v4 是推理模型，返回里带 <think>…</think> 思考块', '后端已自动剥离；Dify 运行面板里仍可见，属正常'],
    ['价格量级', '单份报告仍在一分钱以内', '以官方控制台为准']
  ], [14, 50, 36]);
table('路线二：DashScope（阿里云百炼，v1.0 那把 Key 仍有效）',
  ['字段', '值', '说明'],
  [
    ['供应商', 'OpenAI-API-compatible（langgenius/openai_api_compatible:0.0.73）', '设置 → 模型供应商 → 安装后配置'],
    ['API Key', 'DashScope 的 sk-...', '只存在 Dify 里'],
    ['Base URL', 'https://dashscope.aliyuncs.com/compatible-mode/v1', '必须带 /v1'],
    ['模型', 'qwen-plus（报告）、qwen-max（出题）', '分别加一条']
  ], [14, 50, 36]);
p('保存后点该供应商的「配置」→ 每个模型后面点「测试」→ 出现绿色「可用」即通过。**OpenAI 官方供应商没有 Key 就放着别动**，不影响任何功能。');
h2('2.3 第三步：验证工作流本身');
h3('先做一步：把工作流里的 LLM 节点指到你要用的模型');
p('导入的 DSL 里模型写的是占位值（`OpenAI-API-compatible / qwen-plus`）。配了 DeepSeek 就要改过来，否则运行时仍报"模型未配置"。');
ol([
  '工作室 → 打开「学习评价报告（学生端）」→ 点中间的 **LLM 节点**；',
  '右上「模型」下拉 → 供应商选 **DeepSeek**，模型选 **`deepseek-chat`**（要推理能力就选 `deepseek-reasoner`，但报告场景没必要）；',
  '温度保持 0.5，最大 token 保持 1600；',
  '右上「发布」→ 填版本说明（如 v1-deepseek）→ 确认。'
]);
note('以后换模型只改这一个节点并重新发布，后端代码与接口都不用动。');
h3('方式一：在 Dify 里点"运行"');
ol([
  '工作室 → 打开「学习评价报告（学生端）」→ 右上「运行」。',
  '在 `facts` 里粘一段测试事实，例如：`课程：大学计算机基础（一）；总分 61；班级均分 78.1；班内 43/45；缺勤 4，允许 4；预警：黄灯；分组正确率：简答题 64%/80%；题型：单选题 83%/85%`。',
  '点运行，右侧应返回四段：【结论】【你的强项】【需要补的地方】【下一步做什么】。'
]);
h3('方式二：命令行直接打 Service API');
note('POST http://localhost:8090/v1/workflows/run ，Header: Authorization: Bearer app-QuSV6iZg2lpkuPj3JCiQspEw ，body: {"inputs":{"facts":"……"},"response_mode":"blocking","user":"probe"}');
p('成功返回里 `data.status` 为 `succeeded`，`data.outputs.text` 就是正文，`data.total_tokens` 是本次用量。失败时 `data.error` 会写"模型未配置""provider not found"之类，按 2.6 排查。');
h2('2.4 第四步：把 Key 交给后端');
p('后端只认环境变量，**不要写进代码**。两种方式选一种：');
ul([
  '**IDEA 里跑**：Run Configuration → Environment variables 加 `AI_DASHSCOPE_KEY=sk-xxx;AI_REPORT_APP_KEY=app-QuSV6iZg2lpkuPj3JCiQspEw;AI_DIFY_BASE_URL=http://localhost:8090`（注意 IDEA 里多项用分号分隔）；',
  '**命令行跑**：PowerShell 里先 `$env:AI_REPORT_APP_KEY="app-QuSV..."` 再 `java -jar ...`。'
]);
p('`AI_REPORT_APP_KEY` 一配上，后端就会优先走 Dify；Dify 挂了会自动退直连，不会白屏。');
h2('2.5 第五步：确认后端真的走了 Dify');
table('三处对齐才算通',
  ['看哪里', '命令 / 位置', '期望'],
  [
    ['AI 体检接口', '`GET http://localhost:8080/api/ai/health`', '`reportWorkflowReady: true` 且 `reachable: true`'],
    ['生成一次报告', '学生端「学习评价」→ 选课 → 生成', '`source` 字段变成 `DIFY`'],
    ['数据库', '`select source,status,total_tokens from ai_run order by id desc limit 3;`', '最新一条 `source=DIFY, status=SUCCESS`'],
    ['步级明细', '`select name,validation_status from ai_run_step order by id desc limit 3;`', '出现 `dify-workflow` 且 `PASS`']
  ], [20, 42, 38]);
h2('2.6 常见报错对照表');
table('Dify 侧排错',
  ['现象', '原因', '怎么修'],
  [
    ['登录返回 `Invalid encrypted data`', '密码没 base64', '`password` 用 base64 编码后再传'],
    ['console API 返回 `CSRF token is missing or invalid`', '只用了 Cookie 或只用了 Bearer', 'Cookie + `X-CSRF-Token`（值取 csrf_token cookie）一起带'],
    ['`model-providers` 返回 0，但 `plugin/list` 有记录', '插件装到"预加载"没激活', 'UI 里卸载重装；或重启 api + worker 后再看'],
    ['工作流报 `Workflow not published`', '只导入了草稿没发布', '应用右上「发布」；或调 `POST /console/api/apps/{id}/workflows/publish`'],
    ['工作流报"模型未配置 / provider not found"', '2.2 的凭据没配，或配了但模型名错', '在供应商页「测试」模型；确认模型名是 `qwen-plus`'],
    ['`/v1/workflows/run` 返回 401', 'Key 用错应用', '到该应用「访问 API」页重新复制'],
    ['后端一直 `source=LLM`', '`AI_REPORT_APP_KEY` 没生效 / 没重启', '重启后端；看 `/api/ai/health` 的 `reportWorkflowReady`'],
    ['返回内容四段缺失', '模型的指令跟随弱', '把 `qwen-plus` 换成 `qwen-max`，或在工作流里加一条"只输出四个标题"的兜底提示'],
    ['报错 `Invalid Authorization token`', 'token 过期（1 小时）', '重新登录拿新的 Cookie/Bearer'],
    ['中文乱码', 'PowerShell 控制台编码', '`chcp 65001`，或用文件方式读取响应']
  ], [28, 26, 46]);

/* ============================== 3 测试手册 ============================== */
h1('3 新功能测试手册（照着做，20 分钟跑完）');
h2('3.1 环境准备（5 分钟）');
ol([
  '数据库执行迁移：`mysql -uroot -p study_warning_system_v2 --execute="source v2.0/sql/26_ai_stage3.sql"`（幂等，可重复跑）；',
  '后端重新打包并启动：带 `AI_DASHSCOPE_KEY`（和可选的 `AI_REPORT_APP_KEY`），端口用你自己的 8080；',
  '前端：`cd v2.0/frontend && node node_modules/vite/bin/vite.js dev`（或在 IDEA 里起 Vite）。路由新增了 `学生端 → 学习评价`；',
  'Dify：`cd v2.0/ai/dify/docker && docker compose up -d`，确认 8090 能打开。'
]);
note('三条都在时最好；只起后端也能测——AI 会自动退模板，正好用来验证降级。');
h2('3.2 冒烟测试（8 条，命令可复制）');
p('下面用 PowerShell，先登录拿 token（学生 20250001 / 000000）。验证码调试接口需要后端带 `--app.auth.captcha-debug=true`。');
table('冒烟用例',
  ['#', '用例', '怎么做', '期望结果'],
  [
    ['S1', 'AI 体检（免登录）', '`curl http://localhost:8080/api/ai/health`', '`code=0`，含 `enabled/baseUrl/reportWorkflowReady/reachable/usage` 五项'],
    ['S2', '学生登录', '`POST /api/auth/login`（loginNo/密码/验证码）', '`code=0`，返回 `role=STUDENT`、`refId`'],
    ['S3', '生成报告', '`POST /api/student/reports/generate` body `{"enrollmentId":20001}`', '`code=0`，`sections` 4–5 段，`actions` ≥ 2 个'],
    ['S4', '按教学班生成', '同上 body `{"teachingClassId":201}`', '`code=0`，课程名与教学班一致'],
    ['S5', '报告列表', '`GET /api/student/reports`', '`code=0`，数组，按时间倒序'],
    ['S6', '报告详情（记已读）', '`GET /api/student/reports/{id}`', '`code=0`，`read=true`；DB 里 `read_time` 有值'],
    ['S7', '反馈', '`POST /api/student/reports/{id}/feedback` body `{"useful":true}`', '`code=0`；再查详情 `useful=true`'],
    ['S8', '权限（越权）', '用教师账号 token 打 `/api/student/reports`', '`code=403`（未登录则 401）']
  ], [6, 22, 46, 26]);
h2('3.3 功能与边界用例（15 条）');
table('功能测试矩阵',
  ['用例', '前置条件', '期望'],
  [
    ['正常生成（计基，黄灯）', '有成绩、有作答', '结论提到总分/班级均分/排名，指出最低分组'],
    ['红牌学生的报告（四史）', '缺勤超限', '结论第一句说明"缺勤已达上限，会被取消考试资格"，不给"挂科"字样'],
    ['绿灯学生（数据结构）', '状态良好', '以"维持节奏"为主，不制造焦虑'],
    ['没有定稿成绩的学生', '`score_result` 无记录', '用 `risk_state` 的考勤兜底，不报错、不出现空洞数字'],
    ['评分项未登全', '存在 `missing` 分项', '"需要补的地方"里提示"老师还没登分，结论待补"'],
    ['同一门课重复生成', '已有旧报告', '新报告独立成条，旧报告保留（可对比）'],
    ['数字回验生效', '模型偶发编数字', '`ai_run_step` 出现 WARN/FAIL，自动重写一次，仍不合规退模板'],
    ['AI 关闭', '`AI_ENABLED=false`', '生成成功但 `source=TEMPLATE`，页面提示"规则生成"，不报错'],
    ['没有配任何 Key', '两个 Key 都空', '同上，走模板'],
    ['超出日限额', '同账号当天 30 次以上', '生成成功但 `source=TEMPLATE`，`ai_run.error_message` 写"已达当日调用上限"'],
    ['Dify 停了', '`docker compose stop nginx`', '自动退 DashScope；`source=LLM`，接口仍 `code=0`'],
    ['dashscope 不可达', '断网或改错 base-url', '自动退模板；`ai_run.status=FALLBACK`'],
    ['查看他人报告', '学生 A 用学生 B 的 reportId', '`code=0? 不，应为 400/403`，提示"报告不存在或无权查看"'],
    ['不在该教学班', 'teachingClassId 非本人', '`code=400`"你不在该教学班，或选课已失效"'],
    ['反馈重复提交', '同一份报告点两次"有用"', '允许，取最新一条（前端会禁用按钮）']
  ], [26, 26, 48]);
h2('3.4 数据库核对（5 段 SQL）');
note('都能直接跑；把 student_id=1 换成你要查的学生。');
ul([
  '**运行记录**：`select id,scene,status,source,total_tokens,total_cost,error_message,create_time from ai_run order by id desc limit 5;`',
  '**步级明细（含数字回验结论）**：`select run_id,step_no,name,validation_status,validation_detail,duration_ms from ai_run_step order by id desc limit 8;`',
  '**报告内容与事实快照**：`select id,enrollment_id,source,risk_level,total_score,json_length(fact_json),json_length(content_json) from ai_report order by id desc limit 5;`',
  '**反馈**：`select report_id,useful,reason,create_time from ai_report_feedback order by id desc limit 5;`',
  '**成本**：`select scene,count(*) 次数,sum(total_tokens) tokens,round(sum(cost),4) 元 from ai_usage where create_time>=curdate() group by scene;`'
]);
h2('3.5 页面点检（学生端 6 项）');
ol([
  '菜单出现「学习评价」（在「学情预警」下面），点进去不白屏；',
  '左上角课程下拉能列出本学期课程（应含 5 门）；',
  '选课 → 点「生成这份报告」，按钮变「生成中…（约 10 秒）」，约 5–15 秒后右侧出现正文；',
  '正文分 4–5 段，标题是「结论 / 你的强项 / 需要补的地方 / 学习投入 / 下一步做什么」；',
  '底部动作按钮可点：重做→跳「我的考试」、看导学→跳课程详情、找老师→提示语；',
  '点「有用」出现"已记录：有用"；左侧列表里该报告带「AI」或「规则」标签。'
]);
h2('3.6 验收指标（答辩/验收时用这张表）');
table('AI 报告验收',
  ['指标', '目标', '怎么算'],
  [
    ['数字一致率', '100%', '人工抽查 10 份，逐个数字对 `fact_json`'],
    ['来源分布', 'AI ≥ 70%', '`select source,count(*) from ai_report group by source;`'],
    ['模板兜底占比', '≤ 30%', '同上；过高说明 Key 或网络有问题'],
    ['生成耗时', '单份 ≤ 20 秒', '`ai_report.create_time` 与 `ai_run.create_time` 差值'],
    ['单份成本', '≤ 0.01 元', '`ai_usage.cost / 份数`'],
    ['覆盖', '本学期 5 门课都能生成', '逐课点一遍'],
    ['降级可用', '断 Key / 停 Dify 仍能出报告', '见 3.3 后三条']
  ], [20, 26, 54]);
h2('3.7 三种降级怎么验（各 1 分钟）');
table('降级验证',
  ['场景', '操作', '期望'],
  [
    ['关掉 AI', '`AI_ENABLED=false` 重启后端', '`/api/ai/health` 里 `enabled=false`；生成仍成功，`source=TEMPLATE`'],
    ['只留直连', '清空 `AI_REPORT_APP_KEY`', '`reportWorkflowReady=false`；`source=LLM`'],
    ['全断', '两个 Key 都清空（或停 Dify + 断网）', '`source=TEMPLATE`，`ai_run.status=FALLBACK`，页面无异常']
  ], [16, 46, 38]);
h2('3.8 答辩用的 3 分钟演示脚本');
table('演示路线',
  ['时间', '动作', '要讲的一句话'],
  [
    ['0:00–0:30', '学生端 → 学习评价 → 选「大学计算机基础」→ 生成', '报告里的每个数字都由系统算，AI 只负责说人话'],
    ['0:30–1:20', '读「结论」与「需要补的地方」，指一下 61 / 78.1 / 43·45 / 简答题 64% vs 80%', '这些数字都能在系统里点回去，不是我编的'],
    ['1:20–1:50', '点动作「重做简答题」→ 跳考试列表', '报告的价值是动作，不是文字'],
    ['1:50–2:30', '打开 `/api/ai/health`，再打开 Dify 控制台（8090）', '两套系统各司其职：Dify 生成，业务库存权威数据'],
    ['2:30–3:00', '把 `AI_ENABLED=false` 重启（或直接清 Key）再生成一次', 'AI 是可插拔能力，关掉不影响业务，只是文字退回模板']
  ], [14, 52, 34]);

/* ============================== 4 附录 ============================== */
h1('4 附录');
h2('4.1 新增接口清单');
table('学生端报告接口',
  ['方法与路径', '说明', '权限'],
  [
    ['`GET /api/student/reports`', '报告列表（倒序，最多 50 条）', '学生本人'],
    ['`GET /api/student/reports/{id}`', '详情；首次查看写 `read_time`', '学生本人'],
    ['`POST /api/student/reports/generate`', 'body 支持 `enrollmentId` 或 `teachingClassId`', '学生本人'],
    ['`POST /api/student/reports/{id}/feedback`', '`{useful:true/false, reason}`', '学生本人'],
    ['`GET /api/ai/health`', 'Dify 可达性 + 工作流就绪 + 今日用量', '**免登录**'],
    ['`POST /api/ai/ping`', '手动探活', '登录即可'],
    ['`GET /api/ai/usage`', '成本与调用统计', 'ADMIN']
  ], [38, 44, 18]);
h2('4.2 环境变量一览');
table('后端 AI 配置',
  ['变量', '默认', '作用'],
  [
    ['`AI_ENABLED`', 'true', 'false 时全站不调外部模型，全部走模板'],
    ['`AI_DIFY_BASE_URL`', 'http://localhost:8090', 'Dify 地址'],
    ['`AI_REPORT_APP_KEY`', '空', '报告工作流 Key；配了优先走 Dify'],
    ['`AI_QUESTION_APP_KEY` / `AI_CHAT_APP_KEY`', '空', '出题 / 问答（M2、M4）'],
    ['`AI_LLM_BASE_URL`', '空', '通用直连地址：DeepSeek https://api.deepseek.com/v1，DashScope .../compatible-mode/v1'],
    ['`AI_LLM_KEY`', '空', '通用直连 Key（DeepSeek 或 DashScope 都行）'],
    ['`AI_LLM_MODEL`', '空', '通用直连模型：deepseek-chat 或 qwen-plus'],
    ['`AI_DASHSCOPE_KEY` / _BASE_URL / _MODEL', '空 / DashScope / qwen-plus', '旧配置：上面三项留空时才用它们'],
    ['`AI_DAILY_LIMIT`', '30', '每用户每日调用上限（超额退模板）']
  ], [34, 26, 40]);
h2('4.3 工作流契约（DSL 与后端对齐的地方）');
table('输入输出约定',
  ['项', '约定', '不满足会怎样'],
  [
    ['输入 `facts`', '段落文本，≤ 4000 字，由 `FactPackBuilder` 生成', '模型只能凭空编，数字回验会把它挡掉'],
    ['输入 `course` / `student` / `risk_level` / `total_score` / `class_avg` / `absents` / `allowed_absents`', '均为字符串，可空', '工作流里没声明会被忽略，不影响运行'],
    ['输出 `text`', '**必须叫 text**，四段正文（允许带 <think> 思考块）', '后端会剥掉思考块；找不到 text 则判失败并降级'],
    ['分隔标题', '必须是【结论】【你的强项】【需要补的地方】【下一步做什么】', '后端按标题切段，标题不对会整段塞进"结论"'],
    ['数字口径', '只能引用 facts 里的数字；"下一步"里的训练量可用文字表述', '白名单外数字 → 重写一次 → 仍失败退模板']
  ], [30, 40, 30]);
h2('4.4 一键自检脚本（PowerShell）');
note('保存为 check-ai.ps1，在仓库根目录执行：先看 Dify 是否可达，再看后端体检，最后生成一份报告并打印来源。');
ul([
  '`$h = Invoke-RestMethod http://localhost:8090/console/api/setup -ErrorAction SilentlyContinue; if ($h) { "Dify 在线" } else { "Dify 不可达" }`',
  '`Invoke-RestMethod http://localhost:8080/api/ai/health | ConvertTo-Json -Depth 5`',
  '登录三连：`/api/auth/captcha` → `/api/auth/captcha/debug?captchaId=` → `/api/auth/login`（字段 `loginNo`、`captchaCode`）',
  '`$r = Invoke-RestMethod -Method Post -Uri http://localhost:8080/api/student/reports/generate -Headers @{Authorization="Bearer $tok"} -ContentType application/json -Body (@{teachingClassId=201} | ConvertTo-Json); $r.data.source`'
]);
h2('4.5 补完 Dify 之后要做的两件事');
ol([
  '把 `source=DIFY` 的那份报告截图，补进演示 PPT（对比 `source=LLM` 的版本，讲"同一套事实、不同通道"）；',
  '在 `v2.0/ai/dify-workflows/` 里把最终版工作流再导出一遍（Dify 应用 → 编排 → 右上「导出 DSL」），覆盖 `report-student.yml`，保证"代码里的 DSL = 实际跑的工作流"。'
]);

const meta = {
  title: '学情预警系统 v2.0',
  subtitle: 'Dify 补齐指南 & AI 新功能测试手册',
  sub2: '三步补完 Dify · 8 条冒烟 + 15 条边界用例 · 降级验证 · 答辩演示脚本',
  tip: '本手册只讲操作与验证，不含代码改动；命令、app_id、端口均来自本机实测',
  info: [
    ['文档名称', '学情预警系统 v2.0 · Dify 补齐指南 & AI 新功能测试手册'],
    ['适用对象', '你自己（补 Dify 配置 + 自测）与答辩前的验收'],
    ['当前状态', 'Dify 1.18.0 已起（8090）、报告工作流已导入并发布、后端三层降级已验证；仅剩"模型供应商激活"一步'],
    ['不含', '不改业务代码；不改第二阶段判定口径'],
    ['编写日期', '2026-12']
  ]
};

const out = path.join(__dirname, 'ai_guide.json');
fs.writeFileSync(out, JSON.stringify({ meta, blocks: B }, null, 1), 'utf8');
const stat = { h: 0, p: 0, tbl: 0, li: 0 };
B.forEach((b) => {
  if (b.t[0] === 'h') stat.h++;
  else if (b.t === 'p' || b.t === 'note') stat.p++;
  else if (b.t === 'table') stat.tbl++;
  else stat.li += b.x.length;
});
console.log('WROTE ' + out);
console.log(JSON.stringify(stat));
