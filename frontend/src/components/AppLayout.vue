<script setup>
import { ref, computed, watch, onMounted, onUnmounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useAuth } from '../composables/useAuth.js'
import PasswordModal from './PasswordModal.vue'
import NotificationPanel from './NotificationPanel.vue'
import GlobalAIRegion from './GlobalAIRegion.vue'

const router = useRouter()
const route = useRoute()
const { identity, isLoggedIn, userDisplayName, isTeacherLoggedIn, isStudentLoggedIn, isAdminLoggedIn } = useAuth()

const props = defineProps({
  showLanding: { type: Boolean, default: false },
  pageMessage: { type: String, default: '' },
  pageMessageType: { type: String, default: 'info' }
})

defineEmits(['logout', 'open-profile', 'select-menu'])

const sidebarCollapsed = ref(false)
function toggleSidebar() { sidebarCollapsed.value = !sidebarCollapsed.value }

const showProfileModal = ref(false)
const showNotifications = ref(false)

function toggleNotifications() { showNotifications.value = !showNotifications.value }

// ===== AI 助手面板 =====
const aiRegionRef = ref(null)
function toggleAIPanel() {
  aiRegionRef.value?.toggle()
}

// Ctrl+K 快捷键唤起 AI 助手
function handleKeydown(e) {
  if (e.ctrlKey && e.key === 'k' && isLoggedIn.value) {
    e.preventDefault()
    aiRegionRef.value?.open()
  }
}
onMounted(() => window.addEventListener('keydown', handleKeydown))
onUnmounted(() => window.removeEventListener('keydown', handleKeydown))

// ===== 课程选择状态（响应式，与 localStorage 双向同步） =====
const selectedCourse = ref(JSON.parse(localStorage.getItem('selectedCourse') || 'null'))

// 监听路由变化，同步 localStorage 中的课程选择到响应式状态
watch(() => route.path, () => {
  selectedCourse.value = JSON.parse(localStorage.getItem('selectedCourse') || 'null')
})

// 课程列表缓存
const teacherCoursesCache = ref([])

const sidebarMenu = ref(route.query.menu || (route.path.startsWith('/teacher/course/') ? 'students' : 'courses') ||
  (route.path.startsWith('/admin/') ? route.path.split('/')[2] || 'dashboard' : 'courses'))
function selectMenu(key) {
  sidebarMenu.value = key
  if (isTeacherLoggedIn.value) {
    if (key === 'courses') {
      router.push('/teacher/courses')
    } else if (selectedCourse.value) {
      router.push({ path: `/teacher/course/${selectedCourse.value.courseId}`, query: { menu: key } })
    } else {
      router.push('/teacher/courses')
    }
  } else if (isAdminLoggedIn.value) {
    const routeMap = { dashboard: '/admin/dashboard', config: '/admin/config', audit: '/admin/audit', logs: '/admin/logs' }
    if (routeMap[key]) router.push(routeMap[key])
  }
}

watch(() => route.query.menu, (newMenu) => {
  if (newMenu) sidebarMenu.value = newMenu
})

// 路由变化时（无 menu 参数），根据当前页面设置侧边栏默认高亮
watch(() => route.path, (newPath) => {
  if (!route.query.menu) {
    if (newPath.startsWith('/admin/')) {
      sidebarMenu.value = newPath.split('/')[2] || 'dashboard'
    } else {
      sidebarMenu.value = newPath.startsWith('/teacher/course/') ? 'students' : 'courses'
    }
  }
})

// 面包屑
const breadcrumb = computed(() => {
  const parts = []
  if (isTeacherLoggedIn.value) {
    if (sidebarMenu.value !== 'courses' && selectedCourse.value) {
      parts.push({ label: '课程总览', action: () => router.push('/teacher/courses') })
      parts.push({ label: selectedCourse.value.courseName })
    }
  }
  return parts
})

// Sidebar items
const sidebarItems = computed(() => {
  if (!isLoggedIn.value) return []
  if (isTeacherLoggedIn.value) {
    if (!selectedCourse.value) return [{ key: 'courses', label: '课程总览', icon: '📖' }]
    return [
      { key: 'courses', label: '课程总览', icon: '📖' },
      { key: 'grading', label: '待批改练习', icon: '✏️' },
      { key: 'students', label: '学生学情', icon: '👨‍🎓' },
      { key: 'warnings', label: '预警管理', icon: '🚨' },
      { key: 'dashboard', label: '数据分析', icon: '📊' },
      { key: 'summary', label: '汇总报告', icon: '📋' },
      { key: 'knowledge', label: '知识点管理', icon: '📚' },
      { key: 'exercises', label: '题库管理', icon: '📝' },
      { key: 'import', label: '数据导入', icon: '📤' },
    ]
  }
  if (isStudentLoggedIn.value) return [
    { key: 'info', label: '我的学情', icon: '📊' },
    { key: 'warnings', label: '我的预警', icon: '🚨' },
    { key: 'exercises', label: '我的练习', icon: '📝' },
    { key: 'goals', label: '我的目标', icon: '🎯' },
  ]
  if (isAdminLoggedIn.value) return [
    { key: 'dashboard', label: '教师总览', icon: '👥' },
    { key: 'config', label: '规则配置', icon: '⚙' },
    { key: 'audit', label: '题库审核', icon: '✅' },
    { key: 'logs', label: '管理记录', icon: '📋' },
  ]
  return []
})
</script>

<template>
  <div class="app-layout">
    <!-- ===== 顶栏 ===== -->
    <header class="app-header">
      <span class="app-header-logo">🎓 学情预警系统</span>
      <div class="app-header-right">
        <!-- AI 助手入口按钮 -->
        <button
          class="app-header-btn ai-entry-btn"
          @click="toggleAIPanel"
          title="AI 智能助手 (Ctrl+K)"
          v-if="isLoggedIn && (isTeacherLoggedIn || isStudentLoggedIn)"
        >
          <span class="ai-entry-icon">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8">
              <path d="M12 2C6.5 2 2 6.5 2 12s4.5 10 10 10 10-4.5 10-10S17.5 2 12 2z"/>
              <circle cx="8.5" cy="9.5" r="1.5"/><circle cx="15.5" cy="9.5" r="1.5"/>
              <path d="M8 14c0 0 1.5 2 4 2s4-2 4-2" stroke-linecap="round"/>
            </svg>
          </span>
          <span class="ai-entry-pulse"></span>
          <span class="ai-entry-label">AI</span>
          <kbd class="ai-entry-kbd">⌘K</kbd>
        </button>

        <button class="app-header-btn" @click="toggleNotifications" v-if="isLoggedIn">
          🔔 消息
        </button>
        <span class="app-user-name" @click="showProfileModal = true" v-if="isLoggedIn">👤 {{ userDisplayName }}</span>
      </div>
    </header>

    <div class="app-body">
      <!-- 侧边栏 -->
      <aside class="sidebar" v-if="isLoggedIn" :class="{ collapsed: sidebarCollapsed }">
        <button class="sidebar-toggle" @click="toggleSidebar" :title="sidebarCollapsed ? '展开菜单' : '收起菜单'">
          <span class="sidebar-icon">{{ sidebarCollapsed ? '▶' : '◀' }}</span>
          <span class="sidebar-label" v-show="!sidebarCollapsed">收起</span>
        </button>
        <button v-for="item in sidebarItems" :key="item.key"
                class="sidebar-item" :class="{ active: sidebarMenu === item.key }"
                @click="$emit('select-menu', item.key); selectMenu(item.key)">
          <span class="sidebar-icon">{{ item.icon }}</span>
          <span class="sidebar-label" v-show="!sidebarCollapsed">{{ item.label }}</span>
        </button>
      </aside>

      <!-- 内容区 -->
      <main class="main-content">
        <nav v-if="breadcrumb.length" class="breadcrumb">
          <template v-for="(b, i) in breadcrumb" :key="i">
            <span v-if="i > 0" class="breadcrumb-sep">›</span>
            <span v-if="b.action" class="breadcrumb-link" @click="b.action()">{{ b.label }}</span>
            <span v-else class="breadcrumb-current">{{ b.label }}</span>
          </template>
        </nav>
        <slot></slot>
      </main>

      <!-- ===== AI 右侧面板区域 ===== -->
      <GlobalAIRegion ref="aiRegionRef" />
    </div>

    <!-- 通知面板 -->
    <NotificationPanel v-if="showNotifications" @close="showNotifications = false" />

    <!-- 个人中心模态框 -->
    <PasswordModal v-if="showProfileModal" @close="showProfileModal = false" />
  </div>
</template>

<style scoped>
/* ================================================================
   AppLayout — 主布局（顶栏 + 侧边栏 + 内容区 + AI 面板）
   ================================================================ */
.app-layout { display: flex; flex-direction: column; height: 100vh; overflow: hidden; }

/* --- 顶栏 --- */
.app-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 24px;
  height: 56px;
  background: var(--bg-header);
  border-bottom: 1px solid var(--border-light);
  box-shadow: var(--shadow-sm);
  flex-shrink: 0;
  z-index: 50;
}

.app-header-logo {
  font-size: 18px;
  font-weight: 700;
  color: var(--text-primary);
  letter-spacing: -0.5px;
}

.app-header-right { display: flex; align-items: center; gap: 8px; }

.app-header-btn {
  padding: 7px 14px;
  font-size: 13px;
  color: var(--text-secondary);
  background: var(--bg-card-subtle);
  border: 1px solid var(--border-light);
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s ease;
}
.app-header-btn:hover { color: var(--accent-primary); border-color: var(--accent-primary); background: var(--accent-light); }

/* ===== AI 入口按钮 ===== */
.ai-entry-btn {
  position: relative;
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 7px 12px;
  background: linear-gradient(135deg, rgba(99, 102, 241, 0.06), rgba(139, 92, 246, 0.06));
  border-color: rgba(99, 102, 241, 0.2);
  color: #6366f1;
}

.ai-entry-btn:hover {
  background: linear-gradient(135deg, rgba(99, 102, 241, 0.12), rgba(139, 92, 246, 0.1));
  border-color: #6366f1;
  color: #4f46e5;
  box-shadow: 0 2px 10px rgba(99, 102, 241, 0.15);
}

.ai-entry-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 22px;
  height: 22px;
}

.ai-entry-icon svg {
  width: 20px;
  height: 20px;
}

/* 脉冲指示灯 */
.ai-entry-pulse {
  position: absolute;
  top: 6px;
  right: 6px;
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #6366f1;
  box-shadow: 0 0 0 0 rgba(99, 102, 241, 0.6);
  animation: aiPulse 2s ease-in-out infinite;
}

@keyframes aiPulse {
  0% { box-shadow: 0 0 0 0 rgba(99, 102, 241, 0.6); }
  70% { box-shadow: 0 0 0 6px rgba(99, 102, 241, 0); }
  100% { box-shadow: 0 0 0 0 rgba(99, 102, 241, 0); }
}

.ai-entry-label {
  font-size: 13px;
  font-weight: 600;
  letter-spacing: 0.3px;
}

.ai-entry-kbd {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: 2px 6px;
  font-size: 10px;
  font-family: inherit;
  font-weight: 500;
  color: #a5a3d4;
  background: rgba(99, 102, 241, 0.08);
  border: 1px solid rgba(99, 102, 241, 0.15);
  border-radius: 4px;
  letter-spacing: 0;
  line-height: 1.4;
}

.app-user-name {
  font-size: 13px;
  font-weight: 500;
  color: var(--text-secondary);
  cursor: pointer;
  padding: 7px 12px;
  border-radius: 8px;
  transition: all 0.2s ease;
}
.app-user-name:hover { background: var(--bg-hover); color: var(--text-primary); }

/* --- 主体区域 --- */
.app-body {
  display: flex;
  flex: 1;
  overflow: hidden;
}

/* --- 侧边栏 --- */
.sidebar {
  width: 220px;
  display: flex;
  flex-direction: column;
  padding: 12px 10px;
  background: var(--bg-sidebar);
  overflow-y: auto;
  transition: width 0.2s ease;
  flex-shrink: 0;
  gap: 2px;
}

.sidebar.collapsed { width: 60px; }

.sidebar-toggle {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 10px 8px;
  margin-bottom: 8px;
  font-size: 11px;
  color: var(--sidebar-toggle-text);
  background: var(--sidebar-toggle-bg);
  border: 1px solid var(--border-sidebar);
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s ease;
  width: 100%;
}
.sidebar-toggle:hover { background: var(--sidebar-item-hover-bg); color: #fff; }

.sidebar-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 12px;
  font-size: 14px;
  color: var(--text-sidebar);
  background: transparent;
  border: none;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.15s ease;
  text-align: left;
  width: 100%;
  white-space: nowrap;
}

.sidebar-item:hover { background: var(--sidebar-item-hover-bg); color: #fff; }

.sidebar-item.active {
  background: var(--sidebar-item-active-bg);
  color: var(--text-sidebar-active);
  font-weight: 600;
}

.sidebar-icon { font-size: 18px; flex-shrink: 0; width: 24px; text-align: center; }
.sidebar-label { overflow: hidden; text-overflow: ellipsis; }

/* --- 内容区 --- */
.main-content {
  flex: 1;
  padding: 20px 24px;
  overflow-y: auto;
  background: var(--bg-page);
  min-width: 0;
}

/* --- 面包屑 --- */
.breadcrumb {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
  margin-bottom: 16px;
  color: var(--text-tertiary);
}

.breadcrumb-sep { color: var(--text-muted); font-weight: 300; }

.breadcrumb-link {
  color: var(--accent-primary);
  cursor: pointer;
  transition: color 0.15s ease;
}
.breadcrumb-link:hover { color: var(--accent-hover); text-decoration: underline; }

.breadcrumb-current { font-weight: 600; color: var(--text-primary); }
</style>
