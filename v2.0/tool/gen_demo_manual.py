# -*- coding: utf-8 -*-
"""生成《答辩演示讲解手册》Word：
   每一步都是「点什么 → 屏幕看到什么 → 怎么讲 → 出问题怎么办」四栏，
   讲解词可直接照着念；另附精简版流程、常见追问与演示纪律。
用法：py gen_demo_manual.py [输出路径]
"""
import sys
from docx import Document
from docx.shared import Pt, Cm, RGBColor
from docx.enum.text import WD_ALIGN_PARAGRAPH, WD_LINE_SPACING
from docx.enum.table import WD_TABLE_ALIGNMENT
from docx.enum.section import WD_SECTION
from docx.oxml.ns import qn
from docx.oxml import OxmlElement

OUT = sys.argv[1] if len(sys.argv) > 1 else \
    r'E:\JavaDEVIDEA\workspace\JavaProject\academic_risk_warning\v2.0\word\答辩演示讲解手册.docx'

BLUE = RGBColor(0x01, 0x71, 0xBB)
INK = RGBColor(0x22, 0x2B, 0x38)
SUB = RGBColor(0x55, 0x60, 0x70)
MUTE = RGBColor(0x8A, 0x96, 0xA3)
CJK = '宋体'
CJK_B = '微软雅黑'

doc = Document()

# ---------- 页面与默认字体 ----------
sec = doc.sections[0]
sec.page_height, sec.page_width = Cm(29.7), Cm(21.0)
sec.top_margin = sec.bottom_margin = Cm(2.2)
sec.left_margin = sec.right_margin = Cm(2.2)

normal = doc.styles['Normal']
normal.font.name = CJK
normal.font.size = Pt(10.5)
normal.element.rPr.rFonts.set(qn('w:eastAsia'), CJK)
normal.paragraph_format.line_spacing = 1.45
normal.paragraph_format.space_after = Pt(4)


def set_font(run, size=10.5, bold=False, color=INK, font=CJK):
    run.font.size = Pt(size)
    run.font.bold = bold
    run.font.color.rgb = color
    run.font.name = font
    run._element.rPr.rFonts.set(qn('w:eastAsia'), font)


def p(text='', size=10.5, bold=False, color=INK, font=CJK, align=None,
      space_before=0, space_after=4, indent=0, line=1.45):
    par = doc.add_paragraph()
    if align is not None:
        par.alignment = align
    pf = par.paragraph_format
    pf.space_before = Pt(space_before)
    pf.space_after = Pt(space_after)
    pf.line_spacing = line
    if indent:
        pf.first_line_indent = Pt(indent)
    if text:
        parts = str(text).split('**')
        for i, seg in enumerate(parts):
            if seg == '':
                continue
            set_font(par.add_run(seg), size, bold or (i % 2 == 1), color, font)
    return par


def h1(text, num=None):
    par = doc.add_paragraph()
    par.paragraph_format.space_before = Pt(16)
    par.paragraph_format.space_after = Pt(8)
    if num:
        set_font(par.add_run(num + '  '), 15, True, BLUE, CJK_B)
    set_font(par.add_run(text), 15, True, INK, CJK_B)
    # 下边框
    pPr = par._p.get_or_add_pPr()
    bdr = OxmlElement('w:pBdr')
    bottom = OxmlElement('w:bottom')
    bottom.set(qn('w:val'), 'single'); bottom.set(qn('w:sz'), '8')
    bottom.set(qn('w:space'), '4'); bottom.set(qn('w:color'), '0171BB')
    bdr.append(bottom); pPr.append(bdr)
    return par


def h2(text):
    par = doc.add_paragraph()
    par.paragraph_format.space_before = Pt(10)
    par.paragraph_format.space_after = Pt(4)
    set_font(par.add_run(text), 12, True, BLUE, CJK_B)
    return par


def h3(text):
    par = doc.add_paragraph()
    par.paragraph_format.space_before = Pt(8)
    par.paragraph_format.space_after = Pt(2)
    set_font(par.add_run(text), 11, True, INK, CJK_B)
    return par


def shade(cell, hex_color):
    tcPr = cell._tc.get_or_add_tcPr()
    shd = OxmlElement('w:shd')
    shd.set(qn('w:val'), 'clear'); shd.set(qn('w:color'), 'auto'); shd.set(qn('w:fill'), hex_color)
    tcPr.append(shd)


def script_box(lines):
    """讲解词：浅蓝底 + 左侧竖线，提醒这是可直接念的话"""
    for i, line in enumerate(lines):
        par = doc.add_paragraph()
        pf = par.paragraph_format
        pf.left_indent = Pt(10)
        pf.space_before = Pt(3 if i == 0 else 0)
        pf.space_after = Pt(3)
        pf.line_spacing = 1.5
        pPr = par._p.get_or_add_pPr()
        shd = OxmlElement('w:shd')
        shd.set(qn('w:val'), 'clear'); shd.set(qn('w:color'), 'auto'); shd.set(qn('w:fill'), 'F2F7FB')
        pPr.append(shd)
        bdr = OxmlElement('w:pBdr')
        left = OxmlElement('w:left')
        left.set(qn('w:val'), 'single'); left.set(qn('w:sz'), '18')
        left.set(qn('w:space'), '6'); left.set(qn('w:color'), '0171BB')
        bdr.append(left); pPr.append(bdr)
        set_font(par.add_run(line), 10.5, False, RGBColor(0x22, 0x2B, 0x38), CJK)


def table(rows, widths, head=True, size=9.5):
    t = doc.add_table(rows=len(rows), cols=len(rows[0]))
    t.style = 'Table Grid'
    t.alignment = WD_TABLE_ALIGNMENT.CENTER
    t.autofit = False
    for r_idx, row in enumerate(rows):
        for c_idx, val in enumerate(row):
            cell = t.cell(r_idx, c_idx)
            cell.width = Cm(widths[c_idx])
            cell.text = ''
            par = cell.paragraphs[0]
            par.paragraph_format.space_before = Pt(2)
            par.paragraph_format.space_after = Pt(2)
            par.paragraph_format.line_spacing = 1.25
            run = par.add_run(str(val))
            is_head = head and r_idx == 0
            set_font(run, size, is_head, RGBColor(0xFF, 0xFF, 0xFF) if is_head else INK, CJK_B if is_head else CJK)
            if is_head:
                par.alignment = WD_ALIGN_PARAGRAPH.CENTER
                shade(cell, '0171BB')
            elif r_idx % 2 == 0:
                shade(cell, 'F5F9FC')
    # 列宽（python-docx 需要逐列设置）
    for c_idx, w in enumerate(widths):
        for cell in t.columns[c_idx].cells:
            cell.width = Cm(w)
    return t


def footer_with_page():
    footer = sec.footer
    par = footer.paragraphs[0]
    par.alignment = WD_ALIGN_PARAGRAPH.CENTER
    set_font(par.add_run('答辩演示讲解手册 ｜ 基于 RAG 与多智能体的学情预警与个性化学习管理系统　　'), 8, False, MUTE)
    run = par.add_run()
    fld = OxmlElement('w:fldSimple')
    fld.set(qn('w:instr'), 'PAGE')
    run._element.addnext(fld)
    set_font(run, 8, False, MUTE)


# ============================================================ 封面
p('', space_after=90)
p('答辩演示讲解手册', 26, True, INK, CJK_B, WD_ALIGN_PARAGRAPH.LEFT)
p('', space_after=2)
p('系统演示流程 · 点击路径 · 逐句讲解词 · 常见追问', 12, False, BLUE, CJK_B)
p('', space_after=36)
rule = doc.add_paragraph()
pPr = rule._p.get_or_add_pPr()
bdr = OxmlElement('w:pBdr')
bottom = OxmlElement('w:bottom')
bottom.set(qn('w:val'), 'single'); bottom.set(qn('w:sz'), '10'); bottom.set(qn('w:space'), '2'); bottom.set(qn('w:color'), 'D8E3EC')
bdr.append(bottom); pPr.append(bdr)
p('', space_after=24)
p('基于 RAG 与多智能体的学情预警与个性化学习管理系统的设计与实现', 12, True, INK, CJK_B)
p('', space_after=18)
p('答辩人：狄珂　　学号：23001021010　　专业：软件工程', 11, False, SUB)
p('指导教师：（待填）　　2026 年 11 月', 11, False, SUB)
p('', space_after=40)
p('本手册用于答辩现场的系统演示：每一步都是「点什么 → 看到什么 → 怎么讲 → 出问题怎么办」，'
  '蓝色底纹段落是可直接照念的讲解词。', 9.5, False, MUTE)
doc.add_page_break()
footer_with_page()

# ============================================================ 一、怎么用
h1('这份手册怎么用', '一')
p('答辩演示最容易出问题的不是系统，而是**边点边想词**。所以本手册把每个动作拆成四件事：')
table([
    ['栏目', '含义', '使用方式'],
    ['点什么', '精确到菜单与按钮名，含备选入口', '照着点，不要临场找按钮'],
    ['看到什么', '屏幕上应出现的可核对内容', '点完先扫一眼有没有这些，再开口'],
    ['怎么讲', '蓝色底纹段，可直接照念', '念两到三句即可，不必背全文'],
    ['出问题', '常见异常与兜底话术', '异常时先讲原因，再切备用路径'],
], [2.6, 7.6, 6.4])
p('', space_after=6)
p('建议演示总时长 9 分钟：主线 7 步（表 3-1），若时间被压缩到 5 分钟，用第四章的精简版。', 10, False, SUB)

# ============================================================ 二、演示前准备
h1('演示前 5 分钟准备', '二')
h2('2.1 环境检查（逐项打勾）')
table([
    ['检查项', '操作', '期望结果'],
    ['后端已启动', '浏览器打开 http://127.0.0.1:8080/api/ai/health', '返回 200；若提示未配置模型通道，说明将走规则模板（仍可演示）'],
    ['前端已启动', '打开 http://localhost:5173', '出现登录页'],
    ['数据在位', '在数据库执行：select risk_level,count(*) from risk_state where is_void=0 group by risk_level',
     '合计 540 条（红 16 / 橙 3 / 黄 100 / 绿 421，数量随重判可变，量级一致即可）'],
    ['报告已就绪', '教师端与辅导员端各打开一次报告弹窗', '能看到历史报告，避免现场等待生成'],
    ['账号可用', '四个账号各登录一次', '见 2.2，口令均为 000000'],
], [3.0, 7.4, 6.2])

h2('2.2 演示账号')
table([
    ['角色', '账号', '用在哪些步骤', '说明'],
    ['教师', '1001', 'S1–S4', '任教「大学计算机基础（一）」01 班（教学班 201）'],
    ['教师', '1004', 'S5', '任教「数据结构（Java）」3 个教学班，课程总报告对比最明显'],
    ['辅导员', 'C001', 'S6', '带「计算机1班」等行政班'],
    ['学生', '20250001', 'S7', '有红/黄灯记录，可演示学生视角与答疑'],
], [2.2, 2.0, 3.4, 9.0])

h2('2.3 三种兜底（出问题时不慌）')
p('① **模型不可用或没网**：报告仍能生成，只是来源标注为「规则生成」——这时照讲降级链，反而证明容错设计有效。')
p('② **Dify 未启动**：系统自动走直连模型，来源标注为「AI 生成」，不影响演示。')
p('③ **报告生成很慢（超过 20 秒）**：改讲「历史报告」里的已有记录，说明生成耗时与模型推理有关，课后再补一次现场生成。')

# ============================================================ 三、主线
h1('演示主线（约 9 分钟，7 步）', '三')
h2('3.1 节奏总表')
table([
    ['步骤', '时长', '点击路径', '这一步要说明的事'],
    ['S1', '40 秒', '登录页 → 教师端（1001）', '四端角色与演示数据规模'],
    ['S2', '1.5 分', '预警监控 → 右上角选教学班 201 → 名单', '判定怎么做：三通道、分档线、快照可复算'],
    ['S3', '1 分', '名单某行「详情」→ 右侧预警详情 + AI 解释卡', '为什么被预警：事实链 + 依据 + 降级'],
    ['S4', '2 分', '预警监控右上角「班级报告」', '报告怎么写：四段结构 + 来源 + 数字对账 + 依据摘句'],
    ['S5', '1.5 分', '教师端「全部课程」→ 数据结构卡片「课程总报告」', '三种群体口径可加总：135 = 45×3'],
    ['S6', '1.5 分', '辅导员端（C001）→ 班级管理 → 选班 → 「班级报告」→ 点学生名', '行政班视角 + 点名字进学生详情（闭环）'],
    ['S7', '1 分', '学生端（20250001）→ 学情预警 → 详情；课程学习 → 课程详情 → 课程答疑', '学生视角可读、答疑带引用与拒答'],
], [1.4, 1.8, 6.4, 7.0])

# ---- S1
h2('S1　登录教师端')
h3('点什么')
p('登录页输入账号 1001、口令 000000，点击「登录」；如出现图形验证码，按提示填写。')
h3('看到什么')
p('进入教师端首页：今日待办、任教课程卡片、风险概览（红/橙/黄/绿人数）。')
h3('怎么讲')
script_box([
    '这个系统面向四类用户：学生、任课教师、辅导员、教务。教师看到的是自己任教的教学班，'
    '辅导员看到的是自己带的行政班，数据范围在服务端校验，不是靠前端隐藏菜单。',
    '演示数据是一个行政班 45 人、单门课最多 3 个教学班，共 540 条课程级判定记录。',
])
h3('出问题')
p('登录失败先确认口令是否为 000000；若提示验证码错误，刷新页面重新获取。')

# ---- S2
h2('S2　预警监控：判定是怎么算出来的')
h3('点什么')
p('左侧菜单「预警监控」→ 右上角下拉框选择教学班「CS-01-A 大学计算机基础（一）01班」→ 名单与总览随之刷新。')
h3('看到什么')
p('顶部总览（总数、红/橙/黄人数、未关闭预警数）；下方名单带等级、总分、缺勤、置信度；每行右侧「详情」。')
h3('怎么讲')
script_box([
    '这里的等级不是只看总分。判定用三个通道：A 通道看缺勤迟到这类硬门槛，B 通道看班内相对位置，'
    'C 通道做跨班可比折算——因为同一个分数在不同老师、不同难度的班里含义不一样。',
    '最后的风险分对三通道做加权取更保守的一侧，再按 75、60、40 三档映射成红橙黄绿；'
    '缺勤超过上限会触发下限保护，直接顶到红档，不能被高分掩盖。',
    '每条判定都是一份「快照」，记了当时用的引擎版本，所以任何时候都能复算，这一点答辩老师常问。',
])
h3('出问题')
p('若名单为空，说明该教学班本学期无选课或未判定；换 1004 账号讲数据结构班，或直接进 S4。')

# ---- S3
h2('S3　详情与 AI 解释卡：为什么是我')
h3('点什么')
p('名单中任选一名红灯学生，点击行尾「详情」，右侧抽屉打开。')
h3('看到什么')
p('基本信息、风险分与置信度、缺勤明细；下方「AI 解释」卡片，分四段：为什么会预警 / 你现在差多少 / 马上能做的三件事 / 数据与口径说明，末尾还有「参考依据」。')
h3('怎么讲')
script_box([
    '学生最常问的是「为什么是我」。这张卡把结论、差距、行动分开写，且每个数字都来自系统判定，'
    '不是模型自己算的——模型只负责把事实翻译成人话。',
    '如果模型不可用，这张卡会退回规则模板，仍然给出四段结构；来源字段会标注是 AI 还是规则，'
    '这样「降级」对使用者是可见的，而不是悄悄降质。',
])
h3('出问题')
p('若 AI 解释区提示未配置，直接讲降级：模板仍然四段，依据段会说明没有检索到直接条款。')

# ---- S4
h2('S4　班级报告：数字可对账（本场重点）')
h3('点什么')
p('回到「预警监控」，右上角点「班级报告」→ 弹窗显示最近一份报告；若没有，点右上「生成报告」（约 4–14 秒）。')
h3('看到什么')
p('① 标题带学期；② 来源标签（AI 生成 / 规则生成）；③ 「数字对账 N/N 项与系统判定一致」，可展开看每条「报告写 X / 事实 Y」；'
  '④ 五段正文：风险概览、共性难点、重点关注、教学建议、参考依据（含原文摘句）；⑤ 「报告涉及的学生」标签，可点击。')
h3('怎么讲')
script_box([
    '这份报告的写法是「数字由规则算、语言由模型写」：事实包先把人数、平均分、及格线、预警条数算好，'
    '模型只负责组织语言，不许自己计算。',
    '定稿前还有一道数字对账：用 14 条规则把报告里的关键数字和事实包逐项比对，'
    '现在显示 5 项全部一致；如果模型把「平均 81 分」写成 80 分，这里会标红并要求重新生成。',
    '参考依据不是套话，是检索到的课程资料原文，附了摘句；涉及的学生可以直接点，跳到学生名单继续处置。',
])
h3('出问题')
p('若来源显示「规则生成」，照讲降级链（Dify → 直连 → 模板）；若对账出现不一致，如实说明「系统会标记出来，不会假装正确」，这正是设计目标。')

# ---- S5
h2('S5　课程总报告：三种口径能对账')
h3('点什么')
p('切到教师 1004（或退出重登）→ 左侧「全部课程」→ 找到「数据结构（Java）」卡片，点右下「课程总报告」。')
h3('看到什么')
p('标题显示「3 个教学班」；正文五段含「整体概览」「教学班对比」，对比段按红黄灯人次排序，指出哪个班最吃力；数字对账同样显示一致。')
h3('怎么讲')
script_box([
    '同一个老师教三个班，班级报告只能看一个班，所以我又做了课程维度的总报告：把三个班的事实包聚合起来，'
    '人数相加 135 就是 45 乘 3，红灯 4 是三个班 1 加 2 加 1。',
    '所以这三份报告不是三个模板换皮，而是同一套事实的不同聚合口径，数字能互相加总、能对上账——'
    '这也是我论文里「可信性」那一节的主要证据。',
])
h3('出问题')
p('若该教师只有 1 个教学班，课程报告会退化为班级报告，如实说明这是已知边界，界面提示按班级报告看。')

# ---- S6
h2('S6　辅导员端：跨课程看同一个班')
h3('点什么')
p('退出登录 → 用 C001 登录 → 左侧「班级管理」→ 上方选择行政班「计算机1班」→ 点「班级报告」；随后在「报告涉及的学生」里点任一名字。')
h3('看到什么')
p('行政班报告五段：班级画像、集中风险课程、多门预警学生、干预建议、参考依据；点名字后弹窗关闭，右侧抽屉打开该生的学生详情（本学期课程、判定、干预记录）。')
h3('怎么讲')
script_box([
    '辅导员关心的不是某一门课，而是这个班哪些课集中出问题、哪些学生同时被多门课预警。'
    '所以行政班的统计口径是按选课记录统计，能指出「集中风险课程」和「多门预警学生」。',
    '点名字可以直接进学生详情，看完就能登记干预——报告不是终点，处置才是闭环的落点。',
])
h3('出问题')
p('若点名字没反应，说明该生不在当前班级数据范围内，换报告里另一位学生。')

# ---- S7
h2('S7　学生端：可解释与答疑（时间紧可跳过）')
h3('点什么')
p('退出 → 学生 20250001 登录 → 左侧「学情预警」→ 点任一预警「详情」→ 能看到 AI 解释卡与「判定依据」三点；'
  '再进「课程学习」→ 打开任一课程 → 页面下方「课程答疑（带引用）」输入一个问题。')
h3('看到什么')
p('答疑回答下方列出「依据：文档名 第 N 段」，并显示命中段落数与匹配度；若问课程资料里没有的内容，会显示「未检索到可靠依据，已拒答（不编造）」。')
h3('怎么讲')
script_box([
    '学生端强调可读与可行动：不只是告诉你被预警，还告诉你差多少、马上能做什么。',
    '答疑只在本课程资料里检索，回答必须带引用；检索不到就直接拒答，不编造——'
    '这也是这套系统对待大模型的基本态度：宁可说不知道。',
])
h3('出问题')
p('若问答较慢，先讲「检索 + 大模型」需要 1–3 秒；若模型未配置，会走规则提示，如实说明。')

# ============================================================ 四、精简版
h1('5 分钟精简版（只讲三个动作）', '四')
table([
    ['顺序', '动作', '讲解落点'],
    ['1', 'S4 班级报告：展开数字对账明细 + 指参考依据摘句', '讲「数字由规则算、语言由模型写、可对账」'],
    ['2', 'S5 课程总报告：指出「3 个教学班」与加权平均', '讲「三种口径可加总」'],
    ['3', 'S6 行政班报告 → 点学生名进详情', '讲「跨课程视角 + 处置闭环」'],
], [1.4, 8.4, 6.8])
p('其余（登录、监控名单、学生端）用一句话带过：四端角色分离、判定可复算、学生端可解释且答疑带引用。', 10, False, SUB)

# ============================================================ 五、追问
h1('常见追问与答法', '五')
table([
    ['可能的追问', '答题要点', '可指的证据'],
    ['大模型会不会编数字？', '不会直接采信：数字全部来自事实包，正文数字必须在白名单内，定稿前再用 14 条规则定向对账；不一致会在报告里标注并留痕',
     '报告弹窗的「数字对账」明细；三类报告实测 5/5、6/6、2/2'],
    ['为什么不用微服务 / 不用框架？', '一人开发、演示规模小，单体分层便于排错与讲解；AI 编排用工作流 + 分层职责实现，换取可控的降级与校验',
     '架构图四层；三层降级链'],
    ['判定公式凭什么可信？', '三通道各有明确口径，阈值与权重配置化并可版本化；做过 C1 消融（课程类型因子影响最大 3.3%）与 C2 阈值扫描（红档 70–80 不敏感）',
     '论文口径校验小节；配置表'],
    ['跨班可比是怎么做的？', '教师尺度 Δ 做收缩估计，课程因子按历史挂科率归一，再折算到同一标尺；样本不足会熔断，不硬算',
     '判定引擎说明；C1 消融结果'],
    ['RAG 效果如何？', '64 例盲评（54 可答 + 10 不可答）：混合检索 recall@4 = 1.0、nDCG@4 = 0.96；不可答拒答 0.90，可答误拒 9.3%，两者是权衡',
     '盲评表格；检索配置参数'],
    ['数据是真实的吗？', '演示库为合成数据（学号、姓名均为生成），口径与真实流程一致；真实部署需先做姓名脱敏与授权',
     '项目文档第 8.5 节'],
    ['测试做到什么程度？', '单测 25 例全通过（风险分 20 + 触发闸门 5）、接口与越权 7 例全通过、端到端按 9 节验收手册核对；未做压测、未接 CI，已列入不足',
     '验证页表格；缺陷统计 5 条'],
    ['创新点到底是什么？', '四条：数字与叙述分离且可对账、检索 query 由学情事实驱动、单向三层降级 + 来源留痕、三种群体口径可加总可复算',
     '亮点页；报告里的来源与对账字段'],
], [4.6, 8.0, 4.0])

# ============================================================ 六、纪律
h1('演示纪律与收尾', '六')
h2('6.1 三句必须说的话')
p('① 「数字来自系统判定，模型只写结论」；② 「任何一层失败都能降级，报告一定有结果」；③ 「对账不一致会标注出来，不假装正确」。')
h2('6.2 三句不要说')
p('① 不说「绝对不会错」——说「关键数字可对账、越界会被拦截」；② 不承诺「已上线给真实学生用」——说「演示环境 + 合成数据」；'
  '③ 不回避不足——未压测、未接 CI、知识库覆盖有限，主动说出来反而加分。')
h2('6.3 收尾 30 秒')
script_box([
    '这个项目最大的收获不是接上了大模型，而是认识到：大模型进入业务系统的前提，'
    '是先把数值口径做成机器可校验的东西。凡是无法断言的输出，都不该被当作结论发布。我的汇报到这里，请老师批评指正。',
])
h2('6.4 演示后自查（30 秒内完成）')
table([
    ['检查点', '合格标准'],
    ['报告来源', '三类报告的来源标签都是「AI 生成」（若为规则生成，说明原因并约定课后补测）'],
    ['数字对账', '都显示 N/N 一致；若有标记不一致，确认已口头说明'],
    ['参考依据', '每份报告都有「参考依据」段，且带摘句'],
    ['人员可点', '至少点开一名学生，确认右侧抽屉能打开学生详情'],
    ['账号退出', '演示结束前退出登录，避免下一位同学登录状态冲突'],
], [3.4, 12.6])

doc.save(OUT)
print('WROTE ' + OUT)
print('paragraphs=%d tables=%d' % (len(doc.paragraphs), len(doc.tables)))
