/* build_ai_reference_guide.js —— 生成《学情预警系统 v2.0 · AI 后续优化完善指南》
 *
 * 用途：把本次 AI 专项检查发现的问题、已修复项、以及"让 RAG 名副其实"的分阶段方案，
 *      整理成一份可直接用于排期与论文写作的指南。
 * 依据：代码实读、MySQL 实库统计、Dify/DeepSeek 真跑记录（ai_run / ai_run_step / ai_qa_log）。
 *
 * 用法：node build_ai_reference_guide.js [输出路径]
 *   默认：v2.0/word/学情预警系统_AI后续优化完善指南.docx
 */
'use strict';
const path = require('path');
const dx = require('docx');
const {
  Document, Packer, Paragraph, TextRun, HeadingLevel, AlignmentType,
  Table, TableRow, TableCell, WidthType, TableLayoutType, BorderStyle, Footer, PageNumber
} = dx;

const CJK = { eastAsia: '宋体', ascii: 'Times New Roman', hAnsi: 'Times New Roman' };
const CJK_B = { eastAsia: '黑体', ascii: 'Times New Roman', hAnsi: 'Times New Roman' };
const BODY = 21;
const SMALL = 18;
const CONTENT_W = 9070;
const B = { style: BorderStyle.SINGLE, size: 4, color: '9AA0A6' };
const BORDERS = { top: B, bottom: B, left: B, right: B, insideHorizontal: B, insideVertical: B };

function run(text, o = {}) {
  return new TextRun({
    text: String(text).replace(/[`]/g, ''),
    font: o.bold ? CJK_B : CJK,
    size: o.size || BODY,
    bold: !!o.bold,
    italics: !!o.italics,
    color: o.color
  });
}

function para(text, o = {}) {
  const kids = Array.isArray(text) ? text : [run(text, o)];
  return new Paragraph({
    children: kids,
    alignment: o.align,
    spacing: { before: o.before == null ? 0 : o.before, after: o.after == null ? 90 : o.after, line: o.line || 300 },
    indent: o.indent
  });
}

function h1(t) {
  return new Paragraph({
    heading: HeadingLevel.HEADING_1,
    spacing: { before: 260, after: 120 },
    children: [new TextRun({ text: t, font: CJK_B, size: 28, bold: true, color: '1F3864' })]
  });
}

function h2(t) {
  return new Paragraph({
    heading: HeadingLevel.HEADING_2,
    spacing: { before: 200, after: 90 },
    children: [new TextRun({ text: t, font: CJK_B, size: 24, bold: true, color: '2E5395' })]
  });
}

function h3(t) {
  return new Paragraph({
    heading: HeadingLevel.HEADING_3,
    spacing: { before: 160, after: 70 },
    children: [new TextRun({ text: t, font: CJK_B, size: 22, bold: true, color: '3B5F8A' })]
  });
}

function cell(text, o = {}) {
  const kids = (Array.isArray(text) ? text : [text]).map((t) => new Paragraph({
    children: [run(t, { bold: o.head, size: SMALL })],
    spacing: { before: 20, after: 20, line: 260 }
  }));
  return new TableCell({
    children: kids,
    width: { size: o.w || 1000, type: WidthType.DXA },
    shading: o.head ? { fill: 'EAF1FB' } : undefined
  });
}

function table(headers, rows, widths) {
  const w = widths || headers.map(() => Math.floor(CONTENT_W / headers.length));
  const total = w.reduce((a, b) => a + b, 0);
  return new Table({
    columnWidths: w,
    width: { size: total, type: WidthType.DXA },
    layout: TableLayoutType.FIXED,
    borders: BORDERS,
    rows: [
      new TableRow({ tableHeader: true, children: headers.map((t, i) => cell(t, { head: true, w: w[i] })) }),
      ...rows.map((r) => new TableRow({ children: r.map((t, i) => cell(t, { w: w[i] })) }))
    ]
  });
}

// ============================================================ 内容
const children = [];

children.push(new Paragraph({
  alignment: AlignmentType.CENTER,
  spacing: { after: 60 },
  children: [new TextRun({ text: '学情预警系统 v2.0 · AI 后续优化完善指南', font: CJK_B, size: 36, bold: true })]
}));
children.push(new Paragraph({
  alignment: AlignmentType.CENTER,
  spacing: { after: 220 },
  children: [new TextRun({
    text: '面向「基于 RAG 检索增强大模型的学情预警与个性化学习管理」的现状盘点、问题清单与改造路线（2026-11-01）',
    font: CJK, size: 20, color: '666666'
  })]
}));

children.push(para([
  run('文档定位：', { bold: true }),
  run('本指南不是功能介绍，而是"下一步该改什么、怎么验收"的施工单。所有结论都有证据来源：代码实读、MySQL 实库统计、Dify 与 DeepSeek 的真跑记录（ai_run / ai_run_step / ai_qa_log / ai_report 等），不写未验证的推断。')
]));

// ---------------- 一、当前基线
children.push(h1('一、当前基线（实测，2026-11-01）'));

children.push(h2('1.1 六个 AI 场景的真实通道与运行结果'));
children.push(para('结论先说：六个场景都能真调模型，其中"学习评价报告"走 Dify 工作流，其余走 DeepSeek 直连。下表的 tokens 与耗时来自 ai_run 实测记录，不是估算。'));
children.push(table(
  ['场景', '可用通道（代码事实）', '本次实测结果', 'tokens / 耗时'],
  [
    ['学习评价报告', 'Dify 工作流 → 直连 → 模板', 'DIFY 成功', '1238 / 6s'],
    ['预警解释卡', 'Dify → 直连 → 模板', '直连 LLM 成功', '1471 / 6s'],
    ['群体报告', 'Dify → 直连 → 模板', '直连 LLM 成功（含一次长度重试）', '4763 / 38s'],
    ['AI 批改建议', '仅直连', '直连 LLM 成功，三道校验 PASS', '909 / 3s'],
    ['AI 出题', '仅直连', '直连 LLM 成功，五道闸门 PASS', '2727 / 3s'],
    ['课程问答（RAG）', '仅直连 + 本地检索', '检索 4 命中、带引用、未拒答', '1498 / 4s']
  ],
  [1300, 2600, 2900, 1400]));

children.push(h2('1.2 运行留痕与数据规模'));
children.push(para('ai_run 累计 73 次调用；ai_report 34 篇（含本轮实测）、ai_alert_explain 5 条、ai_group_report 4 条、ai_grading_suggest 4 条、ai_question_draft 6 条、ai_qa_log 18 条；知识库 17 篇文档 / 40 个分块（导学 24 块 + 课件 16 块）。'));
children.push(para('留痕机制已具备：每次调用写 ai_run（场景、状态、来源、tokens、成本）+ ai_run_step（步骤与校验结果）+ 各场景业务表（含引用、校验明细），可作为"AI 可追溯"的物证。'));

children.push(h2('1.3 已经实测确认有效的"抗幻觉"机制'));
children.push(table(
  ['机制', '位置', '实测证据'],
  [
    ['出题五道闸门', 'QuestionGenService', 'quality_json 中 STRUCT/ANSWER/SOURCE/DUP/SCORE 五项 pass，passed=true'],
    ['批改三道校验', 'GradingAssistService', 'checks_json 中 SCORE_RANGE/COMMENT_DIGITS/REF_EXISTS 全 pass；AI 只给建议，教师采纳才写分'],
    ['报告数字回验', 'ReportComposer.invalidNumbersForReport', 'Dify 输出含编造数字 97.2 → validation_status=WARN → 拒收并降级'],
    ['问答拒答与引用', 'StudentQaService', '提问"入党申请书"→ hit_count=0、refused=1；正常问答带 docName/pageNo/sectionNo'],
    ['推理长度重试', 'DashScopeClient', 'deepseek-flash 长提示被截断 → 自动翻倍预算重试后成功']
  ],
  [1700, 2600, 4770]));

// ---------------- 二、问题总览
children.push(h1('二、问题总览（按"影响论文可信度"排序）'));
children.push(para('编号规则：R=RAG 能力（最高优先），Q=输出质量与闸门，A=审计与成本，C=通道与配置，L=限额，D=数据与安全。'));
children.push(table(
  ['编号', '问题', '影响', '现状', '优先级'],
  [
    ['R1', 'RAG 只接入了 1/6 场景（仅课程问答）', '与题目"基于 RAG"不匹配，答辩易被追问', '待改', 'P0'],
    ['R2', '检索是全文（ngram）而非向量/混合，无重排', '检索质量与可调性弱，无法讲"检索增强"', '待改', 'P0'],
    ['R3', '知识库语料过薄：17 篇 / 40 块，单篇最短 55 字', '稍复杂的问题就检索不到，只能拒答', '待改', 'P0'],
    ['R4', '没有检索评测（recall@k / 引用命中 / 拒答准确率）', '无法证明 RAG 有效，缺实验章节数据', '待改', 'P0'],
    ['R5', '检索阈值与打分尺度不匹配', 'top_score 见过 1.35 与 23.07，绝对阈值 0.10 不可比', '待改', 'P1'],
    ['Q1', '解释卡没有输出校验', '同一预警两次生成"触发类型"说法不同（分数临界/缺勤超限）', '待改', 'P1'],
    ['Q2', '解释卡/群体报告不会过期', '数据变了旧卡片照旧展示，口径与当前判定不一致', '待改', 'P1'],
    ['Q3', '报告数字回验是一票否决', '实测 2 次里有 1 次被拦并整篇退回模板，白花 token', '待改', 'P1'],
    ['A1', 'ai_run.model 记的是配置标签不是真实模型', '审计里写 qwen-plus，实际是 deepseek-flash', '待改', 'P1'],
    ['A2', '成本估算写死单价，token 未拆 prompt/completion', '换模型后成本失真（ai_usage.prompt_tokens 恒空）', '待改', 'P2'],
    ['A3', 'ai_run.status 取值混用（OK/SUCCESS/REFUSAL）', '统计口径不统一', '待改', 'P2'],
    ['A4', 'ai_prompt_template 表为空', 'prompt 版本无法追溯，与配置注释不符', '待改', 'P2'],
    ['A5', 'AI 口径与成绩口径混用', '报告出现"过程分 65.4 / 班级均分 30.9"两个尺子', '部分修复', 'P1'],
    ['C1', 'Dify 只接了 3 个场景，其余仅直连', '纯 Dify 部署时批改/出题/问答无法真跑', '已说明', 'P2'],
    ['C2', '缺少 2 个 Dify 应用（解释卡、群体报告）', '这两场景暂时只能走直连', '待建', 'P2'],
    ['L1', '日限额只覆盖 3 个场景', '学生端问答等入口不限流，存在被刷风险', '待改', 'P1'],
    ['D1', '手册文件里存着真实 Dify App Key', '密钥曾入版本库并已推送两个远端', '仓库内已清（2026-11-01），待吊销旧 Key', 'P0'],
    ['D2', '交付包（约 50MB）进入 Git 历史', 'GitHub 报大文件告警，仓库变重', '待处理', 'P2'],
    ['D3', 'Dify 控制台凭据未提供', '我无法代建应用，只能给 DSL 或由人操作', '待配合', 'P2']
  ],
  [820, 2280, 2900, 1000, 900]));

// ---------------- 三、逐项详述（关键项）
children.push(h1('三、关键问题详述与改法'));

children.push(h2('R1 + R2 + R3 + R4：让 RAG 名副其实（核心）'));
children.push(h3('现象与证据'));
children.push(para('检索调用点只有两处：StudentQaService.retrieve（课程问答）与 TeacherKbController（教师检索预览）。其余场景（报告、解释卡、群体报告、批改、出题）用的是 FactPack 结构化事实注入，不是文档检索。'));
children.push(para('检索实现为 MySQL FULLTEXT（WITH PARSER ngram）加 LIKE 兜底；全代码库没有 embedding / vector / cosine 相关实现，也没有重排（rerank）环节。'));
children.push(para('知识库仅 17 篇、40 块，最小文档（期末复习提纲）只有 55 字，覆盖不到"某一章讲什么"这类问题。'));
children.push(para('没有检索评测集与指标，无法回答"检索增强带来了多少提升"。'));
children.push(h3('建议改法（三层，可分期）'));
children.push(table(
  ['层次', '做法', '涉及改动', '工作量'],
  [
    ['第一层：扩场景', '解释卡/学习评价/群体报告在拼 prompt 前各做一次检索，把命中片段作为"可引用材料"注入，并把 citations 回传前端展示；检索不到时降级为纯事实包', 'AlertExplainService / StudentReportService / GroupReportService / ReportComposer', '半天'],
    ['第二层：向量+混合', 'ai_kb_chunk 增加 embedding 列；入库时批量向量化；检索改为"全文 top10 + 向量 top10 → RRF 融合 → 取 4"；保留拒答闸门与数字回验', 'KbService、KbIngest、DashScopeClient（新增 embeddings 接口）、ai_kb_chunk', '1–2 天'],
    ['第三层：评测', '构造 30 条可答问题 + 10 条不可答问题，算 recall@4、引用命中率、拒答准确率、幻觉率，对比"全文 / 向量 / 混合"', '新增评测脚本与结果表（sql 或 JSON + 报告）', '1 天']
  ],
  [1600, 4300, 2400, 800]));
children.push(para('embedding 来源三选一：① 阿里云百炼 text-embedding-v3（需百炼 Key）；② 本地 Ollama + bge-m3（零 Key、可离线、答辩加分）；③ 直接用 Dify 知识库（其底层已配 Weaviate，可在工作流里加"知识检索"节点）。'));
children.push(para('验收：同一问题在"全文 / 混合"两种检索下的命中片段数、answer 引用数、refused 值写入评测表；解释卡与报告中出现"依据：文档名 第N段"字样；ai_run_step 里能看到"检索 → 生成 → 校验"三步。'));

children.push(h2('Q1：解释卡缺少输出校验'));
children.push(para('现象：同一预警（alert 3688）两次生成的 triggerType 一次为 SCORE（分数临界）、一次为 ABSENCE（缺勤超限）。原因是 classify() 依赖当时的 risk_state（缺课从 3 变 4 即改判），模型只是如实复述——但结果看起来自相矛盾，且没有机制发现。'));
children.push(para('建议：像出题/批改那样加三道闸门（生成后、入库前）：RULE_ECHO 必须原样包含 triggerRule；TYPE_CONSISTENT 文中类型词不得与 triggerTypeText 相悖；NO_NEW_NUMBERS 文中数字必须能在事实包中找到。任一失败则回退模板解释（模板本来就照抄系统原文），并把结果写 ai_run_step。'));
children.push(para('验收：人为把 summary 中的触发类型改错，被闸门拦下并回退模板；ai_run_step 出现 validation_status=WARN 及原因。'));

children.push(h2('Q2：解释卡/群体报告不会过期'));
children.push(para('建议：ai_alert_explain / ai_group_report 增加 fact_hash（由 alert_record.update_time 与 risk_state.last_eval_time 组合），查询时若与当前不一致则返回 stale=true，前端提示"数据已更新，请重新生成"。'));
children.push(para('验收：同一预警在缺课变化前后各生成一次，第二张自动标记过期。'));

children.push(h2('Q3：报告数字回验过于刚性'));
children.push(para('现象：Dify 报告输出里出现一个不在事实包中的数字（97.2），整篇被判 WARN 并退回模板，1349–1951 tokens 白花。'));
children.push(para('建议：改为"先修复再降级"——① 若越界数字不超过 3 个，把该句替换为不含数字的表述或删除该句后复核；② 仍不通过则带"禁用具体数字"的追加指令重试一次；③ 再失败才退回模板。并把"拦截率 / 修复率"计入 ai_run 统计，作为质量指标写进论文。'));

children.push(h2('A1 + A2：审计与成本的准确性'));
children.push(para('现象：ai_grading_suggest.model = deepseek-flash，但同一次运行的 ai_run.model 与 ai_usage.model 记的是 ai.model 标签（qwen-plus）。根因：AiRunService.start/usage 写的是 props.getModel()，而真正调用的是 props.directModel()。'));
children.push(para('建议：start/finish/usage 增加 model 与 provider 参数，调用方传 directModel()（直连）或 dify:<场景>（Dify）；解析响应 usage 中的 prompt_tokens / completion_tokens 分别落库（ai_usage 两列已存在，只是恒为空）；成本按"输入价 × prompt + 输出价 × completion"计算，并支持按模型前缀配置单价。'));
children.push(para('验收：跑一次批改，ai_run.model 与 ai_grading_suggest.model 一致；ai_usage 两列非空；换模型后成本随之变化。'));

children.push(h2('L1：限额覆盖不全'));
children.push(para('现象：overLimit 只在 GRADING / QUESTION / REPORT 判定；学生端问答（最容易刷）、解释卡、群体报告均无限制。'));
children.push(para('建议：三个入口补 overLimit；daily-limit 拆成按场景配置（问答建议 20 次/日，其余 30）；超限时写 status=LIMITED 便于统计。'));
children.push(para('验收：连打 21 次问答，第 21 次返回限额提示且不再消耗 token。'));

children.push(h2('A5：AI 口径与成绩口径混用（部分已修）'));
children.push(para('现象：报告模板已改为"过程分（期末未计入）"，但班级均分/排名仍取 score_result（已得分）→ 实测出现"过程分 65.4，班级均分 30.9，班内第 45/45"，两个尺子混在一句话里；解释卡与群体报告的分数也仍来自 score_result。'));
children.push(para('建议：在事实包里定义唯一展示指标 metric（PROCESS / TOTAL）：PARTIAL 时一律用过程分，且班级均分、排名都用同一指标计算；解释卡与群体报告同步；存量旧话术报告（7 篇 source=TEMPLATE 且含"当前总分"）重新生成或标记为 SUPERSEDED。'));

children.push(h2('D1：密钥安全（最高优先，动动手就能修）'));
children.push(para('现象：v2.0/tool/ai_guide.json 这个入库文件里明文保存着真实的 Dify 报告应用 App Key，且已随本次推送上了 Gitee 与 GitHub。'));
children.push(para('进度（2026-11-01）：① 已完成——gen_ai_guide.js 与 ai_guide.json 中的真实 Key 已全部替换为 app-****，重新生成后全仓库已无任何已入库文件包含真实 Key；② 已完成——ai_guide.json 已重新生成；③ 待执行——请到 Dify 控制台吊销并重建该应用 Key（旧 Key 仍在 Git 历史与两个远端中）；④ 之后所有文档/PPT 一律不写真实密钥，真值只放本机 ai.env。'));
children.push(para('另：DeepSeek Key 曾在对话中出现，建议演示验收后轮换一次，代码侧只需改 ai.env 一行。'));

children.push(h2('D2：仓库体积'));
children.push(para('现象：交付包 zip（历次约 36–50MB）进入 Git 历史，GitHub 推送时报 GH001 大文件告警。'));
children.push(para('建议：交付包不再入库，改用 Release 附件；若需瘦身，用 git filter-repo 重写历史（先备份、且注意两个远端需强推）。'));

children.push(h2('C1 + C2：Dify 侧的补齐'));
children.push(para('现状：代码只对报告/解释卡/群体报告提供 Dify 分支；批改/出题/问答仅有直连。已在 ai.env.example 写明"每个场景吃哪个 Key"。'));
children.push(para('建议：在 Dify 里新建两个工作流应用（输入变量：解释卡 facts + trigger_type；群体报告 facts），拿到 Key 填入 AI_ALERT_APP_KEY / AI_GROUP_REPORT_APP_KEY。若希望三个直连场景也能走 Dify，需要新增代码分支，投入产出比不高，建议保持直连。'));

// ---------------- 四、路线图
children.push(h1('四、实施路线图'));
children.push(table(
  ['阶段', '内容', '工作量', '产出 / 验收'],
  [
    ['第 0 步（安全）', 'D1 文档内真 Key 换占位 + 吊销重建；提交推送', '0.5 小时', '仓库中不再出现真实密钥'],
    ['第 1 步（RAG 铺开）', 'R1 四个场景接入检索与引用展示', '半天', '解释卡/报告/群体报告出现"依据：…"；检索缺失可降级'],
    ['第 2 步（口径统一）', 'A5 统一展示指标与班级均分/排名；旧报告清理', '半天', '报告内数字同一把尺子；3 处生成结果一致'],
    ['第 3 步（维度升级）', 'R2 向量化 + 混合检索（全文 + 向量 + RRF）', '1–2 天', '检索接口返回双路命中与融合分；ai_run_step 可见'],
    ['第 4 步（评测）', 'R4 评测集与指标（全文 / 向量 / 混合三行对比）', '1 天', '一张对比表 + 幻觉率、拒答准确率'],
    ['第 5 步（质量闸门）', 'Q1 解释卡三道闸门、Q2 过期标记、Q3 报告修复式重试', '1 天', '错误类型被拦截；旧卡片标记过期；拦截率下降'],
    ['第 6 步（审计与限额）', 'A1/A2/A3/A4 + L1', '1 天', 'ai_run.model 真实；token 拆分；三入口限额生效'],
    ['第 7 步（语料）', 'R3 导入 4 门课课件/习题/大纲，分块到 200+', '半天', '检索命中率提升，拒答率下降']
  ],
  [1500, 3800, 1100, 2670]));

// ---------------- 五、评测与论文
children.push(h1('五、评测办法与论文写作建议'));

children.push(h2('5.1 指标定义（可直接写进实验章节）'));
children.push(table(
  ['指标', '定义', '数据来源'],
  [
    ['recall@4', '标准答案所在文档是否出现在前 4 条命中中', '评测集 + 检索接口返回的 docName/sectionNo'],
    ['引用命中率', '回答中"依据"引用的文档是否在本次命中集合内', 'ai_qa_log.citations_json 与 hits 对比'],
    ['拒答准确率', '不可答问题被拒、可答问题未误拒的比例', 'ai_qa_log.refused 与标注对照'],
    ['幻觉率', '回答中出现事实包/片段之外数字或结论的比例', 'ReportComposer.invalidNumbersAgainst 等校验结果'],
    ['平均首字延迟 / 成本', '每场景 P50 耗时与 token 成本', 'ai_run.create_time→finish_time、total_tokens']
  ],
  [1400, 3800, 3870]));

children.push(h2('5.2 论文中建议呈现的图表'));
children.push(para('① 六场景与通道架构图（Dify 工作流 + 直连 + 模板三级降级）；② RAG 数据流图（离线：资料→抽取→分块→向量化→入库；在线：问题→全文+向量召回→RRF→重排→生成→引用校验→拒答）；③ 三行检索对比表（全文 / 向量 / 混合）；④ 抗幻觉三重闸门的流程图与拦截率；⑤ 六场景实测 tokens / 耗时表。'));

children.push(h2('5.3 表述建议（避免答辩被追问）'));
children.push(para('把"预警解释与学习评价"准确描述为"结构化事实注入（FactPack）的接地生成"，把"课程问答"描述为"文档检索增强（RAG）"，并说明两者共用同一套输出校验与拒答机制。这样既不夸大为"全场景 RAG"，又能体现 RAG 是系统中的关键能力。'));

// ---------------- 六、已修复
children.push(h1('六、本轮已修复项（避免重复排查）'));
children.push(table(
  ['项', '问题', '修复'],
  [
    ['F1', '无 Key 时全部退回模板，且看不出缺哪层', '新增 ai.env + run-ai.ps1；health 增加 directReady / missingKeys / scenes 逐场景通道'],
    ['F2', '推理型模型长提示被截断返回空内容', 'DashScopeClient 增加 finish_reason 诊断 + 长度不足自动翻倍重试（上限 8000）'],
    ['F3', '报告模板把"已得分"当总评说', 'PARTIAL 时改说过程分（期末未计入）；事实包新增 processScore / processCoverage'],
    ['F4', '报告数字回验过程不可观测', 'ai_run_step 记录 WARN 与越界数字列表（保留，待按 Q3 改造）']
  ],
  [700, 3600, 4770]));

// ---------------- 七、命令与附录
children.push(h1('七、常用命令与自查清单'));
children.push(para('启动（带 AI Key）：cd v2.0\\backend → .\\run-ai.ps1（读 ai.env，打码回显后启动）。'));
children.push(para('探活：GET /api/ai/health —— 看 reportWorkflowReady / directReady / reachable / scenes / missingKeys / usage。'));
children.push(para('用量：GET /api/ai/usage（需教务令牌）—— 今日调用数、今日 token、本月成本、Dify 成功数、模板降级数。'));
children.push(para('逐场景验收（每个都要看到 source=LLM/DIFY 且 tokens>0）：POST /api/student/reports/generate；POST /api/student/alerts/{id}/explain；POST /api/teacher/reports/class/{tcId}；POST /api/teacher/ai/grading/submissions/{sid}/suggest；POST /api/teacher/ai/questions/generate；POST /api/student/ai/qa。'));
children.push(para('注意：用 PowerShell 5.1 调带中文 body 的接口时，必须把 JSON 转成 UTF-8 字节再发（否则中文会变成问号，导致检索 0 命中、被误判为"系统缺陷"）。'));

children.push(para(''));
children.push(para('说明：本指南中的"待改/未修"均指截至 2026-11-01 的状态；每项改造完成时，请在本文件对应条目后补记提交号与实测数据，作为论文材料与答辩证据。', { size: SMALL, color: '666666' }));

const doc = new Document({
  styles: { default: { document: { run: { font: CJK, size: BODY } } } },
  sections: [{
    properties: { page: { margin: { top: 1134, right: 1134, bottom: 1134, left: 1134 } } },
    footers: {
      default: new Footer({
        children: [new Paragraph({
          alignment: AlignmentType.CENTER,
          children: [new TextRun({
            children: ['第 ', PageNumber.CURRENT, ' 页 / 共 ', PageNumber.TOTAL_PAGES, ' 页'],
            font: CJK, size: 18, color: '888888'
          })]
        })]
      })
    },
    children
  }]
});

const outPath = process.argv[2] || path.join(__dirname, '..', 'word', '学情预警系统_AI后续优化完善指南.docx');
Packer.toBuffer(doc).then((buf) => {
  require('fs').writeFileSync(outPath, buf);
  console.log('已生成：' + outPath + '（' + Math.round(buf.length / 1024) + ' KB）');
});
