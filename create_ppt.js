const pptxgen = require("pptxgenjs");

// ─── Color Palette: 电网蓝主题 ───
const C = {
  primary: "1A3C6D",      // 深蓝 - 标题/主色
  secondary: "2E75B6",    // 中蓝 - 强调
  accent: "F39C12",       // 金色 - 高亮
  lightBg: "F0F4FA",      // 浅蓝灰背景
  darkBg: "0D2137",       // 深色背景
  white: "FFFFFF",
  text: "2C3E50",         // 深色文字
  gray: "7F8C8D",         // 灰色文字
  lightGray: "ECF0F1",    // 浅灰
  tableHeader: "1A3C6D",  // 表头
  tableStripe: "F0F4FA",  // 表格条纹
  tableBorder: "D5DDE5",  // 表格边框
};

// ─── Font settings ───
const FONT_H = "Microsoft YaHei";
const FONT_B = "Microsoft YaHei";

// ─── Helper: shadow factory ───
const makeShadow = () => ({ type: "outer", blur: 4, offset: 2, angle: 135, color: "000000", opacity: 0.1 });

// ─── Initialize Presentation ───
let pres = new pptxgen();
pres.layout = "LAYOUT_16x9";
pres.author = "北京中兴天安资产评估有限公司";
pres.title = "电网宁夏电力有限公司物业费用测算咨询项目工作方案";

// ============================================================
// SLIDE 1: 封面
// ============================================================
{
  let slide = pres.addSlide();
  slide.background = { color: C.darkBg };

  // Decorative top accent line
  slide.addShape(pres.shapes.RECTANGLE, { x: 0, y: 0, w: 10, h: 0.06, fill: { color: C.accent } });

  // Left accent bar
  slide.addShape(pres.shapes.RECTANGLE, { x: 0.8, y: 1.2, w: 0.06, h: 2.6, fill: { color: C.accent } });

  // Main title
  slide.addText("电网宁夏电力有限公司各分公司", {
    x: 1.2, y: 1.2, w: 7.8, h: 0.7,
    fontSize: 30, fontFace: FONT_H, color: C.white, bold: true, margin: 0,
  });
  slide.addText("物业费用测算咨询项目", {
    x: 1.2, y: 1.85, w: 7.8, h: 0.7,
    fontSize: 30, fontFace: FONT_H, color: C.white, bold: true, margin: 0,
  });
  slide.addText("工作方案", {
    x: 1.2, y: 2.5, w: 7.8, h: 0.8,
    fontSize: 38, fontFace: FONT_H, color: C.accent, bold: true, margin: 0,
  });

  // Bottom separator
  slide.addShape(pres.shapes.RECTANGLE, { x: 1.2, y: 3.6, w: 3, h: 0.03, fill: { color: C.secondary } });

  // Company name and date
  slide.addText("北京中兴天安资产评估有限公司", {
    x: 1.2, y: 3.9, w: 7.8, h: 0.5,
    fontSize: 16, fontFace: FONT_H, color: C.gray, margin: 0,
  });
  slide.addText("2026年7月27日", {
    x: 1.2, y: 4.35, w: 7.8, h: 0.4,
    fontSize: 14, fontFace: FONT_H, color: C.gray, margin: 0,
  });

  // Bottom accent line
  slide.addShape(pres.shapes.RECTANGLE, { x: 0, y: 5.565, w: 10, h: 0.06, fill: { color: C.accent } });
}

// ============================================================
// SLIDE 2: 项目概况
// ============================================================
{
  let slide = pres.addSlide();
  slide.background = { color: C.white };

  // Top bar
  slide.addShape(pres.shapes.RECTANGLE, { x: 0, y: 0, w: 10, h: 0.06, fill: { color: C.primary } });

  // Left accent on title
  slide.addShape(pres.shapes.RECTANGLE, { x: 0.5, y: 0.45, w: 0.06, h: 0.45, fill: { color: C.accent } });

  // Title
  slide.addText("项目概况", {
    x: 0.75, y: 0.35, w: 8, h: 0.65,
    fontSize: 28, fontFace: FONT_H, color: C.primary, bold: true, margin: 0,
  });

  // Separator line
  slide.addShape(pres.shapes.RECTANGLE, { x: 0.5, y: 1.05, w: 9, h: 0.015, fill: { color: C.lightGray } });

  // Content card
  slide.addShape(pres.shapes.RECTANGLE, {
    x: 0.5, y: 1.3, w: 9, h: 3.8,
    fill: { color: C.lightBg }, shadow: makeShadow(),
  });

  // Project name label
  slide.addText([
    { text: "项目名称", options: { bold: true, fontSize: 13, color: C.secondary, breakLine: true } },
    { text: "电网宁夏电力有限公司各分公司物业费用测算咨询项目", options: { fontSize: 15, color: C.text } },
  ], { x: 0.9, y: 1.5, w: 8.2, h: 0.9, fontFace: FONT_H, margin: 0, valign: "top" });

  // Brief description label
  slide.addText([
    { text: "简要概况", options: { bold: true, fontSize: 13, color: C.secondary, breakLine: true } },
    { text: "电网宁夏电力有限公司各分公司拟对物业费用测算进行咨询，需对公司物业费用进行测算，以确定其在咨询基准日的市场价值，为国家电网宁夏电力公司各分公司提供价值参考依据。", options: { fontSize: 14, color: C.text } },
  ], { x: 0.9, y: 2.6, w: 8.2, h: 1.5, fontFace: FONT_H, margin: 0, valign: "top" });

  // Icon-like number indicators at bottom
  const stats = [
    { num: "5", label: "评估对象" },
    { num: "3", label: "评估小组成员" },
    { num: "45天", label: "项目周期" },
  ];
  stats.forEach((s, i) => {
    const xx = 1.2 + i * 2.9;
    slide.addShape(pres.shapes.RECTANGLE, {
      x: xx, y: 4.3, w: 2.3, h: 0.6,
      fill: { color: C.primary },
    });
    slide.addText(s.num, {
      x: xx, y: 4.3, w: 2.3, h: 0.35,
      fontSize: 18, fontFace: FONT_H, color: C.white, bold: true, align: "center", margin: 0,
    });
    slide.addText(s.label, {
      x: xx, y: 4.6, w: 2.3, h: 0.3,
      fontSize: 10, fontFace: FONT_H, color: C.lightGray, align: "center", margin: 0,
    });
  });
}

// ============================================================
// SLIDE 3: 咨询目的与价值类型
// ============================================================
{
  let slide = pres.addSlide();
  slide.background = { color: C.white };

  // Top bar
  slide.addShape(pres.shapes.RECTANGLE, { x: 0, y: 0, w: 10, h: 0.06, fill: { color: C.primary } });
  slide.addShape(pres.shapes.RECTANGLE, { x: 0.5, y: 0.45, w: 0.06, h: 0.45, fill: { color: C.accent } });

  slide.addText("咨询目的与价值类型", {
    x: 0.75, y: 0.35, w: 8, h: 0.65,
    fontSize: 28, fontFace: FONT_H, color: C.primary, bold: true, margin: 0,
  });
  slide.addShape(pres.shapes.RECTANGLE, { x: 0.5, y: 1.05, w: 9, h: 0.015, fill: { color: C.lightGray } });

  // Left card: 咨询目的
  slide.addShape(pres.shapes.RECTANGLE, {
    x: 0.5, y: 1.3, w: 4.2, h: 3.8,
    fill: { color: C.lightBg }, shadow: makeShadow(),
  });
  slide.addShape(pres.shapes.RECTANGLE, { x: 0.5, y: 1.3, w: 4.2, h: 0.55, fill: { color: C.primary } });
  slide.addText("咨询目的", {
    x: 0.5, y: 1.3, w: 4.2, h: 0.55,
    fontSize: 16, fontFace: FONT_H, color: C.white, bold: true, align: "center", margin: 0,
  });
  slide.addText("拟对电网宁夏电力有限公司各分公司物业费用测算进行咨询，确定其市场价值，为各分公司提供价值参考依据。", {
    x: 0.8, y: 2.1, w: 3.6, h: 2.6,
    fontSize: 14, fontFace: FONT_H, color: C.text, margin: 0, valign: "top",
  });

  // Right card: 价值类型与基准日
  slide.addShape(pres.shapes.RECTANGLE, {
    x: 5.3, y: 1.3, w: 4.2, h: 3.8,
    fill: { color: C.lightBg }, shadow: makeShadow(),
  });
  slide.addShape(pres.shapes.RECTANGLE, { x: 5.3, y: 1.3, w: 4.2, h: 0.55, fill: { color: C.primary } });
  slide.addText("价值类型与基准日", {
    x: 5.3, y: 1.3, w: 4.2, h: 0.55,
    fontSize: 16, fontFace: FONT_H, color: C.white, bold: true, align: "center", margin: 0,
  });

  // Value type
  slide.addText("价值类型", {
    x: 5.6, y: 2.1, w: 3.6, h: 0.35,
    fontSize: 13, fontFace: FONT_H, color: C.secondary, bold: true, margin: 0,
  });
  slide.addText("市场价值", {
    x: 5.6, y: 2.45, w: 3.6, h: 0.4,
    fontSize: 18, fontFace: FONT_H, color: C.text, bold: true, margin: 0,
  });

  // Base date
  slide.addText("咨询基准日", {
    x: 5.6, y: 3.15, w: 3.6, h: 0.35,
    fontSize: 13, fontFace: FONT_H, color: C.secondary, bold: true, margin: 0,
  });
  slide.addText("2025年12月31日", {
    x: 5.6, y: 3.5, w: 3.6, h: 0.4,
    fontSize: 18, fontFace: FONT_H, color: C.text, bold: true, margin: 0,
  });

  // Consultation object
  slide.addText("咨询对象", {
    x: 5.6, y: 4.2, w: 3.6, h: 0.35,
    fontSize: 13, fontFace: FONT_H, color: C.secondary, bold: true, margin: 0,
  });
  slide.addText("国家电网宁夏电力公司各分公司物业服务费", {
    x: 5.6, y: 4.5, w: 3.6, h: 0.5,
    fontSize: 13, fontFace: FONT_H, color: C.text, margin: 0,
  });
}

// ============================================================
// SLIDE 4: 咨询范围明细
// ============================================================
{
  let slide = pres.addSlide();
  slide.background = { color: C.white };

  slide.addShape(pres.shapes.RECTANGLE, { x: 0, y: 0, w: 10, h: 0.06, fill: { color: C.primary } });
  slide.addShape(pres.shapes.RECTANGLE, { x: 0.5, y: 0.45, w: 0.06, h: 0.45, fill: { color: C.accent } });

  slide.addText("咨询范围明细", {
    x: 0.75, y: 0.35, w: 8, h: 0.65,
    fontSize: 28, fontFace: FONT_H, color: C.primary, bold: true, margin: 0,
  });
  slide.addShape(pres.shapes.RECTANGLE, { x: 0.5, y: 1.05, w: 9, h: 0.015, fill: { color: C.lightGray } });

  // Table data
  const headerOpts = { fill: { color: C.tableHeader }, color: C.white, bold: true, fontSize: 10, fontFace: FONT_H, align: "center", valign: "middle" };
  const cellOpts = (stripe) => ({ fill: { color: stripe ? C.tableStripe : C.white }, color: C.text, fontSize: 9, fontFace: FONT_H, valign: "middle", align: "center" });
  const cellLeft = (stripe) => ({ fill: { color: stripe ? C.tableStripe : C.white }, color: C.text, fontSize: 9, fontFace: FONT_H, valign: "middle", align: "left" });

  const tableData = [
    [
      { text: "序号", options: headerOpts },
      { text: "房产名称", options: headerOpts },
      { text: "产权证建筑面积\n（㎡）", options: headerOpts },
      { text: "物业服务面积\n（㎡）", options: headerOpts },
      { text: "物业服务事项", options: headerOpts },
    ],
    [
      { text: "1", options: cellOpts(false) },
      { text: "评标中心", options: cellOpts(false) },
      { text: "5813.20", options: cellOpts(false) },
      { text: "5813.20", options: cellOpts(false) },
      { text: "建筑物管理；设备设施管理；公共秩序管理；公共环境管理；消防防灾管理；办公室及住宿房间室内清洁服务；其他后勤保障服务", options: cellLeft(false) },
    ],
    [
      { text: "2", options: cellOpts(true) },
      { text: "湖映康晨1号办公楼", options: cellOpts(true) },
      { text: "7415.05", options: cellOpts(true) },
      { text: "7930.30", options: cellOpts(true) },
      { text: "建筑物管理；设备设施管理；公共秩序管理；公共环境管理；消防防灾管理；会务及接待服务；办公楼公共区域清洁服务；其他后勤保障服务", options: cellLeft(true) },
    ],
    [
      { text: "3", options: cellOpts(false) },
      { text: "湖映康晨1号办公楼院区（硬化、绿化）", options: cellOpts(false) },
      { text: "—", options: cellOpts(false) },
      { text: "7179.65", options: cellOpts(false) },
      { text: "建筑物管理；公共秩序管理；公共环境管理；交通秩序维护；绿化管理；其他后勤保障服务", options: cellLeft(false) },
    ],
    [
      { text: "4", options: cellOpts(true) },
      { text: "中心库房作业区", options: cellOpts(true) },
      { text: "—", options: cellOpts(true) },
      { text: "1050.00", options: cellOpts(true) },
      { text: "建筑物管理；设备设施管理；公共秩序管理；公共环境管理；消防防灾管理；其他后勤保障服务", options: cellLeft(true) },
    ],
    [
      { text: "5", options: cellOpts(false) },
      { text: "中心库封闭库房（1号库）、堆场", options: cellOpts(false) },
      { text: "—", options: cellOpts(false) },
      { text: "16370.88", options: cellOpts(false) },
      { text: "建筑物管理；公共环境管理；其他后勤保障服务", options: cellLeft(false) },
    ],
    [
      { text: "合计", options: { ...headerOpts, fill: { color: C.secondary } } },
      { text: "", options: { ...headerOpts, fill: { color: C.secondary } } },
      { text: "13228.25", options: { ...headerOpts, fill: { color: C.secondary }, fontSize: 11 } },
      { text: "38344.03", options: { ...headerOpts, fill: { color: C.secondary }, fontSize: 11 } },
      { text: "", options: { ...headerOpts, fill: { color: C.secondary } } },
    ],
  ];

  slide.addTable(tableData, {
    x: 0.3, y: 1.2, w: 9.4,
    colW: [0.5, 1.8, 1.2, 1.2, 4.7],
    border: { pt: 0.5, color: C.tableBorder },
    rowH: [0.45, 0.65, 0.65, 0.65, 0.55, 0.55, 0.45],
    autoPage: false,
  });

  slide.addText("产权证建筑面积合计：13,228.25㎡    物业服务面积合计：38,344.03㎡", {
    x: 0.5, y: 5.05, w: 9, h: 0.35,
    fontSize: 11, fontFace: FONT_H, color: C.secondary, bold: true, margin: 0,
  });
}

// ============================================================
// SLIDE 5: 咨询方法
// ============================================================
{
  let slide = pres.addSlide();
  slide.background = { color: C.white };

  slide.addShape(pres.shapes.RECTANGLE, { x: 0, y: 0, w: 10, h: 0.06, fill: { color: C.primary } });
  slide.addShape(pres.shapes.RECTANGLE, { x: 0.5, y: 0.45, w: 0.06, h: 0.45, fill: { color: C.accent } });

  slide.addText("咨询方法", {
    x: 0.75, y: 0.35, w: 8, h: 0.65,
    fontSize: 28, fontFace: FONT_H, color: C.primary, bold: true, margin: 0,
  });
  slide.addShape(pres.shapes.RECTANGLE, { x: 0.5, y: 1.05, w: 9, h: 0.015, fill: { color: C.lightGray } });

  // Three method cards
  const methods = [
    { name: "市场法", status: "不适用", statusColor: C.gray, reason: "同类型物业服务收费范围变化较大，服务标准和服务范围不一致" },
    { name: "收益法", status: "不适用", statusColor: C.gray, reason: "咨询对象为物业服务费，很难取得持续稳定的预期收益" },
    { name: "成本法", status: "采用 √", statusColor: "27AE60", reason: "财务制度完善、相关财务资料齐全，满足成本测算要求" },
  ];

  methods.forEach((m, i) => {
    const xx = 0.5 + i * 3.1;
    const isSelected = m.status.includes("√");

    slide.addShape(pres.shapes.RECTANGLE, {
      x: xx, y: 1.3, w: 2.9, h: 2.0,
      fill: { color: isSelected ? C.primary : C.lightBg },
      shadow: makeShadow(),
    });

    slide.addText(m.name, {
      x: xx, y: 1.4, w: 2.9, h: 0.5,
      fontSize: 18, fontFace: FONT_H, color: isSelected ? C.white : C.text, bold: true, align: "center", margin: 0,
    });
    slide.addText(m.status, {
      x: xx, y: 1.95, w: 2.9, h: 0.4,
      fontSize: 14, fontFace: FONT_H, color: isSelected ? C.accent : m.statusColor, bold: true, align: "center", margin: 0,
    });
    slide.addText(m.reason, {
      x: xx + 0.2, y: 2.4, w: 2.5, h: 0.8,
      fontSize: 10, fontFace: FONT_H, color: isSelected ? C.lightGray : C.gray, align: "center", margin: 0,
    });
  });

  // Formula and explanation card
  slide.addShape(pres.shapes.RECTANGLE, {
    x: 0.5, y: 3.55, w: 9, h: 1.75,
    fill: { color: C.lightBg }, shadow: makeShadow(),
  });

  slide.addText("成本法测算公式", {
    x: 0.8, y: 3.65, w: 8.4, h: 0.35,
    fontSize: 14, fontFace: FONT_H, color: C.primary, bold: true, margin: 0,
  });

  slide.addShape(pres.shapes.RECTANGLE, {
    x: 2.5, y: 4.1, w: 5, h: 0.5,
    fill: { color: C.primary },
  });
  slide.addText("物业服务费 = 年成本 + 利润", {
    x: 2.5, y: 4.1, w: 5, h: 0.5,
    fontSize: 16, fontFace: FONT_H, color: C.white, bold: true, align: "center", margin: 0,
  });

  slide.addText("年成本：管理费用 + 劳务人工成本 + 其他成本     |     利润率：参考上市公司物业服务净利率", {
    x: 0.8, y: 4.75, w: 8.4, h: 0.35,
    fontSize: 11, fontFace: FONT_H, color: C.text, margin: 0,
  });
}

// ============================================================
// SLIDE 6: 准备工作与资料收集
// ============================================================
{
  let slide = pres.addSlide();
  slide.background = { color: C.white };

  slide.addShape(pres.shapes.RECTANGLE, { x: 0, y: 0, w: 10, h: 0.06, fill: { color: C.primary } });
  slide.addShape(pres.shapes.RECTANGLE, { x: 0.5, y: 0.45, w: 0.06, h: 0.45, fill: { color: C.accent } });

  slide.addText("准备工作与资料收集", {
    x: 0.75, y: 0.35, w: 8, h: 0.65,
    fontSize: 28, fontFace: FONT_H, color: C.primary, bold: true, margin: 0,
  });
  slide.addShape(pres.shapes.RECTANGLE, { x: 0.5, y: 1.05, w: 9, h: 0.015, fill: { color: C.lightGray } });

  // Left: Preparatory work
  slide.addShape(pres.shapes.RECTANGLE, {
    x: 0.5, y: 1.25, w: 4.2, h: 4.0,
    fill: { color: C.lightBg }, shadow: makeShadow(),
  });
  slide.addShape(pres.shapes.RECTANGLE, { x: 0.5, y: 1.25, w: 4.2, h: 0.5, fill: { color: C.primary } });
  slide.addText("一、准备工作", {
    x: 0.5, y: 1.25, w: 4.2, h: 0.5,
    fontSize: 14, fontFace: FONT_H, color: C.white, bold: true, align: "center", margin: 0,
  });
  slide.addText([
    { text: "接受委托前与国家电网宁夏电力公司进行会谈", options: { bullet: true, breakLine: true, fontSize: 12 } },
    { text: "签署《咨询委托合同》", options: { bullet: true, breakLine: true, fontSize: 12 } },
    { text: "拟定相应工作计划", options: { bullet: true, fontSize: 12 } },
  ], {
    x: 0.8, y: 2.0, w: 3.6, h: 3.0,
    fontFace: FONT_H, color: C.text, margin: 0, valign: "top", paraSpaceAfter: 8,
  });

  // Right: Required materials
  slide.addShape(pres.shapes.RECTANGLE, {
    x: 5.3, y: 1.25, w: 4.2, h: 4.0,
    fill: { color: C.white }, shadow: makeShadow(),
    line: { color: C.tableBorder, width: 0.5 },
  });
  slide.addShape(pres.shapes.RECTANGLE, { x: 5.3, y: 1.25, w: 4.2, h: 0.5, fill: { color: C.primary } });
  slide.addText("二、需准备的资料（共12项）", {
    x: 5.3, y: 1.25, w: 4.2, h: 0.5,
    fontSize: 14, fontFace: FONT_H, color: C.white, bold: true, align: "center", margin: 0,
  });

  const materials = [
    "物业服务各岗位人员清单及职责说明",
    "房产证复印件",
    "营业执照副本复印件",
    "物业服务合同（2023-2025年）",
    "物业费咨询明细表",
    "组织架构图及岗位职责说明",
    "序时账、科目余额表、辅助明细账（三年）",
    "会计报表、账簿、会计凭证（三年）",
    "成本归集分配台账（三年）",
    "相关收入、成本合同（三年）",
    "相关人员构成、工资表（三年）",
    "其他与成本相关的资料",
  ];

  const materialItems = materials.map((m, i) => ({
    text: m,
    options: { bullet: true, breakLine: true, fontSize: 9.5, paraSpaceAfter: 3 },
  }));

  slide.addText(materialItems, {
    x: 5.6, y: 1.95, w: 3.6, h: 3.1,
    fontFace: FONT_H, color: C.text, margin: 0, valign: "top",
  });
}

// ============================================================
// SLIDE 7: 评估小组与时间安排
// ============================================================
{
  let slide = pres.addSlide();
  slide.background = { color: C.white };

  slide.addShape(pres.shapes.RECTANGLE, { x: 0, y: 0, w: 10, h: 0.06, fill: { color: C.primary } });
  slide.addShape(pres.shapes.RECTANGLE, { x: 0.5, y: 0.45, w: 0.06, h: 0.45, fill: { color: C.accent } });

  slide.addText("评估小组与时间安排", {
    x: 0.75, y: 0.35, w: 8, h: 0.65,
    fontSize: 28, fontFace: FONT_H, color: C.primary, bold: true, margin: 0,
  });
  slide.addShape(pres.shapes.RECTANGLE, { x: 0.5, y: 1.05, w: 9, h: 0.015, fill: { color: C.lightGray } });

  // Left: Team
  slide.addShape(pres.shapes.RECTANGLE, {
    x: 0.5, y: 1.3, w: 4.2, h: 3.9,
    fill: { color: C.lightBg }, shadow: makeShadow(),
  });
  slide.addShape(pres.shapes.RECTANGLE, { x: 0.5, y: 1.3, w: 4.2, h: 0.55, fill: { color: C.primary } });
  slide.addText("评估小组", {
    x: 0.5, y: 1.3, w: 4.2, h: 0.55,
    fontSize: 16, fontFace: FONT_H, color: C.white, bold: true, align: "center", margin: 0,
  });

  // Project manager
  slide.addShape(pres.shapes.RECTANGLE, {
    x: 0.8, y: 2.15, w: 3.6, h: 1.0,
    fill: { color: C.white },
    line: { color: C.secondary, width: 1 },
  });
  slide.addText([
    { text: "项目经理", options: { fontSize: 10, color: C.secondary, bold: true, breakLine: true } },
    { text: "狄寿刚", options: { fontSize: 16, color: C.primary, bold: true, breakLine: true } },
    { text: "资产评估师、高级工程师", options: { fontSize: 11, color: C.gray } },
  ], { x: 1.0, y: 2.25, w: 3.2, h: 0.85, fontFace: FONT_H, margin: 0 });

  // Team members
  slide.addText("小组成员", {
    x: 0.8, y: 3.5, w: 3.6, h: 0.3,
    fontSize: 12, fontFace: FONT_H, color: C.secondary, bold: true, margin: 0,
  });

  slide.addShape(pres.shapes.RECTANGLE, {
    x: 0.8, y: 3.85, w: 1.65, h: 0.7,
    fill: { color: C.white },
    line: { color: C.tableBorder, width: 0.5 },
  });
  slide.addText([
    { text: "周俊儒", options: { fontSize: 12, bold: true, color: C.text, breakLine: true } },
    { text: "资产评估助理", options: { fontSize: 9, color: C.gray } },
  ], { x: 0.9, y: 3.9, w: 1.45, h: 0.6, fontFace: FONT_H, align: "center", margin: 0, valign: "middle" });

  slide.addShape(pres.shapes.RECTANGLE, {
    x: 2.65, y: 3.85, w: 1.65, h: 0.7,
    fill: { color: C.white },
    line: { color: C.tableBorder, width: 0.5 },
  });
  slide.addText([
    { text: "雷亮", options: { fontSize: 12, bold: true, color: C.text, breakLine: true } },
    { text: "助理会计师", options: { fontSize: 9, color: C.gray } },
  ], { x: 2.75, y: 3.9, w: 1.45, h: 0.6, fontFace: FONT_H, align: "center", margin: 0, valign: "middle" });

  // Right: Timeline
  slide.addShape(pres.shapes.RECTANGLE, {
    x: 5.3, y: 1.3, w: 4.2, h: 3.9,
    fill: { color: C.lightBg }, shadow: makeShadow(),
  });
  slide.addShape(pres.shapes.RECTANGLE, { x: 5.3, y: 1.3, w: 4.2, h: 0.55, fill: { color: C.primary } });
  slide.addText("时间安排", {
    x: 5.3, y: 1.3, w: 4.2, h: 0.55,
    fontSize: 16, fontFace: FONT_H, color: C.white, bold: true, align: "center", margin: 0,
  });

  const timeline = [
    { phase: "准备阶段", date: "2026年7月15日 — 25日", color: C.secondary },
    { phase: "现场工作", date: "2026年7月27日 — 8月30日", color: C.primary },
    { phase: "报告草稿", date: "2026年8月30日", color: C.accent },
  ];

  timeline.forEach((t, i) => {
    const yy = 2.2 + i * 1.0;
    // Dot
    slide.addShape(pres.shapes.OVAL, { x: 5.7, y: yy + 0.1, w: 0.22, h: 0.22, fill: { color: t.color } });
    // Line (except last)
    if (i < 2) {
      slide.addShape(pres.shapes.RECTANGLE, { x: 5.79, y: yy + 0.35, w: 0.04, h: 0.55, fill: { color: C.tableBorder } });
    }
    // Text
    slide.addText(t.phase, {
      x: 6.1, y: yy, w: 3, h: 0.3,
      fontSize: 14, fontFace: FONT_H, color: C.text, bold: true, margin: 0,
    });
    slide.addText(t.date, {
      x: 6.1, y: yy + 0.3, w: 3, h: 0.25,
      fontSize: 11, fontFace: FONT_H, color: C.gray, margin: 0,
    });
  });
}

// ============================================================
// SLIDE 8: 结尾页
// ============================================================
{
  let slide = pres.addSlide();
  slide.background = { color: C.darkBg };

  slide.addShape(pres.shapes.RECTANGLE, { x: 0, y: 0, w: 10, h: 0.06, fill: { color: C.accent } });
  slide.addShape(pres.shapes.RECTANGLE, { x: 0, y: 5.565, w: 10, h: 0.06, fill: { color: C.accent } });

  slide.addText("感谢聆听", {
    x: 0, y: 1.5, w: 10, h: 1.0,
    fontSize: 42, fontFace: FONT_H, color: C.white, bold: true, align: "center", margin: 0,
  });

  slide.addShape(pres.shapes.RECTANGLE, { x: 3.5, y: 2.65, w: 3, h: 0.03, fill: { color: C.accent } });

  slide.addText("北京中兴天安资产评估有限公司", {
    x: 0, y: 3.0, w: 10, h: 0.6,
    fontSize: 20, fontFace: FONT_H, color: C.white, align: "center", margin: 0,
  });

  slide.addText("2026年7月27日", {
    x: 0, y: 3.6, w: 10, h: 0.5,
    fontSize: 14, fontFace: FONT_H, color: C.gray, align: "center", margin: 0,
  });
}

// ============================================================
// Save
// ============================================================
const outputPath = "C:\\Users\\狄珂\\Desktop\\电网宁夏物业费用测算咨询项目_工作方案.pptx";
pres.writeFile({ fileName: outputPath })
  .then(() => {
    console.log("PPT saved to: " + outputPath);
  })
  .catch(err => {
    console.error("Error:", err);
  });
