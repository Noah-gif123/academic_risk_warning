# Dify「学习评价报告」工作流（学生端）配置说明

> 后端通过 `AI_REPORT_APP_KEY` 调用这个 **workflow** 应用；报告是本系统唯一默认走 Dify 的场景
> （解释卡 / 群体报告 / 批改 / 出题 / 答疑走直连或模板，见 `backend/README.md`）。
> 本文记录的是**能跑通的最小配置**，以及踩过的坑，方便换机或重建 Dify 时照抄。

## 1 节点结构

```
开始(start_node) ──► LLM(llm_node, 标题「写报告」) ──► 结束(end_node, 输出变量 text = {{#llm_node.text#}})
```

只有一个 LLM 节点，没有知识库、没有代码节点——报告的事实全部由后端算好，模型只负责「说人话」。

## 2 开始节点：输入变量

| 变量 | 类型 | 必填 | 上限 | 说明 |
| --- | --- | --- | --- | --- |
| `facts` | paragraph | 是 | **8000** | 事实包全文（课程、总分/过程分、班级对比、考勤、分项、分组/题型正确率、预警结论、硬性约束） |
| `course` / `student` / `risk_level` / `total_score` / `class_avg` / `absents` / `allowed_absents` | text-input | 否 | 10–100 | 便于在 Dify 侧调试时单独看，后端也会一起传 |

> `facts` 上限必须是 8000。分项/分组多的学生事实包会超过 4000 字，历史上就是因为上限 4000
> 导致这类学生的报告在 Dify 侧直接失败。

## 3 LLM 节点：模型与参数

```json
{"provider":"langgenius/deepseek/deepseek","name":"deepseek-v4-flash","mode":"chat",
 "completion_params":{"temperature":0.5,"max_tokens":4096}}
```

**`max_tokens` 必须 ≥ 4096。** 这类推理模型会把思考过程也计入 `completion_tokens`：
实测 `max_tokens=1600` 时，12 次生成里有 4 次 `finish_reason=length`——思考块把预算吃满，
返回体只有 `<think>…</think>`、没有正文，后端判定「不含四段标题」后整体降级到直连。

## 4 Prompt

### system

```
你是高校学业预警系统里的「学习评价撰写助手」，服务对象是学生本人。
必须遵守：
1. 只能使用用户给出的数字，禁止自己计算、换算或编造任何数字、比例、排名；
2. 严格输出四段，段落标题只能是【结论】【你的强项】【需要补的地方】【下一步做什么】，每段 1-3 句，四个标题各自独占一行；
3. 用第二人称，语气平实，不出现"挂科""必挂""肯定过不了"等判定词，风险结论只能引用给定的预警等级；
4. 不使用 Markdown 星号、表格、代码块；前两段只挑最关键的 3 到 5 个数字，不要罗列；
5. 总字数 350 到 600 字，最后一段给出可以直接执行的动作；
6. 最后一段允许给出训练时长、题量这类计划数字（如"每天 5 道题、限时 8 分钟"）；
7. 直接输出报告正文，不要输出思考过程、不要复述这些规则。
```

### user

```
以下是这名学生的真实数据（由系统计算，不得改动）：
{{#start_node.facts#}}

请据此写一份学习评价报告：先给结论（结合预警等级），再说强项与最需要补的 2 到 3 块，最后给出 3 条可以直接执行的动作。
```

## 5 后端对输出的三道校验（不过就重试一次，再不过换直连）

| 校验 | 位置 | 不过的表现 |
| --- | --- | --- |
| 剥推理块 | `ReportComposer.stripReasoning` | `<think>…</think>` 成对剥离；未闭合时按首个段落标题裁剪 |
| 四段标题 | `ReportComposer.looksLikeReport` | 兼容 `【结论】` 与 `结论：` 两种写法，四项命中 ≥ 2 才收 |
| 数字回验 | `ReportComposer.invalidNumbersForReport` → `allowedNumbers` | 前三段数字必须命中事实包（±0.6 容差），第四段允许计划数字；Dify 首次不过会带「上一版越界数字」重试一次 |

> M16 之后报告的**主口径是过程分**（期末未录时），所以 `ReportFact.keyNumbers()` 必须包含
> `processScore`——漏了它，模型写「过程分 97.2」会被自己的白名单判成越界，报告整篇丢弃。

## 6 怎么确认真的走了 Dify

```sql
select id, scene, source, model, total_tokens, error_message
from ai_run order by id desc limit 5;
```

期望：`source=DIFY`、`model=dify-workflow`、`error_message` 为空。
若出现 `source=LLM` + `error_message like '主通道降级说明%'`，说明 Dify 这次没过校验，
降级原因就写在 `error_message` 里（越界数字 / 不含四段标题 / 工作流调用失败）。
