<template>
  <div class="min-h-full flex flex-col">
    <header class="bg-white border-b border-slate-200">
      <div class="max-w-[1280px] mx-auto px-6 h-14 flex items-center gap-8">
        <div class="flex items-center gap-2">
          <span class="w-7 h-7 rounded-lg bg-brand-500 text-white text-xs flex items-center justify-center font-bold">学</span>
          <span class="font-semibold text-slate-800">学情预警系统</span>
          <span class="tag bg-brand-50 text-brand-600">{{ roleLabel }}</span>
        </div>

        <nav class="flex items-center gap-1 flex-1 min-w-0 overflow-x-auto whitespace-nowrap">
          <template v-for="item in navItems" :key="item.text">
            <!-- 有子项：下拉 -->
            <div v-if="item.children && item.children.length" class="relative shrink-0">
              <button class="px-3 py-1.5 rounded-lg text-sm whitespace-nowrap flex items-center gap-1"
                      :class="isGroupActive(item) ? 'bg-brand-50 text-brand-600 font-medium' : 'text-slate-600 hover:bg-slate-50'"
                      @click="toggleGroup(item.text)">
                {{ item.text }}
                <span class="text-[10px] text-slate-400">▾</span>
              </button>
              <div v-if="openGroup === item.text" class="absolute left-0 mt-1 w-32 card py-1 z-20">
                <button v-for="c in item.children" :key="c.path"
                        class="w-full text-left px-4 py-2 text-sm hover:bg-slate-50"
                        :class="isActive(c.path) ? 'text-brand-600 font-medium' : 'text-slate-600'"
                        @click="goPath(c.path)">{{ c.text }}</button>
              </div>
            </div>
            <!-- 普通项 -->
            <router-link v-else
              :to="item.path"
              class="px-3 py-1.5 rounded-lg text-sm whitespace-nowrap shrink-0"
              :class="isActive(item.path) ? 'bg-brand-50 text-brand-600 font-medium' : 'text-slate-600 hover:bg-slate-50'">
              {{ item.text }}
            </router-link>
          </template>
        </nav>

        <div class="flex items-center gap-3 text-sm text-slate-500 shrink-0">
          <button class="relative flex items-center gap-1 px-2 py-1 rounded-lg hover:bg-slate-50"
                  :class="isActive(noticePath) ? 'text-brand-600' : 'text-slate-600'"
                  title="消息中心" @click="goNotices">
            <svg viewBox="0 0 24 24" class="w-4 h-4" fill="none" stroke="currentColor" stroke-width="1.8"
                 stroke-linecap="round" stroke-linejoin="round">
              <path d="M18 8a6 6 0 1 0-12 0c0 7-3 9-3 9h18s-3-2-3-9" />
              <path d="M13.7 21a2 2 0 0 1-3.4 0" />
            </svg>
            <span>消息</span>
            <span v-if="unread > 0"
                  class="absolute -top-1 left-3 min-w-[16px] h-4 px-1 rounded-full bg-red-500 text-white text-[10px] leading-4 text-center">
              {{ unread > 99 ? '99+' : unread }}
            </span>
          </button>
          <span class="cursor-help" title="帮助文档与常见问题整理中，可先联系管理员">帮助</span>
          <div class="relative">
            <button class="flex items-center gap-2" @click="menuOpen = !menuOpen">
              <span class="w-7 h-7 rounded-full bg-slate-200 flex items-center justify-center text-xs text-slate-600">
                {{ (session.realName || '?').slice(0, 1) }}
              </span>
              <span class="text-slate-700">{{ session.realName }}</span>
            </button>
            <div v-if="menuOpen" class="absolute right-0 mt-2 w-36 card py-1 z-20">
              <button class="w-full text-left px-4 py-2 text-sm hover:bg-slate-50" @click="goNotices">消息中心</button>
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
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { NAV } from '../nav'
import { ROLES, clearSession, session } from '../session'
import { authApi } from '../api/auth'
import { notificationApi } from '../api/notification'

const props = defineProps({ role: { type: String, required: true } })
const route = useRoute()
const router = useRouter()
const menuOpen = ref(false)
const openGroup = ref('')

// 带 children 的导航项做成下拉（学生端「考试」下拉里有 我的考试 / 问卷调查）
const navItems = computed(() => NAV[props.role] || [])
const roleLabel = computed(() => (ROLES[props.role] || {}).label || props.role)
const isActive = (p) => route.path === p
const isGroupActive = (item) => (item.children || []).some((c) => route.path === c.path)

// ---------------- 消息中心：未读红点 + 轮询 ----------------
const noticePath = computed(() => `/${props.role.toLowerCase()}/notifications`)
const unread = ref(0)
let unreadTimer = null

async function loadUnread() {
  if (!session.token) return
  try {
    const n = await notificationApi.unreadCount()
    unread.value = Number(n || 0)
  } catch (e) {
    // 未读数失败不打断页面（如后端未启动/令牌过期由拦截器处理）
  }
}
function goNotices() {
  menuOpen.value = false
  router.push(noticePath.value)
}

onMounted(() => {
  loadUnread()
  unreadTimer = setInterval(loadUnread, 60000)
})
onBeforeUnmount(() => {
  if (unreadTimer) clearInterval(unreadTimer)
})
// 进入子页面（含从消息中心返回）时刷新红点
watch(() => route.fullPath, () => { loadUnread() })

function toggleGroup(text) {
  openGroup.value = openGroup.value === text ? '' : text
}
function goPath(p) {
  openGroup.value = ''
  router.push(p)
}

function goProfile() {
  menuOpen.value = false
  const prefix = props.role.toLowerCase()
  // 学生端的个人中心已经做好；其他端先跳回各自首页
  router.push(prefix === 'student' ? '/student/profile' : `/${prefix}/home`)
}

async function logout() {
  menuOpen.value = false
  try { await authApi.logout() } catch (e) { /* 忽略 */ }
  clearSession()
  router.push('/login')
}
</script>
