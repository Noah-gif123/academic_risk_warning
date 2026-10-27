import { createRouter, createWebHashHistory } from 'vue-router'
import { session, homeOf } from '../session'

const Placeholder = () => import('../pages/Placeholder.vue')

const routes = [
  { path: '/', redirect: '/login' },
  { path: '/login', component: () => import('../pages/Login.vue'), meta: { public: true } },

  { path: '/student', component: () => import('../layouts/PortLayout.vue'), props: { role: 'STUDENT' }, children: [
    { path: 'home', component: () => import('../pages/student/Home.vue'), meta: { title: '首页' } },
    { path: 'notifications', component: () => import('../pages/NotificationCenter.vue'), meta: { title: '消息中心' } },
    { path: 'courses', component: () => import('../pages/student/Courses.vue'), meta: { title: '课程学习' } },
    { path: 'course/:teachingClassId', component: () => import('../pages/student/CourseDetail.vue'), meta: { title: '课程详情' } },
    { path: 'profile', component: () => import('../pages/student/Profile.vue'), meta: { title: '个人中心' } },
    { path: 'exams', component: () => import('../pages/student/Papers.vue'), meta: { title: '我的考试' } },
    { path: 'take', component: () => import('../pages/student/TakePaper.vue'), meta: { title: '作答' } },
    { path: 'projects', component: () => import('../pages/student/Projects.vue'), meta: { title: '我的项目' } },
    { path: 'attendance', component: () => import('../pages/student/Attendance.vue'), meta: { title: '我的考勤' } },
    { path: 'scores', component: () => import('../pages/student/Scores.vue'), meta: { title: '我的成绩' } },
    { path: 'questionnaires', component: () => import('../pages/student/Questionnaires.vue'), meta: { title: '问卷调查' } },
    { path: 'warnings', component: () => import('../pages/student/Warnings.vue'), meta: { title: '学情预警' } },
    { path: 'reports', component: () => import('../pages/student/Reports.vue'), meta: { title: '学习评价' } }
  ] },

  { path: '/teacher', component: () => import('../layouts/PortLayout.vue'), props: { role: 'TEACHER' }, children: [
    { path: 'home', component: () => import('../pages/teacher/Home.vue'), meta: { title: '首页' } },
    { path: 'notifications', component: () => import('../pages/NotificationCenter.vue'), meta: { title: '消息中心' } },
    { path: 'score-scheme', component: () => import('../pages/teacher/ScoreScheme.vue'), meta: { title: '成绩构成' } },
    { path: 'course-content', component: () => import('../pages/teacher/CourseContent.vue'), meta: { title: '课程内容' } },
    { path: 'papers', component: () => import('../pages/teacher/Papers.vue'), meta: { title: '测验与作业' } },
    { path: 'grading', component: () => import('../pages/teacher/Grading.vue'), meta: { title: '交卷与批改' } },
    { path: 'projects', component: () => import('../pages/teacher/Projects.vue'), meta: { title: '项目与贡献度' } },
    { path: 'attendance', component: () => import('../pages/teacher/Attendance.vue'), meta: { title: '考勤与归档' } },
    { path: 'scores', component: () => import('../pages/teacher/Scores.vue'), meta: { title: '成绩与批改' } },
    { path: 'class-access', component: () => import('../pages/teacher/ClassAccess.vue'), meta: { title: '入班设置' } },
    { path: 'courses', component: () => import('../pages/teacher/Courses.vue'), meta: { title: '全部课程' } },
    { path: 'course/:offeringId', component: () => import('../pages/teacher/CourseDetail.vue'), meta: { title: '课程详情' } },
    { path: 'teaching', component: () => import('../pages/teacher/TeachingDetail.vue'), meta: { title: '教学详情' } },
    { path: 'monitor', component: () => import('../pages/teacher/Monitor.vue'), meta: { title: '预警监控' } }
  ] },

  { path: '/counselor', component: () => import('../layouts/PortLayout.vue'), props: { role: 'COUNSELOR' }, children: [
    { path: 'home', component: () => import('../pages/counselor/Home.vue'), meta: { title: '首页看板' } },
    { path: 'notifications', component: () => import('../pages/NotificationCenter.vue'), meta: { title: '消息中心' } },
    { path: 'focus', component: () => import('../pages/counselor/Focus.vue'), meta: { title: '重点关注' } },
    { path: 'classes', component: () => import('../pages/counselor/Classes.vue'), meta: { title: '班级管理' } },
    { path: 'interventions', component: () => import('../pages/counselor/Interventions.vue'), meta: { title: '干预记录' } },
    { path: 'questionnaires', component: () => import('../pages/counselor/Questionnaires.vue'), meta: { title: '问卷' } },
    { path: 'notices', component: () => import('../pages/counselor/Notices.vue'), meta: { title: '通知' } },
    { path: 'profile', component: () => import('../pages/counselor/Profile.vue'), meta: { title: '个人中心' } }
  ] },

  { path: '/admin', component: () => import('../layouts/PortLayout.vue'), props: { role: 'ADMIN' }, children: [
    { path: 'home', component: () => import('../pages/admin/Home.vue'), meta: { title: '首页' } },
    { path: 'notifications', component: () => import('../pages/NotificationCenter.vue'), meta: { title: '消息中心' } },
    { path: 'base', component: () => import('../pages/admin/BaseData.vue'), meta: { title: '基础数据' } },
    { path: 'offerings', component: () => import('../pages/admin/Offerings.vue'), meta: { title: '排课与教学班' } },
    { path: 'schemes', component: () => import('../pages/admin/Schemes.vue'), meta: { title: '成绩构成' } },
    { path: 'attendance', component: () => import('../pages/admin/Attendance.vue'), meta: { title: '考勤归档' } },
    { path: 'coverage', component: () => import('../pages/admin/Coverage.vue'), meta: { title: '覆盖率督办' } },
    { path: 'reports', component: Placeholder, meta: { title: '报表' } },
    { path: 'rules', component: () => import('../pages/admin/Rules.vue'), meta: { title: '规则配置' } },
    { path: 'workflow', component: () => import('../pages/admin/Workflow.vue'), meta: { title: '流程与问卷' } },
    { path: 'audit', component: () => import('../pages/admin/Audit.vue'), meta: { title: '审计' } }
  ] },

  { path: '/:pathMatch(.*)*', redirect: '/login' }
]

const router = createRouter({ history: createWebHashHistory(), routes })

router.beforeEach((to) => {
  if (to.meta.public) return true
  if (!session.token || !session.role) return '/login'
  const own = session.role.toLowerCase()
  const first = to.path.split('/')[1]
  if (first && first !== own) return homeOf(session.role)
  return true
})

export default router
