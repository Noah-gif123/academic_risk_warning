# -*- coding: utf-8 -*-
"""把汇报稿 Markdown 转换为排版好的 Word 文档（无代码、纯文字内容）。"""
import re
import sys

from docx import Document
from docx.enum.text import WD_ALIGN_PARAGRAPH
from docx.oxml.ns import qn
from docx.shared import Pt, RGBColor, Cm

SRC = sys.argv[1]
DST = sys.argv[2]

doc = Document()

# ---------- 全局字体：正文 ----------
normal = doc.styles["Normal"]
normal.font.name = "宋体"
normal.font.size = Pt(12)
normal._element.rPr.rFonts.set(qn("w:eastAsia"), "宋体")
normal.paragraph_format.line_spacing = 1.5
normal.paragraph_format.space_after = Pt(0)

# ---------- 页面设置 ----------
for section in doc.sections:
    section.top_margin = Cm(2.54)
    section.bottom_margin = Cm(2.54)
    section.left_margin = Cm(3.17)
    section.right_margin = Cm(3.17)


def set_font(run, name="宋体", size=12, bold=False, color=None):
    run.font.name = name
    run.font.size = Pt(size)
    run.font.bold = bold
    run._element.rPr.rFonts.set(qn("w:eastAsia"), name)
    if color:
        run.font.color.rgb = RGBColor(*color)


def cn_quotes(text):
    """把直引号成对替换为中文引号。"""
    out = []
    open_q = True
    for ch in text:
        if ch == '"':
            out.append("\u201c" if open_q else "\u201d")
            open_q = not open_q
        else:
            out.append(ch)
    return "".join(out)


def add_body(text, indent=True, bold=False, size=12, space_before=0, space_after=0):
    p = doc.add_paragraph()
    pf = p.paragraph_format
    pf.line_spacing = 1.5
    pf.space_before = Pt(space_before)
    pf.space_after = Pt(space_after)
    if indent:
        pf.first_line_indent = Pt(size * 2)
    run = p.add_run(cn_quotes(text))
    set_font(run, size=size, bold=bold)
    return p


def add_heading(text, level):
    """套用 Word 内置标题样式（可用导航窗格），并覆盖中文字体与颜色。"""
    sizes = {1: 18, 2: 15, 3: 13}
    p = doc.add_paragraph(style=f"Heading {level}")
    pf = p.paragraph_format
    pf.space_before = Pt(18 if level <= 2 else 12)
    pf.space_after = Pt(10 if level <= 2 else 6)
    pf.line_spacing = 1.4
    if level == 1:
        p.alignment = WD_ALIGN_PARAGRAPH.CENTER
    run = p.add_run(cn_quotes(text))
    set_font(run, name="黑体", size=sizes.get(level, 12), bold=True,
             color=(0x1F, 0x37, 0x64) if level <= 2 else (0x2E, 0x2E, 0x2E))
    return p


# ---------- 读取并解析 Markdown ----------
with open(SRC, encoding="utf-8") as f:
    lines = f.read().splitlines()

i = 0
n = len(lines)
body_count = 0
front_matter = True

# ---------- 处理封面标题（开头的标题与副标题） ----------
while i < n and not lines[i].strip():
    i += 1
if i < n:
    add_heading(lines[i].strip(), 1)
    i += 1
if i < n and lines[i].strip().startswith("（"):
    p = doc.add_paragraph()
    p.alignment = WD_ALIGN_PARAGRAPH.CENTER
    p.paragraph_format.space_after = Pt(16)
    run = p.add_run(lines[i].strip())
    set_font(run, name="黑体", size=13, color=(0x40, 0x40, 0x40))
    i += 1

while i < n:
    raw = lines[i]
    line = raw.rstrip()
    stripped = line.strip()

    # 空行
    if not stripped:
        i += 1
        continue

    # 分隔线：其后紧跟的“一、xxx”视为一级标题
    if re.fullmatch(r"[─\-=]{4,}", stripped):
        j = i + 1
        while j < n and not lines[j].strip():
            j += 1
        title = lines[j].strip() if j < n else ""
        if re.match(r"^[一二三四五六七八九十]+、", title) and j == i + 1:
            if "DEBUG" in sys.argv:
                print(f"[H2] line {j + 1}: {title}")
            add_heading(title, 2)
            i = j + 1          # 其后若仍有分割线，由下一轮循环消化
            front_matter = False
            continue
        i += 1
        continue

    # 标题
    m = re.match(r"^(#{1,4})\s+(.*)$", stripped)
    if m:
        level = len(m.group(1))
        add_heading(m.group(2).strip(), level)
        i += 1
        continue

    # 引用块 -> 说明文字
    if stripped.startswith(">"):
        text = stripped.lstrip("> ").strip()
        if text:
            add_body(text, indent=False, size=10.5, space_before=2, space_after=2)
        i += 1
        continue

    # 列表项
    m = re.match(r"^([-*])\s+(.*)$", stripped)
    if m:
        add_body("· " + m.group(2).strip(), indent=False, space_before=0, space_after=0)
        i += 1
        continue

    m = re.match(r"^(\d+)\.\s+(.*)$", stripped)
    if m:
        add_body(f"{m.group(1)}. {m.group(2).strip()}", indent=False)
        i += 1
        continue

    # 封面信息（汇报日期 / 汇报范围）：逐行成段，不与后文合并
    if front_matter:
        add_body(stripped, indent=False, size=11, space_after=2)
        i += 1
        continue

    # 跨行段落：连续的非空、非结构行合并为一段（中文按原样拼接）
    buf = [stripped]
    base_indent = len(raw) - len(raw.lstrip())
    i += 1
    while i < n:
        next_raw = lines[i]
        nxt = next_raw.strip()
        if not nxt or re.fullmatch(r"[─\-=]{4,}", nxt):
            break
        next_indent = len(next_raw) - len(next_raw.lstrip())
        # 与当前段同级或更浅的编号行 / 结构行 -> 新的一段
        if (re.match(r"^#{1,4}\s+", nxt) or nxt.startswith(">")
                or re.match(r"^[-*]\s+", nxt) or re.match(r"^\d+\.\s+", nxt)):
            break
        if (re.match(r"^[一二三四五六七八九十]+、", nxt)
                and next_indent <= base_indent):
            break
        # 前一行以冒号结尾说明是引导语，后一行另起一段
        if buf[-1].endswith(("：", ":")):
            break
        buf.append(nxt)
        i += 1

    # 中文排版：跨行拼接不加空格，仅在英文/数字词之间补一个空格
    text = ""
    for seg in buf:
        if text and re.search(r"[A-Za-z0-9)\]]$", text) and re.match(r"^[A-Za-z0-9(\[]", seg):
            text += " " + seg
        else:
            text += seg
    text = re.sub(r"\s{2,}", " ", text).strip()
    leading = len(raw) - len(raw.lstrip())

    # 缩进的编号行（一、二、三…）-> 悬挂缩进的小点，不与后文合并
    if 0 < leading <= 2 and re.match(r"^[一二三四五六七八九十]+、", text):
        p = doc.add_paragraph()
        pf = p.paragraph_format
        pf.line_spacing = 1.5
        pf.left_indent = Pt(24)
        pf.first_line_indent = Pt(-12)
        run = p.add_run(cn_quotes(text))
        set_font(run, size=12)
        body_count += 1
        continue

    # 正文段落缩进，短标题性行不加缩进
    indent = not (len(text) < 22 and not text.endswith(("。", "：", "；")))
    add_body(text, indent=indent)
    body_count += 1

doc.save(DST)
print(f"OK -> {DST}  (段落数 {body_count})")
