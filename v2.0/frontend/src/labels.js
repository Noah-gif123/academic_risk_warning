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
