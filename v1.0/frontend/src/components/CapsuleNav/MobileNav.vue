<script setup>
import { ref, onMounted, watch, nextTick } from 'vue'
import NavItem from './NavItem.vue'
import { useHamburgerAnimation, useMobileMenuAnimation, useEntranceAnimation } from './useNavAnimation.js'
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

const isOpen = ref(false)
const navRef = ref(null)
const logoRef = ref(null)
const hamburgerRef = ref(null)
const panelRef = ref(null)
const lineRefs = ref([])
const menuItemRefs = ref([])

function setLineRef(el, i) {
  lineRefs.value[i] = el
}
function setMenuItemRef(el, i) {
  if (el) menuItemRefs.value[i] = el.$el || el
}

const { toggle: toggleHamburger } = useHamburgerAnimation(lineRefs)
const { open: openMenu, close: closeMenu } = useMobileMenuAnimation(panelRef, menuItemRefs, props.color)
const { play: playEntrance } = useEntranceAnimation(navRef, logoRef, [])

onMounted(() => {
  playEntrance()
})

watch(isOpen, async (val) => {
  await nextTick()
  toggleHamburger(val)
  if (val) {
    openMenu()
  } else {
    closeMenu()
  }
})

function handleToggle() {
  isOpen.value = !isOpen.value
}

function handleNavigate(key) {
  emit('navigate', key)
  isOpen.value = false
}

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
    class="flex items-center justify-between px-5 py-2.5 backdrop-blur-lg relative z-50"
    :style="{
      background: backgroundColor,
      borderRadius: '999px',
      boxShadow: '0 2px 12px rgba(0,0,0,0.06), 0 0 0 1px ' + color + '1a',
      willChange: 'transform',
    }"
  >
    <!-- Logo -->
    <div
      ref="logoRef"
      class="flex items-center gap-2.5 cursor-pointer select-none shrink-0"
      :style="{ willChange: 'transform' }"
      @mouseenter="onLogoHover"
      @mouseleave="onLogoLeave"
    >
      <div
        class="flex items-center justify-center text-white font-bold shrink-0"
        :style="{
          width: '36px',
          height: '36px',
          borderRadius: '12px',
          background: color,
          fontSize: '18px',
          letterSpacing: '-1px',
        }"
      >学</div>
      <span
        class="font-bold whitespace-nowrap text-[1.05rem] tracking-wider"
        :style="{ color: '#4a3f35' }"
      >{{ logoText }}</span>
    </div>

    <!-- 通知铃铛 (移动端放在汉堡旁) -->
    <div class="flex items-center gap-2">
      <button
        v-if="showNotificationBell"
        class="relative flex items-center justify-center border-none cursor-pointer rounded-full shrink-0"
        :style="{
          width: '36px',
          height: '36px',
          background: 'transparent',
          fontSize: '1rem',
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
            minWidth: '16px',
            height: '16px',
            fontSize: '10px',
            padding: '0 3px',
          }"
        >{{ unreadCount > 99 ? '99+' : unreadCount }}</span>
      </button>

      <!-- 汉堡按钮（两条线 → ✕） -->
      <button
        ref="hamburgerRef"
        class="relative flex flex-col items-center justify-center gap-[7px] border-none cursor-pointer shrink-0"
        :style="{
          width: '36px',
          height: '36px',
          background: 'transparent',
          fontFamily: 'inherit',
        }"
        @click="handleToggle"
      >
        <span
          v-for="i in 2"
          :key="i"
          :ref="(el) => setLineRef(el, i - 1)"
          class="block rounded-full origin-center"
          :style="{
            width: '22px',
            height: '2.5px',
            background: color,
            borderRadius: '999px',
            willChange: 'transform',
          }"
        ></span>
      </button>
    </div>

    <!-- 折叠菜单面板 -->
    <Teleport to="body">
      <div
        ref="panelRef"
        v-show="isOpen"
        class="fixed inset-x-0 top-0 z-40 backdrop-blur-xl shadow-2xl overflow-auto"
        :style="{
          background: backgroundColor,
          borderBottomLeftRadius: '28px',
          borderBottomRightRadius: '28px',
          padding: '72px 24px 32px',
          maxHeight: '85vh',
          willChange: 'transform',
        }"
      >
        <div class="flex flex-col gap-3">
          <NavItem
            v-for="(item, index) in navItems"
            :key="item.keyName"
            :ref="(el) => setMenuItemRef(el, index)"
            :key-name="item.keyName"
            :label="item.label"
            :icon="item.icon"
            :active="activeKey === item.keyName"
            :color="color"
            :type="item.type || 'internal'"
            :href="item.href"
            :badge="item.badge"
            class="w-full justify-center text-base py-3"
            @click="handleNavigate($event)"
          />

          <!-- 移动端个人中心 -->
          <button
            v-if="showNotificationBell"
            class="flex items-center justify-center gap-2 w-full py-3 border-none cursor-pointer rounded-full mt-2"
            :style="{
              background: 'transparent',
              color: color,
              fontSize: '0.95rem',
              fontWeight: 500,
              fontFamily: 'inherit',
              border: '1px solid ' + color + '33',
            }"
            @click="emit('open-profile'); isOpen = false"
          >👤 个人中心</button>

          <!-- 关闭按钮 -->
          <button
            class="flex items-center justify-center w-full py-2.5 mt-2 border-none cursor-pointer rounded-full font-medium opacity-70 hover:opacity-100"
            :style="{
              background: color + '12',
              color: color,
              fontSize: '0.9rem',
              fontFamily: 'inherit',
            }"
            @click="isOpen = false"
          >关闭菜单</button>
        </div>
      </div>
    </Teleport>

    <!-- 遮罩层 -->
    <Teleport to="body">
      <div
        v-show="isOpen"
        class="fixed inset-0 z-30"
        style="background: rgba(0,0,0,0.2);"
        @click="isOpen = false"
      ></div>
    </Teleport>
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
