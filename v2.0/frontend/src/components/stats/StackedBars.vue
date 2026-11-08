<template>
  <div class="space-y-2">
    <div v-for="(r, i) in rows" :key="i" class="flex items-center gap-2">
      <div class="w-24 shrink-0 text-[11px] text-slate-500 truncate text-right" :title="r.label">{{ r.label }}</div>
      <div class="flex-1 h-5 bg-slate-100 rounded overflow-hidden flex">
        <div v-for="seg in segments(r)" :key="seg.key" :style="{ width: seg.w + '%', background: seg.color }"
             class="h-full" :title="seg.title" />
      </div>
      <div class="w-10 text-[11px] text-slate-400 text-right">{{ r.total }}</div>
    </div>
    <div class="flex items-center gap-4 text-[11px] text-slate-500 pt-1">
      <span v-for="l in legend" :key="l.key"><i class="inline-block w-2.5 h-2.5 rounded-sm mr-1 align-middle"
            :style="{ background: l.color }" />{{ l.text }}</span>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
const props = defineProps({ rows: { type: Array, default: () => [] } })
const legend = [
  { key: 'red', text: '红', color: '#ef4444' },
  { key: 'orange', text: '橙', color: '#f97316' },
  { key: 'yellow', text: '黄', color: '#eab308' },
  { key: 'green', text: '绿', color: '#22c55e' }
]
const maxTotal = computed(() => Math.max(1, ...props.rows.map((r) => Number(r.total) || 1)))
const segments = (r) => {
  const total = Number(r.total) || 1
  return legend.map((l) => ({
    key: l.key,
    color: l.color,
    w: ((Number(r[l.key]) || 0) / maxTotal.value) * 100,
    title: `${l.text}：${r[l.key] || 0} / ${total}`
  }))
}
</script>
