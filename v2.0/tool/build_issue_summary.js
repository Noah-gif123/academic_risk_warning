/* build_issue_summary.js —— 生成《基于RAG与多智能体的学情预警系统 v2.0 · 问题汇总》
 *
 * 用途：把 M16 判定口径改造后仍然存在的算法/数据/工程问题，连同"本轮已修复"一起落成一份可评审的 Word。
 * 内容全部来自实测（库里的数、单测、提交号），不写没验证过的结论。
 *
 * 用法：node build_issue_summary.js [输出路径]
 *   默认输出：v2.0/word/基于RAG与多智能体的学情预警系统_问题汇总.docx
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
const BODY = 21;   // 10.5pt
const SMALL = 18;  // 9pt
const CONTENT_W = 9070; // twips ≈ 16cm

const B = { style: BorderStyle.SINGLE, size: 4, color: '9AA0A6' };
const BORDERS = { top: B, bottom: B, left: B, right: B, insideHorizontal: B, insideVertical: B };

function run(text, o = {}) {
  return new TextRun({
    // Word 里不该出现 Markdown 的反引号：源文里为便于对照代码用了 `xxx`，这里统一去掉
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
    indent: o.indent,
    bullet: o.bullet ? { level: 0 } : undefined
  });
}

function h1(text) {
  return new Paragraph({
    heading: HeadingLevel.HEADING_1,
    spacing: { before: 260, after: 120 },
    children: [new TextRun({ text, font: CJK_B, size: 28, bold: true, color: '1F3864' })]
  });
}

function h2(text) {
  return new Paragraph({
    heading: HeadingLevel.HEADING_2,
    spacing: { before: 200, after: 90 },
    children: [new TextRun({ text, font: CJK_B, size: 24, bold: true, color: '2E5395' })]
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
  spacing: { before: 0, after: 60 },
  children: [new TextRun({ text: '基于RAG与多智能体的学情预警系统 v2.0 · 问题汇总', font: CJK_B, size: 36, bold: true })]
}));
children.push(new Paragraph({
  alignment: AlignmentType.CENTER,
  spacing: { after: 200 },
  children: [new TextRun({ text: 'M16 判定口径改造后 · 算法层与数据层的已知问题清单（2026-10-30）', font: CJK, size: 20, color: '666666' })]
}));

children.push(h1('一、总体结论'));
children.push(para('判定口径（v2.1.0）已完成改造并上线到本机演示库：六个维度全部接入判定链路（成绩构成、已得成绩、课程类型、该科历史挂科率、教师风格、个人历史挂科率），另叠加考勤硬门槛；结果一人一课一行落库，每一行的 data_quality_json 都能回溯到具体因子取值。'));
children.push(para([
  run('结论分两句：', { bold: true }),
  run('结构上是完整的、可演示、可回溯的；但算法严谨性上仍有 4 项已知短板（见第二节），其中 2 项属于"因子有消费者、没有生产者"，会在学期切换后暴露。')
]));
children.push(para('实测基线（2026-10-30，本学期 540 条选课，v2.1.0）：红 16 / 橙 3 / 黄 100 / 绿 421；16 个红全部来自"缺课 > 允许缺课"的硬门槛；3 个橙的需考是 62–70 分（期末必须考过及格线）。'));

children.push(h1('二、算法层面的已知局限（重点）'));
children.push(para('按"评审/答辩被问到的概率"排序，四项均为当前实现客观存在，不是待修 bug。'));
children.push(table(
  ['编号', '问题', '影响', '优先级'],
  [
    ['A1', '两个因子有消费者、没有生产者', '学期切换后教师尺度 Δ、课程挂科率不再更新', '高'],
    ['A2', '三个难度因子共线，可能重复计数', '风险分被过度放大（极限 ×1.67）', '高'],
    ['A3', '需考是点估计，没有不确定性度量', '无法回答"他挂科的概率有多大"', '中'],
    ['A4', '没有趋势项；阈值/折点未做数据标定', '看不到"最近连续下滑"，阈值只能答"讨论确定"', '中']
], [900, 2700, 3800, 1100]));

children.push(h2('A1　教师尺度与挂科历史"有消费者、没有生产者"'));
children.push(para('现象：引擎会读 teacher_style.delta（教师尺度）、course_fail_rate_history（课程历史挂科率）、student_fail_history（个人挂科历史）。但代码全文检索显示，这三个数据源目前只有演示数据脚本（seed 与 SyntheticDataService）写入，没有任何"计算/更新"管道。'));
children.push(para('影响：真实系统里运行一个学期后，Δ 不会重估、课程挂科率不会随学期结算更新、挂科历史也不会自动归档；因子会在旧值上一直跑，等于"读一个人的旧记忆"。'));
children.push(para('建议：补两条管道——① 教师尺度估计（同课程跨班对比 + 按样本量收缩，写入 teacher_style）；② 学期结算归档（成绩定稿后写 student_fail_history、按课程聚合写 course_fail_rate_history）。工作量约 1–2 天。'));

children.push(h2('A2　课程类型 / 历史挂科率 / 教师风格共线，可能把同一件事算三遍'));
children.push(para('现象：三个因子语义高度相关（都是"这门课/这个班难"），当前以独立乘子连乘。极限情况 1.10 × 1.20 × 1.15 × 1.10 = 1.67，风险分可被整体抬高 67%。实际数据里已出现叠加：数据结构（考查课 0.90）因历史挂科率 26.67% 触到 ×1.20 上限，乘子回到 1.07；严格老师班（103/105）乘子 1.163–1.194。'));
children.push(para('影响：对"本来就难的课"双重甚至三重惩罚，跨课程可比性下降；评审很容易追问"这三项是不是重复计了"。'));
children.push(para('建议：把三项先合成一次"难度因子"（例如取对数后相加并夹上限），或做残差化（历史挂科率对课程类型回归后的残差再入模）。改动集中在一处，可对比改造前后分布，工作量半天。'));

children.push(h2('A3　需考是点估计，没有不确定性'));
children.push(para('现象：算法把"期末需考 43 分"当成确定值直接映射为风险分，但过程分本身有波动——teacher_style 表里现成有 std_dev 字段，当前没有使用。'));
children.push(para('影响：无法回答"这名学生挂科的概率是多少""同样需考 43 分，波动大的学生是不是更危险"。对"绝对口径"的可解释性有影响。'));
children.push(para('建议：把风险分改为 P(总评 < 及格线)，用过程分与历史分数分布（或班级标准差）做正态近似；需新增一次分布估计，工作量 1 天，且需要与业务确认呈现方式。'));

children.push(h2('A4　没有趋势项；阈值与折点未经数据标定'));
children.push(para('现象：判定只看"当前累计状态"，看不到"最近三次作业连续下滑""缺课集中在近两周"这类趋势（数据里都有）；分级阈值 75/60/40 与分段映射折点（及格线、起点 = 及格线 − 25）均为业务讨论确定，未用历史学期做回测。'));
children.push(para('影响：预警偏"截面"而非"过程"；评审若问"阈值凭什么是 75"，目前只能回答"业务讨论"。'));
children.push(para('建议：① 增加趋势项（近 N 次分项/考勤的斜率）；② 用 term 4 与 term 8 的成绩做离线回测，算混淆矩阵、召回率与阈值敏感性，作为阈值依据。工作量各约 1 天。'));

children.push(h1('三、数据与工程层面的遗留问题'));
children.push(table(
  ['编号', '问题', '现状与影响', '处理建议'],
  [
    ['D1', '演示库重建后数值与老库不同', 'seed_demo.js 重新生成后随机序列前移，答辩材料里引用的具体分数需按新库核对', '答辩前按新库抽查 3–5 个引用数字'],
    ['D2', '《第二阶段功能演示脚本》示例数字未更新', '文中"期末需考 67.9 / 61.9 / 70.3"仍为旧口径', '按 v2.1.0 重算后替换'],
    ['D3', '代码推送未完成', '本机无 gitee/GitHub 凭据；gitee 需改用 openssl 后端', '补令牌后 push（命令见交接文档 §10）'],
    ['D4', '"剩余 2 次缺课"地板取 40', '使 43 人进入黄档；业务已确认保持现状', '如后续需要更安静，降为 35 即可'],
    ['D5', '36 号脚本带 NOT EXISTS 守卫', '库中若已有旧版 v2.1.0 行，重跑脚本不会覆盖，须先删旧行', '已写入 sql/README 部署顺序'],
  ],
  [900, 2100, 3400, 2200]));

children.push(h1('四、本轮已修复的问题（避免重复排查）'));
children.push(table(
  ['编号', '问题', '修复方式'],
  [
    ['F1', '期末成绩被当成"已得分"混进总评', '过程分只取非终结性分支、按已录项归一；期末只作"需考/还差"目标'],
    ['F2', 'r_final 取到子项权重（=1.0），过程分被整段丢掉', '改取终结性根分支权重（0.60 / 0.40 / 0.50）'],
    ['F3', '考查课大作业已批完仍按"还需得"计算', '终结分支已录分时改看缺口 max(0, 需得 − 已得)'],
    ['F4', 'A 通道硬线被 C 通道乘子顶到 100', '门槛地板（95/100/55/40）不参与连乘，取最大'],
    ['F5', '判定结果与成绩单页两套数', '抽出 ScoreCalculator 作为唯一折算入口，两侧同源'],
    ['F6', 'score_result 无法按已录项重算（缺项即抛）', '新增 POST /api/admin/scores/recompute，未录齐记 PARTIAL'],
    ['F7', '一键重建脚本仍含 405 条本学期期末分', '重新生成 20_rebuild_demo.sql，成绩单直写 PARTIAL/m16.v1'],
    ['F8', '库中演示版 v2.1.0 配置行阈值与脚本不一致', '删除旧演示行后重跑 36 号脚本，阈值统一为 75/60/40 并经接口激活留审计'],
    ['F9', 'personal_prior_1/2 列名与实体映射不一致导致 SQL 报错', '实体加 @TableField 对齐列名'],
    ['F10', '批量重算会给教师/学生刷出大量通知', '新增 app.alert.notify-enabled 总开关，重算期间不发消息'],
  ],
  [900, 3300, 4300]));

children.push(h1('五、建议处理顺序'));
children.push(table(
  ['顺序', '事项', '预估工作量', '预期收益'],
  [
    ['1', 'A2 三个难度因子合成一次', '半天', '消除重复计数质疑，跨课可比'],
    ['2', 'A5（D3）补推送 + D1/D2 数值核对', '半天', '交付完整、材料自洽'],
    ['3', 'A1 两条数据管道（Δ 估计、学期归档）', '1–2 天', '因子有真实来源，学期可演进'],
    ['4', 'A4 离线回测阈值', '1 天', '阈值有数据依据，可写进论文'],
    ['5', 'A3 不确定性（挂科概率）', '1 天', '从"需考多少分"升级为"挂科概率"'],
  ],
  [800, 3300, 1900, 2600]));

children.push(h1('六、当前可追溯性（便于复核）'));
children.push(para('① 单元测试：RiskMathTest 共 20 个用例，覆盖硬门槛、剩余可缺、需考不可达、过程分归一、r_final、终结点缺口、门槛地板、分段映射、教师风格连续映射、挂科率小样本收缩、前科衰减等边界，全部通过。'));
children.push(para('② 实库验证：本学期 540 条选课按 v2.1.0 全量重跑（约 4.6 s），分布与因子取值见第一节；每行 data_quality_json 含 processScore / processCoverage / finalRatio / courseTypeFactor / failRateFactor（含数据来源）/ teacherFactor / personalFactor / factor 等字段。'));
children.push(para('③ 口径可回滚：risk_engine_config 按版本管理，risk_state 记录 engine_version，回切旧版本后重跑即可复原；清理 SQL 见交接文档第七节。'));
children.push(para('④ 本次相关提交：adb2e84（口径改造）、c868650（四因子改造）、1affcaa / c4ddd63 / c297c46（交付包）。'));

children.push(para(''));
children.push(para('说明：本清单只记录"已知且未解决"与"本轮已解决"的事实，不对未验证的推测下结论；A1–A4 的具体改造方案需与业务再确认一次口径后实施。', { size: SMALL, color: '666666' }));

const doc = new Document({
  styles: {
    default: {
      document: { run: { font: CJK, size: BODY } }
    }
  },
  sections: [{
    properties: { page: { margin: { top: 1134, right: 1134, bottom: 1134, left: 1134 } } },
    footers: {
      default: new Footer({
        children: [new Paragraph({
          alignment: AlignmentType.CENTER,
          children: [new TextRun({ children: ['第 ', PageNumber.CURRENT, ' 页 / 共 ', PageNumber.TOTAL_PAGES, ' 页'], font: CJK, size: 18, color: '888888' })]
        })]
      })
    },
    children
  }]
});

const outPath = process.argv[2] || path.join(__dirname, '..', 'word', '基于RAG与多智能体的学情预警系统_问题汇总.docx');
Packer.toBuffer(doc).then((buf) => {
  require('fs').writeFileSync(outPath, buf);
  console.log('已生成：' + outPath + '（' + Math.round(buf.length / 1024) + ' KB）');
});
