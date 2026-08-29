import { createRouter, createWebHashHistory } from 'vue-router'
import LandingPage from '../components/LandingPage.vue'
import TeacherLogin from '../pages/teacher/TeacherLogin.vue'
import TeacherCourses from '../pages/teacher/TeacherCourses.vue'
import TeacherCourseDetail from '../pages/teacher/TeacherCourseDetail.vue'
import StudentLogin from '../pages/student/StudentLogin.vue'
import StudentDashboard from '../pages/student/StudentDashboard.vue'
import AdminDashboard from '../pages/admin/AdminDashboard.vue'

const routes = [
  { path: '/', name: 'landing', component: LandingPage, meta: { title: '学情预警系统 - 首页' } },
  { path: '/teacher/login', name: 'teacherLogin', component: TeacherLogin, meta: { title: '教师登录', identity: 'teacher' } },
  { path: '/teacher/courses', name: 'teacherCourses', component: TeacherCourses, meta: { title: '课程总览', identity: 'teacher', requiresAuth: true } },
  { path: '/teacher/course/:courseId', name: 'teacherCourseDetail', component: TeacherCourseDetail, meta: { title: '课程详情', identity: 'teacher', requiresAuth: true } },
  { path: '/student/login', name: 'studentLogin', component: StudentLogin, meta: { title: '学生登录', identity: 'student' } },
  { path: '/student/info', name: 'studentInfo', component: StudentDashboard, meta: { title: '我的学情', identity: 'student', requiresAuth: true } },
  { path: '/student/warnings', name: 'studentWarnings', component: StudentDashboard, meta: { title: '我的预警', identity: 'student', requiresAuth: true, tab: 'warnings' } },
  { path: '/student/exercises', name: 'studentExercises', component: StudentDashboard, meta: { title: '我的练习', identity: 'student', requiresAuth: true, tab: 'exercises' } },
  { path: '/student/goals', name: 'studentGoals', component: StudentDashboard, meta: { title: '我的目标', identity: 'student', requiresAuth: true, tab: 'goals' } },
  { path: '/admin/login', name: 'adminLogin', component: AdminDashboard, meta: { title: '管理员登录', identity: 'admin' } },
  { path: '/admin/dashboard', name: 'adminDashboard', component: AdminDashboard, meta: { title: '教师总览', identity: 'admin', requiresAuth: true } },
  { path: '/admin/config', name: 'adminConfig', component: AdminDashboard, meta: { title: '规则配置', identity: 'admin', requiresAuth: true } },
  { path: '/admin/audit', name: 'adminAudit', component: AdminDashboard, meta: { title: '题库审核', identity: 'admin', requiresAuth: true } },
  { path: '/admin/logs', name: 'adminLogs', component: AdminDashboard, meta: { title: '管理记录', identity: 'admin', requiresAuth: true } },
  { path: '/admin/teacher/:teacherId', name: 'adminTeacherDetail', component: AdminDashboard, meta: { title: '教师详情', identity: 'admin', requiresAuth: true } },
  { path: '/admin/teacher/:teacherId/course/:courseId', name: 'adminTeacherCourseDetail', component: AdminDashboard, meta: { title: '教师课程详情', identity: 'admin', requiresAuth: true } },
  { path: '/:pathMatch(.*)*', redirect: '/' }
]

const router = createRouter({
  history: createWebHashHistory(),
  routes,
  scrollBehavior() { return { top: 0 } }
})

export default router
