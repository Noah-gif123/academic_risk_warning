<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import DesktopNav from './DesktopNav.vue'
import MobileNav from './MobileNav.vue'
import gsap from 'gsap'

/**
 * Vue3 + GSAP 胶囊导航容器
 *
 * Props:
 *   themeColor     — 主题色，会设定 CSS 变量 --nav-theme，所有子组件联动换色
 *   backgroundColor — 胶囊背景色（支持 rgba 毛玻璃）
 *   logoText       — Logo 旁文字
 *   navItems       — 导航项数组 [{ keyName, label, icon?, badge?, type?, href? }]
 *   activeKey      — 当前激活的导航项 key（v-model）
 *   showNotificationBell — 是否显示通知铃铛
 *   unreadCount    — 未读通知数
 *   showProfile    — 是否显示个人中心按钮
 *
 * Emits:
 *   update:activeKey / navigate — 导航切换
 *   toggle-notifications        — 点击铃铛
 *   open-profile                — 点击个人中心
 */

const props = defineProps({
  themeColor: { type: String, default: '#6b5a4b' },
  backgroundColor: { type: String, default: 'rgba(255,254,249,0.92)' },
  logoText: { type: String, default: '学情预警系统' },
  navItems: { type: Array, default: () => [] },
  activeKey: { type: String, default: '' },
  showNotificationBell: { type: Boolean, default: false },
  unreadCount: { type: Number, default: 0 },
  showProfile: { type: Boolean, default: false },
})

const emit = defineEmits(['update:activeKey', 'navigate', 'toggle-notifications', 'open-profile'])

// -----------------------------------------------------------
// 响应式断点
// -----------------------------------------------------------
const MOBILE_BREAKPOINT = 768
const isMobile = ref(false)

function checkMobile() {
  isMobile.value = window.innerWidth < MOBILE_BREAKPOINT
}

let resizeTimer = null
function handleResize() {
  clearTimeout(resizeTimer)
  resizeTimer = setTimeout(checkMobile, 200)
}

onMounted(() => {
  checkMobile()
  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
  clearTimeout(resizeTimer)
  // 清理所有 GSAP 实例
  gsap.globalTimeline.clear()
})

// -----------------------------------------------------------
// 事件传递
// -----------------------------------------------------------
function onNavigate(key) {
  emit('update:activeKey', key)
  emit('navigate', key)
}

// -----------------------------------------------------------
// 公共属性透传给子组件
// -----------------------------------------------------------
const sharedProps = computed(() => ({
  color: props.themeColor,
  backgroundColor: props.backgroundColor,
  logoText: props.logoText,
  navItems: props.navItems,
  activeKey: props.activeKey,
}))
</script>

<template>
  <div
    class="capsule-nav-wrapper"
    :style="{
      '--nav-theme': themeColor,
      marginBottom: '32px',
      position: 'relative',
      zIndex: 100,
    }"
  >
    <!-- 桌面端 -->
    <DesktopNav
      v-if="!isMobile"
      v-bind="sharedProps"
      :show-notification-bell="showNotificationBell"
      :unread-count="unreadCount"
      :show-profile="showProfile"
      @navigate="onNavigate"
      @toggle-notifications="emit('toggle-notifications')"
      @open-profile="emit('open-profile')"
    />

    <!-- 移动端 -->
    <MobileNav
      v-else
      v-bind="sharedProps"
      :show-notification-bell="showNotificationBell"
      :unread-count="unreadCount"
      :show-profile="showProfile"
      @navigate="onNavigate"
      @toggle-notifications="emit('toggle-notifications')"
      @open-profile="emit('open-profile')"
    />
  </div>
</template>

<style scoped>
.capsule-nav-wrapper {
  will-change: transform;
}
</style>
