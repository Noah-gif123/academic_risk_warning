/** 四端导航项（按口径冻结版《功能整理》；2026-10 的两处调整见下方注释） */
export const NAV = {
  STUDENT: [
    { text: '首页', path: '/student/home' },
    { text: '课程学习', path: '/student/courses' },
    { text: '考试', path: '/student/exams', children: [
      { text: '我的考试', path: '/student/exams' },
      { text: '问卷调查', path: '/student/questionnaires' }
    ] },
    { text: '学情预警', path: '/student/warnings' }
  ],
  // 教师端：口径只有「首页 / 预警监控」两项；因新增「全部课程」独立页，补一项「课程」
  TEACHER: [
    { text: '首页', path: '/teacher/home' },
    { text: '课程', path: '/teacher/courses' },
    { text: '预警监控', path: '/teacher/monitor' }
  ],
  COUNSELOR: [
    { text: '首页看板', path: '/counselor/home' },
    { text: '重点关注', path: '/counselor/focus' },
    { text: '班级管理', path: '/counselor/classes' },
    { text: '干预记录', path: '/counselor/interventions' },
    { text: '问卷', path: '/counselor/questionnaires' },
    { text: '通知', path: '/counselor/notices' }
  ],
  // 教务端：2026-10 精简——「覆盖率督办」并入「考勤归档」页签、「审计」并入「流程与问卷」页签；
  // 「报表」「规则配置」属第二阶段能力，页面保留但暂不上导航
  ADMIN: [
    { text: '首页', path: '/admin/home' },
    { text: '基础数据', path: '/admin/base' },
    { text: '排课与教学班', path: '/admin/offerings' },
    { text: '成绩构成', path: '/admin/schemes' },
    { text: '考勤归档', path: '/admin/attendance' },
    { text: '流程与问卷', path: '/admin/workflow' }
  ]
}
