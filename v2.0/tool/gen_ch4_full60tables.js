/* gen_ch4.js — 生成 proj_doc_ch4.json（《项目文档》第 4 章 数据设计）
 * 数据源：v2.0/tool/columns.tsv（由线上库 information_schema 导出，已含 token 两列）
 * 用法: node gen_ch4.js
 */
'use strict';
const fs = require('fs');
const path = require('path');

const here = __dirname;
const lines = fs.readFileSync(path.join(here, 'columns.tsv'), 'utf8').split(/\r?\n/).filter(l => l !== '');

// ---------- 解析表结构 ----------
const tables = new Map();
for (const l of lines) {
  const c = l.split('\t');
  const t = c[0];
  if (!tables.has(t)) tables.set(t, { name: t, comment: c[9], cols: [] });
  tables.get(t).cols.push({
    ord: Number(c[1]), name: c[2], type: c[4], nullable: c[5], key: c[6], comment: c[8]
  });
}
const T = name => {
  const t = tables.get(name);
  if (!t) throw new Error('missing table ' + name);
  return t;
};

// ---------- 域划分 ----------
const DOMAINS = [
  ['基础与组织', ['admin', 'administrative_class', 'counselor', 'counselor_class', 'major', 'student', 'sys_account', 'system_config', 'teacher', 'term', 'term_calendar']],
  ['开课与构成', ['course', 'course_group', 'course_offering', 'score_scheme', 'score_scheme_item', 'score_template', 'teaching_class', 'teaching_class_admin_class']],
  ['选课与考勤', ['attendance_archive', 'attendance_record', 'attendance_record_log', 'course_session', 'enrollment']],
  ['成绩与活动', ['course_join_request', 'paper', 'project_contribution', 'project_submission', 'project_task', 'project_team', 'question', 'score_item', 'score_result', 'submission', 'submission_answer']],
  ['引擎产物', ['admin_class_fail_rate', 'course_fail_rate_history', 'risk_engine_config', 'risk_state', 'student_fail_history', 'teacher_style', 'teaching_class_fail_rate']],
  ['预警干预', ['alert_flow_log', 'alert_record', 'class_alert_notification', 'intervention_record', 'notification', 'notification_receipt']],
  ['流程问卷', ['audit_log', 'coverage_check_log', 'exam_headcount', 'import_batch', 'questionnaire', 'questionnaire_answer', 'questionnaire_flow']],
  ['教学资源', ['course_guide', 'course_lesson', 'lesson_resource', 'resource_file', 'teaching_objective']]
];
const domainOf = {};
DOMAINS.forEach(([d, list]) => list.forEach(n => { domainOf[n] = d; }));
const ordered = [];
DOMAINS.forEach(([d, list]) => list.slice().sort().forEach(n => ordered.push(n)));
if (ordered.length !== tables.size) throw new Error('domain count mismatch: ' + ordered.length + ' vs ' + tables.size);
for (const n of tables.keys()) if (!domainOf[n]) throw new Error('table not assigned: ' + n);

// ---------- 核心表 ----------
const CORE = ['sys_account', 'term', 'student', 'teaching_class', 'enrollment', 'course_session',
  'attendance_record', 'score_scheme_item', 'score_item', 'score_result', 'risk_state', 'alert_record'];

const CORE_TEXT = {
  sys_account: '四端共用的唯一登录表。登录号（学号/工号/管理员号）全库唯一，口令以 PBKDF2WithHmacSHA256 加盐哈希存储（120000 次迭代，存储格式 pbkdf2$迭代次数$Base64(盐)$Base64(哈希)）。role 决定登录后的落地页与导航；token 与 token_expire_time 是 2026-09 令牌落库改造新增的两列，令牌默认 12 小时有效，服务重启不再掉线，退出登录时两列置空（NULL 不参与 uk_token 的唯一性判定）。',
  term: '全库时间轴。任何跨学期统计先按 term_id 收敛，学期切换不搬数据、只改 is_current（全库唯一一条为 1，由服务层维护）。学期只归档不删除：结束后把 status 置 CLOSED，历史课次与判定仍可按学期回溯；周次、假期与考试周在 term_calendar（见表4-17）。',
  student: '学籍与组织归属。年级由 enroll_year 与当前学期推算，不落“大一上”这类学期值；专业（major_id）与行政班（admin_class_id）可变更，变更留痕在 audit_log；account_id 为迁移期可空字段，新建学生时与 sys_account 同时建档。phone / email 暂只作联系方式，不参与判定。',
  teaching_class: '授课单位：一个开课实例下可以有多个教学班，一个教学班只有一位老师（teacher_id 单值），课次、点名、成绩录入都按教学班进行。invite_code 为 6 位唯一的入班邀请码（学期内有效、次数不限），join_review_enabled 决定学生入班是否需要教师审核；is_retake_class 只用于展示与筛选，统计上不作特殊处理。',
  enrollment: '过程数据的锚点。一名学生在同一开课/同一教学班只有一条选课（uk_student_offering、uk_student_teaching_class），retake_times 区分首次修读与重修次数。offering_id 由 teaching_class 派生并在写入时校验一致，冗余它是为了让“按开课汇总覆盖率/成绩”不必多跳一次。状态取 ENROLLED（在册）/REPEAT（重修）/WITHDRAWN（已退课）/EXEMPT（免修）；退课保留过程数据但不计入任何统计。',
  course_session: '课表驱动的“一次课一行”，由开课实例的计划课次生成，uk_class_session_no 保证教学班内课次序号唯一（点名时按序号定位）。status 标记停课/调课/合并，这些课次与 is_void 的课次都不进 1/3 门槛的分母；roll_call_status 与 roll_call_count 是点名覆盖率的直接依据，也是教务督办的教学班清单来源。',
  attendance_record: '一名学生一次课只有一行（uk_enrollment_session），四类状态分开存（PRESENT/LATE/ABSENT/LEAVE），扣分口径 2×缺课 + 1×迟到 + 1×max(0,请假−3) 在成绩单侧计算，本表只留事实。双轨制落在 phase：NORMAL 为平时录入（可改、可作废），ARCHIVE 为归档口径（判定依据，带 archive_id）；归档后教师不可再改，只有教务可更正，且必须填写 update_note 并写 attendance_record_log。batch_key 让“一次点名提交”可整批撤回。',
  score_scheme_item: '成绩构成的两级定义：parent_id 为空是一级项（直接对总评加权），非空是二级项（对父项加权），item_level 显式标注层级。同一层级 weight 合计必须为 100.00，否则不允许确认与锁定。score_type 支持数值/通过制/等级制，后两者靠 mapping_json 换算成数值再参与加权；expected_times 与 aggregate_mode（AVG/SUM/MAX/LAST）处理“作业 9 次取均值”这类多次项；is_anchor 标记终结性锚点项，供通道 C 校准；is_subjective 决定该项是否进入分项级教师尺度 Δ̂ 的估计；考勤不设评分项，缺课扣分在成绩单单列。',
  score_item: '一人一评分项一编号一行（uk_enroll_item_times），是分项级 Δ 与成绩明细的共同基础。full_score 在写入时快照，防止后期改动构成影响历史；is_absent 区分“缺交/缺考（按 0 分计）”与“尚未录入（不参与汇总）”，这是总评能否定稿的关键标记；source 与 scorer_id / scorer_role 记录这笔分是教师录入还是系统按提交得分汇总。',
  score_result: '一条选课一张成绩单（uk_enrollment）。加权分 W = Σ(得分/满分×100×权重)，扣分 P = 2×缺课 + 1×迟到 + 1×max(0,请假−3)（不设上限、不截断），总评 T = W − P（可为负）。判定与计算分开存：pass_score、allowed_absent、effective_sessions 是判定当时的快照，hard_fail（缺课超 1/3，不看分数直接挂科）与 is_fail 分开表示，fail_reason 给出 SCORE/HARD_LINE/NO_SCORE 三类原因。score_status 只表达成绩是否已定（缓考、旷考、成绩未齐），不参与挂科判定。重算覆盖本行并更新 engine_version 与 calculated_at，历史结论由审计日志追溯。',
  risk_state: '一人一课一行的“当前风险”（uk_enrollment），替代按天写快照的做法。A/B/C 三通道各自成段存放，并各有 channel_*_level；risk_score 是对外分数（有 C 用 C、无 C 用 B、A 单列展示），active_channel 指明本次生效通道——教务看 C、导员看 A+B。confidence 与 confidence_level 给出可信度，output_mode 决定对外形态（完整分档/带标注/仅行为预警/数据不足），data_quality_json 存覆盖率、锚点强度、样本量与缺失项，解释“为什么只给行为预警”。trigger_source、last_eval_time、is_void 支撑作废与“干预是否有效”的判定窗口。**第一阶段该表为空**：当前判定按请求实时计算、结果不落库，第二阶段接入持久化时按本结构写入。',
  alert_record: '预警是“事件”，与 risk_state（状态）分离：状态被刷新覆盖，事件逐条累积。uk_enroll_current（enrollment_id, is_current）保证每生每课只有一条当前预警，历史预警的 is_current 置 NULL 后退出唯一约束。trigger_channel 与 trigger_rule 说明“为什么触发”（如 A_HARD_LINE 已超 1/3、A_UNREACHABLE 期末不可达、B_RANK 班内靠前），evidence_json 与 channel_snapshot_json 存触发时的依据与三通道值，供事后解释与申诉。状态机 ACTIVE → ACKNOWLEDGED/HANDLING → CLOSED/DISMISSED/EXPIRED，等级变化记 escalated / previous_level，处置记 handle_*、close_* 与学生回执 student_response，expire_time 驱动超时升级或自动关闭。**第一阶段同样为空**，与 risk_state 同期接入。'
};

// ---------- 字段说明补全 ----------
const COMMON_DESC = {
  id: '主键', create_time: '创建时间', update_time: '更新时间', create_by: '创建人ID',
  update_by: '更新人ID', remark: '备注', is_void: '是否作废', void_reason: '作废原因'
};
const KEY_LABEL = { PRI: 'PK', UNI: 'UK', MUL: 'FK', '': '' };

function colRows(name) {
  const t = T(name);
  return t.cols.map(c => [
    c.name,
    c.type,
    c.nullable === 'YES' ? '是' : '否',
    KEY_LABEL[c.key] === undefined ? '' : KEY_LABEL[c.key],
    c.comment || COMMON_DESC[c.name] || '—'
  ]);
}

// ---------- 主要字段（简要表用） ----------
const SKIP = new Set(['id', 'create_time', 'update_time', 'create_by', 'update_by', 'remark']);
function briefCols(name) {
  const cols = T(name).cols.map(c => c.name).filter(n => !SKIP.has(n));
  if (cols.length <= 12) return cols.join('、');
  return cols.slice(0, 12).join('、') + ' 等 ' + cols.length + ' 个';
}

// ---------- 组装 blocks ----------
const B = [];
const h1 = x => B.push({ t: 'h1', x });
const h2 = x => B.push({ t: 'h2', x });
const h3 = x => B.push({ t: 'h3', x });
const p = x => B.push({ t: 'p', x });
const note = x => B.push({ t: 'note', x });
const img = (file, cap, maxW) => B.push({ t: 'img', file, cap, maxW });
const table = (cap, widths, head, rows) => B.push({ t: 'table', cap, widths, head, rows });

h1('4. 数据设计');
p('本章把第 2 章的业务分工与第 3 章的子系统、类设计落到库表上。全系统使用一个 MySQL 8.0.30 实例、一个库（study_warning_system_v2，字符集 utf8mb4），共 **60 张表**，由 v2.0/sql 下 17 个建库脚本按层建立；本章的字段名、类型与注释均以线上库为准（导出件见 v2.0/tool/columns.tsv）。');
p('组织方式沿用系统自身的四条数据带：**主数据**（学期、专业、班级、学生、教师、课程，相对稳定）、**过程数据**（选课、课次、考勤、提交、得分，只增不改或改动留痕）、**判定数据**（风险状态、成绩单、预警，一次判定一行并带口径快照）、**留痕数据**（审计日志、预警时间线、考勤留痕，只追加）。本章依次给出设计原则、命名与字典、概念模型与各域实体关系、逐表结构、完整性约束与安全运维。');
note('与建库脚本的对应：01 基础层、02 组织与开课、03 选课与课次、04 考勤、05 成绩、06 引擎产物、07 预警与干预、08 流程与审计、11 教学内容、12 测验作业、13 项目、14 访问、15 合成数据、16 令牌落库、17 字典口径对齐。本章不重复贴 DDL，只给结构与口径。');

// ================= 4.1 =================
h2('4.1 设计目标与原则');
p('本章的目标可以压成一句话：**让每一条预警结论都能被复算**。展开成三条：① 一人一课的判定输入可定位，明确“算的是哪次选课”；② 判定时刻的口径可还原，明确“当时用的什么及格线、多少分母课次、哪个引擎版本”；③ 任何一次改动可追到人，明确“谁在什么时候把缺课改成了请假”。以下六条原则就是围绕这三点展开的。');
p('**原则 1：学期是全库时间轴。** 60 张表中有 35 张带 term_id，覆盖开课、课次、考勤、得分、成绩、判定与问卷全链路。学期切换不搬数据，只把 term.is_current 从旧学期挪到新学期（全库唯一一条为 1），因此任何页面的“当前学期”都不需要在客户端判断；跨学期统计（挂科率、学生历史）一律先按 term_id 或 term 序列收敛，避免口径漂移。');
p('**原则 2：过程数据挂在选课上。** 考勤、分项得分、成绩单、风险状态、预警、提交都以 enrollment_id 为锚点，而不是 student_id + course_id。原因很实际：同一门课可能存在多个教学班与重修记录，用“学生 + 课程”会把首修与重修、正班与重修班的口径串在一起；选课行天然区分教学班、学期与重修次数，退课也只需改选课状态。为了少跳表，enrollment 与若干过程表冗余了 offering_id、teaching_class_id，但一律在写入时校验与选课一致。');
p('**原则 3：口径快照随结论落库。** risk_state 落 channel_a/b/c 各项分值、delta_used、course_factor_used、confidence、engine_version；alert_record 落 evidence_json 与 channel_snapshot_json；score_result 落 pass_score、allowed_absent、effective_sessions。判定结论一旦落库即成为“当时的结论”，此后评分构成、课程因子或引擎参数变化只影响新的判定，不改历史行；需要重算时新行覆盖旧行并留下引擎版本与时间。');
p('**原则 4：能推导的不重复存。** 折算分、等级、名次等由基础数据实时计算，只有对外展示值与判定结论才落库（risk_score、total_score，以及 channel_b_rank 这类“当次排名的快照”）。成绩单不保存“每次作业 × 权重”的展开明细，避免同一事实两处写入；需要明细时按 score_item 复现。');
p('**原则 5：只作废、不物理删除。** 过程数据用 is_void + void_reason 标记作废（9 张表），主数据用 status 停用；作废行不进入任何统计与列表，但仍留在库中，保证历史判定可回溯、报表口径不突变。留痕表（audit_log、alert_flow_log、attendance_record_log）只追加，服务层不提供修改与删除接口。');
p('**原则 6：改动留痕双写。** 同一事务内既写业务表也写留痕表：考勤改动写 attendance_record_log（before/after 快照），预警状态流转写 alert_flow_log，所有写操作落 audit_log。代价是每次写多一条 INSERT，收益是事后能回答“这条缺课是谁在第几周改的”。');
note('三条硬边界由库级与应用级双重保证：① 一个教学班只有一位老师（teaching_class.teacher_id 单值，扩班只能新建教学班）；② 同一层评分项权重之和必须为 100.00%，未通过不允许确认与锁定；③ 一人一课最多一条当前预警（uk_enroll_current，历史预警 is_current 置 NULL）。详见 4.5。');

// ================= 4.2 =================
h2('4.2 命名规范与数据字典');
p('命名统一为“对象 + 后缀”两段式，后缀表达职责而不是实现方式。表名不加 tb_/t_ 前缀，字段不用拼音缩写；枚举一律用大写下划线常量而非数字码，让日志与导出件可以直接阅读。');

h3('4.2.1 命名规范');
table('命名规范', [16, 44, 40], ['对象', '规则', '示例'], [
  ['表名', '单数、全小写、下划线分词；业务表不加前缀', 'course_offering、attendance_record、score_scheme_item'],
  ['关联表', '两个实体名下划线拼接，从属在前、主体在后', 'teaching_class_admin_class、counselor_class'],
  ['留痕表', '业务表名加 _log；生命周期类用 _flow_log', 'attendance_record_log、alert_flow_log'],
  ['批次与历史', '_archive 表示批次，_history 表示跨学期历史', 'attendance_archive、course_fail_rate_history'],
  ['主键', '一律 id，bigint 自增，不命名 xx_id', 'id'],
  ['外键', '被引用表名单数 + _id；冗余外键在注释里写明派生来源', 'enrollment_id、teaching_class_id、offering_id'],
  ['布尔', 'is_ 前缀，tinyint(1)，取值 0/1', 'is_current、is_void、is_anchor、is_subjective'],
  ['分值与权重', '一律 decimal，禁止 float；权重两位、分数一至两位', 'weight decimal(5,2)、score decimal(5,1)'],
  ['时间', '动作时间用 xxx_time（datetime），日期用 xxx_date（date）', 'create_time、session_date、deadline'],
  ['JSON 明细', '_json 后缀，存当次快照，可空', 'evidence_json、channel_snapshot_json、mapping_json'],
  ['唯一键', 'uk_ 前缀，多列按选择性从高到低排列', 'uk_enrollment_session、uk_enroll_item_times'],
  ['枚举值', 'varchar(20)，大写下划线常量，不用数字码', 'RED/ORANGE/YELLOW、NOT_CALLED']
]);
note('令牌、验证码等安全相关字段不加业务前缀：sys_account.token、sys_account.token_expire_time，字段名本身已经足够具体。');

h3('4.2.2 公共字段');
p('公共字段只表达“谁在什么时候动过这行”，不承载业务语义，因此不进入任何计算公式。覆盖情况如下（分母为 60 张表）：');
table('公共字段与覆盖情况', [20, 18, 10, 52], ['字段', '类型', '覆盖', '约定'], [
  ['id', 'bigint', '60/60', '自增主键，业务上不承载含义；对外一律用业务编号（学号、工号、课程名）'],
  ['create_time', 'datetime', '59/60', '插入时间，默认 CURRENT_TIMESTAMP；alert_flow_log 用 log_time 表示事件时点'],
  ['update_time', 'datetime', '52/60', '最近更新时间，ON UPDATE CURRENT_TIMESTAMP；只追加的留痕表不设此列'],
  ['create_by / update_by', 'bigint', '19 / 18', '操作人ID，指向 sys_account.id；过程表另用 recorded_by、scorer_id 表达业务角色'],
  ['remark', 'varchar(200)', '13/60', '自由备注，不参与统计与判定'],
  ['term_id', 'bigint', '35/60', '学期轴；凡带此列的表，跨学期统计必须先按学期收敛'],
  ['status', 'varchar(20)', '28/60', '业务状态机当前态，取值见 4.2.3；不写数字码'],
  ['is_void / void_reason', 'tinyint(1) / varchar(200)', '9 / 5', '作废标记与原因；作废行不进任何统计，也不物理删除']
]);

h3('4.2.3 枚举与字典');
p('枚举集中在下表。这些取值同时出现在库注释、接口文档与前端筛选器中，改动需三处同步；新增取值只需扩注释，不必改表结构。');
table('关键枚举与字典口径', [24, 34, 42], ['字段', '取值', '口径说明'], [
  ['sys_account.role', 'STUDENT / TEACHER / COUNSELOR / ADMIN', '四端唯一入口；角色决定落地页与导航，越权在服务端拦截'],
  ['sys_account.status', 'ACTIVE / DISABLED / LOCKED', '停用与锁定都立即失效，拦截器每次请求校验令牌与账号状态'],
  ['term.status', 'PLANNING / ACTIVE / CLOSED', '学期只归档不删除；is_current 另由服务端维护唯一一条为 1'],
  ['enrollment.status', 'ENROLLED / REPEAT / WITHDRAWN / EXEMPT', '在册 / 重修 / 已退课 / 免修；退课后过程数据保留但不进统计'],
  ['course_session.status', 'NORMAL / CANCELLED / ADJUSTED / MERGED', '停课、调课、合并的课次不计入 1/3 门槛分母'],
  ['course_session.roll_call_status', 'NOT_CALLED / CALLED / PARTIAL', '点名状态；覆盖率与“已完成课次”的判定依据'],
  ['attendance_record.status', 'PRESENT / LATE / ABSENT / LEAVE', '四类必须分开存；扣分口径 2×缺课 + 1×迟到 + 1×max(0,请假−3)'],
  ['attendance_record.phase', 'NORMAL / ARCHIVE', '双轨制：平时录入 vs 归档口径；风险判定只认 ARCHIVE'],
  ['attendance_record.source', 'TEACHER / ADMIN', '归档后教师直录被禁止，只有教务可更正并留痕'],
  ['attendance_archive.status', 'PENDING / SUBMITTED / REVIEWED / RETURNED', '归档批次审核流；退回后可重新提交'],
  ['score_scheme.confirm_status', 'DRAFT / CONFIRMING / CONFIRMED / LOCKED', '锁定后评分项不可改，只能升版本'],
  ['score_scheme_item.data_source', 'AUTO / MANUAL', 'AUTO 由提交得分或项目得分汇总，MANUAL 由教师录入'],
  ['score_scheme_item.score_type', 'NUMERIC / PASS_FAIL / GRADE', '后两种经 mapping_json 换算成数值后参与加权'],
  ['score_scheme_item.aggregate_mode', 'AVG / SUM / MAX / LAST', '多次项（如 9 次作业）的汇总方式'],
  ['score_item.source', 'MANUAL / AUTO', '这一笔分是老师录的还是系统汇总的'],
  ['paper.paper_type', 'QUIZ / HOMEWORK / EXAM', '随堂测验、课后作业、线上考试共用一套题目与判分结构'],
  ['submission.marked_status', 'NOT_MARKED / SYSTEM_MARKED / ALL_MARKED', '待批阅列表与批阅进度看这里'],
  ['score_result.score_status', 'PENDING / FINAL / DEFERRED / ABSENT_EXAM / NO_SCORE', '只表达成绩是否已定，不参与挂科判定'],
  ['score_result.fail_reason', 'SCORE / HARD_LINE / NO_SCORE', '挂科三因；HARD_LINE 是不看分数的缺课硬门槛'],
  ['risk_state.risk_level', 'RED / ORANGE / YELLOW / GREEN', '对外等级；A 通道等级（SAFE/WATCH/DANGER/FAIL）单列展示'],
  ['risk_state.output_mode', 'FULL_SCORE / SCORE_WITH_NOTE / BEHAVIOR_ONLY / INSUFFICIENT', '数据不足时只出行为预警，不给分数'],
  ['alert_record.status', 'ACTIVE / ACKNOWLEDGED / HANDLING / CLOSED / DISMISSED / EXPIRED', '与 uk_enroll_current 配合，保证每生每课仅一条当前预警'],
  ['intervention_record.stage', 'TEACHER / COUNSELOR_1 / COUNSELOR_2 / REFERRED / ADMIN_TALK / ADMIN_HELP / TERMINATED', '教师、导员、教务三级处理链与终止'],
  ['questionnaire_flow.stage', 'COUNSELOR_COLLECT / ADMIN_RECEIVE / ADMIN_DISTRIBUTE / TEACHER_VIEW', '问卷四环节，缺一环即断链'],
  ['import_batch.status', 'RUNNING / SUCCESS / PARTIAL / FAILED / ROLLED_BACK', 'PARTIAL 也整批回滚，避免半批数据入库']
]);
note('枚举以 varchar 存储而非 tinyint：代价是每行几十字节，收益是可读、可查、可导入导出，且新增取值不改表结构。数据量大到需要省空间时，再引入字典表与代码映射。');

// ================= 4.3 =================
h2('4.3 概念结构设计');
p('60 张表如果硬塞进一张全景 ER 图，实体框会小到无法辨认，而本章 4.4 已逐表给出字段，图的作用应是“一眼看清主干与归属”。因此 ER 分两层：**图4-1 是概念级**，只画 16 个核心实体与主干联系，回答“系统由哪些东西构成、它们怎么连”；**图4-2 至图4-8 是域级**，按 7 个业务域画出该域全部实体及其主要字段，回答“域内怎么连”。域与域之间只通过 4 个公共锚点连接：term（时间）、student（人）、enrollment（选课）、teaching_class（班），因此任意一张表都能在两跳内找到所属域。');
p('读图约定：框内首行为实体中文名与表名，id* 为关键字；连线两端标注 1 与 N；虚线表示可空外键（不要求被引用行先存在）。图只表达逻辑外键，**库内不建物理外键约束**——分阶段建库与批量导入需要跳过校验，引用完整性由服务层事务与 4.5 的约束清单承担。');
img('out/209_concept.png', '学情预警系统核心实体关系图（概念级）', 600);
p('图4-1 的主干是“学期 → 开课实例 → 教学班 → 选课”，选课往下分叉出三条链：考勤记录（行为）、分项得分（成绩）、风险状态（判定）；风险状态再引出预警记录与干预记录，通知挂在预警上。行政班与辅导员不在主干上而是旁挂：行政班—学生（1:N）、辅导员—行政班（N:N，经 counselor_class）。教师只在教学班这一层出现，避免“教师直接对课程负责”的歧义。');
img('out/210_basic.png', '基础层与组织域实体关系', 600);
p('本域是“时间 + 人 + 组织”的最小集合，其余 7 个域都靠它定位数据范围。term 是全库时间轴，term_calendar 存周次、假期与考试周；sys_account 是四端唯一登录入口，student、teacher、counselor 各自通过 account_id 回指同一账号；student 只存 major_id、admin_class_id 与 enroll_year，年级由入学年份与当前学期推算；counselor_class 把导员与行政班按学期绑定（uk_class_term_counselor），换导员不动学生档案。admin 表与 sys_account 一一对应（图中未单列），system_config 是原子键值表、不参与关系。');
img('out/211_offering.png', '开课与成绩构成域实体关系', 600);
p('课程（course）是全校科目池；课程组（course_group）是“课程 × 学期”的开课准备单元；开课实例（course_offering）由课程组下发，带计划课次、实际课次、有效课次与及格线；教学班（teaching_class）是开课实例下的授课单位，一个班一位老师、一套课次。成绩构成沿“模板 → 构成 → 评分项”三级落地：score_template 是校级模板（uk_template_code），score_scheme 是课程组本学期锁定的构成（uk_group_version 版本化），score_scheme_item 用 parent_id 自关联成两级。教学班与行政班的对应放在 teaching_class_admin_class，供导员按班查看与覆盖率督办使用。');
img('out/212_attendance.png', '选课、课次与考勤域实体关系', 600);
p('选课是过程数据的根；course_session 由课表驱动按教学班生成（uk_class_session_no 保证班内序号唯一），停课、调课只改 status、不删行。考勤采用双轨制：attendance_record.phase 区分平时（NORMAL）与归档（ARCHIVE），归档批次 attendance_archive 一学期两次（期中、期末，uk_class_phase 允许作废后重开），只有归档口径才进入风险判定。每一次行级改动写 attendance_record_log，保留 before/after 快照与操作人。');
img('out/213_score.png', '成绩与教学活动域实体关系', 600);
p('作业、测验、考试共用“试卷 → 题目 → 提交 → 逐题作答”一条链（paper / question / submission / submission_answer），客观题自动判分、主观题教师批阅，结果都落在 submission_answer。分项得分 score_item 是“评分项 × 次数”的明细：自动型评分项（data_source=AUTO）由提交得分或项目得分汇总后写入，手工分项由教师录入。成绩单 score_result 一条选课一行，只存加权分 W、扣分 P、总评 T 与判定结果，折算过程随时可按 score_item 复现。项目类活动另有三张表：project_task（任务）、project_team（组队）、project_submission 与 project_contribution（提交与团队贡献占比，锁定后不可改）。');
img('out/214_engine.png', '引擎产物域实体关系', 600);
p('本域六张表都是“判定的中间产物”，不是业务原始数据。risk_engine_config 按 engine_version 版本化，历史版本只读、可复现当时的判定；risk_state 一人一课一行存当前风险（A/B/C 三通道值、对外风险分、置信度、输出形态、引擎版本）；teacher_style 存分项级教师尺度 Δ̂（含收缩与熔断状态）；三张 fail_rate 表分别按课程、教学班、行政班聚合历史挂科率，是课程因子与班内相对风险的输入；student_fail_history 存学生跨学期挂科历史，供锚点强度与样本充足度估计。');
img('out/215_alert.png', '预警与干预闭环域实体关系', 600);
p('预警由 risk_state 生成，uk_enroll_current 保证每生每课只有一条“当前”预警，等级变化用 escalated 与 previous_level 记录，全过程写 alert_flow_log。干预记录 intervention_record 以 stage 区分教师处理、导员第 1/2 次、上报教务、教务面谈、教务帮助与终止，并保存干预前后的风险分（risk_score_before/after），用于“干预是否有效”的判定（窗口起点取 risk_state.last_eval_time）。通知按 aggregate_key 聚合去重，每个接收人一行 notification_receipt 记录投递与回执；班集体提醒另有 class_alert_notification 防止天天提醒。');
img('out/216_admin.png', '教务流程、问卷与审计域实体关系', 600);
p('教务侧流程表包括：覆盖率体检（coverage_check_log，按教学班 × 周 × 检查类型去重）、实际考核人数核对（exam_headcount，按开课 × 阶段唯一）、导入批次（import_batch，整批可回滚）、统一审计日志（audit_log）与运营配置（system_config）。问卷按四环节流转：导员收集 → 教务接收 → 教务分发 → 授课组查看，状态落在 questionnaire_flow（uk_q_stage 保证每个环节一条）；作答匿名化，questionnaire_answer 只存 respondent_token（SHA-256）用于去重，student_id 允许为空。');
p('未单列图的 5 张附属表：course_guide（每位教师一份课程导学）、teaching_objective（教学目标）、course_lesson（目录课时，一行一次课）、lesson_resource（课时资源与任务，按课前/课中/课后）、resource_file（资源文件）。它们只是“开课实例 → 课时 → 资源”的下挂明细，只引入 1:N 与 1:1 的从属关系、不产生新的多对多联系，故不单独出图，外键指向见表4-17。');

// ================= 4.4 =================
h2('4.4 表的详细设计');
h3('4.4.1 数据表总览');
p('按数据性质，60 张表可以再归成三类：**基础数据**（学期、专业、行政班、学生、教师、辅导员、账号、课程，共 15 张左右，写少读多，变更必须留痕）、**核心业务数据**（开课、教学班、选课、课次、考勤、评分项、分项得分、提交、项目、问卷等过程表，占全库大头，按学期增量增长）、**统计数据**（成绩单、风险状态、预警、干预、三张挂科率表、覆盖率体检，均为“一次计算一行”的结论表，按判定刷新或按学期结算）。三类表的生命周期不同：基础数据只停用不删除，过程数据只作废不删除，统计数据可重算覆盖、但旧结论必须能从留痕表与引擎版本还原。');
p('再按业务域看，60 张表分布在 8 个域，与 4.3 的图一一对应（教学资源域无独立图）。序号只用于索引、与建表顺序无关；字段数为线上库实际列数；主键一律 id，因此“业务唯一键”列只标注该表是否另有唯一约束。');
note('域与存储层是两套划分，不要混淆：8 个域是“阅读与出图”的划分，12 组存储层是“建库脚本”的划分（对应 01 基础层、02 组织与开课、03 选课与课次、04 考勤、05 成绩、06 引擎产物、07 预警与干预、08 流程与审计、11 教学内容、12 测验作业、13 项目、14 访问；15 号是合成数据脚本、16~17 是增量脚本）。两者并非一一对应，例如“开课与构成”域的表分别落在组织与开课层和成绩层，但两套划分都能落到具体的表，第 5 章与第 6 章的部署说明按存储层组织。');
const overviewRows = ordered.map((n, i) => {
  const t = T(n);
  const uk = t.cols.filter(c => c.key === 'UNI').map(c => c.name);
  return [String(i + 1), n, t.comment, domainOf[n], uk.length ? uk.join('/') : '—', String(t.cols.length)];
});
table('数据表总览（60 张）', [6, 25, 43, 12, 10, 4], ['序号', '表名', '说明', '所属域', '业务唯一键', '字段数'], overviewRows);

h3('4.4.2 核心表结构');
p('这里选 12 张“判定链上的关键表”给出完整结构：登录与时间轴（sys_account、term）、主体（student、teaching_class）、过程锚点（enrollment、course_session、attendance_record）、评分（score_scheme_item、score_item、score_result）、判定与处置（risk_state、alert_record）。其余 48 张的用途与主要字段见 4.4.3，完整 DDL 见 v2.0/sql 对应脚本。');
const CORE_TITLE = {
  sys_account: '统一账号表 sys_account', term: '学期表 term', student: '学生表 student',
  teaching_class: '教学班表 teaching_class', enrollment: '选课表 enrollment',
  course_session: '课次表 course_session', attendance_record: '考勤明细表 attendance_record',
  score_scheme_item: '评分项定义表 score_scheme_item', score_item: '分项得分表 score_item',
  score_result: '成绩单 score_result', risk_state: '当前风险状态表 risk_state',
  alert_record: '预警记录表 alert_record'
};
for (const n of CORE) {
  const t = T(n);
  p('**' + CORE_TITLE[n] + '（' + t.cols.length + ' 列）。** ' + CORE_TEXT[n]);
  table(CORE_TITLE[n] + ' 结构', [19, 17, 6, 6, 52], ['字段', '类型', '可空', '键', '说明'], colRows(n));
}

h3('4.4.3 其余数据表');
p('下表给出余下 48 张表的用途与主要业务字段，略去 id、create_time、update_time、create_by、update_by、remark 等公共字段（见 4.2.2）；字段按表中顺序列出，超过 12 列者只列前 12 列并标注总列数，完整定义见建库脚本。');
const briefRows = ordered.filter(n => CORE.indexOf(n) < 0).map(n => {
  const t = T(n);
  return [n, domainOf[n], t.comment, briefCols(n)];
});
table('其余数据表（48 张）', [18, 11, 30, 41], ['表名', '所属域', '用途', '主要字段'], briefRows);

// ================= 4.5 =================
h2('4.5 完整性与一致性约束');
p('约束分三层：**库级**（主键、唯一键、NOT NULL、默认值）、**事务级**（同一事务内的跨表校验与派生字段计算）、**口径级**（同一事实只允许一种算法）。库内未建物理外键，等价的引用完整性由事务级校验承担：这样批量导入可以先灌子表再回填，也不会因为外键顺序失败；代价是绕过服务层直接改库时需要人工保证一致性，这一点在运维规范里明确禁止。');
table('关键完整性约束', [23, 19, 38, 20], ['约束 / 索引', '所在表', '规则', '违反时的处理'], [
  ['uk_login_no', 'sys_account', '登录号全库唯一（学号、工号、管理员号同池）', '重复建档返回 400'],
  ['uk_token', 'sys_account', '令牌唯一；退出登录置 NULL（NULL 不参与唯一判定）', '—'],
  ['uk_student_offering、uk_student_teaching_class', 'enrollment', '同一学生同一开课、同一教学班只能有一条选课', '重复选课返回 409'],
  ['uk_enrollment_session', 'attendance_record', '一名学生一次课只有一条考勤记录', '重复提交按更新处理并写留痕'],
  ['uk_class_phase', 'attendance_archive', '同一教学班同一阶段（期中/期末）只有一个有效批次，is_void 参与唯一键', '重复开批次返回 409'],
  ['uk_class_session_no', 'course_session', '教学班内课次序号唯一，点名按序号定位', '排课冲突返回 409'],
  ['uk_scheme_item', 'score_scheme_item', '同一成绩构成内 item_code 唯一', '保存校验返回 400'],
  ['uk_enroll_item_times', 'score_item', '一人一评分项一次数只有一条得分', '覆盖写并留痕'],
  ['uk_enrollment（score_result）', 'score_result', '一条选课只有一张成绩单', '重算覆盖，保留引擎版本与时间'],
  ['uk_enrollment（risk_state）', 'risk_state', '一人一课一行“当前风险”', '刷新即覆盖，历史由时间线追溯'],
  ['uk_enroll_current', 'alert_record', '(enrollment_id, is_current) 唯一：当前预警只有一条', '生成前先关闭旧预警'],
  ['uk_project_team', 'project_contribution', '同一项目组只有一份团队贡献表，锁定后不可改', '重复提交返回 409'],
  ['uk_q_token_item', 'questionnaire_answer', '匿名问卷按 respondent_token 去重，同一问卷同一题只答一次', '重复提交返回 409'],
  ['uk_engine_version', 'risk_engine_config', '引擎配置按版本号唯一，历史版本只读', '返回 400'],
  ['uk_aggregate_key', 'notification', '通知聚合键唯一（同班同周同规则只发一条）', '静默丢弃重复通知'],
  ['同级权重合计 = 100.00', 'score_scheme_item', '应用级校验：同一层级 weight 之和必须为 100.00', '400，并返回当前差额'],
  ['pass_score ∈ [0, 100]', 'course_offering / score_result', '应用级校验：及格线快照写入前校验范围', '400']
]);
p('派生字段统一在服务层计算并校验：enrollment.offering_id 由 teaching_class 派生，score_item.student_id 与 attendance_record.student_id 由 enrollment 派生，teaching_class_admin_class 由排课时的行政班范围生成。写入时校验派生值与来源一致，从根上避免“选课挂在 A 班、考勤记录却写着 B 班学生”这类脏数据。');
p('三条容易冲突的口径已在实现中固定：① 缺课分母取已完成课次（status 为 NORMAL 且已点名），停课、调课与未点名课次都不进分母；② 挂科判定以 is_fail 为准（total_score 低于 pass_score 或 hard_fail=1），两类原因分别落到 fail_reason；③ 风险等级取三通道最高档，A 通道等级单列展示，不与 B/C 的分值混算。');
p('**索引设计。** 索引只按“页面真实会发的查询”建，不追求字段全覆盖：列表页几乎都带“学期 + 教学班/学生”两个条件，因此过程表统一建 (student_id, term_id) 或 (teaching_class_id, term_id) 前缀索引；归档与督办类页面按“状态 + 时间”翻页，对应 (status, deadline)、(status, expire_time)；统计类页面按“对象 + 判定结果”过滤，对应 (offering_id, is_fail, is_void)、(term_id, risk_level)。唯一键本身也是索引，点名去重、成绩单定位、当前预警判定都直接命中唯一键。下表列出与页面一一对应的主要索引。');
table('主要索引与所服务的查询', [24, 20, 22, 34], ['索引', '表', '列', '服务的查询'], [
  ['uk_enrollment_session', 'attendance_record', 'enrollment_id, session_id', '单人考勤明细，点名去重'],
  ['idx_student_term', 'attendance_record', 'student_id, term_id', '学生端按学期看考勤与缺课次数'],
  ['idx_session_status', 'attendance_record', 'session_id, status', '某次课的点名结果与班级出勤统计'],
  ['idx_roll_call', 'course_session', 'teaching_class_id, roll_call_status', '覆盖率督办：找出未点名课次'],
  ['idx_class_week', 'course_session', 'teaching_class_id, week_no', '课表与“第几周上什么课”'],
  ['idx_teaching_class', 'enrollment', 'teaching_class_id', '教学班名单（按班取学生）'],
  ['idx_student_term', 'enrollment', 'student_id, term_id', '学生端“我的课程”'],
  ['uk_enroll_item_times', 'score_item', 'enrollment_id, scheme_item_id, times_no', '分项得分明细与重复写入保护'],
  ['idx_term_student', 'score_item', 'term_id, student_id', '按学期汇总一个学生的分项得分'],
  ['uk_enrollment', 'score_result', 'enrollment_id', '由选课直接定位成绩单'],
  ['idx_offering_fail', 'score_result', 'offering_id, is_fail, is_void', '教学班挂科名单与挂科率'],
  ['idx_term_level', 'risk_state', 'term_id, risk_level', '预警列表按学期 + 等级筛选'],
  ['idx_class_level', 'risk_state', 'teaching_class_id, risk_level', '教师端按教学班看风险学生'],
  ['idx_teacher_query', 'alert_record', 'teaching_class_id, status', '教师端待处理预警'],
  ['idx_status_expire', 'alert_record', 'status, expire_time', '超时预警扫描与自动升级'],
  ['uk_enroll_current', 'alert_record', 'enrollment_id, is_current', '每生每课唯一当前预警'],
  ['idx_student_time', 'intervention_record', 'student_id, create_time', '干预时间线与“是否有效”判定'],
  ['idx_biz', 'audit_log', 'biz_type, biz_id', '按业务对象追溯单条记录的改动'],
  ['idx_operator_time', 'audit_log', 'operator_id, create_time', '按操作人追溯操作序列'],
  ['idx_admin_class', 'student', 'admin_class_id', '导员按行政班取学生名单'],
  ['idx_role_status', 'sys_account', 'role, status', '四端登录与停用/锁定校验']
]);

// ================= 4.6 =================
h2('4.6 数据安全、留痕与运维');
p('**认证与口令。** 口令使用 PBKDF2WithHmacSHA256 加盐哈希（120000 次迭代、16 字节盐），存储格式为 pbkdf2$迭代次数$Base64(盐)$Base64(哈希)，改迭代次数不改存储结构即可升级；登录接口先校验图形验证码（4 位、5 分钟有效、一次性），再校验口令，失败一律返回“账号或口令错误”，不区分账号是否存在。');
p('**令牌与数据范围。** 令牌为 32 位十六进制随机串，默认 12 小时过期，落库在 sys_account.token 与 token_expire_time，服务重启不掉线、退出登录即失效。每次请求由拦截器校验令牌与账号状态，角色与归属（学生 ID、教学班 ID）随令牌解析，可见范围全部在服务端收窄：学生仅本人、教师仅本人教学班、导员仅名下行政班（经 counselor_class）、教务按学期与开课范围。客户端从不传“可见范围”参数，改参无效。');
p('**审计与留痕。** audit_log 记录操作人 ID 与角色、动作、目标对象、目标 ID、变更摘要、IP 与时间，覆盖四端的写操作；alert_flow_log 专记预警生命周期（生成、升级、通知、处理、关闭、超时），是预警详情页时间线的数据源；attendance_record_log 存考勤改动的 before/after JSON 与操作人。三张表只追加，服务层不提供更新与删除接口。');
p('**导入与回滚。** import_batch 以批次号唯一标识一次导入（来源文件、格式、总行数、成功/失败行数、错误明细、操作人），任一行解析失败则整批不写入，避免半批数据；已写入的批次可整批回滚（状态置 ROLLED_BACK 并删除该批次插入的行），回滚本身也写审计。');
p('**查询与缓存。** 查询侧的策略是“一次查询一次成表”：列表接口直接返回页面要展示的字段（含学生姓名、课程名等展示名），避免前端二次请求；分页统一用 limit/offset 加索引覆盖，不返回总数时不算 count。缓存侧分三层，一期不引入 Redis：① 静态字典（课程类型、枚举标签）在前端常量与后端枚举里各存一份，不查库；② 判定结果本身就是缓存——risk_state 与 score_result 的每一行都是一次计算的快照，页面读的是“上次判定结果”，不必每次重算，重算只在数据变更或人工触发时发生；③ 会话级缓存放在令牌与账号信息上（每请求一次轻量查询，配合角色与状态的联合索引）。数据量进入十万级或出现多实例部署时再评估 Redis，届时只需替换“读判定结果”的那一层。');
p('**敏感字段与脱敏。** 口令只存 PBKDF2 哈希，任何接口都不回显；手机号与邮箱在演示库中为虚构号码，不落真实个人信息；导出名单与成绩时，手机号按“前三位 + 掩码 + 后四位”输出（如 138****1234），仅在教师确有联系需要时才显示完整号码并记审计；问卷作答匿名化，questionnaire_answer 只保存 respondent_token（SHA-256，不含学号）用于防重复提交，student_id 允许为空；日志与审计只记 ID 与动作，不记明文联系方式。');
p('**容量与备份。** 演示库现有数据规模见表4-19，其中考勤明细是增长最快的一张表（720 名学生 × 6 门课 × 80 课次级别），按每学期末归档一次、平时只查当前学期的策略控制热数据量。备份建议每日一次全量 + binlog 增量，恢复到任意时间点；归档后的考勤与成绩建议按学期单独导出留档，库内保留只读副本。');
table('演示库数据规模（截至 2026-10）', [25, 16, 59], ['表', '行数', '说明'], [
  ['sys_account', '812', '四端账号：学生 800、教师 9、导员与教务各若干'],
  ['student', '800', '含教学班 CS-01（2025 级）与合成数据学期两批'],
  ['enrollment', '4587', '全部为 ENROLLED；包含 720 名学生 × 6 门课的合成批次'],
  ['course_session', '606', '按教学班排课生成，含停课与调课样例'],
  ['attendance_record', '43470', '点名记录，覆盖平时与归档两个阶段'],
  ['score_item', '9174', '分项得分明细（手工项 + 自动汇总项）'],
  ['score_result', '4587', '每条选课一张成绩单，与选课数一致'],
  ['submission', '9174', '作业与测验提交记录'],
  ['submission_answer', '6', '逐题作答仅对一份试卷做了样例数据，判分链路已验证'],
  ['paper', '38', '测验、作业、考试三类试卷/题目集'],
  ['project_contribution', '1', '团队贡献表样例（已锁定）'],
  ['intervention_record', '23', '三级干预处理记录样例'],
  ['notification', '1', '通知与聚合去重样例'],
  ['audit_log', '81', '四端写操作审计'],
  ['risk_state', '0', '第一阶段判定按请求实时计算，第二阶段接入落库'],
  ['alert_record', '0', '同上；结构与写入契约已按最终形态固定']
]);
note('risk_state 与 alert_record 为空不是缺表：第一阶段判定在服务层实时完成（教师端与导员端的预警列表即由 score_result 与考勤实时算出），两张表按第二阶段的引擎写入契约一次建好，避免二次改表；本章 4.4.2 按最终形态描述它们。');

fs.writeFileSync(path.join(here, 'proj_doc_ch4.json'),
  JSON.stringify({ chapter: 4, blocks: B }, null, 1), 'utf8');
console.log('WROTE proj_doc_ch4.json blocks=' + B.length);
const kinds = {};
B.forEach(b => { kinds[b.t] = (kinds[b.t] || 0) + 1; });
console.log(JSON.stringify(kinds));
