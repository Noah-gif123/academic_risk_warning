# -*- coding: utf-8 -*-
"""按《项目PPT样例.pptx》的视觉语言重做答辩 PPT：
   深蓝 #0171BB + 浅蓝 #5EA3CC、微软雅黑、大字号标题、章节页大数字、底部进度圆点。
   以样例文件为基底（继承其主题与版式），清空原内容后重新编排，保持简洁、无动画。
用法：py gen_defense_ppt2.py [模板路径] [输出路径]
"""
import sys
from pptx import Presentation
from pptx.util import Inches, Pt, Emu
from pptx.dml.color import RGBColor
from pptx.enum.text import PP_ALIGN, MSO_ANCHOR
from pptx.enum.shapes import MSO_SHAPE
from pptx.oxml.ns import qn

TPL = sys.argv[1] if len(sys.argv) > 1 else \
    r'E:\JavaDEVIDEA\workspace\JavaProject\academic_risk_warning\v2.0\doc_build\tpl\项目PPT样例.pptx'
OUT = sys.argv[2] if len(sys.argv) > 2 else \
    r'E:\JavaDEVIDEA\workspace\JavaProject\academic_risk_warning\v2.0\word\基于RAG与多智能体的学情预警与个性化学习管理系统_答辩PPT.pptx'

BLUE = RGBColor(0x01, 0x71, 0xBB)      # 主色（模板 accent2/dk2）
LBLUE = RGBColor(0x5E, 0xA3, 0xCC)     # 辅色（模板 accent1）
PALE = RGBColor(0xE8, 0xF1, 0xF8)      # 浅底
INK = RGBColor(0x22, 0x2B, 0x38)       # 正文
SUB = RGBColor(0x55, 0x60, 0x70)       # 次文
LINE = RGBColor(0xD8, 0xE3, 0xEC)
WHITE = RGBColor(0xFF, 0xFF, 0xFF)
FONT = '微软雅黑'
SLIM = 'Arial'                          # 数字/西文
HEAVY = 'Arial Black'

prs = Presentation(TPL)

# ---- 清空模板原有幻灯片，只保留主题与母版 ----
sldIdLst = prs.slides._sldIdLst
for sld in list(sldIdLst):
    rId = sld.get(qn('r:id'))
    prs.part.drop_rel(rId)
    sldIdLst.remove(sld)

BLANK = None
for i, lay in enumerate(prs.slide_layouts):
    if lay.name.strip() == '空白':
        BLANK = lay
        break
if BLANK is None:
    BLANK = prs.slide_layouts[1]

prs.slide_width = Inches(13.333)
prs.slide_height = Inches(7.5)
SW, SH = 13.333, 7.5
SECTIONS = ['背景与目标', '方案与架构', '关键技术', '实现与验证', '评价与展望']


def ns():
    return prs.slides.add_slide(BLANK)


def tb(s, x, y, w, h, anchor=MSO_ANCHOR.TOP):
    box = s.shapes.add_textbox(Inches(x), Inches(y), Inches(w), Inches(h))
    tf = box.text_frame
    tf.word_wrap = True
    tf.vertical_anchor = anchor
    tf.margin_left = 0; tf.margin_right = 0; tf.margin_top = 0; tf.margin_bottom = 0
    return tf


def para(tf, text, size=18, bold=False, color=INK, first=False, before=0, after=8,
         align=PP_ALIGN.LEFT, line=1.3, font=FONT):
    p = tf.paragraphs[0] if first else tf.add_paragraph()
    p.alignment = align
    p.space_before = Pt(before)
    p.space_after = Pt(after)
    p.line_spacing = line
    r = p.add_run()
    r.text = text
    r.font.size = Pt(size)
    r.font.bold = bold
    r.font.color.rgb = color
    r.font.name = font
    return p


def rect(s, x, y, w, h, fill=PALE, line_color=None, shape=MSO_SHAPE.RECTANGLE, radius=None):
    sh = s.shapes.add_shape(shape, Inches(x), Inches(y), Inches(w), Inches(h))
    if radius is not None and shape == MSO_SHAPE.ROUNDED_RECTANGLE:
        sh.adjustments[0] = radius
    sh.fill.solid(); sh.fill.fore_color.rgb = fill
    if line_color is None:
        sh.line.fill.background()
    else:
        sh.line.color.rgb = line_color; sh.line.width = Pt(0.75)
    sh.shadow.inherit = False
    return sh


def bottom(s, sh):
    """把形状压到最底层（不遮挡母版以外的内容，但保持为背景）"""
    s.shapes._spTree.remove(sh._element)
    s.shapes._spTree.insert(2, sh._element)


def white_card(s, x=0.34, y=0.34, w=12.65, h=6.8):
    """内容页白色卡片：遮住母版蓝色背景图，保证正文可读；四周留一圈模板底纹"""
    sh = rect(s, x, y, w, h, fill=WHITE)
    bottom(s, sh)
    return sh


def dots(s, idx, total=5, y=6.86, on_blue=False):
    """底部进度圆点：当前章节实心，其余浅色（模板的章节进度语言）"""
    d, gap = 0.15, 0.13
    width = total * d + (total - 1) * gap
    x = (SW - width) / 2 + 3.6      # 靠右一些，给页脚文字留位置
    for i in range(total):
        on = (i == idx)
        c = WHITE if (on_blue and on) else (RGBColor(0xBF, 0xD9, 0xEA) if on_blue else (BLUE if on else RGBColor(0x8F, 0xC2, 0xDE)))
        sh = s.shapes.add_shape(MSO_SHAPE.OVAL, Inches(x), Inches(y), Inches(d), Inches(d))
        sh.fill.solid(); sh.fill.fore_color.rgb = c
        sh.line.fill.background(); sh.shadow.inherit = False
        x += d + gap


def footer(s, page):
    tf = tb(s, 0.86, 6.86, 8.6, 0.3)
    para(tf, '基于 RAG 与多智能体的学情预警与个性化学习管理系统', size=9, color=RGBColor(0x98, 0xA6, 0xB4), first=True, after=0)
    tf2 = tb(s, 11.6, 6.86, 0.9, 0.3)
    para(tf2, str(page), size=10, color=RGBColor(0x98, 0xA6, 0xB4), first=True, after=0, align=PP_ALIGN.RIGHT)


def content_slide(sec_idx, kicker, title, page):
    """常规内容页：白卡片 + 小标签 + 大标题 + 蓝线 + 页脚 + 进度点"""
    s = ns()
    white_card(s)
    # 左上角短蓝条 + 小标签
    rect(s, 0.86, 0.62, 0.5, 0.055, fill=BLUE)
    tf = tb(s, 0.86, 0.78, 11.6, 0.3)
    para(tf, kicker, size=12, color=LBLUE, first=True, after=0)
    tf = tb(s, 0.86, 1.12, 11.6, 0.62)
    para(tf, title, size=30, bold=True, color=INK, first=True, after=0, line=1.1)
    rect(s, 0.86, 1.86, 11.62, 0.012, fill=LINE)
    dots(s, sec_idx)
    footer(s, page)
    return s


def section_slide(sec_idx, page, lead=None):
    """章节过渡页：整屏深蓝 + 大号章节数字 + 章节名 + 进度点"""
    s = ns()
    rect(s, 0, 0, SW, SH, fill=BLUE)
    # 右侧淡色大色块，模仿模板的装饰层次
    sh = s.shapes.add_shape(MSO_SHAPE.OVAL, Inches(9.2), Inches(-1.4), Inches(6.2), Inches(6.2))
    sh.fill.solid(); sh.fill.fore_color.rgb = LBLUE; sh.fill.transparency = 0.25
    sh.line.fill.background(); sh.shadow.inherit = False

    tf = tb(s, 1.05, 2.05, 2.0, 1.6)
    para(tf, '%02d' % (sec_idx + 1), size=72, bold=True, color=WHITE, first=True, after=0, font=HEAVY)
    tf = tb(s, 1.05, 3.55, 9.6, 1.0)
    para(tf, SECTIONS[sec_idx], size=44, bold=True, color=WHITE, first=True, after=0)
    if lead:
        tf = tb(s, 1.05, 4.55, 9.6, 0.6)
        para(tf, lead, size=16, color=RGBColor(0xDC, 0xEC, 0xF7), first=True, after=0)
    dots(s, sec_idx, y=6.7, on_blue=True)
    tf = tb(s, 11.4, 6.66, 1.1, 0.3)
    para(tf, str(page), size=10, color=RGBColor(0xBF, 0xD9, 0xEA), first=True, after=0, align=PP_ALIGN.RIGHT)
    return s


def bullets(s, items, x=0.86, y=2.05, w=11.6, size=18, gap=12, sub_size=14):
    tf = tb(s, x, y, w, 4.6)
    first = True
    for it in items:
        if isinstance(it, tuple):
            head, detail = it
            # 小方块项目符号
            para(tf, '■  ' + head, size=size, bold=True, color=INK, first=first, after=3, line=1.25)
            first = False
            if detail:
                para(tf, '     ' + detail, size=sub_size, color=SUB, after=gap, line=1.35)
        else:
            para(tf, '■  ' + it, size=size, bold=True, color=INK, first=first, after=gap, line=1.3)
            first = False


def table(s, data, x, y, w, col_w, head_size=13, body_size=13, row_h=0.44, head_h=0.44, first_bold=False):
    rows, cols = len(data), len(data[0])
    shp = s.shapes.add_table(rows, cols, Inches(x), Inches(y), Inches(w), Inches(head_h + row_h * (rows - 1)))
    t = shp.table
    t.horz_banding = False
    for i, cw in enumerate(col_w):
        t.columns[i].width = Inches(cw)
    t.rows[0].height = Inches(head_h)
    for r in range(1, rows):
        t.rows[r].height = Inches(row_h)
    for r, row in enumerate(data):
        for c, val in enumerate(row):
            cell = t.cell(r, c)
            cell.margin_left = Inches(0.09); cell.margin_right = Inches(0.07)
            cell.margin_top = Inches(0.02); cell.margin_bottom = Inches(0.02)
            cell.vertical_anchor = MSO_ANCHOR.MIDDLE
            cell.fill.solid()
            cell.fill.fore_color.rgb = BLUE if r == 0 else (WHITE if r % 2 else RGBColor(0xF5, 0xF9, 0xFC))
            p = cell.text_frame.paragraphs[0]
            p.alignment = PP_ALIGN.CENTER if (r == 0 or c > 0) else PP_ALIGN.LEFT
            run = p.add_run(); run.text = str(val)
            run.font.size = Pt(head_size if r == 0 else body_size)
            run.font.bold = (r == 0) or (first_bold and c == 0)
            run.font.name = FONT
            run.font.color.rgb = WHITE if r == 0 else INK
    return t


def label_box(s, x, y, w, h, title, sub=None, filled=False, size=14):
    sh = rect(s, x, y, w, h, fill=BLUE if filled else WHITE,
              line_color=None if filled else LINE, shape=MSO_SHAPE.ROUNDED_RECTANGLE, radius=0.08)
    tf = sh.text_frame
    tf.word_wrap = True
    tf.vertical_anchor = MSO_ANCHOR.MIDDLE
    tf.margin_left = Inches(0.08); tf.margin_right = Inches(0.08)
    tf.margin_top = Inches(0.02); tf.margin_bottom = Inches(0.02)
    p = tf.paragraphs[0]; p.alignment = PP_ALIGN.CENTER
    r = p.add_run(); r.text = title
    r.font.size = Pt(size); r.font.bold = True; r.font.name = FONT
    r.font.color.rgb = WHITE if filled else INK
    if sub:
        p2 = tf.add_paragraph(); p2.alignment = PP_ALIGN.CENTER
        r2 = p2.add_run(); r2.text = sub
        r2.font.size = Pt(size - 3); r2.font.name = FONT
        r2.font.color.rgb = RGBColor(0xE2, 0xF0, 0xFA) if filled else SUB
    return sh


def right_arrow(s, x, y, w=0.42, h=0.26, color=None):
    sh = s.shapes.add_shape(MSO_SHAPE.RIGHT_ARROW, Inches(x), Inches(y), Inches(w), Inches(h))
    sh.fill.solid(); sh.fill.fore_color.rgb = color or LBLUE
    sh.line.fill.background(); sh.shadow.inherit = False


def note(s, text):
    s.notes_slide.notes_text_frame.text = text


# ============================================================ 01 封面
s = ns()
# 左侧保留模板底纹，右侧白面板承载文字（避免文字压在底纹上不可读）
white_card(s, x=5.35, y=0, w=7.99, h=SH)
rect(s, 6.0, 1.62, 0.62, 0.055, fill=BLUE)
tf = tb(s, 6.0, 1.8, 6.9, 0.35)
para(tf, '本科毕业设计（论文）答辩', size=12, color=LBLUE, first=True, after=0)

tf = tb(s, 6.0, 2.2, 6.95, 2.5)
para(tf, '基于 RAG 与多智能体的', size=26, bold=True, color=INK, first=True, after=6, line=1.3)
para(tf, '学情预警与个性化', size=26, bold=True, color=INK, after=2, line=1.3)
para(tf, '学习管理系统的设计与实现', size=26, bold=True, color=BLUE, after=0, line=1.3)

rect(s, 6.0, 4.72, 6.6, 0.012, fill=LINE)
tf = tb(s, 6.0, 4.92, 6.9, 1.4)
para(tf, '答辩人：狄珂', size=13, color=SUB, first=True, after=6)
para(tf, '学　号：23001021010　　专业：软件工程', size=13, color=SUB, after=6)
para(tf, '指导教师：（待填）　　2026 年 11 月', size=13, color=SUB, after=0)
tf = tb(s, 6.0, 6.62, 6.9, 0.35)
para(tf, '汇报分五部分：背景 → 方案 → 关键技术 → 验证 → 评价', size=10.5, color=RGBColor(0x9A, 0xA8, 0xB6), first=True, after=0)
note(s, '开场一句话：把「算得准」和「讲得清」分开做——数值由规则算，语言由模型写，依据由检索给。')

# ============================================================ 02 目录
s = ns()
white_card(s)
rect(s, 0.86, 0.7, 0.5, 0.055, fill=BLUE)
tf = tb(s, 0.86, 0.86, 3.0, 0.3)
para(tf, 'CONTENTS', size=12, color=LBLUE, first=True, after=0)
tf = tb(s, 0.86, 1.15, 3.2, 0.9)
para(tf, '目录', size=40, bold=True, color=INK, first=True, after=0)
rect(s, 3.55, 1.3, 0.012, 4.9, fill=LINE)

items = [
    ('01', '背景与目标', '为什么做、现有做法差在哪、边界是什么'),
    ('02', '方案与架构', '三条主线、分层结构、技术选型理由'),
    ('03', '关键技术', '判定引擎 / RAG 检索 / 多智能体报告与校验'),
    ('04', '实现与验证', '数据与工程要点、单测与越权、检索盲评、报告对账'),
    ('05', '评价与展望', '四条创新点 + 五条如实不足'),
]
y = 1.32
for num, h, d in items:
    tf = tb(s, 3.95, y, 0.9, 0.5)
    para(tf, num, size=24, bold=True, color=LBLUE, first=True, after=0, font=HEAVY)
    tf = tb(s, 4.95, y + 0.02, 7.6, 0.9)
    para(tf, h, size=20, bold=True, color=INK, first=True, after=2)
    para(tf, d, size=13, color=SUB, after=0)
    y += 0.95
dots(s, 0, y=6.86)
footer(s, 2)
note(s, '五部分，10–12 分钟；重点在 03 与 05。')

# ============================================================ 03 章节页 1
section_slide(0, 3, '预警往往「期末才知道」，而且说不清依据')
note(s, '过渡：先讲为什么做这件事。')

# ============================================================ 04 问题
s = content_slide(0, '01 背景与目标', '问题：预警系统「算不准」或「说不清」', 4)
bullets(s, [
    ('过程性数据散落，预警总是迟到最后一步', '考勤、作业、测验、项目分散在教师手里，汇总完已错过干预窗口'),
    ('阈值拍脑袋，跨班跨课程不可比', '同一分数在不同老师、不同难度的班里含义不同，直接比较会误判'),
    ('只给结论、不给依据，学生不认、教师不用', '「你被预警了」没有可核对的事实链，容易变成无效提醒'),
    ('接入大模型后又出现新问题：数字编造、结论无法复算', '模型会补出不存在的比例与名次，同一提示词两次输出还不一致'),
], y=2.08, size=18)
note(s, '四条痛点，最后一条点出本课题的新问题。')

# ============================================================ 05 目标与边界
s = content_slide(0, '01 背景与目标', '目标与边界：先把口径做对，再让模型说话', 5)
label_box(s, 0.86, 2.08, 5.6, 1.0, '业务目标', '判定可解释、可复算；预警有人处置、有复核', size=15)
label_box(s, 6.88, 2.08, 5.6, 1.0, '技术目标', '同一份事实既能算出结论，也能被断言校验', size=15, filled=True)
label_box(s, 0.86, 3.26, 5.6, 1.0, 'AI 目标', '模型只写结论与建议，数字全部来自事实包', size=15, filled=True)
label_box(s, 6.88, 3.26, 5.6, 1.0, '明确不做', '不做自动排课与学费管理；不替代教师做最终判定', size=15)
tf = tb(s, 0.86, 4.65, 11.6, 1.9)
para(tf, '贯穿全文的四条设计原则', size=15, bold=True, color=BLUE, first=True, after=8)
for t in ['关键算法自己写：判定口径不能被框架黑盒化',
          '数字由规则算、语言由模型写：职责不重叠',
          '任何 AI 输出必须有可执行断言，否则不发布',
          '非核心能力可降级，但核心链路必须有结果']:
    para(tf, '· ' + t, size=15, color=SUB, after=6, line=1.25)
note(s, '边界要讲：为什么不做自动排课。')

# ============================================================ 06 章节页 2
section_slide(1, 6, '前后端分离 + 单体后端 + 单库，AI 放在最后')
note(s, '过渡：讲整体怎么搭。')

# ============================================================ 07 架构
s = content_slide(1, '02 方案与架构', '总体架构：四层，职责单向依赖', 7)
layers = [
    ('表现层', 'Vue 3 + Vite + Tailwind　｜　学生端 / 教师端 / 辅导员端 / 教务端'),
    ('接口层', 'Spring Boot 4（内嵌 Tomcat）　｜　统一 Result 返回 + 角色注解 + 数据范围校验'),
    ('业务层', '判定引擎　｜　成绩与考勤　｜　预警处置　｜　报告与问答　｜　问卷与通知'),
    ('数据与 AI 层', 'MySQL 8（判定快照 / 业务表）　｜　检索：全文 + 向量　｜　模型：Dify 工作流 / 直连'),
]
y = 2.1
for i, (name, detail) in enumerate(layers):
    filled = (name == '业务层')
    label_box(s, 0.86, y, 2.2, 0.92, name, size=15, filled=filled)
    sh = rect(s, 3.24, y, 9.24, 0.92, fill=WHITE, line_color=LINE, shape=MSO_SHAPE.ROUNDED_RECTANGLE, radius=0.06)
    tf = sh.text_frame
    tf.word_wrap = True; tf.vertical_anchor = MSO_ANCHOR.MIDDLE
    tf.margin_left = Inches(0.2)
    p = tf.paragraphs[0]; r = p.add_run(); r.text = detail
    r.font.size = Pt(13); r.font.color.rgb = SUB; r.font.name = FONT
    y += 1.12
tf = tb(s, 0.86, 6.62, 11.6, 0.4)
para(tf, '选型理由：单人开发优先「单体分层 + 少依赖 + 离线可构建」；大模型最后接入，且不参与数值计算。',
     size=12, color=LBLUE, first=True, after=0)
note(s, '老师常问为什么不上微服务：一人开发、演示规模小，微服务只增加排错面。')

# ============================================================ 08 主线闭环
s = content_slide(1, '02 方案与架构', '一条主线：数据来源 → 风险判定 → 处置闭环', 8)
steps = [('数据来源', '考勤 / 成绩 / 作业 / 测验'), ('风险判定', '三通道 + 分档线 + 置信度'),
         ('预警生成', '去重、升级、解释卡'), ('处置干预', '约谈 / 补交 / 上报'),
         ('效果复核', '等级变化、两周复盘')]
x = 0.86
for i, (h, d) in enumerate(steps):
    filled = i in (1, 2)
    label_box(s, x, 2.12, 2.05, 1.15, h, d, size=14, filled=filled)
    if i < 4:
        right_arrow(s, x + 2.11, 2.58)
    x += 2.35
tf = tb(s, 0.86, 3.62, 11.6, 2.9)
para(tf, '三个关键约定', size=15, bold=True, color=BLUE, first=True, after=8)
for t in ['判定是「快照」：一人一课一条判定记录，可复算、可回溯当时使用的引擎版本',
          '预警与判定分离：判定覆盖最新状态，预警是事件流，状态变化写时间线',
          '处置必须有落点：每次约谈 / 补交 / 上报都留痕，才能评估「干预是否有成效」',
          'AI 只出现在「解释与建议」环节，不进入判定链路']:
    para(tf, '· ' + t, size=15, color=SUB, after=7, line=1.25)
note(s, '闭环是与「只算分」的系统最大的区别。')

# ============================================================ 09 章节页 3
section_slide(2, 9, '判定引擎、RAG 检索、多智能体报告——三件事各管一段')
note(s, '过渡：进入技术核心。')

# ============================================================ 10 判定引擎
s = content_slide(2, '03 关键技术', '关键技术一：可复算的风险判定引擎（v2.1.0）', 10)
tf = tb(s, 0.86, 2.0, 11.6, 0.5)
para(tf, 'riskScore = clamp100( max( clamp100(base × factor), guardFloor ) )', size=19, bold=True, color=BLUE, first=True, after=2, font=SLIM)
tf = tb(s, 0.86, 2.44, 11.6, 0.4)
para(tf, 'base 取 B 通道（班内相对位置）与 C 通道（跨班折算）中更保守的一侧；硬性不合格（缺勤超限）走 guardFloor 下限保护。',
     size=12, color=SUB, first=True, after=0)
data = [['通道', '衡量什么', '作用'],
        ['A 通道', '缺勤 / 迟到 / 请假（硬门槛）', '满足即触发，直接给下限保护'],
        ['B 通道', '班内相对位置（分数 + 分布）', '避免「绝对分数高就安全」的误判'],
        ['C 通道', '跨班可比折算（教师尺度 Δ + 课程因子）', '让不同老师、难度的班可比']]
table(s, data, 0.86, 3.02, 7.0, [1.1, 3.1, 2.8], row_h=0.52, first_bold=True)
data2 = [['等级', '分数区间', '处置动作'],
         ['红', '≥ 75', '立即约谈 + 通知辅导员'],
         ['橙', '60 – 75', '教师关注 + 补交安排'],
         ['黄', '40 – 60', '提示学生自查'],
         ['绿', '< 40', '正常']]
table(s, data2, 8.15, 3.02, 4.33, [0.72, 1.32, 2.29], row_h=0.42, first_bold=True)
tf = tb(s, 0.86, 5.72, 11.6, 1.0)
para(tf, '口径校验：C1 消融显示课程类型因子影响最大（3.3%），教师风格 1.5%、个人先验 0.2%；C2 阈值扫描显示红档在 70–80 不敏感，'
         '橙 / 黄档更敏感（橙 55→65 使橙档人数 30→1）。', size=12, color=SUB, first=True, after=0, line=1.35)
note(s, '公式与三通道讲清楚；消融数字回答「这些因子到底有没有用」。历史挂科率因子在演示数据里是惰性的（0.0%），如实说明。')

# ============================================================ 11 RAG
s = content_slide(2, '03 关键技术', '关键技术二：事实驱动的 RAG 检索与拒答', 11)
steps = [('查询构造', '课程 + 最弱题型 / 分组'), ('双路召回', '全文 ngram + 向量 bge-m3'),
         ('RRF 融合', 'k = 60，去重后截断'), ('闸门过滤', '低分 / 低支撑即拒答')]
x = 0.86
for i, (h, d) in enumerate(steps):
    label_box(s, x, 2.06, 2.75, 1.0, h, d, size=13, filled=(i == 3))
    if i < 3:
        right_arrow(s, x + 2.81, 2.43)
    x += 3.02
data = [['配置', 'recall@4', 'MRR', 'nDCG@4', '不可答拒答率', '可答误拒率'],
        ['仅全文（MySQL ngram）', '0.8889', '0.8164', '0.8349', '0.90', '9.3%'],
        ['仅向量（bge-m3）', '1.0000', '0.9414', '0.9560', '0.10', '0%'],
        ['全文 + 向量（线上配置）', '1.0000', '0.9491', '0.9621', '0.90', '9.3%']]
table(s, data, 0.86, 3.3, 11.6, [3.6, 1.5, 1.4, 1.5, 2.1, 1.5], row_h=0.44, first_bold=True)
tf = tb(s, 0.86, 5.3, 11.6, 1.5)
para(tf, '评测：64 例盲评（54 可答 + 10 故意不可答），同一批问题跑三组配置。', size=13, color=INK, first=True, after=5)
para(tf, '为什么仍选混合检索：课堂问答里「该答的漏掉」比「多问一句」更伤体验；闸门参数（最低分 0.10、向量分 0.35、词法支撑 0.25）'
         '写进配置而非硬编码，可按课程调。检索片段只作「引用素材」，不与系统指令同权。', size=12, color=SUB, after=0, line=1.35)
note(s, '诚实解读：混合排序最好，但拒答 0.90、误拒 9.3%，这是一对矛盾体，选择偏向前者。')

# ============================================================ 12 报告体系
s = content_slide(2, '03 关键技术', '关键技术三：多智能体报告与三道校验', 12)
chain = [('Dify 工作流', '已编排时优先'), ('直连模型', '未配置 / 失败时'), ('规则模板', '兜底，必有结果')]
x = 0.86
for i, (h, d) in enumerate(chain):
    label_box(s, x, 2.06, 3.5, 0.95, h, d, size=15, filled=(i == 0))
    if i < 2:
        right_arrow(s, x + 3.56, 2.41, 0.5, 0.28)
    x += 4.06
tf = tb(s, 0.86, 3.12, 11.6, 0.36)
para(tf, '单向降级：Dify → 直连 → 模板，不反向升级；来源字段随报告落库，可统计降级率。', size=12, color=LBLUE, first=True, after=0)
data = [['环节', '做什么', '失败后的表现'],
        ['① 事实包', '把判定、成绩、考勤、作答整理成结构化事实', '取数失败 → 不出报告并报错'],
        ['② 生成', '按固定四段写结论与建议', '输出不合格 → 换通道'],
        ['③ 结构校验', '段落标题必须是枚举内的四段', '不合格 → 降级模板'],
        ['④ 数字校验', '正文数字必须在事实包白名单内', '越界 → 拒绝发布'],
        ['⑤ 定向对账', '红 / 黄 / 绿灯人数、平均分、及格线等 14 条规则逐项比对', '不一致 → 报告标注并留痕']]
table(s, data, 0.86, 3.55, 11.6, [1.8, 6.2, 3.6], row_h=0.44, first_bold=True)
tf = tb(s, 0.86, 6.3, 11.6, 0.5)
para(tf, '实测：三类群体报告（教学班 / 行政班 / 课程）对账 5/5、6/6、2/2 全部一致；生成耗时 3.8–14 秒。', size=13, color=INK, first=True, after=0)
note(s, '核心论点：提示词是「请求」，校验才是「验收」；只有把数学性质变成可执行断言，才能回答「模型编了数怎么办」。')

# ============================================================ 13 三种口径
s = content_slide(2, '03 关键技术', '三种群体口径：数字能互相加总', 13)
data = [['口径', '统计范围', '回答什么问题', '独有内容'],
        ['教学班报告', '一门课 × 一个班', '这个班接下来怎么教', '本班红黄灯名单与原因'],
        ['行政班报告', '一个班 × 多门课', '哪些课集中出问题', '集中风险课程、多门预警学生'],
        ['课程报告', '一门课 × 多个班', '三个班谁最吃力、共性短板是什么', '班与班横向对比']]
table(s, data, 0.86, 2.06, 11.6, [2.0, 2.5, 4.0, 3.1], row_h=0.6, first_bold=True)
tf = tb(s, 0.86, 4.5, 11.6, 2.2)
para(tf, '可加总性的证据（课程报告 vs 各班班级报告）', size=15, bold=True, color=BLUE, first=True, after=8)
for t in ['人数：45 × 3 = 135；红灯 1 + 2 + 1 = 4；平均 81.1 是按人数加权的结果',
          '课程报告不重新查底表，而是聚合各班事实包，因此两份报告能逐项对上账',
          '对账允许「分班数字」与「课程总数」并存：报告写「02 班红 2 人」与总数红 4 不冲突',
          '已知边界：同一门课只教 1 个班时，课程报告会退化为班级报告，界面提示按班级报告看']:
    para(tf, '· ' + t, size=13, color=SUB, after=6, line=1.3)
note(s, '这条最能体现「不是三个模板换个皮」，而是同一事实集的不同聚合口径。')

# ============================================================ 14 章节页 4
section_slide(3, 14, '数据怎么建、怎么测、怎么证明它对')
note(s, '过渡：实现与验证。')

# ============================================================ 15 数据与工程
s = content_slide(3, '04 实现与验证', '数据与工程要点', 15)
bullets(s, [
    ('库表：60 张表，其中判定子系统 13 张核心表', '按「结果 / 事件 / 留痕」分层：判定快照、预警事件、时间线、干预记录、通知与回执、班集体提醒'),
    ('不用物理外键，靠逻辑外键 + 唯一约束', '便于演示库重建；关键唯一约束防重复预警与重复通知'),
    ('索引按页面查询建：14 组联合索引', '判定按「班 / 等级 / 状态」过滤，报告按「目标 + 作用域」取最新'),
    ('权限四端分离 + 服务端范围校验', '学生只看自己、教师只看任教班级、辅导员只看所带班级；越权实测返回 400'),
    ('AI 调用全留痕：ai_run / ai_run_step', '记录通道、token、耗时、校验结论，可统计降级率与对账通过率'),
], y=2.1, size=17, gap=10, sub_size=13)
note(s, '工程部分点到为止，强调权限与留痕是设计出来的。')

# ============================================================ 16 验证
s = content_slide(3, '04 实现与验证', '验证：能算、能测、能对账', 16)
data = [['验证项', '方法', '结果'],
        ['风险分与等级', 'JUnit 单测（手算期望值写断言）', '20 / 20 通过'],
        ['预警触发闸门', 'JUnit 单测（幂等与升级）', '5 / 5 通过'],
        ['接口与越权', '脚本化 HTTP 用例', '7 / 7 通过（越权均 400）'],
        ['RAG 检索质量', '64 例盲评，三组配置对比', 'recall@4 = 1.0（混合），拒答 0.90'],
        ['报告可信性', '三类报告实跑 + 数字对账', '对账全部一致；植入错误可被抓出（7/8）'],
        ['端到端', '9 节 15 页点击验收手册', '全部通过']]
table(s, data, 0.86, 2.06, 11.6, [2.6, 4.7, 4.3], row_h=0.5, first_bold=True)
tf = tb(s, 0.86, 5.35, 11.6, 1.4)
para(tf, '缺陷与修复：开发全程 5 条，4 条已修，1 条遗留', size=14, bold=True, color=BLUE, first=True, after=6)
for t in ['致命：提示词里的裸百分号让三类报告同时 500 —— 由验收脚本首轮发现',
          '严重：行政班报告「共 0 人」—— 学籍状态取值错误，人工核对后修正为 45 人',
          '严重：模型输出被截断成 82 字残句后悄悄退回模板 —— 加「截断且过短则双倍预算重试」',
          '遗留：出题闸门单测依赖 Mockito，离线仓库缺依赖，未纳入回归']:
    para(tf, '· ' + t, size=12, color=SUB, after=4, line=1.25)
note(s, '缺陷表比「全部通过」更可信，主动讲出来能加分。')

# ============================================================ 17 章节页 5
section_slide(4, 17, '四条创新点，五条不足，都说清楚')
note(s, '过渡：进入评价。')

# ============================================================ 18 亮点
s = content_slide(4, '05 评价与展望', '亮点与创新点', 18)
items = [
    ('① 数字与叙述彻底分离，且可对账',
     '数字全部来自事实包，模型只写结论；定稿前用 14 条规则定向比对人数 / 分数 / 及格线 / 预警条数，不一致会在报告里显式标注'),
    ('② 检索 query 由学情事实驱动',
     '不用固定串，而是用「课程 + 最弱题型 / 分组」构造查询，检索不足再回退通用串；依据段附原文摘句，使依据与结论对得上'),
    ('③ 单向三层降级 + 来源留痕',
     'Dify → 直连 → 模板，任一层失败都能出报告；来源字段随报告落库，降级率可统计、可追责'),
    ('④ 三种群体口径可加总、可复算',
     '三份报告由同一套事实聚合而来，人数与分类计数能互相对上账；判定用快照 + 引擎版本，任何时候都能复算'),
]
tf = tb(s, 0.86, 2.05, 11.6, 4.6)
first = True
for h, d in items:
    para(tf, h, size=18, bold=True, color=BLUE, first=first, after=3, line=1.2)
    first = False
    para(tf, d, size=13, color=SUB, after=16, line=1.35)
note(s, '本场答辩核心页，四条都要能举出代码或数据证据。')

# ============================================================ 19 不足
s = content_slide(4, '05 评价与展望', '不足与改进方向（如实说明）', 19)
data = [['不足', '现状', '改进'],
        ['未做压测', '只有单请求耗时（报告生成 3.8–14 秒）', '补 50 并发查判定、10 并发生成报告'],
        ['未接 CI', '构建与回归靠本地脚本', '接入流水线：编译 + 单测 + 报告对账门禁'],
        ['知识库覆盖有限', '5 门课 29 篇资料，依据偏「导学段」', '补评分标准、常见失分点等文档'],
        ['姓名进提示词', '报告可读性需要，演示为合成数据', '真实部署前改为脱敏编号'],
        ['并发防护缺失', '重复点生成会产生多份历史报告', '对同一目标加互斥或幂等键']]
table(s, data, 0.86, 2.06, 11.6, [2.7, 5.0, 3.9], row_h=0.62, first_bold=True)
tf = tb(s, 0.86, 5.7, 11.6, 1.2)
para(tf, '后续可做：把「数字对账」扩展为「每个结论都有出处」的完整可信性评估；三种口径接入同一张看板；在真实院系小范围试用并收集教师反馈。',
     size=13, color=SUB, first=True, after=0, line=1.35)
note(s, '「不足」不是扣分项，回避才是；每条都给改进动作。')

# ============================================================ 20 结束
s = ns()
rect(s, 0, 0, SW, SH, fill=BLUE)
sh = s.shapes.add_shape(MSO_SHAPE.OVAL, Inches(10.1), Inches(5.2), Inches(4.4), Inches(4.4))
sh.fill.solid(); sh.fill.fore_color.rgb = LBLUE; sh.fill.transparency = 0.28
sh.line.fill.background(); sh.shadow.inherit = False

tf = tb(s, 1.05, 2.0, 11.0, 0.4)
para(tf, 'Q & A', size=14, color=RGBColor(0xCF, 0xE3, 0xF3), first=True, after=6, font=SLIM)
tf = tb(s, 1.05, 2.45, 11.2, 1.1)
para(tf, '汇报结束，请老师批评指正', size=40, bold=True, color=WHITE, first=True, after=0)
rect(s, 1.05, 3.72, 3.2, 0.014, fill=RGBColor(0x9E, 0xC8, 0xE4))
tf = tb(s, 1.05, 4.0, 10.6, 1.6)
para(tf, '一句话总结：大模型进入业务系统的前提，是先把数值口径做成机器可校验的东西；'
         '凡是无法断言的输出，都不该被当作结论发布。', size=16, color=RGBColor(0xE6, 0xF2, 0xFA), first=True, after=14, line=1.4)
para(tf, '演示入口：教师端 → 全部课程 → 课程总报告；辅导员端 → 班级管理 → 班级报告。',
     size=12, color=RGBColor(0xC9, 0xE1, 0xF2), after=0, line=1.3)
note(s, '结束语控制在一分钟内，随时准备打开系统演示报告与对账。')

prs.core_properties.title = '基于 RAG 与多智能体的学情预警与个性化学习管理系统的设计与实现 · 答辩 PPT'
prs.core_properties.author = '狄珂'
prs.save(OUT)
print('WROTE ' + OUT)
print('slides=' + str(len(prs.slides)))
