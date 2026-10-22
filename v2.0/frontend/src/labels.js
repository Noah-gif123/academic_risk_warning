/** 展示用文案映射：把库里的英文枚举翻成人能读懂的中文 */

/** 课程考核方式：EXAM 考试 / ASSESS 考查 */
export function courseTypeLabel(t) {
  if (!t) return '课程'
  const m = { EXAM: '考试', ASSESS: '考查', CHECK: '考查' }
  return m[String(t).toUpperCase()] || t
}

/** 学年 + 班级，例如“计算机1班（2025级）” */
export function classLabel(name, gradeYear) {
  if (!name) return '—'
  return gradeYear ? `${name}（${gradeYear}级）` : name
}

/** 试卷类型 */
export function paperTypeLabel(t) {
  const m = { QUIZ: '随堂测验', HOMEWORK: '课后作业', EXAM: '线上考试' }
  return m[t] || t
}

/** 预警等级：RED 红 / ORANGE 橙 / YELLOW 黄 / GREEN 绿 */
export function riskLevelLabel(l) {
  const m = { RED: '红色', ORANGE: '橙色', YELLOW: '黄色', GREEN: '绿色' }
  return m[l] || '—'
}

/** 预警等级标签配色（tag 用） */
export function riskLevelBadge(l) {
  const m = {
    RED: 'bg-red-50 text-red-600 border border-red-200',
    ORANGE: 'bg-orange-50 text-orange-600 border border-orange-200',
    YELLOW: 'bg-amber-50 text-amber-700 border border-amber-200',
    GREEN: 'bg-emerald-50 text-emerald-600 border border-emerald-200'
  }
  return m[l] || 'bg-slate-100 text-slate-500'
}

/** 预警处理状态 */
export function alertStatusLabel(s) {
  const m = { ACTIVE: '待处理', HANDLING: '处理中', ESCALATED: '已升级', CLOSED: '已关闭', DISMISSED: '已忽略' }
  return m[s] || s || '—'
}

/** 判定输出模式（置信度决定能输出到哪一步） */
export function outputModeLabel(m) {
  const map = {
    FULL_SCORE: '综合判定',
    SCORE_WITH_NOTE: '综合判定（仅供参考）',
    BEHAVIOR_ONLY: '仅行为预警',
    INSUFFICIENT: '数据不足'
  }
  return map[m] || m || '—'
}

/** 触发通道 A / B / C */
export function channelLabel(c) {
  const m = { A: 'A 通道', B: 'B 通道', C: 'C 通道' }
  return m[c] || (c ? c + ' 通道' : '—')
}

/** 触发规则编码 */
export function triggerRuleLabel(r) {
  const m = {
    A_HARD_LINE: '缺课超限（硬性不合格）',
    A_UNREACHABLE: '期末不可达',
    A_NEAR_LINE: '缺课接近上限',
    B_RANK: '加权分偏低'
  }
  return m[r] || r || '—'
}

/** 干预/处置动作类型 */
export function actionTypeLabel(a) {
  const m = { TALK: '谈话提醒', CONTACT: '联系学生', GUIDE: '学业辅导', PARENT: '联系家长', NOTICE: '书面告知', OTHER: '其他' }
  return m[a] || a || '—'
}

