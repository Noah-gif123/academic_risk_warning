# -*- coding: utf-8 -*-
"""根据库里的演示卷数据生成《AI 批改演示卷（含学生作答与批改结果）》
数据来源：v2.0/tool/out_grading/{summary,questions,suggestions}.tsv（由 Q.java 导出）
用法：py gen_grading_demo_doc.py
"""
import csv, os, sys
from docx import Document
from docx.shared import Pt, Cm, RGBColor
from docx.enum.text import WD_ALIGN_PARAGRAPH
from docx.enum.table import WD_TABLE_ALIGNMENT
from docx.oxml.ns import qn
from docx.oxml import OxmlElement

ROOT = r'E:\JavaDEVIDEA\workspace\JavaProject\academic_risk_warning\v2.0'
DATA = os.path.join(ROOT, 'tool', 'out_grading')
OUT = os.path.join(ROOT, 'word', 'AI批改演示卷（含学生作答与批改结果）.docx')

BLUE = RGBColor(0x01, 0x71, 0xBB); INK = RGBColor(0x22, 0x2B, 0x38)
SUB = RGBColor(0x55, 0x60, 0x70); MUTE = RGBColor(0x8A, 0x96, 0xA3)
CJK = '宋体'; CJK_B = '微软雅黑'


def read_tsv(name):
    with open(os.path.join(DATA, name), encoding='utf-8-sig') as f:
        rows = list(csv.DictReader(f, delimiter='\t'))
    return rows


def clean(v):
    return '' if v is None else str(v).strip()


QTYPE = {'SINGLE': '单选题', 'MULTI': '多选题', 'JUDGE': '判断题', 'BLANK': '填空题', 'SHORT': '简答题'}
summary = read_tsv('summary.tsv')[0]
questions = read_tsv('questions.tsv')
sugs = {int(r['q_no']): r for r in read_tsv('suggestions.tsv')}

doc = Document()
sec = doc.sections[0]
sec.page_height, sec.page_width = Cm(29.7), Cm(21.0)
sec.top_margin = sec.bottom_margin = Cm(2.0)
sec.left_margin = sec.right_margin = Cm(2.0)
normal = doc.styles['Normal']
normal.font.name = CJK
normal.font.size = Pt(10.5)
normal.element.rPr.rFonts.set(qn('w:eastAsia'), CJK)
normal.paragraph_format.line_spacing = 1.4
normal.paragraph_format.space_after = Pt(3)


def set_font(run, size=10.5, bold=False, color=INK, font=CJK):
    run.font.size = Pt(size); run.font.bold = bold; run.font.color.rgb = color; run.font.name = font
    run._element.rPr.rFonts.set(qn('w:eastAsia'), font)


def p(text='', size=10.5, bold=False, color=INK, font=CJK, align=None, before=0, after=3, line=1.4, indent=0):
    par = doc.add_paragraph()
    if align is not None:
        par.alignment = align
    pf = par.paragraph_format
    pf.space_before = Pt(before); pf.space_after = Pt(after); pf.line_spacing = line
    if indent:
        pf.left_indent = Pt(indent)
    if text:
        parts = str(text).split('**')
        for i, seg in enumerate(parts):
            if seg == '':
                continue
            set_font(par.add_run(seg), size, bold or (i % 2 == 1), color, font)
    return par


def h1(text, num=None):
    par = doc.add_paragraph()
    par.paragraph_format.space_before = Pt(14); par.paragraph_format.space_after = Pt(6)
    if num:
        set_font(par.add_run(num + '  '), 14, True, BLUE, CJK_B)
    set_font(par.add_run(text), 14, True, INK, CJK_B)
    pPr = par._p.get_or_add_pPr()
    bdr = OxmlElement('w:pBdr'); bottom = OxmlElement('w:bottom')
    bottom.set(qn('w:val'), 'single'); bottom.set(qn('w:sz'), '8'); bottom.set(qn('w:space'), '4'); bottom.set(qn('w:color'), '0171BB')
    bdr.append(bottom); pPr.append(bdr)


def shade(cell, color):
    tcPr = cell._tc.get_or_add_tcPr()
    shd = OxmlElement('w:shd'); shd.set(qn('w:val'), 'clear'); shd.set(qn('w:color'), 'auto'); shd.set(qn('w:fill'), color)
    tcPr.append(shd)


def table(rows, widths, head=True, size=9.5):
    t = doc.add_table(rows=len(rows), cols=len(rows[0]))
    t.style = 'Table Grid'; t.alignment = WD_TABLE_ALIGNMENT.CENTER; t.autofit = False
    for ri, row in enumerate(rows):
        for ci, val in enumerate(row):
            cell = t.cell(ri, ci); cell.width = Cm(widths[ci]); cell.text = ''
            par = cell.paragraphs[0]
            par.paragraph_format.space_before = Pt(2); par.paragraph_format.space_after = Pt(2)
            par.paragraph_format.line_spacing = 1.25
            is_head = head and ri == 0
            set_font(par.add_run(str(val)), size, is_head, RGBColor(0xFF, 0xFF, 0xFF) if is_head else INK, CJK_B if is_head else CJK)
            if is_head:
                par.alignment = WD_ALIGN_PARAGRAPH.CENTER; shade(cell, '0171BB')
            elif ri % 2 == 0:
                shade(cell, 'F5F9FC')
    for ri, row in enumerate(t.rows):
        trPr = row._tr.get_or_add_trPr()
        trPr.append(OxmlElement('w:cantSplit'))
        if head and ri == 0:
            trPr.append(OxmlElement('w:tblHeader'))
    for ci, w in enumerate(widths):
        for cell in t.columns[ci].cells:
            cell.width = Cm(w)
    return t


def quote(lines, color='F7FAFD'):
    for i, line in enumerate(lines):
        par = doc.add_paragraph()
        pf = par.paragraph_format
        pf.left_indent = Pt(10); pf.space_before = Pt(2 if i == 0 else 0); pf.space_after = Pt(2); pf.line_spacing = 1.45
        pPr = par._p.get_or_add_pPr()
        shd = OxmlElement('w:shd'); shd.set(qn('w:val'), 'clear'); shd.set(qn('w:color'), 'auto'); shd.set(qn('w:fill'), color)
        pPr.append(shd)
        bdr = OxmlElement('w:pBdr'); left = OxmlElement('w:left')
        left.set(qn('w:val'), 'single'); left.set(qn('w:sz'), '16'); left.set(qn('w:space'), '6'); left.set(qn('w:color'), '0171BB')
        bdr.append(left); pPr.append(bdr)
        set_font(par.add_run(line), 10, False, INK, CJK)


def footer_page():
    par = sec.footer.paragraphs[0]
    par.alignment = WD_ALIGN_PARAGRAPH.CENTER
    set_font(par.add_run('AI 批改演示卷 ｜ 教师 1001 · 学生 20250001　　'), 8, False, MUTE)
    run = par.add_run()
    fld = OxmlElement('w:fldSimple'); fld.set(qn('w:instr'), 'PAGE')
    run._element.addnext(fld)
    set_font(run, 8, False, MUTE)


import json

footer_page()
# ---------- 标题 ----------
p('AI 批改演示卷', 22, True, INK, CJK_B, WD_ALIGN_PARAGRAPH.LEFT)
p('学生作答 → 客观题自动判分 → 主观题 AI 建议分 → 教师定稿', 11, False, BLUE, CJK_B)
p('', after=6)
table([
    ['课程 / 教学班', clean(summary['class_name'])],
    ['试卷名称', clean(summary['paper_name'])],
    ['任课教师', clean(summary['teacher_name']) + '（账号 1001）'],
    ['学生', clean(summary['student_name']) + '（学号 ' + clean(summary['student_no']) + '）'],
    ['卷面结构', '6 题 / 100 分：客观题 3 题（单选 2、多选 1）30 分；主观题 3 题（简答）70 分'],
    ['本次结果', '客观 ' + clean(summary['objective_score']) + ' 分 ＋ 主观 ' + clean(summary['subjective_score']) +
     ' 分 ＝ 总分 ' + clean(summary['total_score']) + ' 分（状态 ' + clean(summary['submit_status']) + '）'],
], [3.6, 13.0], head=False, size=10)

# ---------- 卷面与作答 ----------
h1('卷面与学生作答', '一')
for q in questions:
    qno = int(q['q_no']); qtype = clean(q['q_type']); full = float(clean(q['full_score']))
    teacher_score = clean(q['stu_score'])
    ans = clean(q['stu_answer'])
    try:
        ans_obj = json.loads(ans) if ans else {}
    except Exception:
        ans_obj = {}
    p(f'第 {qno} 题（{QTYPE.get(qtype, qtype)}，{full:.0f} 分）', 11, True, BLUE, CJK_B, before=8, after=2)
    p(clean(q['stem']), 10.5, False, INK)
    if qtype in ('SINGLE', 'MULTI'):
        opts = clean(q['options_json'])
        try:
            for o in json.loads(opts):
                p(f"　{o['key']}. {o['text']}", 10, False, SUB)
        except Exception:
            p('　' + opts, 10, False, SUB)
        ref = clean(q['answer_json'])
        try:
            keys = json.loads(ref).get('keys', [])
            p('　参考答案：' + '、'.join(keys), 10, False, SUB)
        except Exception:
            p('　参考答案：' + ref, 10, False, SUB)
    stu_txt = ans_obj.get('keys') and ('、'.join(ans_obj['keys'])) or ans_obj.get('text') or '（未作答）'
    p('　学生作答：' + str(stu_txt), 10, False, INK)
    p('　客观自动判分：' + (teacher_score if qtype != 'SHORT' else '（主观题，不自动判分）') +
      (' 分' if qtype != 'SHORT' and teacher_score else ''), 10, False, SUB)

# ---------- 主观题批改 ----------
h1('主观题：AI 建议分与教师定稿', '二')
rows = [['题号', '满分', 'AI 建议', '教师定稿', '是否采纳建议', 'AI 扣分依据']]
for qno in (4, 5, 6):
    s = sugs.get(qno)
    if not s:
        continue
    rows.append([
        '第 %d 题' % qno, '%.0f' % float(clean(s['full_score'])), '%.0f' % float(clean(s['suggested_score'])),
        '%.0f' % float(clean(s['final_score'])) if clean(s['final_score']) else '（本演示按教师分定稿）',
        '是' if clean(s['status']) == 'APPLIED' else '否',
        clean(s['rubric']),
    ])
table(rows, [1.6, 1.4, 1.7, 2.0, 2.4, 7.5], size=9)
p('', after=2)
p('AI 评语（原文，含维度逐条扣分）', 11, True, BLUE, CJK_B, before=6, after=2)
for qno in (4, 5, 6):
    s = sugs.get(qno)
    if not s:
        continue
    quote(['第 %d 题（AI 建议 %.0f / %.0f 分）：%s' % (qno, float(clean(s['suggested_score'])),
                                                     float(clean(s['full_score'])), clean(s['comment']))])

# ---------- 结论 ----------
h1('这次演示说明了什么', '三')
p('① **分工是清楚的**：客观题由系统按标准答案自动判分（本次 10/30，错的第 2 题与少选的第 3 题都没有得分）；'
  '主观题由 AI 给出「建议分 + 逐维度扣分评语」，教师确认后才写分——AI 不直接改成绩。')
p('② **建议分与人工分接近**：三题 AI 分别为 25 / 13 / 6，教师定稿 22 / 12 / 5，差值 +3 / +1 / +1，'
  '平均绝对误差 1.7 分（满分 70）；排序与作答质量一致（完整 > 部分 > 敷衍）。')
p('③ **偏差方向可解释**：AI 对完整作答略偏松（给满分，教师扣了 3 分），对敷衍作答同样压到很低——'
  '这与「AI 建议 + 教师定稿」的定位吻合，适合做预填，不适合直接发布。')
p('④ **效率**：整卷 3 道主观题一次调用完成，耗时约 4 秒、1461 tokens；三道闸门（分数范围 / 评语不编造数字 / 参考要点存在）全部通过。')

h1('怎么在界面上复现', '四')
table([
    ['步骤', '操作', '应看到'],
    ['1', '学生 20250001 登录 → 「我的考试」→ 找到「AI 批改演示卷 · 计算机基础综合」→ 作答并提交',
     '提交成功，客观题立刻出分（单选/多选自动判）'],
    ['2', '教师 1001 登录 → 「交卷与批改」→ 选中该卷 → 点 20250001 那份「批改」', '右侧抽屉打开展示 6 道题与作答'],
    ['3', '抽屉右上角点「AI 批改建议」', '3 道主观题下方出现「AI 建议 X / Y 分」+ 评语 + 给分依据'],
    ['4', '点某题的「填入评分框」，按需要微调后点「提交批改」', '卷面状态变「已批改」，总分 = 客观 + 主观'],
], [1.3, 9.6, 5.7], size=9)
p('', after=2)
p('说明：演示卷与本次提交通道脚本 `v2.0/tool/demo_grading_full.ps1` 可反复重置并重跑（会清掉旧提交与 AI 建议）；'
  '卷面数据由 `v2.0/sql/42_seed_grading_demo_paper.sql` 建立。', 9.5, False, MUTE)

doc.save(OUT)
print('WROTE ' + OUT)
print('paragraphs=%d tables=%d' % (len(doc.paragraphs), len(doc.tables)))
