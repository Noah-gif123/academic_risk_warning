<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useAuth } from '../composables/useAuth.js'
import GlobalAIRegion from './GlobalAIRegion.vue'

const router = useRouter()
const route = useRoute()
const { userDisplayName, isStudentLoggedIn, studentLogout } = useAuth()

defineEmits(['logout'])

// ===== Tab 导航 =====
const tabs = [
  { key: 'info', label: '学情', icon: '📊', route: '/student/info' },
  { key: 'warnings', label: '预警', icon: '🚨', route: '/student/warnings' },
  { key: 'exercises', label: '练习', icon: '📝', route: '/student/exercises' },
  { key: 'goals', label: '目标', icon: '🎯', route: '/student/goals' },
]

const activeTab = computed(() => {
  const tabMeta = route.meta?.tab || 'info'
  return tabMeta
})

function switchTab(tab) {
  router.push(tab.route)
}

// ===== AI 助手面板 =====
const aiRegionRef = ref(null)
function toggleAIPanel() {
  aiRegionRef.value?.toggle()
}

function handleKeydown(e) {
  if (e.ctrlKey && e.key === 'k') {
    e.preventDefault()
    aiRegionRef.value?.open()
  }
}
onMounted(() => window.addEventListener('keydown', handleKeydown))
onUnmounted(() => window.removeEventListener('keydown', handleKeydown))

function handleLogout() {
  studentLogout()
  router.push('/')
}
</script>

<template>
  <div class="student-layout">
    <!-- ===== 顶栏 ===== -->
    <header class="student-header">
      <div class="student-header-left">
        <span class="student-logo">🎓</span>
        <span class="student-title">学情预警</span>
      </div>
      <div class="student-header-right">
        <!-- AI 入口 -->
        <button class="student-header-btn ai-btn" @click="toggleAIPanel" title="AI 助手 (Ctrl+K)">
          <span class="ai-dot"></span>
          <span>AI</span>
        </button>
        <!-- 用户 -->
        <span class="student-user">👤 {{ userDisplayName }}</span>
        <!-- 退出 -->
        <button class="student-header-btn logout-btn" @click="handleLogout" title="退出登录">
          🚪
        </button>
      </div>
    </header>

    <!-- ===== 内容区 ===== -->
    <main class="student-main">
      <slot></slot>
    </main>

    <!-- ===== 底部 Tab 栏 ===== -->
    <nav class="student-tabs">
      <button
        v-for="tab in tabs" :key="tab.key"
        class="student-tab-item"
        :class="{ active: activeTab === tab.key }"
        @click="switchTab(tab)"
      >
        <span class="student-tab-icon">{{ tab.icon }}</span>
        <span class="student-tab-label">{{ tab.label }}</span>
      </button>
    </nav>

    <!-- ===== AI 右侧面板（绝对定位，不占用 flex 空间） ===== -->
    <div class="student-ai-wrapper">
      <GlobalAIRegion ref="aiRegionRef" />
    </div>
  </div>
</template>

<style scoped>
.student-layout {
  display: flex;
  flex-direction: column;
  height: 100vh;
  overflow: hidden;
  background: var(--bg-page);
  position: relative;
}

/* ===== 顶栏 ===== */
.student-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 16px;
  height: 52px;
  background: #fff;
  border-bottom: 1px solid var(--border-light);
  flex-shrink: 0;
  z-index: 50;
}

.student-header-left {
  display: flex;
  align-items: center;
  gap: 8px;
}

.student-logo {
  font-size: 22px;
}

.student-title {
  font-size: 16px;
  font-weight: 700;
  color: var(--text-primary);
  letter-spacing: -0.5px;
}

.student-header-right {
  display: flex;
  align-items: center;
  gap: 8px;
}

.student-header-btn {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 6px 12px;
  font-size: 13px;
  font-weight: 500;
  color: var(--text-secondary);
  background: var(--bg-card-subtle);
  border: 1px solid var(--border-light);
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s ease;
}

.student-header-btn:hover {
  color: var(--accent-primary);
  border-color: var(--accent-primary);
  background: var(--accent-light);
}

.ai-btn {
  color: #6366f1;
  border-color: rgba(99, 102, 241, 0.2);
  background: rgba(99, 102, 241, 0.04);
}

.ai-dot {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: #6366f1;
  box-shadow: 0 0 0 0 rgba(99, 102, 241, 0.5);
  animation: pulse 2s ease-in-out infinite;
}

@keyframes pulse {
  0% { box-shadow: 0 0 0 0 rgba(99, 102, 241, 0.5); }
  70% { box-shadow: 0 0 0 5px rgba(99, 102, 241, 0); }
  100% { box-shadow: 0 0 0 0 rgba(99, 102, 241, 0); }
}

.student-user {
  font-size: 13px;
  font-weight: 500;
  color: var(--text-secondary);
  padding: 4px 8px;
  max-width: 100px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.logout-btn {
  width: 32px;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 0;
  font-size: 14px;
}

/* ===== 内容区 ===== */
.student-main {
  flex: 1;
  overflow-y: auto;
  padding: 16px;
  min-width: 0;
}

/* ===== 底部 Tab ===== */
.student-tabs {
  display: flex;
  background: #fff;
  border-top: 1px solid var(--border-light);
  flex-shrink: 0;
  z-index: 50;
}

.student-tab-item {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 2px;
  padding: 8px 4px 6px;
  font-size: 11px;
  font-weight: 500;
  color: var(--text-muted);
  background: none;
  border: none;
  border-top: 2px solid transparent;
  cursor: pointer;
  transition: all 0.2s ease;
  position: relative;
}

.student-tab-item:hover {
  color: var(--text-secondary);
}

.student-tab-item.active {
  color: var(--accent-primary);
  border-top-color: var(--accent-primary);
  font-weight: 600;
}

.student-tab-icon {
  font-size: 22px;
  line-height: 1;
}

.student-tab-label {
  font-size: 11px;
}

/* ===== AI 右侧面板容器（绝对定位，不占用 flex 空间） ===== */
.student-ai-wrapper {
  position: absolute;
  top: 0;
  right: 0;
  bottom: 0;
  pointer-events: none;
  overflow: visible;
  z-index: 100; /* 确保覆盖底部 tabs */
}
.student-ai-wrapper > * {
  pointer-events: auto;
  height: 100%;
}

/* ===== 响应式：宽屏下也保持紧凑 ===== */
@media (min-width: 768px) {
  .student-header {
    padding: 0 32px;
  }

  .student-main {
    padding: 24px 32px;
    max-width: 900px;
    margin: 0 auto;
    width: 100%;
  }

  .student-tabs {
    justify-content: center;
    gap: 0;
  }

  .student-tab-item {
    max-width: 120px;
  }
}
</style>
