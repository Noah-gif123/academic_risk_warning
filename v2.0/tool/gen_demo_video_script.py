# -*- coding: utf-8 -*-
"""生成《演示视频话术脚本》Word（按课程 5 条要求：MP4 / 背景与社会责任 / 环境技术部署 / 功能演示 / 总结）
用法：py gen_demo_video_script.py
"""
import os
from docx import Document
from docx.shared import Pt, Cm, RGBColor
from docx.enum.text import WD_ALIGN_PARAGRAPH
from docx.enum.table import WD_TABLE_ALIGNMENT
from docx.oxml.ns import qn
from docx.oxml import OxmlElement

ROOT = r'E:\JavaDEVIDEA\workspace\JavaProject\academic_risk_warning\v2.0'
OUT = os.path.join(ROOT, 'word', '演示视频话术脚本.docx')

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
normal.paragraph_format.line_spacing = 1.4
normal.paragraph_format.space_after = Pt(2)


def set_font(run, size=10, bold=False, color=INK, font=CJK):
    run.font.size = Pt(size); run.font.bold = bold; run.font.color.rgb = color; run.font.name = font
    run._element.rPr.rFonts.set(qn('w:eastAsia'), font)


def p(text='', size=10, bold=False, color=INK, font=CJK, align=None, before=0, after=2, line=1.4, indent=0):
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
    par.paragraph_format.space_before = Pt(9); par.paragraph_format.space_after = Pt(3)
    set_font(par.add_run(text), 11.5, True, BLUE, CJK_B)


def shade(cell, color):
    tcPr = cell._tc.get_or_add_tcPr()
    shd = OxmlElement('w:shd'); shd.set(qn('w:val'), 'clear'); shd.set(qn('w:color'), 'auto'); shd.set(qn('w:fill'), color)
    tcPr.append(shd)


def table(rows, widths, size=9, head=True):
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
    tblPr = t._tbl.tblPr
    layout = OxmlElement('w:tblLayout'); layout.set(qn('w:type'), 'fixed'); tblPr.append(layout)
    for ri, row in enumerate(t.rows):
        trPr = row._tr.get_or_add_trPr(); trPr.append(OxmlElement('w:cantSplit'))
        if head and ri == 0:
            trPr.append(OxmlElement('w:tblHeader'))
    for ci, w in enumerate(widths):
        t.columns[ci].width = Cm(w)
        for cell in t.columns[ci].cells:
            cell.width = Cm(w)
    return t


def script_box(lines, color='F2F7FB'):
    """口播稿：浅蓝底 + 左竖线，可直接照念"""
    for i, line in enumerate(lines):
        par = doc.add_paragraph()
        pf = par.paragraph_format
        pf.left_indent = Pt(10); pf.space_before = Pt(2 if i == 0 else 0); pf.space_after = Pt(2); pf.line_spacing = 1.5
        pPr = par._p.get_or_add_pPr()
        shd = OxmlElement('w:shd'); shd.set(qn('w:val'), 'clear'); shd.set(qn('w:color'), 'auto'); shd.set(qn('w:fill'), color)
        pPr.append(shd)
        bdr = OxmlElement('w:pBdr'); left = OxmlElement('w:left')
        left.set(qn('w:val'), 'single'); left.set(qn('w:sz'), '18'); left.set(qn('w:space'), '6'); left.set(qn('w:color'), '0171BB')
        bdr.append(left); pPr.append(bdr)
        set_font(par.add_run(line), 10, False, INK, CJK)


def footer():
    par = sec.footer.paragraphs[0]
    par.alignment = WD_ALIGN_PARAGRAPH.CENTER
    set_font(par.add_run('演示视频话术脚本 ｜ 基于 RAG 与多智能体的学情预警与个性化学习管理系统　　'), 8, False, MUTE)
    r = par.add_run(); fld = OxmlElement('w:fldSimple'); fld.set(qn('w:instr'), 'PAGE')
    r._element.addnext(fld); set_font(r, 8, False, MUTE)


footer()

# ============================================================ 封面
p('', after=70)
p('演示视频话术脚本', 26, True, INK, CJK_B)
p('基于 RAG 与多智能体的学情预警与个性化学习管理系统的设计与实现', 11, False, BLUE, CJK_B, after=6)
p('（v2.0 定稿版 ｜ 成片约 10 分钟 ｜ 输出 MP4）', 10, False, SUB, after=16)
table([
    ['课程要求', '本脚本对应章节'],
    ['① 视频格式为 MP4', '六、剪辑与交付（参数、命名、封装）'],
    ['② 先讲选题背景，并结合安全、健康与福祉、环境保护的社会责任', '三、S2 选题背景　＋　S3 社会责任'],
    ['③ 介绍开发环境、使用技术、部署方式', '三、S4 开发环境与技术　＋　S5 部署方式'],
    ['④ 介绍主要功能，演示讲解运行效果', '三、S6 功能演示（6 个场景）　＋　S7 运行效果与指标'],
    ['⑤ 总结优点、不足、完善改进与推广计划', '三、S8 总结与展望'],
], [10.6, 6.2], size=9.5)
p('', after=10)
p('使用方式：按 S1–S9 顺序录屏，浅蓝底纹段落是**可直接照念的口播稿**；每段标了时间码与画面操作，建议先按「五、录制操作清单」把页面点一遍再正式录。', 9, False, MUTE)
doc.add_page_break()

# ============================================================ 一、拍摄前准备
h1('拍摄前准备（建议提前 15 分钟）', '一')
table([['检查项', '要求', '怎么确认']] + [
    ['后端已启动', '8080 可访问，AI 通道就绪', '浏览器打开 http://127.0.0.1:8080/api/ai/health，返回 200'],
    ['前端已启动', '5173 可打开登录页', '浏览器打开 http://localhost:5173'],
    ['数据库在位', '540 条课程级判定', 'select count(*) from risk_state where is_void=0'],
    ['报告已就绪', '教师端与辅导员端各有一份历史报告', '提前打开一次报告弹窗，避免现场等 3–14 秒生成'],
    ['演示卷在位', '「AI 批改演示卷」与 20250001 的提交', '教师端「交卷与批改」能看到该卷'],
    ['账号可登录', '5 个账号各试一次（口令 000000）', '1001 / 1004 / C001 / 20250001 / admin001'],
    ['屏幕与录音', '1920×1080、缩放 100%；麦克风无底噪', '录 10 秒回放检查音画'],
    ['敏感信息', '演示库为合成数据；不出现密钥、ai.env、真实手机号', '录制前关掉 IDEA、终端里的 ai.env 窗口'],
], [3.0, 7.2, 6.6], size=9)

# ============================================================ 二、成片规范
h1('成片规范', '二')
table([['项', '要求']] + [
    ['格式', 'MP4（H.264 + AAC），容器封装 MP4'],
    ['分辨率 / 帧率', '1920×1080，30fps（录屏即可；PPT 静帧可 1080p）'],
    ['声音', '普通话，语速约 230 字/分钟；建议单独录音后对轨，避免键盘噪音'],
    ['字幕', '全程中文硬字幕（或 SRT 同封）；术语与数字必须与画面一致'],
    ['时长', '完整版 9–10 分钟；若限时 6 分钟，按第七节精简方案裁剪'],
    ['命名', '23001021010_狄珂_学情预警系统_演示视频.mp4'],
    ['禁止', '不出现真实学生姓名与手机号、不出现密钥与服务器口令、不出现与项目无关的桌面内容'],
], [3.2, 13.6], size=9)

# ============================================================ 三、分镜与口播稿
h1('分镜与口播稿（主体）', '三')

SCENES = [
    ('S1', '片头', '00:00–00:15', '标题页（黑底白字或 PPT 封面）', '无操作，静帧 + 字幕',
     ['各位老师好，我是软件工程专业的狄珂，学号 23001021010。',
      '下面演示我的毕业设计：《基于 RAG 与多智能体的学情预警与个性化学习管理系统的设计与实现》。'],
     '报题目时放慢，题目全称不要念错；字幕同步显示题目。'),

    ('S2', '选题背景（为什么做）', '00:15–01:45', 'PPT 三页：痛点一/二/三 → 大模型带来的新问题',
     '切 PPT；讲到"新问题"时停一拍',
     ['先说为什么做这个题目。',
      '高校的过程性考核里，考勤、作业、随堂测验、项目分项分散在不同老师手里，学生往往到期末才发现有挂科风险，那时干预窗口已经过去了。这是第一痛点。',
      '第二个问题是阈值靠经验。同样是 60 分，在均分 80 的班和均分 45 的班里含义完全不同，直接比阈值会误判。',
      '第三个问题，学校即使做了预警，通知往往只有一句"你被预警了"，不给依据、不给行动，学生不认，老师也不用。',
      '而这两年大模型进来以后，又多了新问题：模型会编数字、说得很像但不可复算。一旦报告里的数字对不上，预警就失去公信力。',
      '所以我把题目定为：把"算得准"和"讲得清"分开做——数值由规则算，语言由模型写，依据由检索给。'],
     '这一段的落点是最后一句，语速放慢；不要展开技术细节（留给 S4）。'),

    ('S3', '社会责任（安全 / 健康与福祉 / 环境保护）', '01:45–03:00', 'PPT 一页三栏：安全与正确性｜健康与福祉｜环境与公平',
     '讲到每条时用鼠标或激光笔指向对应栏',
     ['这个项目涉及学生数据，所以我想先讲三条社会责任。',
      '第一，安全与正确性。系统不替老师下结论，只做辅助：报告里每个数字都来自事实包，发布前用十几条规则逐项对账，越界的数字会被拦下；跨班、跨课程的越权访问直接拒绝。',
      '第二，健康与福祉。预警的目的不是给谁贴标签，而是早发现、早帮扶。所以学生端看到的不是评判，而是"你差多少、马上能做什么"；数据不足的时候我们选择熔断、不出结论，而不是给一个看起来很专业的错误数字；检索不到依据时，答疑会直接拒答，不编造。',
      '第三，环境保护与评价公平。全流程无纸化——考勤、作业、测验、报告都在线，减少打印与耗材；模型有日限额，失败自动降级到规则模板，避免无谓的算力消耗。另外我们做了跨班尺度校准，避免"严老师的班被系统性误判"，这关系到评价公平。'],
     '这是课程明确要求的一条，必须讲满：安全、健康与福祉、环境保护三块都要出现关键词。'),

    ('S4', '开发环境与使用技术', '03:00–04:20', 'PPT 一页技术栈图（分层）+ 一页"离线构建"说明',
     '可切入 IDEA 工程结构 3–5 秒，展示 backend / frontend / sql 目录',
     ['接下来说开发环境和用到的技术。',
      '后端是 Java 17、Spring Boot 4.0.6、MyBatis-Plus 3.5.15，数据库 MySQL 8.0.30，一共 60 张表，其中和学情判定直接相关的有 13 张核心表。前端是 Vue 3 加 Vite 加 Tailwind。',
      'AI 部分做了三层通道：Dify 工作流、直连大模型（DeepSeek 或百炼）、以及规则模板兜底；检索是全文索引加 bge-m3 向量的混合检索，融合之后过闸门过滤，检索不到就拒答。',
      '开发上有一个特别之处：这台机器不能用 Maven 和 esbuild，所以构建改成了离线方式——javac 加固定依赖目录，前端跑 Vite。相应的回归也脚本化了：单元测试用 JUnit Platform 直接跑，接口和报告用 PowerShell 脚本验收。'],
     '注意不要念成"微服务"；本项目是单体分层，被问到时再解释取舍。'),

    ('S5', '项目部署方式', '04:20–05:10', 'PPT 一页部署拓扑（浏览器—前端 5173—后端 8080—MySQL 3306—Dify 8090）',
     '可切终端展示启动命令（只露命令，不露密钥）',
     ['部署是单机形态：MySQL 在本机，后端一个 Spring Boot 进程跑在 8080，前端 Vite 跑在 5173；如果要走工作流通道，本机再起一个 Dify，8090。',
      '密钥不入库，通过 ai.env 注入，由 application.yml 的 spring.config.import 自动加载；不配置也能启动，此时 AI 能力自动降级成规则模板，报告照样能出。',
      '这一点对学校落地很重要：没有大模型额度，也能先把预警用起来。'],
     '强调"不配也能启动"——这是可推广性的关键证据。'),

    ('S6.1', '功能演示①：教师端与判定引擎', '05:10–05:55', '屏幕：教师端登录 → 预警监控（选教学班 201）',
     '登录 1001（口令 000000）→ 左侧「预警监控」→ 右上角选「CS-01-A 大计基 01班」',
     ['现在进入系统演示。我用教师 1001 登录，进入"预警监控"，右上角选择我任教的教学班。',
      '这里的等级不是只看总分：判定有三个通道——A 通道看缺勤迟到这类硬门槛，B 通道看班内相对位置，C 通道做跨班可比折算。',
      '最后按 75、60、40 分档映射成红橙黄绿；缺勤超过上限会触发下限保护，直接顶到红档，不会被高分掩盖。每条判定都是一份快照，记了引擎版本，任何时候都能复算。'],
     '语速可稍快；讲"三通道"时鼠标划过表头，让画面有指向。'),

    ('S6.2', '功能演示②：班级报告与数字对账（重点）', '05:55–06:55', '屏幕：预警监控右上角「班级报告」→ 弹窗',
     '点「班级报告」→ 展开「数字对账」明细 → 指「参考依据」→ 点一个「报告涉及的学生」',
     ['接下来是本系统的核心：班级报告。',
      '这份报告是"数字由规则算、语言由模型写"——事实包先把人数、平均分、及格线、预警条数算好，模型只负责组织语言，不许自己计算。',
      '定稿前还有一道数字对账：用十四条规则把报告里的关键数字和事实包逐项比对。现在显示五项全部一致；如果模型把"平均 81 分"写成 80 分，这里会标红并要求重新生成。',
      '参考依据不是套话，是检索到的课程资料原文，附了摘句；报告涉及的学生可以直接点，跳到名单继续处置。'],
     '这段是全片最有说服力的地方：一定要把「数字对账」明细展开给镜头看。'),

    ('S6.3', '功能演示③：课程总报告（三种口径可加总）', '06:55–07:30', '屏幕：教师端「全部课程」→ 数据结构卡片「课程总报告」',
     '切换到教师 1004 → 全部课程 → 点卡片右下「课程总报告」',
     ['用一个教三个班的老师 1004 演示。班级报告只能看一个班，所以我又做了课程维度的总报告：把三个班的事实包聚合起来。',
      '人数相加 135 就是 45 乘 3，红灯 4 是三个班 1 加 2 加 1。所以这三份报告不是三个模板换皮，而是同一套事实的不同聚合，数字能互相加总、能对上账。'],
     '预先切好账号，避免现场等登录；数字要念准。'),

    ('S6.4', '功能演示④：辅导员端（跨课程视角 + 处置闭环）', '07:30–08:05', '屏幕：辅导员端 C001 → 班级管理 → 选行政班 → 班级报告 → 点学生名',
     '登录 C001 → 班级管理 → 选「计算机1班」→ 点「班级报告」→ 点「报告涉及的学生」中的名字',
     ['辅导员关心的不是某一门课，而是这个班哪些课集中出问题、哪些学生同时被多门课预警。所以行政班的统计口径按选课记录统计，能指出"集中风险课程"和"多门预警学生"。',
      '点名字可以直接进学生详情，看完就能登记干预——报告不是终点，处置才是闭环的落点。'],
     '点名字后右侧抽屉会打开，注意镜头跟一下。'),

    ('S6.5', '功能演示⑤：学生端（可解释 + 答疑带引用）', '08:05–08:40', '屏幕：学生端 20250001 → 学情预警 → 详情；课程答疑',
     '登录 20250001 → 左侧「学情预警」→ 点「详情」→ 往下滚到课程答疑，输入一个问题并回车',
     ['学生端强调可读与可行动：不只是告诉你被预警，还告诉你差多少、马上能做什么。',
      '答疑只在本课程资料里检索，回答必须带引用，标出文档名和段号；检索不到就直接拒答，不编造——这是这套系统对待大模型的基本态度：宁可说不知道。'],
     '提前准备一个"能答"的问题和一个"资料里没有"的问题，现场各演示一次。'),

    ('S6.6', '功能演示⑥：AI 批改与建议分', '08:40–09:20', '屏幕：教师端「交卷与批改」→ 打开演示卷 → 「AI 批改建议」',
     '1001 → 交卷与批改 → 选「AI 批改演示卷」→ 点 20250001 那份「批改」→ 右上角「AI 批改建议」',
     ['最后看 AI 批改。这是 6 题 100 分的演示卷，客观题自动判分，这份学生得 10 分；三道简答题由 AI 给建议分和逐维度扣分理由。',
      'AI 建议是 25、13、6，教师按评分标准给的是 22、12、5，平均差不到 2 分，排序完全一致。而且 AI 不直接改分——要点"填入评分框"再"提交批改"才生效。'],
     '这一页建议放慢，把"AI 建议 vs 教师定稿"的三行数字念清楚。'),
]

for sid, title, t, screen, action, narration, tip in SCENES:
    h2(f'{sid}　{title}')
    table([['时间码', '画面', '操作（点击路径）'],
           [t, screen, action]], [2.6, 6.6, 7.6], size=9)
    p('口播稿（照念）：', 9, True, BLUE, CJK_B, before=4, after=2)
    script_box(narration)
    p('提示：' + tip, 8.8, False, MUTE, before=2, after=6)

# S7 / S8 / S9
SCENES2 = [
    ('S7', '运行效果与关键指标', '09:20–09:50', 'PPT 一页指标表（可叠在报告画面上）',
     '切 PPT，逐条念指标',
     ['再补充几个实测数字，都是本机跑出来的：',
      '课程级判定 540 条；单元测试 25 个全部通过；越权用例 7 个全部被拒绝；检索做了 64 例盲评，混合检索的 recall@4 是 1.0，不可答问题的拒答率是 0.9；三类群体报告的数字对账全部一致；AI 批改与人工评分的平均差在 2 分以内。'],
     '数字要与画面表格一致；不要念成"准确率 100%"这种绝对表述。'),
    ('S8', '总结：优点、不足、改进与推广', '09:50–10:30', 'PPT 三栏：优点｜不足｜改进与推广',
     '切 PPT，按三栏讲',
     ['最后做个总结。',
      '优点是四点：判定可复算、报告可对账、失败可降级、处置可留痕，形成"预警—研判—干预—复核"的闭环。',
      '不足我也如实说：没有做并发压测，没有接入持续集成；课程知识库目前只有 5 门课 29 篇资料，部分报告的依据还偏通用；报告里的姓名需要做脱敏；同一目标重复生成没有互斥。',
      '下一步的改进方向是 v3.0：一是把数据做成能体现跨班差异的拟真数据，并补齐课程因子校准；二是做统计分析看板，把"预警到底有没有效"做成可归因的指标，包括对照组；三是把 AI 做成四角色 Agent，加证据台账与记忆，并支持 Dify 双实现。',
      '推广上，先在 1 个班试点 4 周，跑通三个指标，再扩展到院系；判定口径是配置化的，换课程、换学校改配置而不是改代码。'],
     '不足要主动讲，评委更看重这一点；"推广计划"必须给出试点路径与时间。'),
    ('S9', '片尾', '10:30–10:45', '标题页 + 致谢字幕',
     '无操作',
     ['以上就是我的演示，感谢各位老师。'],
     '最后 2 秒留白，方便剪辑。'),
]
for sid, title, t, screen, action, narration, tip in SCENES2:
    h2(f'{sid}　{title}')
    table([['时间码', '画面', '操作（点击路径）'],
           [t, screen, action]], [2.6, 6.6, 7.6], size=9)
    p('口播稿（照念）：', 9, True, BLUE, CJK_B, before=4, after=2)
    script_box(narration)
    p('提示：' + tip, 8.8, False, MUTE, before=2, after=6)

doc.add_page_break()

# ============================================================ 四、录制顺序清单
h1('录制操作清单（按顺序点，边点边念）', '四')
table([['#', '账号 / 页面', '操作', '对应段落']] + [
    [1, '—', '打开 PPT 封面，开始录屏（先录 3 秒静帧）', 'S1'],
    [2, '—', 'PPT：痛点三页 + 新问题一页', 'S2'],
    [3, '—', 'PPT：社会责任三栏', 'S3'],
    [4, '—', 'PPT：技术栈 + 离线构建；可切 IDEA 目录 3 秒', 'S4'],
    [5, '—', 'PPT：部署拓扑；可切终端展示启动命令（遮住密钥）', 'S5'],
    [6, '教师 1001', '登录 → 预警监控 → 选 CS-01-A 教学班', 'S6.1'],
    [7, '教师 1001', '班级报告 → 展开数字对账 → 指参考依据 → 点涉及学生', 'S6.2'],
    [8, '教师 1004', '全部课程 → 数据结构 → 课程总报告', 'S6.3'],
    [9, '辅导员 C001', '班级管理 → 选计算机1班 → 班级报告 → 点学生名', 'S6.4'],
    [10, '学生 20250001', '学情预警 → 详情 → 课程答疑（先问可答、再问无依据）', 'S6.5'],
    [11, '教师 1001', '交卷与批改 → 演示卷 → 批改 → AI 批改建议', 'S6.6'],
    [12, '—', 'PPT：指标表', 'S7'],
    [13, '—', 'PPT：优点 / 不足 / 改进与推广', 'S8'],
    [14, '—', 'PPT：致谢页，静帧 3 秒后停止录制', 'S9'],
], [0.9, 3.0, 9.6, 2.3], size=9)

# ============================================================ 五、兜底与禁语
h1('现场兜底与禁语', '五')
h2('三种异常怎么讲（不要停录，继续念）')
table([['异常', '口播应对']] + [
    ['AI 报告显示"规则生成"', '"这次报告走的是规则模板通道，因为模型服务未就绪；系统设计上保证任一层失败都能出报告，来源字段会明确标注。"'],
    ['报告生成超过 20 秒', '"生成需要调用模型，通常几秒到十几秒；我们今天先用已生成的报告讲结构，课后可以再现场生成一次。"'],
    ['答疑被拒答', '"这个问题在课程资料里没有依据，系统选择拒答而不是编造——这是我们刻意设计的边界。"'],
    ['网络中断', '切到已生成的报告与本地统计，说明"系统在无外网时仍可用规则模板出报告"。'],
], [4.6, 12.2], size=9)
h2('五条禁语')
for i, t in enumerate([
    '不说"绝对不会错"——说"关键数字可对账、越界会被拦截"',
    '不说"已经在真实学生中上线使用"——说"演示环境 + 合成数据，下一步做班级试点"',
    '不说"用了大模型所以更智能"——要说清"数字由规则算、模型只写结论"',
    '不念真实学生姓名与手机号，也不展示密钥、ai.env、服务器口令',
    '不承诺"完全自动、无需人工"——AI 批改、干预草稿都需要教师确认',
], 1):
    p(f'{i}. {t}', 9.5, False, INK, after=1)

# ============================================================ 六、剪辑与交付
h1('剪辑与交付', '六')
table([['环节', '要求']] + [
    ['转场', '只做硬切或淡入淡出；不加花哨特效，避免遮挡界面文字'],
    ['字幕', '全程硬字幕；数字、术语（三通道、数字对账、证据台账）与画面一致'],
    ['音量', '口播 −16 LUFS 左右；键盘声与环境音降噪'],
    ['画面标注', '关键界面加 2–3 个箭头/框选标注（数字对账、来源字段、AI 建议分）'],
    ['片头片尾', '片头 3 秒标题卡；片尾致谢 3 秒'],
    ['导出', 'H.264 / MP4，1080p，码率 6–8 Mbps，文件名：23001021010_狄珂_学情预警系统_演示视频.mp4'],
    ['自查', '成片时长 9–10 分钟；五条课程要求逐条对照（本文件封面表格）'],
], [3.0, 13.8], size=9)

# ============================================================ 七、精简版
h1('6 分钟精简版（若限时）', '七')
table([['保留', '时长', '说明']] + [
    ['S1 片头', '15s', '原样'],
    ['S2 + S3 合并', '2.5min', '背景只讲两条痛点 + 新问题；社会责任三块各一句，保留关键词'],
    ['S4 + S5 合并', '1.5min', '技术与部署各一段，删掉"离线构建"细节'],
    ['S6.2 班级报告', '1min', '保留，含数字对账'],
    ['S6.3 课程总报告', '30s', '保留（讲可加总）'],
    ['S6.6 AI 批改', '30s', '保留（讲建议分与教师定稿）'],
    ['S8 总结', '40s', '优点两句、不足两句、推广一句'],
], [4.2, 2.0, 10.6], size=9)
p('砍掉的：S6.1（判定引擎）压成 20 秒口播；S6.4 辅导员端、S6.5 学生端、S7 指标可只留一句话带过。', 9, False, SUB, before=3)

# ============================================================ 八、字数核对
h1('时长与口播字数核对（录完后核对）', '八')
table([['段落', '计划时长', '口播字数（约）', '语速检查']] + [
    ['S1 片头', '15s', '约 60 字', '放慢'],
    ['S2 背景', '90s', '约 330 字', '正常'],
    ['S3 社会责任', '75s', '约 270 字', '正常，关键词加重'],
    ['S4 技术', '80s', '约 300 字', '可稍快'],
    ['S5 部署', '50s', '约 190 字', '正常'],
    ['S6 功能演示', '250s', '约 740 字', '按画面走，宁慢勿快'],
    ['S7 指标', '30s', '约 120 字', '数字慢读'],
    ['S8 总结', '40s', '约 170 字', '正常'],
    ['S9 片尾', '15s', '约 20 字', '放慢'],
    ['合计', '约 10 分 45 秒', '约 2200 字', '语速约 230 字/分钟'],
], [3.4, 3.0, 4.4, 6.0], size=9)

doc.save(OUT)
print('WROTE ' + OUT)
print('paragraphs=%d tables=%d' % (len(doc.paragraphs), len(doc.tables)))
