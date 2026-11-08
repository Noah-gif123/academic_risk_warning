/**
 * P2-0c 知识库语料补齐（v3.0）：为缺失课程生成确定性课程资料，写入 ai_kb_doc/ai_kb_chunk。
 *
 * 背景：v3 初始 KB 为 29 篇 / 214 chunk，只覆盖课程 1/4/7/8/10；v3 有 9 门课。
 * 本脚本为课程 5/6/9/11 各生成 8 篇（大纲/6 讲/习题），约 32 篇、100+ chunk，
 * 语料覆盖达到 9/9 门课、60+ 篇（P2R-3 再扩到 100–150 篇）。
 *
 * 输出：v3.0/sql/seeds/v3_kb_seed.sql（幂等：先删 id>=9001/900001 的本脚本数据）。
 * 运行：node v3.0/tool/gen_kb_seed.js
 */
const fs = require('fs');
const path = require('path');
const crypto = require('crypto');

const V3 = path.join(__dirname, '..');
const OUT = path.join(V3, 'sql', 'seeds', 'v3_kb_seed.sql');

const COURSES = {
  5: { name: '大学物理（二）', topics: [
    ['静电场与电势', '库仑定律、电场强度、电势与电势差、高斯定理的适用条件。'],
    ['恒定磁场', '毕奥-萨伐尔定律、安培环路定理、洛伦兹力、磁介质。'],
    ['电磁感应', '法拉第电磁感应定律、楞次定律、动生电动势与感生电动势。'],
    ['波动光学', '杨氏双缝干涉、薄膜干涉、单缝衍射、光栅衍射。'],
    ['量子物理基础', '黑体辐射、光电效应、德布罗意波、不确定关系。'],
    ['热学与统计初步', '理想气体状态方程、热力学第一定律、熵与第二定律。']
  ] },
  6: { name: '程序设计基础', topics: [
    ['变量与数据类型', '基本类型、类型转换、常量与作用域、命名规范。'],
    ['控制结构', '顺序、选择（if/switch）、循环（for/while）、break/continue。'],
    ['函数与递归', '函数声明与定义、参数传递、返回值、递归与栈。'],
    ['数组与字符串', '一维/二维数组、遍历、排序查找、字符串常用操作。'],
    ['指针与内存', '地址与指针、指针运算、动态内存、常见越界错误。'],
    ['文件与调试', '文件读写、错误处理、断点调试、单元测试入门。']
  ] },
  9: { name: '数据库原理', topics: [
    ['关系模型', '关系、元组、属性、主键与外键、关系代数基本运算。'],
    ['SQL 基础', 'DDL/DML/DQL、连接查询、聚合与分组、子查询。'],
    ['函数依赖与范式', '函数依赖、候选键、1NF/2NF/3NF/BCNF 的判定。'],
    ['事务与并发', 'ACID、事务隔离级别、脏读/幻读、锁与 MVCC。'],
    ['索引与查询优化', 'B+ 树索引、复合索引、最左前缀、执行计划。'],
    ['数据库设计', 'E-R 模型、逻辑设计、物理设计、反范式权衡。']
  ] },
  11: { name: '线性代数', topics: [
    ['行列式', '行列式定义、性质、按行展开、克拉默法则。'],
    ['矩阵运算', '矩阵加法乘法、转置、逆矩阵、分块矩阵。'],
    ['向量组与秩', '线性相关与无关、极大无关组、向量组的秩。'],
    ['线性方程组', '齐次与非齐次方程组的解结构、基础解系。'],
    ['特征值与特征向量', '特征多项式、相似对角化、实对称矩阵。'],
    ['二次型', '标准形、配方法、正定二次型与判定。']
  ] }
};

let docId = 9001;
let chunkId = 900001;
const docs = [];
const chunks = [];

function addDoc(courseId, docName, sections) {
  const id = docId++;
  const content = sections.map((s) => s.trim()).join('\n\n');
  const hash = crypto.createHash('sha1').update(content, 'utf8').digest('hex').slice(0, 32);
  docs.push({
    id, course_id: courseId, offering_id: null, source_type: 'RESOURCE', source_id: id,
    doc_name: docName, content_hash: hash, char_count: content.length, chunk_count: sections.length,
    status: 'READY', error_message: null,
    create_time: '2026-10-20 10:00:00', update_time: '2026-10-20 10:00:00'
  });
  sections.forEach((section, i) => {
    chunks.push({
      id: chunkId++, course_id: courseId, offering_id: null, teaching_class_id: null,
      source_type: 'RESOURCE', source_id: id, doc_name: docName, section_no: i + 1,
      content: section.trim(), char_len: section.trim().length,
      create_time: '2026-10-20 10:00:00'
    });
  });
}

Object.entries(COURSES).forEach(([cid, info]) => {
  const courseId = Number(cid);
  // 1) 课程大纲
  addDoc(courseId, `${info.name} · 课程大纲.md`, [
    `${info.name}课程大纲\n本课程为计算机类专业基础课，共 48 学时（理论 32 + 实验/习题 16），3 学分。课程目标：掌握核心概念、能完成典型计算与证明，并具备工程应用意识。`,
    '考核方式\n平时成绩占 40%（作业、随堂测验、课堂表现），期末考试占 60%；及格线 60 分。缺课超过总学时 1/3 直接记不合格。',
    '参考教材与资源\n主教材 + 课程讲义 + 习题集；实验/习题课按周安排，答疑在课程群与课程网站进行。'
  ]);
  // 2) 六讲（每讲 3 节）
  info.topics.forEach(([topic, summary], i) => {
    addDoc(courseId, `${info.name} · 第 ${i + 1} 讲 ${topic}.md`, [
      `第 ${i + 1} 讲 ${topic}\n本讲目标：理解${topic}的基本概念与适用条件，能完成与${topic}相关的典型题目。`,
      `核心内容\n${summary}\n关键结论：先判断条件是否满足，再选择公式或方法；计算类题目要写出依据与步骤。`,
      `典型例题\n围绕${topic}给出 2 道例题：一道概念辨析，一道计算/证明。解题步骤：审题 → 选方法 → 计算 → 检验。`,
      `易错点与作业\n常见错误：忽略条件、符号错误、步骤缺失。作业：教材第 ${i + 1} 章习题 1、3、5；下次课抽查。`
    ]);
  });
  // 3) 习题课
  addDoc(courseId, `${info.name} · 习题课与复习.md`, [
    `${info.name}习题课\n覆盖各讲重点题型，强调“条件—方法—步骤—检验”的解题范式。`,
    '复习提纲\n按讲列出必会公式/结论；把易错点整理成清单；做一遍综合练习并对照参考答案复盘。'
  ]);
});

const esc = (v) => {
  if (v === null || v === undefined) return 'NULL';
  if (typeof v === 'number') return String(v);
  return "'" + String(v).replace(/\\/g, '\\\\').replace(/'/g, "\\'") + "'";
};
const sql = [];
sql.push('-- P2-0c KB 语料补齐（由 v3.0/tool/gen_kb_seed.js 生成，幂等）');
sql.push('DELETE FROM ai_kb_chunk WHERE id >= 900001;');
sql.push('DELETE FROM ai_kb_doc WHERE id >= 9001;');
function insert(table, cols, rows) {
  if (!rows.length) return;
  sql.push(`INSERT INTO ${table} (${cols.join(', ')}) VALUES`);
  sql.push(rows.map((r) => '(' + cols.map((c) => esc(r[c])).join(', ') + ')').join(',\n') + ';');
}
insert('ai_kb_doc', ['id', 'course_id', 'offering_id', 'source_type', 'source_id', 'doc_name',
  'content_hash', 'char_count', 'chunk_count', 'status', 'error_message', 'create_time', 'update_time'], docs);
insert('ai_kb_chunk', ['id', 'course_id', 'offering_id', 'teaching_class_id', 'source_type', 'source_id',
  'doc_name', 'section_no', 'content', 'char_len', 'create_time'], chunks);
fs.writeFileSync(OUT, sql.join('\n') + '\n', 'utf8');
console.log(`KB seed: ${docs.length} docs / ${chunks.length} chunks -> ${OUT}`);
COURSES && Object.keys(COURSES).forEach((c) => {
  const n = docs.filter((d) => d.course_id === Number(c)).length;
  console.log(`  course ${c}: ${n} docs`);
});
