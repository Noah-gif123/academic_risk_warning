/* patch_ch3.js — 用新的教师端章节替换 feature_content.json 中的第3章，并补充待确认项 */
'use strict';
const fs = require('fs'), path = require('path');
const gen = __dirname;
const file = path.join(gen, 'feature_content.json');
const doc = JSON.parse(fs.readFileSync(file, 'utf8'));
const ch3 = JSON.parse(fs.readFileSync(path.join(gen, 'chapter3_teacher.json'), 'utf8'));

const blocks = doc.blocks;
const i3 = blocks.findIndex(b => b.t === 'h1' && b.x.indexOf('第3章') === 0);
const i4 = blocks.findIndex(b => b.t === 'h1' && b.x.indexOf('第4章') === 0);
if (i3 < 0 || i4 < 0) throw new Error('chapter markers not found: ' + i3 + '/' + i4);
const removed = i4 - i3;
doc.blocks = blocks.slice(0, i3).concat(ch3, blocks.slice(i4));

// 更新第6章覆盖率口径（标注 1/3 与 1/2 的冲突）
const t8 = doc.blocks.find(b => b.t === 'table' && b.cap.indexOf('考勤与扣分口径') >= 0);
if (t8) {
  t8.rows.forEach(r => {
    if (r[0] === '覆盖率') {
      r[1] = '已点名课次 ÷ 应完成课次（应完成＝已过日期且未取消且未作废）';
      r[2] = '低于阈值：提醒补录 + 置信度判低。⚠ 阈值待定：教师端补充要求“考勤次数不低于课次的 1/3”，原口径为 1/2';
    }
  });
}

// 补充第8章待确认清单
const i8 = doc.blocks.findIndex(b => b.t === 'h1' && b.x.indexOf('第8章') === 0);
let olBlock = null;
for (let i = i8; i < doc.blocks.length && i < i8 + 5; i++) if (doc.blocks[i].t === 'ol') { olBlock = doc.blocks[i]; break; }
if (!olBlock) throw new Error('第8章 ol not found');
const added = [
  "考勤合规线（重要冲突）：教师端补充要求“老师考勤次数不低于课次的 1/3”，而之前锁定的是“覆盖率低于 1/2 判低置信度”。是统一改成 1/3（提醒、督办、置信度判低都用 1/3），还是 1/3 只是最低要求、1/2 仍是置信度线？",
  "目录挂在哪一级：参考平台的“教学详情（含目录）”是从某个教学班级进入的，而你此前确认“目录＝开课实例级、同课程同教师共用”。同一门课有多个教学班时，是各有一套目录，还是共用一套？",
  "教学目标考核：参考平台“课程考核”下有“教学目标考核”子页签、教学统计里有“教学目标平均达成度”卡片。要不要保留？（保留需要把教学活动与教学目标做关联，工作量不小。）",
  "课程考核页的开关：是否保留“允许学生查看考核规则”“允许学生查看考核结果”两个开关？（参考平台默认开启前者、关闭后者。）",
  "教师课程卡片上的“快捷”入口是什么？要不要保留？",
  "“我的教学项目”首页区块放什么？（参考平台为空；我建议放本人负责的项目型任务与提交/批阅进度。）",
  "课程封面图与“引用数据 / 更换图片”：要不要做图片上传与封面（参考平台每门课都有封面图）？",
  "基础设置里的“课程组设置 / 助教设置 / 生成资源包”：课程组设置与我们已有的“成绩构成由课程组确认并锁定”有关，建议保留简化版；助教设置与生成资源包是否要做？",
  "预警提醒设置：参考平台有“任务未完成预警、完成率低预警、出勤率低预警、多次缺勤预警、活跃异常预警”以及每周发送周期。移到“预警监控”后，这些提醒项要保留哪些？（注意与我们已有的风险预警不是一回事。）",
  "“资料”类任务（课件 / 预习资料）是否计入成绩？参考平台把它做成评分项（资料(5)），我们的成绩构成是否也允许把“资料”作为评分项？"
];
added.forEach(s => olBlock.x.push(s));

fs.writeFileSync(file, JSON.stringify(doc, null, 1), 'utf8');
console.log('chapter3: removed ' + removed + ' blocks, inserted ' + ch3.length);
console.log('pending items now: ' + olBlock.x.length);
console.log('total blocks: ' + doc.blocks.length);
