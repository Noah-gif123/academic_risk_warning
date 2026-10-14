<script setup>
import { ref, computed } from 'vue'
import { useNavItemAnimation } from './useNavAnimation.js'

const props = defineProps({
  /** 导航项唯一标识 */
  keyName: { type: String, required: true },
  /** 显示文本 */
  label: { type: String, required: true },
  /** 可选图标 emoji 或文本 */
  icon: { type: String, default: '' },
  /** 是否激活 */
  active: { type: Boolean, default: false },
  /** 主题色 */
  color: { type: String, default: '#6b5a4b' },
  /** 链接类型：internal 触发 emit，external 用 a 标签 */
  type: { type: String, default: 'internal' },
  /** 外部链接 href（仅 type=external 时有效） */
  href: { type: String, default: '' },
  /** 角标数字 */
  badge: { type: [Number, String], default: null },
})

const emit = defineEmits(['click'])

const itemRef = ref(null)
const circleRef = ref(null)
const textOuterRef = ref(null)
const textInnerRef = ref(null)

const isExternal = computed(() => props.type === 'external' && props.href)

const { onEnter, onLeave } = useNavItemAnimation(itemRef, circleRef, textOuterRef, textInnerRef, props.color)

function handleClick() {
  emit('click', props.keyName)
}
</script>

<template>
  <component
    :is="isExternal ? 'a' : 'button'"
    :href="isExternal ? href : undefined"
    :target="isExternal ? '_blank' : undefined"
    :rel="isExternal ? 'noopener noreferrer' : undefined"
    ref="itemRef"
    class="relative overflow-hidden inline-flex items-center gap-1.5 px-4 py-2 rounded-full cursor-pointer border-none select-none transition-none"
    :class="active ? 'shadow-sm' : ''"
    :style="{
      background: active ? color : 'transparent',
      fontFamily: 'inherit',
      fontSize: '0.88rem',
      fontWeight: 500,
      letterSpacing: '0.02em',
      textDecoration: 'none',
      willChange: 'transform',
    }"
    @mouseenter="!active && onEnter()"
    @mouseleave="!active && onLeave()"
    @click="handleClick"
  >
    <!-- 扩散圆形遮罩 -->
    <span
      ref="circleRef"
      class="absolute rounded-full pointer-events-none"
      :style="{
        width: '100%',
        height: '100%',
        left: 0,
        top: 0,
        background: color,
        transform: 'scale(0)',
        opacity: 0,
        zIndex: 0,
      }"
    ></span>

    <!-- 双层文字容器 -->
    <span class="relative inline-flex items-center overflow-hidden whitespace-nowrap" style="z-index: 1;">
      <!-- 第一层：默认深色文字（悬浮时向上滑出） -->
      <span
        ref="textOuterRef"
        class="inline-flex items-center gap-1.5"
        :style="{ color: active ? '#ffffff' : '#4a3f35', willChange: 'transform, opacity' }"
      >
        <span v-if="icon" class="text-base leading-none">{{ icon }}</span>
        <span>{{ label }}</span>
      </span>
      <!-- 第二层：白色文字（悬浮时从下方滑入） -->
      <span
        ref="textInnerRef"
        class="absolute inset-0 inline-flex items-center gap-1.5"
        :style="{ color: '#ffffff', transform: 'translateY(100%)', opacity: 0, willChange: 'transform, opacity' }"
      >
        <span v-if="icon" class="text-base leading-none">{{ icon }}</span>
        <span>{{ label }}</span>
      </span>
    </span>

    <!-- 角标 -->
    <span
      v-if="badge !== null && badge !== undefined"
      class="relative inline-flex items-center justify-center min-w-[18px] h-[18px] px-1 rounded-full text-white text-[11px] font-semibold leading-none"
      :style="{ background: color, zIndex: 1 }"
    >
      {{ badge }}
    </span>

    <!-- 外部链接箭头 -->
    <span
      v-if="isExternal"
      class="relative text-[10px] opacity-60"
      :style="{ zIndex: 1, color: active ? 'rgba(255,255,255,0.7)' : '#a89880' }"
    >↗</span>
  </component>
</template>

<style scoped>
button {
  appearance: none;
  -webkit-appearance: none;
  font-family: inherit;
  cursor: pointer;
  outline: none;
  line-height: 1.4;
}
</style>
