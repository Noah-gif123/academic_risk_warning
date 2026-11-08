<template>
  <div class="space-y-2">
    <div v-for="(s, i) in stages" :key="s.key || i" class="flex items-center gap-3">
      <div class="w-28 shrink-0 text-xs text-slate-500 text-right">{{ s.name }}</div>
      <div class="flex-1 h-7 bg-slate-100 rounded relative overflow-hidden">
        <div class="h-full rounded bg-gradient-to-r from-brand-400 to-brand-600"
             :style="{ width: widthOf(s) + '%' }" />
        <span class="absolute inset-y-0 left-3 flex items-center text-xs font-medium"
              :class="widthOf(s) > 18 ? 'text-white' : 'text-slate-700'">{{ s.value }}</span>
      </div>
      <div class="w-12 text-[11px] text-slate-400">{{ pctOf(s) }}</div>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
const props = defineProps({ stages: { type: Array, default: () => [] } })
const max = computed(() => Math.max(1, ...props.stages.map((s) => Number(s.value) || 0)))
const widthOf = (s) => Math.max(2, ((Number(s.value) || 0) / max.value) * 100)
const pctOf = (s) => (max.value ? Math.round(((Number(s.value) || 0) / max.value) * 100) + '%' : '—')
</script>
