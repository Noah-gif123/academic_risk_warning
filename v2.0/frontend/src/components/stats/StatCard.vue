<template>
  <div class="card p-4">
    <div class="text-xs text-slate-500 flex items-center justify-between">
      <slot name="label">{{ label }}</slot>
      <slot name="extra" />
    </div>
    <div class="mt-2 flex items-baseline gap-1">
      <span class="text-2xl font-semibold text-slate-800">{{ display }}</span>
      <span v-if="unit" class="text-xs text-slate-400">{{ unit }}</span>
    </div>
    <div v-if="hint" class="mt-1 text-[11px] text-slate-400">{{ hint }}</div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
const props = defineProps({
  label: { type: String, default: '' },
  value: { type: [Number, String], default: null },
  unit: { type: String, default: '' },
  hint: { type: String, default: '' }
})
const display = computed(() => {
  if (props.value === null || props.value === undefined || props.value === '') return '—'
  const n = Number(props.value)
  return Number.isFinite(n) ? (Math.abs(n) >= 100 ? n.toFixed(1) : n.toFixed(2)) : props.value
})
</script>
