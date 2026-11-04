# -*- coding: utf-8 -*-
"""生成《v3.0 规划清单》Word（可打勾执行版）
用法：py gen_v30_plan_docx.py
"""
import os
from docx import Document
from docx.shared import Pt, Cm, RGBColor
from docx.enum.text import WD_ALIGN_PARAGRAPH
from docx.enum.table import WD_TABLE_ALIGNMENT
from docx.oxml.ns import qn
from docx.oxml import OxmlElement

ROOT = r'E:\JavaDEVIDEA\workspace\JavaProject\academic_risk_warning\v3.0'
OUT = os.path.join(ROOT, 'word', 'v3.0规划清单.docx')

BLUE = RGBColor(0x01, 0x71, 0xBB); INK = RGBColor(0x22, 0x2B, 0x38)
SUB = RGBColor(0x55, 0x60, 0x70); MUTE = RGBColor(0x8A, 0x96, 0xA3)
CJK = '宋体'; CJK_B = '微软雅黑'

doc = Document()
sec = doc.sections[0]
sec.page_height, sec.page_width = Cm(29.7), Cm(21.0)
sec.top_margin = sec.bottom_margin = Cm(2.0)
sec.left_margin = sec.right_margin = Cm(2.1)
normal = doc.styles['Normal']
normal.font.name = CJK; normal.font.size = Pt(10)
normal.element.rPr.rFonts.set(qn('w:eastAsia'), CJK)
normal.paragraph_format.line_spacing = 1.35
normal.paragraph_format.space_after = Pt(2)


def set_font(run, size=10, bold=False, color=INK, font=CJK):
    run.font.size = Pt(size); run.font.bold = bold; run.font.color.rgb = color; run.font.name = font
    run._element.rPr.rFonts.set(qn('w:eastAsia'), font)


def p(text='', size=10, bold=False, color=INK, font=CJK, align=None, before=0, after=2, line=1.35, indent=0):
    par = doc.add_paragraph()
    if align is not None:
        par.alignment = align
    pf = par.paragraph_format
    pf.space_before = Pt(before); pf.space_after = Pt(after); pf.line_spacing = line
    if indent:
        pf.left_indent = Pt(indent)
    if text:
        for i, seg in enumerate(str(text).split('**')):
            if seg:
                set_font(par.add_run(seg), size, bold or (i % 2 == 1), color, font)
    return par


def h1(text, num=None):
    par = doc.add_paragraph()
    par.paragraph_format.space_before = Pt(13); par.paragraph_format.space_after = Pt(5)
    if num:
        set_font(par.add_run(num + '  '), 13.5, True, BLUE, CJK_B)
    set_font(par.add_run(text), 13.5, True, INK, CJK_B)
    pPr = par._p.get_or_add_pPr()
    bdr = OxmlElement('w:pBdr'); b = OxmlElement('w:bottom')
    b.set(qn('w:val'), 'single'); b.set(qn('w:sz'), '8'); b.set(qn('w:space'), '4'); b.set(qn('w:color'), '0171BB')
    bdr.append(b); pPr.append(bdr)


def h2(text):
    par = doc.add_paragraph()
    par.paragraph_format.space_before = Pt(8); par.paragraph_format.space_after = Pt(3)
    set_font(par.add_run(text), 11.5, True, BLUE, CJK_B)


def shade(cell, color):
    tcPr = cell._tc.get_or_add_tcPr()
    shd = OxmlElement('w:shd'); shd.set(qn('w:val'), 'clear'); shd.set(qn('w:color'), 'auto'); shd.set(qn('w:fill'), color)
    tcPr.append(shd)


def table(rows, widths, size=8.8, head=True, align_first_left=True):
    t = doc.add_table(rows=len(rows), cols=len(rows[0]))
    t.style = 'Table Grid'; t.alignment = WD_TABLE_ALIGNMENT.CENTER; t.autofit = False
    for ri, row in enumerate(rows):
        for ci, val in enumerate(row):
            cell = t.cell(ri, ci); cell.width = Cm(widths[ci]); cell.text = ''
            par = cell.paragraphs[0]
            par.paragraph_format.space_before = Pt(1); par.paragraph_format.space_after = Pt(1)
            par.paragraph_format.line_spacing = 1.2
            is_head = head and ri == 0
            set_font(par.add_run(str(val)), size, is_head,
                     RGBColor(0xFF, 0xFF, 0xFF) if is_head else INK, CJK_B if is_head else CJK)
            if is_head:
                par.alignment = WD_ALIGN_PARAGRAPH.CENTER; shade(cell, '0171BB')
            elif ri % 2 == 0:
                shade(cell, 'F5F9FC')
    # 表格布局固定 + 显式 grid 宽度（否则 LibreOffice/Word 会按内容自动分配）
    tblPr = t._tbl.tblPr
    layout = OxmlElement('w:tblLayout'); layout.set(qn('w:type'), 'fixed'); tblPr.append(layout)
    tblW = OxmlElement('w:tblW'); tblW.set(qn('w:w'), str(int(sum(widths) * 567))); tblW.set(qn('w:type'), 'dxa')
    tblPr.append(tblW)
    for ci, w in enumerate(widths):
        t.columns[ci].width = Cm(w)
        for cell in t.columns[ci].cells:
            cell.width = Cm(w)
    for ri, row in enumerate(t.rows):
        trPr = row._tr.get_or_add_trPr()
        trPr.append(OxmlElement('w:cantSplit'))
        if head and ri == 0:
            trPr.append(OxmlElement('w:tblHeader'))
    return t


def mono(lines):
    for ln in lines:
        par = doc.add_paragraph()
        pf = par.paragraph_format
        pf.left_indent = Pt(10); pf.space_before = Pt(0); pf.space_after = Pt(0); pf.line_spacing = 1.25
        pPr = par._p.get_or_add_pPr()
        shd = OxmlElement('w:shd'); shd.set(qn('w:val'), 'clear'); shd.set(qn('w:color'), 'auto'); shd.set(qn('w:fill'), 'F4F7FA')
        pPr.append(shd)
        set_font(par.add_run(ln), 8.5, False, SUB, 'Consolas')


def footer():
    par = sec.footer.paragraphs[0]
    par.alignment = WD_ALIGN_PARAGRAPH.CENTER
    set_font(par.add_run('v3.0 规划清单 ｜ 学情预警与个性化学习管理系统　　'), 8, False, MUTE)
    r = par.add_run(); fld = OxmlElement('w:fldSimple'); fld.set(qn('w:instr'), 'PAGE')
    r._element.addnext(fld); set_font(r, 8, False, MUTE)


footer()

# ============================================================ 封面
p('', after=80)
p('v3.0 规划清单', 26, True, INK, CJK_B)
p('Agent 平台化 · 拟真数据 · 统计分析 · AI 亮点', 11.5, False, BLUE, CJK_B, after=10)
p('', after=26)
table([
    ['文档定位', 'v3.0 的可执行任务清单（逐条可勾选）'],
    ['上游依据', 'v3.0/PLAN_v3.0.md（终版 v3.0.3）'],
    ['范围', '六个阶段 P0–P5，共 49 项任务，约 42 人日（单人 8 周左右）'],
    ['确认口径', '关闭率分母＝周期内新增；复评窗口＝干预后 7 天；Dify 本机部署；四角色 + 记忆；看板先辅导员+教务'],
    ['资料位置', 'v3.0/（backend / frontend / ai / sql / tool / word / eval / evidence）'],
    ['记录方式', '仅本机记录，不推送远端'],
], [3.2, 13.6], head=False, size=9.5)
p('', after=20)
p('使用方式：每完成一项在「状态」列打 ✓；「验收标准」不满足不算完成。', 9, False, MUTE)
doc.add_page_break()

# ============================================================ 一、决策记录
h1('已确认决策', '一')
table([['#', '议题', '决策']] + [
    [1, 'course_factor 未初始化（v2.0 遗留）', 'v3.0 第一件事修复，并重跑 C1/C2 消融'],
    [2, '演示叙事', '保留「02 班最吃力 + 重修班黄灯多」'],
    [3, 'Agent 路线', '自研主线 + Dify 双实现（完整档）；Dify 本机部署'],
    [4, 'Agent 角色', '四角色 Planner / Executor / Critic / Writer'],
    [5, 'Agent 记忆', '要做；跨学期长期画像，写作时引用'],
    [6, '统计看板', '先辅导员 + 教务；教师端推迟 v3.1'],
    [7, '统计口径', '关闭率分母＝周期内新增；复评窗口＝干预后 7 天'],
    [8, '记忆采样', '按事件类型可开关（问卷/答疑低权重）'],
    [9, '评测集', '我起草工具链，你复核；期望数字由 SQL 生成'],
    [10, 'CI', '本地一键脚本（8 步门禁），不上云'],
    [11, '证据台账', 'Agent 每个数字必须命中台账（取代单事实包白名单）'],
    [12, '对照组', '演示数据构造延迟干预组，使改善率可归因'],
    [13, '记忆合规 + 草稿追踪', '记忆可查可删；干预表记 ai_draft_id'],
    [14, '自适应编排', '简单问题 2 角色、复杂问题 4 角色（auto/2/4）'],
    [15, '最小交付切分', 'P0+P1 为可随时停下的最小可用集'],
], [0.9, 5.6, 10.3])

# ============================================================ 二、验收指标
h1('总体验收指标', '二')
table([['维度', '指标', '门槛']] + [
    ['数据', 'C1 消融效应量', '教师松紧 1.5–3%、课程因子 2–4%、历史挂科率 1–2%'],
    ['数据', '班间 avg 差 / 班内 sd', '同课程班间 6–12 分；班内 sd 9–12'],
    ['数据', '重算流水线', '一条命令跑通，耗时 < 3 分钟'],
    ['Agent', '30 题：工具选择/数字一致/P95/平均步数', '≥90% / 100% / <8s / 2.5–3.5'],
    ['Agent', '证据台账命中率', '100%（每个数字可追到某步工具返回）'],
    ['Agent', '护栏注入 10 例', '100% 有明确降级与留痕'],
    ['Agent', '自适应编排', 'auto 模式调用次数比固定 4 角色低 ≥25%，质量不降'],
    ['Agent', '记忆有效性', '同生二次提问能引用上次结论；不重复问已知信息'],
    ['Parity', 'LOCAL vs DIFY（30 题，共享 prompt）', '关键数字一致 ≥95% 或方向一致 ≥90%'],
    ['统计', '图表 ↔ SQL 一致', '100%（脚本断言）；首屏 <2s；快照过期可见'],
    ['统计', '对照组差值', '干预组 7 天改善率 − 对照组 ≥20 个百分点'],
    ['合规', '记忆可查可删', '学生端可见摘要并可删除，删除后不再注入'],
], [1.8, 6.4, 8.6])

# ============================================================ 三、创新点
h1('创新点定位（v3.0 新增）', '三')
p('v2.0 的底色是「集成/应用创新」；v3.0 通过三件事把创新抬到方法层，另配四条支撑与两条应用创新。', 9.5, False, SUB)
table([['类型', '创新点', '机制（一句话）', '证据 / 边界']] + [
    ['方法（主）', 'M-1 跨步骤证据台账', '把模型输出当声明集合，每个数字必须命中某步工具返回的台账；未命中→受控重写→降级',
     '台账命中率 100%、注入检出率、重写收敛率；边界：只覆盖数值型声明'],
    ['方法（主）', 'M-3 可归因成效评估', '延迟干预对照组 + 固定 T+7 窗口，比较改善率差值并列出偏倚来源',
     '差值 ≥20 个百分点、跨学期可复现；边界：准实验，非净效应'],
    ['方法（支撑）', 'M-2 规则化 Critic', '每类问题配最小证据集，评审=契约检查+语义判断；连续同工具同参数判空转',
     '信息不足用例召回 ≥80%；边界：依赖问题分类'],
    ['方法（支撑）', 'M-4 可撤回加权记忆', '高权重进摘要、低权重进事件流；删除即不注入，保留审计位',
     '同生引用率、删除后 0 泄漏；边界：摘要必然有损'],
    ['方法（支撑）', '降级不降约束', '三通道都满足同一组结构+数字约束，只降表达质量',
     '关 key/越界/Dify 超时三种场景断言通过'],
    ['方法（支撑）', 'SQL 生成期望值的评测协议', '评测期望数字由 SQL 真值生成，避免自证',
     '30 题基线可复现；边界：需维护 SQL 与题目映射'],
    ['应用（主）', 'A-1 可核查对话式研判', '问一句话多步取证，轨迹可逐步点开核对数字来源',
     '30 题工具链与数字、轨迹页可演示；边界：定性结论仍需人工'],
    ['应用（支撑）', 'A-2 预警作为可评估项目', '学院层面看关闭率/时长/改善率与对照差值',
     '五个闭环指标有周趋势'],
    ['应用（支撑）', 'A-3 评分公平性可视化', 'Δ、收缩 λ、课程因子、熔断原因可视化，服务教研',
     '教务端校准散点与熔断清单'],
    ['应用（支撑）', 'A-4 低代码可移植', '同一 ToolSpec 生成 OpenAPI，自研与 Dify 双跑并出 parity',
     'parity 报告 + 画布演示'],
], [1.7, 3.9, 6.6, 4.6])

# ============================================================ 四、任务清单
PHASES = [
    ('P0 数据底座', '0.5–1 周', '先把数据修对：能让三通道产生可解释差异，且一条命令可重算/可回滚', [
        ('P0-1', '建立 v3.0 目录骨架 + snapshot_v2.0.sql 回滚快照', '目录与快照脚本', '回滚后 v2.0 验收脚本全绿', 0.5),
        ('P0-2', 'course_factor 初始化（clamp 公式）+ 引擎 NULL 兜底与 WARN', 'SQL + 引擎改动', '5 行 factor 非空、is_initialized=1；NULL 时按 1.0 且有日志', 0.5),
        ('P0-3', 'λ 随样本量生效（λ=n/(n+k)）+ 造 1–2 个样本<10 的熔断案例', 'SQL + 熔断案例', 'λ 不再恒为 0.62；熔断班 fused=1 且有原因', 0.5),
        ('P0-4', '拟真数据生成器：教师 12–15 / 课程 8–10 / 班 6–8 / 3 学期 / 对照组 60:40', '生成器 + params.json', '参数化、同 seed 同结果；班间差 6–12、sd 9–12', 1.5),
        ('P0-5', '重算流水线：seed→判定→报告→快照→基线（一条命令）', 'run_v3_pipeline.ps1', '幂等；全流程 <3 分钟；失败可续跑', 1.0),
        ('P0-6', '数据校验脚本 + data_story.md（每班/课的成因说明）', '校验脚本 + 说明', '分类合计=样本数、无孤儿、三通道非空率 100%', 0.75),
        ('P0-7', '重跑 C1/C2 消融，产出「修前 vs 修后」对照', '证据卡 evidence/P0.md', '效应量达标（1.5–3% / 2–4% / 1–2%）', 0.5),
        ('P0-8', 'ci.ps1 骨架：编译/单测/数据校验/报告对账', 'ci.ps1 + ci_report.md', '四步全绿；非零退出阻断', 0.75),
    ]),
    ('P1 统计模块（最小可用集）', '1 周', '把"预警有没有用"做成可看的指标与图表，物化 + 失效可追溯', [
        ('P1-1', '五个闭环指标口径规格（含 SQL）', 'docs/metrics.md', '每个指标有主口径+辅助口径+SQL', 0.5),
        ('P1-2', 'stat_daily_class / stat_weekly_kpi / stat_intervention_effect DDL', 'v3.0/sql/5x_stats.sql', '三表结构含 engine_version/source_max_update_time/is_stale', 0.5),
        ('P1-3', '物化任务 + 失效校验（过期自动重算并标 stale）', 'StatsJob + 校验', '改判定后重算生效；界面显示"数据重算于…"', 1.0),
        ('P1-4', '/api/stats 七接口（overview/trend/distribution/compare/funnel/effect/export）', 'Controller+Service', '统一 current/previous/deltaRate', 1.5),
        ('P1-5', '辅导员看板（趋势/多门预警/成效漏斗/对照组/班级对比）', 'CounselorStats.vue', '5 张图可交互，含对照组差值', 1.5),
        ('P1-6', '教务看板（覆盖率/集中风险 Top5/因子校准散点/时效分布/AI 质量卡）', 'AdminStats.vue', '5 张图；散点能看出挂科率与因子关系', 1.5),
        ('P1-7', '口径说明气泡 + 导出（CSV/Excel）', '组件 + 导出接口', '每图可点开定义与 SQL；导出数字与页面一致', 0.5),
        ('P1-8', '图表↔SQL 一致断言脚本', 'verify_stats.ps1', '7 接口逐一比对通过', 0.5),
        ('P1-9', '证据卡 P1（看板截图 + 复现命令）', 'evidence/P1.md', '含对照组差值数据', 0.25),
    ]),
    ('P2 Agent 平台化', '2 周', '契约 → 工具 → 四角色 → 台账 → 记忆 → 观测 → 评测', [
        ('P2-1', 'ToolSpec schema + 9 工具定义（含 evidence_fields）', 'tools.yaml', 'schema 校验通过；可生成 OpenAPI', 1.0),
        ('P2-2', '本地工具注册表 + 参数校验/超时/结果摘要', 'ToolRegistry', '9 工具可调用；非法参数 100% 拦截', 1.5),
        ('P2-3', '四角色编排（Planner/Executor/Critic/Writer）+ 步数上限', 'AgentOrchestrator', '单轮 ≤4 步；超限中止并降级', 2.0),
        ('P2-4', 'Critic 最小证据集 + 空转检测（同工具同参数连续两次判失败）', 'CriticRules', '10 例信息不足 ≥8 次正确要求取证', 0.75),
        ('P2-5', 'EvidenceLedger：入账/校验/一次重写/降级 + 落库', 'Ledger + 校验', '台账命中率 100%；未命中必标红', 1.5),
        ('P2-6', '自适应编排 auto/2/4 + 配置项', 'agent.roles 配置', 'auto 调用次数降 ≥25% 且质量不降', 0.75),
        ('P2-7', '记忆表 + 写入/读取 + 类型开关（高/低权重分级）', 'student_memory*', '摘要 ≤300 字；关闭类型即停写', 1.25),
        ('P2-8', '记忆合规：学生端「我的画像」可查可删', 'StudentMemory.vue', '删除后 getStudentMemory 不再返回', 0.75),
        ('P2-9', 'ai_run/step 扩展 + 轨迹页（角色/工具/入参/返回/校验/数字溯源）', 'Trace.vue', '每步可展开；数字可点回来源步', 1.5),
        ('P2-10', '30 题评测集起草 + SQL 生成期望值 + 评测脚本', 'eval/agent_cases.json + run_eval', '期望数字非手写；报告含工具链对比', 1.5),
        ('P2-11', '护栏注入 10 例（越界/超步/工具报错）回归', 'inject_cases.json', '10/10 有明确降级与留痕', 0.75),
        ('P2-12', '证据卡 P2（轨迹截图 + 评测报告）', 'evidence/P2.md', '含工具选择正确率与台账命中率', 0.25),
    ]),
    ('P3 Dify 双实现（本机）', '1 周', '同一契约在 Dify 侧跑通，并给出 parity 证明', [
        ('P3-1', '本机 Dify 启动脚本 + 自检（8090、模型 Key）', 'start_dify.ps1', '一条命令拉起；自检通过', 0.5),
        ('P3-2', 'OpenAPI 生成器 + 导入 Dify 自定义工具（8 个只读工具）', 'tools.openapi.json', 'Dify 工具列表可见并可试调用', 1.0),
        ('P3-3', '建 Dify Agent 应用（非普通工作流），共享同一份 system prompt', '应用配置导出', 'prompt 与本地同源；inputs 仅 3 个', 1.0),
        ('P3-4', 'DifyAgentProvider（输出 JSON 契约 + 同套校验含台账）', 'Provider 实现', '校验不过即降级到 LOCAL', 1.25),
        ('P3-5', 'Parity 测试 30 题 + 对比报告', 'parity_report.md', '数字一致 ≥95% 或方向 ≥90%', 1.0),
        ('P3-6', '演示录屏（1–2 分钟）+ 降级演练', 'demo_dify.mp4', '断网/Dify 停服时能讲降级', 0.5),
        ('P3-7', '证据卡 P3', 'evidence/P3.md', '含 parity 表与截图', 0.25),
    ]),
    ('P4 AI 亮点收口', '1 周', '把 AI 能力做成可度量、可追踪、可演', [
        ('P4-1', '报告→干预草稿（约谈提纲/补交清单，只出草稿）', 'DraftService + UI', '不落库；生成含对象与频次', 1.0),
        ('P4-2', '草稿采纳追踪（ai_draft_id）+ 采纳率/编辑字数指标', '字段 + 指标', '能统计采纳率与平均编辑字数', 0.5),
        ('P4-3', 'RAG 评测看板（64 例，按课程/题型，可一键重跑）', 'RagEval.vue', '指标可回归；重跑结果落库', 1.0),
        ('P4-4', '幻觉/降级看板（台账未命中/对账不一致/来源分布，按周）', 'AiQuality.vue', '四条趋势线有数据', 0.75),
        ('P4-5', '个性化解释卡（引用记忆，动作顺序随画像调整）', 'ExplainCard v2', '同生两次解释动作命中率提升', 0.75),
        ('P4-6', '流式 SSE（step_start/step_end/token/answer）+ 轨迹同步渲染', 'SSE + 前端', '首字 <1s；步骤实时出现', 1.0),
        ('P4-7', '证据卡 P4', 'evidence/P4.md', '含四项指标截图', 0.25),
    ]),
    ('P5 文档与答辩', '0.5–1 周', '把 v3.0 讲清楚，并保住可复现', [
        ('P5-1', 'v3.0 说明文档（架构/指标/证据/边界）', 'v3.0/README.md', '含复现命令与指标出处', 1.0),
        ('P5-2', '答辩 PPT 增页（Agent 架构/台账/统计/对照组/parity）', 'PPT 更新', '新增 4–5 页，可与 v2.0 版合并', 1.0),
        ('P5-3', '演示脚本更新（v3.0 点击路径与新功能）', '演示手册增补', '每步含"看到什么/怎么讲"', 0.5),
        ('P5-4', '全量回归 + 行为基线更新', 'baseline.json', 'CI 8 步全绿；diff 已确认', 0.5),
        ('P5-5', '证据卡汇总 + 论文创新点素材（M-1/M-3/A-1）', 'evidence/SUMMARY.md', '三张证据卡可直引', 0.5),
        ('P5-6', '提交包整理（源码/SQL/文档）', '提交文件夹', '结构同 v2.0', 0.25),
    ]),
]

h1('任务清单（逐条勾选）', '四')
p('每阶段先给阶段目标与出口，再列任务。人日为粗略估计（单人），验收标准不满足不计完成。', 9, False, SUB)
for name, dur, goal, tasks in PHASES:
    h2(f'{name}　（{dur}，{len(tasks)} 项）')
    p('阶段目标：' + goal, 9, False, SUB, after=3)
    rows = [['编号', '任务', '产出', '验收标准', '人日', '状态']]
    total = 0.0
    for t in tasks:
        total += t[4]
        rows.append([t[0], t[1], t[2], t[3], f'{t[4]:.2f}', '□'])
    rows.append(['', f'小计：{len(tasks)} 项', '', '', f'{total:.2f}', ''])
    table(rows, [1.25, 5.35, 2.9, 5.0, 1.3, 1.0], size=8.3)

# ============================================================ 五、目录结构
h1('v3.0 目录结构（对齐 v2.0）', '五')
mono([
    'v3.0/',
    '├─ backend/            后端增量（判定修复、统计接口、Agent 编排、工具注册）',
    '├─ frontend/           前端增量（统计看板、轨迹页、记忆页、草稿交互）',
    '├─ ai/                 本机 Dify 配置与 Agent 应用导出、共享 prompt',
    '│  └─ dify/            启动脚本与导出的 workflow/agent 配置',
    '├─ sql/                v3.0 数据脚本（修复、拟真生成、统计表）',
    '│  └─ seeds/           params.json、data_story.md',
    '├─ tool/               生成器、重算流水线、ci.ps1、评测脚本',
    '├─ eval/               agent_cases.json、baseline.json、parity_report.md',
    '├─ evidence/           P0–P5 证据卡（截图 + SQL/命令 + 关键数字）',
    '├─ word/               v3.0 规划清单.docx（本文档）与后续说明',
    '├─ PLAN_v3.0.md        规划正文（终版 v3.0.3）',
    '└─ README.md           入口说明（复用 v2.0 源码，本目录放增量与文档）',
])
p('说明：v3.0 不复制 v2.0 源码，backend/frontend 只放增量与改动说明；数据与文档全部落在本目录，便于随时回滚到 v2.0 快照。', 9, False, MUTE, before=3)

# ============================================================ 六、风险与不做
h1('风险与对策', '六')
table([['风险', '对策']] + [
    ['双实现分叉（本地能查、Dify 侧查不到）', 'ToolSpec 单一来源 + 共享 prompt + parity 进 CI'],
    ['多智能体变"多次调用"', '四角色各自验收；Critic 连续同工具同参数判空转'],
    ['四角色成本翻倍', '自适应 auto（2/4 角色），评测对比质量与调用次数'],
    ['台账形同虚设', '台账未命中率进 CI 门禁；轨迹页每个数字可点开'],
    ['数据改造破坏 v2.0 演示', '独立 v3.0/sql + snapshot 回滚 + 改造后先跑 v2.0 验收'],
    ['快照与业务表不一致', 'engine_version + source_max_update_time 失效校验 + is_stale 展示'],
    ['记忆隐私', '学生端可查可删；关闭类型即停止注入'],
    ['改善率不可归因', '对照组（延迟干预 40%）+ 差值指标'],
    ['范围过大中途打断', 'P0+P1 为最小可用集；P2 之后为增强'],
], [6.4, 10.4])

h1('明确不做', '七')
table([['想法', '不做的理由']] + [
    ['评测集扩到 100 题', '30 题已覆盖类型，扩量只增加标注成本'],
    ['给每个教师/班都做报告', '演示只需 2–3 份讲得清的报告'],
    ['向量化记忆（embedding 检索历史）', '演示规模下摘要足够，成本收益不划算'],
    ['接 LangChain4j 等 agent 框架', '护栏/台账/留痕是核心资产，套框架要迁就抽象'],
    ['教师端统计看板（v3.0 内）', '先做辅导员+教务，教师端 v3.1'],
], [6.0, 10.8])

# ============================================================ 八、工程约定
h1('工程约定（CI / 基线 / 证据卡）', '八')
h2('CI 八步（本地一键，非零退出即不可提交）')
p('1 编译 → 2 单测 → 3 数据校验 → 4 报告对账（不一致=0） → 5 快照一致性断言 → 6 Agent 冒烟（30 题取 5，工具选择 ≥80%） → 7 行为基线 diff（关键报告数字/统计指标/评测汇总） → 8 输出 ci_report.md', 9, False, INK)
h2('行为基线')
p('eval/baseline.json 保存：三份演示报告的关键数字、五个统计指标、Agent 评测汇总。diff 超阈值即失败，更新基线需人工确认。', 9, False, INK)
h2('证据卡')
p('每阶段产出 evidence/P{n}.md：截图要点 + 复现 SQL/命令 + 关键数字。P0 的证据卡即「修前 vs 修后的 C1 消融对照」。', 9, False, INK)

# ============================================================ 九、起步顺序
h1('P0 起步顺序（第一周）', '九')
for i, t in enumerate([
    'v3.0/sql 与 snapshot_v2.0.sql（先能回滚，再动数据）',
    'course_factor 初始化 + 引擎 NULL 兜底',
    'λ 随样本量生效 + 熔断案例',
    '拟真数据生成器（含对照组 60/40）',
    '重算流水线（一条命令）',
    '数据校验 + data_story.md',
    '重跑 C1/C2 → 出「修前 vs 修后」对照（P0 证据卡）',
    'ci.ps1 骨架（编译/单测/数据校验/报告对账）',
], 1):
    p(f'{i}. {t}', 9.5, False, INK, after=1)

doc.save(OUT)
print('WROTE ' + OUT)
print('paragraphs=%d tables=%d' % (len(doc.paragraphs), len(doc.tables)))
