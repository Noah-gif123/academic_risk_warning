<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import NavItem from './NavItem.vue'
import { useEntranceAnimation } from './useNavAnimation.js'
import gsap from 'gsap'

const props = defineProps({
  color: { type: String, default: '#6b5a4b' },
  backgroundColor: { type: String, default: 'rgba(255,254,249,0.92)' },
  logoText: { type: String, default: '学情预警系统' },
  navItems: { type: Array, default: () => [] },
  activeKey: { type: String, default: '' },
  showNotificationBell: { type: Boolean, default: false },
  unreadCount: { type: Number, default: 0 },
})

const emit = defineEmits(['navigate', 'toggle-notifications', 'open-profile'])

const navRef = ref(null)
const logoRef = ref(null)
const itemRefs = ref([])

// 给每个 item 设置 ref（取组件根 DOM 元素）
function setItemRef(el, index) {
  if (el) {
    itemRefs.value[index] = el.$el || el
  }
}

const { play: playEntrance } = useEntranceAnimation(navRef, logoRef, itemRefs)

onMounted(() => {
  playEntrance()
})

function onLogoHover() {
  gsap.to(logoRef.value, {
    rotation: 360,
    scale: 1.1,
    duration: 0.6,
    ease: 'power2.inOut',
    overwrite: 'auto',
  })
}

function onLogoLeave() {
  gsap.to(logoRef.value, {
    rotation: 0,
    scale: 1,
    duration: 0.35,
    ease: 'power2.out',
    overwrite: 'auto',
  })
}
</script>

<template>
  <nav
    ref="navRef"
    class="flex items-center justify-between px-6 py-2.5 backdrop-blur-lg"
    :style="{
      background: backgroundColor,
      borderRadius: '999px',
      boxShadow: '0 2px 12px rgba(0,0,0,0.06), 0 0 0 1px ' + color + '1a',
      willChange: 'transform',
    }"
  >
    <!-- Logo 区域 -->
    <div
      ref="logoRef"
      class="flex items-center gap-3 cursor-pointer select-none shrink-0"
      :style="{ willChange: 'transform' }"
      @mouseenter="onLogoHover"
      @mouseleave="onLogoLeave"
    >
      <div
        class="flex items-center justify-center text-white font-bold shrink-0"
        :style="{
          width: '40px',
          height: '40px',
          borderRadius: '14px',
          background: color,
          fontSize: '20px',
          letterSpacing: '-1px',
        }"
      >学</div>
      <span
        class="font-bold whitespace-nowrap hidden sm:inline text-[1.15rem] tracking-wider"
        :style="{ color: '#4a3f35' }"
      >{{ logoText }}</span>
    </div>

    <!-- 导航项列表 -->
    <div class="flex items-center gap-1">
      <NavItem
        v-for="(item, index) in navItems"
        :key="item.keyName"
        :ref="(el) => setItemRef(el, index)"
        :key-name="item.keyName"
        :label="item.label"
        :icon="item.icon"
        :active="activeKey === item.keyName"
        :color="color"
        :type="item.type || 'internal'"
        :href="item.href"
        :badge="item.badge"
        @click="emit('navigate', $event)"
      />

      <!-- 通知铃铛 -->
      <button
        v-if="showNotificationBell"
        class="relative flex items-center justify-center ml-1 border-none cursor-pointer rounded-full shrink-0"
        :style="{
          width: '38px',
          height: '38px',
          background: 'transparent',
          fontSize: '1.1rem',
          fontFamily: 'inherit',
        }"
        @click="emit('toggle-notifications')"
      >
        🔔
        <span
          v-if="unreadCount > 0"
          class="absolute -top-0.5 -right-0.5 flex items-center justify-center text-white font-semibold rounded-full leading-none"
          :style="{
            background: color,
            minWidth: '18px',
            height: '18px',
            fontSize: '11px',
            padding: '0 4px',
          }"
        >{{ unreadCount > 99 ? '99+' : unreadCount }}</span>
      </button>

      <!-- 个人中心 -->
      <button
        v-if="showNotificationBell"
        class="flex items-center justify-center ml-0.5 border-none cursor-pointer rounded-full shrink-0"
        :style="{
          width: '38px',
          height: '38px',
          background: 'transparent',
          fontSize: '1.1rem',
          fontFamily: 'inherit',
        }"
        @click="emit('open-profile')"
      >👤</button>
    </div>
  </nav>
</template>

<style scoped>
button {
  appearance: none;
  -webkit-appearance: none;
  font-family: inherit;
  cursor: pointer;
  outline: none;
  transition: opacity 0.2s;
}
button:hover {
  opacity: 0.75;
}
</style>
