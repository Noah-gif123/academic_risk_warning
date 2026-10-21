/* gen_ch4.js — 生成 proj_doc_ch4.json（《项目文档》第 4 章 数据设计 · 只写学情预警子系统）
 * 排版严格对照「2026 届论文模板 3.x 数据库设计 + 项目文档模板第 4 章」：
 *   数据字典（标识符/同义名）→ 概念结构设计（一张 ER 图）→ 表的详细设计（逐表：用途 + 主要字段 + 表结构）
 * 数据源：v2.0/tool/columns.tsv（线上库 information_schema 导出）
 * 用法: node gen_ch4.js
 */
'use strict';
const fs = require('fs');
const path = require('path');

const here = __dirname;
const lines = fs.readFileSync(path.join(here, 'columns.tsv'), 'utf8').split(/\r?\n/).filter(l => l !== '');
const tables = new Map();
for (const l of lines) {
  const c = l.split('\t');
  if (!tables.has(c[0])) tables.set(c[0], { name: c[0], comment: c[9], cols: [] });
  tables.get(c[0]).cols.push({ ord: Number(c[1]), name: c[2], type: c[4], nullable: c[5], key: c[6], comment: c[8] });
}
const T = n => { const t = tables.get(n); if (!t) throw new Error('missing table ' + n); return t; };

const COMMON_DESC = {
  id: '主键', create_time: '创建时间', update_time: '更新时间', create_by: '创建人ID',
  update_by: '更新人ID', remark: '备注', is_void: '是否作废', void_reason: '作废原因'
};
const KEY_LABEL = { PRI: 'PK', UNI: 'UK', MUL: 'FK', '': '' };

const B = [];
const h1 = x => B.push({ t: 'h1', x });
const h2 = x => B.push({ t: 'h2', x });
const h3 = x => B.push({ t: 'h3', x });
const p = x => B.push({ t: 'p', x });
const note = x => B.push({ t: 'note', x });
const img = (file, cap, maxW) => B.push({ t: 'img', file, cap, maxW });
const table = (cap, widths, head, rows) => B.push({ t: 'table', cap, widths, head, rows });

// ================= 章首 =================
h1('4. 数据设计');
p('一次风险判定需要哪些表、按什么顺序被读写，是本章要回答的全部问题。系统整体由教学数据与活动子系统、学情预警子系统两部分组成：前者采集课程、课次、考勤、得分与提交等过程数据；后者消费这些数据，产出风险结论并推动处置。本章按论文模板给定的顺序，**只对学情预警子系统**做完整的数据设计——先给数据字典，再给概念模型（ER 图），最后逐表给出表结构。');
note('范围约定：教学数据与活动子系统的表（course、course_offering、teaching_class、enrollment、course_session、attendance_record、score_item、score_result 等）不在本章重复设计，学情预警子系统只通过 enrollment_id、student_id、teaching_class_id、term_id 四个外键读取它们，不写不改。表标识符即线上库的真实表名（本项目不加 t_ 前缀）。');

// ================= 4.1 =================
h2('4.1 设计范围与总体思路');
p('学情预警子系统共 13 张表，按“输入与校准 → 判定状态 → 事件与处置”三组排列。**输入与校准组** 6 张：引擎配置（阈值与系数）、教师尺度（评分松紧 Δ）、学生历史（跨学期挂科与总评）、课程挂科率、教学班挂科率、行政班挂科率（三个维度的历史基准）。**判定状态组** 1 张：风险状态，一人一课一行保存当前结论。**事件与处置组** 6 张：预警记录、预警时间线、干预记录、通知、通知回执、班集体提醒。读的顺序就是分组的顺序，写的顺序相反：先落风险状态，再由状态生成预警事件，事件推动干预与通知。');
p('两种承载方式决定了整套表的分工：**状态覆盖、事件追加**。risk_state 保存“当前风险”，每次刷新覆盖本行，因此查当前风险只需一次唯一键命中；alert_record 保存“发生过什么”，只增不改，因此追溯某次预警的来龙去脉只需按 enrollment_id 走事件链。这也是本子系统不按天写风险快照的原因——历史结论由预警时间线承载，不必每天存一份全量曲线。');
p('四类粒度贯穿全表：一人一课（risk_state、alert_record）、一次状态变化（alert_flow_log）、一次处置（intervention_record）、一次投递（notification_receipt）。班集体提醒的粒度是“行政班 × 学期 × 档位”，它不针对某个学生，只解决“不要天天提醒同一个班”的问题。');

// ================= 4.2 数据字典 =================
h2('4.2 数据字典');
p('按模板要求，先说明各表与关键数据项的标识符、同义名及含义。同义名列给出该表在需求文档、界面文案与代码中出现的其他叫法，用于全文对照；数据项字典只列判定链上的关键项，普通字段在各表结构表内说明。');

h3('4.2.1 表标识符与同义名');
table('学情预警子系统的表标识符与同义名', [18, 26, 24, 32], ['中文名', '标识符（表名）', '同义名', '记录粒度'], [
  ['引擎配置', 'risk_engine_config', '判定参数表 / 阈值表', '一行一个引擎版本'],
  ['教师尺度', 'teacher_style', 'Δ 表 / 评分松紧表', '一行一位教师 × 开课 × 评分项'],
  ['学生历史', 'student_fail_history', '挂科历史 / 学业档案', '一行一个学生 × 开课'],
  ['课程挂科率', 'course_fail_rate_history', '课程因子表 / c 值表', '一行一门课程 × 学期'],
  ['教学班挂科率', 'teaching_class_fail_rate', '班内基准 / 班挂科率', '一行一个教学班 × 学期'],
  ['行政班挂科率', 'admin_class_fail_rate', '班级挂科率（导员视图）', '一行一个行政班 × 开课'],
  ['风险状态', 'risk_state', '当前风险 / 判定结果表', '一行一次选课（一人一课一行）'],
  ['预警记录', 'alert_record', '预警事件 / 预警单', '一行一条预警事件'],
  ['预警时间线', 'alert_flow_log', '预警流水 / 生命周期记录', '一行一次状态变化'],
  ['干预记录', 'intervention_record', '处置记录 / 跟进记录', '一行一次干预动作'],
  ['通知', 'notification', '消息主体 / 提醒', '一行一条聚合后的通知'],
  ['通知回执', 'notification_receipt', '投递记录 / 已读回执', '一行一个接收人 × 渠道'],
  ['班集体提醒', 'class_alert_notification', '班级提醒去重表', '一行一个行政班 × 学期 × 档位']
]);

h3('4.2.2 关键数据项字典');
table('关键数据项字典', [16, 22, 14, 20, 28], ['数据项', '标识符', '类型', '取值 / 单位', '说明'], [
  ['引擎版本', 'engine_version', 'varchar(20)', '如 V2026.1', '阈值、公式或权重变化即升版，历史版本只读、可复现'],
  ['生效版本标记', 'is_active', 'tinyint(1)', '0 / 1', '全库仅一条为 1，由服务端在切换版本时维护'],
  ['允许缺课上限', 'allowed_absent', 'int', '次', 'N_max = floor(已完成课次 / 3)，快照写入风险状态'],
  ['剩余可缺次数', 'remaining_absent', 'int', '次', 'N_max − 已缺课；0 表示再缺一次即挂，≤ −1 表示必然挂科'],
  ['期末需考分', 'final_exam_needed', 'decimal(6,1)', '分', 'x = (及格线 + P − 其余项加权) / 期末权重'],
  ['A 通道等级', 'channel_a_level', 'varchar(20)', 'SAFE / WATCH / DANGER / FAIL', '确定性判定，不做概率估计'],
  ['班内风险分', 'channel_b_score', 'decimal(5,2)', '0–100', '班内相对位置，越高越危险'],
  ['班内排名', 'channel_b_rank', 'int', '名次', '判定当次快照，1 为最危险'],
  ['跨班折算分', 'channel_c_score', 'decimal(6,2)', '分', '影子分数，经 Δ 与课程因子折算，不写成绩表'],
  ['教师尺度', 'delta_used', 'decimal(5,2)', '分', '本次判定使用的收缩后 Δ̂'],
  ['课程因子', 'course_factor_used', 'decimal(5,3)', '无量纲', '由课程历史挂科率映射而来'],
  ['风险分', 'risk_score', 'decimal(5,1)', '0–100', '对外分数：有 C 用 C、无 C 用 B，A 单列展示'],
  ['风险等级', 'risk_level', 'varchar(10)', 'RED / ORANGE / YELLOW / GREEN', '取三通道最高档'],
  ['置信度', 'confidence', 'decimal(4,3)', '0–1', '锚点强度 × 数据完备度 × 样本充足度'],
  ['输出形态', 'output_mode', 'varchar(20)', 'FULL_SCORE / SCORE_WITH_NOTE / BEHAVIOR_ONLY / INSUFFICIENT', '数据不足时只给行为预警，不给分数'],
  ['触发规则', 'trigger_rule', 'varchar(50)', 'A_HARD_LINE / A_NEAR_LINE / A_UNREACHABLE / B_RANK / C_THRESHOLD', '用于解释“为什么触发这条预警”'],
  ['当前预警标记', 'is_current', 'tinyint(1)', '1 / NULL', '与唯一键配合，保证一人一课只有一条当前预警'],
  ['聚合键', 'aggregate_key', 'varchar(60)', '如 CLASS_ALERT:12:5:8', '同一聚合键只发一条汇总通知'],
  ['处置阶段', 'stage', 'varchar(30)', 'TEACHER / COUNSELOR_1 / COUNSELOR_2 / REFERRED / ADMIN_TALK / ADMIN_HELP / TERMINATED', '教师、导员、教务三级干预链'],
  ['事件类型', 'event_type', 'varchar(30)', 'CREATED / LEVEL_UP / NOTIFIED / HANDLED / CLOSED / EXPIRED 等 14 类', '预警时间线的事件口径']
]);

// ================= 4.3 =================
h2('4.3 概念结构设计');
p('由 4.2 的 13 张表抽象出实体与联系，得到学情预警子系统的概念模型：**六张校准表共同约束一个风险状态，风险状态向下派生预警事件，预警事件再派生时间线、干预与通知，通知最后派生回执**。联系以 1:N 为主：一个引擎版本可以判定多条风险状态；一位教师在一个开课上的尺度、一门课程的历史挂科率可以被多次判定复用；一条预警可以多次流转（时间线）、可以被多次干预，也可以多次催办（通知）。风险状态与预警记录之间是 1:N——一次状态可以触发多条预警，但只有一条处于“当前”态。');
p('读图约定：方框为实体，首行为实体中文名与表名，id* 为关键字，其余为该实体的主要属性；连线两端标注 1 与 N；左下角便签注明外部锚点（enrollment、student、teaching_class 由教学数据与活动子系统提供，本子系统只读引用）。图只表达逻辑外键，库内不建物理外键约束，一致性由服务层事务与 4.5 的约束清单保证。');
img('out/230_warning_subsystem.png', '学情预警子系统概念模型（ER 图）', 600);
p('概念模型的主干是“校准 → 判定 → 预警 → 处置”。左上六张校准表固定在判定之前：引擎配置决定阈值与公式，教师尺度与课程因子解决“不同老师、不同课程怎么比”，学生历史与两张班级挂科率提供基准与样本量。中间的风险状态是唯一的“当前结论”实体，所有校准结果都收敛到这里。下半部分是一条事件链：预警记录作为聚合根，向下挂时间线（发生了什么）、干预（谁做了什么）、通知（通知了谁），通知再挂回执（是否送达与确认）。班集体提醒单独接在行政班挂科率之后——它只处理“整班预警”，与个体预警是两条并行线。');

// ================= 4.4 =================
h2('4.4 表的详细设计');
p('通过对领域对象、业务对象及对象间关联的分析，对本子系统的数据库表进行了设计，如表4-3 至表4-15 所示。各表主键统一为 id（bigint 自增），其余均为逻辑外键、不建物理外键约束；“键”列中 PK 表示主键、UK 表示唯一键、FK 表示外键索引。字段说明采用库内注释原文，保证与实现一致。');

const SUBSYS = [
  ['引擎配置表', 'risk_engine_config',
    '引擎配置表主要用于保存风险判定所用的阈值与系数，使判定口径可版本化、可复现。主要字段包括：引擎版本与生效时间、是否当前生效版本、A/B/C 三通道开关、个体信号权重（signal_weights_json）、红橙黄三档分档线、点名覆盖率阈值与软提醒周数、导员班集体提醒的起始周与两档阈值、多科目预警门限、干预无效判定天数、三项扣分系数与请假免扣次数、期末满分、教师尺度 Δ 的收缩常数与最小样本量与熔断阈值、置信度下限。'],
  ['教师尺度表', 'teacher_style',
    '教师尺度表主要用于保存每位教师在某门课某个评分项上的评分松紧程度 Δ，即该班与同课程平均的偏差，是跨班可比判定的校准依据。主要字段包括：教师、开课、课程、学期与评分项编码、未收缩原始 Δ、收缩后 Δ̂、收缩系数 λ、样本量与标准差、是否熔断与熔断原因、复核人与复核时间、状态、引擎版本与估计时间。'],
  ['学生历史表', 'student_fail_history',
    '学生历史表主要用于保存学生跨学期的成绩与挂科记录，供锚点强度与样本充足度估计使用。主要字段包括：学生、学期、课程与开课、最终总评与当次及格线、是否挂科、是否因三分之一硬门槛挂科、是否重修记录、成绩状态、引擎版本与归档时间。'],
  ['课程挂科率表', 'course_fail_rate_history',
    '课程挂科率表主要用于按“课程 × 学期”保存历史挂科率并映射为课程因子 c，供跨班可比判定做课程难易归一。主要字段包括：课程与学期、实际考核人数与挂科人数、挂科率、硬门槛挂科人数、本学期教学班数、是否为手工初始化值、课程因子 c、引擎版本与计算时间。'],
  ['教学班挂科率表', 'teaching_class_fail_rate',
    '教学班挂科率表主要用于给班内相对风险提供基准，同时构成教师端与教务端的班情视图。主要字段包括：教学班、开课、任课教师与学期、实际考核人数、挂科人数与挂科率、硬门槛挂科人数、是否重修班、引擎版本与计算时间。'],
  ['行政班挂科率表', 'admin_class_fail_rate',
    '行政班挂科率表主要用于导员端按行政班查看挂科情况，也是班集体提醒的人数与占比来源。主要字段包括：行政班、开课、课程与学期、实际考核人数、挂科人数与挂科率、硬门槛挂科人数、引擎版本与计算时间。'],
  ['风险状态表', 'risk_state',
    '风险状态表主要用于保存一人一课的当前判定结论，是本子系统的核心结果表。主要字段包括：选课、学生、开课、教学班与学期；A 通道的缺课、迟到、请假次数与允许缺课上限、剩余可缺次数、是否触发硬门槛、期末需考分与是否可达、通道等级；B 通道的班内风险分、班内排名与班级人数；C 通道的跨班折算分、使用的 Δ̂ 与课程因子、通道等级；以及对外风险分、风险等级、生效通道、置信度与置信档、输出形态、数据质量明细、刷新来源与时间、引擎版本、是否作废。'],
  ['预警记录表', 'alert_record',
    '预警记录表主要用于保存每一条预警事件，与风险状态分离：状态覆盖、事件累积。主要字段包括：学生、选课、开课、教学班、课程与学期、预警等级、触发通道与触发规则、触发时的风险分与置信度与输出形态、预警原因、触发依据快照与三通道快照、状态、是否当前预警、是否升级与升级前等级、是否已通知与首次通知时间、超时时间、处理人与角色与说明、关闭人与结论、忽略原因、学生回应、引擎版本与是否作废。'],
  ['预警时间线表', 'alert_flow_log',
    '预警时间线表主要用于记录预警的每一次状态变化，是预警详情页时间线与审计的数据源。主要字段包括：预警、学生与学期、事件类型、变更前等级与变更后等级、变更前状态与变更后状态、事件说明、操作人与操作角色、事件时间。'],
  ['干预记录表', 'intervention_record',
    '干预记录表主要用于记录教师、导员、教务三级的处置动作与成效判定。主要字段包括：学生、学期、关联预警（可为空，也可一次覆盖多条预警）、阶段、处理人与角色、动作类型、处理内容与结果、是否有成效与成效检查时间、干预前后风险分及其变化、上报状态与上报人/时间/说明、教务反馈、是否作废。'],
  ['通知表', 'notification',
    '通知表主要用于保存通知主体，按聚合键去重，保证同一件事只发一条汇总。主要字段包括：业务类型与业务对象、标题与正文、结构化载荷、聚合键与聚合条数、接收范围描述、优先级、过期时间。'],
  ['通知回执表', 'notification_receipt',
    '通知回执表主要用于按“接收人 × 渠道”记录投递与确认情况。主要字段包括：通知、接收人与接收角色、渠道、发送状态与发送时间、是否已读与已读时间、是否需要回执、回执状态与确认时间与说明、已采取的行动。'],
  ['班集体提醒表', 'class_alert_notification',
    '班集体提醒表主要用于防止天天提醒同一个班，按“行政班 × 学期 × 档位”唯一。主要字段包括：行政班、学期、主责导员、档位、触发周次、班级学生数与预警学生数及占比、本次使用的风险分下限、阈值快照、纳入提醒的学生列表、生成的汇总通知、是否抄送教务、是否已发送与发送时间。']
];

const usedNums = [];
let seq = 2; // 表4-1、表4-2 已用
SUBSYS.forEach(([cn, name, prose], i) => {
  seq += 1;
  const num = seq;
  usedNums.push({ num, name });
  p('**（' + (i + 1) + '）' + cn + '**');
  p(prose + '表结构如表4-' + num + '。');
  const t = T(name);
  table(cn + '（' + name + '）', [19, 17, 6, 58], ['字段名', '类型', '键', '说明'],
    t.cols.map(c => [c.name, c.type, KEY_LABEL[c.key] === undefined ? '' : KEY_LABEL[c.key],
      c.comment || COMMON_DESC[c.name] || '—']));
});

// ================= 4.5 =================
h2('4.5 完整性与一致性约束');
p('约束分三层：库级（主键、唯一键、NOT NULL、默认值）、事务级（同一事务内的跨表校验与派生字段计算）、口径级（同一事实只允许一种算法）。下表给出本子系统的关键约束与违反时的处理方式。');
table('关键完整性约束', [24, 22, 34, 20], ['约束 / 索引', '所在表', '规则', '违反时的处理'], [
  ['uk_engine_version', 'risk_engine_config', '引擎版本号唯一；历史版本只读', '重复版本返回 400'],
  ['is_active 唯一为 1', 'risk_engine_config', '应用级：全库只能有一条生效版本', '切换版本时同一事务内先撤旧再置新'],
  ['uk_teacher_offering_item', 'teacher_style', '同一教师 × 开课 × 评分项只有一条尺度记录', '覆盖写并更新样本量与时间'],
  ['uk_student_offering', 'student_fail_history', '同一学生 × 开课只有一条历史记录', '覆盖写'],
  ['uk_course_term', 'course_fail_rate_history', '同一课程 × 学期只有一条挂科率', '重算覆盖'],
  ['uk_class_term', 'teaching_class_fail_rate', '同一教学班 × 学期只有一条挂科率', '重算覆盖'],
  ['uk_class_offering', 'admin_class_fail_rate', '同一行政班 × 开课只有一条挂科率', '重算覆盖'],
  ['uk_enrollment（risk_state）', 'risk_state', '一人一课一行“当前风险”', '刷新即覆盖，历史由时间线承载'],
  ['uk_enroll_current', 'alert_record', '(enrollment_id, is_current) 唯一：当前预警只有一条', '生成新预警前先把旧预警 is_current 置 NULL'],
  ['uk_aggregate_key', 'notification', '聚合键唯一，同班同周同规则只发一条', '重复生成时静默丢弃'],
  ['uk_notify_recipient_channel', 'notification_receipt', '同一通知 × 接收人 × 渠道只有一行', '重发时更新发送状态与时间'],
  ['uk_class_term_level', 'class_alert_notification', '同一行政班 × 学期 × 档位只有一条提醒', '同档不重复提醒，升级档另起一行'],
  ['剩余可缺 = 上限 − 已缺', 'risk_state', '应用级：remaining_absent 由 allowed_absent 与缺课次数派生', '写入时校验，不一致直接拒绝'],
  ['证据快照非空', 'alert_record', '应用级：有等级就必须有 evidence_json', '缺证据的预警不允许生成'],
  ['成效检查时间', 'intervention_record', '应用级：effect_check_time = min(下次风险刷新, 干预后 invalid_check_days)', '定时任务与刷新流程共同维护']
]);
p('**索引设计。** 索引只按页面真实会发的查询建：预警列表按“学期 + 等级”或“教学班 + 状态”过滤，待办列表按“状态 + 超时时间”翻页，学生详情按“学生 + 是否作废”取当前行，干预流水按“学生 + 时间”倒序取最近几条。下表列出本子系统的主要索引。');
table('主要索引与所服务的查询', [26, 24, 26, 24], ['索引', '表', '列', '服务的查询'], [
  ['idx_term_level', 'risk_state', 'term_id, risk_level', '按学期 + 等级筛风险学生'],
  ['idx_class_level', 'risk_state', 'teaching_class_id, risk_level', '教师端按教学班看风险名单'],
  ['idx_offering_level', 'risk_state', 'offering_id, risk_level', '教务端按开课看风险分布'],
  ['idx_student_void', 'risk_state', 'student_id, is_void', '学生详情页取当前有效判定'],
  ['idx_eval_time', 'risk_state', 'last_eval_time', '定时判定与增量刷新扫描'],
  ['idx_teacher_query', 'alert_record', 'teaching_class_id, status', '教师端待处理预警'],
  ['idx_status_expire', 'alert_record', 'status, expire_time', '超时预警扫描与自动升级'],
  ['idx_student_status', 'alert_record', 'student_id, status, is_void', '学生维度的预警列表'],
  ['uk_enroll_current', 'alert_record', 'enrollment_id, is_current', '一人一课当前预警定位'],
  ['idx_term_level（alert）', 'alert_record', 'term_id, alert_level', '按学期 + 等级统计预警量'],
  ['idx_student_time', 'intervention_record', 'student_id, create_time', '干预时间线与成效判定'],
  ['idx_effect', 'intervention_record', 'effective, effect_check_time', '待检查成效的干预记录'],
  ['idx_biz', 'notification', 'biz_type, biz_id', '按业务对象反查通知'],
  ['uk_aggregate_key', 'notification', 'aggregate_key', '生成前判重']
]);
note('alert_flow_log 只按 alert_id 查询、单条预警的事件数在个位到十位，因此只保留主键，不单独建索引；这是有意为之，避免为小表付维护成本。');

// ================= 4.6 =================
h2('4.6 数据安全与运维');
p('**结论不可篡改。** 风险状态与预警记录一经生成即视为“当时的结论”：需要更正时只能作废（is_void + void_reason）或新建预警，不允许直接改动等级与风险分；作废行不进入任何统计与列表，但仍留在库中，保证历史可回溯。引擎版本随每条结论落库，任何一次重算都能还原当时用的阈值与公式。');
p('**证据与留痕。** 每一条预警都带 evidence_json（缺课次数、允许上限、剩余可缺、期末需考分、覆盖率等）与 channel_snapshot_json（三通道当时的取值），页面上的“为什么预警”直接读这两列，不做事后重算；预警的每一次状态变化写 alert_flow_log，干预的每一次动作写 intervention_record，配置与版本的每一次调整由审计日志记录。留痕表只追加，服务层不提供修改与删除接口。');
p('**数据范围与权限。** 可见范围全部在服务端收窄：学生只能看到与自己 enrollment_id 绑定的风险状态与预警；教师只能看到自己教学班内的风险名单与预警；导员只能看到名下行政班（经 counselor_class 关联）的学生与班集体提醒；教务按学期与开课范围查看全量。客户端不传“可见范围”参数，改参无效。');
p('**当前数据状态说明。** 第一阶段这两张核心表（risk_state、alert_record）在演示库中为空：判定在服务层按请求实时完成，结论不落库，教师端与导员端的预警列表由成绩单与考勤实时计算。表结构已按第二阶段的写入契约一次建好，接入持久化时不需要改表，只需在判定流程末尾按 4.4 的字段清单写入即可。');
note('容量与备份：本子系统数据量随“学生 × 课程”线性增长，演示库的规模是 4 587 条选课对应 4 587 行风险状态与同量级预警；备份按每日全量 + binlog 增量，可恢复到任意时间点。引擎配置与教师尺度属于“口径资产”，单独导出留档，避免误改后无法复现历史判定。');

fs.writeFileSync(path.join(here, 'proj_doc_ch4.json'), JSON.stringify({ chapter: 4, blocks: B }, null, 1), 'utf8');
console.log('WROTE proj_doc_ch4.json blocks=' + B.length);
const kinds = {};
B.forEach(b => { kinds[b.t] = (kinds[b.t] || 0) + 1; });
console.log(JSON.stringify(kinds));
console.log('表结构编号：' + usedNums.map(u => '4-' + u.num + ' ' + u.name).join(' | '));
