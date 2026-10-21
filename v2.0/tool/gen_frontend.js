/* gen_frontend.js — 生成 v2.0 前端骨架（Vue 3 + Vite + Tailwind，四端外壳 + 登录） */
'use strict';
const fs = require('fs');
const path = require('path');

const FE = 'E:/JavaDEVIDEA/workspace/JavaProject/academic_risk_warning/v2.0/frontend';
function w(rel, content) {
  const p = path.join(FE, rel);
  fs.mkdirSync(path.dirname(p), { recursive: true });
  fs.writeFileSync(p, content.replace(/^\n/, ''), 'utf8');
}

// ---------------------------------------------------------------- 工程配置
w('package.json', `
{
  "name": "academic-risk-warning-web",
  "private": true,
  "version": "2.0.0",
  "type": "module",
  "scripts": {
    "dev": "vite",
    "build": "vite build",
    "preview": "vite preview"
  },
  "dependencies": {
    "autoprefixer": "^10.5.0",
    "postcss": "^8.5.15",
    "tailwindcss": "^3.4.19",
    "vue": "^3.4.21",
    "vue-router": "^4.6.4"
  },
  "devDependencies": {
    "@vitejs/plugin-vue": "^4.6.2",
    "vite": "^4.5.3"
  },
  "engines": {
    "node": ">=16.0.0"
  }
}
`);

w('vite.config.js', `
import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

export default defineConfig({
  plugins: [vue()],
  server: {
    port: 5173,
    proxy: {
      '/api': { target: 'http://127.0.0.1:8080', changeOrigin: true }
    }
  },
  build: { outDir: 'dist', chunkSizeWarningLimit: 1500 }
})
`);

w('tailwind.config.js', `
/** @type {import('tailwindcss').Config} */
export default {
  content: ['./index.html', './src/**/*.{vue,js}'],
  theme: {
    extend: {
      colors: {
        brand: {
          50: '#eff6ff',
          100: '#dbeafe',
          500: '#2b6cb0',
          600: '#2c5282',
          700: '#1a365d'
        }
      }
    }
  },
  plugins: []
}
`);

w('postcss.config.js', `
export default {
  plugins: {
    tailwindcss: {},
    autoprefixer: {}
  }
}
`);

w('index.html', `
<!DOCTYPE html>
<html lang="zh-CN">
  <head>
    <meta charset="UTF-8" />
    <meta name="viewport" content="width=device-width, initial-scale=1.0" />
    <title>学情预警系统</title>
  </head>
  <body>
    <div id="app"></div>
    <script type="module" src="/src/main.js"></script>
  </body>
</html>
`);

// ---------------------------------------------------------------- 入口与样式
w('src/main.js', `
import { createApp } from 'vue'
import App from './App.vue'
import router from './router'
import './style.css'

createApp(App).use(router).mount('#app')
`);

w('src/App.vue', `
<template>
  <router-view />
</template>
`);

w('src/style.css', `
@tailwind base;
@tailwind components;
@tailwind utilities;

@layer base {
  html, body, #app { height: 100%; }
  body { @apply bg-slate-100 text-slate-800; font-family: 'Microsoft YaHei', 'PingFang SC', system-ui, sans-serif; }
}

@layer components {
  .card { @apply bg-white rounded-xl shadow-sm border border-slate-200; }
  .btn { @apply inline-flex items-center justify-center rounded-lg px-4 py-2 text-sm font-medium transition; }
  .btn-primary { @apply btn bg-brand-500 text-white hover:bg-brand-600; }
  .btn-ghost { @apply btn border border-slate-300 text-slate-600 hover:bg-slate-50; }
  .input { @apply w-full rounded-lg border border-slate-300 px-3 py-2 text-sm outline-none focus:border-brand-500 focus:ring-2 focus:ring-brand-100; }
  .tag { @apply inline-flex items-center rounded-full px-2 py-0.5 text-xs; }
}
`);

// ---------------------------------------------------------------- 会话与接口
w('src/api/http.js', `
const BASE = '/api'

export function getToken() {
  return localStorage.getItem('token') || ''
}

export function clearSession() {
  localStorage.removeItem('token')
  localStorage.removeItem('role')
  localStorage.removeItem('realName')
  localStorage.removeItem('refId')
}

async function request(method, url, body, options = {}) {
  const headers = { 'Content-Type': 'application/json', ...(options.headers || {}) }
  const token = getToken()
  if (token) headers.Authorization = 'Bearer ' + token

  const res = await fetch(BASE + url, {
    method,
    headers,
    body: body === undefined ? undefined : JSON.stringify(body)
  })

  if (res.status === 401) {
    clearSession()
    if (location.hash !== '#/login') location.hash = '#/login'
    throw new Error('未登录或登录已过期')
  }

  const data = await res.json().catch(() => ({ code: res.status, message: '响应解析失败' }))
  if (data.code !== 0) {
    throw new Error(data.message || '请求失败')
  }
  return data.data
}

export const http = {
  get: (url, options) => request('GET', url, undefined, options),
  post: (url, body, options) => request('POST', url, body, options)
}
`);

w('src/api/auth.js', `
import { http } from './http'

export const authApi = {
  login: (loginNo, password) => http.post('/auth/login', { loginNo, password }),
  me: () => http.get('/auth/me'),
  logout: () => http.post('/auth/logout'),
  changePassword: (oldPassword, newPassword) => http.post('/auth/change-password', { oldPassword, newPassword })
}
`);

w('src/session.js', `
import { reactive } from 'vue'

export const ROLES = {
  STUDENT: { key: 'STUDENT', label: '学生', home: '/student/home' },
  TEACHER: { key: 'TEACHER', label: '教师', home: '/teacher/home' },
  COUNSELOR: { key: 'COUNSELOR', label: '辅导员', home: '/counselor/home' },
  ADMIN: { key: 'ADMIN', label: '教务', home: '/admin/home' }
}

export const session = reactive({
  token: localStorage.getItem('token') || '',
  role: localStorage.getItem('role') || '',
  realName: localStorage.getItem('realName') || '',
  refId: localStorage.getItem('refId') || ''
})

export function saveSession(vo) {
  session.token = vo.token || session.token
  session.role = vo.role || ''
  session.realName = vo.realName || ''
  session.refId = vo.refId == null ? '' : String(vo.refId)
  localStorage.setItem('token', session.token)
  localStorage.setItem('role', session.role)
  localStorage.setItem('realName', session.realName)
  localStorage.setItem('refId', session.refId)
}

export function clearSession() {
  session.token = ''
  session.role = ''
  session.realName = ''
  session.refId = ''
  localStorage.removeItem('token')
  localStorage.removeItem('role')
  localStorage.removeItem('realName')
  localStorage.removeItem('refId')
}

export function homeOf(role) {
  return (ROLES[role] || ROLES.STUDENT).home
}
`);

// ---------------------------------------------------------------- 导航配置
w('src/nav.js', `
/** 四端导航项（按口径冻结版《功能整理》） */
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
  TEACHER: [
    { text: '首页', path: '/teacher/home' },
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
  ADMIN: [
    { text: '首页', path: '/admin/home' },
    { text: '基础数据', path: '/admin/base' },
    { text: '排课与教学班', path: '/admin/offerings' },
    { text: '成绩构成', path: '/admin/schemes' },
    { text: '考勤归档', path: '/admin/attendance' },
    { text: '覆盖率督办', path: '/admin/coverage' },
    { text: '报表', path: '/admin/reports' },
    { text: '规则配置', path: '/admin/rules' },
    { text: '流程与问卷', path: '/admin/workflow' },
    { text: '审计', path: '/admin/audit' }
  ]
}
`);

// ---------------------------------------------------------------- 路由
w('src/router/index.js', `
import { createRouter, createWebHashHistory } from 'vue-router'
import { session, homeOf } from '../session'

const Placeholder = () => import('../pages/Placeholder.vue')

const routes = [
  { path: '/', redirect: '/login' },
  { path: '/login', component: () => import('../pages/Login.vue'), meta: { public: true } },

  { path: '/student', component: () => import('../layouts/PortLayout.vue'), props: { role: 'STUDENT' }, children: [
    { path: 'home', component: () => import('../pages/student/Home.vue'), meta: { title: '首页' } },
    { path: 'courses', component: Placeholder, meta: { title: '课程学习' } },
    { path: 'exams', component: Placeholder, meta: { title: '我的考试' } },
    { path: 'questionnaires', component: Placeholder, meta: { title: '问卷调查' } },
    { path: 'warnings', component: Placeholder, meta: { title: '学情预警' } }
  ] },

  { path: '/teacher', component: () => import('../layouts/PortLayout.vue'), props: { role: 'TEACHER' }, children: [
    { path: 'home', component: () => import('../pages/teacher/Home.vue'), meta: { title: '首页' } },
    { path: 'monitor', component: Placeholder, meta: { title: '预警监控' } }
  ] },

  { path: '/counselor', component: () => import('../layouts/PortLayout.vue'), props: { role: 'COUNSELOR' }, children: [
    { path: 'home', component: () => import('../pages/counselor/Home.vue'), meta: { title: '首页看板' } },
    { path: 'focus', component: Placeholder, meta: { title: '重点关注' } },
    { path: 'classes', component: Placeholder, meta: { title: '班级管理' } },
    { path: 'interventions', component: Placeholder, meta: { title: '干预记录' } },
    { path: 'questionnaires', component: Placeholder, meta: { title: '问卷' } },
    { path: 'notices', component: Placeholder, meta: { title: '通知' } }
  ] },

  { path: '/admin', component: () => import('../layouts/PortLayout.vue'), props: { role: 'ADMIN' }, children: [
    { path: 'home', component: () => import('../pages/admin/Home.vue'), meta: { title: '首页' } },
    { path: 'base', component: Placeholder, meta: { title: '基础数据' } },
    { path: 'offerings', component: Placeholder, meta: { title: '排课与教学班' } },
    { path: 'schemes', component: Placeholder, meta: { title: '成绩构成' } },
    { path: 'attendance', component: Placeholder, meta: { title: '考勤归档' } },
    { path: 'coverage', component: Placeholder, meta: { title: '覆盖率督办' } },
    { path: 'reports', component: Placeholder, meta: { title: '报表' } },
    { path: 'rules', component: Placeholder, meta: { title: '规则配置' } },
    { path: 'workflow', component: Placeholder, meta: { title: '流程与问卷' } },
    { path: 'audit', component: Placeholder, meta: { title: '审计' } }
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
`);

// ---------------------------------------------------------------- 布局
w('src/layouts/PortLayout.vue', `
<template>
  <div class="min-h-full flex flex-col">
    <header class="bg-white border-b border-slate-200">
      <div class="max-w-[1280px] mx-auto px-6 h-14 flex items-center gap-8">
        <div class="flex items-center gap-2">
          <span class="w-7 h-7 rounded-lg bg-brand-500 text-white text-xs flex items-center justify-center font-bold">学</span>
          <span class="font-semibold text-slate-800">学情预警系统</span>
          <span class="tag bg-brand-50 text-brand-600">{{ roleLabel }}</span>
        </div>

        <nav class="flex items-center gap-1 flex-1">
          <template v-for="item in navItems" :key="item.text">
            <router-link
              :to="item.path"
              class="px-3 py-1.5 rounded-lg text-sm"
              :class="isActive(item.path) ? 'bg-brand-50 text-brand-600 font-medium' : 'text-slate-600 hover:bg-slate-50'">
              {{ item.text }}
            </router-link>
          </template>
        </nav>

        <div class="flex items-center gap-3 text-sm text-slate-500">
          <span class="cursor-default">消息</span>
          <span class="cursor-default">帮助</span>
          <div class="relative">
            <button class="flex items-center gap-2" @click="menuOpen = !menuOpen">
              <span class="w-7 h-7 rounded-full bg-slate-200 flex items-center justify-center text-xs text-slate-600">
                {{ (session.realName || '?').slice(0, 1) }}
              </span>
              <span class="text-slate-700">{{ session.realName }}</span>
            </button>
            <div v-if="menuOpen" class="absolute right-0 mt-2 w-36 card py-1 z-20">
              <button class="w-full text-left px-4 py-2 text-sm hover:bg-slate-50" @click="goProfile">个人中心</button>
              <button class="w-full text-left px-4 py-2 text-sm text-red-600 hover:bg-slate-50" @click="logout">退出系统</button>
            </div>
          </div>
        </div>
      </div>
    </header>

    <main class="flex-1 max-w-[1280px] w-full mx-auto px-6 py-6">
      <router-view />
    </main>

    <footer class="text-center text-xs text-slate-400 py-4">学情预警系统 v2.0 · 四端统一入口</footer>
  </div>
</template>

<script setup>
import { computed, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { NAV } from '../nav'
import { ROLES, clearSession, session } from '../session'
import { authApi } from '../api/auth'

const props = defineProps({ role: { type: String, required: true } })
const route = useRoute()
const router = useRouter()
const menuOpen = ref(false)

const navItems = computed(() => (NAV[props.role] || []).filter(i => !i.children))
const roleLabel = computed(() => (ROLES[props.role] || {}).label || props.role)
const isActive = (p) => route.path === p

function goProfile() {
  menuOpen.value = false
}

async function logout() {
  menuOpen.value = false
  try { await authApi.logout() } catch (e) { /* 忽略 */ }
  clearSession()
  router.push('/login')
}
</script>
`);

// ---------------------------------------------------------------- 页面
w('src/pages/Login.vue', `
<template>
  <div class="min-h-full grid grid-cols-1 lg:grid-cols-2">
    <div class="hidden lg:flex flex-col justify-center px-16 bg-gradient-to-br from-brand-700 to-brand-500 text-white">
      <h1 class="text-3xl font-bold mb-3">学情预警系统</h1>
      <p class="text-white/80 leading-relaxed">
        四端统一入口：教师发布 → 学生提交 → 教师批改 → 数据入库，<br />
        在学期进行中提前发现学业风险。
      </p>
      <p class="mt-8 text-sm text-white/60">v2.0 · 学生 / 教师 / 辅导员 / 教务</p>
    </div>

    <div class="flex items-center justify-center px-6">
      <div class="w-full max-w-sm">
        <h2 class="text-xl font-semibold mb-1">欢迎使用</h2>
        <p class="text-sm text-slate-500 mb-6">学校名称占位 · 请使用学号 / 工号 / 教务账号登录</p>

        <form @submit.prevent="submit">
          <label class="block text-sm text-slate-600 mb-1">登录号</label>
          <input v-model="loginNo" class="input mb-4" placeholder="学号 / 教职工号 / 教务账号" autocomplete="username" />

          <label class="block text-sm text-slate-600 mb-1">密码</label>
          <input v-model="password" type="password" class="input mb-2" placeholder="初始密码 000000" autocomplete="current-password" />

          <p v-if="error" class="text-sm text-red-600 mb-3">{{ error }}</p>

          <button type="submit" class="btn-primary w-full mt-2" :disabled="loading">
            {{ loading ? '登录中…' : '登录' }}
          </button>
        </form>

        <p class="text-xs text-slate-400 mt-6">首次登录后请点击右上角头像修改密码</p>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { authApi } from '../api/auth'
import { homeOf, saveSession } from '../session'

const router = useRouter()
const loginNo = ref('')
const password = ref('000000')
const loading = ref(false)
const error = ref('')

async function submit() {
  error.value = ''
  if (!loginNo.value.trim()) { error.value = '请输入登录号'; return }
  loading.value = true
  try {
    const vo = await authApi.login(loginNo.value.trim(), password.value)
    saveSession(vo)
    router.push(homeOf(vo.role))
  } catch (e) {
    error.value = e.message || '登录失败'
  } finally {
    loading.value = false
  }
}
</script>
`);

w('src/pages/Placeholder.vue', `
<template>
  <div class="card p-10 text-center">
    <p class="text-slate-500 text-sm">该页面属于第一阶段后续任务，尚未实现</p>
    <p class="text-xs text-slate-400 mt-2">{{ route.meta.title }} · {{ route.path }}</p>
  </div>
</template>

<script setup>
import { useRoute } from 'vue-router'
const route = useRoute()
</script>
`);

function homePage(role, title, intro, cards) {
  return `
<template>
  <div class="space-y-6">
    <section class="card p-6">
      <h1 class="text-2xl font-semibold">Hello，{{ name }}</h1>
      <p class="text-slate-500 mt-1">${intro}</p>
    </section>

    <section>
      <div class="flex items-center justify-between mb-3">
        <h2 class="text-base font-semibold text-slate-700">${title}</h2>
        <span class="text-xs text-slate-400">第一阶段建设中</span>
      </div>
      <div class="grid grid-cols-1 md:grid-cols-2 xl:grid-cols-4 gap-4">
        <div v-for="c in cards" :key="c" class="card p-5 h-32 flex items-center justify-center text-slate-400 text-sm">
          {{ c }}
        </div>
      </div>
    </section>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { session } from '../../session'
const name = computed(() => session.realName || '同学')
const cards = [${cards.map(c => `'${c}'`).join(', ')}]
</script>
`;
}

w('src/pages/student/Home.vue', homePage(
  'STUDENT', '我的课程',
  '欢迎来到学情预警系统 · 这里将显示本学期课程、我的项目与预警入口',
  ['课程卡片（进行中 / 未开课）', '我的项目（待提交）', '待完成测验与作业', '学情预警入口']
));

w('src/pages/teacher/Home.vue', homePage(
  'TEACHER', '我的教学课程',
  '欢迎来到学情预警系统 · 这里将显示本人任课课程与待批改任务',
  ['教学课程卡片（置顶 / 结课）', '待批改 · 作业', '待批改 · 测试', '待批改 · 项目']
));

w('src/pages/counselor/Home.vue', homePage(
  'COUNSELOR', '重点关注名单',
  '欢迎来到学情预警系统 · 这里将按行政班聚合多科目预警学生',
  ['重点关注（≥3 门课程预警）', '班集体预警占比', '待处理提醒', '干预记录']
));

w('src/pages/admin/Home.vue', homePage(
  'ADMIN', '教务工作台',
  '欢迎来到学情预警系统 · 这里将显示本学期排课、归档与督办概况',
  ['本学期开课与教学班', '考勤归档待处理', '覆盖率督办清单', '规则版本与报表']
));

w('.gitignore', `
node_modules/
dist/
.vite/
*.local
.DS_Store
`);

console.log('前端骨架生成完成');
