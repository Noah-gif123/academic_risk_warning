# -*- coding: utf-8 -*-
"""把总结稿 Markdown 转换为排版好的 Word 文档（最终版 v6）。

源文件约定：
  1. 首行 = 主标题；紧随的“（…）”行 = 副标题
  2. 章标题 = 顶格“@N、章名”（渲染为“一、二、三…”）
  3. 节标题 = 顶格“（一）（二）…”
  4. 列表项 = 缩进块；块内首行渲染为悬挂缩进小点，块内同级行另起一项
"""
import re
import sys

from docx import Document
from docx.enum.text import WD_ALIGN_PARAGRAPH
from docx.oxml.ns import qn
from docx.shared import Pt, RGBColor, Cm

SRC, DST = sys.argv[1], sys.argv[2]

doc = Document()
normal = doc.styles["Normal"]
normal.font.name = "宋体"
normal.font.size = Pt(12)
normal._element.rPr.rFonts.set(qn("w:eastAsia"), "宋体")
normal.paragraph_format.line_spacing = 1.5
normal.paragraph_format.space_after = Pt(0)

for s in doc.sections:
    s.top_margin = Cm(2.54)
    s.bottom_margin = Cm(2.54)
    s.left_margin = Cm(3.17)
    s.right_margin = Cm(3.17)

CN = "一二三四五六七八九十"
RE_MARK = re.compile(r"^@(\d+)、\s*(.+)$")
RE_SECTION = re.compile(r"^（[" + CN + r"]+）")
RE_RULE = re.compile(r"[─\-=]{4,}")


def cn_quotes(text):
    out, open_q = [], True
    for ch in text:
        if ch == '"':
            out.append("\u201c" if open_q else "\u201d")
            open_q = not open_q
        else:
            out.append(ch)
    return "".join(out)


def set_font(run, name="宋体", size=12, bold=False, color=None):
    run.font.name = name
    run.font.size = Pt(size)
    run.font.bold = bold
    run._element.rPr.rFonts.set(qn("w:eastAsia"), name)
    if color:
        run.font.color.rgb = RGBColor(*color)


def add_body(text, indent=True, size=12, hanging=False):
    p = doc.add_paragraph()
    pf = p.paragraph_format
    pf.line_spacing = 1.5
    if hanging:
        pf.left_indent = Pt(24)
        pf.first_line_indent = Pt(-12)
    elif indent:
        pf.first_line_indent = Pt(size * 2)
    set_font(p.add_run(cn_quotes(text)), size=size)
    return p


def add_heading(text, level):
    sizes = {1: 18, 2: 15, 3: 13}
    p = doc.add_paragraph(style=f"Heading {level}")
    pf = p.paragraph_format
    pf.space_before = Pt(18 if level <= 2 else 12)
    pf.space_after = Pt(10 if level <= 2 else 6)
    pf.line_spacing = 1.4
    if level == 1:
        p.alignment = WD_ALIGN_PARAGRAPH.CENTER
    set_font(p.add_run(cn_quotes(text)), name="黑体", size=sizes.get(level, 12),
             bold=True, color=(0x1F, 0x37, 0x64) if level <= 2 else (0x2E, 0x2E, 0x2E))
    return p


def cn_number(num):
    if num <= 10:
        return CN[num - 1]
    if num < 20:
        return "十" + CN[num - 11]
    return CN[num // 10 - 1] + "十" + (CN[num % 10 - 1] if num % 10 else "")


with open(SRC, encoding="utf-8") as f:
    lines = f.read().splitlines()
n = len(lines)


def ind(idx):
    return len(lines[idx]) - len(lines[idx].lstrip())


# ---------- 封面 ----------
i = 0
while i < n and not lines[i].strip():
    i += 1
add_heading(lines[i].strip(), 1)
i += 1
if i < n and lines[i].strip().startswith("（"):
    p = doc.add_paragraph()
    p.alignment = WD_ALIGN_PARAGRAPH.CENTER
    p.paragraph_format.space_after = Pt(16)
    set_font(p.add_run(lines[i].strip()), name="黑体", size=13, color=(0x40, 0x40, 0x40))
    i += 1

body = items = 0

while i < n:
    s = lines[i].strip()

    if not s or RE_RULE.search(s):
        i += 1
        continue

    start_indent = ind(i)          # 单独保存，勿被合并循环覆盖

    m = RE_MARK.match(s)
    if start_indent == 0 and m:
        add_heading("%s、%s" % (cn_number(int(m.group(1))), m.group(2).strip()), 2)
        i += 1
        continue

    if start_indent == 0 and RE_SECTION.match(s):
        add_heading(s, 3)
        i += 1
        continue

    is_item = start_indent > 0
    item_indent = start_indent if is_item else None

    # 合并跨行：新章 / 新节 / 同级列表项都另起一段
    buf = [s]
    i += 1
    while i < n:
        ns = lines[i].strip()
        if not ns or RE_RULE.search(ns):
            break
        ni = ind(i)
        if ni == 0 and (RE_MARK.match(ns) or RE_SECTION.match(ns)):
            break
        if item_indent is not None and ni <= item_indent and ni > 0:
            break                      # 同级或更浅的缩进行 -> 新的列表项
        buf.append(ns)
        i += 1

    text = ""
    for seg in buf:
        if text and re.search(r"[A-Za-z0-9)\]]$", text) and re.match(r"^[A-Za-z0-9(\[]", seg):
            text += " " + seg
        else:
            text += seg
    text = re.sub(r"\s{2,}", " ", text).strip()

    if is_item:
        add_body(text, hanging=True)
        items += 1
    else:
        add_body(text)
        body += 1

doc.save(DST)
print(f"OK -> {DST} (正文段 {body} / 列表项 {items})")
