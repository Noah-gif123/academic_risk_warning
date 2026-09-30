# -*- coding: utf-8 -*-
"""把汇报稿 Markdown 转换为排版好的 Word 文档。

文档约定：
  1. 首行（之前可能有空行）= 主标题；紧随的“（…）”行 = 副标题
  2. 章标题 = 顶格独占一行的“一、二、三…”
  3. 节标题 = 顶格独占一行的“（一）（二）…”
  4. 正文列表 = 缩进的“一、二、三…”，渲染为悬挂缩进小点

消歧关键：章标题与列表项都以“一、”开头，唯一区别是顶格 / 缩进。
为稳妥，另加前瞻校验：章标题的下一非空行必须是“（一）”节标题、分隔线或空行。
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

for section in doc.sections:
    section.top_margin = Cm(2.54)
    section.bottom_margin = Cm(2.54)
    section.left_margin = Cm(3.17)
    section.right_margin = Cm(3.17)

CN = "一二三四五六七八九十"
RE_CHAPTER = re.compile(r"^[" + CN + r"]+、")
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


with open(SRC, encoding="utf-8") as f:
    lines = f.read().splitlines()
n = len(lines)


def indent_of(idx):
    return len(lines[idx]) - len(lines[idx].lstrip())


def next_nonempty(idx):
    j = idx + 1
    while j < n and not lines[j].strip():
        j += 1
    return j if j < n else None


def starts_paragraph(j):
    """判断 j 行是否开启一个新段落块（顶格且是章/节标题，或分隔线）。"""
    if j is None:
        return True
    s = lines[j].strip()
    if RE_RULE.search(s):
        return True
    if indent_of(j) != 0:
        return False
    return bool(RE_CHAPTER.match(s) or RE_SECTION.match(s))


# ---------------- 封面 ----------------
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
    raw = lines[i]
    s = raw.strip()

    if not s or RE_RULE.search(s):
        i += 1
        continue

    lead = indent_of(i)

    # 章标题：顶格 + “一、” + 下一段块不是缩进列表项
    if lead == 0 and RE_CHAPTER.match(s):
        nj = next_nonempty(i)
        nxt_lead = indent_of(nj) if nj is not None else 0
        if nj is None or RE_SECTION.match(lines[nj].strip()) or nxt_lead == 0:
            add_heading(s, 2)
            i += 1
            continue

    if lead == 0 and RE_SECTION.match(s):
        add_heading(s, 3)
        i += 1
        continue

    m = re.match(r"^(#{1,4})\s+(.*)$", s)
    if m:
        add_heading(m.group(2).strip(), len(m.group(1)))
        i += 1
        continue

    # 合并跨行段落（缩进列表项作为独立块，不与后续正文粘连）
    buf = [s]
    i += 1
    while i < n:
        ns = lines[i].strip()
        if not ns or RE_RULE.search(ns):
            break
        ni = indent_of(i)
        if ni == 0 and (RE_CHAPTER.match(ns) or RE_SECTION.match(ns)):
            break
        buf.append(ns)
        i += 1

    text = ""
    for seg in buf:
        if text and re.search(r"[A-Za-z0-9)\]]$", text) and re.match(r"^[A-Za-z0-9(\[]", seg):
            text += " " + seg
        else:
            text += seg
    text = re.sub(r"\s{2,}", " ", text).strip()

    if 0 < lead <= 2 and RE_CHAPTER.match(text) and len(text) < 70:
        add_body(text, hanging=True)
        items += 1
    else:
        add_body(text)
        body += 1

doc.save(DST)
print(f"OK -> {DST} (正文段 {body} / 列表项 {items})")
